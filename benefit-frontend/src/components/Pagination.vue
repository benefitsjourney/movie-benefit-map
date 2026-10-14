<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { Icon } from "@iconify/vue";
import chevronLeft from "@iconify-icons/mdi/chevron-left";
import chevronRight from "@iconify-icons/mdi/chevron-right";
import AppButton from "@/components/AppButton.vue";

const props = defineProps({
  page: { type: Number, required: true },
  totalPages: { type: Number, required: true },
  label: { type: String, default: "分頁" },
});

const emit = defineEmits(["update:page"]);

const compact = ref(
  typeof window !== "undefined" &&
    window.matchMedia("(max-width: 640px)").matches,
);

let pagerQuery;

const pageNumbers = computed(() => {
  const total = props.totalPages;
  if (total <= 1) return [];

  const windowSize = compact.value ? 3 : 5;
  const offset = compact.value ? 1 : 2;
  let start = Math.max(1, props.page - offset);
  let end = Math.min(total, start + windowSize - 1);
  start = Math.max(1, end - windowSize + 1);

  return Array.from({ length: end - start + 1 }, (_, index) => start + index);
});

function select(next) {
  if (!Number.isInteger(next) || next < 1 || next > props.totalPages || next === props.page) return;
  emit("update:page", next);
}

function syncPager() {
  compact.value = pagerQuery.matches;
}

onMounted(() => {
  pagerQuery = window.matchMedia("(max-width: 640px)");
  syncPager();
  pagerQuery.addEventListener("change", syncPager);
});

onBeforeUnmount(() => {
  pagerQuery?.removeEventListener("change", syncPager);
});
</script>

<template>
  <nav class="pager" :aria-label="label">
    <AppButton
      type="button"
      color="coral"
      :icon="chevronLeft"
      :disabled="page <= 1"
      @click="select(page - 1)"
    >
      <span class="pager-label">上一頁</span>
    </AppButton>

    <div class="pager-pages">
      <button
        v-for="number in pageNumbers"
        :key="number"
        type="button"
        class="pager-page"
        :class="{ active: number === page }"
        :aria-current="number === page ? 'page' : undefined"
        :aria-label="`第 ${number} 頁`"
        @click="select(number)"
      >
        {{ number }}
      </button>
    </div>

    <AppButton
      type="button"
      color="coral"
      :disabled="page >= totalPages"
      @click="select(page + 1)"
    >
      <span class="pager-label">下一頁</span>
      <Icon :icon="chevronRight" aria-hidden="true" />
    </AppButton>
  </nav>
</template>

<style scoped>
.pager {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 0.55rem 0.7rem;
  margin-top: 1rem;
}

.pager-pages {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0.4rem;
}

.pager-page {
  min-width: 2.6rem;
  min-height: 2.6rem;
  padding: 0.2rem 0.55rem;
  border: 1.5px solid var(--c-border);
  border-radius: 999px;
  background: var(--c-card);
  color: var(--c-ink);
  font: inherit;
  font-size: var(--text-control, 1rem);
  font-weight: 800;
  cursor: pointer;
  box-shadow: var(--shadow-soft);
  transition:
    transform var(--ease),
    background var(--ease),
    color var(--ease);
}

.pager-page.active {
  border-color: transparent;
  background: #ff7049;
  color: #fff;
}

.pager-page:hover:not(.active) {
  transform: translateY(-2px);
}

.pager-page:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

.pager-page:focus-visible {
  outline: 2px solid #ff7049;
  outline-offset: 3px;
}

.pager :deep(.app-button) {
  position: relative;
}

@media (max-width: 640px) {
  .pager-label {
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

  .pager {
    flex-wrap: nowrap;
    gap: 0.35rem;
  }

  .pager :deep(.app-button) {
    width: auto;
    min-height: 2.5rem;
    padding-inline: 0.65rem;
  }

  .pager-page {
    min-width: 2.25rem;
    min-height: 2.25rem;
    padding-inline: 0.4rem;
  }
}
</style>
