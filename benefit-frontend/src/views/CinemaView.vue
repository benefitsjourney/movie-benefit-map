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
    <header class="hero">
      <div class="hero-copy">
        <p class="hero-kicker">✦ CINEMA JOURNEY</p>
        <h1>下一站，<span class="title-mark">電影院</span>！</h1>
        <p>探索城市裡的每間影城，開啟你的特典旅程。</p>
      </div>

      <div class="hero-art" aria-hidden="true">
        <span class="hero-path"></span>
        <Icon :icon="ticketIcon" class="art-ticket" />
        <Icon :icon="popcornIcon" class="art-popcorn" />
        <Icon :icon="pinIcon" class="art-pin" />
        <span class="art-star">✦</span>
      </div>
    </header>

    <SearchFilter
      v-model="appliedFilters"
      title="尋找下一站"
      :title-icon="compassIcon"
      submit-label="探索影城"
      :fields="filterFields"
    />

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
        <TicketCard
          v-for="(cinema, index) in cinemas"
          :key="cinema.cinemaId"
          :color="countyTicketColor(cinema.county)"
          :appear="index < 6"
          :stamp="countyLabel(cinema.county)"
          :title="cinema.cinemaName"
          :href="cinema.officialWebsiteUrl || ''"
          link-text="影城官網"
          empty-text="尚未提供官網"
        >
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
/* 頁面主視覺 */
.hero {
  position: relative;
  display: grid;
  margin-bottom: 1rem;
  align-items: center;
  min-height: 180px;
  padding: 1.5rem;
  overflow: hidden;
  border: 1.5px solid var(--c-border);
  border-radius: 22px;
  background: var(--c-card);
  box-shadow: var(--shadow-soft);
}

.hero::after {
  position: absolute;
  top: -70px;
  right: -55px;
  width: 190px;
  height: 190px;
  border-radius: 50%;
  background: rgb(255 200 87 / 35%);
  content: "";
}

.hero-copy {
  position: relative;
  z-index: 1;
}

.hero-kicker {
  display: inline-block;
  margin: 0 0 0.5rem;
  padding: 0.2rem 0.8rem;
  border-radius: 999px;
  background: #fff1cc;
  color: #865500;
  font-family: var(--font-latin);
  font-weight: 700;
  letter-spacing: 0.08em;
}

.hero h1 {
  margin-bottom: 0.4rem;
  font-size: clamp(1.8rem, 3vw, 2.3rem);
  font-weight: 900;
}

.hero-copy > p:last-child {
  margin: 0;
  color: var(--c-muted);
}

.title-mark {
  background: linear-gradient(#ffe08a, #ffe08a) left 85% / 100% 0.38em no-repeat;
}

.hero-art {
  display: none;
}

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
  font-size: 1.2rem;
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
  font-size: 1.05rem;
  font-weight: 800;
}

.result-meta {
  color: #8d877e;
  font-weight: 600;
}

.result-count {
  margin-right: 0.2rem;
  color: var(--c-brand);
  font-size: 1.2rem;
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

@keyframes twinkle {
  0%,
  100% {
    opacity: 0.35;
  }
  50% {
    opacity: 1;
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
  .hero {
    grid-template-columns: 1.2fr 0.8fr;
    min-height: 215px;
  }

  .hero-art {
    position: relative;
    display: block;
    height: 160px;
  }

  .hero-path {
    position: absolute;
    top: 45px;
    left: 0;
    width: 100%;
    height: 90px;
    border-top: 3px dashed var(--c-brand);
    border-radius: 50%;
    transform: rotate(-16deg);
  }

  .hero-art > svg {
    position: absolute;
    filter: drop-shadow(0 6px 0 rgb(41 43 53 / 9%));
  }

  .art-ticket {
    top: 28px;
    left: 10%;
    color: var(--c-brand);
    font-size: 5rem;
    transform: rotate(-8deg);
    animation: float 4.5s ease-in-out infinite;
  }

  .art-popcorn {
    right: 8%;
    bottom: 0;
    color: var(--c-sun);
    font-size: 4.7rem;
    animation: float 4.5s ease-in-out 0.6s infinite;
  }

  .art-pin {
    top: 0;
    right: 20%;
    color: var(--c-sky);
    font-size: 3.5rem;
    animation: float 4.5s ease-in-out 0.3s infinite;
  }

  .art-star {
    position: absolute;
    top: 0;
    left: 35%;
    color: var(--c-brand);
    font-size: 1.8rem;
    animation: twinkle 3s ease-in-out infinite;
  }

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
