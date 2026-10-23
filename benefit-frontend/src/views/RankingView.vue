<script setup>
import { computed, onMounted, ref } from "vue";
import { Icon } from "@iconify/vue";

import trophyIcon from "@iconify-icons/mdi/trophy-outline";
import crownIcon from "@iconify-icons/mdi/crown";
import medalIcon from "@iconify-icons/mdi/medal-outline";
import starIcon from "@iconify-icons/mdi/star-four-points-outline";
import calendarIcon from "@iconify-icons/mdi/calendar-month-outline";
import megaphoneIcon from "@iconify-icons/mdi/bullhorn-outline";
import arrowIcon from "@iconify-icons/mdi/arrow-right";
import infoIcon from "@iconify-icons/mdi/information-outline";

import http from "@/plugins/axios";
import { useAuth } from "@/composables/useAuth";
import PageIntro from "@/components/layout/PageIntro.vue";
import StatePanel from "@/components/layout/StatePanel.vue";

// 顯示前幾名
const LIMIT = 10;

const PERIODS = [
  { key: "weekly", label: "本週" },
  { key: "monthly", label: "本月" },
  { key: "all", label: "全部" },
];

// 頭像底色（依會員編號輪流）
const AVATAR_COLORS = ["#ff7049", "#7ea7e8", "#8d72ed", "#3fb68b", "#f29c38", "#e86a9a"];

const auth = useAuth();

const period = ref("weekly");
const loading = ref(true);
const loadFailed = ref(false);
const result = ref(null);
let requestSeq = 0;

// 目前登入會員 ID（沒登入是 null）
const myId = computed(() => {
  if (!auth.isLoggedIn.value) return null;
  const user = auth.user.value;
  return user?.usersId ?? null;
});

const rankings = computed(() => result.value?.rankings ?? []);
const me = computed(() => result.value?.me ?? null);

// 頒獎台：前三位，排成 第2、第1、第3 的位置
// place＝站的位置（決定動畫順序）；台子顏色、高度、皇冠看名次 rank，並列的人會一樣
const podium = computed(() => {
  const top = rankings.value.slice(0, 3).map((entry, index) => ({ ...entry, place: index + 1 }));
  const order = [1, 0, 2];
  return order.map((i) => top[i]).filter(Boolean);
});

// 第 4 名以後
const restList = computed(() => rankings.value.slice(3));

const periodLabel = computed(() => PERIODS.find((p) => p.key === period.value)?.label ?? "");

// ── 日期 ──
function pad(n) {
  return String(n).padStart(2, "0");
}

function md(date) {
  return `${date.getMonth() + 1}/${pad(date.getDate())}`;
}

// 上方的期間文字，例如「10/19 ～ 10/25」
const rangeText = computed(() => {
  if (period.value === "all") return "從開站到現在";
  const matched = result.value?.since?.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (!matched) return "";
  const start = new Date(Number(matched[1]), Number(matched[2]) - 1, Number(matched[3]));
  const end = period.value === "weekly"
    ? new Date(start.getFullYear(), start.getMonth(), start.getDate() + 6)
    : new Date(start.getFullYear(), start.getMonth() + 1, 0);
  return `${md(start)} ～ ${md(end)}`;
});

// ── 顯示小工具 ──
function initialOf(entry) {
  const name = (entry?.displayName ?? "").trim();
  return name ? Array.from(name)[0].toUpperCase() : "?";
}

function avatarColor(entry) {
  const id = Number(entry?.usersId) || 0;
  return AVATAR_COLORS[id % AVATAR_COLORS.length];
}

function isMe(entry) {
  return myId.value != null && entry?.usersId === myId.value;
}

function detailText(entry) {
  const parts = [];
  if (entry.inventoryCount) parts.push(`庫存回報 ${entry.inventoryCount}`);
  if (entry.benefitReportCount) parts.push(`缺漏回報 ${entry.benefitReportCount}`);
  return parts.join(" · ");
}

// ── 載入 ──
async function loadRanking() {
  const seq = ++requestSeq;
  loading.value = true;
  loadFailed.value = false;
  try {
    const params = { period: period.value, limit: LIMIT };
    if (myId.value != null) params.userId = myId.value;
    const { data } = await http.get("/users/report-ranking", { params });
    if (seq !== requestSeq) return; // 已經切到別的期間，這次結果不用了
    result.value = data;
  } catch {
    if (seq !== requestSeq) return;
    result.value = null;
    loadFailed.value = true;
  } finally {
    if (seq === requestSeq) loading.value = false;
  }
}

function switchPeriod(key) {
  if (key === period.value) return;
  period.value = key;
  loadRanking();
}

onMounted(loadRanking);
</script>

<template>
  <section>
    <PageIntro kicker="✦ RANKING" lead="" mark="貢獻" tail="排行榜"
      description="回報庫存、補上缺漏的特典，讓大家少跑一趟。謝謝這些熱心的特典獵人！"
      :icon-left="trophyIcon" :icon-right="medalIcon" :icon-top="starIcon" />

    <!-- 期間切換 -->
    <div class="rank-toolbar">
      <div class="period-switch" role="tablist" aria-label="排行期間">
        <button v-for="p in PERIODS" :key="p.key" type="button" role="tab" class="period-btn"
          :class="{ 'is-on': period === p.key }" :aria-selected="period === p.key" @click="switchPeriod(p.key)">
          {{ p.label }}
        </button>
      </div>
      <p v-if="rangeText" class="period-range">
        <Icon :icon="calendarIcon" aria-hidden="true" />
        {{ rangeText }}
      </p>
    </div>

    <StatePanel v-if="loading && !result" variant="loading" message="正在統計排行…" :icon="trophyIcon" />
    <StatePanel v-else-if="loadFailed" variant="error" title="目前無法取得排行榜" message="請確認連線後再試一次。"
      @retry="loadRanking" />

    <div v-else class="rank-body" :class="{ 'is-loading': loading }">
      <!-- 我的名次（有登入才顯示） -->
      <div v-if="myId != null" class="me-bar" :class="{ 'has-rank': me }">
        <template v-if="me">
          <span class="me-badge">#{{ me.rank }}</span>
          <p class="me-text">{{ periodLabel }}你是第 <strong>{{ me.rank }}</strong> 名，共貢獻
            <strong>{{ me.totalCount }}</strong> 次
          </p>
        </template>
        <template v-else>
          <Icon :icon="megaphoneIcon" class="me-icon" aria-hidden="true" />
          <p class="me-text">{{ periodLabel }}還沒有你的貢獻，回報一次庫存就能上榜！</p>
        </template>
        <RouterLink class="me-link" to="/map">
          去回報庫存
          <Icon :icon="arrowIcon" aria-hidden="true" />
        </RouterLink>
      </div>

      <!-- 沒有人上榜 -->
      <div v-if="!rankings.length" class="empty-card">
        <Icon :icon="trophyIcon" aria-hidden="true" />
        <h2>{{ periodLabel }}還沒有人上榜</h2>
        <p>到地圖上回報特典庫存，<br />你就是第一名！</p>
        <RouterLink class="empty-btn" to="/map">
          去回報庫存
          <Icon :icon="arrowIcon" aria-hidden="true" />
        </RouterLink>
      </div>

      <template v-else>
        <!-- 頒獎台 -->
        <div :key="period" class="stage">
          <span class="stage-star s1" aria-hidden="true">✦</span>
          <span class="stage-star s2" aria-hidden="true">✦</span>
          <span class="stage-star s3" aria-hidden="true">✦</span>

          <ol class="podium" aria-label="前三名">
            <li v-for="entry in podium" :key="entry.usersId" class="spot" :class="[`place-${entry.place}`, `medal-${entry.rank}`, {
              'is-me': isMe(entry)
            }]" :title="detailText(entry)">
              <Icon v-if="entry.rank === 1" :icon="crownIcon" class="crown" aria-hidden="true" />
              <span class="avatar" :style="{ '--avatar-bg': avatarColor(entry) }">{{ initialOf(entry) }}</span>
              <p class="spot-name">
                {{ entry.displayName }}
                <span v-if="isMe(entry)" class="me-tag">你</span>
              </p>
              <p class="spot-count"><strong>{{ entry.totalCount }}</strong> 次</p>
              <div class="pedestal">
                <span class="pedestal-rank">{{ entry.rank }}</span>
              </div>
            </li>
          </ol>
        </div>

        <!-- 第 4 名以後 -->
        <ol v-if="restList.length" class="rank-list">
          <li v-for="entry in restList" :key="entry.usersId" class="rank-row" :class="{ 'is-me': isMe(entry) }">
            <span class="row-rank">{{ entry.rank }}</span>
            <span class="avatar small" :style="{ '--avatar-bg': avatarColor(entry) }">{{ initialOf(entry) }}</span>
            <div class="row-body">
              <p class="row-name">
                {{ entry.displayName }}
                <span v-if="isMe(entry)" class="me-tag">你</span>
              </p>
              <p class="row-detail">{{ detailText(entry) }}</p>
            </div>
            <p class="row-count"><strong>{{ entry.totalCount }}</strong> 次</p>
          </li>
        </ol>

        <p class="rank-note">
          <Icon :icon="infoIcon" aria-hidden="true" />
          每回報一次庫存 +1，回報的缺漏特典審核通過 +1。
        </p>
      </template>
    </div>
  </section>
</template>

<style scoped>
/* ── 期間切換 ── */
.rank-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin: 1.25rem 0 1rem;
}

.period-switch {
  display: inline-flex;
  gap: 0.25rem;
  padding: 0.3rem;
  border-radius: 999px;
  background: #fff;
  box-shadow: 0 6px 18px rgb(41 43 53 / 8%);
}

.period-btn {
  min-width: 4.5rem;
  min-height: 2.4rem;
  padding: 0.3rem 1.1rem;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #6c7380;
  font-size: var(--text-control);
  font-weight: 800;
  cursor: pointer;
  transition: background-color var(--ease), color var(--ease);
}

.period-btn:hover {
  color: #1e2b45;
}

.period-btn.is-on {
  background: #1e2b45;
  color: #ffc857;
}

.period-btn:focus-visible {
  outline: 2px solid #ff7049;
  outline-offset: 2px;
}

.period-range {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  margin: 0;
  color: #6c7380;
  font-size: var(--text-control);
  font-weight: 700;
}

.period-range :deep(svg) {
  color: #ff7049;
  font-size: 1.2rem;
}

/* 切換期間時，舊資料淡一點 */
.rank-body {
  transition: opacity var(--ease);
}

.rank-body.is-loading {
  opacity: 0.55;
  pointer-events: none;
}

/* ── 我的名次 ── */
.me-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.6rem 0.9rem;
  margin-bottom: 1rem;
  padding: 0.8rem 1rem;
  border: 1.5px dashed #ffc857;
  border-radius: 16px;
  background: #fffaf0;
}

.me-bar.has-rank {
  border-style: solid;
}

.me-badge {
  display: grid;
  place-items: center;
  min-width: 2.8rem;
  height: 2.8rem;
  padding: 0 0.5rem;
  border-radius: 999px;
  background: #ff7049;
  color: #fff;
  font-family: var(--font-latin);
  font-weight: 900;
}

.me-icon {
  color: #ff7049;
  font-size: 1.6rem;
}

.me-text {
  flex: 1;
  min-width: 12rem;
  margin: 0;
  color: #3e4450;
  font-weight: 700;
}

.me-text strong {
  color: #ff7049;
  font-size: 1.15rem;
}

.me-link {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.4rem 1rem;
  border-radius: 999px;
  background: #1e2b45;
  color: #fff;
  font-size: var(--text-control);
  font-weight: 800;
  text-decoration: none;
}

.me-link:hover {
  background: #2c3d5f;
  color: #ffc857;
}

/* ── 空狀態 ── */
.empty-card {
  display: grid;
  justify-items: center;
  gap: 0.6rem;
  padding: 3rem 1.5rem;
  border-radius: 20px;
  background: #fff;
  text-align: center;
  box-shadow: 0 10px 30px rgb(41 43 53 / 8%);
}

.empty-card > :deep(svg) {
  color: #ffc857;
  font-size: 3.4rem;
}

.empty-card h2 {
  margin: 0;
  color: #1e2b45;
  font-size: 1.4rem;
  font-weight: 900;
}

.empty-card p {
  margin: 0;
  color: #6c7380;
  line-height: 1.7;
  text-wrap: balance;
}

.empty-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  margin-top: 0.5rem;
  padding: 0.55rem 1.3rem;
  border-radius: 999px;
  background: #ff7049;
  color: #fff;
  font-weight: 800;
  text-decoration: none;
}

.empty-btn:hover {
  background: #e85f3a;
  color: #fff;
}

/* ── 頒獎台 ── */
.stage {
  position: relative;
  overflow: hidden;
  padding: 2.2rem 1rem 0;
  border-radius: 22px;
  background: linear-gradient(160deg, #1e2b45 0%, #2c3d5f 100%);
  box-shadow: 0 12px 32px rgb(30 43 69 / 24%);
}

.stage-star {
  position: absolute;
  color: #ffc857;
  animation: twinkle 2.6s ease-in-out infinite;
}

.stage-star.s1 {
  top: 1rem;
  left: 8%;
  font-size: 1.1rem;
}

.stage-star.s2 {
  top: 2.6rem;
  right: 10%;
  font-size: 0.9rem;
  animation-delay: 0.8s;
}

.stage-star.s3 {
  top: 5.5rem;
  left: 22%;
  font-size: 0.7rem;
  animation-delay: 1.6s;
}

.podium {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 11rem));
  align-items: end;
  justify-content: center;
  gap: 0.75rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.spot {
  --medal: #ffc857;
  --pedestal-h: 7.5rem;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 0;
  animation: rise 0.6s cubic-bezier(0.2, 0.8, 0.3, 1.2) both;
}

/* 位置：只決定升起的順序（2 → 1 → 3） */
.spot.place-1 {
  animation-delay: 0.25s;
}

.spot.place-2 {
  animation-delay: 0.1s;
}

.spot.place-3 {
  animation-delay: 0.4s;
}

/* 名次：決定台子顏色和高度（並列的人一樣） */
.spot.medal-1 {
  --medal: #ffc857;
  --pedestal-h: 7.5rem;
}

.spot.medal-2 {
  --medal: #c9d3e3;
  --pedestal-h: 5.3rem;
}

.spot.medal-3 {
  --medal: #f0a97a;
  --pedestal-h: 3.9rem;
}

.crown {
  margin-bottom: -0.35rem;
  color: #ffc857;
  font-size: 2rem;
  filter: drop-shadow(0 2px 6px rgb(255 200 87 / 45%));
}

.avatar {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 4rem;
  height: 4rem;
  border: 3px solid var(--medal, #fff);
  border-radius: 50%;
  background: var(--avatar-bg, #ff7049);
  color: #fff;
  font-size: 1.5rem;
  font-weight: 900;
  box-shadow: 0 6px 16px rgb(0 0 0 / 22%);
}

.medal-1 .avatar {
  width: 4.8rem;
  height: 4.8rem;
  font-size: 1.8rem;
}

.spot-name {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.3rem;
  max-width: 100%;
  margin: 0.55rem 0 0;
  overflow: hidden;
  color: #fff;
  font-weight: 800;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.spot-count {
  margin: 0.1rem 0 0.6rem;
  color: #cfd6e4;
  font-size: var(--text-min);
}

.spot-count strong {
  color: var(--medal);
  font-family: var(--font-latin);
  font-size: 1.35rem;
}

.pedestal {
  display: grid;
  place-items: start center;
  width: 100%;
  height: var(--pedestal-h);
  padding-top: 0.5rem;
  border-radius: 14px 14px 0 0;
  background: linear-gradient(180deg, var(--medal) 0%, color-mix(in srgb, var(--medal) 55%, #1e2b45) 100%);
}

.pedestal-rank {
  color: #1e2b45;
  font-family: var(--font-latin);
  font-size: 1.9rem;
  font-weight: 900;
  opacity: 0.8;
}

.spot.is-me .avatar {
  outline: 3px solid #ff7049;
  outline-offset: 3px;
}

.me-tag {
  flex-shrink: 0;
  padding: 0 0.45rem;
  border-radius: 999px;
  background: #ff7049;
  color: #fff;
  font-size: 0.75rem;
  font-weight: 800;
  line-height: 1.6;
}

/* ── 第 4 名以後 ── */
.rank-list {
  display: grid;
  gap: 0.5rem;
  margin: 1rem 0 0;
  padding: 0;
  list-style: none;
}

.rank-row {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 0.7rem 1.1rem;
  border: 1.5px solid transparent;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 6px 18px rgb(41 43 53 / 6%);
}

.rank-row.is-me {
  border-color: #ff7049;
  background: #fff7f3;
}

.row-rank {
  width: 2rem;
  color: #9aa1ad;
  font-family: var(--font-latin);
  font-size: 1.25rem;
  font-weight: 900;
  text-align: center;
}

.avatar.small {
  width: 2.6rem;
  height: 2.6rem;
  border: 0;
  font-size: 1.05rem;
  box-shadow: none;
}

.row-body {
  flex: 1;
  min-width: 0;
}

.row-name {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  margin: 0;
  overflow: hidden;
  color: #1e2b45;
  font-weight: 800;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.row-detail {
  margin: 0;
  color: #9aa1ad;
  font-size: var(--text-min);
}

.row-count {
  margin: 0;
  color: #6c7380;
  font-size: var(--text-min);
  white-space: nowrap;
}

.row-count strong {
  color: #1e2b45;
  font-family: var(--font-latin);
  font-size: 1.3rem;
}

.rank-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  margin: 1.1rem 0 0;
  color: #9aa1ad;
  font-size: var(--text-min);
  text-align: center;
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(40px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes twinkle {
  0%,
  100% {
    opacity: 0.3;
  }

  50% {
    opacity: 1;
  }
}

/* ── 手機 ── */
@media (max-width: 519px) {
  .rank-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .period-switch {
    justify-content: space-between;
  }

  .period-btn {
    flex: 1;
    min-width: 0;
  }

  .stage {
    padding: 1.8rem 0.5rem 0;
  }

  .podium {
    gap: 0.4rem;
  }

  .avatar {
    width: 3.2rem;
    height: 3.2rem;
    font-size: 1.2rem;
  }

  .medal-1 .avatar {
    width: 3.8rem;
    height: 3.8rem;
    font-size: 1.45rem;
  }

  .spot-name {
    font-size: var(--text-min);
  }

  .rank-row {
    gap: 0.6rem;
    padding: 0.65rem 0.8rem;
  }
}
</style>
