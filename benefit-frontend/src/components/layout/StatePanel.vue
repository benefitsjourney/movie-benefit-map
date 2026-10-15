<script>
export const ACCENTS = {
  coral: "#ff7049",
  purple: "#8D72ED",
};
</script>

<script setup>
import { computed } from "vue";
import { Icon } from "@iconify/vue";
import AppButton from "@/components/ui/Button.vue";

const props = defineProps({
  variant: {
    type: String,
    required: true,
    validator: (value) => ["loading", "error"].includes(value),
  },
  title: { type: String, default: "" },
  message: { type: String, required: true },
  icon: { type: [Object, String], default: null },
  retryLabel: { type: String, default: "重新載入" },
  color: {
    type: String,
    default: "coral",
    validator: (value) => Object.hasOwn(ACCENTS, value),
  },
});

defineEmits(["retry"]);
const isLoading = computed(() => props.variant === "loading");
const accent = computed(() => ACCENTS[props.color] ?? ACCENTS.coral);
</script>

<template>
  <div class="state-panel" :role="isLoading ? 'status' : 'alert'" :style="{ '--panel-accent': accent }">
    <Icon v-if="isLoading && icon" :icon="icon" class="loading-icon" aria-hidden="true" />
    <h2 v-if="!isLoading && title">{{ title }}</h2>
    <p>{{ message }}</p>
    <AppButton v-if="!isLoading" type="button" :color="color" @click="$emit('retry')">
      {{ retryLabel }}
    </AppButton>
  </div>
</template>

<style scoped>
.state-panel {
  display: grid;
  justify-items: center;
  gap: 0.5rem;
  padding: 2rem 1rem;
  border: 1.5px solid var(--c-border);
  border-radius: 22px;
  background: var(--c-card);
  box-shadow: var(--shadow-soft);
  text-align: center;
}

.state-panel h2,
.state-panel p {
  margin: 0;
}

.state-panel h2 {
  font-size: var(--text-section-title, 1.875rem);
  line-height: 1.3;
  font-weight: 900;
}

.loading-icon {
  color: var(--panel-accent);
  font-size: 2.4rem;
  animation: float 1s ease-in-out infinite;
}

@keyframes float {
  50% {
    translate: 0 -7px;
  }
}
</style>
