<script>
import { reactive } from "vue";
import syncIcon from "@iconify-icons/mdi/sync";
import loadingIcon from "@iconify-icons/mdi/loading";
import checkIcon from "@iconify-icons/mdi/check-circle-outline";
import alertIcon from "@iconify-icons/mdi/alert-circle-outline";
import infoIcon from "@iconify-icons/mdi/information-outline";
import warningIcon from "@iconify-icons/mdi/alert-outline";

/** 定義 Alert 類型對應的顏色、圖示、可否關閉與預設焦點 */
const VARIANTS = {
  confirm: { color: "purple", icon: syncIcon, dismissible: true, focus: "cancel" },
  loading: { color: "blue", icon: loadingIcon, dismissible: false, focus: "dialog" },
  success: { color: "green", icon: checkIcon, dismissible: true, focus: "confirm" },
  error: { color: "red", icon: alertIcon, dismissible: true, focus: "confirm" },
  info: { color: "yellow", icon: infoIcon, dismissible: true, focus: "confirm" },
  warning: { color: "orange", icon: warningIcon, dismissible: true, focus: "confirm" },
};

/** 集中管理彈窗預設文案，減少開啟時的重複預設值 */
const ALERT_DEFAULTS = {
  title: "",
  text: "",
  confirmText: "確定",
  cancelText: "取消",
  extraText: "",
  fileButtonText: "選擇檔案",
};

/** 統一三種按鈕操作的回傳結果格式，供呼叫端判斷 */
const DISMISSED = { isConfirmed: false, isDismissed: true, isExtra: false };
const CONFIRMED = { isConfirmed: true, isDismissed: false, isExtra: false };
const EXTRA = { isConfirmed: false, isDismissed: false, isExtra: true };

/** 全域共用的響應式彈窗狀態，包含文字、表單及上傳檔案資訊 */
const state = reactive({
  open: false,
  token: 0,
  variant: "confirm",
  title: "",
  text: "",
  rows: [],
  confirmText: "確定",
  cancelText: "取消",
  extraText: "",
  fields: [],
  fieldValues: {},
  fieldErrors: {},
  requireFile: false,
  fileAccept: "",
  fileButtonText: "選擇檔案",
  fileName: "",
  fileError: "",
  hasFile: false,
});

/** resolver 用來完成 openAppAlert 的 Promise；檔案物件獨立儲存，不放入響應式狀態 */
let resolver = null;
let selectedFile = null;

/** 關閉彈窗並結束本次等待中的 Promise */
function settle(result) {
  const resolve = resolver;
  resolver = null;
  state.open = false;
  resolve?.(result);
}

/** 開啟 Alert；先結束前一個彈窗，再依設定重設欄位並等待使用者操作 */
export function openAppAlert(options = {}) {
  const previous = resolver;
  resolver = null;
  previous?.(DISMISSED);

  const variant = VARIANTS[options.variant] ? options.variant : "confirm";
  const fields = variant === "loading" || !Array.isArray(options.fields) ? [] : options.fields;
  const fieldValues = Object.fromEntries(
    fields.map(({ key, value }) => [key, value == null ? "" : String(value)]),
  );

  selectedFile = null;

  Object.assign(state, {
    open: true,
    token: state.token + 1,
    variant,
    title: options.title ?? ALERT_DEFAULTS.title,
    text: options.text ?? ALERT_DEFAULTS.text,
    rows: Array.isArray(options.rows) ? options.rows : [],
    confirmText: options.confirmText ?? ALERT_DEFAULTS.confirmText,
    cancelText: variant === "loading" ? "" : (options.cancelText ?? ALERT_DEFAULTS.cancelText),
    extraText: variant === "loading" ? "" : (options.extraText ?? ALERT_DEFAULTS.extraText),
    fields,
    fieldValues,
    fieldErrors: {},
    requireFile: variant !== "loading" && Boolean(options.file),
    fileAccept: options.file?.accept ?? "",
    fileButtonText: options.file?.buttonText ?? ALERT_DEFAULTS.fileButtonText,
    fileName: "",
    fileError: "",
    hasFile: false,
  });

  return new Promise((resolve) => {
    resolver = resolve;
  });
}

/** 載入等執行過程中，僅更新目前彈窗的標題、內容或統計列 */
export function updateAppAlert(patch = {}) {
  if (!state.open) return;

  for (const key of ["title", "text", "rows"]) {
    if (patch[key] != null) state[key] = patch[key];
  }
}

/** 由外部主動關閉 Alert，並通知呼叫端操作已取消 */
export function closeAppAlert() {
  if (!state.open && !resolver) return;
  settle(DISMISSED);
}
</script>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { Icon } from "@iconify/vue";
import TicketCard from "@/components/ui/TicketCard.vue";

/** 保存彈窗和按鈕的 DOM 參照，用於焦點管理及檔案選取 */
const dialogRef = ref(null);
const confirmRef = ref(null);
const cancelRef = ref(null);
const extraRef = ref(null);
const fileInputRef = ref(null);

/** 根據當前類型讀取外觀及互動規則 */
const currentVariant = computed(() => VARIANTS[state.variant] ?? VARIANTS.confirm);
/** 未提供必填檔案或存在空白欄位時，停用確認按鈕 */
const confirmBlocked = computed(() => {
  if (state.requireFile && !state.hasFile) return true;
  return state.fields.some((field) => !String(state.fieldValues[field.key] ?? "").trim());
});
/** 依實際顯示內容建立無障礙描述 ID */
const describedBy = computed(() =>
  [state.text ? "app-alert-text" : "", state.rows.length ? "app-alert-rows" : ""]
    .filter(Boolean)
    .join(" "),
);

let previousOverflow = "";
let previousFocus = null;

/** 按下確認後檢查輸入、執行欄位驗證，再回傳整理後的資料 */
function onConfirm() {
  if (confirmBlocked.value) return;

  const errors = Object.fromEntries(
    state.fields.flatMap((field) => {
      const value = String(state.fieldValues[field.key] ?? "").trim();
      const message = field.validate?.(value) ?? "";
      return message ? [[field.key, message]] : [];
    }),
  );
  state.fieldErrors = errors;
  if (Object.keys(errors).length) return;

  const file = selectedFile;
  const values = Object.fromEntries(
    state.fields.map(({ key }) => [key, String(state.fieldValues[key] ?? "").trim()]),
  );
  selectedFile = null;
  state.hasFile = false;
  settle({ ...CONFIRMED, values, file });
}

/** 觸發隱藏的原生檔案選擇器 */
function openFilePicker() {
  fileInputRef.value?.click();
}

/** 記錄選擇的檔案並更新檔名顯示 */
function onFileChange(event) {
  const file = event.target.files?.[0] ?? null;
  event.target.value = "";

  if (!file) return;

  selectedFile = file;
  state.hasFile = true;
  state.fileName = file.name;
  state.fileError = "";
}

/** 額外操作按鈕：回傳 isExtra，供呼叫端執行其他導向 */
function onExtra() {
  settle(EXTRA);
}

/** 僅允許可關閉的彈窗以取消操作結束 */
function onDismiss() {
  if (currentVariant.value.dismissible) settle(DISMISSED);
}

/** 攔截 Escape 與 Tab，防止焦點離開彈窗並允許適當關閉 */
function onKeydown(event) {
  if (!state.open) return;

  if (event.key === "Escape") {
    if (!currentVariant.value.dismissible) return;
    event.preventDefault();
    onDismiss();
    return;
  }

  if (event.key !== "Tab") return;

  const nodes = focusableNodes();
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

/** 找出目前可操作的輸入欄位與按鈕，供 Tab 焦點循環使用 */
function focusableNodes() {
  if (!dialogRef.value) return [];
  return [...dialogRef.value.querySelectorAll("button, input, select, textarea")].filter(
    (node) => !node.disabled && node.tabIndex >= 0,
  );
}

/** 依表單、選檔或 Alert 類型決定開啟時的初始焦點 */
function focusPreferred() {
  if (state.fields.length) {
    dialogRef.value?.querySelector(".alert-input")?.focus();
    return;
  }

  if (state.requireFile) {
    dialogRef.value?.querySelector(".alert-file-trigger")?.focus();
    return;
  }

  const target = {
    cancel: cancelRef.value,
    confirm: confirmRef.value,
    dialog: dialogRef.value,
  }[currentVariant.value.focus];

  (target || confirmRef.value || dialogRef.value)?.focus();
}

/** 彈窗開啟時鎖住背景捲動；關閉時恢復捲動及先前焦點 */
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

/** 彈窗每次重新開啟，都等 DOM 更新後再指定焦點 */
watch(
  () => state.token,
  async (token) => {
    if (!token || !state.open) return;
    await nextTick();
    if (state.open && state.token === token) focusPreferred();
  },
);

/** 掛載時註冊鍵盤事件；卸載時清理監聽與未完成彈窗 */
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
  <!-- 將彈窗傳送到 body，避免受到原頁面版面與層級影響 -->
  <Teleport to="body">
    <div v-if="state.open" class="alert-backdrop" @click.self="onDismiss">
      <div :key="state.token" ref="dialogRef" class="alert-dialog"
        :class="{ 'is-loading': state.variant === 'loading', 'has-extra': Boolean(state.extraText) }" role="alertdialog" aria-modal="true"
        aria-labelledby="app-alert-title" :aria-describedby="describedBy || undefined" :aria-busy="state.variant === 'loading'"
        tabindex="-1">
        <TicketCard appear :color="currentVariant.color" :title="state.title" :detail="state.text">
          <template #title>
            <span id="app-alert-title">{{ state.title }}</span>
          </template>

          <template v-if="state.text" #detail>
            <span id="app-alert-text">{{ state.text }}</span>
          </template>

          <div class="alert-hero" aria-hidden="true">
            <span class="alert-badge">
              <Icon :icon="currentVariant.icon" :class="{ 'is-spin': state.variant === 'loading' }" />
            </span>
          </div>

          <!-- 成功或資訊類彈窗可選擇顯示統計資料 -->
          <dl v-if="state.rows.length" id="app-alert-rows" class="alert-stats">
            <div v-for="row in state.rows" :key="row.label">
              <dt>{{ row.label }}</dt>
              <dd>{{ row.value }}</dd>
            </div>
          </dl>

          <!-- 視設定呈現檔案上傳或動態輸入欄位 -->
          <div v-if="state.requireFile || state.fields.length" class="alert-form">
            <div v-if="state.requireFile" class="alert-field">
              <span class="alert-label">JSON 檔案</span>
              <div class="alert-file-row">
                <button type="button" class="ticket-link alert-file-trigger" @click="openFilePicker">
                  {{ state.fileButtonText }}
                </button>
                <p class="alert-file-name">{{ state.fileName || "尚未選擇檔案" }}</p>
              </div>
              <p v-if="state.fileError" class="alert-error">{{ state.fileError }}</p>
              <input ref="fileInputRef" class="alert-file-input" type="file" tabindex="-1" :accept="state.fileAccept"
                @change="onFileChange" />
            </div>

            <div v-for="field in state.fields" :key="field.key" class="alert-field">
              <label class="alert-label" :for="`alert-field-${field.key}`">{{ field.label }}</label>
              <input :id="`alert-field-${field.key}`" v-model="state.fieldValues[field.key]" class="alert-input"
                :class="{ 'is-invalid': state.fieldErrors[field.key] }" :type="field.type || 'text'"
                :inputmode="field.inputmode" :placeholder="field.placeholder" autocomplete="off"
                :aria-invalid="Boolean(state.fieldErrors[field.key])" @input="state.fieldErrors[field.key] = ''" />
              <p v-if="field.hint" class="alert-hint">{{ field.hint }}</p>
              <p v-if="state.fieldErrors[field.key]" class="alert-error">{{ state.fieldErrors[field.key] }}</p>
            </div>
          </div>

          <!-- 載入期間隱藏操作鈕，其餘類型依設定顯示取消、額外操作與確認 -->
          <template v-if="state.variant !== 'loading'" #action>
            <button v-if="state.cancelText" ref="cancelRef" type="button" class="ticket-link" @click="onDismiss">
              {{ state.cancelText }}
            </button>

            <button v-if="state.extraText" ref="extraRef" type="button" class="ticket-link is-dark" @click="onExtra">
              {{ state.extraText }}
            </button>

            <button ref="confirmRef" type="button" class="ticket-link is-dark" :disabled="confirmBlocked"
              @click="onConfirm">
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
  display: grid;
  grid-template-columns: 6.75rem minmax(0, 1fr);
  align-items: stretch;
}

.alert-stats > div + div {
  border-top: 2px dashed var(--accent);
}

.alert-stats dt,
.alert-stats dd {
  margin: 0;
  padding: 0.72rem 0.9rem;
  font-size: var(--text-body);
  line-height: var(--leading-body);
}

.alert-stats dt {
  display: flex;
  align-items: center;
  border-right: 2px dashed var(--accent);
  color: var(--ticket-ink);
  font-weight: 800;
}

.alert-stats dd {
  min-width: 0;
  color: var(--c-ink);
  font-weight: 900;
  font-variant-numeric: tabular-nums;
  overflow-wrap: anywhere;
  text-align: left;
}

.alert-form {
  display: grid;
  gap: 0.85rem;
  width: 100%;
  margin-top: 0.95rem;
}

.alert-label {
  display: block;
  margin-bottom: 0.35rem;
  color: var(--ticket-ink);
  font-size: var(--text-control);
  font-weight: 800;
}

.alert-file-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.65rem 0.8rem;
}

.alert-file-name {
  min-width: 0;
  margin: 0;
  color: var(--c-ink);
  font-size: var(--text-control);
  font-weight: 700;
  overflow-wrap: anywhere;
}

.alert-input {
  display: block;
  width: 100%;
  min-height: 3rem;
  padding: 0.5rem 0.9rem;
  border: 1.5px solid color-mix(in srgb, var(--ticket-ink) 35%, white);
  border-radius: 14px;
  background: #fff;
  color: var(--c-ink);
  font: inherit;
  font-size: var(--text-control);
  line-height: 1.5;
}

.alert-input::placeholder {
  color: color-mix(in srgb, var(--c-muted) 72%, white);
}

.alert-input:focus {
  border-color: var(--ticket-ink);
  outline: none;
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--ticket-ink) 22%, transparent);
}

.alert-input.is-invalid {
  border-color: #e11d48;
}

.alert-hint {
  margin: 0.35rem 0 0;
  color: var(--c-muted);
  font-size: var(--text-min);
  font-weight: 700;
}

.alert-error {
  margin: 0.35rem 0 0;
  color: #9f1239;
  font-size: var(--text-min);
  font-weight: 700;
}

.alert-file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
  border: 0;
}

.alert-dialog :deep(.ticket-stub) {
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 0.6rem;
  padding: var(--alert-stub-pad) 1.15rem;
}

@media (max-width: 560px) {
  .alert-dialog.has-extra {
    --stub-size: calc(var(--alert-stub-pad) * 2 + var(--alert-action-size) * 2 + 0.6rem + var(--dash));
  }
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
