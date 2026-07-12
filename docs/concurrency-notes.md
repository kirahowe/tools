# Concurrency: goroutines & mutexes vs Clojure/core.async

Written as reference for the compositor daemon, but it's a general
compare-and-contrast. The daemon is the ideal teaching example because it needs
exactly the primitives in question: several producers (file watchers, a socket
server) feeding one resource that must be touched by one thing at a time (the
`jj` CLI against one repo).

## 1. The problem both languages are solving

The daemon has concurrent *sources of work*:

- a watcher per session workspace, firing when an agent saves a file,
- a socket accept loop, firing when the human runs `comp toggle 2`.

They all end in the same place: **run a `jj` command that rewrites the repo**.
Two `jj` rewrites at once against one repo interleave operations and can write a
half-built composite to disk. So the requirement is: *many producers, but
repo-mutating work happens one at a time, in order.* Every concurrency tool
below is just a different answer to that one requirement.

---

## 2. Go's answer

### Goroutines
A goroutine is a function scheduled onto a small pool of OS threads by the Go
runtime. `go f()` starts one. They're cheap (a few KB of stack, grown on
demand), so "thousands of them" is normal. The runtime **parks** a goroutine
that blocks on I/O or a channel — it's lifted off its OS thread so the thread
can run another goroutine. That parking is why Go scales to huge numbers of
concurrent-but-mostly-waiting tasks. Hold onto this word *park*; it's the exact
thing babashka does differently (§4).

### Channels
A typed conduit: `ch := make(chan Job)`. `ch <- job` sends (blocks if full),
`job := <-ch` receives (blocks if empty). Channels are how goroutines
coordinate without sharing memory — Go's slogan is *"don't communicate by
sharing memory; share memory by communicating."*

The daemon in Go, the idiomatic way — **one worker owns the repo**:

```go
jobs := make(chan func(), 1024)
go func() {                 // the single jj worker
    for job := range jobs { // one at a time, in arrival order
        job()               // runs a jj command
    }
}()
// any goroutine, from anywhere:
jobs <- func() { runJJ("rebase", "-s", mm, ...) }
```

No lock anywhere. Serialization is a *consequence of there being one receiver*.
This is the pattern the compositor uses — see §5.

### The mutex (the other answer)
`sync.Mutex` guards shared memory directly:

```go
var mu sync.Mutex
mu.Lock()
// ... only one goroutine in here at a time ...
mu.Unlock()
```

A mutex protects a *region of code / piece of state*. Whoever holds the lock
proceeds; everyone else blocks at `Lock()` until it's released. It's correct and
fast, but it's **advisory discipline**: nothing stops code from touching the
guarded state without taking the lock, and a path that forgets to `Unlock`
(e.g. an early return before `defer mu.Unlock()`) deadlocks everything behind it.
Mutexes also don't impose an *order* — whoever the scheduler wakes next wins, so
you get mutual exclusion but not FIFO fairness.

**Mutex vs worker-channel** — two ways to get "one at a time":
- *Mutex*: N goroutines each run the work themselves, taking turns holding a
  lock. Shared state, guarded.
- *Worker channel*: N goroutines hand the work to 1 goroutine that runs it.
  No shared state; the single consumer *is* the exclusion, and the queue gives
  you ordering for free.

For "all repo mutations must be one-at-a-time and in order," the worker channel
is the better fit — which is why the compositor uses it in both languages.

---

## 3. Clojure's answer — a broader toolbox

Clojure's headline concurrency claim is about **state**, not threads: values are
immutable, so "sharing" data across threads is safe by default — there's nothing
to corrupt. You only need coordination for the genuinely mutable bits, and
Clojure gives you four *reference types*, chosen by the semantics you need:

| Type | For | Coordinated? | Sync? |
|---|---|---|---|
| **atom** | one independent piece of state | no | synchronous |
| **ref** (STM) | several pieces that must change together | yes (transactions) | synchronous |
| **agent** | one piece, updated in the background | no | asynchronous |
| **var** (binding) | per-thread dynamic scope | — | — |

### atom — the workhorse
An atom holds one immutable value; you swap it with a pure function:

```clojure
(def counter (atom 0))
(swap! counter inc)     ; atomic compare-and-set retry loop, lock-free
```

`swap!` applies your function to the current value and CAS-installs the result,
retrying if another thread beat it. No locks, no forgetting to unlock — the
worst case is `swap!` runs your function twice under contention, which is why
the function **must be pure**. This is the closest Clojure analogue to "a mutex
around a variable," but it's *optimistic* (retry) rather than *pessimistic*
(block), and the discipline (purity) is enforceable in a way "remember to lock"
is not. The compositor's store is an atom (`store.clj`).

### ref / STM — when several things must move together
If you must change two structures atomically (the classic bank-transfer), refs
in a `dosync` transaction give you that with no explicit lock ordering — the STM
runtime handles it and retries on conflict. The daemon doesn't need this; it's
the tool you reach for when *one* atom can't express "these must be consistent
with each other."

### agent — fire-and-forget serialized updates
`(send some-agent f)` queues `f` to run on a thread pool, one action at a time,
in order. That is *itself* a serialized worker — an agent is essentially the
worker-channel pattern with the queue hidden. Worth knowing because our jj queue
could have been an agent; we use core.async instead for explicit control and
because the daemon already lives in a channel world.

### core.async — channels & go blocks, ported from Go/CSP
core.async deliberately brings Go's model to Clojure:

- `(chan)`, `(chan 1024)` — unbuffered / buffered channels.
- `(>!! ch x)` / `(<!! ch)` — **blocking** put/take (two bangs), for real
  threads.
- `(>! ch x)` / `(<! ch)` — **parking** put/take (one bang), only legal inside a
  `(go ...)` block.
- `(go ...)` — on the JVM, runs the body on a shared thread pool and *parks* at
  `<!`/`>!`, exactly like a goroutine. `(thread ...)` runs the body on a real
  dedicated thread and returns a channel with its result.
- `(alts!! [ch (timeout 500)])` — take from whichever is ready first; this is
  how you express "debounce" or "wait, but not forever."

So the Go worker-channel translates almost line for line:

```clojure
(def jobs (chan 1024))
(thread                          ; the single jj worker (real thread)
  (loop []
    (when-let [job (<!! jobs)]    ; one at a time, FIFO
      (job)
      (recur))))
;; from anywhere:
(>!! jobs #(run-jj "rebase" "-s" mm ...))
```

---

## 4. The one real difference: babashka doesn't park

Standard Clojure/JVM core.async `go` blocks park like goroutines. **Babashka's
`go` does not** — bb maps `go` onto `clojure.core.async/thread` (a real OS
thread) and its one-bang ops onto the blocking two-bang ops. Concretely, in bb:

- `(go ...)` ≈ `(thread ...)` — a real thread, not a parked lightweight task.
- `(<! ch)` inside it behaves like `(<!! ch)`.

Why it matters, and why it doesn't matter *here*:

- **Where it would bite:** the classic core.async flex of spawning 10,000 `go`
  blocks multiplexed onto a handful of threads. In bb that's 10,000 OS threads;
  it falls over. Parking is what makes the JVM version cheap, and bb gave it up
  to stay a fast-booting single binary.
- **Why the daemon is fine:** we spawn a *bounded, tiny* number of long-lived
  threads — one jj worker, one watcher feed, one socket accept loop, plus a
  short-lived thread per in-flight client request. That's single digits to low
  double digits. Real threads are completely comfortable there; parking buys us
  nothing. So we write with `thread` + blocking ops (`<!!`/`>!!`) deliberately,
  which also makes the real-thread cost visible at the call site instead of
  hidden behind a `go`.

Rule of thumb for bb: **use `thread` and blocking ops; treat `go` as
compatibility sugar, and never rely on go blocks being cheap.**

---

## 5. What the compositor actually does

`jj.clj`'s `queue` is the whole synchronization story:

```clojure
(defn queue []
  (let [ch (async/chan 1024)]
    (async/thread                          ; ONE consumer thread
      (loop []
        (when-let [[thunk p] (async/<!! ch)]
          (deliver p (try {:val (thunk)} (catch Throwable t {:ex t})))
          (recur))))
    {:submit (fn [thunk]                    ; called from watcher / socket threads
               (let [p (promise)]
                 (async/>!! ch [thunk p])   ; hand work to the one consumer
                 (let [{:keys [val ex]} @p] ; block until it ran; get result back
                   (if ex (throw ex) val))))
     :stop  #(async/close! ch)}))
```

Read it against the requirement in §1:

- **Many producers:** every watcher thread and every socket-handler thread calls
  `submit`. They can call it concurrently; the channel serializes the handoff.
- **One-at-a-time, in order:** there is exactly one consumer thread taking from
  `ch`, so the thunks (each a `jj` command) run one at a time, FIFO. *No lock
  exists* — mutual exclusion is a property of single-consumer, not of a guard we
  remembered to take. There's no `Unlock` to forget, no lock-ordering to get
  wrong, no way for "some other code path" to run jj without going through the
  channel, because the channel is the only path the daemon offers.
- **Results flow back:** the `promise` turns an async handoff into a synchronous
  call for the producer — it blocks on `@p` until its thunk has run and gets the
  return value or the exception rethrown on its own thread. This is the piece a
  bare mutex doesn't give you: with a mutex you'd run the work on the calling
  thread; here the work runs *elsewhere* but the caller still reads like a normal
  function call.

The one discipline: a thunk (running on the consumer thread) must never call
`submit` — that would be the consumer waiting on itself, a deadlock. So outer
threads submit; the submitted thunk does the whole operation directly. That's the
bb analogue of "don't call `Lock()` while you already hold it."

### Mapping the whole daemon

| Concern | Go | Compositor (bb/core.async) |
|---|---|---|
| Lightweight task | goroutine (`go f()`) | `(thread ...)` — real thread, bounded count |
| Coordination conduit | `chan` | `clojure.core.async/chan` |
| Serialize repo writes | single worker goroutine `for range ch` | single `thread` draining the jj `chan` |
| Get a result back | reply channel per job | a `promise` per job |
| "Wait, but debounce" | `select { case <-ch: ; case <-time.After(d): }` | `(alts!! [ch (timeout d)])` |
| Shared counter/state | `sync.Mutex` + variable, or worker | `atom` + `swap!` (lock-free) |
| Multi-structure atomic update | mutex(es) with careful ordering | `ref`s in `dosync` (STM) |

## 6. Takeaways

1. "One at a time" has two shapes: **guard the state** (mutex / atom) or **funnel
   the work to one worker** (channel + single consumer). For "all repo mutations,
   ordered," the worker wins — no lock to forget, ordering for free.
2. core.async is Go's channels-and-goroutines, deliberately ported — the worker
   pattern is nearly identical source.
3. Clojure adds a *state* story Go doesn't have: immutability-by-default plus
   atoms/refs/agents, so most "shared state" needs no coordination at all, and
   the parts that do pick a reference type by semantics.
4. babashka's caveat is real but narrow: no parking, so `go` blocks are real
   threads — use `thread` + blocking ops and keep the thread count bounded. At
   the daemon's scale that's not a compromise, it's just the honest primitive.
