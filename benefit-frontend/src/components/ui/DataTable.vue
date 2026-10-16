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
</script>

<script setup>
/**
 * 共用資料表。傳入欄位與資料列即可顯示，適合後台列表重複套用。
 * 某一欄需要連結或狀態標籤時，使用具名插槽 cell-{欄位 key}。
 *
 * columns 項目：
 * - key：資料列上的欄位名稱
 * - label：表頭文字
 * - align：儲存格對齊 start | center | end；表頭一律置中
 * - minWidth：欄位最小寬度，例如 "8rem"
 * - nowrap：內容不換行
 * - emphasis：內容加粗
 * - type：text（預設）| link
 * - format(value, row)：文字欄轉成顯示文字；回傳空值時顯示 —
 *
 * 頁面只傳 columns 與 rows。某一欄要自訂內容時，再用具名插槽 cell-{欄位 key} 覆寫。
 */
import { computed } from "vue";

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

const themeStyle = computed(() => THEMES[props.theme] ?? THEMES.coral);

function columnStyle(column) {
  return column.minWidth ? { minWidth: column.minWidth } : undefined;
}

function headerClass(column) {
  return {
    "is-center": column.align === "center",
    "is-end": column.align === "end",
  };
}

function cellClass(column) {
  return {
    ...headerClass(column),
    "is-emphasis": column.emphasis,
    "is-nowrap": column.nowrap,
  };
}

function rowKeyOf(row, index) {
  const id = row?.[props.rowKey];
  return id == null || id === "" ? `row-${index}` : id;
}

function cellValue(column, row) {
  return row?.[column.key];
}

function displayText(column, row) {
  const value = cellValue(column, row);
  const formatted = typeof column.format === "function" ? column.format(value, row) : value;
  if (formatted == null || formatted === "") return "—";
  if (typeof formatted === "boolean") return formatted ? "是" : "否";
  return String(formatted);
}

function httpUrl(value) {
  if (typeof value !== "string" || !value.trim()) return "";
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:" ? url.href : "";
  } catch {
    return "";
  }
}
</script>

<template>
  <div class="data-sheet" :class="{ 'is-loading': loading }" :style="themeStyle" :aria-busy="loading">
    <table>
      <caption class="sheet-caption">{{ caption }}</caption>
      <thead>
        <tr>
          <th
            v-for="column in columns"
            :key="column.key"
            scope="col"
            :class="headerClass(column)"
            :style="columnStyle(column)"
          >
            {{ column.label }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="!rows.length">
          <td class="is-empty" :colspan="Math.max(columns.length, 1)">{{ emptyText }}</td>
        </tr>
        <tr v-for="(row, index) in rows" v-else :key="rowKeyOf(row, index)">
          <td
            v-for="column in columns"
            :key="column.key"
            :class="cellClass(column)"
            :style="columnStyle(column)"
          >
            <slot
              :name="`cell-${column.key}`"
              :row="row"
              :column="column"
              :value="cellValue(column, row)"
            >
              <a
                v-if="column.type === 'link' && httpUrl(cellValue(column, row))"
                class="sheet-link"
                :href="httpUrl(cellValue(column, row))"
                target="_blank"
                rel="noopener noreferrer"
              >{{ cellValue(column, row) }}</a>
              <template v-else>{{ displayText(column, row) }}</template>
            </slot>
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
  border-collapse: separate;
  border-spacing: 0;
  color: var(--c-ink);
  font-size: var(--text-body);
  line-height: var(--leading-body);
}

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
  border-bottom: 1.5px solid var(--c-border);
  background: var(--sheet-head);
  color: var(--sheet-head-text);
  font-size: var(--text-control);
  font-weight: 800;
  line-height: var(--leading-title);
  text-align: center;
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

td.is-center {
  text-align: center;
}

td.is-end {
  text-align: end;
}

td.is-emphasis {
  font-weight: 800;
}

.is-nowrap {
  white-space: nowrap;
}

.is-empty {
  padding: 2.15rem 1.25rem;
  color: var(--c-muted);
  font-weight: 700;
  text-align: center;
}

tbody tr:hover td.is-empty {
  background: transparent;
}

.sheet-link {
  color: var(--sheet-accent);
  font-weight: 700;
  text-underline-offset: 0.18em;
  word-break: break-all;
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
