// Mutation-log storage: every change is a Mutation applied to an in-memory
// replica, appended to a log, and persisted. Milestone 2 adds sync by pushing
// the log to the Worker and pulling changes — a transport, not a rewrite.
//
// Milestone 1 persists to localStorage (the whole state is a few tens of KB;
// the replica moves to IndexedDB if quota ever becomes real).

import type { Area, Kind, Meta, Mutation, MutationOp, Project, Settings, State, Task } from "./types.js";
import { DEFAULT_SETTINGS } from "./types.js";
import { ulid } from "./ulid.js";

const STATE_KEY = "command:state";
const LOG_KEY = "command:log";
const LOG_CAP = 1000; // until sync exists, the log is bounded history, not a queue

export let state: State = emptyState();

function emptyState(): State {
  return {
    areas: [],
    projects: [],
    tasks: [],
    settings: { ...DEFAULT_SETTINGS, decayDays: { ...DEFAULT_SETTINGS.decayDays } },
    meta: { lastHumanActivityAt: Date.now() },
  };
}

export function load(): void {
  const raw = localStorage.getItem(STATE_KEY);
  if (!raw) return;
  try {
    const parsed = JSON.parse(raw) as State;
    state = {
      ...emptyState(),
      ...parsed,
      settings: {
        ...DEFAULT_SETTINGS,
        ...parsed.settings,
        decayDays: { ...DEFAULT_SETTINGS.decayDays, ...parsed.settings?.decayDays },
      },
      meta: parsed.meta ?? { lastHumanActivityAt: Date.now() },
    };
  } catch {
    /* corrupt state: start empty rather than crash; the log may still help */
  }
}

function persist(): void {
  try {
    localStorage.setItem(STATE_KEY, JSON.stringify(state));
  } catch {
    /* quota — nothing sensible to drop; state stays in memory this session */
  }
}

function appendLog(mut: Mutation): void {
  try {
    const raw = localStorage.getItem(LOG_KEY);
    const log = raw ? (JSON.parse(raw) as Mutation[]) : [];
    log.push(mut);
    localStorage.setItem(LOG_KEY, JSON.stringify(log.slice(-LOG_CAP)));
  } catch {
    /* the log is an audit trail until sync lands; losing entries is safe */
  }
}

function collection(kind: Kind): (Area | Project | Task)[] {
  return kind === "area" ? state.areas : kind === "project" ? state.projects : state.tasks;
}

function find(kind: Kind, id: string): Area | Project | Task | undefined {
  return collection(kind).find((i) => i.id === id);
}

function apply(mut: Mutation): void {
  const at = mut.at;
  switch (mut.op) {
    case "put": {
      const items = collection(mut.kind);
      const item = { ...mut.item, updatedAt: at };
      const idx = items.findIndex((i) => i.id === item.id);
      if (idx >= 0) items[idx] = item;
      else items.push(item);
      break;
    }
    case "compost": {
      const item = find(mut.kind, mut.id);
      if (item) {
        item.compostedAt = at;
        item.updatedAt = at;
      }
      break;
    }
    case "revive": {
      const item = find(mut.kind, mut.id);
      if (item) {
        delete item.compostedAt;
        item.touchedAt = at; // revival is the strongest possible renewal
        item.updatedAt = at;
      }
      break;
    }
    case "touch": {
      const item = find(mut.kind, mut.id);
      if (item) {
        item.touchedAt = at;
        item.updatedAt = at;
      }
      break;
    }
    case "done": {
      const task = state.tasks.find((t) => t.id === mut.id);
      if (task) {
        if (mut.done) task.doneAt = at;
        else delete task.doneAt;
        task.touchedAt = at;
        task.updatedAt = at;
      }
      break;
    }
    case "bump": {
      // Shift every live clock forward: nothing aged while you were away.
      const ms = mut.days * 86400000;
      for (const item of [...state.areas, ...state.projects, ...state.tasks]) {
        if (item.compostedAt) continue;
        item.touchedAt = Math.min(item.touchedAt + ms, at);
        item.updatedAt = at;
      }
      break;
    }
    case "settings": {
      state.settings = mut.settings;
      break;
    }
  }
}

export interface MutateOptions {
  /** Agent/system actions don't count as human attention (away detection). */
  human?: boolean;
}

export function mutate(op: MutationOp, opts: MutateOptions = {}): void {
  const mut: Mutation = { ...op, mutId: ulid(), at: Date.now() };
  apply(mut);
  if (opts.human !== false) state.meta.lastHumanActivityAt = mut.at;
  appendLog(mut);
  persist();
}

/** Meta (activity marks, the away offer) is device-local state, not synced
 * data — it changes without going through the mutation log. */
export function setMeta(patch: Partial<Meta>): void {
  state.meta = { ...state.meta, ...patch };
  persist();
}

export function markHumanActivity(): void {
  setMeta({ lastHumanActivityAt: Date.now() });
}

/* ---- export / import ---------------------------------------------------- */

export function exportJson(): string {
  const { areas, projects, tasks, settings } = state;
  return JSON.stringify({ format: "command-centre", version: 1, exportedAt: new Date().toISOString(), areas, projects, tasks, settings }, null, 2);
}

export function importJson(raw: string): { areas: number; projects: number; tasks: number } {
  const parsed = JSON.parse(raw) as Partial<State> & { format?: string };
  if (parsed.format !== "command-centre") throw new Error("Not a command-centre export");
  const counts = { areas: 0, projects: 0, tasks: 0 };
  for (const a of parsed.areas ?? []) {
    mutate({ op: "put", kind: "area", item: a as Area });
    counts.areas++;
  }
  for (const p of parsed.projects ?? []) {
    mutate({ op: "put", kind: "project", item: p as Project });
    counts.projects++;
  }
  for (const t of parsed.tasks ?? []) {
    mutate({ op: "put", kind: "task", item: t as Task });
    counts.tasks++;
  }
  if (parsed.settings) mutate({ op: "settings", settings: { ...DEFAULT_SETTINGS, ...parsed.settings } as Settings });
  return counts;
}

export function resetAll(): void {
  localStorage.removeItem(STATE_KEY);
  localStorage.removeItem(LOG_KEY);
  state = emptyState();
}
