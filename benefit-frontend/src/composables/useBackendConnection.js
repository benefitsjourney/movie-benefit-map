import { reactive, readonly } from 'vue'
import http from '@/plugins/axios'

const REQUEST_TIMEOUT_MS = 10_000
const RETRY_DELAY_MS = 5_000
const SLOW_AFTER_MS = 60_000
const SUCCESS_VISIBLE_MS = 2_000
// 健康檢查很快成功時不提示；超過這段時間仍在連，才顯示「正在連線」
const CONNECTING_REVEAL_MS = 700

// 模組層級狀態：所有頁面共用同一次連線檢查，避免重複打 /health
const state = reactive({
  phase: 'idle',
  slow: false,
  visible: false,
})

let started = false
let inFlight = false
let stopped = false
let attemptToken = 0
let retryTimer = null
let slowTimer = null
let hideTimer = null
let revealTimer = null
let revealedConnecting = false

function clearTimer(timer) {
  if (timer != null) {
    clearTimeout(timer)
  }
}

function revealConnecting() {
  revealTimer = null
  if (stopped || state.phase !== 'connecting') return
  revealedConnecting = true
  state.visible = true
}

function markSuccess() {
  stopped = true
  clearTimer(retryTimer)
  clearTimer(slowTimer)
  clearTimer(revealTimer)
  retryTimer = null
  slowTimer = null
  revealTimer = null
  state.phase = 'success'
  state.slow = false
  clearTimer(hideTimer)
  hideTimer = null

  if (!revealedConnecting) {
    state.visible = false
    return
  }

  state.visible = true
  hideTimer = setTimeout(() => {
    state.visible = false
  }, SUCCESS_VISIBLE_MS)
}

function scheduleRetry() {
  clearTimer(revealTimer)
  revealConnecting()
  clearTimer(retryTimer)
  retryTimer = setTimeout(() => {
    retryTimer = null
    void attempt()
  }, RETRY_DELAY_MS)
}

async function attempt() {
  if (stopped || inFlight) return
  inFlight = true
  const current = ++attemptToken
  try {
    const response = await http.get('/health', { timeout: REQUEST_TIMEOUT_MS })
    if (stopped || current !== attemptToken) return
    if (response.data?.status === 'UP') {
      markSuccess()
      return
    }
    scheduleRetry()
  } catch {
    if (stopped || current !== attemptToken) return
    scheduleRetry()
  } finally {
    if (current === attemptToken) {
      inFlight = false
    }
  }
}

function start() {
  if (started) return
  started = true
  stopped = false
  revealedConnecting = false
  state.phase = 'connecting'
  state.slow = false
  state.visible = false
  clearTimer(slowTimer)
  clearTimer(revealTimer)
  slowTimer = setTimeout(() => {
    if (state.phase === 'connecting') {
      state.slow = true
    }
  }, SLOW_AFTER_MS)
  revealTimer = setTimeout(revealConnecting, CONNECTING_REVEAL_MS)
  void attempt()
}

export function useBackendConnection() {
  return {
    state: readonly(state),
    start,
  }
}
