<script setup>
import { computed, ref } from "vue";
import { Icon } from "@iconify/vue";
import searchIcon from "@iconify-icons/mdi/magnify";
import sortIcon from "@iconify-icons/mdi/sort";
import filmIcon from "@iconify-icons/mdi/filmstrip";
import clapperIcon from "@iconify-icons/mdi/movie-open";
import reelIcon from "@iconify-icons/mdi/movie-roll";
import cameraIcon from "@iconify-icons/mdi/video-vintage";
import compassIcon from "@iconify-icons/mdi/compass-outline";
import cinemaIcon from "@iconify-icons/mdi/movie-open-outline";
import calendarIcon from "@iconify-icons/mdi/calendar-blank-outline";

import { MOVIE_SORT_DEFAULT, MOVIE_SORT_OPTIONS, parseMovieSort } from "@/constants/sort";
import { useApiCollection } from "@/composables/useApiCollection";
import PageIntro from "@/components/layout/PageIntro.vue";
import Pagination from "@/components/ui/Pagination.vue";
import ResultBanner from "@/components/layout/ResultBanner.vue";
import SearchFilter from "@/components/layout/SearchFilter.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import TicketCard from "@/components/ui/TicketCard.vue";

const PAGE_SIZE = 24;
const filters = ref({ keyword: "", sortKey: "" });
const page = ref(1);
const listTop = ref(null);
const brokenPosters = ref(new Set());

const appliedFilters = computed({
  get: () => filters.value,
  set: (next) => {
    filters.value = {
      keyword: next?.keyword ?? "",
      sortKey: next?.sortKey ?? "",
    };
    page.value = 1;
  },
});

const filterFields = [
  {
    key: "keyword",
    label: "電影名稱",
    type: "search",
    placeholder: "搜尋電影名稱",
    icon: searchIcon,
  },
  {
    key: "sortKey",
    label: "排序",
    type: "select",
    emptyLabel: MOVIE_SORT_DEFAULT.label,
    icon: sortIcon,
    options: MOVIE_SORT_OPTIONS,
  },
];

const requestParams = computed(() => {
  const { orderBy, sort } = parseMovieSort(filters.value.sortKey);
  const params = {
    page: page.value,
    size: PAGE_SIZE,
    isVisible: true,
    orderBy,
    sort,
  };
  const keyword = filters.value.keyword?.trim();
  if (keyword) params.keyword = keyword;
  return params;
});

const {
  items: movies,
  totalElements,
  totalPages,
  loading,
  error,
  retry,
} = useApiCollection("/movies", requestParams, { page });

const resultUnit = computed(() =>
  filters.value.keyword?.trim() ? "部符合條件的電影" : "部電影等你探索",
);

function posterUrl(movie) {
  const url =
    typeof movie.imageUrl === "string" ? movie.imageUrl.trim() : "";
  return url && !brokenPosters.value.has(url) ? url : "";
}

function markPosterBroken(movie) {
  const url =
    typeof movie.imageUrl === "string" ? movie.imageUrl.trim() : "";
  if (url) brokenPosters.value.add(url);
}

function releaseDateText(value) {
  const matched = typeof value === "string" && value.match(/^(\d{4})-(\d{2})-(\d{2})/);
  return matched
    ? `上映日期 ${matched[1]}/${matched[2]}/${matched[3]}`
    : "上映日期未提供";
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
    <PageIntro kicker="✦ MOVIE JOURNEY" lead="下一場，" mark="電影" tail="！" description="從喜歡的電影出發，開啟特典收藏旅程。"
      :icon-left="clapperIcon" :icon-right="reelIcon" :icon-top="cameraIcon" />

    <SearchFilter v-model="appliedFilters" title="尋找想看的電影" :title-icon="compassIcon" submit-label="探索電影"
      :fields="filterFields" />

    <StatePanel v-if="loading && !movies.length" variant="loading" message="正在尋找電影…" :icon="filmIcon" />
    <StatePanel v-else-if="error" variant="error" title="目前無法取得電影資料" message="請確認連線後再試一次。" @retry="retry" />

    <template v-else>
      <ResultBanner :icon="clapperIcon" title="探索中的電影" :count="totalElements" :unit-text="resultUnit"
        empty-text="這條路線還沒有找到電影，換個條件試試吧！" action-to="/cinemas" :action-icon="cinemaIcon" action-label="探索影城" />

      <div v-if="movies.length" ref="listTop" class="ticket-grid" :class="{ 'is-loading': loading }"
        :aria-busy="loading">
        <TicketCard v-for="(movie, index) in movies" :key="movie.movieId" color="yellow" :appear="index < 6"
          :title="movie.movieName">
          <template #poster>
            <img v-if="posterUrl(movie)" :src="posterUrl(movie)" :alt="`${movie.movieName} 海報`" loading="lazy"
              decoding="async" referrerpolicy="no-referrer" @error="markPosterBroken(movie)" />
            <span v-else class="poster-fallback">
              <Icon :icon="filmIcon" aria-hidden="true" />
            </span>
          </template>
          <template #detail>
            <Icon :icon="calendarIcon" aria-hidden="true" />
            <span>{{ releaseDateText(movie.releaseDate) }}</span>
          </template>
        </TicketCard>
      </div>

      <Pagination v-if="totalPages > 1" :page="page" :total-pages="totalPages" label="電影列表分頁" @update:page="goToPage" />
    </template>
  </section>
</template>

<style scoped>
.ticket-grid {
  gap: 1.125rem;
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

@media (min-width: 1100px) {
  .ticket-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (min-width: 1280px) {
  .ticket-grid {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }
}

@media (min-width: 1680px) {
  .ticket-grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }
}

.poster-fallback :deep(svg) {
  font-size: 2.4rem;
}
</style>
