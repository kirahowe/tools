// The data model, straight from docs/command-centre-design.md.
//
// Everything that decays carries `touchedAt`; freshness is always derived from
// it, never stored. `updatedAt` is sync bookkeeping (last-write-wins) so that
// milestone 2 can add a transport without touching this file.

export type Horizon = "now" | "next" | "later" | "someday";

export const HORIZONS: Horizon[] = ["now", "next", "later", "someday"];

/** Decay stage, derived from age vs the level's clock. "ripe" = past the
 * clock and due to compost at the next sweep. */
export type Stage = "fresh" | "aging" | "stale" | "ripe";

/** Which decay clock applies to an item. */
export type Level = "inbox" | "task" | "project" | "area";

export interface BaseItem {
  id: string; // ULID, client-generated
  createdAt: number;
  touchedAt: number;
  updatedAt: number;
  compostedAt?: number;
}

export interface Area extends BaseItem {
  name: string;
  colour: string; // for scanability, nothing more
}

export interface Project extends BaseItem {
  areaId: string;
  name: string;
  doneWhen: string; // one line; forces "finite outcome" thinking
  horizon: Horizon;
}

export interface Task extends BaseItem {
  projectId?: string; // or attached straight to an area
  areaId?: string;
  title: string;
  note?: string;
  due?: string; // ISO date; presence makes it surface on the dashboard
  doneAt?: number;
  inbox?: boolean; // untriaged capture — fastest decay clock
  slate?: boolean; // pinned to the Now slate (capped)
}

export interface Settings {
  nowProjectCap: number;
  nowTaskCap: number;
  decayDays: Record<Level, number>;
  awayGapDays: number; // gap that triggers the "away?" offer
}

/** A pending "away?" offer: decay is computed but never executed while one of
 * these is unresolved — the system makes no moves behind your back. */
export interface AwayOffer {
  gapDays: number;
  detectedAt: number;
}

export interface Meta {
  lastHumanActivityAt: number;
  awayOffer?: AwayOffer | null;
}

export interface State {
  areas: Area[];
  projects: Project[];
  tasks: Task[];
  settings: Settings;
  meta: Meta;
}

export const DEFAULT_SETTINGS: Settings = {
  nowProjectCap: 3,
  nowTaskCap: 7,
  decayDays: { inbox: 7, task: 30, project: 180, area: 400 },
  awayGapDays: 4,
};

/* ---- mutations ------------------------------------------------------------
 * Every change to the state is one of these, applied through store.mutate().
 * The store keeps them in a log so milestone 2's sync can push the queue to
 * the Worker instead of rewriting the storage layer. */

export type Kind = "area" | "project" | "task";

export type MutationOp =
  | { op: "put"; kind: "area"; item: Area }
  | { op: "put"; kind: "project"; item: Project }
  | { op: "put"; kind: "task"; item: Task }
  | { op: "compost"; kind: Kind; id: string }
  | { op: "revive"; kind: Kind; id: string }
  | { op: "touch"; kind: Kind; id: string }
  | { op: "done"; id: string; done: boolean }
  | { op: "bump"; days: number }
  | { op: "settings"; settings: Settings };

export type Mutation = MutationOp & { mutId: string; at: number };
