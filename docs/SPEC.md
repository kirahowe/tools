# Compositor — build spec

> Working title. Rename it. Just don't call it anything with "git" in it.

> **Status note:** this is the original spec, committed verbatim for reference.
> See [REVIEW.md](./REVIEW.md) for the design review (several §6 recipes are
> corrected there, with evidence) and [PLAN.md](./PLAN.md) for the
> implementation plan.

## 0. What we are building, in one paragraph

A local tool that manages many concurrent coding-agent sessions against a single repo, and composites all their in-flight work into **one live, running dev server** that the human plays with. The human's review gesture is not reading a diff — it is *using the app*. When the app feels right, they accept a session and it lands on trunk.

This is **not** a git UI. Version control is the storage engine, and the user must never see it.

It is also **not an application with a window.** See §3.

---

## 1. Mental model (do not violate this)

- The **stage** is the running app. One dev server. Hot reload. Always up.
- A **session** is a conversation with an agent about one desired change. It has an intent, a transcript, a diff, and a state.
- Sessions are **applied** or **not applied**. The stage renders `trunk + every applied session`.
- **Accepting** a session flattens it into trunk. The stage does not visibly change — accept is a visual no-op. This is correct and intentional. The user is confirming what is already in the room with them.
- **Rejecting** unapplies it. The app snaps back.

The one-line architectural claim:

> **Isolation for writing, composition for feeling.**

Agents must NOT see each other's work — otherwise agent B reads the working tree, sees agent A's half-finished button, and silently writes code that depends on it. Agents get isolated workspaces branched off clean trunk. The *human* sees everything at once, because "does it feel polished" is a judgment about the whole product.

### Vocabulary (user-facing)

Naming is the model. If we use git's nouns we will accidentally build git.

| Never say | Say |
|---|---|
| branch | **change**, or **take** |
| merge / land | **keep** |
| checkout | **try it on** |
| pull request | (nothing — the conversation *is* the review) |
| main / trunk | **what's live** |
| worktree, commit, rebase | (never surface these at all) |

---

## 2. Substrate: Jujutsu (jj) — non-negotiable

We use **jj**, not git. jj is git-compatible — it writes real git objects into a normal git repo, so GitHub, CI, and teammates are unaffected.

**This is not a preference and there is no `--backend=git` escape hatch.** The composite is only possible because of jj's data model. A git implementation would be a promise we cannot keep.

Four jj properties are load-bearing. If you find yourself fighting one of these, you have misunderstood the design:

1. **The working copy is a commit.** jj auto-snapshots the working copy into a real commit on nearly every command. No staging area, no `add`. Consequence: **the agent never runs a VCS command.** It just edits files. We snapshot.
2. **Change IDs are stable.** Each commit has a commit hash (content) *and* a change ID that survives amends, rebases, splits, and squashes. Consequence: **`session.change_id` is our primary key and it never goes stale.**
3. **Rewriting a commit auto-rebases its descendants.** Consequence: when an agent edits, the megamerge downstream of it is rebased *for us*. We do not orchestrate this. We observe it.
4. **Conflicts are data, not errors.** A conflict is stored as a logical structure inside the commit. Rebases *always succeed*. Conflicted commits can be further rebased and merged. Consequence: **a conflicting composite still exists as an object we can inspect and report on**, instead of exploding. This is impossible in git and is the entire reason we're on jj.

Plus: the **operation log** versions the whole repo state, and `jj op restore` undoes anything — including rebases and merges. That's our global undo.

### Talking to jj

**Shell out to the `jj` CLI. Do not link `jj-lib`.** Its API is explicitly unstabilized; jj's own architecture docs admit little thought has gone into which symbols are exposed. The CLI is the supported contract.

- Always pass `--no-pager`.
- Never scrape human-readable output. Use `-T/--template`, and use the template language's `.escape_json()` to emit JSON/JSONL you can parse safely.
- `jj config get` (not `jj config list`) for scripting.

---

## 3. What we own: nothing but the composite

The user brings their own everything:

| Concern | Whose | How |
|---|---|---|
| Browser | Theirs | We just `open http://localhost:PORT`. **We do not embed a webview.** They need devtools and responsive mode; an embedded browser is a strictly worse browser. |
| Terminal | Theirs | Ghostty, kitty, WezTerm, iTerm — their font, their keybinds, their config. |
| Agent | Theirs | We spawn the real `claude` / `codex` CLI in a pty. We are wrapping these agents, not reimplementing them. |
| Editor | Theirs | Untouched. |
| **The composite** | **Ours** | This is the whole product. |

**We do not render terminal cells and we do not render web pages.** No Electron, no Tauri, no xterm.js, no node-pty. If a feature requires us to emulate something someone else already emulates better, cut the feature.

### tmux is the pty host

One session = one tmux window running the real agent CLI. We drive it via `new-window`, `send-keys`, `switch-client`, `capture-pane`. The user never needs to *know* tmux is there; they just attach with their own terminal.

**Run on a dedicated socket: `tmux -L compositor`.** This avoids colliding with any tmux the user already runs, and prevents nested-tmux hell.

Free consequences: works over SSH, survives our daemon crashing, and the whole thing is a single binary.

### Stack

- One binary. Go or Rust or Bun/TS — pick what you'll ship fastest in. There is no rendering problem left to solve, so the runtime barely matters.
- SQLite for session state.
- An fs watcher (`notify` / `chokidar` / `fsnotify`).
- That's the entire dependency list.

---

## 4. The commit graph

```
                        ┌── session A ──┐
  trunk ────────────────┼── session B ──┼── [MEGAMERGE] ── [SCRATCH] ← dev workspace @
                        └── session C ──┘
                             (each in its
                              own workspace)
```

- **trunk** — a jj bookmark. "What's live."
- **session** — exactly one jj change, whose change ID identifies the session forever.
- **MEGAMERGE** — a commit whose parents are trunk + every *applied* session. `jj new` accepts any number of parents; octopus merges are natively supported. This commit *is* the composite.
- **SCRATCH** — an empty child of the megamerge. **The dev server's working copy is SCRATCH, not MEGAMERGE.** Two reasons:
  - Build artifacts and hot-reload junk get snapshotted into SCRATCH, where they're harmless, instead of polluting the composite.
  - When the human hand-edits something while poking at the app, that edit lands in SCRATCH — and `jj absorb` automatically redistributes each hunk back into whichever session's commit last touched those lines. **`comp absorb` is a headline feature. Get it working early.**

---

## 5. Data model

```ts
type SessionState =
  | 'drafting'    // prompt written, agent not started
  | 'running'     // agent working
  | 'ready'       // agent says done; awaiting human judgment
  | 'accepted'    // landed on trunk (terminal)
  | 'discarded'   // abandoned (terminal)

interface Session {
  id: number              // short, human-typeable: `comp keep 3`
  changeId: string        // jj change ID — stable forever, survives all rewrites
  workspacePath: string   // jj workspace for this session's agent
  tmuxWindow: string      // window in the `compositor` socket
  applied: boolean        // is it a parent of the megamerge?
  exclusive: boolean      // touches migrations — see §9
  intent: string          // the original prompt, verbatim
  state: SessionState
  conflictsWith: number[] // computed, see §8
}

interface Project {
  repoPath: string
  trunkBookmark: string       // default 'main'
  devServerCmd: string        // 'npm run dev'
  agentCmd: string            // 'claude' | 'codex' | ...
  warmupCmd: string | null    // 'npm ci' — run in each new workspace
  openAppCmd: string          // 'open http://localhost:3000'
  mux: string                 // 'tmux -L compositor'
  devWorkspacePath: string
  migrationGlobs: string[]    // e.g. ['**/migrations/**']
}
```

Persist to SQLite. `changeId` is the join key to the repo. Every field in `Project` is somebody else's program — that uniformity is the product boundary.

---

## 6. Operations → jj commands

> **⚠ Superseded.** Several recipes below are subtly wrong (verified against jj
> 0.43): the workspace/session identity flow, the apply/unapply rebuild, and
> the keep revset. See REVIEW.md findings F1–F5 and the corrected recipes in
> PLAN.md §4. The intent of each operation is unchanged.

**Create session**
```
jj new <trunk> -m "<intent>"                       # capture the new change ID
jj workspace add --name <sid> <path> --revision <changeId>
<warmupCmd> in <path>
tmux -L compositor new-window -n <sid> -c <path> '<agentCmd>'
tmux send-keys -t <sid> '<intent>' Enter
```

**Agent made edits (the daemon loop)**
```
jj st                     # in the session workspace → snapshots, amends the session commit
                          # jj auto-rebases the megamerge (a descendant)
jj workspace update-stale # in the dev workspace → writes the new composite to disk
                          # hot reload fires. We wrote no sync code.
```

**Apply / unapply** — rebuild the megamerge with the new parent set:
```
jj new <trunk> <appliedChangeIds...> -m "composite"
jj new <megamergeChangeId> -m "scratch"
# point the dev workspace @ at SCRATCH, update-stale
```

**Keep (accept)**
```
jj rebase -s <sessionChangeId> -d <trunk>
jj bookmark move <trunk> --to <sessionChangeId>
# rebuild megamerge without it as an explicit parent (it's in trunk now)
# → composite content is byte-identical → NO VISIBLE CHANGE. Correct.
jj rebase -s 'all:roots(<trunk>..@)' -d <trunk>   # rebase every other live session onto new trunk
jj workspace forget <sid>; rm -rf <path>; tmux kill-window -t <sid>
```
That penultimate line is the whole ballgame: it rebases *every* in-flight session onto the new trunk in one command, with conflicts **recorded, not thrown**. This is what makes "spawn sessions willy-nilly" survivable.

**Discard**
```
jj abandon <sessionChangeId>
jj workspace forget <sid>; rm -rf <path>; tmux kill-window -t <sid>
# rebuild megamerge; dev server snaps back
```

**Absorb my hand-tweaks**
```
jj absorb        # in the dev workspace (SCRATCH) — hunks fly back to the right sessions
```

**Undo (global, always available)**
```
jj op log
jj op restore <opId>
```
Every destructive action must be one command from being un-done.

---

## 7. The daemon

This is the heart of the app and it is small.

```
loop:
  for each running session:
    if fs watcher fired on <workspacePath> and quiet for 500ms:
      run `jj st` there        # snapshot → amend → jj auto-rebases the megamerge
      mark composite dirty
  if composite dirty:
    check conflicts (§8)
    if clean:
      `jj workspace update-stale` in devWorkspacePath
      # files change on disk; the dev server's own hot reload does the rest
    else:
      do NOT write the conflicted state to the dev workspace
      leave the last-good composite on disk; surface the conflict
```

**Do not write a "sync to dev server" mechanism.** jj *is* the sync mechanism. If you're writing file-copy code, stop and re-read §2.3.

Debounce hard. An agent mid-edit will thrash the fs watcher.

---

## 8. Conflicts

Because jj stores conflicts as data, the megamerge always *exists* even when two applied sessions collide. But its materialized working copy would contain conflict markers, which breaks the build. So:

- After rebuilding the megamerge, run `jj resolve --list` on it.
- If non-empty: **do not** update the dev workspace. The app stays alive on the last good composite.
- Surface it as a product decision, not an error: *"Session 3 collides with Session 1 in 3 places."* Offer:
  - **Stack it** — `jj rebase -s <B> -d <A>`. B now depends on A; both stay applied. Usually what they want.
  - **Pick one** — unapply the other.
  - **Send it back** — hand the conflict to B's agent as a prompt.

Never show conflict markers. Never open a three-way merge tool. That's a git UI and we are not building one.

---

## 9. Runtime state — the real boss fight

jj versions files. It does not version your database, seed data, caches, background jobs, or `.env`. **Unapplying a session that ran a migration does not un-migrate the DB.** This is the single thing most likely to make "magically works" not work.

MVP policy (implement this; don't get clever yet):

- If a session's diff touches `migrationGlobs`, mark it **exclusive**.
- An exclusive session can only be applied alone. Applying it unapplies everything else, and vice versa.
- Snapshot/restore the dev DB around exclusive sessions (`pg_dump`, template db, or `cp` for SQLite — per project config).

Later: per-session database branching. Not now.

---

## 10. Surfaces

**The CLI** is the primary interface.
```
comp new "add a dark mode toggle"    # spawn session, open its tmux window
comp ls                              # session list
comp toggle 2                        # apply/unapply — app changes under your cursor
comp keep 1                          # land it
comp drop 3                          # abandon it
comp absorb                          # push my hand-tweaks back into the right sessions
comp undo                            # jj op restore
comp app                             # open the stage in my browser
```

**The status pane** is our only GUI: a ~200-line TUI in a tmux pane. A session list — id, intent (one line), state, applied toggle, conflict badge, and a "needs you" marker when an agent goes `ready`. It renders a list. It does not emulate anything.

**The accept gesture.** When an agent reports done, the session goes `ready` and asks, in its own window: *"Is that done?"* The human answers by **playing with the app**, then runs `comp keep 1` or types what's still wrong into the agent's window. Feedback goes back into the same session, which is *still applied*, and the app hot-reloads under their hands.

**The diff is an escape hatch, not a surface.** `comp diff 2` exists. If the user lives in it, we've failed.

---

## 11. Milestones

**M1 — the spike. Do this first and take its verdict seriously.**
No status pane, no polish. A CLI that creates 2 sessions in 2 jj workspaces, opens 2 tmux windows running the real agent, maintains a megamerge + scratch, runs the daemon loop, and serves one dev server.

> **Success criterion: two agents edit different files; both changes appear in the running app; toggling one off makes it disappear.**

That is the entire hypothesis. It should take an evening. If it works, everything else is decoration. If it doesn't, the design is wrong — stop and rethink, don't build around it.

**M2 — lifecycle.** keep, drop, rebase-all-on-land, `comp undo`.

**M3 — the good parts.** `comp absorb`. Conflict detection + stack/pick/send-back. Exclusive sessions for migrations.

**M4 — the status pane.** Only now. It's 200 lines and it's the last thing you need.

---

## 12. Known sharp edges

- **Workspace warm-up cost.** Each jj workspace is a fresh checkout — no `node_modules`, no `.env`. This is the #1 reason people abandon parallel-agent setups. **Solve it properly on day one** via `warmupCmd` (symlink, copy, or hardlink the dep tree).
- **Colocated git + conflicted commits.** jj's docs warn that git tooling misreads conflicted commits. Don't colocate the dev workspace; keep git tools away from it.
- **Don't let agents see the composite.** An agent's workspace `@` is *its own session, off trunk*. Never off the megamerge. If you catch yourself giving an agent the composite so it "has more context," you have reintroduced the exact coupling this design exists to prevent.
- **Don't teach the agent jj.** It has no VCS instructions in its prompt. It edits files. Any design where the agent runs `jj` commands is a bug.
- **Nested tmux.** Always `-L compositor`. Never assume the user isn't already in tmux.

---

## 13. Non-goals

- Not a git client. Not a diff reviewer. Not a kanban board. Not a PR tool.
- **Not a terminal emulator. Not a browser.** We render neither cells nor pages.
- Not multiplayer. One human, one machine, one dev server.
- **Not a replacement for Claude Code / Codex — a harness around them.** Do not drive them headlessly via their streaming APIs and render our own chat UI: that path means reimplementing permission prompts, plan mode, and slash commands, i.e. rebuilding the agent's front end. We spawn the real CLI.
- Not competing with Conductor / Sculptor / Vibe Kanban. Their thing — session management — is commoditized and is being absorbed into the agents themselves. **Our thing is the compositor.** If a feature is not in service of the live composite, cut it.
