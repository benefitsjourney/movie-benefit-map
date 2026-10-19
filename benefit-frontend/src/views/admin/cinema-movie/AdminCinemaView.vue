<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import movieIcon from "@iconify-icons/mdi/movie-open-outline";
import pinIcon from "@iconify-icons/mdi/map-marker-outline";
import tableIcon from "@iconify-icons/mdi/table-large";
import searchIcon from "@iconify-icons/mdi/magnify";
import filterIcon from "@iconify-icons/mdi/filter-variant";
import sortIcon from "@iconify-icons/mdi/sort";
import eyeIcon from "@iconify-icons/mdi/eye-outline";
import storeIcon from "@iconify-icons/mdi/store-outline";
import plusIcon from "@iconify-icons/mdi/plus";
import { openAppAlert } from "@/components/ui/Alert.vue";

import { COUNTY_LABELS, countyLabel } from "@/constants/county";
import {
  ADMIN_CINEMA_SORT_DEFAULT,
  ADMIN_CINEMA_SORT_OPTIONS,
  parseSort,
} from "@/constants/sort";
import { useApiCollection } from "@/composables/useApiCollection";
import http from "@/plugins/axios";
import PageIntro from "@/components/layout/PageIntro.vue";
import ResultBanner from "@/components/layout/ResultBanner.vue";
import SearchFilter from "@/components/layout/SearchFilter.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import DataTable, { dateTimeText } from "@/components/ui/DataTable.vue";
import Pagination from "@/components/ui/Pagination.vue";
import AppButton from "@/components/ui/Button.vue";
import Switch from "@/components/ui/Switch.vue";

const router = useRouter();
const PAGE_SIZE = 20;
const ACTIVE_OPTIONS = [
  { value: "true", label: "營業中" },
  { value: "false", label: "已歇業" },
];
const VISIBLE_OPTIONS = [
  { value: "true", label: "顯示" },
  { value: "false", label: "隱藏" },
];

const filters = ref({
  keyword: "",
  county: "",
  active: "",
  visible: "",
  sortKey: "",
});
const page = ref(1);
const listTop = ref(null);
const pendingVisibility = ref({});

const appliedFilters = computed({
  get: () => filters.value,
  set: (next) => {
    filters.value = {
      keyword: next?.keyword ?? "",
      county: next?.county ?? "",
      active: next?.active ?? "",
      visible: next?.visible ?? "",
      sortKey: next?.sortKey ?? "",
    };
    page.value = 1;
  },
});

const filterFields = [
  {
    key: "keyword",
    label: "影城名稱",
    type: "search",
    placeholder: "搜尋影城名稱",
    icon: searchIcon,
  },
  {
    key: "county",
    label: "縣市",
    type: "select",
    emptyLabel: "全部縣市",
    icon: pinIcon,
    options: COUNTY_LABELS,
  },
  {
    key: "active",
    label: "營業中",
    type: "select",
    emptyLabel: "全部",
    icon: storeIcon,
    options: ACTIVE_OPTIONS,
  },
  {
    key: "visible",
    label: "前台顯示",
    type: "select",
    emptyLabel: "全部",
    icon: eyeIcon,
    options: VISIBLE_OPTIONS,
  },
  {
    key: "sortKey",
    label: "排序",
    type: "select",
    emptyLabel: ADMIN_CINEMA_SORT_DEFAULT.label,
    icon: sortIcon,
    options: ADMIN_CINEMA_SORT_OPTIONS,
  },
];

const columns = [
  { key: "cinemaId", label: "編號", minWidth: "6rem", nowrap: true, align: "center" },
  { key: "cinemaName", label: "影城名稱", minWidth: "18rem" },
  {
    key: "county",
    label: "縣市",
    minWidth: "6rem",
    nowrap: true,
    align: "center",
    format: (value) => countyLabel(value),
  },
  {
    key: "isActive",
    label: "營業狀態",
    minWidth: "7.5rem",
    nowrap: true,
    align: "center",
    format: (value) => (value === true ? "營業中" : "已歇業"),
  },
  {
    key: "updatedAt",
    label: "更新時間",
    minWidth: "12rem",
    nowrap: true,
    align: "center",
    format: dateTimeText,
  },
  {
    key: "isVisible",
    label: "前台顯示",
    minWidth: "7.5rem",
    nowrap: true,
    align: "center",
  },
  { key: "actions", label: "操作", minWidth: "8rem", align: "center" },
];

function flagParam(value) {
  if (value === "true") return true;
  if (value === "false") return false;
  return undefined;
}

const requestParams = computed(() => {
  const { orderBy, sort } = parseSort(
    filters.value.sortKey,
    ADMIN_CINEMA_SORT_DEFAULT,
    ADMIN_CINEMA_SORT_OPTIONS,
  );
  const params = {
    page: page.value,
    size: PAGE_SIZE,
    orderBy,
    sort,
  };
  const keyword = filters.value.keyword?.trim();
  if (keyword) params.keyword = keyword;
  if (filters.value.county) params.county = filters.value.county;
  const isActive = flagParam(filters.value.active);
  const isVisible = flagParam(filters.value.visible);
  if (isActive !== undefined) params.isActive = isActive;
  if (isVisible !== undefined) params.isVisible = isVisible;
  return params;
});

const {
  items: cinemas,
  totalElements,
  totalPages,
  loading,
  error,
  retry,
} = useApiCollection("/cinemas", requestParams, { page });

const hasFilters = computed(
  () =>
    Boolean(filters.value.keyword?.trim()) ||
    Boolean(filters.value.county) ||
    Boolean(filters.value.active) ||
    Boolean(filters.value.visible),
);

const resultUnit = computed(() =>
  hasFilters.value ? "間符合條件的影城" : "間影城",
);

function apiMessage(error, fallback) {
  const message = error?.response?.data?.message;
  return typeof message === "string" && message.trim() ? message.trim() : fallback;
}

function isUnauthorized(error) {
  return error?.response?.status === 401;
}

function openCreate() {
  router.push({ name: "admin-cinema-create" });
}

function openDetail(cinemaId) {
  router.push({ name: "admin-cinema-detail", params: { id: cinemaId } });
}

async function toggleVisible(row, next) {
  const cinemaId = row?.cinemaId;
  if (cinemaId == null || pendingVisibility.value[cinemaId]) return;

  const previous = row.isVisible;
  // 先更新列資料維持操作回饋；請求失敗時再還原。
  row.isVisible = next;
  pendingVisibility.value = { ...pendingVisibility.value, [cinemaId]: true };

  try {
    const { data } = await http.patch(`/cinemas/${cinemaId}`, { isVisible: next });
    if (data && typeof data === "object") Object.assign(row, data);
  } catch (error) {
    row.isVisible = previous;
    await openAppAlert({
      variant: "error",
      title: "無法更新顯示狀態",
      text: isUnauthorized(error) ? "請登入管理員帳號" : apiMessage(error, "請確認連線後再試一次。"),
      confirmText: "關閉",
      cancelText: "",
    });
  } finally {
    const nextPending = { ...pendingVisibility.value };
    delete nextPending[cinemaId];
    pendingVisibility.value = nextPending;
  }
}

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
    <PageIntro kicker="✦ CINEMA DATA" lead="影城" mark="資料" description="管理影城資料，查看縣市、營業狀態及顯示狀態。" :icon-left="movieIcon"
      :icon-right="pinIcon" :icon-top="tableIcon" theme="purple" />

    <ResultBanner v-if="!(loading && !cinemas.length) && !error" :icon="movieIcon" title="收錄中的影城" :count="totalElements"
      :unit-text="resultUnit" empty-text="目前沒有符合條件的影城。" theme="purple">
      <template #action>
        <AppButton type="button" color="purple" :icon="plusIcon" @click="openCreate">
          新增影城
        </AppButton>
      </template>
    </ResultBanner>

    <SearchFilter v-if="!(loading && !cinemas.length) && !error" v-model="appliedFilters" title="篩選影城"
      :title-icon="filterIcon" submit-label="查詢影城" :fields="filterFields" theme="purple">
      <div ref="listTop" class="cinema-sheet">
        <DataTable caption="影城資料表" :columns="columns" :rows="cinemas" row-key="cinemaId" empty-text="目前沒有符合條件的影城。"
          :loading="loading" theme="purple">
          <template #cell-isVisible="{ row }">
            <div class="visible-switch">
              <Switch :model-value="row.isVisible === true" :disabled="Boolean(pendingVisibility[row.cinemaId])"
                :aria-label="`${row.cinemaName || '影城'}的前台顯示`" @update:model-value="toggleVisible(row, $event)" />
            </div>
          </template>
          <template #cell-actions="{ row }">
            <div class="cinema-actions">
              <AppButton type="button" color="purple" variant="outline" :icon="eyeIcon"
                @click="openDetail(row.cinemaId)">
                詳細
              </AppButton>
            </div>
          </template>
        </DataTable>
      </div>

      <Pagination v-if="totalPages > 1" :page="page" :total-pages="totalPages" label="影城列表分頁" color="purple"
        @update:page="goToPage" />
    </SearchFilter>

    <StatePanel v-if="loading && !cinemas.length" variant="loading" message="正在載入影城資料…" :icon="movieIcon"
      color="purple" />
    <StatePanel v-else-if="error" variant="error" title="目前無法取得影城資料" message="請確認連線後再試一次。" color="purple"
      @retry="retry" />
  </section>
</template>

<style scoped>
.cinema-sheet {
  scroll-margin-top: 5.5rem;
}

.cinema-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0.4rem;
}

.cinema-actions :deep(.app-button) {
  min-height: 2.25rem;
  padding: 0.3rem 0.8rem;
  box-shadow: none;
}

.visible-switch {
  display: flex;
  justify-content: center;
}

.visible-switch :deep(.app-switch) {
  min-height: 0;
  --form-accent: #8d72ed;
}
</style>
