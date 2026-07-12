# compositor

Manage many concurrent coding-agent sessions against one repo, and composite
all their in-flight work into **one live, running dev server**. Review by
*using the app*, not reading diffs. Version control (jujutsu) is the storage
engine and stays invisible.

See [`docs/SPEC.md`](docs/SPEC.md) for the vision, [`docs/REVIEW.md`](docs/REVIEW.md)
for the (evidence-backed) design review, [`docs/PLAN.md`](docs/PLAN.md) for the
implementation plan, and [`docs/concurrency-notes.md`](docs/concurrency-notes.md)
for the daemon's concurrency model.

> **Status: M0–M1 skeleton, reviewed but not yet run end-to-end.** Read
> [`TESTING.md`](TESTING.md) — the jj/tmux command sequences are executed and
> green; the babashka code around them has not been run in the build
> environment (no `bb` available there).

## Requirements

- [babashka](https://babashka.org) (`bb`) ≥ 1.3
- [jujutsu](https://github.com/jj-vcs/jj) (`jj`) ≥ 0.43
- `tmux` ≥ 3.4
- your agent CLI (`claude` / `codex`) and a dev server in the target project

## Try it

```
cd your-project
bb --config /path/to/compositor/bb.edn comp init      # from source
# or, once installed via bbin/Homebrew:
comp init
comp new "add a dark mode toggle"
comp ls
comp toggle 2        # the running app changes under your cursor
comp keep 1          # land it — the app doesn't flinch
```

## Develop

```
cd compositor
bb test          # unit tests (queue serialization, config, graph)
bb integration   # jj/tmux command-sequence test (needs jj + tmux)
bb gen-script    # emit the standalone ./compositor uberscript
```

## Layout

```
src/compositor/
  main.clj       CLI dispatch + init + client transport
  jj.clj         the single jj contract: templates→data, the serialization queue
  tmux.clj       socket-scoped tmux driver
  graph.clj      megamerge lifecycle (create once, rebase -s in place)
  session.clj    create / toggle / keep / drop
  daemon.clj     core.async loop: jj queue + watch + socket
  store.clj      session records (EDN now, sqlite pod at M2)
  config.clj     project config + paths
  gen_script.clj uberscript emitter
```
