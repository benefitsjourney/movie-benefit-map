<script>
export const COLORS = {
  coral: {
    "--btn-bg": "#ff7049",
    "--btn-muted": "#5c564e",
  },
  purple: {
    "--btn-bg": "#8D72ED",
    "--btn-muted": "#6d6882",
  },
};
</script>

<script setup>
import { computed } from "vue";
import { Icon } from "@iconify/vue";

const props = defineProps({
  type: {
    type: String,
    default: "button",
  },
  color: {
    type: String,
    default: "coral",
    validator: (value) => Object.hasOwn(COLORS, value),
  },
  // 外觀名稱。傳 "reset" 切換成淡色文字的輕量按鈕
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

const buttonStyle = computed(() => COLORS[props.color] ?? COLORS.coral);
</script>

<template>
  <button class="app-button" :class="variant" :type="type" :disabled="disabled" :style="buttonStyle">
    <slot name="icon">
      <Icon v-if="icon" :icon="icon" aria-hidden="true" />
    </slot>
    <slot />
  </button>
</template>

<style scoped>
.app-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  min-height: 3rem;
  padding: 0.6rem 1.45rem;
  border: none;
  border-radius: 999px;
  background: var(--btn-bg);
  color: #fff;
  font: inherit;
  font-size: var(--text-control);
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
  font-size: var(--text-card-title);
}

.app-button.reset {
  min-height: 0;
  padding: 0.15rem 0.1rem;
  background: transparent;
  color: var(--btn-muted);
  font-weight: 700;
  box-shadow: none;
  transition: color var(--ease);
}

.app-button.reset :deep(svg) {
  font-size: var(--text-min);
  transition: transform var(--ease);
}

.app-button.reset:hover:not(:disabled) {
  transform: none;
  box-shadow: none;
  color: var(--btn-bg);
}

.app-button.reset:hover:not(:disabled) :deep(svg) {
  transform: rotate(-22deg);
}

.app-button.reset:active:not(:disabled) {
  transform: none;
}

.app-button.reset:focus-visible {
  outline-color: var(--btn-bg);
}
</style>
