<script setup>
import { computed, ref } from "vue";
import { Icon } from "@iconify/vue";
import megaphoneIcon from "@iconify-icons/mdi/bullhorn-outline";
import clipboardIcon from "@iconify-icons/mdi/clipboard-text-outline";
import pinIcon from "@iconify-icons/mdi/map-marker-outline";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import arrowIcon from "@iconify-icons/mdi/arrow-right";

import { useApiCollection } from "@/composables/useApiCollection";
import { collectionLoadError } from "@/utils/apiError";
import PageIntro from "@/components/layout/PageIntro.vue";
import ResultBanner from "@/components/layout/ResultBanner.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import DataTable, { dateTimeText } from "@/components/ui/DataTable.vue";
import Pagination from "@/components/ui/Pagination.vue";

const PAGE_SIZE = 10;

// 庫存狀態（跟特典詳細頁一樣）
const STOCK_STATUS = {
  IN_STOCK: { label: "有庫存", tone: "in" },
  LOW_STOCK: { label: "即將售罄", tone: "low" },
  EXHAUSTED: { label: "已售罄", tone: "out" },
  UNKNOWN: { label: "未知", tone: "unknown" },
};

// 缺漏回報的審核狀態（後端 BenefitReportStatus）
const REVIEW_STATUS = {
  PENDING: { label: "審核中", tone: "low" },
  APPROVED: { label: "已通過", tone: "in" },
  REJECTED: { label: "未通過", tone: "out" },
};

function stockOf(code) {
  return STOCK_STATUS[code] ?? STOCK_STATUS.UNKNOWN;
}

function reviewOf(code) {
  return REVIEW_STATUS[code] ?? { label: code || "—", tone: "unknown" };
}

// ── 分頁標籤 ──
const TABS = [
  { key: "inventory", label: "庫存回報", icon: megaphoneIcon },
  { key: "benefit", label: "缺漏回報", icon: clipboardIcon },
];
const activeTab = ref("inventory");

// ── 庫存回報：GET /inventory-reports/me ──
const inventoryPage = ref(1);
const inventoryParams = computed(() => ({ page: inventoryPage.value, size: PAGE_SIZE }));
const inventory = useApiCollection("/inventory-reports/me", inventoryParams, { page: inventoryPage });

const inventoryColumns = [
  { key: "reportedAt", label: "回報時間", minWidth: "17rem", format: (v) => dateTimeText(v) },
  { key: "benefitName", label: "特典", minWidth: "16rem", emphasis: true },
  { key: "movieName", label: "電影", minWidth: "14rem" },
  { key: "cinemaName", label: "影城", minWidth: "12rem" },
  { key: "status", label: "我回報的狀態", minWidth: "10rem", align: "center" },
];

// ── 缺漏回報：GET /benefit-reports/me ──
const benefitPage = ref(1);
const benefitParams = computed(() => ({ page: benefitPage.value, size: PAGE_SIZE }));
const benefit = useApiCollection("/benefit-reports/me", benefitParams, { page: benefitPage });

const benefitColumns = [
  { key: "createdAt", label: "回報時間", minWidth: "17rem", format: (v) => dateTimeText(v) },
  { key: "benefitName", label: "特典名稱", minWidth: "16rem", emphasis: true },
  // 資料庫有的電影／影城用正式名稱，沒有就顯示使用者自己填的名稱
  { key: "movieName", label: "電影", minWidth: "14rem", format: (v, row) => v || row.customMovieName },
  { key: "cinemaName", label: "影城", minWidth: "12rem", format: (v, row) => v || row.customCinemaName },
  { key: "status", label: "審核狀態", minWidth: "9rem", align: "center" },
  { key: "note", label: "說明", minWidth: "16rem" },
];

// ── 目前分頁要顯示的資料 ──
const current = computed(() => (activeTab.value === "inventory" ? inventory : benefit));
const currentPage = computed(() => (activeTab.value === "inventory" ? inventoryPage : benefitPage));

const loadError = computed(() =>
  collectionLoadError(current.value.errorStatus.value, "目前無法取得回報紀錄"),
);

const listTop = ref(null);

function goToPage(next) {
  const pageRef = currentPage.value;
  if (!Number.isInteger(next) || next < 1 || next > current.value.totalPages.value || next === pageRef.value) return;
  pageRef.value = next;
  listTop.value?.scrollIntoView({
    behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth",
    block: "start",
  });
}
</script>

<template>
  <section>
    <PageIntro kicker="✦ MY REPORTS" lead="我的" mark="回報紀錄" description="看看你回報過的庫存與缺漏特典，以及審核結果。"
      :icon-left="megaphoneIcon" :icon-right="clipboardIcon" :icon-top="pinIcon" />

    <!-- 分頁標籤：數字是各自的總筆數 -->
    <div class="report-tabs" role="tablist" aria-label="回報種類">
      <button v-for="tab in TABS" :key="tab.key" type="button" role="tab" class="report-tab"
        :class="{ active: activeTab === tab.key }" :aria-selected="activeTab === tab.key" @click="activeTab = tab.key">
        <Icon :icon="tab.icon" aria-hidden="true" />
        {{ tab.label }}
        <span class="tab-count">
          {{ (tab.key === "inventory" ? inventory : benefit).totalElements.value }}
        </span>
      </button>
    </div>

    <StatePanel v-if="current.loading.value && !current.items.value.length" variant="loading" message="正在載入回報紀錄…"
      :icon="megaphoneIcon" />
    <StatePanel v-else-if="current.error.value" variant="error" :title="loadError.title" :message="loadError.message"
      @retry="current.retry" />

    <template v-else>
      <!-- 庫存回報 -->
      <template v-if="activeTab === 'inventory'">
        <ResultBanner :icon="megaphoneIcon" title="我的庫存回報" :count="inventory.totalElements.value" unit-text="筆回報"
          empty-text="還沒有回報過庫存，到特典頁幫大家回報吧！" action-to="/benefits" :action-icon="giftIcon" action-label="去看特典" />

        <div v-if="inventory.items.value.length" ref="listTop" class="report-sheet">
          <DataTable caption="我的庫存回報" :columns="inventoryColumns" :rows="inventory.items.value" row-key="reportId"
            :loading="inventory.loading.value">
            <template #cell-benefitName="{ row }">
              <RouterLink class="benefit-link" :to="`/benefits/${row.benefitId}`">
                {{ row.benefitName }}
                <Icon :icon="arrowIcon" aria-hidden="true" />
              </RouterLink>
            </template>
            <template #cell-status="{ row }">
              <span class="status-badge" :class="`tone-${stockOf(row.status).tone}`">
                {{ stockOf(row.status).label }}
              </span>
            </template>
          </DataTable>
        </div>
      </template>

      <!-- 缺漏回報 -->
      <template v-else>
        <ResultBanner :icon="clipboardIcon" title="我的缺漏回報" :count="benefit.totalElements.value" unit-text="筆回報"
          empty-text="還沒有回報過缺漏特典。" />

        <div v-if="benefit.items.value.length" ref="listTop" class="report-sheet">
          <DataTable caption="我的缺漏回報" :columns="benefitColumns" :rows="benefit.items.value" row-key="benefitReportId"
            :loading="benefit.loading.value">
            <template #cell-status="{ row }">
              <span class="status-badge" :class="`tone-${reviewOf(row.status).tone}`">
                {{ reviewOf(row.status).label }}
              </span>
            </template>
            <template #cell-note="{ row }">
              <!-- 通過：連到正式特典；未通過：顯示管理員寫的理由；審核中：提示等待 -->
              <RouterLink v-if="row.status === 'APPROVED' && row.benefitId" class="benefit-link"
                :to="`/benefits/${row.benefitId}`">
                查看特典
                <Icon :icon="arrowIcon" aria-hidden="true" />
              </RouterLink>
              <span v-else-if="row.status === 'REJECTED'" class="note-text">{{ row.reviewNote || "審核未通過" }}</span>
              <span v-else class="note-muted">等待管理員審核</span>
            </template>
          </DataTable>
        </div>
      </template>

      <Pagination v-if="current.totalPages.value > 1" :page="currentPage.value" :total-pages="current.totalPages.value"
        label="回報紀錄分頁" @update:page="goToPage" />
    </template>
  </section>
</template>

<style scoped>
/* ── 分頁標籤 ── */
.report-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin-bottom: 1rem;
}

.report-tab {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1.1rem;
  border: 1.5px solid #ff7049;
  border-radius: 999px;
  background: #fff;
  color: #ff7049;
  font-size: var(--text-control);
  font-weight: 800;
  cursor: pointer;
}

.report-tab.active {
  background: #ff7049;
  color: #fff;
}

.report-tab:focus-visible {
  outline: 2px solid #ff7049;
  outline-offset: 3px;
}

.tab-count {
  min-width: 1.6rem;
  padding: 0 0.4rem;
  border-radius: 999px;
  background: color-mix(in srgb, #ff7049 14%, white);
  color: #9a4632;
  font-size: var(--text-min);
  text-align: center;
}

.report-sheet {
  scroll-margin-top: 5.5rem;
}

/* ── 表格內容 ── */
.benefit-link {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  color: #ff7049;
  font-weight: 800;
  text-decoration: none;
}

.benefit-link:hover {
  color: #9a4632;
}

.note-text {
  color: #a33b42;
  font-weight: 700;
}

.note-muted {
  color: var(--c-muted);
}

/* 狀態徽章：綠＝有庫存／通過、橘＝即將售罄／審核中、紅＝售罄／未通過、灰＝未知 */
.status-badge {
  display: inline-block;
  padding: 0.2rem 0.7rem;
  border-radius: 999px;
  background: color-mix(in srgb, var(--chip-ink) 12%, white);
  color: var(--chip-ink);
  font-size: var(--text-min);
  font-weight: 800;
  white-space: nowrap;
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
</style>
