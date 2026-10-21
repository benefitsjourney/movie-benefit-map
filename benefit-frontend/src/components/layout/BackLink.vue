<script setup>
import { Icon } from "@iconify/vue";
import arrowLeftIcon from "@iconify-icons/mdi/arrow-left";

defineProps({
  to: {
    type: [String, Object],
    required: true,
  },
});
</script>

<template>
  <RouterLink class="back-link" :to="to">
    <Icon :icon="arrowLeftIcon" aria-hidden="true" />
    <slot>返回</slot>
  </RouterLink>
</template>

<style scoped>
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  color: var(--detail-accent, #ff7049);
  font-size: var(--text-control);
  font-weight: 800;
  line-height: 1.4;
  text-decoration: none;
}

.back-link :deep(svg) {
  font-size: 1.15rem;
}

.back-link:hover {
  color: var(--detail-accent-deep, #9a4632);
}

.back-link:focus-visible {
  border-radius: 4px;
  outline: 2px solid var(--detail-accent, #ff7049);
  outline-offset: 3px;
}
</style>
