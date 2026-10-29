<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useId, watch } from "vue";
import { RouterLink } from "vue-router";
import { Icon } from "@iconify/vue";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import arrowIcon from "@iconify-icons/mdi/arrow-right";
import http from "@/plugins/axios";
import TicketCard from "@/components/ui/TicketCard.vue";

// 後端庫存代碼對應的中文狀態
const INVENTORY_STATUS_LABELS = {
  UNKNOWN: "庫存狀態待確認",
  IN_STOCK: "還有庫存",
  LOW_STOCK: "剩下不多了",
  EXHAUSTED: "已售罄",
};

const ZOOM_SCALE = 2.75;
const ZOOM_MARGIN = 12;

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

/** 依特典編號快取補查到的官方圖片 */
const imageCache = ref(new Map());
/** 正在補查圖片的特典編號 */
const pendingImageIds = ref(new Set());
/** 載入失敗的圖片網址，改顯示禮物圖示 */
const brokenImages = ref(new Set());
/** 游標停在縮圖上時的放大預覽 */
const zoomUrl = ref("");
const zoomStyle = ref({});

/** 視窗關閉或清單更新時，丟掉進行中的補查結果 */
let imageRequestId = 0;
let originalOverflow = "";

const rows = computed(() => (Array.isArray(props.benefits) ? props.benefits : []));
const describedBy = computed(() => (rows.value.length ? rowsId : statusId));

/** 取得特典標題，沒有名稱時使用預設文字 */
function getHeadline(benefit) {
  const value = benefit?.[props.headlineKey];
  return typeof value === "string" && value.trim() ? value : props.headlineFallback;
}

/** 將後端庫存代碼轉換成中文狀態 */
function getInventoryStatus(status) {
  return INVENTORY_STATUS_LABELS[status] || INVENTORY_STATUS_LABELS.UNKNOWN;
}

/** 統一處理圖片網址，避免空值與多餘空白 */
function normalizeImageUrl(value) {
  return typeof value === "string" ? value.trim() : "";
}

/** 優先使用清單圖片，沒有再看補查結果；失效網址不顯示 */
function imageUrl(benefit) {
  const directUrl = normalizeImageUrl(benefit?.officialImageUrl);
  const cachedUrl = imageCache.value.get(benefit?.benefitId) || "";
  const url = directUrl || cachedUrl;

  return brokenImages.value.has(url) ? "" : url;
}

/** 圖片載入失敗時記錄網址，避免重複顯示失效圖片 */
function handleImageError(url) {
  if (!url) return;

  brokenImages.value = new Set([...brokenImages.value, url]);

  if (zoomUrl.value === url) hideZoom();
}

/** 庫存清單沒有圖片網址時，依特典編號向詳情 API 補查 */
async function fetchMissingImages(list) {
  const requestId = ++imageRequestId;

  const ids = [...new Set(
    list
      .filter((benefit) => {
        const id = benefit?.benefitId;
        return id != null
          && !normalizeImageUrl(benefit.officialImageUrl)
          && !imageCache.value.has(id);
      })
      .map((benefit) => benefit.benefitId),
  )];

  pendingImageIds.value = new Set(ids);
  if (!ids.length) return;

  const results = await Promise.all(ids.map(async (id) => {
    try {
      const { data } = await http.get(`/benefits/${id}`);
      return [id, normalizeImageUrl(data?.officialImageUrl)];
    } catch {
      return [id, ""];
    }
  }));

  if (requestId !== imageRequestId) return;

  imageCache.value = new Map([...imageCache.value, ...results]);
  pendingImageIds.value = new Set();
}

// 特典清單更新時重新檢查缺少的圖片
watch(rows, (list) => {
  hideZoom();
  fetchMissingImages(list);
}, { immediate: true });

/** 限制圖片放大位置，避免超出視窗 */
function clamp(value, min, max) {
  return Math.max(min, Math.min(value, max));
}

/** 游標停在縮圖上時，於原位放大約 2.75 倍 */
function showZoom(benefit, event) {
  const url = imageUrl(benefit);
  if (!url) return;

  const rect = event.currentTarget.getBoundingClientRect();
  const width = rect.width * ZOOM_SCALE;
  const height = rect.height * ZOOM_SCALE;

  const left = clamp(
    rect.left + (rect.width - width) / 2,
    ZOOM_MARGIN,
    Math.max(ZOOM_MARGIN, window.innerWidth - width - ZOOM_MARGIN),
  );

  const top = clamp(
    rect.top + (rect.height - height) / 2,
    ZOOM_MARGIN,
    Math.max(ZOOM_MARGIN, window.innerHeight - height - ZOOM_MARGIN),
  );

  zoomUrl.value = url;
  zoomStyle.value = {
    left: `${left}px`,
    top: `${top}px`,
    width: `${width}px`,
    height: `${height}px`,
  };
}

/** 關閉圖片放大預覽 */
function hideZoom() {
  zoomUrl.value = "";
}

/** 通知外層關閉視窗 */
function close() {
  hideZoom();
  emit("close");
}

/** 按 Escape 關閉視窗 */
function onKeydown(event) {
  if (event.key === "Escape") close();
}

// 打開時鎖住背景捲動，並把焦點移進視窗
onMounted(async () => {
  originalOverflow = document.body.style.overflow;
  document.body.style.overflow = "hidden";

  window.addEventListener("keydown", onKeydown);

  await nextTick();
  dialogRef.value?.focus();
});

onBeforeUnmount(() => {
  imageRequestId++;

  window.removeEventListener("keydown", onKeydown);
  document.body.style.overflow = originalOverflow;

  hideZoom();
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

          <p v-if="loading && !rows.length" class="benefit-dialog-status">
            正在載入特典資訊…
          </p>

          <p v-else-if="error && !rows.length" class="benefit-dialog-status">
            特典資訊暫時載入失敗，請稍後再試。
          </p>

          <p v-else-if="!rows.length" class="benefit-dialog-status">
            {{ emptyText }}
          </p>

          <div v-else class="benefit-stats-scroll" @scroll="hideZoom">
            <ol :id="rowsId" class="benefit-list">
              <li v-for="(benefit, index) in rows"
                :key="benefit.cinemaMovieBenefitId ?? `${headlineKey}-${benefit.benefitId}-${index}`"
                class="benefit-item">
                <div class="benefit-thumb" @mouseenter="showZoom(benefit, $event)" @mouseleave="hideZoom">
                  <img v-if="imageUrl(benefit)" :src="imageUrl(benefit)" :alt="`${benefit.benefitName || '特典'} 圖片`"
                    loading="lazy" decoding="async" referrerpolicy="no-referrer"
                    @error="handleImageError($event.currentTarget.getAttribute('src'))" />

                  <!-- 補查還沒回來時留白，確認沒有圖片才顯示禮物圖示 -->
                  <span v-else-if="benefit.benefitId == null || !pendingImageIds.has(benefit.benefitId)"
                    class="benefit-thumb-fallback" aria-hidden="true">
                    <Icon :icon="giftIcon" />
                  </span>
                </div>

                <div class="benefit-item-copy">
                  <div class="benefit-item-head">
                    <h3>{{ getHeadline(benefit) }}</h3>

                    <span class="benefit-index" aria-hidden="true">
                      {{ String(index + 1).padStart(2, "0") }}
                    </span>
                  </div>

                  <div class="benefit-item-body">
                    <span class="benefit-name">
                      {{ benefit.benefitName || "特典" }}
                    </span>

                    <span class="benefit-status">
                      {{ getInventoryStatus(benefit.status) }}
                    </span>
                  </div>

                  <RouterLink v-if="benefit.benefitId != null" class="benefit-detail-link"
                    :to="`/benefits/${benefit.benefitId}`">
                    前往詳細資訊
                    <Icon :icon="arrowIcon" aria-hidden="true" />
                  </RouterLink>
                </div>
              </li>
            </ol>
          </div>

          <template #action>
            <button type="button" class="ticket-link" @click="close">
              {{ closeLabel }}
            </button>
          </template>
        </TicketCard>
      </div>
    </div>

    <img v-if="zoomUrl" class="benefit-zoom" :src="zoomUrl" alt="" :style="zoomStyle" />
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

.benefit-badge :deep(svg),
.benefit-thumb-fallback :deep(svg) {
  display: block;
  width: 1em;
  height: 1em;
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
  grid-template-columns: 6.5rem minmax(0, 1fr);
  align-items: start;
  gap: 0.75rem;
  padding: 0.75rem 0.85rem 0.8rem;
  border: 2px dashed var(--accent);
  border-radius: 14px;
  background: #fff;
}

.benefit-thumb {
  display: grid;
  place-items: center;
  align-self: center;
  width: 6.5rem;
  aspect-ratio: 3 / 4;
  overflow: hidden;
  border-radius: 10px;
  background: var(--ticket-pale);
}

.benefit-thumb:has(img) {
  cursor: zoom-in;
}

.benefit-thumb img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.benefit-thumb-fallback {
  color: var(--ticket-ink);
  font-size: 1.65rem;
}

.benefit-zoom {
  position: fixed;
  z-index: 2100;
  pointer-events: none;
  object-fit: contain;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 16px 36px rgb(38 28 18 / 28%);
  animation: benefit-zoom-in 0.16s ease;
}

.benefit-item-copy {
  display: grid;
  gap: 0.55rem;
  min-width: 0;
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

.benefit-detail-link {
  justify-self: end;
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.32rem 0.75rem;
  border: 1.5px solid var(--ticket-ink);
  border-radius: 999px;
  background: #fff;
  color: var(--ticket-ink);
  font-size: var(--text-control);
  font-weight: 800;
  line-height: 1.4;
  text-decoration: none;
}

.benefit-detail-link:hover,
.benefit-detail-link:focus-visible {
  background: var(--accent);
  color: var(--c-ink);
}

.benefit-detail-link:focus-visible {
  outline: 2px solid var(--ticket-ink);
  outline-offset: 2px;
}

.benefit-detail-link :deep(svg) {
  width: 1em;
  height: 1em;
}

@keyframes benefit-zoom-in {
  from {
    opacity: 0.45;
    scale: 0.62;
  }

  to {
    opacity: 1;
    scale: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .benefit-zoom {
    animation: none;
  }
}
</style>
