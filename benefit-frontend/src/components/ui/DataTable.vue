<script>
export const THEMES = {
  coral: {
    "--sheet-head": "#fff3ee",
    "--sheet-head-text": "#9a4632",
    "--sheet-hover": "#fff8f5",
    "--sheet-accent": "#ff7049",
  },
  purple: {
    "--sheet-head": "#f3ecff",
    "--sheet-head-text": "#5c4696",
    "--sheet-hover": "#f8f4ff",
    "--sheet-accent": "#8d72ed",
  },
};

export function textOf(value) {
  return value == null ? "" : String(value).trim();
}

export function dateTimeText(value) {
  const text = textOf(value);
  return text ? text.replace("T", " ") : "";
}

export function displayText(value, fallback = "—") {
  return textOf(value) || fallback;
}

export function httpUrl(value) {
  const text = textOf(value);
  if (!text) return "";

  try {
    const url = new URL(text);
    return url.protocol === "http:" || url.protocol === "https:" ? url.href : "";
  } catch {
    return "";
  }
}
</script>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, useId, useSlots } from "vue";

/** columns 定義欄位與 format；#cell-{key} 可覆寫儲存格。 */
const props = defineProps({
  columns: { type: Array, required: true },
  rows: { type: Array, default: () => [] },
  rowKey: { type: String, required: true },
  caption: { type: String, default: "資料表" },
  emptyText: { type: String, default: "目前沒有資料" },
  loading: { type: Boolean, default: false },
  theme: {
    type: String,
    default: "coral",
    validator: (value) => Object.hasOwn(THEMES, value),
  },
});

const slots = useSlots();
const themeStyle = computed(() => THEMES[props.theme] ?? THEMES.coral);

// ── 表格與儲存格 ───────────────────────────────────────────────
const tableStyle = computed(() => {
  const widths = props.columns.map(({ minWidth }) =>
    String(minWidth ?? "").match(/^(\d+(?:\.\d+)?)rem$/)?.[1],
  );
  if (widths.some((width) => width === undefined)) return undefined;

  const total = widths.reduce((sum, width) => sum + Number(width), 0);
  return total > 0 ? { width: `${total}rem` } : undefined;
});

const columnStyle = (column) =>
  column.minWidth ? { width: column.minWidth } : undefined;

const hasCellSlot = (column) => Boolean(slots[`cell-${column.key}`]);

function cellClass(column) {
  return {
    "is-center": column.align === "center",
    "is-end": column.align === "end",
    "is-emphasis": column.emphasis,
    "is-nowrap": column.nowrap,
    "is-clip": !hasCellSlot(column),
  };
}

function rowKeyOf(row, index) {
  const id = row?.[props.rowKey];
  return id == null || id === "" ? `row-${index}` : id;
}

const cellValue = (column, row) => row?.[column.key];

function cellText(column, row) {
  const value = cellValue(column, row);
  const formatted = typeof column.format === "function"
    ? column.format(value, row)
    : value;

  if (formatted == null || formatted === "") return "—";
  if (typeof formatted === "boolean") return formatted ? "是" : "否";
  return String(formatted);
}

// ── 裁切文字提示 ───────────────────────────────────────────────
const TIP_MOTION_MS = 200;
const TIP_MARGIN = 10;
const TIP_ARROW_INSET = 16;

const tipId = useId();
const tipEl = ref(null);
const tip = ref(null);
let anchor = null;
let listening = false;
let hideTimer = 0;

const isClipped = (el) => el.scrollWidth - el.clientWidth > 1;
const motionMs = () =>
  window.matchMedia("(prefers-reduced-motion: reduce)").matches ? 0 : TIP_MOTION_MS;

function startListening() {
  if (listening) return;
  listening = true;
  window.addEventListener("scroll", placeTip, true);
  window.addEventListener("resize", placeTip);
  window.addEventListener("keydown", onKeydown);
}

function stopListening() {
  if (!listening) return;
  listening = false;
  window.removeEventListener("scroll", placeTip, true);
  window.removeEventListener("resize", placeTip);
  window.removeEventListener("keydown", onKeydown);
}

function openTip(event) {
  const el = event.currentTarget;
  if (!(el instanceof HTMLElement) || !isClipped(el)) return;

  const text = el.textContent?.replace(/\s+/g, " ").trim() ?? "";
  if (!text || text === "—") return;

  window.clearTimeout(hideTimer);
  if (anchor && anchor !== el) anchor.removeAttribute("aria-describedby");
  anchor = el;
  el.setAttribute("aria-describedby", tipId);

  const previous = tip.value;
  const ready = Boolean(previous?.ready);
  tip.value = {
    text,
    top: ready ? previous.top : -9999,
    left: ready ? previous.left : 0,
    arrow: ready ? previous.arrow : TIP_ARROW_INSET,
    placement: ready ? previous.placement : "top",
    ready,
    phase: ready && previous.phase === "in" ? "in" : "",
  };

  startListening();
  nextTick(placeTip);
}

function closeTip() {
  anchor?.removeAttribute("aria-describedby");
  anchor = null;
  stopListening();
  window.clearTimeout(hideTimer);

  if (tip.value?.phase !== "in") {
    tip.value = null;
    return;
  }

  tip.value = { ...tip.value, phase: "out" };
  hideTimer = window.setTimeout(() => { tip.value = null; }, motionMs());
}

function onKeydown(event) {
  if (event.key === "Escape") closeTip();
}

function placeTip() {
  const el = anchor;
  const bubble = tipEl.value;
  if (!el || !bubble || !tip.value) return;

  const rect = el.getBoundingClientRect();
  const outside = rect.bottom <= 0 || rect.top >= window.innerHeight ||
    rect.right <= 0 || rect.left >= window.innerWidth;
  if (!isClipped(el) || outside) {
    closeTip();
    return;
  }

  const { width, height } = bubble.getBoundingClientRect();
  const centerX = rect.left + rect.width / 2;
  const left = Math.min(
    Math.max(centerX - width / 2, TIP_MARGIN),
    Math.max(TIP_MARGIN, window.innerWidth - width - TIP_MARGIN),
  );
  const above = rect.top - height - TIP_MARGIN;
  const placement = above < TIP_MARGIN ? "bottom" : "top";
  const top = placement === "top" ? above : rect.bottom + TIP_MARGIN;
  const arrow = Math.min(
    Math.max(centerX - left, TIP_ARROW_INSET),
    Math.max(width - TIP_ARROW_INSET, TIP_ARROW_INSET),
  );

  const replay = tip.value.phase !== "in";
  tip.value = {
    ...tip.value, top, left, arrow, placement,
    ready: true,
    phase: replay ? "set" : "in",
  };
  if (!replay) return;

  nextTick(() => {
    tipEl.value?.getBoundingClientRect();
    if (tip.value?.phase === "set" && anchor) {
      tip.value = { ...tip.value, phase: "in" };
    }
  });
}

// 維持文字與連結各自原有的觸發行為。
const tooltipHoverEvents = { mouseenter: openTip, mouseleave: closeTip };
const tooltipLinkEvents = {
  ...tooltipHoverEvents,
  focus: openTip,
  blur: closeTip,
};

onBeforeUnmount(() => {
  window.clearTimeout(hideTimer);
  anchor?.removeAttribute("aria-describedby");
  anchor = null;
  tip.value = null;
  stopListening();
});
</script>

<template>
  <div class="data-sheet" :class="{ 'is-loading': loading }" :style="themeStyle" :aria-busy="loading">
    <table :style="tableStyle">
      <caption class="sheet-caption">{{ caption }}</caption>

      <colgroup>
        <col v-for="column in columns" :key="column.key" :style="columnStyle(column)" />
      </colgroup>

      <thead>
        <tr>
          <th v-for="column in columns" :key="column.key" scope="col">
            {{ column.label }}
          </th>
        </tr>
      </thead>

      <tbody>
        <tr v-if="!rows.length">
          <td class="is-empty" :colspan="Math.max(columns.length, 1)">
            {{ emptyText }}
          </td>
        </tr>

        <tr v-for="(row, index) in rows" v-else :key="rowKeyOf(row, index)">
          <td v-for="column in columns" :key="column.key" :class="cellClass(column)">
            <!-- 自訂欄位內容 -->
            <slot v-if="hasCellSlot(column)" :name="`cell-${column.key}`" :row="row" :column="column"
              :value="cellValue(column, row)" />

            <!-- 網址欄位 -->
            <a v-else-if="column.type === 'link' && httpUrl(cellValue(column, row))" class="sheet-link"
              :href="httpUrl(cellValue(column, row))" target="_blank" rel="noopener noreferrer"
              v-on="tooltipLinkEvents">
              {{ cellText(column, row) }}
            </a>

            <!-- 一般文字欄位 -->
            <span v-else class="sheet-clip" v-on="tooltipHoverEvents">
              {{ cellText(column, row) }}
            </span>
          </td>
        </tr>
      </tbody>
    </table>

    <Teleport to="body">
      <div v-if="tip" :id="tipId" ref="tipEl" class="sheet-bubble" :class="{
        'is-set': tip.phase === 'set' || tip.phase === 'in' || tip.phase === 'out',
        'is-in': tip.phase === 'in',
        'is-out': tip.phase === 'out',
      }" role="tooltip" :data-placement="tip.placement" :style="{
        ...themeStyle,
        top: `${tip.top}px`,
        left: `${tip.left}px`,
        '--arrow-x': `${tip.arrow}px`,
      }">
        {{ tip.text }}
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.data-sheet {
  position: relative;
  overflow-x: auto;
  border: 1.5px solid var(--c-border);
  border-radius: 22px;
  background: var(--c-card);
  box-shadow: var(--shadow-soft);
  transition: opacity var(--ease);
}

.data-sheet.is-loading {
  opacity: 0.48;
  pointer-events: none;
}

.data-sheet table {
  width: max-content;
  min-width: 100%;
  table-layout: fixed;
  border-collapse: separate;
  border-spacing: 0;
  color: var(--c-ink);
  font-size: var(--text-body);
  line-height: var(--leading-body);
}

/* 無障礙標題：保留給螢幕閱讀器 */
.sheet-caption {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

/* 表頭與儲存格 */
th,
td {
  padding: 0.85rem 1rem;
  vertical-align: middle;
  text-align: start;
}

th {
  position: sticky;
  top: 0;
  z-index: 1;
  overflow: hidden;
  border-bottom: 1.5px solid var(--c-border);
  background: var(--sheet-head);
  color: var(--sheet-head-text);
  font-size: var(--text-control);
  font-weight: 800;
  line-height: var(--leading-title);
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

td {
  border-bottom: 1px solid var(--c-border);
  font-size: var(--text-body);
  line-height: var(--leading-body);
}

tbody tr:last-child td {
  border-bottom: none;
}

tbody tr:hover td {
  background: var(--sheet-hover);
}

/* 欄位顯示設定 */
td.is-center {
  text-align: center;
}

td.is-end {
  text-align: end;
}

td.is-emphasis {
  font-weight: 800;
}

td.is-clip {
  overflow: hidden;
}

td.is-nowrap {
  white-space: nowrap;
}

.sheet-clip,
.sheet-link {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 空資料提示 */
.is-empty {
  padding: 2.15rem 1.25rem;
  color: var(--c-muted);
  font-weight: 700;
  text-align: center;
}

tbody tr:hover td.is-empty {
  background: transparent;
}

/* 連結樣式 */
.sheet-link {
  color: var(--sheet-accent);
  font-weight: 700;
  text-underline-offset: 0.18em;
}

.sheet-link:hover {
  color: var(--sheet-head-text);
}

.sheet-link:focus-visible {
  outline: 2px solid var(--sheet-accent);
  outline-offset: 3px;
  border-radius: 4px;
}

/* 被裁切內容的完整文字泡泡 */
.sheet-bubble {
  position: fixed;
  z-index: 40;
  max-width: min(22rem, calc(100vw - 1.25rem));
  padding: 0.5rem 0.75rem;
  border: 1.5px solid var(--sheet-accent);
  border-radius: 14px;
  background: var(--sheet-head);
  color: var(--sheet-head-text);
  font-size: var(--text-min);
  font-weight: 700;
  line-height: 1.45;
  overflow-wrap: anywhere;
  box-shadow: 0 12px 28px rgb(41 43 53 / 14%);
  pointer-events: none;
  opacity: 0;
  visibility: hidden;
  transform: translateY(0.75rem);
  transition: opacity 0.2s ease-out, transform 0.2s ease-out;
}

.sheet-bubble.is-set {
  visibility: visible;
}

.sheet-bubble.is-in {
  opacity: 1;
  transform: translateY(0);
}

.sheet-bubble.is-out {
  transition-timing-function: ease-in;
}

.sheet-bubble::before,
.sheet-bubble::after {
  content: "";
  position: absolute;
  left: var(--arrow-x);
  transform: translateX(-50%);
  border: 0.5rem solid transparent;
}

.sheet-bubble[data-placement="top"]::before {
  top: 100%;
  border-top-color: var(--sheet-accent);
}

.sheet-bubble[data-placement="top"]::after {
  top: calc(100% - 3px);
  border-top-color: var(--sheet-head);
}

.sheet-bubble[data-placement="bottom"]::before {
  bottom: 100%;
  border-bottom-color: var(--sheet-accent);
}

.sheet-bubble[data-placement="bottom"]::after {
  bottom: calc(100% - 3px);
  border-bottom-color: var(--sheet-head);
}
</style>
