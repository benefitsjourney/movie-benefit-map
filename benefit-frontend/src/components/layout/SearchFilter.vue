<script>
export const THEMES = {
  coral: {
    "--filter-accent": "#ff7049",
    "--filter-focus": "rgb(255 112 73 / 20%)",
  },
  purple: {
    "--filter-accent": "#8D72ED",
    "--filter-focus": "rgb(141 114 237 / 20%)",
  },
};
</script>

<script setup>
import { computed, reactive, useId, watch } from "vue";
import { Icon } from "@iconify/vue";
import searchIcon from "@iconify-icons/mdi/magnify";
import refreshIcon from "@iconify-icons/mdi/refresh";
import AppButton from "@/components/ui/Button.vue";

const filters = defineModel({
  type: Object,
  default: () => ({}),
});

const props = defineProps({
  title: { type: String, default: "搜尋" },
  titleIcon: { type: [Object, String], default: () => searchIcon },
  submitLabel: { type: String, default: "搜尋" },
  submitIcon: { type: [Object, String], default: () => searchIcon },
  resetLabel: { type: String, default: "清除條件" },
  fields: { type: Array, default: () => [] },
  theme: {
    type: String,
    default: "coral",
    validator: (value) => Object.hasOwn(THEMES, value),
  },
});

const themeStyle = computed(() => THEMES[props.theme] ?? THEMES.coral);
const fieldId = useId();
const draft = reactive({});

const isCheckbox = (field) => field.type === "checkbox";
const isSelect = (field) => field.type === "select";
const getFieldId = (field) => `${fieldId}-${field.key}`;

/**
 * 統一欄位值格式：
 * - checkbox：布林值
 * - select：保留選項原始型別
 * - 文字欄位：字串，可選擇去除前後空白
 */
function normalizeValue(field, value, trim = false) {
  if (isCheckbox(field)) return value === true;
  if (isSelect(field)) return value ?? "";

  const text = String(value ?? "");
  return trim ? text.trim() : text;
}

/** 根據欄位設定建立完整的搜尋條件 */
function buildFilters(source, trim = false) {
  return Object.fromEntries(
    props.fields.map((field) => [
      field.key,
      normalizeValue(field, source?.[field.key], trim),
    ]),
  );
}

/**
 * 同步外部條件至編輯表單。
 * 監聽欄位 key/type，讓動態新增或切換欄位類型時也能更新。
 */
watch(
  [
    filters,
    () => props.fields.map(({ key, type }) => `${key}:${type}`).join("|"),
  ],
  () => {
    const next = buildFilters(filters.value);

    // 移除已不存在的欄位，避免殘留舊資料
    for (const key of Object.keys(draft)) {
      if (!Object.hasOwn(next, key)) delete draft[key];
    }

    Object.assign(draft, next);
  },
  { immediate: true },
);

/** 已套用至少一個非空條件時，顯示清除按鈕 */
const showReset = computed(() =>
  props.fields.some((field) => {
    const value = normalizeValue(field, filters.value?.[field.key], true);
    return isCheckbox(field) ? value : value !== "";
  }),
);

/** 支援陣列或物件格式的下拉選項 */
function fieldOptions(options) {
  if (Array.isArray(options)) return options;

  return Object.entries(options ?? {}).map(([value, label]) => ({
    value,
    label,
  }));
}

/** 套用搜尋條件，僅在內容變動時更新 v-model */
function search() {
  const next = buildFilters(draft, true);
  const current = buildFilters(filters.value);

  const changed = props.fields.some(
    ({ key }) => !Object.is(next[key], current[key]),
  );

  Object.assign(draft, next);

  if (changed) filters.value = next;
}

/** 清空所有欄位並套用搜尋 */
function resetQuery() {
  Object.assign(draft, buildFilters({}));
  search();
}
</script>

<template>
  <div class="search-filter" :style="themeStyle">
    <!-- 標題與自訂操作區 -->
    <div v-if="title || $slots.actions" class="search-filter-head">
      <h2 v-if="title" class="search-filter-title">
        <Icon v-if="titleIcon" :icon="titleIcon" aria-hidden="true" />
        {{ title }}
      </h2>

      <div v-if="$slots.actions" class="search-filter-head-actions">
        <slot name="actions" />
      </div>
    </div>

    <!-- 搜尋欄位 -->
    <form class="search-filter-grid" @submit.prevent="search">
      <div v-for="field in fields" :key="field.key" class="field" :class="{ 'is-check': isCheckbox(field) }">
        <!-- Checkbox：變更後立即搜尋 -->
        <label v-if="isCheckbox(field)" class="check-control" :for="getFieldId(field)">
          <input :id="getFieldId(field)" v-model="draft[field.key]" type="checkbox" @change="search" />
          <span>{{ field.label }}</span>
        </label>

        <!-- Select / Text / Search -->
        <template v-else>
          <label :for="getFieldId(field)">
            {{ field.label }}
          </label>

          <div class="field-control" :class="{ 'has-icon': field.icon }">
            <Icon v-if="field.icon" :icon="field.icon" aria-hidden="true" />

            <select v-if="isSelect(field)" :id="getFieldId(field)" v-model="draft[field.key]" class="form-select"
              @change="search">
              <option v-if="field.emptyLabel !== null" value="">
                {{ field.emptyLabel ?? "全部" }}
              </option>

              <option v-for="option in fieldOptions(field.options)" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>

            <input v-else :id="getFieldId(field)" v-model="draft[field.key]"
              :type="field.type === 'search' ? 'search' : 'text'" class="form-control"
              :placeholder="field.placeholder" />
          </div>
        </template>
      </div>

      <!-- 搜尋與清除操作 -->
      <div class="search-filter-actions">
        <AppButton class="search-submit" type="submit" :icon="submitIcon" :color="theme">
          {{ submitLabel }}
        </AppButton>

        <AppButton v-if="showReset" class="search-reset" type="button" variant="reset" :color="theme"
          :icon="refreshIcon" @click="resetQuery">
          {{ resetLabel }}
        </AppButton>
      </div>
    </form>

    <!-- 搜尋結果插槽 -->
    <div v-if="$slots.default" class="search-filter-results">
      <slot />
    </div>
  </div>
</template>

<style scoped>
/* ===== 容器與標題 ===== */

.search-filter {
  margin-bottom: 1rem;
  padding: 1.15rem 1.35rem 1.35rem;
  border: 1.5px solid var(--c-border);
  border-radius: 22px;
  background: var(--c-card);
  box-shadow: var(--shadow-soft);
}

.search-filter-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem 1rem;
  margin-bottom: 0.8rem;
}

.search-filter-title {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  min-width: 0;
  margin: 0;
  font-size: var(--text-section-title);
  line-height: 1.3;
  font-weight: 900;
}

.search-filter-head-actions {
  display: flex;
  flex: none;
  margin-left: auto;
}

/* ===== 搜尋表單 ===== */

.search-filter-grid {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 0.8rem;
}

.field {
  flex: 1 1 200px;
  min-width: 0;
}

.field.is-check {
  flex: 0 1 auto;
}

.field label {
  display: block;
  margin-bottom: 0.3rem;
  color: var(--c-muted);
  font-size: var(--text-control);
  font-weight: 700;
}

/* Checkbox */

.field .check-control {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  min-height: 3rem;
  margin: 0;
  color: var(--c-ink);
  font-weight: 800;
  cursor: pointer;
}

.check-control input {
  width: 1.15rem;
  height: 1.15rem;
  margin: 0;
  accent-color: var(--filter-accent);
  cursor: pointer;
}

/* 輸入框與下拉選單 */

.field-control {
  position: relative;
}

:is(.search-filter-title, .field-control)>svg {
  color: var(--filter-accent);
  font-size: var(--text-card-title);
}

.field-control>svg {
  position: absolute;
  top: 50%;
  left: 0.8rem;
  z-index: 1;
  pointer-events: none;
  transform: translateY(-50%);
}

:is(.form-select, .form-control) {
  width: 100%;
  min-height: 3rem;
  padding-top: 0.5rem;
  padding-bottom: 0.5rem;
  border: 1.5px solid #eadfce;
  border-radius: 14px;
  background-color: #fff;
  color: var(--c-ink);
  font-size: var(--text-control);
  line-height: 1.5;
}

:is(.form-select, .form-control):focus {
  border-color: var(--filter-accent);
  box-shadow: 0 0 0 3px var(--filter-focus);
}

:is(.form-select, .form-control):focus-visible {
  outline: 2px solid var(--filter-accent);
  outline-offset: 2px;
}

.field-control.has-icon :is(.form-select, .form-control) {
  padding-left: 2.7rem;
}

/* ===== 操作按鈕與搜尋結果 ===== */

.search-filter-actions {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem 1rem;
}

.search-filter-results:has(*) {
  margin-top: 2rem;
}

.search-filter-results:not(:has(*)) {
  display: none;
}

/* ===== 手機版 RWD ===== */

@media (max-width: 519px) {
  .search-filter-head {
    align-items: stretch;
  }

  .search-filter-title {
    font-size: var(--text-feature-title);
  }

  .search-filter-head-actions {
    flex-basis: 100%;
    margin-left: 0;
  }

  .search-filter-head-actions :deep(.app-button) {
    width: 100%;
  }

  .field,
  .search-filter-actions {
    flex-basis: 100%;
  }

  .search-filter-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .search-submit {
    width: 100%;
  }

  .search-reset {
    align-self: center;
  }
}
</style>
