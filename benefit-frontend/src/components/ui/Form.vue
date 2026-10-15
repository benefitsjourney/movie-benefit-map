<script>
export const THEMES = {
  coral: {
    "--form-accent": "#ff7049",
    "--form-focus": "rgb(255 112 73 / 20%)",
  },
  purple: {
    "--form-accent": "#8D72ED",
    "--form-focus": "rgb(141 114 237 / 20%)",
  },
};
</script>

<script setup>
/**
 * 各主題新增／修改共用的表單外框。
 * 負責排版、送出、載入中與載入失敗、表單錯誤。
 * 欄位、驗證與 API 由各主題頁面放進預設插槽。
 */
import { computed } from "vue";
import { Icon } from "@iconify/vue";
import alertIcon from "@iconify-icons/mdi/alert-circle-outline";
import saveIcon from "@iconify-icons/mdi/content-save-outline";
import AppButton from "@/components/ui/Button.vue";
import StatePanel from "@/components/layout/StatePanel.vue";

const props = defineProps({
  title: { type: String, default: "" },
  titleIcon: { type: [Object, String], default: null },
  loading: { type: Boolean, default: false },
  loadingMessage: { type: String, default: "正在載入資料…" },
  loadingIcon: { type: [Object, String], default: null },
  loadFailed: { type: Boolean, default: false },
  loadErrorTitle: { type: String, default: "目前無法載入資料" },
  loadErrorMessage: { type: String, default: "請確認連線後再試一次。" },
  submitting: { type: Boolean, default: false },
  formError: { type: String, default: "" },
  submitLabel: { type: String, default: "儲存" },
  submittingLabel: { type: String, default: "儲存中…" },
  submitIcon: { type: [Object, String], default: () => saveIcon },
  theme: {
    type: String,
    default: "coral",
    validator: (value) => Object.hasOwn(THEMES, value),
  },
});

defineEmits(["submit", "retry"]);

const themeStyle = computed(() => THEMES[props.theme] ?? THEMES.coral);
</script>

<template>
  <StatePanel
    v-if="loading"
    variant="loading"
    :message="loadingMessage"
    :icon="loadingIcon"
    :color="theme"
  />

  <StatePanel
    v-else-if="loadFailed"
    variant="error"
    :title="loadErrorTitle"
    :message="loadErrorMessage"
    :color="theme"
    @retry="$emit('retry')"
  />

  <form
    v-else
    class="app-form"
    :style="themeStyle"
    novalidate
    :aria-busy="submitting"
    @submit.prevent="$emit('submit')"
  >
    <h2 v-if="title" class="form-title">
      <Icon v-if="titleIcon" :icon="titleIcon" aria-hidden="true" />
      {{ title }}
    </h2>

    <p v-if="formError" class="form-banner" role="alert">
      <Icon :icon="alertIcon" aria-hidden="true" />
      <span>{{ formError }}</span>
    </p>

    <div class="form-grid">
      <slot />
    </div>

    <div class="form-actions">
      <AppButton type="submit" :color="theme" :icon="submitIcon" :disabled="submitting">
        {{ submitting ? submittingLabel : submitLabel }}
      </AppButton>
    </div>
  </form>
</template>

<style scoped>
.app-form {
  margin-bottom: 1rem;
  padding: 1.15rem 1.35rem 1.35rem;
  border: 1.5px solid var(--c-border);
  border-radius: 22px;
  background: var(--c-card);
  box-shadow: var(--shadow-soft);
}

.form-title {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin: 0 0 1rem;
  font-size: var(--text-section-title);
  line-height: 1.3;
  font-weight: 900;
}

.form-title svg {
  color: var(--form-accent);
  font-size: var(--text-card-title);
}

.form-banner {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  margin: 0 0 1rem;
  padding: 0.75rem 0.95rem;
  border-radius: 14px;
  background: #fff1f3;
  color: #9f1239;
  font-size: var(--text-control);
  font-weight: 700;
  line-height: 1.5;
}

.form-banner svg {
  flex: none;
  margin-top: 0.15rem;
  font-size: var(--text-card-title);
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 0.85rem 1.15rem;
  margin-top: 1.35rem;
  padding-top: 1.15rem;
  border-top: 1.5px solid var(--c-border);
}

@media (max-width: 519px) {
  .form-title {
    font-size: var(--text-feature-title);
  }

  .form-actions {
    flex-direction: column-reverse;
    align-items: stretch;
  }

  .form-actions :deep(.app-button) {
    width: 100%;
  }
}
</style>

<!-- 插槽欄位由主題頁面提供，樣式掛在表單根節點上，讓各主題共用同一套控制項。 -->
<style>
.app-form .form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem 1.1rem;
}

.app-form .form-section {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem 1.1rem;
  margin: 0;
  padding: 0;
  border: 0;
}

.app-form .form-section + .form-section {
  padding-top: 1.2rem;
  border-top: 1.5px solid var(--c-border);
}

.app-form .form-section-title {
  grid-column: 1 / -1;
  margin: 0;
  font-size: var(--text-card-title);
  line-height: 1.3;
  font-weight: 900;
}

.app-form .form-field.is-wide {
  grid-column: 1 / -1;
}

.app-form .field-label {
  display: block;
  margin-bottom: 0.3rem;
  color: var(--c-muted);
  font-size: var(--text-control);
  font-weight: 700;
}

.app-form .field-required {
  color: var(--form-accent);
}

.app-form .form-control,
.app-form .form-select {
  display: block;
  width: 100%;
  min-height: 3rem;
  padding: 0.5rem 0.9rem;
  border: 1.5px solid var(--c-border);
  border-radius: 14px;
  background-color: #fff;
  color: var(--c-ink);
  font-family: inherit;
  font-size: var(--text-control);
  line-height: 1.5;
  box-shadow: none;
}

.app-form .form-control::placeholder {
  color: color-mix(in srgb, var(--c-muted) 72%, white);
}

.app-form .form-control:focus,
.app-form .form-select:focus {
  border-color: var(--form-accent);
  outline: none;
  box-shadow: 0 0 0 3px var(--form-focus);
}

.app-form .form-control:focus-visible,
.app-form .form-select:focus-visible {
  outline: 2px solid var(--form-accent);
  outline-offset: 2px;
}

.app-form .form-control.is-invalid,
.app-form .form-select.is-invalid {
  border-color: #e11d48;
}

.app-form .form-control.is-invalid:focus,
.app-form .form-select.is-invalid:focus {
  box-shadow: 0 0 0 3px rgb(225 29 72 / 16%);
}

.app-form .field-hint {
  margin: 0.35rem 0 0;
  color: var(--c-muted);
  font-size: var(--text-min);
  font-weight: 600;
  line-height: 1.5;
}

.app-form .field-error {
  margin: 0.35rem 0 0;
  color: #9f1239;
  font-size: var(--text-min);
  font-weight: 700;
  line-height: 1.5;
}

@media (max-width: 719px) {
  .app-form .form-grid,
  .app-form .form-section {
    grid-template-columns: 1fr;
  }
}
</style>
