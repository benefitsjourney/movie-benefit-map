<script setup>
import { computed, ref } from "vue";
import { Icon } from "@iconify/vue";
import pinIcon from "@iconify-icons/mdi/map-marker-outline";
import searchIcon from "@iconify-icons/mdi/magnify";
import filmIcon from "@iconify-icons/mdi/filmstrip";
import compassIcon from "@iconify-icons/mdi/compass-outline";
import ticketIcon from "@iconify-icons/mdi/ticket-outline";
import popcornIcon from "@iconify-icons/mdi/popcorn";

import { COUNTY_LABELS, countyLabel, countyTicketColor } from "@/constants/county";
import { CINEMA_SORT_DEFAULT } from "@/constants/sort";
import { useApiCollection } from "@/composables/useApiCollection";
import PageIntro from "@/components/layout/PageIntro.vue";
import Pagination from "@/components/ui/Pagination.vue";
import ResultBanner from "@/components/layout/ResultBanner.vue";
import SearchFilter from "@/components/layout/SearchFilter.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import TicketCard from "@/components/ui/TicketCard.vue";

const PAGE_SIZE = 20;
const filters = ref({ keyword: "", county: "" });
const page = ref(1);
const listTop = ref(null);

const appliedFilters = computed({
  get: () => filters.value,
  set: (next) => {
    filters.value = {
      keyword: next?.keyword ?? "",
      county: next?.county ?? "",
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
</script>

<template>
  <section class="cinema-page">
    <PageIntro kicker="✦ CINEMA JOURNEY" lead="下一站，" mark="電影院" tail="！" description="探索城市裡的每間影城，開啟你的特典旅程。"
      :icon-left="ticketIcon" :icon-right="popcornIcon" :icon-top="pinIcon" />

    <ResultBanner v-if="!(loading && !cinemas.length) && !error" :icon="ticketIcon" title="探索中的影城" :count="totalElements"
      :unit-text="filters.keyword?.trim() ? '間符合條件的影城' : '間影城等你探索'" empty-text="這條路線還沒有找到影城，換個條件試試吧！" action-to="/map"
      :action-icon="pinIcon" :action-label="scopeText" />

    <SearchFilter v-if="!(loading && !cinemas.length) && !error" v-model="appliedFilters" title="尋找下一站"
      :title-icon="compassIcon" submit-label="探索影城" :fields="filterFields">
      <div v-if="cinemas.length" ref="listTop" class="ticket-grid" :class="{ 'is-loading': loading }" :aria-busy="loading">
        <TicketCard v-for="(cinema, index) in cinemas" :key="cinema.cinemaId" :color="countyTicketColor(cinema.county)"
          :appear="index < 6" :stamp="countyLabel(cinema.county)" :title="cinema.cinemaName"
          :href="cinema.officialWebsiteUrl || ''" link-text="影城官網" empty-text="尚未提供官網">
          <template #detail>
            <Icon :icon="pinIcon" aria-hidden="true" />
            <span>{{ cinema.cinemaAddress || "地址未提供" }}</span>
          </template>
        </TicketCard>
      </div>

      <Pagination v-if="totalPages > 1" :page="page" :total-pages="totalPages" label="影城列表分頁" @update:page="goToPage" />
    </SearchFilter>

    <StatePanel v-if="loading && !cinemas.length" variant="loading" message="正在尋找影城…" :icon="filmIcon" />
    <StatePanel v-else-if="error" variant="error" title="目前無法取得影城資料" message="請確認連線後再試一次。" @retry="retry" />
  </section>
</template>

<style scoped>
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
