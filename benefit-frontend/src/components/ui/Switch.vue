<script setup>
/**
 * 切換開關。外觀是滑動開關，實際仍是核取方塊，可用 v-model 綁布林值。
 */
defineOptions({ inheritAttrs: false });

const model = defineModel({ type: Boolean, default: false });

defineProps({
  disabled: { type: Boolean, default: false },
});
</script>

<template>
  <label class="app-switch">
    <input v-bind="$attrs" v-model="model" type="checkbox" role="switch" :disabled="disabled" />
    <span class="switch-track" aria-hidden="true">
      <span class="switch-thumb"></span>
    </span>
    <span v-if="$slots.default" class="switch-text">
      <slot />
    </span>
  </label>
</template>

<style scoped>
.app-switch {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-height: 3rem;
  margin: 0;
  cursor: pointer;
}

.app-switch input {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.switch-track {
  position: relative;
  flex: none;
  width: 2.75rem;
  height: 1.625rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--c-muted) 28%, white);
  transition: background var(--ease);
}

.switch-thumb {
  position: absolute;
  top: 0.1875rem;
  left: 0.1875rem;
  width: 1.25rem;
  height: 1.25rem;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 2px 6px rgb(41 43 53 / 18%);
  transition: transform var(--ease);
}

.app-switch input:checked + .switch-track {
  background: var(--form-accent, #8d72ed);
}

.app-switch input:checked + .switch-track .switch-thumb {
  transform: translateX(1.125rem);
}

.app-switch input:focus-visible + .switch-track {
  outline: 2px solid var(--form-accent, #8d72ed);
  outline-offset: 3px;
}

.app-switch input:disabled + .switch-track,
.app-switch input:disabled ~ .switch-text {
  opacity: 0.45;
}

.app-switch:has(input:disabled) {
  cursor: not-allowed;
}

.switch-text {
  color: var(--c-ink);
  font-size: var(--text-control);
  font-weight: 800;
  line-height: 1.4;
}
</style>
