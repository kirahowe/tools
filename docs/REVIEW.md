# Compositor — design review

Every claim in this review marked **[verified]** was tested against a real
repo with `jj 0.43.0` before writing. The experiment script and raw findings
live in [`experiments/verify-jj-claims.sh`](./experiments/verify-jj-claims.sh);
it is re-runnable.

## Verdict

The design is sound and the core hypothesis is true. In a sandbox: two
sessions in isolated workspaces edited different files, both edits appeared
in the megamerge's materialized working copy after one `jj workspace
update-stale`, un-applying one made its edit vanish, and accepting a session
produced a **byte-identical composite** (hash-compared) — the "accept is a
visual no-op" claim holds exactly. **[verified]**

Also as promised by the spec: conflicts are recorded, not thrown — the
megamerge kept existing as an inspectable object while conflicted
**[verified]**; `jj op restore` rewound a keep, a rebase, and a bookmark move
in one command **[verified]**; and `jj absorb` routed a hand-edit made in the
dev workspace *through the merge commit* into the session that owned those
lines **[verified]**. The architecture earns its one-liner: isolation for
writing, composition for feeling.

But the spec's §6 recipes are pseudocode that has never met jj, and three of
them are wrong in ways that would silently break the M1 spike — worst of all
the very first one (create session), which produces a session whose change ID
permanently points at an empty commit. The fixes are all *simplifications*,
which is a good sign for the design: the substrate wants to do this. The
findings below are ordered by severity. F1–F5 are bugs in the recipes; F6–F9
are underspecified subsystems that need a decision before M2/M3; F10–F14 are
sharp edges the spec missed.

---

## What survives review unchanged

- **jj as substrate, CLI-only, no `jj-lib`.** Correct call. Everything below
  was done by shelling out; templates (`-T`) gave machine-readable output
  every time, including a one-token conflict probe:
  `jj log -r <mm> --no-graph -T 'if(conflict, "1", "0")'`. **[verified]**
- **The megamerge shape** (§4): octopus `jj new` with N parents works; edits
  in session workspaces auto-rebase it; `update-stale` in the dev workspace
  materializes it. The daemon writes no sync code. **[verified]**
- **tmux on a dedicated socket, user brings browser/terminal/agent** (§3).
  No notes. This is the right amount of nothing.
- **The isolation rule** (§12): session workspaces get their own directory
  with no `.git`; `git status` inside one fails with "not a git repository"
  **[verified]** — so agents physically can't use git even if they try, and
  git tooling never sees a conflicted commit in agent or dev workspaces. The
  spec's colocation worry mostly evaporates: only the user's own original
  checkout is colocated, and it sits on trunk.
- **Exclusive sessions for migrations** (§9). Right MVP policy. One addition
  in F13.
- **Milestone order and the M1 success criterion** (§11). Kept verbatim in
  the plan.

---

## Findings

### F1 — The create-session recipe binds the session to the wrong commit
**Severity: breaks M1. [verified]**

Spec §6:

```
jj new <trunk> -m "<intent>"                      # capture the new change ID
jj workspace add --name <sid> <path> --revision <changeId>
```

`jj workspace add --revision X` does not put the workspace's working copy
*at* X — it creates a **new working-copy commit as a child of X** (same
semantics as `jj new X`). Verified directly: the new workspace's `@` was
`mrqqpspt...`, a child of the captured `xykovtll...`.

Consequence: the agent's edits snapshot into the *child*. The captured
`session.changeId` — the spec's "primary key that never goes stale" — points
at an empty commit forever. A megamerge parented on it composites nothing.

**Fix (simpler than the spec):** the session commit *is* the workspace's
working-copy commit. Never create it separately:

```
jj workspace add --name s<N> <path> --revision <trunk>   # @ = fresh child of trunk
(in <path>)  jj describe -m "<intent>"                    # name it
session.changeId = jj log -r 's<N>@' -T 'change_id'       # THE session identity
```

The same simplification collapses SCRATCH: don't create a scratch commit at
all. `jj workspace add --name dev --revision <megamerge>` auto-creates the
empty child, and that child is scratch. One less object to manage.
**[verified]** — this is exactly how every later experiment ran.

### F2 — The apply/unapply recipe rebuilds the wrong thing; and the obvious repair is also wrong
**Severity: breaks M1 toggling. [verified]**

Spec §6 rebuilds the composite with fresh `jj new` commits on every toggle.
Three problems: `jj new` moves `@` of whichever workspace runs it (a fs-event
race window where scratch junk snapshots into the megamerge); the megamerge
and scratch change IDs churn on every toggle, so nothing downstream can hold
a reference; and the *old* scratch — holding the user's uncommitted
hand-tweaks — is orphaned by the repoint.

The tempting repair, re-parenting in place with `jj rebase -r <mm> -d
<parents...>`, is a trap: `-r` **extracts** the revision and re-parents its
*descendants onto its old parents*. Verified: after `rebase -r`, the dev
working copy itself became the three-parent merge, and the "unapplied"
session's edits stayed in the composite. Confusing to debug precisely
because the files on disk look almost right.

**Fix:** `-s` instead of `-r`. It moves the revision *with* its descendants:

```
jj rebase -s <megamerge> -d <trunk> -d <appliedA> -d <appliedB> ...
```

Verified: megamerge change ID stable across toggles, scratch (and the user's
hand-edits in it) rides along, composite content flips correctly both ways.
Apply/unapply is one command, and the megamerge is created **once** per
project, at init, with trunk as its sole parent.

### F3 — Nothing is immutable by default: `absorb` rewrote trunk
**Severity: silent data corruption in M3. [verified]**

The spec's absorb flow assumes hunks fly back into *sessions*. But `jj
absorb` targets **any mutable ancestor** whose commit last touched the lines
— and in a repo whose trunk bookmark has no remote counterpart, jj's default
`immutable_heads()` protects nothing. Verified: a hand-edit to a line only
trunk had touched was absorbed **into the trunk commit itself** ("Absorbed
changes into: wwysvxqu main* | trunk"), rewriting landed history and moving
the bookmark.

The same hole means any stray `jj describe`/`squash` a curious user runs in
our workspaces could rewrite trunk.

**Fix:** `comp init` writes repo-level config:

```
revset-aliases."immutable_heads()" = "present(<trunkBookmark>)"
```

Verified with that config: the trunk-owned hunk stays in scratch (reported
to the user as "this tweak belongs to what's live — keep it as a new
session?"), session-owned hunks still absorb correctly, and direct rewrites
of trunk are refused ("Commit ... is immutable").

### F4 — Keep can land a conflicted session onto trunk
**Severity: corrupts "what's live". Found by inspection; gate verified.**

`jj bookmark move <trunk> --to <session>` succeeds even when the session
commit is conflicted (rebases always succeed — that's the point of jj). If
trunk moved under a session (someone else kept first) and the rebase
recorded a conflict, the spec's keep recipe would promote conflict markers
to "what's live" and every future session would branch off a broken trunk.

**Fix:** keep is gated:

```
jj rebase -s <session> -d <trunk>                    # usually a no-op
jj log -r <session> -T 'if(conflict, "1", "0")'      # gate
# "1" → abort keep; tell the user this take needs reconciling first
jj bookmark move <trunk> --to <session>              # only when clean
```

### F5 — Keep's "rebase everything" revset quietly skips half the sessions
**Severity: drift accumulates. Found by inspection.**

Spec §6: `jj rebase -s 'all:roots(<trunk>..@)' -d <trunk>`. Two problems:
`@` is ambiguous (each workspace has its own; whichever directory the daemon
runs in decides what gets rebased), and `<trunk>..@` only reaches commits
that are *ancestors of scratch* — i.e. **applied** sessions. Unapplied
sessions silently stay based on old trunk and drift further behind with
every keep.

**Fix:** we already have the authoritative list of live sessions in SQLite.
Rebase them explicitly, one command each:

```
for s in liveSessions: jj rebase -s <s.changeId> -d <trunk>
```

Same cost, no ambiguity, and it hands us per-session conflict attribution
for free: after each rebase, probe `if(conflict)` and mark exactly which
session now needs attention.

### F6 — A conflicted composite must never be snapshotted, or markers become source code
**Severity: corrupts sessions in a way `absorb` can't repair. [verified]**

The spec's rule "don't update-stale while conflicted" is framed as UX (keep
the app alive on the last good composite). It is actually a *correctness
invariant*. In an early experiment run the conflicted composite was
materialized into the dev workspace and a subsequent jj command ran there:
the conflict markers were **snapshotted as literal file content** into
scratch. From that moment they persist even after the underlying conflict is
resolved, `absorb` refuses the file ("Skipping src/app.js: Is a conflict"),
and the junk sits one absorb away from being written into a session.

**Fix (daemon invariant):** while `if(conflict)` is true on the megamerge,
the daemon runs *no* jj command in the dev workspace — not `st`, not
`absorb`, not `update-stale`. It probes conflict state from any *other*
workspace (`-R` doesn't snapshot remote workspaces — only the cwd workspace
gets snapshotted). `comp absorb` gets the same gate.

### F7 — Agents *do* see conflict markers; make it a feature on purpose
**Severity: policy gap. [verified]**

"Never show conflict markers" (§8) is achievable for the human. Not for
agents: when a session commit becomes conflicted (stacking, keep-rebase) and
its workspace runs `update-stale`, jj materializes markers into the agent's
files — verified, complete with jj's annotated marker format naming both
sides. An agent mid-task will trip over them; there is no way to hold a
workspace on unconflicted content, because the conflict *is* the commit's
content now.

This is actually the mechanism that makes **send it back** work: the markers
plus a prompt ("your take now collides with what's live in src/app.js —
reconcile it") is a well-formed agent task, and when the agent saves resolved
content, the next snapshot clears the conflict for the whole graph
**[verified]** — megamerge included.

**Fix (policy, not code):** after any operation that rewrites session
commits (keep, stack, toggle of an overlapping session), the daemon runs
`update-stale` in affected session workspaces *immediately* — never lazily —
and flips conflicted sessions to a `needs-rebase` badge. Silent staleness is
the only wrong option: `jj st` errors in a stale workspace **[verified]**,
so a lazy daemon would wedge its own snapshot loop.

### F8 — "Agent says done" has no mechanism
**Severity: `ready` state is unimplementable as specced.**

§5 defines the `ready` state and §10 hangs the whole accept gesture on it,
but nothing in the design detects it. We refuse to parse the pty (correctly
— §3), so the only honest signals are:

1. **Agent hooks.** Claude Code's `Stop` hook and Codex's `notify` config can
   run a command when the agent finishes a turn. `comp new` provisions the
   session workspace with a hook config pointing at
   `comp _agent-done <sid>` (a hidden subcommand that pokes the daemon).
   Precise, per-agent, and invisible to the user.
2. **Quiescence fallback** for agents without hooks: N minutes of no fs
   events + no tmux pane activity (`#{window_activity}`) → show as `idle`,
   not `ready` — we genuinely don't know whether it finished or stalled, and
   the badge shouldn't lie.

`ready` also needs to *fail* honestly: hooks fire per-turn, so a follow-up
prompt (send-back, "still wrong") must flip the state back to `running` —
`comp` does that whenever it injects text into the session's window.

### F9 — `send-keys` for the opening prompt is a race
**Severity: flaky M1. Found by inspection.**

§6 types the intent into the agent's window immediately after spawning it.
If the CLI hasn't finished booting, keys vanish or half-arrive; multi-line
intents with quotes are a shell-escaping minefield.

**Fix:** pass the intent as an argument — `claude "<intent>"` and
`codex "<intent>"` both accept an initial prompt argv and start working on
it. `agentCmd` becomes a template (`claude {prompt}`). `send-keys` (with
`-l`, literal mode) survives only for *send-back*, where a human is watching
the window anyway.

### F10 — Undo is a three-system problem; `jj op restore` only rewinds one of them
**Severity: `comp undo` as specced leaves the tool lying about the world. [verified]**

State lives in three places: the jj repo, SQLite, and "the world"
(workspace directories, tmux windows, the dev database). `op restore`
rewinds only the first. Verified concretely: `workspace forget` + `rm -rf` +
`jj undo` resurrects the workspace *record* — `jj workspace list` shows it —
while the directory is gone; every subsequent jj touch of that workspace
errors. Undoing a `keep` has the same shape: trunk moves back, the session
commit revives, but its workspace, agent window, and SQLite row (`accepted`)
are all gone or stale.

**Fix:** `comp undo` = `jj op restore` **followed by `reconcile()`**, a
function that treats the repo as the source of truth and repairs the other
two systems to match:

- For each SQLite session: does its change ID exist? abandoned? an ancestor
  of trunk? → correct `state`/`applied`.
- Workspace record without a directory → recreate
  (`jj workspace add` + `jj edit <changeId>` inside it); directory without a
  record → quarantine.
- Missing tmux window for a live session → respawn with the agent's resume
  flag (`claude --continue`), which restores the transcript too.

The payoff: `reconcile()` is *also* crash recovery. Daemon start runs it
unconditionally, which makes the daemon killable at any moment by
construction — cheaper than trying to make every operation atomic across
three systems.

### F11 — The daemon needs one mutex and two snapshot settings
**Severity: robustness. [verified in part.]**

- **Serialize all jj invocations** behind one in-process mutex. jj tolerates
  concurrent ops (it forks and merges the op log), but a daemon that
  interleaves `st`/`rebase`/`update-stale` across workspaces gets
  nondeterministic op ordering for zero benefit.
- **Large files:** snapshotting warns and refuses files >1MiB
  (`snapshot.max-new-file-size`) — verified with a 2MB artifact. Not fatal,
  but each snapshot re-warns, and genuinely large artifacts (SQLite dev DBs,
  media) silently never enter the composite. `comp init` should check
  `.gitignore` covers build output and the dev DB, and say so when it
  doesn't.
- The `snapshot.auto-track` escape hatch exists and works
  (`--config 'snapshot.auto-track="none()"'` verified) if a project's dev
  server writes junk we can't ignore-list; not needed for MVP.

### F12 — `conflictsWith` needs an attribution story
**Severity: underspecified; cheap answer exists.**

Pairwise exact answers cost O(n²) trial merges. MVP: `jj resolve --list -r
<megamerge>` names the conflicted *files* **[verified]** (including
"3-sided conflict" annotations); each session's touched-file set is one
`jj diff -r <s> --name-only` away and cached on every snapshot. Intersecting
them attributes almost all real conflicts correctly, and F5's explicit
per-session rebases catch the rest at keep time. Good enough for a badge;
exact pairwise merges can wait until someone actually asks.

### F13 — Lockfiles are migrations
**Severity: will hit within the first hour of real use.**

Adjacent-line edits conflict more readily than intuition suggests
(**[verified]** — the first sandbox, a 4-line file, conflicted on edits to
*neighboring* lines because the diff hunks' context overlapped). The
worst real-world instance: two sessions each run `npm install something` and
both rewrite `package-lock.json`; the composite conflicts forever after.
Same fix as §9, zero new machinery: ship default `migrationGlobs`-style
handling for lockfiles — either include them in `exclusiveGlobs` defaults,
or (better, later) resolve lockfile conflicts by regenerating in scratch.
The review flags it; the plan ships the cheap version (default globs).

### F14 — Operational gaps: who starts what, where
**Severity: unwritten but necessary.**

The spec never says who runs the daemon or the dev server. Cheapest answer
consistent with §3: **the tmux socket is the supervisor.** `comp init`
creates the `compositor` tmux session with window 0 = daemon (`comp
_daemon`), window 1 = dev server (`devServerCmd` in the dev workspace);
sessions get windows 2+. Everything survives terminal disconnects, the user
can eavesdrop on any component, and "is it running" is `tmux -L compositor
ls`. `comp` commands talk to the daemon over a unix socket in the state dir;
if it's dead, any `comp` command restarts it (then `reconcile()` makes that
safe).

One more default the spec leaves open: **where workspaces live**. Not inside
the repo (agents would see each other via relative paths; the fs watcher
would recurse). Default: `~/.local/state/compositor/<project>/ws/<sid>`.

---

## Smaller notes

- **`update-stale` is safe to call when not stale** ("Attempted recovery,
  but the working copy is not stale", exit 0) **[verified]** — the daemon
  can call it unconditionally instead of tracking staleness.
- **Scratch's change ID is not stable** — `absorb` rebuilt the dev `@` with
  a new change ID **[verified]**. Never cache it; resolve via the workspace
  revset symbol (`dev@`) every time. Session `@`s are only rewritten in
  place (amend), so `session.changeId` remains a safe primary key — F1's fix
  is what makes that true.
- **Keep = byte-identical is hash-verified**, not just eyeballed: sha256 of
  the composite before and after a keep matched exactly.
- **`jj new --no-edit`** everywhere the daemon creates commits; it's what
  prevents the daemon's own workspace `@` from wandering.
- The user's own checkout (the original repo directory) is jj's `default`
  workspace. With F3's immutability config it effectively never goes stale
  (nothing rewrites trunk ancestors), and their normal git tooling keeps
  working there. Worth a line in the docs, nothing more.
- Vocabulary table (§1): good; the plan adopts it in all CLI strings. One
  addition — conflicts are user-facing as **"collides"**, never "conflict",
  which reads as VCS-speak.

## Two philosophical challenges (recorded, then dropped)

1. **Is "agents never see each other" too absolute?** Two sessions building
   against the same API will each invent half of it. The spec's answer
   (stacking) covers the discovered-dependency case, and the escape hatch —
   keep A early, new session branches off updated trunk — is honest. The
   restraint survives contact; no change.
2. **Is one dev server enough?** When the composite collides, the human
   can't try the colliding take at all until they intervene. A per-session
   preview server would fix that and quadruple the runtime-state problem
   (§9). The spec's priorities are right; noted as a possible M5, not argued
   further.
