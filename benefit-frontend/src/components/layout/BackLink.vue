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
  color: #5c4696;
  font-size: var(--text-control);
  font-weight: 800;
  line-height: 1.4;
  text-decoration: none;
}

.back-link :deep(svg) {
  font-size: 1.15rem;
}

.back-link:hover {
  color: #8d72ed;
}

.back-link:focus-visible {
  border-radius: 4px;
  outline: 2px solid #8d72ed;
  outline-offset: 3px;
}
</style>
