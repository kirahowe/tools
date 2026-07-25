# Command centre — design notes

A place to think through what a "life command centre" should actually be before
writing any code. Manually populated at first; the interesting problems are not
data entry but *staleness* and *prioritization*.

## The problem, stated precisely

Every conventional productivity tool fails the same way, in the same order:

1. **Capture is free, curation is expensive.** Adding an item costs five
   seconds; deciding whether it still matters costs real thought. So the list
   grows monotonically.
2. **Tools assume permanence.** An item entered in March is presented in July
   with exactly the same confidence, even though priorities have shifted and
   half the projects have quietly changed shape underneath it.
3. **Priority is a static label on a moving target.** "P1" was true the day it
   was set. Nothing re-asks the question.
4. **The guilt pile kills the tool.** Once the list contains enough dead items,
   opening the app means wading through your own broken promises. So you stop
   opening it, and start a fresh list somewhere else. Repeat.

The failure is structural, not a matter of discipline. Any design that relies
on the user diligently pruning a growing list will meet the same end.

## Design principles

### 1. Staleness is the default fate, not a failure state

Invert the polarity: in a normal todo app an item lives forever unless you
kill it. Here, **an item expires unless you affirmatively keep it alive**.

Every item carries a `touchedAt` timestamp, refreshed implicitly whenever you
interact with it (edit, complete a subtask, check it off in a review). Items
that go untouched visibly age through stages — fresh → aging → stale — and
eventually **compost**: they drop out of every working view into an archive.
Nothing is ever deleted, and composted items can be revived in one tap, but
the *default* trajectory of anything you stop caring about is silent,
guilt-free disappearance. The list you look at is, by construction, the list
of things you have recently cared about.

This is the single most important idea in the tool. Everything else supports it.

Decay runs at the tempo of real life, which for projects is slow — things
legitimately span a year or more. The clocks per level:

| level    | composts after untouched for | why |
|----------|------------------------------|-----|
| inbox    | 7 days                       | early chance to drop things that seemed important but weren't |
| tasks    | 30 days                      | a next action nobody has taken in a month isn't next |
| projects | 6 months                     | long arcs are normal; renewal is cheap when you review |
| areas    | ~1 year+                     | near-fixed stars; decay here only flags a life chapter that has genuinely closed |

Renewal is cheap and implicit, so a year-long project stays alive simply by
being occasionally worked, reviewed, or glanced at in a review pass. The
clocks are for things *nothing* has happened to — not slow burns.

### 2. Prioritization by scarcity, not by scoring

Priority labels don't force trade-offs; caps do. The centre of the tool is a
**Now slate** with a small hard cap (say three projects and a handful of
tasks). Adding something to the slate when it's full means choosing what to
bump. That moment of displacement *is* the prioritization — the tool's job is
to make you have it, not to compute a ranking for you.

Below the slate, projects sit on coarse horizons: **now / next / later /
someday**. Moving between horizons is drag-simple. No numeric scores, no
eisenhower quadrants, no weighted formulas — those give the feeling of rigor
while going stale just as fast as the labels they replace. (A computed
urgency nudge from approaching due dates is fine; a computed *importance* is
not.)

### 3. The review is the heartbeat

Decay only works if there's a lightweight ritual for renewal. A **review
mode** presents one item at a time — starting with whatever is closest to
composting — and asks one question: *does this still matter?*

- **Renew** — refreshes `touchedAt`, item stays where it is
- **Demote** — push it down a horizon (now → next → later → someday)
- **Compost** — let it go, guilt-free, revivable from the archive

Three big buttons, one item at a time, done in two minutes. The dashboard
shows a gentle nudge when items are nearing compost, so the cadence is driven
by actual decay rather than a calendar obligation you can fail at. Skipping
reviews doesn't break anything — items just compost on their own, which is an
acceptable outcome by principle 1.

### 4. Assume good intent — never nag

Silence from the user is a signal about the user, not about the items. The
tool never shames: no red badges, no overdue counts, no "you have 47 stale
items", no notifications. The review nudge is an invitation, not an alarm.

Concretely, the system distinguishes *"you looked and didn't care"* from
*"you weren't looking"*:

- The system tracks the last **human** activity (any interaction from the UI —
  agent API calls don't count, see the API section).
- After a gap of more than a few days, the next visit opens with a one-tap
  offer instead of consequences: **"Away for 9 days? Bump everything by 9
  days"** — which shifts every `touchedAt` forward by the gap (or a chosen
  n), so nothing aged while you were on a beach.
- **Nothing composts during a gap.** Decay staging is computed live, but the
  compost transition is only *executed* once the return-offer has been shown
  and resolved. The system never makes moves behind your back; things fade,
  they don't vanish while you're away.

Declining the bump is also fine — maybe the time away *was* the verdict on
some of those items. The point is it's a choice, made once, cheaply.

### 5. Model life shallowly

Deep hierarchies are where items go to hide. Three levels, no more:

- **Areas** — ongoing responsibilities (health, home, work, a relationship).
  Areas decay on a ~year-plus clock: effectively permanent, but even a life
  area can genuinely end, and the system should notice rather than enshrine it.
- **Projects** — finite outcomes, each belonging to an area, each with a
  one-line *"done when…"* statement. Projects sit on a horizon and decay on
  the six-month clock.
- **Tasks** — next actions, attached to a project or directly to an area.
  Tasks decay fastest of the three.

Plus an **inbox** for raw capture: items land there untriaged, and the inbox
has the fastest decay in the system (a week). Triage it or it composts itself
— the inbox literally cannot accumulate.

### 6. The dashboard answers one question

"What should I be doing?" — not "what is everything I have ever considered
doing?". The main view shows:

- the Now slate (capped, principle 2)
- hard deadlines approaching, regardless of horizon (dated items are the one
  case where the tool, not the user, forces attention)
- a review nudge when things are decaying
- one-line quick capture into the inbox

Everything else — full project lists, the archive, per-area views — is a
click away, deliberately off the main screen.

## Data model (first cut)

```ts
type Horizon = "now" | "next" | "later" | "someday";

interface Area {
  id: string;              // ULID, client-generated
  name: string;
  colour: string;          // for scanability, nothing more
  createdAt: number;
  touchedAt: number;
  compostedAt?: number;
  updatedAt: number;       // sync bookkeeping (LWW)
}

interface Project {
  id: string;
  areaId: string;
  name: string;
  doneWhen: string;        // one line; forces "finite outcome" thinking
  horizon: Horizon;
  createdAt: number;
  touchedAt: number;
  compostedAt?: number;
  updatedAt: number;
}

interface Task {
  id: string;
  projectId?: string;      // or attached straight to an area
  areaId?: string;
  title: string;
  note?: string;
  due?: string;            // ISO date; presence makes it surface on the dashboard
  doneAt?: number;
  createdAt: number;
  touchedAt: number;
  compostedAt?: number;
  inbox?: boolean;         // untriaged capture
  slate?: boolean;         // pinned to the Now slate (capped, like now-projects)
  updatedAt: number;
}

interface Settings {
  nowProjectCap: number;   // default 3
  nowTaskCap: number;      // default 7
  decayDays: {             // days-until-compost per level
    inbox: number;         // default 7
    task: number;          // default 30
    project: number;       // default 180
    area: number;          // default 400
  };
  awayGapDays: number;     // gap that triggers the "away?" offer, default 4
}

interface ActivityMark {   // for away detection — human surfaces only
  lastHumanActivityAt: number;
}
```

Freshness is derived, never stored: `age = now - touchedAt`, mapped against
the relevant `decayDays` to a stage (fresh / aging / stale / composted).
Changing the decay settings retroactively re-stages everything, which is the
behaviour you'd want.

## Sync: local-first, two-plus devices, offline

localStorage alone doesn't survive contact with a second device. The shape:

- **Each client keeps a full local replica** (IndexedDB) and is fully usable
  offline — reads, writes, reviews, capture all work on a plane.
- **The Worker owns the authoritative copy** in Cloudflare **D1** (SQLite —
  the data is relational and small; KV's eventual consistency and lack of
  queries make it the wrong tool).
- Clients queue mutations while offline and push them when connectivity
  returns; they pull changes with a `?since=` cursor on `updatedAt`.
- **Conflict resolution is last-write-wins per record.** This is a
  single-user system with a handful of devices; the worst realistic conflict
  is editing the same task title on two devices in the same offline window.
  LWW loses nothing structural, and per-field merge machinery (CRDTs etc.)
  would be complexity spent on a problem this system barely has.
- IDs are client-generated ULIDs, so offline creation never needs the server
  and every mutation is idempotent (retry-safe by construction).

Auth: a single bearer token (this is one person's life, not a multi-tenant
product). The UI stores it after first entry; agents are given the same or a
second token (see below).

## The API is a first-class surface

Agents should be able to interact with the system deterministically — capture
into the inbox, read the slate, renew or demote items — through the same
Worker that serves sync. The UI is just another client of this API.

Sketch, mounted under `/command/api/`:

| route | verb | purpose |
|---|---|---|
| `/state` | GET | full snapshot (also the export format) |
| `/changes?since=` | GET | incremental pull for sync |
| `/areas`, `/projects`, `/tasks` | GET/POST/PATCH | CRUD; PATCH is partial, PUT-by-ULID makes creates idempotent |
| `/inbox` | POST | quick capture — the primary agent entry point |
| `/now` | GET | the slate + upcoming due dates — "what matters right now" |
| `/touch/:id` | POST | renew an item |
| `/review/next` | GET | items nearest composting, in review order |
| `/bump` | POST | `{days}` — shift all `touchedAt` forward (the away offer, also invocable directly) |

Deterministic means: plain JSON in and out, client-supplied IDs, idempotent
writes, no server-side magic that reorders or reinterprets — an agent that
replays a request gets the same end state.

**Agent actions carry an `actor` distinction** (separate token or an
`X-Actor: agent` marker), with two consequences:

1. Agent activity does **not** count as human activity for away-detection —
   an agent filing things into the inbox all week doesn't mask a vacation.
2. Whether an agent write refreshes an item's `touchedAt` is a policy choice,
   not an accident. Default: agent *captures* are fresh (they're new), but
   agent edits to existing items do **not** renew them — freshness measures
   *your* attention, and an agent shouldn't be able to keep an item alive
   that you've stopped caring about. Revisit if this proves too strict.

## Where it lives

A new tool in this repo — `tools/command/`, mounted at `/command` — following
the annotate pattern for the client (base-path-aware TypeScript, PWA so it
installs to a phone home screen; quick capture only earns its keep if it's
two taps away). Unlike annotate, the Worker gains real routes
(`/command/api/…`) and a D1 binding.

That nudges against the repo philosophy — a tool with its own database is
most of the way to "heavier backend, graduates to its own repo". The call:
start here anyway. One D1 database and a few Worker routes don't change the
release lifecycle (everything still deploys together), and moving a
Worker+D1 app out later is mechanical. Graduation is triggered by the tool
proving itself, not preempted.

## Build order

Milestone 1 — the mechanics, single device:

1. Areas / projects / tasks CRUD with horizons and the inbox
2. Decay staging + visual aging + compost/revive, archive
3. Now slate with caps and displacement (adding past the cap asks what to bump)
4. Review mode (renew / demote / compost, one item at a time)
5. Dashboard (slate, due dates, review nudge, quick capture)
6. Away detection + the bump offer
7. JSON export/import

Built from day one on a mutation-log storage layer (every change is a queued
mutation applied to the local replica), so milestone 2 adds a transport, not
a rewrite. (M1 persists the replica to localStorage — the data is tiny; the
IndexedDB move can ride along with sync if quota ever becomes real.)

**Status: milestone 1 is built** — see [tools/command](../tools/command).
Two design details settled during the build: attention propagates upward
(touching a task renews its project, a live project keeps its area alive),
and tasks with a future due date are exempt from auto-compost while the date
is ahead — dated items force attention instead of quietly disappearing.

Milestone 2 — the surface area:

8. Worker API routes + D1 schema, bearer-token auth
9. Client sync (push queue, `?since=` pull, LWW)
10. Actor distinction + agent policy above

Deliberately not planned: recurring tasks, notifications, calendar
integration, any automation of importance. Manual population is fine — the
hypothesis under test is that *decay + scarcity + cheap reviews* keeps a
manually-fed system alive, which no amount of integration can substitute for.

## Open questions

- **Cap sizes.** Is three now-projects right, or does real life need four?
- **Should completing feel different from composting?** Probably yes — a
  small "done" log gives the dashboard a rear-view mirror, which matters for
  morale in a tool whose whole aesthetic is letting things go.
- **Phone vs desktop as primary surface.** Capture is phone-shaped; reviews
  could be either; the dashboard is probably desktop-shaped. Which one drives
  the layout?
- **Snooze.** Is "renew" enough, or do dated snoozes ("ask me again in
  October") earn a place? Risk: snooze is how items hide from decay.
- **Agent renewal policy.** Is "agent edits don't refresh freshness" right,
  or should some agent actions (e.g. marking a task done on your behalf)
  count as attention?
- **One token or two.** Same bearer token for UI and agents, or a separate
  agent token so the actor distinction can't be spoofed by accident?
