<script setup>
import { computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import { Icon } from "@iconify/vue";
import loadingIcon from "@iconify-icons/mdi/loading";
import checkIcon from "@iconify-icons/mdi/check-circle-outline";
import { useBackendConnection } from "@/composables/useBackendConnection";

const { state, start } = useBackendConnection();
const route = useRoute();

const message = computed(() => {
  if (state.phase === "success") return "已成功連線到後端";
  if (state.slow) return "伺服器啟動時間較長，仍在嘗試連線...";
  return "正在連線到後端...";
});

const icon = computed(() => (state.phase === "success" ? checkIcon : loadingIcon));
const themeClass = computed(() => (route.path.startsWith("/admin") ? "is-admin" : "is-front"));

onMounted(start);
</script>

<template>
  <Transition name="backend-toast">
    <div v-if="state.visible" class="backend-toast-anchor">
      <div
        class="backend-toast"
        :class="[themeClass, state.phase === 'success' ? 'is-success' : 'is-connecting']"
        role="status"
        aria-live="polite"
      >
        <span class="backend-toast-mark" aria-hidden="true">
          <Icon
            :icon="icon"
            class="backend-toast-icon"
            :class="{ spin: state.phase !== 'success' }"
          />
        </span>
        <span>{{ message }}</span>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.backend-toast-anchor {
  position: fixed;
  top: 5.5rem;
  left: 50%;
  z-index: 40;
  transform: translateX(-50%);
  pointer-events: none;
}

.backend-toast {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  max-width: min(92vw, 36rem);
  padding: 0.4rem 1rem 0.4rem 0.4rem;
  border: 1.5px solid var(--toast-border);
  border-radius: 999px;
  font-size: var(--text-min);
  font-weight: 700;
  line-height: 1.4;
  color: var(--toast-ink);
  background: var(--toast-bg);
  box-shadow: var(--shadow-soft);
}

.backend-toast.is-front {
  --toast-bg: #fffdf8;
  --toast-border: #f3e0c4;
  --toast-ink: #1e2b45;
  --toast-mark: #ffc857;
  --toast-mark-ink: #1e2b45;
}

.backend-toast.is-front.is-success {
  --toast-border: #8fd9c4;
  --toast-mark: #8fd9c4;
  --toast-mark-ink: #0f6e5c;
}

.backend-toast.is-admin {
  --toast-bg: #ffffff;
  --toast-border: #e5dcf5;
  --toast-ink: #27346b;
  --toast-mark: #8d72ed;
  --toast-mark-ink: #ffffff;
}

.backend-toast.is-admin.is-success {
  --toast-border: #8fd9c4;
  --toast-mark: #8fd9c4;
  --toast-mark-ink: #0f6e5c;
}

.backend-toast.is-connecting {
  animation: backend-toast-bounce 1.15s ease-in-out infinite;
}

.backend-toast-mark {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 2rem;
  height: 2rem;
  border-radius: 999px;
  background: var(--toast-mark);
  color: var(--toast-mark-ink);
}

.backend-toast-icon {
  width: 1.2rem;
  height: 1.2rem;
}

.backend-toast-icon.spin {
  animation: backend-toast-spin 0.9s linear infinite;
}

.backend-toast-enter-active .backend-toast {
  animation: backend-toast-pop 0.45s ease;
}

.backend-toast-leave-active {
  transition: opacity 0.45s ease;
}

.backend-toast-leave-to {
  opacity: 0;
}

@keyframes backend-toast-bounce {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(6px);
  }
}

@keyframes backend-toast-pop {
  0% {
    opacity: 0;
    transform: translateY(-12px);
  }

  60% {
    opacity: 1;
    transform: translateY(4px);
  }

  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes backend-toast-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
