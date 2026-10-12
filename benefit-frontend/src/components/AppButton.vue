<script setup>
import { computed } from "vue";
import { Icon } from "@iconify/vue";

const props = defineProps({
  type: {
    type: String,
    default: "button",
  },
  // 沒指定時使用品牌橘色；可傳 CSS 顏色，例如 #79cdb5
  color: {
    type: String,
    default: "",
  },
  // 外觀名稱。傳 "reset" 切換成灰棕色文字的輕量按鈕
  variant: {
    type: String,
    default: "",
  },
  icon: {
    type: [Object, String],
    default: null,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
});

const buttonStyle = computed(() =>
  props.color ? { "--btn-bg": props.color } : undefined,
);
</script>

<template>
  <button
    class="app-button"
    :class="variant"
    :type="type"
    :disabled="disabled"
    :style="buttonStyle"
  >
    <slot name="icon">
      <Icon v-if="icon" :icon="icon" aria-hidden="true" />
    </slot>
    <slot />
  </button>
</template>

<style scoped>
.app-button {
  --btn-bg: var(--c-brand);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  min-height: 46px;
  padding: 0.55rem 1.4rem;
  border: none;
  border-radius: 999px;
  background: var(--btn-bg);
  color: #fff;
  font: inherit;
  font-size: var(--text-control, 1rem);
  line-height: 1.5;
  font-weight: 800;
  white-space: nowrap;
  cursor: pointer;
  box-shadow: 0 8px 16px color-mix(in srgb, var(--btn-bg) 28%, transparent);
  transition:
    transform var(--ease),
    box-shadow var(--ease);
}

.app-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 20px color-mix(in srgb, var(--btn-bg) 34%, transparent);
}

.app-button:active:not(:disabled) {
  transform: scale(0.98);
}

.app-button:disabled {
  cursor: not-allowed;
  opacity: 0.4;
  box-shadow: none;
}

.app-button:focus-visible {
  outline: 2px solid var(--btn-bg);
  outline-offset: 3px;
}

.app-button :deep(svg) {
  font-size: 1.2rem;
}

.app-button.reset {
  min-height: 0;
  padding: 0.15rem 0.1rem;
  background: transparent;
  color: var(--c-muted);
  font-weight: 700;
  box-shadow: none;
  transition: color var(--ease);
}

.app-button.reset :deep(svg) {
  font-size: 1rem;
  transition: transform var(--ease);
}

.app-button.reset:hover:not(:disabled) {
  transform: none;
  box-shadow: none;
  color: var(--c-brand);
}

.app-button.reset:hover:not(:disabled) :deep(svg) {
  transform: rotate(-22deg);
}

.app-button.reset:active:not(:disabled) {
  transform: none;
}

.app-button.reset:focus-visible {
  outline-color: var(--c-brand);
}
</style>
