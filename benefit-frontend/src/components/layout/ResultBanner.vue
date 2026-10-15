<script>
export const THEMES = {
  yellow: {
    "--result-mark": "#f3b23c",
    "--result-mark-hover": "#e09a1a",
    "--result-meta": "#8d877e",
    "--result-count": "#ff7049",
    "--result-title": "#1e2b45",
  },
  purple: {
    "--result-mark": "#8574df",
    "--result-mark-hover": "#5141a3",
    "--result-meta": "#6d6882",
    "--result-count": "#8D72ED",
    "--result-title": "#27346B",
  },
};
</script>

<script setup>
import { computed } from "vue";
import { Icon } from "@iconify/vue";

const props = defineProps({
  icon: { type: [Object, String], required: true },
  title: { type: String, required: true },
  count: { type: Number, default: 0 },
  unitText: { type: String, default: "" },
  emptyText: { type: String, default: "" },
  actionTo: { type: String, required: true },
  actionIcon: { type: [Object, String], required: true },
  actionLabel: { type: String, required: true },
  theme: {
    type: String,
    default: "yellow",
    validator: (value) => Object.hasOwn(THEMES, value),
  },
});

const themeStyle = computed(() => THEMES[props.theme] ?? THEMES.yellow);
</script>

<template>
  <div class="result-banner" aria-live="polite" :style="themeStyle">
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
  gap: 0.9rem;
  margin: 0.2rem 0 1.15rem;
  padding: 0.9rem 1rem;
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
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: var(--result-mark, #f3b23c);
  color: #fff;
}

.result-mark svg {
  font-size: var(--text-feature-title);
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
  color: var(--result-title);
  font-size: var(--text-feature-title);
  font-weight: 800;
}

.result-meta {
  margin-top: 0.15rem;
  color: var(--result-meta, #8d877e);
  font-size: var(--text-body);
  line-height: 1.5;
  font-weight: 600;
}

.result-count {
  margin-right: 0.2rem;
  color: var(--result-count);
  font-size: var(--text-feature-title);
  font-weight: 800;
}

.result-scope {
  display: inline-flex;
  flex: none;
  align-items: center;
  gap: 0.3rem;
  padding: 0.45rem 0.95rem;
  border-radius: 999px;
  background: var(--result-mark, #f3b23c);
  color: #fff;
  font-size: var(--text-control);
  font-weight: 800;
  white-space: nowrap;
  text-decoration: none;
  cursor: pointer;
  box-shadow: 0 6px 14px color-mix(in srgb, var(--result-mark, #f3b23c) 40%, transparent);
  transition:
    background var(--ease),
    box-shadow var(--ease),
    transform var(--ease);
}

.result-scope:hover {
  background: var(--result-mark-hover, #e09a1a);
  box-shadow: 0 10px 18px color-mix(in srgb, var(--result-mark-hover, #e09a1a) 42%, transparent);
  transform: translateY(-2px);
}

.result-scope:active {
  transform: scale(0.98);
}

.result-scope svg {
  font-size: var(--text-control);
}

.result-scope:focus-visible {
  outline: 2px solid var(--result-mark-hover, #e09a1a);
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
