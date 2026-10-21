<script setup>
import { computed, ref } from "vue";
import { Icon } from "@iconify/vue";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import ticketIcon from "@iconify-icons/mdi/ticket-outline";
import starIcon from "@iconify-icons/mdi/star-four-points-outline";
import calendarIcon from "@iconify-icons/mdi/calendar-blank-outline";
import accountIcon from "@iconify-icons/mdi/account-star-outline";
import arrowIcon from "@iconify-icons/mdi/arrow-right";
import cinemaIcon from "@iconify-icons/mdi/movie-open-outline";
import filmIcon from "@iconify-icons/mdi/filmstrip";
import searchIcon from "@iconify-icons/mdi/magnify";
import shapeIcon from "@iconify-icons/mdi/shape-outline";
import compassIcon from "@iconify-icons/mdi/compass-outline";

import { useApiCollection } from "@/composables/useApiCollection";
import PageIntro from "@/components/layout/PageIntro.vue";
import ResultBanner from "@/components/layout/ResultBanner.vue";
import SearchFilter from "@/components/layout/SearchFilter.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import Pagination from "@/components/ui/Pagination.vue";
import TicketCard from "@/components/ui/TicketCard.vue";

const PAGE_SIZE = 20;

// 後端 BenefitType對應的中文名稱與卡片顏色
const BENEFIT_TYPES = {
  REGULAR: { label: "一般場次", color: "yellow" },
  SPECIAL_BY_DATE: { label: "入場贈送", color: "pink" },
  SPECIAL_UNTIL_OUT: { label: "限量兌換", color: "purple" },
};

const filters = ref({ keyword: "", benefitType: "", status: "APPROVED" });
const page = ref(1);
const listTop = ref(null);
// 圖片網址失效時改顯示預設圖
const brokenImages = ref(new Set());

// 搜尋條件改變時回到第 1 頁（寫法同 MovieView）
const appliedFilters = computed({
  get: () => filters.value,
  set: (next) => {
    filters.value = {
      keyword: next?.keyword ?? "",
      benefitType: next?.benefitType ?? "",
      status: next?.status || "APPROVED",
    };
    page.value = 1;
  },
});

const filterFields = [
  {
    key: "keyword",
    label: "關鍵字",
    type: "search",
    placeholder: "搜尋特典或電影名稱",
    icon: searchIcon,
  },
  {
    key: "benefitType",
    label: "特典類型",
    type: "select",
    emptyLabel: "全部類型",
    icon: shapeIcon,
    options: Object.entries(BENEFIT_TYPES).map(([value, { label }]) => ({ value, label })),
  },
    {
    key: "status",
    label: "狀態",
    type: "select",
    emptyLabel: null,
    icon: calendarIcon,
    options: [
      { value: "APPROVED", label: "上架中" },
      { value: "EXPIRED", label: "已結束" },
    ],
  },
];

// GET /benefits：回傳特典；沒填的條件不送
const requestParams = computed(() => {
  const params = { page: page.value, size: PAGE_SIZE };
  const keyword = filters.value.keyword?.trim();
  if (keyword) params.keyword = keyword;
  if (filters.value.benefitType) params.benefitType = filters.value.benefitType;
  params.status = filters.value.status;
  return params;
});

const hasFilters = computed(
  () => Boolean(filters.value.keyword?.trim()) || Boolean(filters.value.benefitType),
);

const {
  items: benefits,
  totalElements,
  totalPages,
  loading,
  error,
  retry,
} = useApiCollection("/benefits", requestParams, { page });

function typeLabel(type) {
  return BENEFIT_TYPES[type]?.label ?? "特典";
}

function typeColor(type) {
  return BENEFIT_TYPES[type]?.color ?? "yellow";
}

function imageUrl(benefit) {
  const url = typeof benefit.officialImageUrl === "string" ? benefit.officialImageUrl.trim() : "";
  return url && !brokenImages.value.has(url) ? url : "";
}

function markImageBroken(benefit) {
  const url = typeof benefit.officialImageUrl === "string" ? benefit.officialImageUrl.trim() : "";
  if (url) brokenImages.value.add(url);
}

// "2026-10-07 00:00:00" → "10/07"
function shortDate(value) {
  const matched = typeof value === "string" && value.match(/^\d{4}-(\d{2})-(\d{2})/);
  return matched ? `${matched[1]}/${matched[2]}` : "";
}

function periodText(benefit) {
  const start = shortDate(benefit.startDate);
  const end = shortDate(benefit.endDate);
  if (start && end) return `${start} ～ ${end}`;
  if (start) return `${start} 起，送完為止`;
  return "日期未提供";
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
    <PageIntro kicker="✦ BENEFIT LIST" lead="本週" mark="特典" tail="！" description="看看現在有哪些特典正在發放，別錯過想收藏的那一款。"
      :icon-left="giftIcon" :icon-right="ticketIcon" :icon-top="starIcon" />

    <ResultBanner v-if="!(loading && !benefits.length) && !error" :icon="giftIcon" title="發放中的特典"
      :count="totalElements" :unit-text="hasFilters ? '款符合條件的特典' : '款特典等你收藏'"
      :empty-text="hasFilters ? '沒有找到符合條件的特典，換個關鍵字試試吧！' : '目前沒有發放中的特典，晚點再來看看吧！'" action-to="/map"
      :action-icon="cinemaIcon" action-label="找附近影城" />

    <SearchFilter v-if="!(loading && !benefits.length) && !error" v-model="appliedFilters" title="尋找特典"
      :title-icon="compassIcon" submit-label="搜尋特典" :fields="filterFields">
    <div v-if="benefits.length" ref="listTop" class="ticket-grid" :class="{ 'is-loading': loading }"
      :aria-busy="loading">
      <TicketCard v-for="(benefit, index) in benefits" :key="benefit.benefitId" :color="typeColor(benefit.benefitType)"
        :appear="index < 6" :stamp="typeLabel(benefit.benefitType)" :title="benefit.benefitName">
        <template #poster>
          <img v-if="imageUrl(benefit)" :src="imageUrl(benefit)" :alt="`${benefit.benefitName} 圖片`" loading="lazy"
            decoding="async" referrerpolicy="no-referrer" @error="markImageBroken(benefit)" />
          <span v-else class="poster-fallback">
            <Icon :icon="giftIcon" aria-hidden="true" />
          </span>
        </template>

        <template #detail>
          <Icon :icon="calendarIcon" aria-hidden="true" />
          <span>{{ periodText(benefit) }}</span>
        </template>

        <p class="movie-name">
          <Icon :icon="filmIcon" aria-hidden="true" />
          <span>{{ benefit.movieName || "電影未提供" }}</span>
        </p>

        <p v-if="benefit.isMemberOnly" class="member-only">
          <Icon :icon="accountIcon" aria-hidden="true" />
          會員限定
        </p>

        <template #action>
          <RouterLink class="ticket-link" :to="`/benefits/${benefit.benefitId}`">
            看哪裡有
            <Icon :icon="arrowIcon" aria-hidden="true" />
          </RouterLink>
        </template>
      </TicketCard>
    </div>

    <Pagination v-if="totalPages > 1" :page="page" :total-pages="totalPages" label="特典列表分頁" @update:page="goToPage" />
    </SearchFilter>

    <StatePanel v-if="loading && !benefits.length" variant="loading" message="正在尋找特典…" :icon="giftIcon" />
    <StatePanel v-else-if="error" variant="error" title="目前無法取得特典資料" message="請確認連線後再試一次。" @retry="retry" />
  </section>
</template>

<style scoped>
.ticket-grid {
  gap: 1.125rem;
  scroll-margin-top: 5.5rem;
}

@media (min-width: 640px) {
  .ticket-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 960px) {
  .ticket-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (min-width: 1280px) {
  .ticket-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

.poster-fallback :deep(svg) {
  font-size: var(--text-page-title);
}

.movie-name {
  display: flex;
  align-items: flex-start;
  gap: 0.4rem;
  margin: 0.3rem 0 0;
  color: #3e4450;
  font-size: var(--text-control);
  font-weight: 700;
  overflow-wrap: anywhere;
}

.movie-name :deep(svg) {
  flex: none;
  margin-top: 0.2em;
}

.member-only {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  align-self: flex-start;
  margin: 0.5rem 0 0;
  padding: 0.1rem 0.6rem;
  border-radius: 999px;
  background: #fff0f6;
  color: #a33b6b;
  font-size: var(--text-min);
  font-weight: 800;
}
</style>
