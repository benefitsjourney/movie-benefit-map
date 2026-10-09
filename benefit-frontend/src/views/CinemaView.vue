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
import { COUNTY_LABELS, countyLabel, countyTicketColor } from "@/constants/county";
import TicketCard from "@/components/TicketCard.vue";

const PAGE_SIZE = 200;

const countyOptions = Object.entries(COUNTY_LABELS);

const cinemas = ref([]);
const loading = ref(true);
const error = ref(false);
const keyword = ref("");
const county = ref("");
const retry = ref(0);

const hasFilter = computed(() => Boolean(keyword.value.trim() || county.value));

function resetQuery() {
  keyword.value = "";
  county.value = "";
}

async function loadCinemas(signal) {
  const params = { page: 1, size: PAGE_SIZE };

  if (keyword.value.trim()) params.keyword = keyword.value.trim();
  if (county.value) params.county = county.value;

  try {
    const { data } = await http.get("/cinemas", { params, signal });
    const items = [...data.content];

    // 後端有分頁，依序取完才能顯示全部影城。
    for (let page = 2; page <= data.totalPages; page++) {
      const response = await http.get("/cinemas", {
        params: { ...params, page },
        signal,
      });
      items.push(...response.data.content);
    }

    if (signal.aborted) return;
    cinemas.value = items;
  } catch {
    if (signal.aborted) return;
    error.value = true;
    cinemas.value = [];
  } finally {
    if (!signal.aborted) loading.value = false;
  }
}

// 條件改變時防抖；新請求或離開頁面時自動取消舊請求。
watch(
  [keyword, county, retry],
  (_, __, onCleanup) => {
    const controller = new AbortController();
    loading.value = true;
    error.value = false;

    const timer = setTimeout(() => loadCinemas(controller.signal), 300);
    onCleanup(() => {
      clearTimeout(timer);
      controller.abort();
    });
  },
  { immediate: true },
);
</script>

<template>
  <section class="cinema-page">
    <header class="hero ui-panel">
      <div class="hero-copy">
        <p class="hero-kicker latin">✦ CINEMA JOURNEY</p>
        <h1>下一站，<span class="title-mark">電影院</span>！</h1>
        <p>探索城市裡的每間影城，開啟你的電影旅程。</p>
      </div>

      <div class="hero-art" aria-hidden="true">
        <span class="hero-path"></span>
        <Icon :icon="ticketIcon" class="art-ticket ui-float" />
        <Icon :icon="popcornIcon" class="art-popcorn ui-float" />
        <Icon :icon="pinIcon" class="art-pin ui-float" />
        <span class="art-star ui-twinkle">✦</span>
      </div>
    </header>

    <div class="finder ui-panel">
      <h2 class="finder-title">
        <Icon :icon="compassIcon" aria-hidden="true" />
        尋找下一站
      </h2>

      <div class="finder-grid">
        <div class="field">
          <label for="cinema-keyword">關鍵字</label>
          <div class="field-control">
            <Icon :icon="searchIcon" aria-hidden="true" />
            <input
              id="cinema-keyword"
              v-model="keyword"
              type="search"
              class="form-control ui-control"
              placeholder="搜尋影城名稱"
            />
          </div>
        </div>

        <div class="field">
          <label for="cinema-county">縣市</label>
          <div class="field-control">
            <Icon :icon="pinIcon" aria-hidden="true" />
            <select
              id="cinema-county"
              v-model="county"
              class="form-select ui-control"
            >
              <option value="">全部縣市</option>
              <option
                v-for="[value, label] in countyOptions"
                :key="value"
                :value="value"
              >
                {{ label }}
              </option>
            </select>
          </div>
        </div>

        <button
          class="ui-button clear-btn"
          type="button"
          :disabled="!hasFilter"
          @click="resetQuery"
        >
          清除
        </button>
      </div>
    </div>

    <div v-if="loading" class="state-panel ui-panel" role="status">
      <Icon :icon="filmIcon" class="loading-icon" aria-hidden="true" />
      <p>正在尋找影城…</p>
    </div>

    <div v-else-if="error" class="state-panel ui-panel" role="alert">
      <h2>目前無法取得影城資料</h2>
      <p>請確認連線後再試一次。</p>
      <button type="button" class="ui-button" @click="retry++">重新載入</button>
    </div>

    <div v-else-if="!cinemas.length" class="state-panel ui-panel">
      <h2>{{ hasFilter ? "這站沒有符合的影城" : "旅程還沒有影城" }}</h2>
      <p>
        {{
          hasFilter ? "換個關鍵字，或清除條件再出發。" : "目前沒有影城資料。"
        }}
      </p>
      <button
        v-if="hasFilter"
        type="button"
        class="ui-button"
        @click="resetQuery"
      >
        清除條件
      </button>
    </div>

    <template v-else>
      <p class="result-line" aria-live="polite">
        <span class="count-stamp">{{ cinemas.length }}</span> 家影城
      </p>

      <div class="cinema-list">
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
.hero {
  position: relative;
  display: grid;
  align-items: center;
  min-height: 180px;
  margin-bottom: 1rem;
  padding: 1.5rem;
  overflow: hidden;
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

.finder {
  margin-bottom: 1rem;
  padding: 1rem 1.25rem 1.25rem;
}

.finder-title {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin: 0 0 0.8rem;
  font-size: 1.1rem;
  font-weight: 900;
}

.finder-title svg {
  color: var(--c-brand);
  font-size: 1.3rem;
}
.finder-grid {
  display: grid;
  gap: 0.8rem;
}
.field label {
  display: block;
  margin-bottom: 0.3rem;
  font-weight: 700;
  color: var(--c-muted);
}
.field-control {
  position: relative;
}
.field-control > svg {
  position: absolute;
  top: 50%;
  left: 0.8rem;
  z-index: 1;
  font-size: 1.2rem;
  color: var(--c-brand);
  pointer-events: none;
  transform: translateY(-50%);
}
.field-control .ui-control {
  padding-left: 2.5rem;
}
.clear-btn {
  align-self: end;
}

.state-panel {
  display: grid;
  justify-items: center;
  gap: 0.5rem;
  padding: 2rem 1rem;
  text-align: center;
}

.state-panel h2 {
  margin: 0;
  font-size: 1.2rem;
  font-weight: 900;
}
.state-panel p {
  margin: 0;
}
.loading-icon {
  color: var(--c-brand);
  font-size: 2.4rem;
  animation: float 1s ease-in-out infinite;
}

.result-line {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0 0 0.9rem;
  color: var(--c-muted);
  font-weight: 700;
}
.count-stamp {
  padding: 0.1rem 0.7rem;
  border: 2px dashed var(--c-brand);
  border-radius: 999px;
  background: #fff;
  color: #ad3e19;
}

.cinema-list {
  display: grid;
  grid-template-columns: 1fr;
  align-items: stretch;
  gap: 1.1rem;
}

@media (min-width: 520px) {
  .finder-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .clear-btn {
    grid-column: 1 / -1;
    justify-self: start;
  }
}

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
  }
  .art-popcorn {
    right: 8%;
    bottom: 0;
    color: var(--c-sun);
    font-size: 4.7rem;
    animation-delay: 0.6s;
  }
  .art-pin {
    top: 0;
    right: 20%;
    color: var(--c-sky);
    font-size: 3.5rem;
    animation-delay: 0.3s;
  }
  .art-star {
    position: absolute;
    top: 0;
    left: 35%;
    color: var(--c-brand);
    font-size: 1.8rem;
  }
  .cinema-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 900px) {
  .finder-grid {
    grid-template-columns: 1.4fr 0.85fr auto;
    align-items: end;
  }
  .clear-btn {
    grid-column: auto;
  }
}

@media (min-width: 1100px) {
  .cinema-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
