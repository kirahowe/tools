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

### 4. Model life shallowly

Deep hierarchies are where items go to hide. Three levels, no more:

- **Areas** — ongoing responsibilities that are never "done" (health, home,
  work, a relationship). Areas don't decay; they're the fixed stars.
- **Projects** — finite outcomes, each belonging to an area, each with a
  one-line *"done when…"* statement. Projects sit on a horizon and decay.
- **Tasks** — next actions, attached to a project or directly to an area.
  Tasks decay fastest of all.

Plus an **inbox** for raw capture: items land there untriaged, and the inbox
has the fastest decay in the system. Triage it or it composts itself — the
inbox literally cannot accumulate.

### 5. The dashboard answers one question

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
  id: string;
  name: string;
  colour: string;          // for scanability, nothing more
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
}

interface Settings {
  nowProjectCap: number;   // default 3
  nowTaskCap: number;      // default 7
  decayDays: {             // days-until-compost per context
    inbox: number;         // default 7
    task: number;          // default 30
    project: number;       // default 60
  };
}
```

Freshness is derived, never stored: `age = now - touchedAt`, mapped against
the relevant `decayDays` to a stage (fresh / aging / stale / composted).
Changing the decay settings retroactively re-stages everything, which is the
behaviour you'd want.

## Where it lives

A new tool in this repo — `tools/command/`, mounted at `/command` — following
the annotate pattern:

- **Local-first, no backend.** State in localStorage (with JSON export/import
  from day one, both as backup and as the escape hatch every tool should
  offer). No account, no sync, no server state. The Worker gains no routes.
- **PWA**, like annotate, so it installs to a phone home screen — quick
  capture only earns its keep if it's two taps away.
- Sync across devices is explicitly deferred. If the tool proves itself, that
  is the moment it may graduate per the repo philosophy (its own repo, a real
  backend, D1 or KV). Building sync first is how prototypes die.

## MVP cut

1. Areas / projects / tasks CRUD with horizons and the inbox
2. Decay staging + visual aging + auto-compost, archive with revive
3. Now slate with caps and displacement (adding past the cap asks what to bump)
4. Review mode (renew / demote / compost, one item at a time)
5. Dashboard (slate, due dates, review nudge, quick capture)
6. JSON export/import

Deliberately not in the MVP: recurring tasks, reminders/notifications, sync,
calendar integration, any automation of importance. Manual population is fine
— the hypothesis under test is that *decay + scarcity + cheap reviews* keeps a
manually-fed system alive, which no amount of integration can substitute for.

## Open questions

- **Decay rates.** Are 7/30/60 days (inbox/task/project) the right defaults?
  Too fast and renewal becomes a chore; too slow and staleness creeps back.
- **Cap sizes.** Is three now-projects right, or does real life need four?
- **Should completing feel different from composting?** Probably yes — a
  small "done" log gives the dashboard a rear-view mirror, which matters for
  morale in a tool whose whole aesthetic is letting things go.
- **Phone vs desktop as primary surface.** Capture is phone-shaped; reviews
  could be either; the dashboard is probably desktop-shaped. Which one drives
  the layout?
- **Snooze.** Is "renew" enough, or do dated snoozes ("ask me again in
  October") earn a place? Risk: snooze is how items hide from decay.
