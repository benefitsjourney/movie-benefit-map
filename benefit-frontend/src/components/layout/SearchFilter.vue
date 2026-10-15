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

// 同步已套用的搜尋條件到表單
watch(
  [filters, () => props.fields.map((field) => field.key)],
  () => {
    for (const { key } of props.fields) {
      draft[key] = filters.value?.[key] ?? "";
    }
  },
  { immediate: true },
);

// 只有已套用搜尋條件才顯示清除按鈕
const showReset = computed(() =>
  props.fields.some(
    ({ key }) => String(filters.value?.[key] ?? "").trim() !== "",
  ),
);

// 統一下拉選單格式
function fieldOptions({ options }) {
  if (Array.isArray(options)) return options;

  return Object.entries(options ?? {}).map(([value, label]) => ({
    value,
    label,
  }));
}

// 套用搜尋條件
function search() {
  const next = {};

  for (const { key, type } of props.fields) {
    const value = draft[key] ?? "";
    next[key] = type === "select" ? value : String(value).trim();
  }

  Object.assign(draft, next);

  // 條件有變動才通知父元件
  const changed = Object.entries(next).some(
    ([key, value]) => value !== (filters.value?.[key] ?? ""),
  );

  if (changed) filters.value = next;
}

// 清除所有條件
function resetQuery() {
  for (const { key } of props.fields) {
    draft[key] = "";
  }

  search();
}
</script>

<template>
  <div class="search-filter" :style="themeStyle">
    <h2 v-if="title" class="search-filter-title">
      <Icon v-if="titleIcon" :icon="titleIcon" aria-hidden="true" />
      {{ title }}
    </h2>

    <form class="search-filter-grid" @submit.prevent="search">
      <div v-for="field in fields" :key="field.key" class="field">
        <label :for="`${fieldId}-${field.key}`">
          {{ field.label }}
        </label>

        <div class="field-control" :class="{ 'has-icon': field.icon }">
          <Icon v-if="field.icon" :icon="field.icon" aria-hidden="true" />

          <select v-if="field.type === 'select'" :id="`${fieldId}-${field.key}`" v-model="draft[field.key]"
            class="form-select" @change="search">
            <option v-if="field.emptyLabel !== null" value="">
              {{ field.emptyLabel ?? "全部" }}
            </option>

            <option v-for="option in fieldOptions(field)" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>

          <input v-else :id="`${fieldId}-${field.key}`" v-model="draft[field.key]"
            :type="field.type === 'search' ? 'search' : 'text'" class="form-control" :placeholder="field.placeholder" />
        </div>
      </div>

      <div class="search-filter-actions">
        <AppButton class="search-submit" type="submit" :icon="submitIcon" :color="theme">
          {{ submitLabel }}
        </AppButton>

        <AppButton v-if="showReset" class="search-reset" type="button" variant="reset" :color="theme" :icon="refreshIcon"
          @click="resetQuery">
          {{ resetLabel }}
        </AppButton>
      </div>
    </form>
  </div>
</template>

<style scoped>
.search-filter {
  margin-bottom: 1rem;
  padding: 1rem 1.25rem 1.25rem;
  border: 1.5px solid var(--c-border);
  border-radius: 22px;
  background: var(--c-card);
  box-shadow: var(--shadow-soft);
}

.search-filter-title {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin: 0 0 0.8rem;
  font-size: var(--text-section-title, 1.875rem);
  line-height: 1.3;
  font-weight: 900;
}

.search-filter-title svg,
.field-control>svg {
  color: var(--filter-accent);
}

.search-filter-title svg {
  font-size: 1.3rem;
}

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

.field label {
  display: block;
  margin-bottom: 0.3rem;
  color: var(--c-muted);
  font-size: var(--text-control, 1rem);
  font-weight: 700;
}

.field-control {
  position: relative;
}

.field-control>svg {
  position: absolute;
  top: 50%;
  left: 0.8rem;
  z-index: 1;
  font-size: 1.2rem;
  pointer-events: none;
  transform: translateY(-50%);
}

.form-select,
.form-control {
  width: 100%;
  min-height: 46px;
  border: 1.5px solid #eadfce;
  border-radius: 14px;
  background-color: #fff;
  color: var(--c-ink);
  font-size: var(--text-control, 1rem);
}

.form-select:focus,
.form-control:focus {
  border-color: var(--filter-accent);
  box-shadow: 0 0 0 3px var(--filter-focus);
}

.form-select:focus-visible,
.form-control:focus-visible {
  outline: 2px solid var(--filter-accent);
  outline-offset: 2px;
}

.field-control.has-icon .form-select,
.field-control.has-icon .form-control {
  padding-left: 2.5rem;
}

.search-filter-actions {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem 1rem;
}

@media (max-width: 519px) {
  .search-filter-title {
    font-size: 1.625rem;
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
