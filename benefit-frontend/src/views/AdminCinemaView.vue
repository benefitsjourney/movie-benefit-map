<script setup>
import { computed, ref } from "vue";
import movieIcon from "@iconify-icons/mdi/movie-open-outline";
import pinIcon from "@iconify-icons/mdi/map-marker-outline";
import tableIcon from "@iconify-icons/mdi/table-large";
import searchIcon from "@iconify-icons/mdi/magnify";
import filterIcon from "@iconify-icons/mdi/filter-variant";
import sortIcon from "@iconify-icons/mdi/sort";
import eyeIcon from "@iconify-icons/mdi/eye-outline";
import storeIcon from "@iconify-icons/mdi/store-outline";

import { COUNTY_LABELS, countyLabel } from "@/constants/county";
import { useApiCollection } from "@/composables/useApiCollection";
import PageIntro from "@/components/layout/PageIntro.vue";
import ResultBanner from "@/components/layout/ResultBanner.vue";
import SearchFilter from "@/components/layout/SearchFilter.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import DataTable from "@/components/ui/DataTable.vue";
import Pagination from "@/components/ui/Pagination.vue";

const PAGE_SIZE = 20;
const SORT_DEFAULT = { orderBy: "CINEMA_ID", sort: "ASC" };
const SORT_OPTIONS = [
  { value: "CINEMA_ID:DESC", label: "編號（大 → 小）" },
  { value: "COUNTY:ASC", label: "縣市（北 → 南）" },
  { value: "COUNTY:DESC", label: "縣市（南 → 北）" },
  { value: "UPDATED_AT:DESC", label: "更新時間（新 → 舊）" },
  { value: "UPDATED_AT:ASC", label: "更新時間（舊 → 新）" },
  { value: "CREATED_AT:DESC", label: "建立時間（新 → 舊）" },
  { value: "CREATED_AT:ASC", label: "建立時間（舊 → 新）" },
];
const SORT_KEYS = new Set(["CINEMA_ID:ASC", ...SORT_OPTIONS.map((option) => option.value)]);
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
    emptyLabel: "編號（小 → 大）",
    icon: sortIcon,
    options: SORT_OPTIONS,
  },
];

const columns = [
  { key: "cinemaId", label: "編號", minWidth: "5.5rem", nowrap: true, align: "center" },
  { key: "sourceId", label: "來源編號", minWidth: "8rem", nowrap: true, align: "center" },
  { key: "cinemaName", label: "影城名稱", minWidth: "12rem" },
  {
    key: "county",
    label: "縣市",
    minWidth: "6rem",
    nowrap: true,
    align: "center",
    format: (value) => countyLabel(value),
  },
  { key: "cinemaAddress", label: "地址", minWidth: "16rem" },
  {
    key: "latitude",
    label: "緯度",
    minWidth: "8.5rem",
    nowrap: true,
    align: "center",
    format: coordText,
  },
  {
    key: "longitude",
    label: "經度",
    minWidth: "8.5rem",
    nowrap: true,
    align: "center",
    format: coordText,
  },
  { key: "officialWebsiteUrl", label: "官網", minWidth: "14rem", type: "link" },
  {
    key: "isActive",
    label: "營業狀態",
    minWidth: "7rem",
    nowrap: true,
    align: "center",
    format: (value) => (value === true ? "營業中" : value === false ? "已歇業" : ""),
  },
  {
    key: "isVisible",
    label: "前台顯示",
    minWidth: "7rem",
    nowrap: true,
    align: "center",
    format: (value) => (value === true ? "顯示" : value === false ? "隱藏" : ""),
  },
  { key: "createdAt", label: "建立時間", minWidth: "12rem", nowrap: true, align: "center" },
  { key: "updatedAt", label: "更新時間", minWidth: "12rem", nowrap: true, align: "center" },
];

function parseSort(sortKey) {
  const key = String(sortKey ?? "");
  if (!SORT_KEYS.has(key)) return SORT_DEFAULT;
  const [orderBy, sort] = key.split(":");
  return { orderBy, sort };
}

function coordText(value) {
  if (value == null || value === "") return "";
  const number = Number(value);
  return Number.isFinite(number) ? number.toFixed(6) : String(value);
}

function flagParam(value) {
  if (value === "true") return true;
  if (value === "false") return false;
  return undefined;
}

const requestParams = computed(() => {
  const { orderBy, sort } = parseSort(filters.value.sortKey);
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
    <PageIntro kicker="✦ CINEMA DATA" lead="影城" mark="資料" description="查看目前收錄的影城名稱、地址、座標與顯示狀態。" :icon-left="movieIcon"
      :icon-right="pinIcon" :icon-top="tableIcon" theme="purple" />

    <SearchFilter v-model="appliedFilters" title="篩選影城" :title-icon="filterIcon" submit-label="查詢影城"
      :fields="filterFields" theme="purple" />

    <StatePanel v-if="loading && !cinemas.length" variant="loading" message="正在載入影城資料…" :icon="movieIcon"
      color="purple" />
    <StatePanel v-else-if="error" variant="error" title="目前無法取得影城資料" message="請確認連線後再試一次。" color="purple"
      @retry="retry" />

    <template v-else>
      <ResultBanner :icon="movieIcon" title="收錄中的影城" :count="totalElements" :unit-text="resultUnit"
        empty-text="目前沒有符合條件的影城。" action-to="/cinemas" :action-icon="pinIcon" action-label="前往前台影城" theme="purple" />

      <div ref="listTop" class="cinema-sheet">
        <DataTable caption="影城資料表" :columns="columns" :rows="cinemas" row-key="cinemaId" empty-text="目前沒有符合條件的影城。"
          :loading="loading" theme="purple" />
      </div>

      <Pagination v-if="totalPages > 1" :page="page" :total-pages="totalPages" label="影城列表分頁" color="purple"
        @update:page="goToPage" />
    </template>
  </section>
</template>

<style scoped>
.cinema-sheet {
  scroll-margin-top: 5.5rem;
}
</style>
