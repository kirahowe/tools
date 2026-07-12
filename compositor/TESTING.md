# Testing status — read this before trusting the code

Compositor is being built in an environment where **babashka cannot be
installed** (its only binary distribution is GitHub Releases, which the egress
policy here blocks; `jj` came from crates.io and `tmux` from apt, so those are
present). That shapes what has and hasn't been executed. Being explicit:

## What IS verified (executed, green)

| Layer | How | Where |
|---|---|---|
| The jj data-model claims the whole design rests on | `jj 0.43.0`, 16 assertions | `docs/experiments/verify-jj-claims.sh` |
| The exact jj/tmux **command sequences** the bb code issues (init → new×2 → snapshot → rebuild → materialize → toggle → keep → conflict) | `jj 0.43.0` + `tmux 3.4` | `compositor/test/integration.sh` |
| **M1 success criterion** — two sessions edit different files, both appear in the composite, toggling one off removes it | part of `integration.sh` | ✅ passes |
| keep is a **byte-identical** visual no-op | sha256 compare in `integration.sh` | ✅ passes |
| Every `.clj` namespace has balanced delimiters | Clojure-aware balance checker | ✅ all ok |

The riskiest part of this tool is the VCS orchestration — getting the jj
command sequences exactly right. That part is executed and green. `integration.sh`
mirrors `session.clj`/`graph.clj` op-for-op, so a passing run means the
substrate usage is correct even though the Clojure wrapper around it was not run
here.

## What is NOT yet executed (you must run locally)

- **The babashka code itself.** No `bb` in this environment ⇒ `bb test` and any
  live `comp` command are unrun. The code is written to be idiomatic and was
  read carefully, but it has not been compiled. Treat M0/M1 as "reviewed, not
  run" until you do:

  ```
  cd compositor
  bb test            # unit tests: queue serialization, config validation, graph parent-set
  bb integration     # the shell test above (needs jj + tmux)
  ```

- **The full M1 loop with a real agent + dev server.** Needs `claude`/`codex`
  and a dev server present; inherently interactive. `bb comp init` in a sample
  Vite app is the first thing to try.

## Known gaps / TODO before this is real

- `comp init` sets tmux window 0 to run `comp _daemon`, which assumes `comp` is
  on PATH (the bbin/Homebrew install). From a source checkout, that command
  should be `bb comp _daemon` in the `compositor/` dir — wire this to the actual
  install path.
- Runtime pods (`fswatcher`, sqlite) fetch from GitHub at first use; the M1
  watcher deliberately uses a polling `fingerprint` so it needs no pod.
- `reconcile()` (review F10) is stubbed as a comment in `daemon/start` — it is
  M2 work and daemon-boot crash-recovery depends on it.
