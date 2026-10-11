<script setup>
import { computed, reactive, useId, watch } from "vue";
import { Icon } from "@iconify/vue";
import searchIcon from "@iconify-icons/mdi/magnify";
import refreshIcon from "@iconify-icons/mdi/refresh";
import AppButton from "@/components/AppButton.vue";

const filters = defineModel({
  type: Object,
  default: () => ({}),
});

const props = defineProps({
  title: {
    type: String,
    default: "搜尋",
  },
  titleIcon: {
    type: [Object, String],
    default: () => searchIcon,
  },
  submitLabel: {
    type: String,
    default: "搜尋",
  },
  submitIcon: {
    type: [Object, String],
    default: () => searchIcon,
  },
  submitColor: {
    type: String,
    default: "",
  },
  resetLabel: {
    type: String,
    default: "清除條件",
  },
  // { key, label, type: "search" | "text" | "select", placeholder?, emptyLabel?, icon?, options? }
  // options 可為 { value, label }[]，或 { 值: 標籤 }
  fields: {
    type: Array,
    default: () => [],
  },
});

const fieldId = useId();
const draft = reactive({});

function readValue(source, key) {
  return source?.[key] ?? "";
}

function syncDraft(source) {
  for (const field of props.fields) {
    draft[field.key] = readValue(source, field.key);
  }
}

syncDraft(filters.value);

watch(filters, (value) => {
  syncDraft(value);
});

watch(
  () => props.fields.map((field) => field.key).join("|"),
  () => syncDraft(filters.value),
);

const showReset = computed(() =>
  props.fields.some((field) => {
    const draftValue = String(draft[field.key] ?? "").trim();
    const appliedValue = String(readValue(filters.value, field.key)).trim();
    return draftValue || appliedValue;
  }),
);

function fieldOptions(field) {
  const options = field.options ?? [];
  if (Array.isArray(options)) return options;
  return Object.entries(options).map(([value, label]) => ({ value, label }));
}

function emptyLabel(field) {
  return field.emptyLabel === undefined ? "全部" : field.emptyLabel;
}

function search() {
  const next = {};

  for (const field of props.fields) {
    const raw = draft[field.key] ?? "";
    next[field.key] = field.type === "select" ? raw : String(raw).trim();
    draft[field.key] = next[field.key];
  }

  const current = filters.value ?? {};
  const changed = props.fields.some(
    (field) => readValue(current, field.key) !== next[field.key],
  );

  if (changed) filters.value = next;
}

function resetQuery() {
  for (const field of props.fields) draft[field.key] = "";
  search();
}
</script>

<template>
  <div class="search-filter">
    <h2 v-if="title" class="search-filter-title">
      <Icon v-if="titleIcon" :icon="titleIcon" aria-hidden="true" />
      {{ title }}
    </h2>

    <form class="search-filter-grid" @submit.prevent="search">
      <div v-for="field in fields" :key="field.key" class="field">
        <label :for="`${fieldId}-${field.key}`">{{ field.label }}</label>
        <div class="field-control" :class="{ 'has-icon': field.icon }">
          <Icon v-if="field.icon" :icon="field.icon" aria-hidden="true" />

          <select
            v-if="field.type === 'select'"
            :id="`${fieldId}-${field.key}`"
            v-model="draft[field.key]"
            class="form-select"
          >
            <option v-if="emptyLabel(field) != null" value="">
              {{ emptyLabel(field) }}
            </option>
            <option
              v-for="option in fieldOptions(field)"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </option>
          </select>

          <input
            v-else
            :id="`${fieldId}-${field.key}`"
            v-model="draft[field.key]"
            :type="field.type === 'search' ? 'search' : 'text'"
            class="form-control"
            :placeholder="field.placeholder"
          />
        </div>
      </div>

      <div class="search-filter-actions">
        <AppButton
          class="search-submit"
          type="submit"
          :icon="submitIcon"
          :color="submitColor"
        >
          {{ submitLabel }}
        </AppButton>

        <AppButton
          v-if="showReset"
          class="search-reset"
          type="button"
          variant="reset"
          :icon="refreshIcon"
          @click="resetQuery"
        >
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
  font-size: 1.1rem;
  font-weight: 900;
}

.search-filter-title svg {
  color: var(--c-brand);
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
  font-weight: 700;
}

.field-control {
  position: relative;
}

.field-control > svg {
  position: absolute;
  top: 50%;
  left: 0.8rem;
  z-index: 1;
  color: var(--c-brand);
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
  font-size: 1rem;
}

.form-select:focus,
.form-control:focus {
  border-color: var(--c-brand);
  box-shadow: 0 0 0 3px rgb(255 112 73 / 20%);
}

.form-select:focus-visible,
.form-control:focus-visible {
  outline: 2px solid var(--c-brand);
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
