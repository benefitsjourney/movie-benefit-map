<script setup>
import { computed, ref, watch } from "vue";
import { Icon } from "@iconify/vue";
import pinIcon from "@iconify-icons/mdi/map-marker-outline";
import searchIcon from "@iconify-icons/mdi/magnify";
import filmIcon from "@iconify-icons/mdi/filmstrip";
import compassIcon from "@iconify-icons/mdi/compass-outline";
import ticketIcon from "@iconify-icons/mdi/ticket-outline";
import popcornIcon from "@iconify-icons/mdi/popcorn";

import http from "@/plugins/axios";
import {
  COUNTY_LABELS,
  countyLabel,
  countyTicketColor,
} from "@/constants/county";
import AppButton from "@/components/AppButton.vue";
import PageIntro from "@/components/PageIntro.vue";
import SearchFilter from "@/components/SearchFilter.vue";
import TicketCard from "@/components/TicketCard.vue";

// 資料與狀態
const cinemas = ref([]);
const loading = ref(true);
const error = ref(false);
const retry = ref(0);

const appliedFilters = ref({ keyword: "", county: "" });

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

const scopeText = computed(() =>
  appliedFilters.value.county
    ? `探索${countyLabel(appliedFilters.value.county)}`
    : "探索全台",
);

// 條件變更或重試時重新取得資料
watch(
  [appliedFilters, retry],
  async (_current, _previous, onCleanup) => {
    const controller = new AbortController();
    const signal = controller.signal;
    onCleanup(() => controller.abort());

    loading.value = true;
    error.value = false;

    const params = {
      page: 1,
      size: 200,
      ...appliedFilters.value,
    };

    try {
      const { data: firstPage } = await http.get("/cinemas", {
        params,
        signal,
      });

      // 平行取得剩餘分頁
      const remainingPages = Array.from(
        { length: Math.max(0, firstPage.totalPages - 1) },
        (_, index) => index + 2,
      );

      const responses = await Promise.all(
        remainingPages.map((page) =>
          http.get("/cinemas", {
            params: { ...params, page },
            signal,
          }),
        ),
      );

      if (signal.aborted) return;

      cinemas.value = [
        ...firstPage.content,
        ...responses.flatMap(({ data }) => data.content),
      ];
    } catch {
      if (!signal.aborted) {
        error.value = true;
        cinemas.value = [];
      }
    } finally {
      if (!signal.aborted) loading.value = false;
    }
  },
  { immediate: true },
);
</script>

<template>
  <section class="cinema-page">
    <!-- 頁面介紹 -->
    <PageIntro kicker="✦ CINEMA JOURNEY" lead="下一站，" mark="電影院" tail="！" description="探索城市裡的每間影城，開啟你的特典旅程。"
      :icon-left="ticketIcon" :icon-right="popcornIcon" :icon-top="pinIcon" />

    <SearchFilter v-model="appliedFilters" title="尋找下一站" :title-icon="compassIcon" submit-label="探索影城"
      :fields="filterFields" />

    <!-- 載入中 -->
    <div v-if="loading" class="state-panel" role="status">
      <Icon :icon="filmIcon" class="loading-icon" aria-hidden="true" />
      <p>正在尋找影城…</p>
    </div>

    <!-- 載入失敗 -->
    <div v-else-if="error" class="state-panel" role="alert">
      <h2>目前無法取得影城資料</h2>
      <p>請確認連線後再試一次。</p>
      <AppButton type="button" @click="retry++">重新載入</AppButton>
    </div>

    <!-- 搜尋結果 -->
    <template v-else>
      <div class="result-banner" aria-live="polite">
        <span class="result-mark" aria-hidden="true">
          <Icon :icon="ticketIcon" />
        </span>

        <div class="result-copy">
          <p class="result-title">探索中的影城</p>

          <p class="result-meta">
            <template v-if="!cinemas.length">
              這條路線還沒有找到影城，換個條件試試吧！
            </template>
            <template v-else>
              <span class="result-count">{{ cinemas.length }}</span>
              {{
                appliedFilters.keyword ? "間符合條件的影城" : "間影城等你探索"
              }}
            </template>
          </p>
        </div>

        <RouterLink class="result-scope" to="/map">
          <Icon :icon="pinIcon" aria-hidden="true" />
          {{ scopeText }}
        </RouterLink>
      </div>

      <!-- 影城票券 -->
      <div v-if="cinemas.length" class="cinema-list">
        <TicketCard v-for="(cinema, index) in cinemas" :key="cinema.cinemaId" :color="countyTicketColor(cinema.county)"
          :appear="index < 6" :stamp="countyLabel(cinema.county)" :title="cinema.cinemaName"
          :href="cinema.officialWebsiteUrl || ''" link-text="影城官網" empty-text="尚未提供官網">
          <template #detail>
            <Icon :icon="pinIcon" aria-hidden="true" />
            <span>{{ cinema.cinemaAddress || "地址未提供" }}</span>
          </template>
        </TicketCard>
      </div>
    </template>
  </section>
</template>

<style scoped>
/* 載入與錯誤提示 */
.state-panel {
  display: grid;
  justify-items: center;
  gap: 0.5rem;
  padding: 2rem 1rem;
  border: 1.5px solid var(--c-border);
  border-radius: 22px;
  background: var(--c-card);
  box-shadow: var(--shadow-soft);
  text-align: center;
}

.state-panel h2,
.state-panel p {
  margin: 0;
}

.state-panel h2 {
  font-size: var(--text-section-title, 1.875rem);
  line-height: 1.3;
  font-weight: 900;
}

.loading-icon {
  color: var(--c-brand);
  font-size: 2.4rem;
  animation: float 1s ease-in-out infinite;
}

/* 搜尋結果 */
.result-banner {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  margin: 0.2rem 0 1rem;
  padding: 0.7rem 0.85rem;
  border: 1.5px solid var(--c-border);
  border-radius: 22px;
  background: var(--c-card);
  box-shadow: var(--shadow-soft);
  animation: result-fade 0.4s ease;
}

.result-mark {
  display: grid;
  flex: none;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: #f3b23c;
  color: #fff;
}

.result-mark svg {
  font-size: 1.35rem;
}

.result-copy {
  flex: 1;
  min-width: 0;
}

.result-title,
.result-meta {
  margin: 0;
  line-height: 1.35;
}

.result-title {
  color: var(--c-navy);
  font-size: var(--text-feature-title, 1.25rem);
  font-weight: 800;
}

.result-meta {
  color: #8d877e;
  font-size: var(--text-body, 1.0625rem);
  font-weight: 600;
}

.result-count {
  margin-right: 0.2rem;
  color: var(--c-brand);
  font-size: var(--text-feature-title, 1.25rem);
  font-weight: 800;
}

.result-scope {
  display: inline-flex;
  flex: none;
  align-items: center;
  gap: 0.3rem;
  padding: 0.45rem 0.95rem;
  border-radius: 999px;
  background: #f3b23c;
  color: #fff;
  font-size: var(--text-control, 1rem);
  font-weight: 800;
  white-space: nowrap;
  text-decoration: none;
  cursor: pointer;
  box-shadow: 0 6px 14px rgb(243 178 60 / 40%);
  transition:
    background var(--ease),
    box-shadow var(--ease),
    transform var(--ease);
}

.result-scope:hover {
  background: #e09a1a;
  box-shadow: 0 10px 18px rgb(224 154 26 / 42%);
  transform: translateY(-2px);
}

.result-scope:active {
  transform: scale(0.98);
}

.result-scope svg {
  font-size: 1.05rem;
}

.result-scope:focus-visible {
  outline: 2px solid #e09a1a;
  outline-offset: 3px;
}

@keyframes result-fade {
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
}

@keyframes float {
  50% {
    translate: 0 -7px;
  }
}

/* 影城卡片 */
.cinema-list {
  display: grid;
  grid-template-columns: 1fr;
  align-items: stretch;
  gap: 1.1rem;
}

/* 手機 */
@media (max-width: 519px) {
  .result-banner {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .result-scope {
    margin-left: auto;
  }
}

/* 平板 */
@media (min-width: 768px) {
  .cinema-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

/* 桌面 */
@media (min-width: 1100px) {
  .cinema-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
