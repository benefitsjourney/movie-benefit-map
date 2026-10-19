<script>
import { reactive } from "vue";
import syncIcon from "@iconify-icons/mdi/sync";
import loadingIcon from "@iconify-icons/mdi/loading";
import checkIcon from "@iconify-icons/mdi/check-circle-outline";
import alertIcon from "@iconify-icons/mdi/alert-circle-outline";
import infoIcon from "@iconify-icons/mdi/information-outline";
import warningIcon from "@iconify-icons/mdi/alert-outline";

const VARIANTS = {
  confirm: { color: "purple", icon: syncIcon, dismissible: true, focus: "cancel" },
  loading: { color: "blue", icon: loadingIcon, dismissible: false, focus: "dialog" },
  success: { color: "green", icon: checkIcon, dismissible: true, focus: "confirm" },
  error: { color: "red", icon: alertIcon, dismissible: true, focus: "confirm" },
  info: { color: "yellow", icon: infoIcon, dismissible: true, focus: "confirm" },
  warning: { color: "orange", icon: warningIcon, dismissible: true, focus: "confirm" },
};

const DISMISSED = { isConfirmed: false, isDismissed: true };
const CONFIRMED = { isConfirmed: true, isDismissed: false };

const state = reactive({
  open: false,
  token: 0,
  variant: "confirm",
  title: "",
  text: "",
  rows: [],
  confirmText: "確定",
  cancelText: "取消",
});

let resolver = null;

function settle(result) {
  const resolve = resolver;
  resolver = null;
  state.open = false;
  resolve?.(result);
}

/** 開啟 Alert，回傳使用者確認或取消的結果。 */
export function openAppAlert(options = {}) {
  const previous = resolver;
  resolver = null;
  previous?.(DISMISSED);

  const variant = VARIANTS[options.variant] ? options.variant : "confirm";

  Object.assign(state, {
    open: true,
    token: state.token + 1,
    variant,
    title: options.title ?? "",
    text: options.text ?? "",
    rows: Array.isArray(options.rows) ? options.rows : [],
    confirmText: options.confirmText ?? "確定",
    cancelText: variant === "loading" ? "" : (options.cancelText ?? "取消"),
  });

  return new Promise((resolve) => {
    resolver = resolve;
  });
}

/** 更新目前 Alert 的標題或內容。 */
export function updateAppAlert(patch = {}) {
  if (!state.open) return;

  if (patch.title != null) state.title = patch.title;
  if (patch.text != null) state.text = patch.text;
  if (patch.rows != null) state.rows = patch.rows;
}

/** 關閉 Alert，並回傳取消結果。 */
export function closeAppAlert() {
  if (!state.open && !resolver) return;
  settle(DISMISSED);
}
</script>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { Icon } from "@iconify/vue";
import TicketCard from "@/components/ui/TicketCard.vue";

const dialogRef = ref(null);
const confirmRef = ref(null);
const cancelRef = ref(null);

const currentVariant = computed(() => VARIANTS[state.variant] ?? VARIANTS.confirm);

let previousOverflow = "";
let previousFocus = null;

function onConfirm() {
  settle(CONFIRMED);
}

function onDismiss() {
  if (currentVariant.value.dismissible) settle(DISMISSED);
}

/** 將焦點限制在彈窗內，並支援 Escape 關閉。 */
function onKeydown(event) {
  if (!state.open) return;

  if (event.key === "Escape") {
    if (!currentVariant.value.dismissible) return;
    event.preventDefault();
    onDismiss();
    return;
  }

  if (event.key !== "Tab") return;

  const nodes = [cancelRef.value, confirmRef.value].filter(Boolean);
  if (!nodes.length) {
    event.preventDefault();
    dialogRef.value?.focus();
    return;
  }

  const first = nodes[0];
  const last = nodes[nodes.length - 1];
  const active = document.activeElement;

  if (event.shiftKey && (active === first || !dialogRef.value?.contains(active))) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && (active === last || !dialogRef.value?.contains(active))) {
    event.preventDefault();
    first.focus();
  }
}

/** 依 Alert 類型設定初始焦點。 */
function focusPreferred() {
  const target = {
    cancel: cancelRef.value,
    confirm: confirmRef.value,
    dialog: dialogRef.value,
  }[currentVariant.value.focus];

  (target || confirmRef.value || dialogRef.value)?.focus();
}

/** 開啟時鎖定背景捲動，關閉後還原。 */
watch(
  () => state.open,
  (open) => {
    if (open) {
      previousOverflow = document.body.style.overflow;
      previousFocus = document.activeElement;
      document.body.style.overflow = "hidden";
      return;
    }

    document.body.style.overflow = previousOverflow;
    if (previousFocus?.isConnected) previousFocus.focus();
    previousFocus = null;
  },
);

/** 每次開啟新 Alert 都重新設定焦點。 */
watch(
  () => state.token,
  async (token) => {
    if (!token || !state.open) return;
    await nextTick();
    if (state.open && state.token === token) focusPreferred();
  },
);

onMounted(() => {
  window.addEventListener("keydown", onKeydown);
});

onUnmounted(() => {
  window.removeEventListener("keydown", onKeydown);

  if (state.open) {
    document.body.style.overflow = previousOverflow;
    settle(DISMISSED);
  }
});
</script>

<template>
  <Teleport to="body">
    <div v-if="state.open" class="alert-backdrop" @click.self="onDismiss">
      <div :key="state.token" ref="dialogRef" class="alert-dialog"
        :class="{ 'is-loading': state.variant === 'loading' }" role="alertdialog" aria-modal="true"
        aria-labelledby="app-alert-title" aria-describedby="app-alert-text" :aria-busy="state.variant === 'loading'"
        tabindex="-1">
        <TicketCard appear :color="currentVariant.color" :title="state.title" :detail="state.rows.length ? '' : state.text">
          <template #title>
            <span id="app-alert-title">{{ state.title }}</span>
          </template>

          <template v-if="!state.rows.length" #detail>
            <span id="app-alert-text">{{ state.text }}</span>
          </template>

          <div class="alert-hero" aria-hidden="true">
            <span class="alert-badge">
              <Icon :icon="currentVariant.icon" :class="{ 'is-spin': state.variant === 'loading' }" />
            </span>
          </div>

          <dl v-if="state.rows.length" id="app-alert-text" class="alert-stats">
            <div v-for="row in state.rows" :key="row.label">
              <dt>{{ row.label }}</dt>
              <dd>{{ row.value }}</dd>
            </div>
          </dl>

          <template v-if="state.variant !== 'loading'" #action>
            <button v-if="state.cancelText" ref="cancelRef" type="button" class="ticket-link" @click="onDismiss">
              {{ state.cancelText }}
            </button>

            <button ref="confirmRef" type="button" class="ticket-link is-dark" @click="onConfirm">
              {{ state.confirmText }}
            </button>
          </template>
        </TicketCard>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
/* 彈窗遮罩與外框 */
.alert-backdrop {
  position: fixed;
  z-index: 1200;
  display: grid;
  align-items: safe center;
  justify-items: center;
  padding: 1.25rem;
  overflow: auto;
  background: color-mix(in srgb, var(--c-ink) 46%, transparent);
  backdrop-filter: blur(3px);
  inset: 0;
}

.alert-dialog {
  width: min(32rem, 100%);
  outline: none;
}

.alert-dialog :deep(.ticket),
.alert-dialog :deep(.ticket:hover) {
  --alert-stub-pad: 0.85rem;
  --alert-action-size: 2.75rem;
  --stub-size: calc(var(--alert-stub-pad) * 2 + var(--alert-action-size) + var(--dash));
  min-height: 0;
}

/* 狀態圖示 */
.alert-hero {
  order: -1;
  align-self: flex-start;
}

.alert-badge {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border: 1px solid color-mix(in srgb, var(--ticket-ink) 18%, white);
  border-radius: 12px;
  background: var(--ticket-pale);
  color: var(--ticket-ink);
  font-size: var(--text-section-title);
}

.alert-badge :deep(svg) {
  display: block;
  width: 1em;
  height: 1em;
}

/* TicketCard 內容排版 */
.alert-dialog :deep(.ticket-body) {
  align-items: flex-start;
  padding: 1.25rem 1.35rem 1.15rem;
  text-align: left;
}

.alert-dialog :deep(.ticket-body h2) {
  margin-top: 0.85rem;
  font-size: var(--text-feature-title);
  line-height: 1.4;
}

.alert-dialog :deep(.ticket-detail) {
  margin-top: 0.55rem;
  font-size: var(--text-body);
  line-height: var(--leading-body);
}

.alert-stats {
  display: grid;
  width: 100%;
  margin: 0.95rem 0 0;
  overflow: hidden;
  border: 2px dashed var(--accent);
  border-radius: 14px;
}

.alert-stats > div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.72rem 0.9rem;
}

.alert-stats > div + div {
  border-top: 2px dashed var(--accent);
}

.alert-stats dt,
.alert-stats dd {
  margin: 0;
  font-size: var(--text-body);
  line-height: var(--leading-body);
}

.alert-stats dt {
  color: var(--ticket-ink);
  font-weight: 800;
}

.alert-stats dd {
  color: var(--c-ink);
  font-weight: 900;
  font-variant-numeric: tabular-nums;
}

.alert-dialog :deep(.ticket-stub) {
  justify-content: flex-end;
  gap: 0.6rem;
  padding: var(--alert-stub-pad) 1.15rem;
}

.alert-dialog :deep(.ticket-film) {
  display: none;
}

/* 操作按鈕 */
.alert-dialog .ticket-link {
  justify-content: center;
  min-width: 6rem;
  min-height: var(--alert-action-size);
  padding: 0.5rem 1.05rem;
  font-size: var(--text-control);
}

/* 載入動畫 */
.is-spin {
  animation: alert-spin 0.8s linear infinite;
}

@keyframes alert-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
