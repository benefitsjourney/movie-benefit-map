<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { Icon } from "@iconify/vue";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import ticketIcon from "@iconify-icons/mdi/ticket-outline";
import pinIcon from "@iconify-icons/mdi/map-marker-outline";
import filmIcon from "@iconify-icons/mdi/filmstrip";
import searchIcon from "@iconify-icons/mdi/magnify";
import linkIcon from "@iconify-icons/mdi/open-in-new";
import megaphoneIcon from "@iconify-icons/mdi/bullhorn-outline";
import heartIcon from "@iconify-icons/mdi/heart";
import heartOutlineIcon from "@iconify-icons/mdi/heart-outline";
import arrowIcon from "@iconify-icons/mdi/arrow-right";
import { openAppAlert } from "@/components/ui/Alert.vue";

import http from "@/plugins/axios";
import { useFavoriteBenefits } from "@/composables/useFavoriteBenefits";
import BenefitFollowButton from "@/components/ui/BenefitFollowButton.vue";
import PageIntro from "@/components/layout/PageIntro.vue";
import DetailCard from "@/components/layout/DetailCard.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import { displayText, httpUrl } from "@/components/ui/DataTable.vue";
import ImageViewer from "@/components/ui/ImageViewer.vue";

// 後端 BenefitType 對應的中文名稱（跟列表頁一樣）
const BENEFIT_TYPES = {
  REGULAR: "一般場次",
  SPECIAL_BY_DATE: "入場贈送",
  SPECIAL_UNTIL_OUT: "限量兌換",
};

// 後端 BenefitInventoryStatus 對應的中文與顏色
const STOCK_STATUS = {
  IN_STOCK: { label: "有庫存", tone: "in" },
  LOW_STOCK: { label: "即將售罄", tone: "low" },
  EXHAUSTED: { label: "已售罄", tone: "out" },
  UNKNOWN: { label: "未知", tone: "unknown" },
};
// 篩選按鈕的順序
const STATUS_FILTERS = ["ALL", "IN_STOCK", "LOW_STOCK", "EXHAUSTED", "UNKNOWN"];

const route = useRoute();

// 網址 /benefits/:id id不是正整數就當作錯誤
const benefitId = computed(() => {
  const id = Number(route.params.id);
  return Number.isInteger(id) && id > 0 ? id : null;
});

const loading = ref(false);
const loadFailed = ref(false);
const loadErrorMessage = ref("請確認連線後再試一次。");
const benefit = ref(null);
// 點特典圖片看大圖
const viewerOpen = ref(false);
const stocks = ref([]);
const imageBroken = ref(false);

const keyword = ref("");
const statusFilter = ref("ALL");

// 路由快速切換時，只讓最後一次請求更新畫面
let requestSerial = 0;

function apiMessage(error, fallback) {
  const message = error?.response?.data?.message;
  return typeof message === "string" && message.trim() ? message.trim() : fallback;
}

async function loadBenefit() {
  const serial = ++requestSerial;
  benefit.value = null;
  stocks.value = [];
  imageBroken.value = false;
  keyword.value = "";
  statusFilter.value = "ALL";

  if (benefitId.value == null) {
    loading.value = false;
    loadFailed.value = true;
    loadErrorMessage.value = "網址中的特典編號不正確。";
    return;
  }

  loading.value = true;
  loadFailed.value = false;

  try {
    // 兩支API同時送出，等兩個都回來
    const [benefitRes, stockRes] = await Promise.all([
      http.get(`/benefits/${benefitId.value}`),
      http.get(`/benefits/${benefitId.value}/cinemas/status`),
    ]);
    if (serial !== requestSerial) return;
    benefit.value = benefitRes.data;
    stocks.value = Array.isArray(stockRes.data) ? stockRes.data : [];
  } catch (error) {
    if (serial !== requestSerial) return;
    loadFailed.value = true;
    loadErrorMessage.value =
      error?.response?.status === 404 ? "找不到這個特典。" : apiMessage(error, "請確認連線後再試一次。");
  } finally {
    if (serial === requestSerial) loading.value = false;
  }
}

// ── 特典資訊 ──────────────────────────────────────
const imageUrl = computed(() => {
  const url = httpUrl(benefit.value?.officialImageUrl);
  return url && !imageBroken.value ? url : "";
});

const sourceHref = computed(() => httpUrl(benefit.value?.sourceUrl));

// 電影名稱從庫存清單拿（特典詳情API沒有電影資訊），去掉重複
const movieNames = computed(() => [...new Set(stocks.value.map((s) => s.movieName).filter(Boolean))]);

function dateText(value) {
  const matched = typeof value === "string" && value.match(/^(\d{4})-(\d{2})-(\d{2})/);
  return matched ? `${matched[1]}/${matched[2]}/${matched[3]}` : "";
}

const periodText = computed(() => {
  const start = dateText(benefit.value?.startDate);
  const end = dateText(benefit.value?.endDate);
  if (start && end) return `${start} ～ ${end}`;
  if (start) return `${start} 起，送完為止`;
  return "";
});

const infoFields = computed(() => {
  const current = benefit.value;
  if (!current) return [];
  return [
    { label: "電影", value: movieNames.value.join("、") },
    { label: "類型", value: BENEFIT_TYPES[current.benefitType] ?? "" },
    { label: "發放期間", value: periodText.value },
    { label: "會員限定", value: current.isMemberOnly ? "是" : "否" },
    { label: "狀態", value: current.status === "APPROVED" ? "發放中" : "已結束" },
  ];
});

// ── 各影城庫存 ────────────────────────────────────
function statusOf(code) {
  return STOCK_STATUS[code] ?? STOCK_STATUS.UNKNOWN;
}

// 每種狀態各有幾間（篩選按鈕上的數字）
const statusCounts = computed(() => {
  const counts = { ALL: stocks.value.length };
  for (const s of stocks.value) {
    const key = STOCK_STATUS[s.status] ? s.status : "UNKNOWN";
    counts[key] = (counts[key] ?? 0) + 1;
  }
  return counts;
});

function filterLabel(key) {
  return key === "ALL" ? "全部" : statusOf(key).label;
}

// 依影城名稱關鍵字＋狀態篩選（只在前端過濾）
const filteredStocks = computed(() => {
  const word = keyword.value.trim();
  return stocks.value.filter((s) => {
    const key = STOCK_STATUS[s.status] ? s.status : "UNKNOWN";
    if (statusFilter.value !== "ALL" && key !== statusFilter.value) return false;
    if (word && !String(s.cinemaName ?? "").includes(word)) return false;
    return true;
  });
});

// ── 庫存回報 ──────────────────────────────────────
// 可以回報的三種狀態（不能回報「未知」）
const REPORT_OPTIONS = ["IN_STOCK", "LOW_STOCK", "EXHAUSTED"];

// 目前展開回報選項的是哪一間影城（存 cinemaMovieBenefitId），null＝都沒展開
const reportingId = ref(null);
// 正在送出的那一間，避免連點
const submittingId = ref(null);

function openReport(stock) {
  // 沒有 Token 就先提示登入，不用等後端回 401
  if (!localStorage.getItem("token")) {
    openAppAlert({
      variant: "info",
      title: "請先登入",
      text: "登入會員後才能回報庫存。",
      confirmText: "關閉",
      cancelText: "",
    });
    return;
  }
  reportingId.value = reportingId.value === stock.cinemaMovieBenefitId ? null : stock.cinemaMovieBenefitId;
}

async function submitReport(stock, status) {
  if (submittingId.value) return;
  submittingId.value = stock.cinemaMovieBenefitId;

  try {
    // POST /inventory-reports：帶影城特典 ID 和狀態
    await http.post("/inventory-reports", {
      cinemaMovieBenefitId: stock.cinemaMovieBenefitId,
      status,
    });
    // 成功後直接更新畫面上的狀態，不用重新整理
    stock.status = status;
    reportingId.value = null;
    await openAppAlert({
      variant: "success",
      title: "感謝回報！",
      text: `已將「${stock.cinemaName}」回報為「${statusOf(status).label}」。`,
      confirmText: "關閉",
      cancelText: "",
    });
  } catch (error) {
    const code = error?.response?.status;
    const text =
      code === 401 ? "登入已過期，請重新登入。"
        : code === 429 ? apiMessage(error, "同一間影城需間隔 15 分鐘才能再回報。")
          : apiMessage(error, "回報失敗，請稍後再試。");
    await openAppAlert({
      variant: "error",
      title: "無法回報",
      text,
      confirmText: "關閉",
      cancelText: "",
    });
  } finally {
    submittingId.value = null;
  }
}

watch(() => route.params.id, loadBenefit, { immediate: true });

onUnmounted(() => {
  requestSerial += 1;
});

// ── 收藏：跟特典一覽的愛心共用同一套（未收藏→收藏；已收藏→確認後取消）──
const { loadFavorites, isFavorite, isBusy, toggleFavorite } = useFavoriteBenefits();
onMounted(loadFavorites);

const favored = computed(() => Boolean(benefit.value) && isFavorite(benefit.value.benefitId));
const favoriteBusy = computed(() => Boolean(benefit.value) && isBusy(benefit.value.benefitId));
</script>

<template>
  <section>
    <PageIntro kicker="✦ BENEFIT DETAIL" lead="特典" mark="在哪裡" tail="？" description="查看特典資訊，以及各影城目前的庫存狀況。"
      :icon-left="giftIcon" :icon-right="ticketIcon" :icon-top="pinIcon" />

    <StatePanel v-if="loading" variant="loading" message="正在載入特典資料…" :icon="giftIcon" />
    <StatePanel v-else-if="loadFailed" variant="error" title="目前無法取得特典資料" :message="loadErrorMessage"
      @retry="loadBenefit" />

    <template v-else-if="benefit">
      <!-- 特典資訊 -->
      <DetailCard :title="displayText(benefit.benefitName)" :title-icon="giftIcon" back-to="/benefits"
        back-label="返回特典一覽" theme="coral">
        <div class="benefit-info">
          <div class="benefit-image">
            <img v-if="imageUrl" :src="imageUrl" :alt="`${benefit.benefitName} 圖片`" referrerpolicy="no-referrer"
              @error="imageBroken = true" class="poster-click" @click="viewerOpen = true" />
            <span v-else class="image-fallback">
              <Icon :icon="giftIcon" aria-hidden="true" />
            </span>
          </div>

          <section class="detail-section">
            <h3>特典資訊</h3>
            <dl>
              <div v-for="field in infoFields" :key="field.label">
                <dt>{{ field.label }}</dt>
                <dd>{{ displayText(field.value) }}</dd>
              </div>
              <div>
                <dt>資料來源</dt>
                <dd>
                  <a v-if="sourceHref" class="detail-link" :href="sourceHref" target="_blank" rel="noopener noreferrer">
                    查看官方公告
                    <Icon :icon="linkIcon" aria-hidden="true" />
                  </a>
                  <template v-else>—</template>
                </dd>
              </div>
            </dl>
            <div class="collect-row">
              <!-- 空心＝加入收藏、實心＝已收藏（點了會先確認才取消） -->
              <button type="button" class="collect-btn" :class="{ 'is-on': favored }" :aria-pressed="favored"
                :disabled="favoriteBusy" @click="toggleFavorite(benefit)">
                <Icon :icon="favored ? heartIcon : heartOutlineIcon" aria-hidden="true" />
                {{ favoriteBusy ? "處理中…" : favored ? "已收藏" : "加入收藏" }}
              </button>
              <BenefitFollowButton :benefit="benefit" />
              <RouterLink v-if="favored" to="/collection" class="collect-link">
                到我的收藏上傳照片
                <Icon :icon="arrowIcon" aria-hidden="true" />
              </RouterLink>
            </div>
          </section>
        </div>
      </DetailCard>

      <ImageViewer v-model="viewerOpen" :images="[imageUrl]" :title="benefit.benefitName" />
      <!-- 各影城庫存 -->
      <DetailCard title="各影城庫存" :title-icon="pinIcon" theme="coral">
        <template v-if="stocks.length">
          <div class="stock-tools">
            <label class="stock-search">
              <Icon :icon="searchIcon" aria-hidden="true" />
              <input v-model="keyword" type="search" class="form-control" placeholder="搜尋影城名稱"
                aria-label="搜尋影城名稱" />
            </label>

            <div class="stock-filters" role="group" aria-label="依庫存狀態篩選">
              <button v-for="key in STATUS_FILTERS" :key="key" type="button" class="filter-chip"
                :class="[`tone-${key === 'ALL' ? 'all' : statusOf(key).tone}`, { active: statusFilter === key }]"
                :aria-pressed="statusFilter === key" @click="statusFilter = key">
                {{ filterLabel(key) }}
                <span class="chip-count">{{ statusCounts[key] ?? 0 }}</span>
              </button>
            </div>
          </div>

          <ul v-if="filteredStocks.length" class="stock-list">
                        <li v-for="stock in filteredStocks" :key="stock.cinemaMovieBenefitId" class="stock-item">
              <div class="stock-row">
                <div class="stock-main">
                  <strong class="stock-cinema">{{ displayText(stock.cinemaName) }}</strong>
                </div>
                <span class="stock-badge" :class="`tone-${statusOf(stock.status).tone}`">
                  {{ statusOf(stock.status).label }}
                </span>
                <button v-if="benefit.status === 'APPROVED'" type="button" class="report-toggle"
                  :aria-expanded="reportingId === stock.cinemaMovieBenefitId" @click="openReport(stock)">
                  <Icon :icon="megaphoneIcon" aria-hidden="true" />
                  回報
                </button>
              </div>

              <!-- 點「回報」後展開三個選項 -->
              <div v-if="reportingId === stock.cinemaMovieBenefitId" class="report-options">
                <span class="report-hint">現在現場的狀況是？</span>
                <button v-for="option in REPORT_OPTIONS" :key="option" type="button" class="report-option"
                  :class="`tone-${statusOf(option).tone}`" :disabled="submittingId === stock.cinemaMovieBenefitId"
                  @click="submitReport(stock, option)">
                  {{ statusOf(option).label }}
                </button>
              </div>
            </li>
          </ul>
          <p v-else class="stock-empty">沒有符合條件的影城，換個條件試試吧。</p>
        </template>

        <p v-else class="stock-empty">這個特典目前還沒有綁定影城。</p>
      </DetailCard>
    </template>
  </section>
</template>

<style scoped>
/* ── 特典資訊：左圖右文，手機改上下 ── */
.benefit-info {
  display: grid;
  grid-template-columns: minmax(0, 16rem) minmax(0, 1fr);
  gap: 1.5rem;
  align-items: start;
}

.benefit-image {
  display: grid;
  place-items: center;
  overflow: hidden;
  aspect-ratio: 2 / 3;
  border: 1.5px solid var(--c-border);
  border-radius: 16px;
  background: #fffbeb;
}

.benefit-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-fallback {
  color: #8a6400;
  font-size: var(--text-page-title);
}

.detail-link {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
}

@media (max-width: 719px) {
  .benefit-info {
    grid-template-columns: 1fr;
  }

  .benefit-image {
    max-width: 16rem;
  }
}

/* ── 庫存：搜尋＋篩選 ── */
.stock-tools {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem 1rem;
  margin-bottom: 1rem;
}

.stock-search {
  position: relative;
  flex: 1 1 16rem;
  margin: 0;
}

.stock-search :deep(svg) {
  position: absolute;
  top: 50%;
  left: 0.8rem;
  color: #ff7049;
  font-size: var(--text-card-title);
  transform: translateY(-50%);
  pointer-events: none;
}

.stock-search .form-control {
  min-height: 3rem;
  padding-left: 2.7rem;
  border: 1.5px solid #eadfce;
  border-radius: 14px;
  font-size: var(--text-control);
}

.stock-search .form-control:focus {
  border-color: #ff7049;
  box-shadow: 0 0 0 3px rgb(255 112 73 / 20%);
}

.stock-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.85rem;
  border: 1.5px solid var(--chip-ink);
  border-radius: 999px;
  background: #fff;
  color: var(--chip-ink);
  font-size: var(--text-control);
  font-weight: 800;
  cursor: pointer;
}

.filter-chip.active {
  background: var(--chip-ink);
  color: #fff;
}

.filter-chip:focus-visible {
  outline: 2px solid var(--chip-ink);
  outline-offset: 2px;
}

.chip-count {
  min-width: 1.5rem;
  padding: 0 0.35rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--chip-ink) 14%, white);
  color: var(--chip-ink);
  font-size: var(--text-min);
  text-align: center;
}

.filter-chip.active .chip-count {
  background: #fff;
}

/* ── 庫存：每間影城一列 ── */
.stock-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(22rem, 1fr));
  gap: 0.65rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.stock-item {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  padding: 0.75rem 1rem;
  border: 1px solid var(--c-border);
  border-radius: 14px;
  background: #fffaf5;
}

.stock-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stock-cinema {
  font-size: var(--text-body);
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.stock-movie {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  color: var(--c-muted);
  font-size: var(--text-min);
}

.stock-badge {
  flex: none;
  padding: 0.2rem 0.7rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--chip-ink) 12%, white);
  color: var(--chip-ink);
  font-size: var(--text-min);
  font-weight: 800;
  white-space: nowrap;
}


.stock-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.stock-row .stock-main {
  flex: 1;
}

.report-toggle {
  display: inline-flex;
  flex: none;
  align-items: center;
  gap: 0.25rem;
  padding: 0.2rem 0.65rem;
  border: 1.5px solid #ff7049;
  border-radius: 999px;
  background: #fff;
  color: #ff7049;
  font-size: var(--text-min);
  font-weight: 800;
  cursor: pointer;
}

.report-toggle[aria-expanded="true"],
.report-toggle:hover {
  background: #ff7049;
  color: #fff;
}

.report-options {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.4rem;
  padding-top: 0.6rem;
  border-top: 1px dashed var(--c-border);
}

.report-hint {
  margin-right: auto;
  color: var(--c-muted);
  font-size: var(--text-min);
  font-weight: 700;
}

.report-option {
  padding: 0.25rem 0.8rem;
  border: 1.5px solid var(--chip-ink);
  border-radius: 999px;
  background: #fff;
  color: var(--chip-ink);
  font-size: var(--text-min);
  font-weight: 800;
  cursor: pointer;
}

.report-option:hover:not(:disabled) {
  background: var(--chip-ink);
  color: #fff;
}

.report-option:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.stock-empty {
  margin: 0;
  padding: 1.5rem 0;
  color: var(--c-muted);
  font-weight: 700;
  text-align: center;
}

/* 狀態顏色：綠＝有、橘＝即將送完、紅＝送完、灰＝沒人回報 */
.tone-all {
  --chip-ink: #1e2b45;
}

.tone-in {
  --chip-ink: #0f6e5c;
}

.tone-low {
  --chip-ink: #b85a12;
}

.tone-out {
  --chip-ink: #a33b42;
}

.tone-unknown {
  --chip-ink: #6c7380;
}

/* ── 收藏按鈕：空心＝未收藏（白底粉框）、實心＝已收藏（粉底白字）── */
.collect-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  margin-top: 1rem;
}

.collect-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  min-height: 2.75rem;
  padding: 0.4rem 1.1rem;
  border: 1.5px solid #e0457b;
  border-radius: 999px;
  background: #fff;
  color: #e0457b;
  font-size: var(--text-control);
  font-weight: 800;
  cursor: pointer;
  transition:
    background-color var(--ease),
    color var(--ease);
}

.collect-btn :deep(svg) {
  font-size: 1.3rem;
}

.collect-btn:hover:not(:disabled) {
  background: #fff0f6;
}

.collect-btn.is-on {
  background: #e0457b;
  color: #fff;
}

.collect-btn.is-on:hover:not(:disabled) {
  background: #c93a6b;
}

.collect-btn.is-on :deep(svg) {
  animation: heart-pop 0.35s ease;
}

.collect-btn:disabled {
  opacity: 0.6;
  cursor: wait;
}

.collect-btn:focus-visible {
  outline: 2px solid #e0457b;
  outline-offset: 3px;
}

.collect-link {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  color: #a33b6b;
  font-size: var(--text-min);
  font-weight: 800;
  text-underline-offset: 0.18em;
}

@keyframes heart-pop {
  50% {
    transform: scale(1.3);
  }
}

.poster-click {
  cursor: zoom-in;
}
</style>
