# Compositor — implementation plan

This is the plan I intend to build, incorporating every fix from
[REVIEW.md](./REVIEW.md). Where it deviates from [SPEC.md](./SPEC.md), the
review finding is cited. The mental model, vocabulary, non-goals, and
milestone order are unchanged.

## 1. Decisions up front

| Decision | Choice | Why |
|---|---|---|
| Language | **TypeScript on Bun** | Spec allows Go/Rust/Bun and says ship fastest. Bun has sqlite (`bun:sqlite`), subprocess (`Bun.spawn`), and fs watching built in — the dependency list §3 demands is literally zero packages. `bun build --compile` gives the single binary. Tradeoff acknowledged: ~90MB binary vs Go's ~10MB; irrelevant for a local dev tool. Nothing below is Bun-specific in shape — if this offends, say so before M1 and it becomes Go with `fsnotify` at near-zero translation cost. |
| jj version | **pin minimum 0.43** | Everything was verified against 0.43.0. `comp init` hard-fails on older with an install hint. |
| Name | **compositor / `comp`** for now | Spec says rename; naming is not on M1's critical path. Candidates parked: `greenroom` (where acts wait before going live), `soundstage`, `mixer`. Decide before anything is published. |
| Repo layout | project at repo root | This repo currently contains only a LICENSE. If it's meant to stay a multi-tool monorepo, everything moves under `compositor/` — one `git mv`, zero code changes. |
| Workspace homes | `~/.local/state/compositor/<project-hash>/ws/<sid>`, dev workspace at `.../ws/dev` | Outside the repo so agents can't see each other or the composite, and the repo's own watchers don't recurse into them (review F14). |
| State dir | same root: `state.sqlite`, `daemon.sock`, `daemon.log` | One place to `rm -rf` when developing. |

## 2. Architecture (unchanged from spec, with F14's supervisor answer)

```
tmux -L compositor                      ← the supervisor; survives everything
 ├─ win 0: comp _daemon                 ← the loop (§7), foreground, logs visible
 ├─ win 1: devServerCmd                 ← in the dev workspace
 ├─ win 2: s1 → claude "<intent>"       ← session 1
 └─ win 3: s2 → codex "<intent>"        ← session 2

comp <cmd>  ── unix socket ──▶  daemon  ── mutex'd jj CLI ──▶  repo store
                                   │
                                   └── SQLite (sessions, project config)
```

- Every jj invocation goes through one wrapper: adds `--no-pager`,
  `--color never`, the F3 immutability config, a per-daemon mutex, and
  parses only `-T` template output (JSON via `.escape_json()`). Raw
  human output is never scraped.
- The CLI is thin: it sends a command over the socket and prints the reply.
  If the daemon is down, `comp` starts it (tmux window 0) and retries;
  `reconcile()` on daemon boot makes this safe (F10).

## 3. The graph, corrected (F1, F2)

```
                       ┌── s1@  (session commit = workspace @) ──┐
 trunk (bookmark) ─────┼── s2@                                   ┼── MEGAMERGE ── dev@ (scratch)
                       └── s3@                                   ┘
```

- **Session commit = the session workspace's working-copy commit.** Created
  by `jj workspace add -r <trunk>` + `jj describe -m <intent>` inside it.
  Its change ID is the session's primary key. There is no separate
  pre-created commit (F1 — the spec's recipe binds to an empty parent).
- **Megamerge is created once** at init (`jj new <trunk> -m "compositor
  megamerge" --no-edit`) and never recreated. Apply/unapply/keep all mutate
  its parent list in place via `jj rebase -s` (F2 — `jj new` rebuilds churn
  identity; `rebase -r` extracts instead of re-parenting).
- **Scratch is not created explicitly.** `jj workspace add --name dev -r
  <megamerge>` auto-creates the working-copy child; that child *is* scratch.
  Its change ID is unstable (absorb rebuilds it) — always resolved as the
  revset `dev@`, never cached.

## 4. Operations → jj commands (corrected recipes)

Each block replaces the corresponding §6 recipe. `JJ` = the wrapped CLI.

**init** (once per project)
```
jj git init --colocate            # only if repo isn't jj-managed yet
write repo config:
  revset-aliases."immutable_heads()" = "present(<trunk>)"        # F3
sanity: .gitignore covers build dirs + dev DB; warn loudly if not # F11
MM = jj new <trunk> -m "compositor megamerge" --no-edit           # once, forever
jj workspace add --name dev <ws/dev> -r <MM>
tmux -L compositor new-session: win0 comp _daemon, win1 devServerCmd (cwd ws/dev)
```

**new** (create session)
```
jj workspace add --name s<N> <ws/sN> -r <trunk>     # @ = the session commit (F1)
(in ws)  jj describe -m "<intent>"
changeId = jj log -r 's<N>@' --no-graph -T 'change_id'
run warmupCmd in ws
tmux new-window -n s<N> -c <ws> -- <agentCmd rendered with {prompt}=intent>   # F9, no send-keys
apply it:  jj rebase -s <MM> -d <trunk> -d <...currently applied> -d <changeId>
provision agent-done hook in ws (claude Stop-hook / codex notify → comp _agent-done sN)  # F8
```

**daemon loop** (§7 shape kept; gates added)
```
on fs events in a session ws, debounced 500ms quiet:
    (in ws) jj st                       # snapshot; megamerge auto-rebases
    on "stale working copy" error: jj workspace update-stale, notify if session
      now conflicted, then retry once                                  # F7
    refresh session.filesTouched (jj diff -r sN@ --name-only), exclusive flag
    mark composite dirty
when dirty:
    conflicted = jj log -r <MM> --no-graph -T 'if(conflict, "1", "0")'
    if "0": (in ws/dev) jj workspace update-stale     # hot reload does the rest
    else:   DO NOT run any jj command in ws/dev       # F6 — poisoning invariant
            attribute: jj resolve --list -r <MM> ∩ session file-sets   # F12
            surface "s3 collides with s1 (2 files)"
```

**toggle** (apply/unapply)
```
jj rebase -s <MM> -d <trunk> -d <...new applied set>     # F2; MM identity stable
then the dirty-composite path above (conflict gate before update-stale)
exclusive rule (§9): applying an exclusive session unapplies others & vice versa,
with DB snapshot/restore hooks around it
```

**keep**
```
jj rebase -s <sid> -d <trunk>                    # usually no-op
GATE: if(conflict) on <sid> → abort: "this take collides with what's live —
      send it back to its agent first"                              # F4
jj bookmark move <trunk> --to <sid>
for each other live session (from SQLite, applied or not):          # F5
    jj rebase -s <other> -d <trunk>
    probe if(conflict) → badge those sessions "collides with what's live"
    (in that session's ws) jj workspace update-stale                # F7
jj rebase -s <MM> -d <trunk> -d <...remaining applied>   # kept take now arrives via trunk
(in ws/dev) jj workspace update-stale     # byte-identical → visual no-op (hash-verified)
jj workspace forget s<N>; rm -rf ws; tmux kill-window
```

**drop**
```
jj abandon <sid>
jj rebase -s <MM> -d <trunk> -d <...remaining applied>
forget/rm/kill as above; conflict-gate + update-stale in ws/dev
```

**absorb**
```
GATE: dev@ must be conflict-free (F6) and trunk immutability configured (F3)
(in ws/dev) jj absorb
leftovers stay in scratch → report: "2 tweaks belong to what's live —
  make them a new take?  comp new --from-scratch"
refresh all session file-sets (absorb amended session commits)
```

**undo**
```
jj op restore <opId>        # or `jj undo` for last op
reconcile()                 # F10 — repo is truth; repair SQLite + world:
  - session change id missing/abandoned/in-trunk → fix state & applied flags
  - workspace record without dir → jj workspace add + jj edit <changeId>
  - live session without tmux window → respawn agent with --continue
  - MM missing → recreate from applied set
reconcile() also runs unconditionally on daemon start = crash recovery
```

## 5. Data model deltas from §5

```ts
interface Session {
  // ... as specced, plus:
  filesTouched: string[]   // cached from jj diff --name-only on every snapshot (F12)
  lastError: string | null // last surfaced problem (collision, stale, hook failure)
  readyAt: number | null   // set by the agent-done hook (F8)
}
type SessionState = /* as specced */ | 'idle'   // quiescence fallback ≠ ready (F8)

interface Project {
  // ... as specced, plus:
  exclusiveGlobs: string[]     // supersedes migrationGlobs; DEFAULTS include
                               // '**/migrations/**' AND lockfiles (F13)
  dbSnapshotCmd?: string       // §9 mentions these; now modeled
  dbRestoreCmd?: string
  agentDoneHook: 'claude' | 'codex' | 'none'   // F8 provisioning
}
```

`applied` and `conflictsWith` are treated as *derived* caches — `reconcile()`
recomputes them from the repo (megamerge parent list; conflict probes).
SQLite is authoritative only for things the repo can't know: intent text,
numbering, tmux window names, state-machine timestamps.

## 6. Module layout

```
src/
  cli.ts          # arg parsing, socket client, output formatting (vocabulary lives here)
  daemon.ts       # loop, socket server, debouncer
  jj.ts           # THE wrapper: mutex, --no-pager, immutability config, -T parsing
  graph.ts        # megamerge lifecycle: apply set → rebase -s command; conflict probes
  sessions.ts     # create/keep/drop/toggle state machine
  reconcile.ts    # repo→(SQLite, world) repair; used by undo, boot, crash recovery
  tmux.ts         # socket-scoped tmux driver
  hooks.ts        # agent-done hook provisioning per agent flavor
  db.ts           # bun:sqlite schema + migrations
  config.ts       # project config load/validate
docs/
  SPEC.md  REVIEW.md  PLAN.md
  experiments/verify-jj-claims.sh
```

Estimated size: ~1500–2000 lines through M3. The daemon loop itself should
stay under 100.

## 7. Milestones

**M0 — skeleton (half a day).** `jj.ts` wrapper + `tmux.ts` + `db.ts` +
`comp init` (including F3 config write and the F11 gitignore check).
Exit: `comp init` on a sample Vite app yields a running dev server in the
dev workspace, megamerge parented on trunk only.

**M1 — the spike (one day).** `comp new`, `comp ls`, `comp toggle`,
`comp app`, `comp _daemon` (fs-watch → snapshot → gate → update-stale).
Sessions spawn real `claude` CLIs.
**Exit criterion, verbatim from the spec: two agents edit different files;
both changes appear in the running app; toggling one off makes it
disappear.** The corrected recipes were already proven by hand in the
review experiments, so the spike is wiring, not research. Spec's warning
stands: if this doesn't feel magic, stop and rethink — don't decorate.

**M2 — lifecycle (1–2 days).** `comp keep` (gated, F4/F5/F7), `comp drop`,
`comp undo` + `reconcile()` (F10) wired into daemon boot.
Exit: keep is hash-verified byte-identical; kill -9 the daemon mid-keep,
restart, `comp ls` tells the truth.

**M3 — the good parts (2–3 days).** `comp absorb` (gated, F3/F6), collision
surfacing + stack/pick/send-back (F12, F7's markers-as-prompt), exclusive
sessions incl. lockfile defaults + DB snapshot/restore hooks (§9, F13),
agent-done hooks (F8).
Exit: the §8 conflict scenario end-to-end — collide, stack, watch the
composite go clean; hand-tweak in the browser-facing app, absorb, see the
hunk in the right session's take.

**M4 — the status pane (1 day).** The ~200-line list TUI in a tmux pane.
Renders from `comp ls --json` over the socket. Nothing else.

Deliberately not scheduled (matching §13 plus review): per-session preview
servers, exact pairwise conflict attribution, lockfile regeneration,
multi-project daemon.

## 8. Risks that remain after the review

1. **Agent-done hooks are per-agent and version-sensitive** (F8). Mitigation:
   the `idle` quiescence fallback is agent-agnostic and ships in M1; hooks
   are M3 polish.
2. **Warmup cost is a product risk, not a technical one** (§12). `warmupCmd`
   is the user's script; the sample project's docs will show the fast
   patterns (hardlink clone, pnpm store, `.env` copy). If warmup is slow,
   sessions feel expensive and the willy-nilly spawning ethos dies.
3. **Composite collides too often in practice** (F13 adjacency). The
   dampeners: lockfile exclusivity by default, stacking as the
   one-keystroke resolution. If real usage still collides constantly, that's
   a verdict on the hypothesis, and M1's "take its verdict seriously" clause
   applies.
4. **jj evolves fast.** The wrapper pins a minimum version and centralizes
   every invocation; command-shape drift is one file's problem.
