<script setup>
import { computed, ref } from "vue";
import historyIcon from "@iconify-icons/mdi/history";
import clockIcon from "@iconify-icons/mdi/clock-check-outline";
import tableIcon from "@iconify-icons/mdi/table-large";
import filterIcon from "@iconify-icons/mdi/filter-variant";
import sortIcon from "@iconify-icons/mdi/sort";
import shapeIcon from "@iconify-icons/mdi/shape-outline";
import statusIcon from "@iconify-icons/mdi/list-status";
import {
  ADMIN_CRAWLER_LOG_SORT_DEFAULT,
  ADMIN_CRAWLER_LOG_SORT_OPTIONS,
  parseSort,
} from "@/constants/sort";
import { useApiCollection } from "@/composables/useApiCollection";
import { collectionLoadError } from "@/utils/apiError";
import PageIntro from "@/components/layout/PageIntro.vue";
import ResultBanner from "@/components/layout/ResultBanner.vue";
import SearchFilter from "@/components/layout/SearchFilter.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import DataTable, { dateTimeText } from "@/components/ui/DataTable.vue";
import Pagination from "@/components/ui/Pagination.vue";

const PAGE_SIZE = 20;
const CRAWLER_TYPE_OPTIONS = [
  { value: "CINEMA", label: "影城" },
  { value: "MOVIE", label: "電影" },
  { value: "BENEFIT", label: "特典" },
];
const CRAWLER_STATUS_OPTIONS = [
  { value: "SUCCESS", label: "成功" },
  { value: "PARTIAL", label: "部分成功" },
  { value: "FAILURE", label: "失敗" },
];

function crawlerTypeLabel(value) {
  return CRAWLER_TYPE_OPTIONS.find((option) => option.value === value)?.label ?? value ?? "";
}

function crawlerStatusLabel(value) {
  return CRAWLER_STATUS_OPTIONS.find((option) => option.value === value)?.label ?? value ?? "";
}

function countText(value) {
  return value == null || value === "" ? "" : String(value);
}

const filters = ref({
  crawlerType: "",
  crawlerStatus: "",
  sortKey: "",
});
const page = ref(1);
const listTop = ref(null);

const appliedFilters = computed({
  get: () => filters.value,
  set: (next) => {
    filters.value = {
      crawlerType: next?.crawlerType ?? "",
      crawlerStatus: next?.crawlerStatus ?? "",
      sortKey: next?.sortKey ?? "",
    };
    page.value = 1;
  },
});

const filterFields = [
  {
    key: "crawlerType",
    label: "爬蟲類型",
    type: "select",
    emptyLabel: "全部類型",
    icon: shapeIcon,
    options: CRAWLER_TYPE_OPTIONS,
  },
  {
    key: "crawlerStatus",
    label: "執行狀態",
    type: "select",
    emptyLabel: "全部狀態",
    icon: statusIcon,
    options: CRAWLER_STATUS_OPTIONS,
  },
  {
    key: "sortKey",
    label: "排序",
    type: "select",
    emptyLabel: ADMIN_CRAWLER_LOG_SORT_DEFAULT.label,
    icon: sortIcon,
    options: ADMIN_CRAWLER_LOG_SORT_OPTIONS,
  },
];

const columns = [
  { key: "crawlerLogId", label: "編號", minWidth: "6rem", nowrap: true, align: "center" },
  {
    key: "crawlerType",
    label: "類型",
    minWidth: "6rem",
    nowrap: true,
    align: "center",
    format: crawlerTypeLabel,
  },
  {
    key: "crawlerStatus",
    label: "狀態",
    minWidth: "7.5rem",
    nowrap: true,
    align: "center",
  },
  { key: "fetchedCount", label: "抓取", minWidth: "5rem", nowrap: true, align: "center", format: countText },
  { key: "createdCount", label: "新增", minWidth: "5rem", nowrap: true, align: "center", format: countText },
  { key: "updatedCount", label: "更新", minWidth: "5rem", nowrap: true, align: "center", format: countText },
  { key: "skippedCount", label: "略過", minWidth: "5rem", nowrap: true, align: "center", format: countText },
  {
    key: "pendingReviewCount",
    label: "待審核",
    minWidth: "6rem",
    nowrap: true,
    align: "center",
    format: countText,
  },
  { key: "message", label: "訊息", minWidth: "16rem" },
  {
    key: "executedAt",
    label: "開始時間",
    minWidth: "12rem",
    nowrap: true,
    align: "center",
    format: dateTimeText,
  },
  {
    key: "finishedAt",
    label: "結束時間",
    minWidth: "12rem",
    nowrap: true,
    align: "center",
    format: dateTimeText,
  },
];

const requestParams = computed(() => {
  const { orderBy, sort } = parseSort(
    filters.value.sortKey,
    ADMIN_CRAWLER_LOG_SORT_DEFAULT,
    ADMIN_CRAWLER_LOG_SORT_OPTIONS,
  );
  const params = {
    page: page.value,
    size: PAGE_SIZE,
    orderBy,
    sort,
  };
  if (filters.value.crawlerType) params.crawlerType = filters.value.crawlerType;
  if (filters.value.crawlerStatus) params.crawlerStatus = filters.value.crawlerStatus;
  return params;
});

const {
  items: logs,
  totalElements,
  totalPages,
  loading,
  error,
  errorStatus,
  retry,
} = useApiCollection("/crawler-logs", requestParams, { page });

const hasFilters = computed(
  () => Boolean(filters.value.crawlerType) || Boolean(filters.value.crawlerStatus),
);

const resultUnit = computed(() =>
  hasFilters.value ? "筆符合條件的紀錄" : "筆紀錄",
);

const loadError = computed(() => collectionLoadError(errorStatus.value, "目前無法取得爬蟲紀錄"));

function goToPage(next) {
  if (!Number.isInteger(next) || next < 1 || next > totalPages.value || next === page.value) return;
  page.value = next;
  listTop.value?.scrollIntoView({
    behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth",
    block: "start",
  });
}
</script>

<template>
  <section>
    <PageIntro kicker="✦ CRAWLER LOG" lead="爬蟲" mark="紀錄" description="查看各次爬蟲的執行狀態、筆數與訊息。" :icon-left="historyIcon"
      :icon-right="clockIcon" :icon-top="tableIcon" theme="purple" />

    <ResultBanner v-if="!(loading && !logs.length) && !error" :icon="historyIcon" title="爬蟲執行紀錄" :count="totalElements"
      :unit-text="resultUnit" empty-text="目前沒有符合條件的紀錄。" theme="purple" />

    <SearchFilter v-if="!(loading && !logs.length) && !error" v-model="appliedFilters" title="篩選紀錄"
      :title-icon="filterIcon" submit-label="查詢紀錄" :fields="filterFields" theme="purple">
      <div ref="listTop" class="log-sheet">
        <DataTable caption="爬蟲紀錄資料表" :columns="columns" :rows="logs" row-key="crawlerLogId" empty-text="目前沒有符合條件的紀錄。"
          :loading="loading" theme="purple">
          <template #cell-crawlerStatus="{ row }">
            {{ crawlerStatusLabel(row.crawlerStatus) || "—" }}
          </template>
        </DataTable>
      </div>

      <Pagination v-if="totalPages > 1" :page="page" :total-pages="totalPages" label="爬蟲紀錄分頁" color="purple"
        @update:page="goToPage" />
    </SearchFilter>

    <StatePanel v-if="loading && !logs.length" variant="loading" message="正在載入爬蟲紀錄…" :icon="historyIcon"
      color="purple" />
    <StatePanel v-else-if="error" variant="error" :title="loadError.title" :message="loadError.message" color="purple"
      @retry="retry" />
  </section>
</template>

<style scoped>
.log-sheet {
  scroll-margin-top: 5.5rem;
}
</style>
