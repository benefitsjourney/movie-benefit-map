<script setup>
/**
 * 共用彈出視窗（表單用）。
 * 用 v-model 控制開關：<AppModal v-model="open" title="標題">內容</AppModal>
 * 按右上角 ✕、按背景、按 Esc 都會關閉（送出中時不能關）。
 */
import { onBeforeUnmount, watch } from "vue";
import { Icon } from "@iconify/vue";
import closeIcon from "@iconify-icons/mdi/close";

const open = defineModel({ type: Boolean, default: false });

const props = defineProps({
  title: { type: String, default: "" },
  // 送出中時不能關閉，避免資料送到一半視窗就不見
  busy: { type: Boolean, default: false },
  width: { type: String, default: "36rem" },
});

function close() {
  if (!props.busy) open.value = false;
}

function onKeydown(event) {
  if (event.key === "Escape") close();
}

// 打開時鎖住背景捲動、監聽 Esc；關閉時還原
watch(open, (value) => {
  if (value) {
    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", onKeydown);
  } else {
    document.body.style.overflow = "";
    window.removeEventListener("keydown", onKeydown);
  }
});

onBeforeUnmount(() => {
  document.body.style.overflow = "";
  window.removeEventListener("keydown", onKeydown);
});
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="modal-backdrop" @click.self="close">
      <div class="modal-dialog-box" role="dialog" aria-modal="true" :aria-label="title" :style="{ width }">
        <div class="modal-head">
          <h2>{{ title }}</h2>
          <button type="button" class="modal-close" aria-label="關閉" :disabled="busy" @click="close">
            <Icon :icon="closeIcon" aria-hidden="true" />
          </button>
        </div>

        <div class="modal-body">
          <slot />
        </div>

        <div v-if="$slots.footer" class="modal-foot">
          <slot name="footer" />
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1100;
  display: grid;
  place-items: safe center;
  padding: 1.25rem;
  overflow: auto;
  background: color-mix(in srgb, #292b35 46%, transparent);
  backdrop-filter: blur(3px);
}

.modal-dialog-box {
  max-width: 100%;
  border: 1.5px solid var(--c-border);
  border-radius: 22px;
  background: var(--c-card);
  box-shadow: 0 18px 40px rgb(41 43 53 / 18%);
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.1rem 1.35rem 0.85rem;
  border-bottom: 1.5px solid var(--c-border);
}

.modal-head h2 {
  margin: 0;
  font-size: var(--text-feature-title);
  line-height: 1.35;
  font-weight: 900;
}

.modal-close {
  display: grid;
  place-items: center;
  width: 2.5rem;
  height: 2.5rem;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: var(--c-muted);
  font-size: var(--text-section-title);
  cursor: pointer;
}

.modal-close:hover:not(:disabled) {
  background: color-mix(in srgb, var(--c-muted) 12%, white);
}

.modal-body {
  padding: 1.15rem 1.35rem;
}

.modal-foot {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 0.9rem 1.35rem 1.15rem;
  border-top: 1.5px solid var(--c-border);
}

/* 讓表單欄位在視窗裡也有一致的樣式 */
.modal-body :deep(.field) {
  margin-bottom: 1rem;
}

.modal-body :deep(.field-label) {
  display: block;
  margin-bottom: 0.3rem;
  color: var(--c-muted);
  font-size: var(--text-control);
  font-weight: 700;
}

.modal-body :deep(.form-control),
.modal-body :deep(.form-select) {
  min-height: 2.9rem;
  border: 1.5px solid var(--c-border);
  border-radius: 14px;
  font-size: var(--text-control);
}

.modal-body :deep(.field-hint) {
  margin: 0.3rem 0 0;
  color: var(--c-muted);
  font-size: var(--text-min);
}
</style>
