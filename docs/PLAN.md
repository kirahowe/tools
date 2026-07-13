# Compositor — implementation plan

This is the plan I intend to build, incorporating every fix from
[REVIEW.md](./REVIEW.md). Where it deviates from [SPEC.md](./SPEC.md), the
review finding is cited. The mental model, vocabulary, non-goals, and
milestone order are unchanged.

## 1. Decisions up front

| Decision | Choice | Why |
|---|---|---|
| Language | **Babashka (decided)** | Bun ruled out (vendor independence — see §1a). bb chosen: first-class concurrency (core.async, built in), an objectively superior data-wrangling language for parsing jj's JSON templates, ecosystem match with `clef`, and an already-owned `gen-script`→homebrew release pipeline. §1a records the full reasoning and the Go comparison. |
| TUI (M4) | **charm.clj** (Bubble Tea for Clojure) | The status pane is a *live, self-refreshing* dashboard, not one-shot prompts — Elm-style model/update/view is the right shape, and charm.clj ships a scrollable list. It's in-process and reacts to the same core.async messages the daemon emits, needs no extra binary (bb ≥ 1.12.215 bundles JLine3; borkdude made bb charm.clj-compatible on purpose), and already depends on core.async like us. Beta API — acceptable at M4. (gum was the earlier pick; it's for prompts, not live views — see §6.) |
| Concurrency | **core.async** — channels + real threads | Built into bb (no pod). The daemon's serialized jj access is a single-consumer channel (no explicit lock); per-session watch events and the socket loop are their own threads. See §1a and `docs/concurrency-notes.md`. |
| jj version | **pin minimum 0.43** | Everything was verified against 0.43.0. `comp init` hard-fails on older with an install hint. |
| Name | **compositor / `comp`** for now | Spec says rename; naming is not on M1's critical path. Candidates parked: `greenroom` (where acts wait before going live), `soundstage`, `mixer`. Decide before anything is published. |
| Repo layout | **`compositor/` subdirectory** | The repo is named `tools` and plausibly a monorepo; the tool gets its own directory with its `bb.edn` at the root. Docs live with it at `compositor/docs/`. |
| Workspace homes | `~/.local/state/compositor/<project-hash>/ws/<sid>`, dev workspace at `.../ws/dev` | Outside the repo so agents can't see each other or the composite, and the repo's own watchers don't recurse into them (review F14). |
| State dir | same root: `state.sqlite`, `daemon.sock`, `daemon.log` | One place to `rm -rf` when developing. |

### 1a. Language rationale (the one decision that needed you)

Three candidates were on the table. This tool is, at its core, a subprocess
conductor — its hot path is spawning and parsing `jj` and `tmux`, watching a
filesystem, and holding a little SQLite state. Every candidate can do that;
they differ on distribution and on how much of the runtime you inherit.

- **Bun / TS — dropped, for the reason you raised.** The zero-dependency
  story was the whole appeal (sqlite, subprocess, fs-watch all built in),
  but a tool whose pitch is *"the user brings their own everything; we own
  almost nothing"* should not itself be married to a runtime now owned by one
  of the agent vendors it wraps. That's not a technical objection — Bun is
  fine software — it's a coherence one. A vendor-neutral compositor sitting
  on top of `claude` and `codex` shouldn't have a vendor's runtime as its
  foundation. Not insane to keep; just off-brand, and easy to avoid.

- **Babashka — the leading candidate. Two objections I raised against it
  were wrong; I checked, and they don't survive contact with real projects.**
  For a shell-orchestration tool it's the most natural fit of the three:
  `babashka.process` and `babashka.fs` are built for exactly this, and parsing
  jj's `-T ... .escape_json()` output into Clojure maps is *nicer* than
  unmarshalling into Go structs — the whole jj/tmux-wrapping core, which is
  where the real work is, is more pleasant in bb.

  I had docked it for two things. Both are false:
  1. ~~"Can't ship a self-contained binary without a heavyweight GraalVM
     build."~~ **Wrong.** Evidence: the author's own `clef`
     (github.com/kirahowe/clef). A `gen-script` task emits a standalone
     uberscript; on a `v*` tag, `release.yml` verifies + smoke-tests it,
     SHA256s the source archive, and auto-updates the `homebrew-clef` tap's
     `Formula/clef.rb`. `brew install` then delivers a working command in one
     step (bb comes in as a formula dependency). No native-image, no finicky
     build — and it's a release pipeline the author already owns and runs.
     `bbin` covers the non-Homebrew install path with the same ease.
  2. ~~"Weaker fit for a long-running stateful daemon."~~ **Overstated.**
     Evidence: borkdude's `quickblog` runs a long-lived `watch` loop on the
     `org.babashka/fswatcher` pod plus a `babashka.http-server`, for hours;
     bb also hosts nREPL servers. The always-up watch+serve shape is proven
     in production.

  The honest *remaining* cost of bb, now that the hallucinated ones are gone,
  is narrow: (a) the artifact is a script + the `bb` runtime + the fswatcher
  pod, not a single zero-runtime file you can `scp` to a bare box — which
  barely matters for a local tool installed via `brew`/`bbin`; and (b) the
  daemon's specific concurrency (N debounced watchers + a socket server + a
  serialized jj mutex) is more idiomatic with goroutines/channels than with
  bb's futures/atoms — surmountable, and with one design note: use a
  **localhost TCP socket** for the daemon, not a unix-domain socket, to
  sidestep bb's uncertain UDS support.

- **core.async settles the last open question.** The one place Go still led
  was the daemon's concurrency, and I'd vaguely pictured bb doing it with
  futures+atoms. It doesn't have to: **core.async is built into babashka**
  (namespace `clojure.core.async`, aliased `async`, no pod). The one caveat —
  bb's `go` macro maps to a real OS `thread` and its channel ops are blocking
  (`<!`→`<!!`), so go blocks don't *park* — is a non-issue at our scale: the
  daemon runs a bounded handful of threads (one jj consumer, one watcher feed
  per session, one socket loop), nowhere near the thousands-of-go-blocks
  regime where the lack of parking bites. And it maps our design *better*
  than a mutex would (see below and `docs/concurrency-notes.md`).

- **Go — considered and set aside.** Its real edges: a true single static
  binary (no runtime, no pod), trivial cross-compile, and goroutines. None is
  decisive for a *local, personal, brew-installed* dev tool whose author
  ships Clojure this way — and with core.async, the concurrency argument is
  neutral-to-bb. Go would only re-enter if this needed to run on arbitrary
  bare machines with nothing installed.

**Decided: Babashka.** Clojure's concurrency primitives are first-class and
its data handling is the right tool for a program whose whole job is parsing
and reshaping VCS state; the `clef` ecosystem and release pipeline are
already in hand. The recipes in §3–§8 are substrate-level (jj/tmux command
sequences) and were validated at the shell before any code — the language
choice sits above them.

**The concurrency shape (bb/core.async).** The plan's "serialize all jj
behind one mutex" (§7, F11) becomes, idiomatically: one `chan` is the jj work
queue; a single long-lived `thread` drains it and runs every `jj` invocation.
Serialization falls out of *single consumer* — there is no lock to hold or
forget, and FIFO ordering is guaranteed by the queue. fs-watch callbacks
`>!!` debounced snapshot requests onto per-session channels; a `thread` per
session (or one `alts!!` loop) folds them in; the client↔daemon socket accept
loop is its own `thread`. Everything that mutates the repo is funnelled
through the one jj channel, so the composite can never be written mid-rewrite.

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
recomputes them from the repo (megamerge parent list; conflict probes). The
store is authoritative only for things the repo can't know: intent text,
numbering, tmux window names, state-machine timestamps.

> **Data model notation.** The interfaces above are written as TypeScript for
> readability; the implementation is Clojure maps with namespaced keys
> (`:session/change-id`, `:session/state`, …). A `clojure.spec` for each is
> the real contract.

## 6. Module layout

```
compositor/
  bb.edn                       # deps, tasks (clef-style), bbin/bin, gen-script
  src/compositor/
    main.clj                   # entry: dispatch `comp <cmd>` (client) vs `comp _daemon`
    jj.clj                     # THE wrapper: single-consumer chan, --no-pager,
                               #   immutability config, -T templates → data via JSON
    tmux.clj                   # socket-scoped tmux driver (-L compositor)
    graph.clj                  # megamerge lifecycle: apply set → rebase -s; conflict probes
    session.clj                # create/keep/drop/toggle state machine
    daemon.clj                 # core.async loop, socket accept thread, watch debouncer
    reconcile.clj              # repo→(store, world) repair; undo, boot, crash recovery
    hooks.clj                  # agent-done hook provisioning per agent flavor
    store.clj                  # state persistence (EDN file for M0–M1; sqlite pod at M2)
    config.clj                 # project config load/validate (clojure.spec)
    tui.clj                    # M4 only: charm.clj list model (Bubble Tea for Clojure)
  test/compositor/             # bb test namespaces (unit) + integration.sh (jj/tmux)
  clef-style ./compositor      # the generated uberscript (gen-script output, committed)
  docs/
    SPEC.md  REVIEW.md  PLAN.md  concurrency-notes.md
    experiments/verify-jj-claims.sh
```

**Store note:** the plan (§5) specifies SQLite. For the M0–M1 spike the store
is a single EDN file behind a tiny protocol (`read-state`/`write-state`) — it
removes a runtime pod download from the critical path and the state model is
still in flux. It swaps to the `pod-babashka-go-sqlite3` pod at M2, when the
schema stabilizes, with no caller changes. The protocol boundary is the point.

The client/daemon split is one program that branches on the first arg:
user-facing `comp <cmd>` marshals a request over the socket; `comp _daemon`
is the long-running server tmux window 0 runs. Both are the same `main.clj`
entrypoint / generated uberscript.

Estimated size: ~1200–1600 lines of Clojure through M3 (bb is terser than the
Go sketch — data-first parsing and no struct boilerplate). The daemon loop
body stays well under 100 lines — everything hard is in `jj.clj` and
`reconcile.clj`.

**Runtime: track the latest babashka.** Pinned via `:min-bb-version` in
`bb.edn` (currently `1.12.218`, the latest). Rationale: the M4 TUI needs the
JLine3 that landed in 1.12.215, and there's no reason to lag — bb is a single
binary the user installs, so "latest" costs nothing and avoids version
archaeology later. Bump the pin when a newer bb ships.

**Dependencies** (the whole tree): `clojure.core.async` (built into bb, no
pod) for the daemon; the `org.babashka/fswatcher` pod for watching (as in
quickblog); at M2 the `pod-babashka-go-sqlite3` pod for state; at M4
`charm.clj` (a git/Clojars dep via `babashka.deps/add-deps`; no pod or extra
binary — bb's bundled JLine3 carries it). Client↔daemon link
is a **localhost TCP socket**
(not a unix-domain socket — bb's UDS support is uncertain, and localhost TCP
is trivially served). tmux and jj are invoked as subprocesses, never linked —
the intelligence lives in them, not in us.

> **Runtime pods need `github.com` at install time.** `fswatcher` and the
> sqlite pod are fetched from the pod registry (GitHub-hosted). On a locked-
> down network they must be pre-fetched/vendored. This does not affect M0–M1:
> the store is an EDN file, and M1's watch loop can start on a
> `babashka.fs`/polling watcher, upgrading to the fswatcher pod when
> available. Noted so it isn't a surprise in a restricted environment.

## 7. Milestones

**M0 — skeleton (half a day).** `jj.clj` wrapper + `tmux.clj` driver +
`store.clj` + `comp init` (including F3 config write and the F11 gitignore
check). Scaffold: `bb.edn` with tasks + `gen-script`.
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

**M4 — the status pane (1 day).** A charm.clj model/update/view in a tmux
pane: a scrollable session list (id, intent, state, applied toggle, collision
badge, "needs you" marker) that re-renders on daemon messages. Reads the same
data as `comp ls`; ~200 lines of Elm-shaped Clojure, no hand-rolled terminal
emulation. Nothing else.

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
