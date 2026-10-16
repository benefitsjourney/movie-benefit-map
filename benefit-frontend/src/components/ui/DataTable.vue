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
/**
 * columns 定義欄位與 format；具名插槽 #cell-{key} 可取代單一儲存格。
 */
import { computed, useSlots } from "vue";

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

/** 所有欄位皆使用 rem 寬度時，計算表格總寬度。 */
const tableStyle = computed(() => {
  let totalWidth = 0;

  for (const { minWidth } of props.columns) {
    const match = String(minWidth ?? "").match(/^(\d+(?:\.\d+)?)rem$/);
    if (!match) return undefined;

    totalWidth += Number(match[1]);
  }

  return totalWidth > 0 ? { width: `${totalWidth}rem` } : undefined;
});

function columnStyle(column) {
  return column.minWidth ? { width: column.minWidth } : undefined;
}

function cellClass(column) {
  return {
    "is-center": column.align === "center",
    "is-end": column.align === "end",
    "is-emphasis": column.emphasis,
    "is-nowrap": column.nowrap,
    "is-clip": !hasCellSlot(column),
  };
}

function hasCellSlot(column) {
  return Boolean(slots[`cell-${column.key}`]);
}

/** 優先使用資料 ID，沒有 ID 時以索引建立備用 key。 */
function rowKeyOf(row, index) {
  const id = row?.[props.rowKey];
  return id == null || id === "" ? `row-${index}` : id;
}

function cellValue(column, row) {
  return row?.[column.key];
}

/** 格式化儲存格內容，統一處理空值與布林值。 */
function cellText(column, row) {
  const value = cellValue(column, row);
  const formatted =
    typeof column.format === "function"
      ? column.format(value, row)
      : value;

  if (formatted == null || formatted === "") return "—";
  if (typeof formatted === "boolean") return formatted ? "是" : "否";

  return String(formatted);
}

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
              :href="httpUrl(cellValue(column, row))" :title="cellText(column, row)" target="_blank"
              rel="noopener noreferrer">
              {{ cellText(column, row) }}
            </a>

            <!-- 一般文字欄位 -->
            <span v-else class="sheet-clip"
              :title="cellText(column, row) === '—' ? undefined : cellText(column, row)">
              {{ cellText(column, row) }}
            </span>
          </td>
        </tr>
      </tbody>
    </table>
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
</style>
