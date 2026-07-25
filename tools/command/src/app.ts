// Command centre UI. All state changes go through store.mutate(); rendering is
// a full re-render of the active view (the data is a few hundred items at most).

import type { Area, Horizon, Kind, Project, Task } from "./types.js";
import { DEFAULT_SETTINGS, HORIZONS } from "./types.js";
import * as store from "./store.js";
import { ulid } from "./ulid.js";
import {
  ageRatio,
  awayGapDays,
  daysLeft,
  daysUntouched,
  demoteHorizon,
  reviewQueue,
  ripeItems,
  stageFor,
  type ReviewEntry,
} from "./decay.js";

const $ = <T extends HTMLElement = HTMLElement>(id: string): T => document.getElementById(id) as T;

const SWATCHES = ["#2f7a5d", "#4b6fce", "#b0532e", "#7a2f6b", "#2e7ab0", "#8a8a2e", "#c0392b", "#5d5d66"];

const HORIZON_LABEL: Record<Horizon, string> = { now: "Now", next: "Next", later: "Later", someday: "Someday" };

type View = "now" | "projects" | "archive";
let view: View = "now";

/* ============================== helpers ================================== */

function esc(s: string): string {
  return s.replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c] as string);
}

let toastTimer: number | undefined;
function toast(msg: string): void {
  const el = $("status");
  el.textContent = msg;
  el.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => el.classList.remove("show"), 2600);
}

function area(id: string | undefined): Area | undefined {
  return store.state.areas.find((a) => a.id === id);
}
function project(id: string | undefined): Project | undefined {
  return store.state.projects.find((p) => p.id === id);
}
function areaOfTask(t: Task): Area | undefined {
  return t.projectId ? area(project(t.projectId)?.areaId) : area(t.areaId);
}

function liveProjects(): Project[] {
  return store.state.projects.filter((p) => !p.compostedAt);
}
function liveTasks(): Task[] {
  return store.state.tasks.filter((t) => !t.compostedAt);
}
function openTasks(): Task[] {
  return liveTasks().filter((t) => !t.doneAt);
}
function nowProjects(): Project[] {
  return liveProjects().filter((p) => p.horizon === "now");
}
function slateTasks(): Task[] {
  return openTasks().filter((t) => t.slate && !t.inbox);
}

function stageAttrs(kind: Kind, item: Area | Project | Task): { cls: string; life: string } {
  const now = Date.now();
  const ratio = Math.min(1, ageRatio(kind, item, store.state, now));
  const stage = stageFor(ratio);
  const left = Math.max(0, Math.round((1 - ratio) * 100));
  return { cls: `stage-${stage}`, life: `<span class="life" title="${left}% life left"><i style="width:${left}%"></i></span>` };
}

function dot(a: Area | undefined): string {
  return a ? `<span class="dot" style="background:${esc(a.colour)}" title="${esc(a.name)}"></span>` : "";
}

function fmtDue(due: string): string {
  const d = new Date(due + "T00:00:00");
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const days = Math.round((d.getTime() - today.getTime()) / 86400000);
  if (days < 0) return `${-days}d overdue`;
  if (days === 0) return "today";
  if (days === 1) return "tomorrow";
  return `in ${days}d`;
}

/* ============================== rendering ================================ */

function render(): void {
  $("viewNow").hidden = view !== "now";
  $("viewProjects").hidden = view !== "projects";
  $("viewArchive").hidden = view !== "archive";
  $("tabNow").classList.toggle("active", view === "now");
  $("tabProjects").classList.toggle("active", view === "projects");
  $("tabArchive").classList.toggle("active", view === "archive");

  renderReviewButton();
  if (view === "now") renderNow();
  else if (view === "projects") renderProjects();
  else renderArchive();
}

function renderReviewButton(): void {
  const n = reviewQueue(store.state, Date.now()).length;
  const btn = $("reviewBtn");
  btn.hidden = n === 0;
  $("reviewCount").textContent = String(n);
}

function renderNow(): void {
  const state = store.state;
  const now = Date.now();

  const isEmpty = state.areas.length === 0 && state.projects.length === 0 && state.tasks.length === 0;
  $("emptyNow").hidden = !isEmpty;

  // Inbox
  const inbox = openTasks()
    .filter((t) => t.inbox)
    .sort((a, b) => a.createdAt - b.createdAt);
  $("inboxSection").hidden = inbox.length === 0;
  $("inboxCount").textContent = String(inbox.length);
  $<HTMLUListElement>("inboxList").innerHTML = inbox
    .map((t) => {
      const s = stageAttrs("task", t);
      const left = daysLeft("task", t, state, now);
      return `<li class="${s.cls}" data-id="${t.id}">
        <span class="row-title">${esc(t.title)}</span>
        <span class="row-sub">${left > 0 ? `composts in ${left}d` : "composting"}</span>
        <span class="row-actions">
          <button class="link-btn" data-act="triage" type="button">Triage</button>
          <button class="link-btn quiet" data-act="compost-task" type="button">Compost</button>
        </span>
      </li>`;
    })
    .join("");

  // Review nudge — an invitation, not an alarm
  const q = reviewQueue(state, now);
  const nudge = $("reviewNudge");
  nudge.hidden = q.length === 0;
  if (q.length > 0) {
    nudge.innerHTML = `<span>${q.length} item${q.length === 1 ? " is" : "s are"} fading — a two-minute review keeps what still matters.</span>
      <button class="btn ghost sm" data-act="open-review" type="button">Review</button>`;
  }

  // Coming up: dated tasks in the next 14 days (or overdue)
  const soon = openTasks()
    .filter((t) => t.due && new Date(t.due + "T00:00:00").getTime() - now < 14 * 86400000)
    .sort((a, b) => (a.due! < b.due! ? -1 : 1));
  $("dueSection").hidden = soon.length === 0;
  $<HTMLUListElement>("dueList").innerHTML = soon
    .map((t) => {
      const where = t.projectId ? project(t.projectId)?.name : areaOfTask(t)?.name;
      return `<li data-id="${t.id}">
        <input class="check" type="checkbox" data-act="toggle-done" aria-label="Done" />
        <span class="row-title">${esc(t.title)}${where ? `<span class="where">${esc(where)}</span>` : ""}</span>
        <span class="row-sub">${fmtDue(t.due!)}</span>
        <span class="row-actions"><button class="link-btn quiet" data-act="edit-task" type="button">Edit</button></span>
      </li>`;
    })
    .join("");

  // The slate
  const nowPs = nowProjects();
  $("slateMeta").textContent = `${nowPs.length}/${state.settings.nowProjectCap} projects`;
  $("slateProjects").innerHTML = nowPs
    .map((p) => {
      const s = stageAttrs("project", p);
      const a = area(p.areaId);
      const tasks = openTasks()
        .filter((t) => t.projectId === p.id)
        .sort((x, y) => x.createdAt - y.createdAt)
        .slice(0, 5);
      const rows = tasks
        .map(
          (t) => `<li data-id="${t.id}">
            <input class="check" type="checkbox" data-act="toggle-done" aria-label="Done" />
            <span class="row-title">${esc(t.title)}</span>
            ${t.due ? `<span class="row-sub">${fmtDue(t.due)}</span>` : ""}
            <span class="row-actions"><button class="link-btn quiet" data-act="edit-task" type="button">Edit</button></span>
          </li>`,
        )
        .join("");
      return `<div class="card ${s.cls}" data-id="${p.id}">
        <div class="card-top">
          ${dot(a)}
          <span class="card-title"><button data-act="edit-project" type="button">${esc(p.name)}</button></span>
          ${s.life}
        </div>
        ${p.doneWhen ? `<p class="card-sub">done when ${esc(p.doneWhen)}</p>` : ""}
        <ul class="rows">${rows}</ul>
        <form class="inline-add" data-act="add-task-inline" autocomplete="off">
          <input type="text" placeholder="Add a task…" aria-label="Add a task" />
          <button class="btn ghost sm" type="submit">Add</button>
        </form>
      </div>`;
    })
    .join("");
  if (nowPs.length === 0 && !isEmpty) {
    $("slateProjects").innerHTML = `<p class="muted">No projects on the slate. Pull up to ${state.settings.nowProjectCap} from the Projects tab — the cap is the point.</p>`;
  }

  // Focus tasks
  const pinned = slateTasks();
  $("slateTasksWrap").hidden = pinned.length === 0;
  $<HTMLUListElement>("slateTasks").innerHTML = pinned
    .map((t) => {
      const s = stageAttrs("task", t);
      const where = t.projectId ? project(t.projectId)?.name : areaOfTask(t)?.name;
      return `<li class="${s.cls}" data-id="${t.id}">
        <input class="check" type="checkbox" data-act="toggle-done" aria-label="Done" />
        <span class="row-title">${esc(t.title)}${where ? `<span class="where">${esc(where)}</span>` : ""}</span>
        ${t.due ? `<span class="row-sub">${fmtDue(t.due)}</span>` : ""}
        ${s.life}
        <span class="row-actions"><button class="link-btn quiet" data-act="edit-task" type="button">Edit</button></span>
      </li>`;
    })
    .join("");

  // Recently done (the rear-view mirror)
  const done = liveTasks()
    .filter((t) => t.doneAt && now - t.doneAt < 14 * 86400000)
    .sort((a, b) => b.doneAt! - a.doneAt!)
    .slice(0, 8);
  $("doneSection").hidden = done.length === 0;
  $<HTMLUListElement>("doneList").innerHTML = done
    .map(
      (t) => `<li data-id="${t.id}">
        <input class="check" type="checkbox" checked data-act="toggle-done" aria-label="Not done" />
        <span class="row-title">${esc(t.title)}</span>
      </li>`,
    )
    .join("");
}

function renderProjects(): void {
  const state = store.state;
  const liveAreas = state.areas.filter((a) => !a.compostedAt);

  $("areasStrip").innerHTML = liveAreas
    .map(
      (a) => `<button class="area-chip" data-id="${a.id}" data-act="edit-area" type="button">
        ${dot(a)} ${esc(a.name)}
      </button>`,
    )
    .join("");

  const sections = HORIZONS.map((h) => {
    const ps = liveProjects().filter((p) => p.horizon === h);
    if (ps.length === 0 && h !== "now") return "";
    const rows = ps
      .map((p) => {
        const s = stageAttrs("project", p);
        const a = area(p.areaId);
        const count = openTasks().filter((t) => t.projectId === p.id).length;
        const options = HORIZONS.map(
          (o) => `<option value="${o}"${o === p.horizon ? " selected" : ""}>${HORIZON_LABEL[o]}</option>`,
        ).join("");
        return `<div class="project-row ${s.cls}" data-id="${p.id}">
          ${dot(a)}
          <span class="row-title"><button data-act="edit-project" type="button">${esc(p.name)}</button>
            ${count ? `<span class="where">${count} task${count === 1 ? "" : "s"}</span>` : ""}
          </span>
          ${s.life}
          <select data-act="move-horizon" aria-label="Horizon">${options}</select>
        </div>`;
      })
      .join("");
    const capNote = h === "now" ? ` <span class="muted">${ps.length}/${state.settings.nowProjectCap}</span>` : "";
    const empty = ps.length === 0 ? `<p class="muted">Nothing here.</p>` : "";
    return `<section class="horizon-section"><h2>${HORIZON_LABEL[h]}${capNote}</h2>${rows}${empty}</section>`;
  }).join("");
  $("horizonSections").innerHTML = sections;

  // Standalone tasks: attached to an area (or nothing), not inbox
  const loose = openTasks()
    .filter((t) => !t.projectId && !t.inbox)
    .sort((a, b) => b.touchedAt - a.touchedAt);
  $("looseTasksSection").hidden = loose.length === 0;
  $<HTMLUListElement>("looseTasks").innerHTML = loose
    .map((t) => {
      const s = stageAttrs("task", t);
      const a = areaOfTask(t);
      return `<li class="${s.cls}" data-id="${t.id}">
        <input class="check" type="checkbox" data-act="toggle-done" aria-label="Done" />
        <span class="row-title">${esc(t.title)}${a ? `<span class="where">${esc(a.name)}</span>` : ""}</span>
        ${s.life}
        <span class="row-actions"><button class="link-btn quiet" data-act="edit-task" type="button">Edit</button></span>
      </li>`;
    })
    .join("");
}

function renderArchive(): void {
  const state = store.state;
  const composted: { kind: Kind; label: string; item: Area | Project | Task; name: string }[] = [];
  for (const a of state.areas) if (a.compostedAt) composted.push({ kind: "area", label: "area", item: a, name: a.name });
  for (const p of state.projects) if (p.compostedAt) composted.push({ kind: "project", label: "project", item: p, name: p.name });
  for (const t of state.tasks) if (t.compostedAt) composted.push({ kind: "task", label: t.inbox ? "inbox" : "task", item: t, name: t.title });
  composted.sort((a, b) => (b.item.compostedAt ?? 0) - (a.item.compostedAt ?? 0));

  $("compostEmpty").hidden = composted.length > 0;
  $<HTMLUListElement>("compostList").innerHTML = composted
    .map(
    ({ kind, label, item, name }) => `<li data-id="${item.id}" data-kind="${kind}">
        <span class="row-title">${esc(name)}<span class="where">${label}</span></span>
        <span class="row-sub">${new Date(item.compostedAt!).toLocaleDateString()}</span>
        <span class="row-actions"><button class="link-btn" data-act="revive" type="button">Revive</button></span>
      </li>`,
    )
    .join("");

  const done = state.tasks.filter((t) => t.doneAt && !t.compostedAt).sort((a, b) => b.doneAt! - a.doneAt!);
  $("doneEmpty").hidden = done.length > 0;
  $<HTMLUListElement>("archiveDone").innerHTML = done
    .map(
      (t) => `<li data-id="${t.id}">
        <span class="row-title">${esc(t.title)}</span>
        <span class="row-sub">${new Date(t.doneAt!).toLocaleDateString()}</span>
        <span class="row-actions"><button class="link-btn quiet" data-act="reopen" type="button">Reopen</button></span>
      </li>`,
    )
    .join("");
}

/* ============================== modals =================================== */

const MODALS = ["projectModal", "taskModal", "areaModal", "displaceModal", "settingsModal"] as const;

function openModal(id: (typeof MODALS)[number]): void {
  closeModals();
  $("scrim").hidden = false;
  $(id).hidden = false;
  const first = $(id).querySelector<HTMLElement>("input, select, textarea");
  first?.focus();
}

function closeModals(): void {
  $("scrim").hidden = true;
  for (const id of MODALS) $(id).hidden = true;
}

/* ---- area editor ---- */
let editingAreaId: string | null = null;
let chosenColour = SWATCHES[0];

function openAreaEditor(a?: Area): void {
  editingAreaId = a?.id ?? null;
  $("areaModalTitle").textContent = a ? "Edit area" : "New area";
  $<HTMLInputElement>("areaName").value = a?.name ?? "";
  chosenColour = a?.colour ?? SWATCHES[store.state.areas.length % SWATCHES.length];
  $("areaSwatches").innerHTML = SWATCHES.map(
    (c) => `<button class="swatch${c === chosenColour ? " selected" : ""}" style="background:${c}" data-colour="${c}" type="button" aria-label="Colour ${c}"></button>`,
  ).join("");
  $("areaCompost").hidden = !a;
  openModal("areaModal");
}

function saveArea(): void {
  const name = $<HTMLInputElement>("areaName").value.trim();
  if (!name) return toast("Give the area a name");
  const now = Date.now();
  const existing = editingAreaId ? area(editingAreaId) : undefined;
  const item: Area = existing
    ? { ...existing, name, colour: chosenColour, touchedAt: now }
    : { id: ulid(), name, colour: chosenColour, createdAt: now, touchedAt: now, updatedAt: now };
  store.mutate({ op: "put", kind: "area", item });
  closeModals();
  render();
}

/* ---- project editor ---- */
let editingProjectId: string | null = null;

function areaOptions(selected?: string): string {
  const areas = store.state.areas.filter((a) => !a.compostedAt);
  return areas.map((a) => `<option value="${a.id}"${a.id === selected ? " selected" : ""}>${esc(a.name)}</option>`).join("");
}

function openProjectEditor(p?: Project): void {
  if (store.state.areas.filter((a) => !a.compostedAt).length === 0) {
    toast("Create an area first — every project belongs to one");
    openAreaEditor();
    return;
  }
  editingProjectId = p?.id ?? null;
  $("projectModalTitle").textContent = p ? "Edit project" : "New project";
  $<HTMLInputElement>("projectName").value = p?.name ?? "";
  $<HTMLInputElement>("projectDoneWhen").value = p?.doneWhen ?? "";
  $<HTMLSelectElement>("projectArea").innerHTML = areaOptions(p?.areaId);
  $<HTMLSelectElement>("projectHorizon").value = p?.horizon ?? "next";
  $("projectCompost").hidden = !p;
  $("projectTasksWrap").hidden = !p;
  if (p) renderProjectTaskList(p.id);
  openModal("projectModal");
}

function renderProjectTaskList(projectId: string): void {
  const tasks = store.state.tasks
    .filter((t) => t.projectId === projectId && !t.compostedAt)
    .sort((a, b) => (a.doneAt ? 1 : 0) - (b.doneAt ? 1 : 0) || a.createdAt - b.createdAt);
  $<HTMLUListElement>("projectTasks").innerHTML = tasks
    .map(
      (t) => `<li data-id="${t.id}"${t.doneAt ? ' class="done-log"' : ""}>
        <input class="check" type="checkbox" ${t.doneAt ? "checked" : ""} data-act="toggle-done-inmodal" aria-label="Done" />
        <span class="row-title">${esc(t.title)}</span>
        <span class="row-actions"><button class="link-btn quiet" data-act="edit-task" type="button">Edit</button></span>
      </li>`,
    )
    .join("");
}

function saveProject(): void {
  const name = $<HTMLInputElement>("projectName").value.trim();
  if (!name) return toast("Give the project a name");
  const now = Date.now();
  const horizon = $<HTMLSelectElement>("projectHorizon").value as Horizon;
  const existing = editingProjectId ? project(editingProjectId) : undefined;
  const item: Project = existing
    ? { ...existing, name, doneWhen: $<HTMLInputElement>("projectDoneWhen").value.trim(), areaId: $<HTMLSelectElement>("projectArea").value, horizon, touchedAt: now }
    : {
        id: ulid(),
        name,
        doneWhen: $<HTMLInputElement>("projectDoneWhen").value.trim(),
        areaId: $<HTMLSelectElement>("projectArea").value,
        horizon,
        createdAt: now,
        touchedAt: now,
        updatedAt: now,
      };

  const movingToNow = horizon === "now" && existing?.horizon !== "now";
  if (movingToNow && !ensureNowRoom(item)) return; // displacement dialog took over
  store.mutate({ op: "put", kind: "project", item });
  closeModals();
  render();
}

/* ---- task editor ---- */
let editingTaskId: string | null = null;

function parentOptions(t?: Task): string {
  const ps = liveProjects();
  const areas = store.state.areas.filter((a) => !a.compostedAt);
  const selected = t?.projectId ? `p:${t.projectId}` : t?.areaId ? `a:${t.areaId}` : "";
  const opts = [
    `<option value=""${selected === "" ? " selected" : ""}>— nothing in particular —</option>`,
    ...ps.map((p) => `<option value="p:${p.id}"${selected === `p:${p.id}` ? " selected" : ""}>${esc(p.name)} (project)</option>`),
    ...areas.map((a) => `<option value="a:${a.id}"${selected === `a:${a.id}` ? " selected" : ""}>${esc(a.name)} (area)</option>`),
  ];
  return opts.join("");
}

function openTaskEditor(t?: Task, opts: { triage?: boolean; presetProject?: string } = {}): void {
  editingTaskId = t?.id ?? null;
  $("taskModalTitle").textContent = opts.triage ? "Triage into a real task" : t ? "Edit task" : "New task";
  $<HTMLInputElement>("taskTitle").value = t?.title ?? "";
  $<HTMLTextAreaElement>("taskNote").value = t?.note ?? "";
  $<HTMLSelectElement>("taskParent").innerHTML = parentOptions(
    t ?? (opts.presetProject ? ({ projectId: opts.presetProject } as Task) : undefined),
  );
  $<HTMLInputElement>("taskDue").value = t?.due ?? "";
  $<HTMLInputElement>("taskSlate").checked = !!t?.slate;
  $("taskCompost").hidden = !t;
  openModal("taskModal");
}

function saveTask(): void {
  const title = $<HTMLInputElement>("taskTitle").value.trim();
  if (!title) return toast("Give the task a title");
  const now = Date.now();
  const parent = $<HTMLSelectElement>("taskParent").value;
  const note = $<HTMLTextAreaElement>("taskNote").value.trim();
  const due = $<HTMLInputElement>("taskDue").value;
  const wantSlate = $<HTMLInputElement>("taskSlate").checked;

  const existing = editingTaskId ? store.state.tasks.find((t) => t.id === editingTaskId) : undefined;
  const item: Task = existing
    ? { ...existing, title, touchedAt: now }
    : { id: ulid(), title, createdAt: now, touchedAt: now, updatedAt: now };
  item.note = note || undefined;
  item.due = due || undefined;
  item.projectId = parent.startsWith("p:") ? parent.slice(2) : undefined;
  item.areaId = parent.startsWith("a:") ? parent.slice(2) : undefined;
  // Saving through the full editor is triage by definition: the item leaves
  // the inbox and joins the normal task clock.
  delete item.inbox;

  const pinning = wantSlate && !existing?.slate;
  item.slate = wantSlate || undefined;
  if (pinning && !ensureSlateRoom(item)) return;
  store.mutate({ op: "put", kind: "task", item });
  closeModals();
  render();
}

/* ---- displacement: the cap is the point ---- */

/** Returns true if there is room (after possibly opening the dialog). When the
 * dialog opens it takes over: picking a victim applies both moves. */
function ensureNowRoom(incoming: Project): boolean {
  const cap = store.state.settings.nowProjectCap;
  const current = nowProjects().filter((p) => p.id !== incoming.id);
  if (current.length < cap) return true;
  $("displaceText").textContent = `“${incoming.name}” wants a Now slot, but all ${cap} are taken. Bump one to Next:`;
  $<HTMLUListElement>("displaceList").innerHTML = current
    .map(
      (p) => `<li data-id="${p.id}">
        <span class="row-title">${dot(area(p.areaId))} ${esc(p.name)}</span>
        <span class="row-actions"><button class="link-btn" data-act="displace-project" data-incoming="${incoming.id}" type="button">Bump to Next</button></span>
      </li>`,
    )
    .join("");
  pendingDisplacement = { kind: "project", incoming };
  openModal("displaceModal");
  return false;
}

function ensureSlateRoom(incoming: Task): boolean {
  const cap = store.state.settings.nowTaskCap;
  const current = slateTasks().filter((t) => t.id !== incoming.id);
  if (current.length < cap) return true;
  $("displaceText").textContent = `All ${cap} focus slots are taken. Unpin one to make room for “${incoming.title}”:`;
  $<HTMLUListElement>("displaceList").innerHTML = current
    .map(
      (t) => `<li data-id="${t.id}">
        <span class="row-title">${esc(t.title)}</span>
        <span class="row-actions"><button class="link-btn" data-act="displace-task" type="button">Unpin</button></span>
      </li>`,
    )
    .join("");
  pendingDisplacement = { kind: "task", incoming };
  openModal("displaceModal");
  return false;
}

let pendingDisplacement: { kind: "project"; incoming: Project } | { kind: "task"; incoming: Task } | null = null;

/* ============================== review =================================== */

let queue: ReviewEntry[] = [];
let queueIndex = 0;

function openReview(): void {
  queue = reviewQueue(store.state, Date.now());
  queueIndex = 0;
  $("reviewOverlay").hidden = false;
  renderReviewCard();
}

function closeReview(): void {
  $("reviewOverlay").hidden = true;
  render();
}

function renderReviewCard(): void {
  const entry = queue[queueIndex];
  const body = $("reviewBody");
  const doneMsg = $("reviewDoneMsg");
  if (!entry) {
    body.hidden = true;
    doneMsg.hidden = false;
    $("reviewProgress").textContent = "";
    return;
  }
  body.hidden = false;
  doneMsg.hidden = true;
  $("reviewProgress").textContent = `${queueIndex + 1} of ${queue.length}`;

  const { kind, item } = entry;
  const now = Date.now();
  const untouched = daysUntouched(kind, item, store.state, now);
  const left = Math.max(0, daysLeft(kind, item, store.state, now));
  const isTask = kind === "task";
  const t = item as Task;
  const p = item as Project;

  let kindLabel: string = kind;
  let context = "";
  if (isTask && t.inbox) kindLabel = "inbox";
  if (isTask) {
    const where = t.projectId ? project(t.projectId)?.name : areaOfTask(t)?.name;
    context = where ? `in ${where}` : "";
  } else if (kind === "project") {
    context = [area(p.areaId)?.name, p.doneWhen ? `done when ${p.doneWhen}` : ""].filter(Boolean).join(" · ");
  }

  $("reviewKind").textContent = kindLabel;
  $("reviewTitle").textContent = isTask ? t.title : (item as Project | Area).name;
  $("reviewContext").textContent = context;
  $("reviewAge").textContent = `Untouched for ${untouched} days — composts in ${left}.`;

  const actions: string[] = [];
  actions.push(`<button class="btn" data-ract="renew" type="button">Still matters <small>renew</small></button>`);
  if (isTask && t.inbox) {
    actions.push(`<button class="btn" data-ract="triage" type="button">Make it real <small>becomes a task</small></button>`);
  } else if (isTask) {
    actions.push(`<button class="btn" data-ract="done" type="button">Actually done <small>mark done</small></button>`);
  } else if (kind === "project") {
    const target = demoteHorizon(p.horizon);
    if (target) actions.push(`<button class="btn" data-ract="demote" type="button">Not now <small>${HORIZON_LABEL[p.horizon]} → ${HORIZON_LABEL[target]}</small></button>`);
  }
  actions.push(`<button class="btn" data-ract="compost" type="button">Let it go <small>compost</small></button>`);
  $("reviewActions").innerHTML = actions.join("");
}

function reviewAct(action: string): void {
  const entry = queue[queueIndex];
  if (!entry) return;
  const { kind, item } = entry;
  if (action === "renew") {
    store.mutate({ op: "touch", kind, id: item.id });
  } else if (action === "compost") {
    store.mutate({ op: "compost", kind, id: item.id });
    toast("Composted — revivable from the Archive");
  } else if (action === "done") {
    store.mutate({ op: "done", id: item.id, done: true });
  } else if (action === "demote") {
    const p = item as Project;
    const target = demoteHorizon(p.horizon);
    if (target) store.mutate({ op: "put", kind: "project", item: { ...p, horizon: target, touchedAt: Date.now() } });
  } else if (action === "triage") {
    closeReview();
    openTaskEditor(item as Task, { triage: true });
    return;
  }
  queueIndex++;
  renderReviewCard();
}

/* ============================== away & compost =========================== */

function checkAway(): void {
  const state = store.state;
  const banner = $("awayBanner");
  let offer = state.meta.awayOffer ?? null;
  if (!offer) {
    const gap = awayGapDays(state, Date.now());
    if (gap !== null) {
      offer = { gapDays: gap, detectedAt: Date.now() };
      store.setMeta({ awayOffer: offer });
    }
  }
  // The visit itself is human activity — but only after the gap was measured.
  store.markHumanActivity();

  if (offer) {
    banner.hidden = false;
    $("awayText").textContent = `Quiet for ${offer.gapDays} days — on vacation? Nothing has aged or composted while this question is open.`;
    $("awayBump").textContent = `Bump everything ${offer.gapDays} days`;
  } else {
    banner.hidden = true;
    compostSweep();
  }
}

/** Executes pending compost. Never runs while an away offer is unresolved. */
function compostSweep(): void {
  if (store.state.meta.awayOffer) return;
  const ripe = ripeItems(store.state, Date.now());
  for (const { kind, item } of ripe) {
    store.mutate({ op: "compost", kind, id: item.id }, { human: false });
  }
  if (ripe.length > 0) {
    toast(`${ripe.length} faded item${ripe.length === 1 ? "" : "s"} composted — see Archive`);
    render();
  }
}

function resolveAway(bump: boolean): void {
  const offer = store.state.meta.awayOffer;
  if (!offer) return;
  if (bump) store.mutate({ op: "bump", days: offer.gapDays });
  store.setMeta({ awayOffer: null });
  $("awayBanner").hidden = true;
  compostSweep();
  render();
  toast(bump ? `Everything bumped ${offer.gapDays} days — welcome back` : "Okay — clocks kept running");
}

/* ============================== settings ================================= */

function openSettings(): void {
  const s = store.state.settings;
  $<HTMLInputElement>("setProjectCap").value = String(s.nowProjectCap);
  $<HTMLInputElement>("setTaskCap").value = String(s.nowTaskCap);
  $<HTMLInputElement>("setDecayInbox").value = String(s.decayDays.inbox);
  $<HTMLInputElement>("setDecayTask").value = String(s.decayDays.task);
  $<HTMLInputElement>("setDecayProject").value = String(s.decayDays.project);
  $<HTMLInputElement>("setDecayArea").value = String(s.decayDays.area);
  $<HTMLInputElement>("setAwayGap").value = String(s.awayGapDays);
  openModal("settingsModal");
}

function saveSettings(): void {
  const num = (id: string, fallback: number) => {
    const v = parseInt($<HTMLInputElement>(id).value, 10);
    return Number.isFinite(v) && v > 0 ? v : fallback;
  };
  const d = DEFAULT_SETTINGS;
  store.mutate({
    op: "settings",
    settings: {
      nowProjectCap: num("setProjectCap", d.nowProjectCap),
      nowTaskCap: num("setTaskCap", d.nowTaskCap),
      decayDays: {
        inbox: num("setDecayInbox", d.decayDays.inbox),
        task: num("setDecayTask", d.decayDays.task),
        project: num("setDecayProject", d.decayDays.project),
        area: num("setDecayArea", d.decayDays.area),
      },
      awayGapDays: num("setAwayGap", d.awayGapDays),
    },
  });
  closeModals();
  render();
  toast("Settings saved");
}

function doExport(): void {
  const blob = new Blob([store.exportJson()], { type: "application/json" });
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = `command-centre-${new Date().toISOString().slice(0, 10)}.json`;
  a.click();
  URL.revokeObjectURL(a.href);
}

/* ============================== wiring =================================== */

function taskFromRow(el: HTMLElement): Task | undefined {
  const id = el.closest<HTMLElement>("[data-id]")?.dataset.id;
  return store.state.tasks.find((t) => t.id === id);
}

function init(): void {
  store.load();
  checkAway();

  // navigation
  $("tabNow").addEventListener("click", () => ((view = "now"), render()));
  $("tabProjects").addEventListener("click", () => ((view = "projects"), render()));
  $("tabArchive").addEventListener("click", () => ((view = "archive"), render()));

  // capture
  $("captureForm").addEventListener("submit", (e) => {
    e.preventDefault();
    const input = $<HTMLInputElement>("captureInput");
    const title = input.value.trim();
    if (!title) return;
    const now = Date.now();
    store.mutate({ op: "put", kind: "task", item: { id: ulid(), title, inbox: true, createdAt: now, touchedAt: now, updatedAt: now } });
    input.value = "";
    render();
  });

  // main click delegation
  document.querySelector("main")!.addEventListener("click", (e) => {
    const el = e.target as HTMLElement;
    const btn = el.closest<HTMLElement>("[data-act]");
    if (!btn) return;
    const act = btn.dataset.act;
    if (act === "open-review") return openReview();
    if (act === "triage") {
      const t = taskFromRow(btn);
      if (t) openTaskEditor(t, { triage: true });
      return;
    }
    if (act === "compost-task") {
      const t = taskFromRow(btn);
      if (t) {
        store.mutate({ op: "compost", kind: "task", id: t.id });
        toast("Composted — revivable from the Archive");
        render();
      }
      return;
    }
    if (act === "edit-task") {
      const t = taskFromRow(btn);
      if (t) openTaskEditor(t);
      return;
    }
    if (act === "edit-project") {
      const id = btn.closest<HTMLElement>("[data-id]")?.dataset.id;
      const p = project(id);
      if (p) openProjectEditor(p);
      return;
    }
    if (act === "edit-area") {
      const id = btn.closest<HTMLElement>("[data-id]")?.dataset.id;
      const a = area(id);
      if (a) openAreaEditor(a);
      return;
    }
    if (act === "revive") {
      const li = btn.closest<HTMLElement>("[data-id]")!;
      store.mutate({ op: "revive", kind: li.dataset.kind as Kind, id: li.dataset.id! });
      toast("Revived");
      render();
      return;
    }
    if (act === "reopen") {
      const t = taskFromRow(btn);
      if (t) {
        store.mutate({ op: "done", id: t.id, done: false });
        render();
      }
      return;
    }
  });

  // checkbox changes (done toggles)
  document.querySelector("main")!.addEventListener("change", (e) => {
    const el = e.target as HTMLElement;
    if (el.matches('[data-act="toggle-done"]')) {
      const t = taskFromRow(el);
      if (t) {
        store.mutate({ op: "done", id: t.id, done: (el as HTMLInputElement).checked });
        render();
      }
    }
    if (el.matches('[data-act="move-horizon"]')) {
      const id = el.closest<HTMLElement>("[data-id]")?.dataset.id;
      const p = project(id);
      if (!p) return;
      const target = (el as HTMLSelectElement).value as Horizon;
      const moved = { ...p, horizon: target, touchedAt: Date.now() };
      if (target === "now" && !ensureNowRoom(moved)) {
        (el as HTMLSelectElement).value = p.horizon; // dialog took over; revert UI
        return;
      }
      store.mutate({ op: "put", kind: "project", item: moved });
      render();
    }
  });

  // inline add-task on slate cards
  document.querySelector("main")!.addEventListener("submit", (e) => {
    const form = e.target as HTMLElement;
    if (!form.matches('[data-act="add-task-inline"]')) return;
    e.preventDefault();
    const input = form.querySelector("input")!;
    const title = input.value.trim();
    const projectId = form.closest<HTMLElement>("[data-id]")?.dataset.id;
    if (!title || !projectId) return;
    const now = Date.now();
    store.mutate({ op: "put", kind: "task", item: { id: ulid(), title, projectId, createdAt: now, touchedAt: now, updatedAt: now } });
    input.value = "";
    render();
  });

  // away banner
  $("awayBump").addEventListener("click", () => resolveAway(true));
  $("awayDecline").addEventListener("click", () => resolveAway(false));

  // review
  $("reviewBtn").addEventListener("click", openReview);
  $("reviewClose").addEventListener("click", closeReview);
  $("reviewFinish").addEventListener("click", closeReview);
  $("reviewSkip").addEventListener("click", () => {
    queueIndex++;
    renderReviewCard();
  });
  $("reviewActions").addEventListener("click", (e) => {
    const btn = (e.target as HTMLElement).closest<HTMLElement>("[data-ract]");
    if (btn) reviewAct(btn.dataset.ract!);
  });

  // modals: shared close behaviour
  for (const id of MODALS) {
    $(id).addEventListener("click", (e) => {
      if ((e.target as HTMLElement).closest("[data-close]")) closeModals();
    });
  }
  $("scrim").addEventListener("click", closeModals);
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
      closeModals();
      if (!$("reviewOverlay").hidden) closeReview();
    }
  });

  // displacement picks
  $("displaceList").addEventListener("click", (e) => {
    const btn = (e.target as HTMLElement).closest<HTMLElement>("[data-act]");
    if (!btn || !pendingDisplacement) return;
    const victimId = btn.closest<HTMLElement>("[data-id]")!.dataset.id!;
    if (pendingDisplacement.kind === "project" && btn.dataset.act === "displace-project") {
      const victim = project(victimId);
      if (victim) store.mutate({ op: "put", kind: "project", item: { ...victim, horizon: "next" } });
      store.mutate({ op: "put", kind: "project", item: pendingDisplacement.incoming });
      toast(`Moved “${victim?.name}” to Next`);
    } else if (pendingDisplacement.kind === "task" && btn.dataset.act === "displace-task") {
      const victim = store.state.tasks.find((t) => t.id === victimId);
      if (victim) store.mutate({ op: "put", kind: "task", item: { ...victim, slate: undefined } });
      store.mutate({ op: "put", kind: "task", item: pendingDisplacement.incoming });
    }
    pendingDisplacement = null;
    closeModals();
    render();
  });

  // editors
  $("newAreaBtn").addEventListener("click", () => openAreaEditor());
  $("newProjectBtn").addEventListener("click", () => openProjectEditor());
  $("newTaskBtn").addEventListener("click", () => openTaskEditor());
  $("areaSave").addEventListener("click", saveArea);
  $("projectSave").addEventListener("click", saveProject);
  $("taskSave").addEventListener("click", saveTask);
  $("areaSwatches").addEventListener("click", (e) => {
    const sw = (e.target as HTMLElement).closest<HTMLElement>("[data-colour]");
    if (!sw) return;
    chosenColour = sw.dataset.colour!;
    for (const b of $("areaSwatches").querySelectorAll(".swatch")) b.classList.toggle("selected", b === sw);
  });
  $("areaCompost").addEventListener("click", () => {
    if (editingAreaId) {
      store.mutate({ op: "compost", kind: "area", id: editingAreaId });
      closeModals();
      render();
      toast("Area composted — its projects stay put, revive it any time");
    }
  });
  $("projectCompost").addEventListener("click", () => {
    if (editingProjectId) {
      store.mutate({ op: "compost", kind: "project", id: editingProjectId });
      closeModals();
      render();
      toast("Project composted — revivable from the Archive");
    }
  });
  $("taskCompost").addEventListener("click", () => {
    if (editingTaskId) {
      store.mutate({ op: "compost", kind: "task", id: editingTaskId });
      closeModals();
      render();
      toast("Composted");
    }
  });

  // project modal: inline tasks
  $("projectAddTaskForm").addEventListener("submit", (e) => {
    e.preventDefault();
    if (!editingProjectId) return;
    const input = $<HTMLInputElement>("projectAddTaskInput");
    const title = input.value.trim();
    if (!title) return;
    const now = Date.now();
    store.mutate({ op: "put", kind: "task", item: { id: ulid(), title, projectId: editingProjectId, createdAt: now, touchedAt: now, updatedAt: now } });
    input.value = "";
    renderProjectTaskList(editingProjectId);
  });
  $("projectTasks").addEventListener("click", (e) => {
    const btn = (e.target as HTMLElement).closest<HTMLElement>('[data-act="edit-task"]');
    if (!btn) return;
    const t = taskFromRow(btn);
    if (t) openTaskEditor(t);
  });
  $("projectTasks").addEventListener("change", (e) => {
    const el = e.target as HTMLInputElement;
    if (!el.matches('[data-act="toggle-done-inmodal"]')) return;
    const t = taskFromRow(el);
    if (t && editingProjectId) {
      store.mutate({ op: "done", id: t.id, done: el.checked });
      renderProjectTaskList(editingProjectId);
    }
  });

  // settings
  $("settingsBtn").addEventListener("click", openSettings);
  $("settingsSave").addEventListener("click", saveSettings);
  $("exportBtn").addEventListener("click", doExport);
  $("importBtn").addEventListener("click", () => $<HTMLInputElement>("importFile").click());
  $("importFile").addEventListener("change", async () => {
    const file = $<HTMLInputElement>("importFile").files?.[0];
    if (!file) return;
    try {
      const counts = store.importJson(await file.text());
      toast(`Imported ${counts.areas} areas, ${counts.projects} projects, ${counts.tasks} tasks`);
      closeModals();
      render();
    } catch (err) {
      toast(err instanceof Error ? err.message : "Import failed");
    }
    $<HTMLInputElement>("importFile").value = "";
  });
  $("resetBtn").addEventListener("click", () => {
    if (confirm("Erase every area, project and task on this device? Export first if unsure.")) {
      store.resetAll();
      closeModals();
      render();
      toast("Fresh start");
    }
  });

  // Returning to the tab after a while re-checks the away gap.
  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible") {
      checkAway();
      render();
    }
  });

  render();

  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register(`${__BASE__}/sw.js`).catch(() => {});
  }
}

init();
