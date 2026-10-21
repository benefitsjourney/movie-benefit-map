<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useId } from "vue";
import { Icon } from "@iconify/vue";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import TicketCard from "@/components/ui/TicketCard.vue";

const INVENTORY_STATUS_LABELS = {
  UNKNOWN: "庫存狀態待確認",
  IN_STOCK: "還有庫存",
  LOW_STOCK: "剩下不多了",
  EXHAUSTED: "已售罄",
};

const props = defineProps({
  title: {
    type: String,
    default: "",
  },
  detail: {
    type: String,
    default: "",
  },
  benefits: {
    type: Array,
    default: () => [],
  },
  headlineKey: {
    type: String,
    required: true,
  },
  headlineFallback: {
    type: String,
    default: "尚未提供名稱",
  },
  loading: {
    type: Boolean,
    default: false,
  },
  error: {
    type: Boolean,
    default: false,
  },
  closeLabel: {
    type: String,
    default: "關閉",
  },
  emptyText: {
    type: String,
    default: "目前沒有找到符合條件的特典",
  },
});

const emit = defineEmits(["close"]);

const dialogRef = ref(null);
const titleId = useId();
const statusId = useId();
const rowsId = useId();

const rows = computed(() => (Array.isArray(props.benefits) ? props.benefits : []));
const describedBy = computed(() => (rows.value.length ? rowsId : statusId));

function headline(benefit) {
  const value = benefit?.[props.headlineKey];
  return typeof value === "string" && value.trim() ? value : props.headlineFallback;
}

function statusLabel(status) {
  return INVENTORY_STATUS_LABELS[status] || "庫存狀態待確認";
}

function close() {
  emit("close");
}

function onKeydown(event) {
  if (event.key === "Escape") close();
}

onMounted(async () => {
  document.body.style.overflow = "hidden";
  window.addEventListener("keydown", onKeydown);
  await nextTick();
  dialogRef.value?.focus();
});

onBeforeUnmount(() => {
  window.removeEventListener("keydown", onKeydown);
  document.body.style.overflow = "";
});
</script>

<template>
  <Teleport to="body">
    <div class="benefit-dialog-backdrop" @click.self="close">
      <div ref="dialogRef" class="benefit-dialog" role="dialog" aria-modal="true" tabindex="-1"
        :aria-labelledby="titleId" :aria-describedby="describedBy">
        <TicketCard appear color="orange" :title="title">
          <template #title>
            <span :id="titleId">{{ title }}</span>
          </template>
          <template #detail>
            <span :id="statusId">{{ detail }}</span>
          </template>

          <div class="benefit-hero" aria-hidden="true">
            <span class="benefit-badge">
              <Icon :icon="giftIcon" />
            </span>
          </div>

          <p v-if="loading && !rows.length" class="benefit-dialog-status">正在載入特典資訊…</p>
          <p v-else-if="error && !rows.length" class="benefit-dialog-status">
            特典資訊暫時載入失敗，請稍後再試。
          </p>
          <p v-else-if="!rows.length" class="benefit-dialog-status">{{ emptyText }}</p>
          <div v-else class="benefit-stats-scroll">
            <ol :id="rowsId" class="benefit-list">
              <li v-for="(benefit, index) in rows"
                :key="benefit.cinemaMovieBenefitId ?? `${headlineKey}-${benefit.benefitId}-${index}`"
                class="benefit-item">
                <div class="benefit-item-head">
                  <h3>{{ headline(benefit) }}</h3>
                  <span class="benefit-index" aria-hidden="true">{{ String(index + 1).padStart(2, "0") }}</span>
                </div>
                <div class="benefit-item-body">
                  <span class="benefit-name">{{ benefit.benefitName || "特典" }}</span>
                  <span class="benefit-status">{{ statusLabel(benefit.status) }}</span>
                </div>
              </li>
            </ol>
          </div>

          <template #action>
            <button type="button" class="ticket-link" @click="close">{{ closeLabel }}</button>
          </template>
        </TicketCard>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.benefit-dialog-backdrop {
  position: fixed;
  z-index: 2000;
  display: grid;
  align-items: safe center;
  justify-items: center;
  padding: 1.25rem;
  overflow: auto;
  background: color-mix(in srgb, var(--c-ink) 46%, transparent);
  backdrop-filter: blur(3px);
  inset: 0;
}

.benefit-dialog {
  width: min(52rem, 100%);
  outline: none;
}

.benefit-dialog :deep(.ticket),
.benefit-dialog :deep(.ticket:hover) {
  --alert-stub-pad: 0.85rem;
  --alert-action-size: 2.75rem;
  --stub-size: calc(var(--alert-stub-pad) * 2 + var(--alert-action-size) + var(--dash));
  min-height: 0;
}

.benefit-hero {
  order: -1;
  align-self: flex-start;
}

.benefit-badge {
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

.benefit-badge :deep(svg) {
  display: block;
  width: 1em;
  height: 1em;
}

.benefit-dialog :deep(.ticket-body) {
  align-items: flex-start;
  padding: 1.25rem 1.35rem 1.15rem;
  text-align: left;
}

.benefit-dialog :deep(.ticket-body h2) {
  margin-top: 0.85rem;
  font-size: var(--text-feature-title);
  line-height: 1.4;
}

.benefit-dialog :deep(.ticket-detail) {
  margin-top: 0.55rem;
  font-size: var(--text-body);
  line-height: var(--leading-body);
}

.benefit-dialog-status {
  margin: 0.95rem 0 0;
  color: var(--c-muted, #6c7380);
  font-weight: 700;
}

.benefit-stats-scroll {
  box-sizing: border-box;
  width: calc(100% + 1.35rem);
  max-height: min(42vh, 22rem);
  margin: 0.95rem -1.35rem 0 0;
  padding-right: 0.7rem;
  overflow: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  scrollbar-color: var(--ticket-ink) transparent;
  scrollbar-width: thin;
}

.benefit-stats-scroll::-webkit-scrollbar {
  width: 0.7rem;
}

.benefit-stats-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.benefit-stats-scroll::-webkit-scrollbar-thumb {
  border: 2px solid transparent;
  border-radius: 999px;
  background: var(--ticket-ink);
  background-clip: padding-box;
}

.benefit-stats-scroll::-webkit-scrollbar-thumb:hover {
  background: color-mix(in srgb, var(--ticket-ink) 82%, black);
  background-clip: padding-box;
}

.benefit-list {
  display: grid;
  gap: 0.7rem;
  width: 100%;
  margin: 0;
  padding: 0;
  list-style: none;
}

.benefit-item {
  display: grid;
  gap: 0.55rem;
  padding: 0.75rem 0.85rem 0.8rem;
  border: 2px dashed var(--accent);
  border-radius: 14px;
  background: #fff;
}

.benefit-item-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.benefit-item-head h3 {
  margin: 0;
  color: var(--ticket-ink);
  font-size: var(--text-body);
  font-weight: 800;
  line-height: var(--leading-body);
  overflow-wrap: anywhere;
}

.benefit-index {
  flex-shrink: 0;
  color: color-mix(in srgb, var(--ticket-ink) 70%, white);
  font-size: var(--text-control);
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.04em;
  line-height: var(--leading-body);
}

.benefit-item-body {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.15rem;
  padding: 0.65rem 0.75rem;
  border-radius: 10px;
  background: var(--ticket-pale);
}

.benefit-name {
  color: var(--c-ink);
  font-size: var(--text-body);
  font-weight: 900;
  line-height: var(--leading-body);
  overflow-wrap: anywhere;
}

.benefit-status {
  color: var(--ticket-ink);
  font-size: var(--text-control);
  font-weight: 800;
}

.benefit-dialog :deep(.ticket-stub) {
  justify-content: flex-end;
  padding: var(--alert-stub-pad) 1.15rem;
}

.benefit-dialog :deep(.ticket-film) {
  display: none;
}

.benefit-dialog :deep(.ticket-link) {
  justify-content: center;
  min-width: 6rem;
  min-height: var(--alert-action-size);
  padding: 0.5rem 1.05rem;
  font-size: var(--text-control);
}
</style>
