<script setup>
import { computed, ref } from "vue";
import { Icon } from "@iconify/vue";
import megaphoneIcon from "@iconify-icons/mdi/bullhorn-outline";
import tableIcon from "@iconify-icons/mdi/table-large";
import pinIcon from "@iconify-icons/mdi/map-marker-outline";
import deleteIcon from "@iconify-icons/mdi/trash-can-outline";
import linkIcon from "@iconify-icons/mdi/open-in-new";

import http from "@/plugins/axios";
import { useApiCollection } from "@/composables/useApiCollection";
import { openAppAlert } from "@/components/ui/Alert.vue";
import PageIntro from "@/components/layout/PageIntro.vue";
import ResultBanner from "@/components/layout/ResultBanner.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import DataTable, { dateTimeText } from "@/components/ui/DataTable.vue";
import Pagination from "@/components/ui/Pagination.vue";
import AppButton from "@/components/ui/Button.vue";

const PAGE_SIZE = 20;

// 後端 BenefitInventoryStatus 對應的中文與顏色（跟特典詳細頁一樣）
const STOCK_STATUS = {
  IN_STOCK: { label: "有庫存", tone: "in" },
  LOW_STOCK: { label: "即將售罄", tone: "low" },
  EXHAUSTED: { label: "已售罄", tone: "out" },
  UNKNOWN: { label: "未知", tone: "unknown" },
};

function statusOf(code) {
  return STOCK_STATUS[code] ?? STOCK_STATUS.UNKNOWN;
}

function apiMessage(error, fallback) {
  const message = error?.response?.data?.message;
  return typeof message === "string" && message.trim() ? message.trim() : fallback;
}

function showError(title, error, fallback = "請確認連線後再試一次。") {
  const code = error?.response?.status;
  const text =
    code === 401 ? "請先登入管理員帳號。"
      : code === 403 ? "這個帳號沒有管理員權限。"
        : apiMessage(error, fallback);
  return openAppAlert({ variant: "error", title, text, confirmText: "關閉", cancelText: "" });
}

// ── 列表：GET /inventory-reports?page=&size=（後端已依回報時間新到舊排序）──
const page = ref(1);
const listTop = ref(null);

const requestParams = computed(() => ({ page: page.value, size: PAGE_SIZE }));

const {
  items: reports,
  totalElements,
  totalPages,
  loading,
  error,
  retry,
} = useApiCollection("/inventory-reports", requestParams, { page });

const columns = [
  { key: "reportId", label: "編號", minWidth: "6rem", align: "center" },
  { key: "reportedAt", label: "回報時間", minWidth: "17rem", format: (v) => dateTimeText(v) },
  { key: "benefitName", label: "特典", minWidth: "16rem", emphasis: true },
  { key: "movieName", label: "電影", minWidth: "14rem" },
  { key: "cinemaName", label: "影城", minWidth: "12rem" },
  { key: "status", label: "回報狀態", minWidth: "9rem", align: "center" },
  { key: "actions", label: "操作", minWidth: "15rem", align: "center" },
];

function goToPage(next) {
  if (!Number.isInteger(next) || next < 1 || next > totalPages.value || next === page.value) return;
  page.value = next;
  listTop.value?.scrollIntoView({
    behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth",
    block: "start",
  });
}

// ── 刪除：DELETE /inventory-reports/{reportId} ──
// 後端會一起刪掉這筆的貢獻紀錄，並重新計算這間影城的庫存狀態
const deletingId = ref(null);

async function removeReport(row) {
  if (deletingId.value) return;

  const result = await openAppAlert({
    variant: "warning",
    title: `刪除回報 #${row.reportId}？`,
    text: `「${row.cinemaName}」的「${row.benefitName}」回報為「${statusOf(row.status).label}」。刪除後會一併移除這筆貢獻，並重新計算該影城的庫存狀態，無法復原。`,
    confirmText: "刪除",
    cancelText: "取消",
  });
  if (!result.isConfirmed) return;

  deletingId.value = row.reportId;
  try {
    await http.delete(`/inventory-reports/${row.reportId}`);
    await openAppAlert({
      variant: "success",
      title: "已刪除",
      text: `回報 #${row.reportId} 已刪除。`,
      confirmText: "關閉",
      cancelText: "",
    });
    retry(); // 重新載入目前這一頁
  } catch (e) {
    await showError("刪除失敗", e);
  } finally {
    deletingId.value = null;
  }
}
</script>

<template>
  <section>
    <PageIntro kicker="✦ INVENTORY REPORTS" lead="庫存" mark="回報管理" description="查看會員回報的特典庫存，刪除惡意或誤報的回報。"
      :icon-left="megaphoneIcon" :icon-right="pinIcon" :icon-top="tableIcon" theme="purple" />

    <template v-if="!(loading && !reports.length) && !error">
      <ResultBanner :icon="megaphoneIcon" title="全部庫存回報" :count="totalElements" unit-text="筆回報"
        empty-text="目前還沒有任何庫存回報。" theme="purple" />

      <div ref="listTop" class="report-sheet">
        <DataTable caption="庫存回報資料表" :columns="columns" :rows="reports" row-key="reportId"
          empty-text="目前還沒有任何庫存回報。" :loading="loading" theme="purple">
          <template #cell-status="{ row }">
            <span class="stock-badge" :class="`tone-${statusOf(row.status).tone}`">
              {{ statusOf(row.status).label }}
            </span>
          </template>

          <template #cell-actions="{ row }">
            <div class="row-actions">
              <RouterLink class="view-link" :to="`/benefits/${row.benefitId}`" target="_blank" title="看前台特典頁">
                <Icon :icon="linkIcon" aria-hidden="true" />
                前台
              </RouterLink>
              <AppButton type="button" color="purple" variant="outline" :icon="deleteIcon"
                :disabled="deletingId === row.reportId" @click="removeReport(row)">
                {{ deletingId === row.reportId ? "刪除中…" : "刪除" }}
              </AppButton>
            </div>
          </template>
        </DataTable>
      </div>

      <Pagination v-if="totalPages > 1" :page="page" :total-pages="totalPages" label="庫存回報分頁" color="purple"
        @update:page="goToPage" />
    </template>

    <StatePanel v-if="loading && !reports.length" variant="loading" message="正在載入庫存回報…" :icon="megaphoneIcon"
      color="purple" />
    <StatePanel v-else-if="error" variant="error" title="目前無法取得庫存回報" message="請確認已用管理員帳號登入，或稍後再試。"
      color="purple" @retry="retry" />
  </section>
</template>

<style scoped>
.report-sheet {
  scroll-margin-top: 5.5rem;
}

.row-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
}

.view-link {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  color: #8d72ed;
  font-size: var(--text-control);
  font-weight: 800;
  text-decoration: none;
  white-space: nowrap;
}

.view-link:hover {
  color: #5c4696;
}

/* 狀態徽章：綠＝有、橘＝即將售罄、紅＝售罄、灰＝未知 */
.stock-badge {
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
