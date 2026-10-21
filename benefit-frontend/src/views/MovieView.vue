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
import giftIcon from "@iconify-icons/mdi/gift-outline";

import http from "@/plugins/axios";
import { MOVIE_SORT_DEFAULT, MOVIE_SORT_OPTIONS, parseMovieSort } from "@/constants/sort";
import { useApiCollection } from "@/composables/useApiCollection";
import BenefitDialog from "@/components/ui/BenefitDialog.vue";
import PageIntro from "@/components/layout/PageIntro.vue";
import Pagination from "@/components/ui/Pagination.vue";
import ResultBanner from "@/components/layout/ResultBanner.vue";
import SearchFilter from "@/components/layout/SearchFilter.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import TicketCard from "@/components/ui/TicketCard.vue";

const PAGE_SIZE = 30;
const filters = ref({ keyword: "", sortKey: "", hasBenefit: false });
const page = ref(1);
const listTop = ref(null);
const brokenPosters = ref(new Set());

const appliedFilters = computed({
  get: () => filters.value,
  set: (next) => {
    filters.value = {
      keyword: next?.keyword ?? "",
      sortKey: next?.sortKey ?? "",
      hasBenefit: next?.hasBenefit === true,
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
  {
    key: "hasBenefit",
    label: "只看有特典的電影",
    type: "checkbox",
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
  if (filters.value.hasBenefit === true) params.hasBenefit = true;
  return params;
});

const {
  items: movies,
  totalElements,
  totalPages,
  loading,
  error,
  retry,
} = useApiCollection("/movies/explore", requestParams, { page });

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

const detailMovieId = ref(null);
const detailBenefits = ref([]);
const detailLoading = ref(false);
const detailFailed = ref(false);
let detailRequest = 0;

function benefitCountOf(movie) {
  const count = Number(movie?.benefitCount);
  return Number.isInteger(count) && count >= 0 ? count : 0;
}

function dialogDetail(movie) {
  return `電影 · ${benefitCountOf(movie)} 間影城`;
}

function formatBenefitCount(movie) {
  const count = benefitCountOf(movie);
  return count ? `${count} 間影城` : "目前沒有特典";
}

function canViewBenefits(movie) {
  return benefitCountOf(movie) > 0;
}

const detailMovie = computed(() => {
  const movie = movies.value.find((item) => item.movieId === detailMovieId.value);
  if (!movie) return null;
  return {
    ...movie,
    benefits: detailBenefits.value,
    benefitCount: benefitCountOf(movie),
  };
});

function closeBenefitDetail() {
  detailRequest += 1;
  detailMovieId.value = null;
  detailBenefits.value = [];
  detailLoading.value = false;
  detailFailed.value = false;
}

async function openBenefitDetail(movie) {
  const movieId = movie?.movieId;
  if (movieId == null || !canViewBenefits(movie)) return;
  const requestId = ++detailRequest;
  detailMovieId.value = movieId;
  detailBenefits.value = [];
  detailLoading.value = true;
  detailFailed.value = false;
  try {
    const { data } = await http.get(`/movies/${movieId}/benefits/status`);
    if (requestId !== detailRequest) return;
    if (!Array.isArray(data)) throw new Error("電影特典 API 回傳格式有誤");
    detailBenefits.value = data;
  } catch (error) {
    if (requestId !== detailRequest) return;
    console.error("電影特典載入失敗", error);
    detailFailed.value = true;
  } finally {
    if (requestId === detailRequest) detailLoading.value = false;
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
    <PageIntro kicker="✦ MOVIE JOURNEY" lead="下一場，" mark="電影" tail="！" description="從喜歡的電影出發，開啟特典收藏旅程。"
      :icon-left="clapperIcon" :icon-right="reelIcon" :icon-top="cameraIcon" />

    <ResultBanner v-if="!(loading && !movies.length) && !error" :icon="clapperIcon" title="探索中的電影"
      :count="totalElements" :unit-text="resultUnit" empty-text="這條路線還沒有找到電影，換個條件試試吧！" action-to="/cinemas"
      :action-icon="cinemaIcon" action-label="探索影城" />

    <SearchFilter v-if="!(loading && !movies.length) && !error" v-model="appliedFilters" title="尋找想看的電影"
      :title-icon="compassIcon" submit-label="探索電影" :fields="filterFields">
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
          <div class="ticket-benefit-row">
            <span class="ticket-benefits">
              <Icon :icon="giftIcon" aria-hidden="true" />
              <span>{{ formatBenefitCount(movie) }}</span>
            </span>
          </div>
          <template #action>
            <button type="button" class="ticket-link is-dark" :disabled="!canViewBenefits(movie)"
              @click="openBenefitDetail(movie)">
              庫存查詢
            </button>
          </template>
        </TicketCard>
      </div>

      <Pagination v-if="totalPages > 1" :page="page" :total-pages="totalPages" label="電影列表分頁" @update:page="goToPage" />
    </SearchFilter>

    <StatePanel v-if="loading && !movies.length" variant="loading" message="正在尋找電影…" :icon="filmIcon" />
    <StatePanel v-else-if="error" variant="error" title="目前無法取得電影資料" message="請確認連線後再試一次。" @retry="retry" />

    <BenefitDialog v-if="detailMovie" :title="detailMovie.movieName || '電影'"
      :detail="dialogDetail(detailMovie)" :benefits="detailMovie.benefits" headline-key="cinemaName"
      headline-fallback="尚未提供影城名稱" :loading="detailLoading" :error="detailFailed" close-label="關閉"
      empty-text="這部電影目前沒有特典" @close="closeBenefitDetail" />
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
  font-size: var(--text-page-title);
}
</style>
