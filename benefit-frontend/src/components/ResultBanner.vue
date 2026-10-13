<script setup>
import { Icon } from "@iconify/vue";

defineProps({
  icon: { type: [Object, String], required: true },
  title: { type: String, required: true },
  count: { type: Number, default: 0 },
  unitText: { type: String, default: "" },
  emptyText: { type: String, default: "" },
  actionTo: { type: String, required: true },
  actionIcon: { type: [Object, String], required: true },
  actionLabel: { type: String, required: true },
});

</script>

<template>
  <div class="result-banner" aria-live="polite">
    <span class="result-mark" aria-hidden="true">
      <Icon :icon="icon" />
    </span>

    <div class="result-copy">
      <p class="result-title">{{ title }}</p>
      <p class="result-meta">
        <template v-if="count <= 0">{{ emptyText }}</template>
        <template v-else>
          <span class="result-count">{{ count }}</span>
          {{ unitText }}
        </template>
      </p>
    </div>

    <RouterLink class="result-scope" :to="actionTo">
      <Icon :icon="actionIcon" aria-hidden="true" />
      {{ actionLabel }}
    </RouterLink>
  </div>
</template>

<style scoped>
.result-banner {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  margin: 0.2rem 0 1rem;
  padding: 0.7rem 0.85rem;
  border: 1.5px solid var(--c-border);
  border-radius: 22px;
  background: var(--c-card);
  box-shadow: var(--shadow-soft);
  animation: result-fade 0.4s ease;
}

.result-mark {
  display: grid;
  flex: none;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: #f3b23c;
  color: #fff;
}

.result-mark svg {
  font-size: 1.35rem;
}

.result-copy {
  flex: 1;
  min-width: 0;
}

.result-title,
.result-meta {
  margin: 0;
  line-height: 1.35;
}

.result-title {
  color: var(--c-navy);
  font-size: var(--text-feature-title, 1.25rem);
  font-weight: 800;
}

.result-meta {
  color: #8d877e;
  font-size: var(--text-body, 1.0625rem);
  font-weight: 600;
}

.result-count {
  margin-right: 0.2rem;
  color: var(--c-brand);
  font-size: var(--text-feature-title, 1.25rem);
  font-weight: 800;
}

.result-scope {
  display: inline-flex;
  flex: none;
  align-items: center;
  gap: 0.3rem;
  padding: 0.45rem 0.95rem;
  border-radius: 999px;
  background: #f3b23c;
  color: #fff;
  font-size: var(--text-control, 1rem);
  font-weight: 800;
  white-space: nowrap;
  text-decoration: none;
  cursor: pointer;
  box-shadow: 0 6px 14px rgb(243 178 60 / 40%);
  transition:
    background var(--ease),
    box-shadow var(--ease),
    transform var(--ease);
}

.result-scope:hover {
  background: #e09a1a;
  box-shadow: 0 10px 18px rgb(224 154 26 / 42%);
  transform: translateY(-2px);
}

.result-scope:active {
  transform: scale(0.98);
}

.result-scope svg {
  font-size: 1.05rem;
}

.result-scope:focus-visible {
  outline: 2px solid #e09a1a;
  outline-offset: 3px;
}

@keyframes result-fade {
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
}

@media (max-width: 519px) {
  .result-banner {
    align-items: center;
    flex-wrap: wrap;
  }

  .result-copy {
    flex: 1 1 12rem;
  }

  .result-scope {
    margin-left: auto;
  }
}
</style>
