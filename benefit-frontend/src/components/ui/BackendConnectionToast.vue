<script setup>
import { computed, onMounted, onUnmounted, reactive } from "vue";
import { useRoute } from "vue-router";
import { Icon } from "@iconify/vue";
import http from "@/plugins/axios";
import loadingIcon from "@iconify-icons/mdi/loading";
import checkIcon from "@iconify-icons/mdi/check-circle-outline";
import alertIcon from "@iconify-icons/mdi/alert-circle-outline";

/** 1秒內連上後端時，連線中與成功提示都不顯示 */
const SILENT_SUCCESS_MS = 1_000;

/** 連線超過4分鐘時，顯示「連線有點久」 */
const SLOW_AFTER_MS = 4 * 60 * 1000;

/** 連線超過5分鐘時，顯示「連線失敗」 */
const FAIL_AFTER_MS = 5 * 60 * 1000;

/** 成功提示顯示3.5秒 */
const SUCCESS_VISIBLE_MS = 3_500;

/** 連線失敗後隔2秒再試，偵測後端是否啟動成功 */
const RETRY_GAP_MS = 2_000;

/** 狀態 */
const state = reactive({
  phase: "idle",
  slow: false,
  visible: false,
  elapsedSeconds: 0,
});

const route = useRoute();

let stopped = false;
let attemptToken = 0;
let startedAt = 0;
let abortController = null;
let tickTimer = null;
let slowTimer = null;
let failTimer = null;
let hideTimer = null;
let revealTimer = null;
let retryTimer = null;
let releaseRetryWait = null;

function clearSchedule() {
  clearTimeout(slowTimer);
  clearTimeout(failTimer);
  clearTimeout(hideTimer);
  clearTimeout(revealTimer);
  clearTimeout(retryTimer);
  clearInterval(tickTimer);
  slowTimer = failTimer = hideTimer = revealTimer = retryTimer = tickTimer = null;
  releaseRetryWait?.();
  releaseRetryWait = null;
}

function waitRetry(ms) {
  return new Promise((resolve) => {
    releaseRetryWait = resolve;
    retryTimer = setTimeout(() => {
      retryTimer = null;
      releaseRetryWait = null;
      resolve();
    }, ms);
  });
}

/** 以實際開始時間計算耗時，避免計時器延遲造成秒數不準 */
function updateElapsedTime() {
  state.elapsedSeconds = Math.max(0, Math.floor((Date.now() - startedAt) / 1000));
}

/** 成功與逾時共用收尾流程，避免舊請求或計時器再次修改畫面 */
function finish(phase) {
  stopped = true;
  updateElapsedTime();
  const showSuccess = state.visible;
  clearSchedule();
  state.phase = phase;
  state.slow = false;

  if (phase === "failed") {
    state.visible = true;
    attemptToken += 1;
    abortController?.abort();
    abortController = null;
    return;
  }

  state.visible = showSuccess;
  if (!state.visible) return;

  hideTimer = setTimeout(() => {
    state.visible = false;
    hideTimer = null;
  }, SUCCESS_VISIBLE_MS);
}

/** 開始連線 */
async function begin() {
  if (state.phase === "connecting") return;
  stopped = false;
  startedAt = Date.now();
  state.phase = "connecting";
  state.slow = false;
  state.visible = false;
  state.elapsedSeconds = 0;
  clearSchedule();
  abortController?.abort();

  const current = ++attemptToken;

  tickTimer = setInterval(() => {
    if (state.phase === "connecting") updateElapsedTime();
  }, 1000);

  slowTimer = setTimeout(() => {
    slowTimer = null;
    if (state.phase === "connecting") state.slow = true;
  }, SLOW_AFTER_MS);

  failTimer = setTimeout(() => {
    failTimer = null;
    if (stopped || state.phase !== "connecting") return;
    finish("failed");
  }, FAIL_AFTER_MS);

  revealTimer = setTimeout(() => {
    revealTimer = null;
    if (stopped || state.phase !== "connecting") return;
    state.visible = true;
  }, SILENT_SUCCESS_MS);

  while (!stopped && current === attemptToken && state.phase === "connecting") {
    const remaining = FAIL_AFTER_MS - (Date.now() - startedAt);
    if (remaining <= 0) {
      finish("failed");
      return;
    }

    const controller = new AbortController();
    abortController = controller;
    let connected = false;

    try {
      const response = await http.get("/health", {
        timeout: remaining,
        signal: controller.signal,
      });
      connected = response.data?.status === "UP";
    } catch {
      // 後端尚未起來或資料庫暫時不可用時繼續重試，直到 5 分鐘才顯示失敗
      connected = false;
    } finally {
      if (abortController === controller) abortController = null;
    }

    if (stopped || current !== attemptToken || state.phase !== "connecting") return;
    if (connected) {
      finish("success");
      return;
    }

    const gap = Math.min(RETRY_GAP_MS, FAIL_AFTER_MS - (Date.now() - startedAt));
    if (gap <= 0) {
      finish("failed");
      return;
    }
    await waitRetry(gap);
  }
}

/** 統一格式化連線耗時，讓成功與等待訊息使用相同格式 */
const elapsedText = computed(() => {
  const total = Math.max(0, state.elapsedSeconds);
  const minutes = Math.floor(total / 60);
  const seconds = total % 60;
  if (state.phase === "success" && total === 0) return "不到 1 秒";
  return minutes === 0 ? `${seconds} 秒` : `${minutes} 分 ${seconds} 秒`;
});

/** 訊息 */
const message = computed(() => {
  if (state.phase === "success") return `連線成功，花費 ${elapsedText.value}`;
  if (state.phase === "failed") return "連線失敗，請稍後再試";
  if (state.slow) return `伺服器回應較慢，請稍候（已等待 ${elapsedText.value}）`;
  return `正在連線，請稍候（已等待 ${elapsedText.value}）`;
});

/** 圖示 */
const icon = computed(() => {
  if (state.phase === "success") return checkIcon;
  if (state.phase === "failed") return alertIcon;
  return loadingIcon;
});

/** 樣式 */
const toastClasses = computed(() => [
  route.path.startsWith("/admin") ? "is-admin" : "is-front",
  `is-${state.phase}`,
]);

onMounted(begin);
onUnmounted(() => {
  stopped = true;
  attemptToken += 1;
  abortController?.abort();
  abortController = null;
  clearSchedule();
});
</script>

<template>
  <Transition name="backend-toast">
    <div v-if="state.visible" class="backend-toast-anchor" :class="{ 'is-interactive': state.phase === 'failed' }">
      <div class="backend-toast" :class="toastClasses" :role="state.phase === 'failed' ? 'alert' : 'status'"
        aria-live="polite">
        <span class="backend-toast-mark" aria-hidden="true">
          <Icon :icon="icon" class="backend-toast-icon" :class="{ spin: state.phase === 'connecting' }" />
        </span>
        <span>{{ message }}</span>
        <button v-if="state.phase === 'failed'" type="button" class="backend-toast-retry" @click="begin">
          重新連線
        </button>
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

.backend-toast-anchor.is-interactive {
  pointer-events: auto;
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

.backend-toast.is-admin {
  --toast-bg: #ffffff;
  --toast-border: #e5dcf5;
  --toast-ink: #27346b;
  --toast-mark: #8d72ed;
  --toast-mark-ink: #ffffff;
}

.backend-toast.is-success {
  --toast-border: #8fd9c4;
  --toast-mark: #8fd9c4;
  --toast-mark-ink: #0f6e5c;
}

.backend-toast.is-front.is-failed,
.backend-toast.is-admin.is-failed {
  --toast-bg: #fff6f6;
  --toast-border: #f0b4b4;
  --toast-ink: #9b2c2c;
  --toast-mark: #e85d5d;
  --toast-mark-ink: #ffffff;
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

.backend-toast-retry {
  flex: 0 0 auto;
  margin-left: 0.15rem;
  padding: 0.28rem 0.75rem;
  border: 1.5px solid #e8a0a0;
  border-radius: 999px;
  background: #fff6f6;
  color: #9b2c2c;
  font: inherit;
  font-size: 0.92rem;
  font-weight: 700;
  line-height: 1.2;
  cursor: pointer;
}

.backend-toast-retry:hover {
  background: #fdeeee;
  border-color: #e08585;
}

.backend-toast-retry:focus-visible {
  outline: 2px solid #9b2c2c;
  outline-offset: 2px;
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
