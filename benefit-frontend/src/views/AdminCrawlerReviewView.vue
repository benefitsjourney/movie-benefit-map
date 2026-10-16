<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import clipboardIcon from "@iconify-icons/mdi/clipboard-check-outline";
import sourceBranchIcon from "@iconify-icons/mdi/source-branch";
import tableIcon from "@iconify-icons/mdi/table-large";
import filterIcon from "@iconify-icons/mdi/filter-variant";
import sortIcon from "@iconify-icons/mdi/sort";
import shapeIcon from "@iconify-icons/mdi/shape-outline";
import eyeIcon from "@iconify-icons/mdi/eye-outline";

import { countyLabel } from "@/constants/county";
import {
  ADMIN_CRAWLER_REVIEW_SORT_DEFAULT,
  ADMIN_CRAWLER_REVIEW_SORT_OPTIONS,
  parseSort,
} from "@/constants/sort";
import { useApiCollection } from "@/composables/useApiCollection";
import PageIntro from "@/components/layout/PageIntro.vue";
import ResultBanner from "@/components/layout/ResultBanner.vue";
import SearchFilter from "@/components/layout/SearchFilter.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import DataTable, { dateTimeText } from "@/components/ui/DataTable.vue";
import Pagination from "@/components/ui/Pagination.vue";
import AppButton from "@/components/ui/Button.vue";

const router = useRouter();
const PAGE_SIZE = 20;
const CRAWLER_TYPE_OPTIONS = [
  { value: "CINEMA", label: "影城" },
  { value: "MOVIE", label: "電影" },
  { value: "BENEFIT", label: "優惠" },
];
const REVIEW_STATUS_OPTIONS = [
  { value: "PENDING", label: "待審核" },
  { value: "APPROVED", label: "已接受" },
  { value: "REJECTED", label: "已拒絕" },
];
const REVIEW_FIELD_LABELS = {
  cinema_name: "影城名稱",
  cinema_address: "地址",
  county: "縣市",
  latitude: "緯度",
  longitude: "經度",
  official_website_url: "官網",
  is_active: "營業狀態",
  is_visible: "前台顯示",
  movie_name: "電影名稱",
  image_url: "電影圖片",
  release_date: "上映日期",
};
const ENTITY_ROUTES = {
  CINEMA: "admin-cinema-detail",
  MOVIE: "admin-movie-detail",
};

function crawlerTypeLabel(value) {
  return CRAWLER_TYPE_OPTIONS.find((option) => option.value === value)?.label ?? value ?? "";
}

function reviewStatusLabel(value) {
  return REVIEW_STATUS_OPTIONS.find((option) => option.value === value)?.label ?? value ?? "";
}

function reviewFieldLabel(value) {
  return REVIEW_FIELD_LABELS[value] ?? value ?? "";
}

function reviewValueText(value, row) {
  if (value == null || value === "") return "";
  if (row?.fieldName === "county") return countyLabel(value);
  if (row?.fieldName === "is_active") {
    if (value === "true") return "營業中";
    if (value === "false") return "已歇業";
  }
  if (row?.fieldName === "is_visible") {
    if (value === "true") return "顯示";
    if (value === "false") return "隱藏";
  }
  return dateTimeText(value);
}

function reviewEntityRoute(row) {
  const name = ENTITY_ROUTES[row?.crawlerType];
  const id = Number(row?.entityId);
  if (!name || !Number.isInteger(id) || id <= 0) return null;
  return { name, params: { id } };
}

const filters = ref({
  crawlerType: "",
  reviewStatus: "PENDING",
  sortKey: "",
});
const page = ref(1);
const listTop = ref(null);

const appliedFilters = computed({
  get: () => filters.value,
  set: (next) => {
    filters.value = {
      crawlerType: next?.crawlerType ?? "",
      reviewStatus: next?.reviewStatus ?? "",
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
    key: "reviewStatus",
    label: "審查狀態",
    type: "select",
    emptyLabel: "全部狀態",
    icon: clipboardIcon,
    options: REVIEW_STATUS_OPTIONS,
  },
  {
    key: "sortKey",
    label: "排序",
    type: "select",
    emptyLabel: ADMIN_CRAWLER_REVIEW_SORT_DEFAULT.label,
    icon: sortIcon,
    options: ADMIN_CRAWLER_REVIEW_SORT_OPTIONS,
  },
];

const columns = [
  { key: "crawlerReviewId", label: "編號", minWidth: "6rem", nowrap: true, align: "center" },
  {
    key: "crawlerType",
    label: "類型",
    minWidth: "6rem",
    nowrap: true,
    align: "center",
    format: crawlerTypeLabel,
  },
  { key: "entityId", label: "資料編號", minWidth: "7rem", nowrap: true, align: "center" },
  {
    key: "fieldName",
    label: "欄位",
    minWidth: "8rem",
    nowrap: true,
    align: "center",
    format: reviewFieldLabel,
  },
  { key: "crawlerValue", label: "爬蟲值", minWidth: "12rem", format: reviewValueText },
  { key: "previousValue", label: "原值", minWidth: "12rem", format: reviewValueText },
  {
    key: "reviewStatus",
    label: "審查狀態",
    minWidth: "7rem",
    nowrap: true,
    align: "center",
    format: reviewStatusLabel,
  },
  {
    key: "crawledAt",
    label: "爬取時間",
    minWidth: "12rem",
    nowrap: true,
    align: "center",
    format: dateTimeText,
  },
  { key: "actions", label: "操作", minWidth: "8rem", align: "center" },
];

const requestParams = computed(() => {
  const { orderBy, sort } = parseSort(
    filters.value.sortKey,
    ADMIN_CRAWLER_REVIEW_SORT_DEFAULT,
    ADMIN_CRAWLER_REVIEW_SORT_OPTIONS,
  );
  const params = {
    page: page.value,
    size: PAGE_SIZE,
    orderBy,
    sort,
  };
  if (filters.value.crawlerType) params.crawlerType = filters.value.crawlerType;
  if (filters.value.reviewStatus) params.reviewStatus = filters.value.reviewStatus;
  return params;
});

const {
  items: reviews,
  totalElements,
  totalPages,
  loading,
  error,
  retry,
} = useApiCollection("/crawler-reviews", requestParams, { page });

const hasFilters = computed(
  () => Boolean(filters.value.crawlerType) || Boolean(filters.value.reviewStatus),
);

const resultUnit = computed(() =>
  hasFilters.value ? "筆符合條件的審核" : "筆審核",
);

function openDetail(reviewId) {
  router.push({ name: "admin-crawler-review-detail", params: { id: reviewId } });
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
    <PageIntro kicker="✦ CRAWLER REVIEW" lead="爬蟲" mark="審核" description="核對爬蟲與正式資料的差異。接受會寫入正式資料，拒絕則保留原值。"
      :icon-left="clipboardIcon" :icon-right="sourceBranchIcon" :icon-top="tableIcon" theme="purple" />

    <ResultBanner v-if="!(loading && !reviews.length) && !error" :icon="clipboardIcon" title="爬蟲審核紀錄"
      :count="totalElements" :unit-text="resultUnit" empty-text="目前沒有符合條件的審核。" theme="purple" />

    <SearchFilter v-if="!(loading && !reviews.length) && !error" v-model="appliedFilters" title="篩選審核"
      :title-icon="filterIcon" submit-label="查詢審核" :fields="filterFields" theme="purple">
      <div ref="listTop" class="review-sheet">
        <DataTable caption="爬蟲審核資料表" :columns="columns" :rows="reviews" row-key="crawlerReviewId"
          empty-text="目前沒有符合條件的審核。" :loading="loading" theme="purple">
          <template #cell-entityId="{ row }">
            <RouterLink v-if="reviewEntityRoute(row)" class="entity-link" :to="reviewEntityRoute(row)">
              {{ row.entityId }}
            </RouterLink>
            <template v-else>{{ row.entityId ?? "—" }}</template>
          </template>
          <template #cell-actions="{ row }">
            <div class="review-actions">
              <AppButton type="button" color="purple" variant="outline" :icon="eyeIcon"
                @click="openDetail(row.crawlerReviewId)">
                詳細
              </AppButton>
            </div>
          </template>
        </DataTable>
      </div>

      <Pagination v-if="totalPages > 1" :page="page" :total-pages="totalPages" label="爬蟲審核分頁" color="purple"
        @update:page="goToPage" />
    </SearchFilter>

    <StatePanel v-if="loading && !reviews.length" variant="loading" message="正在載入爬蟲審核…" :icon="clipboardIcon"
      color="purple" />
    <StatePanel v-else-if="error" variant="error" title="目前無法取得爬蟲審核" message="請確認連線後再試一次。" color="purple"
      @retry="retry" />
  </section>
</template>

<style scoped>
.review-sheet {
  scroll-margin-top: 5.5rem;
}

.review-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0.4rem;
}

.review-actions :deep(.app-button) {
  min-height: 2.25rem;
  padding: 0.3rem 0.8rem;
  box-shadow: none;
}

.entity-link {
  color: #8d72ed;
  font-weight: 800;
  text-underline-offset: 0.18em;
}

.entity-link:hover {
  color: #5c4696;
}

.entity-link:focus-visible {
  outline: 2px solid #8d72ed;
  outline-offset: 3px;
  border-radius: 4px;
}
</style>
