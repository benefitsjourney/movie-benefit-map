<script setup>
import { computed, ref } from "vue";
import { Icon } from "@iconify/vue";
import pinIcon from "@iconify-icons/mdi/map-marker-outline";
import searchIcon from "@iconify-icons/mdi/magnify";
import filmIcon from "@iconify-icons/mdi/filmstrip";
import compassIcon from "@iconify-icons/mdi/compass-outline";
import ticketIcon from "@iconify-icons/mdi/ticket-outline";
import popcornIcon from "@iconify-icons/mdi/popcorn";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import externalIcon from "@iconify-icons/mdi/open-in-new";

import http from "@/plugins/axios";
import { COUNTY_LABELS, countyLabel, countyTicketColor } from "@/constants/county";
import { CINEMA_SORT_DEFAULT } from "@/constants/sort";
import { useApiCollection } from "@/composables/useApiCollection";
import BenefitDialog from "@/components/ui/BenefitDialog.vue";
import PageIntro from "@/components/layout/PageIntro.vue";
import Pagination from "@/components/ui/Pagination.vue";
import ResultBanner from "@/components/layout/ResultBanner.vue";
import SearchFilter from "@/components/layout/SearchFilter.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import TicketCard from "@/components/ui/TicketCard.vue";

const PAGE_SIZE = 21;
const filters = ref({ keyword: "", county: "", hasBenefit: false });
const page = ref(1);
const listTop = ref(null);

const appliedFilters = computed({
  get: () => filters.value,
  set: (next) => {
    filters.value = {
      keyword: next?.keyword ?? "",
      county: next?.county ?? "",
      hasBenefit: next?.hasBenefit === true,
    };
    page.value = 1;
  },
});

const filterFields = [
  {
    key: "keyword",
    label: "關鍵字",
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
    key: "hasBenefit",
    label: "只看有特典的影城",
    type: "checkbox",
  },
];

const requestParams = computed(() => {
  const params = {
    page: page.value,
    size: PAGE_SIZE,
    orderBy: CINEMA_SORT_DEFAULT.orderBy,
    sort: CINEMA_SORT_DEFAULT.sort,
  };
  const keyword = filters.value.keyword?.trim();
  if (keyword) params.keyword = keyword;
  if (filters.value.county) params.county = filters.value.county;
  if (filters.value.hasBenefit === true) params.hasBenefit = true;
  return params;
});

const {
  items: cinemas,
  totalElements,
  totalPages,
  loading,
  error,
  retry,
} = useApiCollection("/cinemas/explore", requestParams, { page });

const scopeText = computed(() =>
  filters.value.county ? `探索${countyLabel(filters.value.county)}` : "探索全台",
);

function goToPage(next) {
  if (!Number.isInteger(next) || next < 1 || next > totalPages.value || next === page.value) return;
  page.value = next;
  listTop.value?.scrollIntoView({
    behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth",
    block: "start",
  });
}

const detailCinemaId = ref(null);
const detailBenefits = ref([]);
const detailLoading = ref(false);
const detailFailed = ref(false);
let detailRequest = 0;

function benefitCountOf(cinema) {
  const count = Number(cinema?.benefitCount);
  return Number.isInteger(count) && count >= 0 ? count : 0;
}

function dialogDetail(cinema) {
  const place = countyLabel(cinema?.county) || "影城";
  const count = benefitCountOf(cinema);
  return `${place} · ${count} 項特典`;
}

function formatBenefitCount(cinema) {
  const count = benefitCountOf(cinema);
  return count ? `${count} 項特典` : "目前沒有特典";
}

function canViewBenefits(cinema) {
  return benefitCountOf(cinema) > 0;
}

const detailCinema = computed(() => {
  const cinema = cinemas.value.find((item) => item.cinemaId === detailCinemaId.value);
  if (!cinema) return null;
  return {
    ...cinema,
    benefits: detailBenefits.value,
    benefitCount: benefitCountOf(cinema),
  };
});

function closeBenefitDetail() {
  detailRequest += 1;
  detailCinemaId.value = null;
  detailBenefits.value = [];
  detailLoading.value = false;
  detailFailed.value = false;
}

async function openBenefitDetail(cinema) {
  const cinemaId = cinema?.cinemaId;
  if (cinemaId == null || !canViewBenefits(cinema)) return;
  const requestId = ++detailRequest;
  detailCinemaId.value = cinemaId;
  detailBenefits.value = [];
  detailLoading.value = true;
  detailFailed.value = false;
  try {
    const { data } = await http.get(`/cinemas/${cinemaId}/benefits/status`);
    if (requestId !== detailRequest) return;
    if (!Array.isArray(data)) throw new Error("影城特典 API 回傳格式有誤");
    detailBenefits.value = data;
  } catch (error) {
    if (requestId !== detailRequest) return;
    console.error("影城特典載入失敗", error);
    detailFailed.value = true;
  } finally {
    if (requestId === detailRequest) detailLoading.value = false;
  }
}
</script>

<template>
  <section class="cinema-page">
    <PageIntro kicker="✦ CINEMA JOURNEY" lead="下一站，" mark="電影院" tail="！" description="探索城市裡的每間影城，開啟你的特典旅程。"
      :icon-left="ticketIcon" :icon-right="popcornIcon" :icon-top="pinIcon" />

    <ResultBanner v-if="!(loading && !cinemas.length) && !error" :icon="ticketIcon" title="探索中的影城"
      :count="totalElements" :unit-text="filters.keyword?.trim() ? '間符合條件的影城' : '間影城等你探索'"
      empty-text="這條路線還沒有找到影城，換個條件試試吧！" action-to="/map" :action-icon="pinIcon" :action-label="scopeText" />

    <SearchFilter v-if="!(loading && !cinemas.length) && !error" v-model="appliedFilters" title="尋找下一站"
      :title-icon="compassIcon" submit-label="探索影城" :fields="filterFields">
      <div v-if="cinemas.length" ref="listTop" class="ticket-grid" :class="{ 'is-loading': loading }"
        :aria-busy="loading">
        <TicketCard v-for="(cinema, index) in cinemas" :key="cinema.cinemaId" :color="countyTicketColor(cinema.county)"
          :appear="index < 6" :stamp="countyLabel(cinema.county)" :title="cinema.cinemaName">
          <template #detail>
            <Icon :icon="pinIcon" aria-hidden="true" />
            <span>{{ cinema.cinemaAddress || "地址未提供" }}</span>
          </template>
          <div class="ticket-benefit-row">
            <span class="ticket-benefits">
              <Icon :icon="giftIcon" aria-hidden="true" />
              <span>{{ formatBenefitCount(cinema) }}</span>
            </span>
          </div>
          <template #action>
            <button type="button" class="ticket-link is-dark" :disabled="!canViewBenefits(cinema)"
              @click="openBenefitDetail(cinema)">
              特典庫存查詢
            </button>
            <a v-if="cinema.officialWebsiteUrl" :href="cinema.officialWebsiteUrl" target="_blank"
              rel="noopener noreferrer" class="ticket-link">
              影城官網
              <Icon :icon="externalIcon" aria-hidden="true" />
            </a>
            <span v-else class="ticket-empty">尚未提供官網</span>
          </template>
        </TicketCard>
      </div>

      <Pagination v-if="totalPages > 1" :page="page" :total-pages="totalPages" label="影城列表分頁" @update:page="goToPage" />
    </SearchFilter>

    <StatePanel v-if="loading && !cinemas.length" variant="loading" message="正在尋找影城…" :icon="filmIcon" />
    <StatePanel v-else-if="error" variant="error" title="目前無法取得影城資料" message="請確認連線後再試一次。" @retry="retry" />

    <BenefitDialog v-if="detailCinema" :title="detailCinema.cinemaName || '影城'" :detail="dialogDetail(detailCinema)"
      :benefits="detailCinema.benefits" headline-key="movieName" headline-fallback="尚未提供電影名稱" :loading="detailLoading"
      :error="detailFailed" close-label="關閉" empty-text="這間影城目前沒有特典" @close="closeBenefitDetail" />
  </section>
</template>

<style scoped>
.ticket-benefit-row {
  display: flex;
  width: 100%;
  align-items: center;
  margin-top: 0.75rem;
}

.ticket-benefits {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  gap: 0.35rem;
  color: var(--ticket-ink);
  font-size: var(--text-control);
  line-height: 1.45;
  font-weight: 800;
}

.ticket-benefits :deep(svg) {
  display: block;
  flex: none;
  width: 1.15em;
  height: 1.15em;
  margin-top: calc((1lh - 1.15em) / 2);
  font-size: inherit;
  line-height: inherit;
  color: var(--ticket-ink);
}

.cinema-page :deep(.ticket-stub) {
  flex-wrap: wrap;
}

@media (min-width: 768px) {
  .ticket-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1100px) {
  .ticket-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
