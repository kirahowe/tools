// Freshness, always derived: age against the level's clock, with attention
// propagating upward — working a task renews its project, a live project
// keeps its area alive. The clocks are for things *nothing* has happened to.

import type { Area, Kind, Level, Project, Stage, State, Task } from "./types.js";
import { HORIZONS } from "./types.js";

const DAY = 86400000;

export function levelForTask(task: Task): Level {
  return task.inbox ? "inbox" : "task";
}

/** A task's own clock, unless it's done (done tasks don't decay — they're a
 * record, not a commitment). */
export function taskAge(task: Task, now: number): number {
  return now - task.touchedAt;
}

/** Effective touch of a project = its own or its liveliest open task's. */
export function projectTouchedAt(project: Project, state: State): number {
  let t = project.touchedAt;
  for (const task of state.tasks) {
    if (task.projectId === project.id && !task.compostedAt && task.touchedAt > t) t = task.touchedAt;
  }
  return t;
}

/** Effective touch of an area = its own or its liveliest project's or task's. */
export function areaTouchedAt(area: Area, state: State): number {
  let t = area.touchedAt;
  for (const p of state.projects) {
    if (p.areaId === area.id && !p.compostedAt) {
      const pt = projectTouchedAt(p, state);
      if (pt > t) t = pt;
    }
  }
  for (const task of state.tasks) {
    if (task.areaId === area.id && !task.compostedAt && task.touchedAt > t) t = task.touchedAt;
  }
  return t;
}

/** age / clock-length: 0 fresh, 1 due to compost. */
export function ageRatio(kind: Kind, item: Area | Project | Task, state: State, now: number): number {
  const s = state.settings;
  let level: Level;
  let touched: number;
  if (kind === "area") {
    level = "area";
    touched = areaTouchedAt(item as Area, state);
  } else if (kind === "project") {
    level = "project";
    touched = projectTouchedAt(item as Project, state);
  } else {
    const task = item as Task;
    if (task.doneAt) return 0;
    level = levelForTask(task);
    touched = task.touchedAt;
  }
  const days = Math.max(1, s.decayDays[level]);
  return (now - touched) / (days * DAY);
}

export function stageFor(ratio: number): Stage {
  if (ratio >= 1) return "ripe";
  if (ratio >= 0.8) return "stale";
  if (ratio >= 0.5) return "aging";
  return "fresh";
}

export function daysLeft(kind: Kind, item: Area | Project | Task, state: State, now: number): number {
  const s = state.settings;
  const level: Level = kind === "area" ? "area" : kind === "project" ? "project" : levelForTask(item as Task);
  const ratio = ageRatio(kind, item, state, now);
  return Math.ceil((1 - ratio) * s.decayDays[level]);
}

export function daysUntouched(kind: Kind, item: Area | Project | Task, state: State, now: number): number {
  const level: Level = kind === "area" ? "area" : kind === "project" ? "project" : levelForTask(item as Task);
  const ratio = ageRatio(kind, item, state, now);
  return Math.floor(ratio * state.settings.decayDays[level]);
}

/* ---- review queue -------------------------------------------------------- */

export interface ReviewEntry {
  kind: Kind;
  item: Area | Project | Task;
  ratio: number;
}

/** Everything aging or worse, nearest compost first — the review order. */
export function reviewQueue(state: State, now: number): ReviewEntry[] {
  const entries: ReviewEntry[] = [];
  const push = (kind: Kind, item: Area | Project | Task) => {
    if (item.compostedAt) return;
    if (kind === "task" && (item as Task).doneAt) return;
    const ratio = ageRatio(kind, item, state, now);
    if (ratio >= 0.5) entries.push({ kind, item, ratio });
  };
  for (const t of state.tasks) push("task", t);
  for (const p of state.projects) push("project", p);
  for (const a of state.areas) push("area", a);
  return entries.sort((x, y) => y.ratio - x.ratio);
}

/* ---- compost sweep ------------------------------------------------------- */

/** Items past their clock, ready to compost. Exceptions:
 *  - tasks with a future due date (dated items force attention instead)
 *  - done tasks (records don't decay)
 * The caller gates this behind the away offer: nothing composts until the
 * user has had the chance to say "I was just away". */
export function ripeItems(state: State, now: number): { kind: Kind; item: Area | Project | Task }[] {
  const out: { kind: Kind; item: Area | Project | Task }[] = [];
  for (const t of state.tasks) {
    if (t.compostedAt || t.doneAt) continue;
    if (t.due && new Date(t.due + "T23:59:59").getTime() >= now) continue;
    if (ageRatio("task", t, state, now) >= 1) out.push({ kind: "task", item: t });
  }
  for (const p of state.projects) {
    if (p.compostedAt) continue;
    if (ageRatio("project", p, state, now) >= 1) out.push({ kind: "project", item: p });
  }
  for (const a of state.areas) {
    if (a.compostedAt) continue;
    if (ageRatio("area", a, state, now) >= 1) out.push({ kind: "area", item: a });
  }
  return out;
}

/* ---- horizons ------------------------------------------------------------ */

export function demoteHorizon(h: Project["horizon"]): Project["horizon"] | null {
  const i = HORIZONS.indexOf(h);
  return i < HORIZONS.length - 1 ? HORIZONS[i + 1] : null;
}

/* ---- away detection ------------------------------------------------------ */

/** Days of full human absence, if it crosses the settings threshold. */
export function awayGapDays(state: State, now: number): number | null {
  const gap = Math.floor((now - state.meta.lastHumanActivityAt) / DAY);
  return gap >= state.settings.awayGapDays ? gap : null;
}
