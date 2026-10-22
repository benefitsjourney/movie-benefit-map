<script setup>
import { computed, onMounted, ref } from "vue";
import { Icon } from "@iconify/vue";

import bellIcon from "@iconify-icons/mdi/bell-ring-outline";
import bellOffIcon from "@iconify-icons/mdi/bell-off-outline";
import alarmIcon from "@iconify-icons/mdi/alarm";
import calendarIcon from "@iconify-icons/mdi/calendar-month-outline";
import calendarStarIcon from "@iconify-icons/mdi/calendar-star";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import ticketIcon from "@iconify-icons/mdi/ticket-outline";
import filmIcon from "@iconify-icons/mdi/filmstrip";
import googleIcon from "@iconify-icons/mdi/google";
import arrowIcon from "@iconify-icons/mdi/arrow-right";
import accountIcon from "@iconify-icons/mdi/account-star-outline";

import http from "@/plugins/axios";
import { useBenefitReminders } from "@/composables/useBenefitReminders";
import PageIntro from "@/components/layout/PageIntro.vue";
import StatePanel from "@/components/layout/StatePanel.vue";

// 幾天內開始／截止要放進「近期提醒」
const SOON_DAYS = 3;

const GROUPS = [
  { key: "ongoing", label: "發放中", hint: "現在就能去領", tone: "tone-ongoing" },
  { key: "upcoming", label: "即將開始", hint: "還沒開始發放", tone: "tone-upcoming" },
  { key: "movie", label: "追蹤的電影", hint: "上映日會出現在行事曆", tone: "tone-movie" },
  { key: "ended", label: "已結束", hint: "發放期間已經過了", tone: "tone-ended" },
];

const BENEFIT_TYPES = {
  REGULAR: "一般場次",
  SPECIAL_BY_DATE: "入場贈送",
  SPECIAL_UNTIL_OUT: "限量兌換",
};

const { loadReminders, toggleFollow } = useBenefitReminders();

const items = ref([]);
const loading = ref(true);
const loadFailed = ref(false);
const brokenImages = ref(new Set());

// ── 日期小工具 ──
function pad(n) {
  return String(n).padStart(2, "0");
}

// "2026-10-24 00:00:00" → 當天 00:00 的 Date
function dayStart(value) {
  const matched = typeof value === "string" && value.match(/^(\d{4})-(\d{2})-(\d{2})/);
  return matched ? new Date(Number(matched[1]), Number(matched[2]) - 1, Number(matched[3])) : null;
}

function today() {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
}

// 從今天到那天差幾天（今天＝0、明天＝1、昨天＝-1）
function daysFromToday(value) {
  const day = dayStart(value);
  if (!day) return null;
  return Math.round((day - today()) / 86400000);
}

function ymd(date) {
  return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}`;
}

// "2026-10-07 00:00:00" → "10/07"
function shortDate(value) {
  const matched = typeof value === "string" && value.match(/^\d{4}-(\d{2})-(\d{2})/);
  return matched ? `${matched[1]}/${matched[2]}` : "";
}

function periodText(item) {
  const start = shortDate(item.startDate);
  const end = shortDate(item.endDate);
  if (start && end) return `${start} ～ ${end}`;
  if (start) return `${start} 起，送完為止`;
  return "日期未提供";
}

// ── 每筆追蹤的狀態 ──
function isMovie(item) {
  return item.reminderType === "MOVIE";
}

function groupOf(item) {
  if (isMovie(item)) return "movie";
  if (item.benefitStatus === "EXPIRED") return "ended";
  const startIn = daysFromToday(item.startDate);
  const endIn = daysFromToday(item.endDate);
  if (startIn != null && startIn > 0) return "upcoming";
  if (endIn != null && endIn < 0) return "ended";
  return "ongoing";
}

// 卡片右上角的倒數文字
function countdownText(item) {
  if (isMovie(item)) {
    const d = daysFromToday(item.releaseDate);
    if (d == null) return "上映日未定";
    if (d > 0) return `${d} 天後上映`;
    if (d === 0) return "今天上映";
    return "已上映";
  }
  const group = groupOf(item);
  if (group === "upcoming") {
    const d = daysFromToday(item.startDate);
    return d === 1 ? "明天開始" : `${d} 天後開始`;
  }
  if (group === "ongoing") {
    const d = daysFromToday(item.endDate);
    if (d == null) return "送完為止";
    if (d === 0) return "今天截止";
    return `剩 ${d} 天`;
  }
  return "已結束";
}

// 近期提醒：3 天內開始、3 天內截止、今天剛開始
const soonItems = computed(() => {
  const list = [];
  for (const item of items.value) {
    if (isMovie(item)) {
      const d = daysFromToday(item.releaseDate);
      if (d != null && d >= 0 && d <= SOON_DAYS) list.push({ item, kind: "movie", days: d });
      continue;
    }
    const group = groupOf(item);
    if (group === "upcoming") {
      const d = daysFromToday(item.startDate);
      if (d <= SOON_DAYS) list.push({ item, kind: "start", days: d });
    } else if (group === "ongoing") {
      const startIn = daysFromToday(item.startDate);
      const endIn = daysFromToday(item.endDate);
      if (endIn != null && endIn <= SOON_DAYS) list.push({ item, kind: "end", days: endIn });
      else if (startIn === 0) list.push({ item, kind: "start", days: 0 });
    }
  }
  // 越快發生的越前面；同一天的話「截止」排前面（比較急）
  return list.sort((a, b) => a.days - b.days || (a.kind === "end" ? -1 : 1));
});

function soonDayText(days) {
  if (days === 0) return "今天";
  if (days === 1) return "明天";
  return `${days} 天後`;
}

function soonKindText(kind) {
  return { start: "開始發放", end: "發放截止", movie: "電影上映" }[kind];
}

const grouped = computed(() =>
  GROUPS.map((group) => ({
    ...group,
    items: items.value
      .filter((item) => groupOf(item) === group.key)
      .sort((a, b) => {
        // 發放中：快截止的在前；即將開始：快開始的在前；已結束：最近結束的在前
        const key = group.key === "ongoing" ? "endDate" : group.key === "movie" ? "releaseDate" : "startDate";
        const av = dayStart(a[key])?.getTime() ?? Number.MAX_SAFE_INTEGER;
        const bv = dayStart(b[key])?.getTime() ?? Number.MAX_SAFE_INTEGER;
        return group.key === "ended" ? bv - av : av - bv;
      }),
  })).filter((group) => group.items.length),
);

const benefitCount = computed(() => items.value.filter((item) => !isMovie(item)).length);
const ongoingCount = computed(() => items.value.filter((item) => groupOf(item) === "ongoing").length);

// ── 顯示 ──
function titleOf(item) {
  return isMovie(item) ? item.movieName || "未命名電影" : item.benefitName || "未命名特典";
}

function imageOf(item) {
  const raw = isMovie(item) ? item.movieImageUrl : item.officialImageUrl;
  const url = typeof raw === "string" ? raw.trim() : "";
  return url && !brokenImages.value.has(url) ? url : "";
}

function markBroken(item) {
  const raw = isMovie(item) ? item.movieImageUrl : item.officialImageUrl;
  if (raw) brokenImages.value = new Set(brokenImages.value).add(raw.trim());
}

// 加入 Google 日曆：特典有開始和截止日就建成跨天的活動，沒有截止日就只放開始那天
function googleCalendarUrl(item) {
  const startDay = dayStart(isMovie(item) ? item.releaseDate : item.startDate);
  if (!startDay) return "";
  const endDay = (!isMovie(item) && dayStart(item.endDate)) || startDay;
  const after = new Date(endDay);
  after.setDate(after.getDate() + 1);

  const lines = [];
  if (isMovie(item)) {
    lines.push("電影上映");
  } else {
    if (item.movieName) lines.push(`電影：《${item.movieName}》`);
    lines.push(`發放期間：${periodText(item)}`);
    lines.push(`${window.location.origin}/benefits/${item.benefitId}`);
  }

  const params = new URLSearchParams({
    action: "TEMPLATE",
    text: isMovie(item) ? `【電影上映】${titleOf(item)}` : `【特典】${titleOf(item)}`,
    dates: `${ymd(startDay)}/${ymd(after)}`,
    details: lines.join("\n"),
  });
  return `https://calendar.google.com/calendar/render?${params.toString()}`;
}

// ── 載入／取消追蹤 ──
async function loadList() {
  loading.value = true;
  loadFailed.value = false;
  try {
    const { data } = await http.get("/reminders/me");
    items.value = Array.isArray(data) ? data : [];
  } catch {
    items.value = [];
    loadFailed.value = true;
  } finally {
    loading.value = false;
  }
}

async function cancelFollow(item) {
  if (isMovie(item)) {
    // 目前只有特典有追蹤按鈕；電影追蹤直接刪除
    try {
      await http.delete(`/reminders/${item.reminderId}`);
      items.value = items.value.filter((x) => x.reminderId !== item.reminderId);
    } catch {
      await loadList();
    }
    return;
  }
  const changed = await toggleFollow(item, { confirm: true });
  if (changed) {
    items.value = items.value.filter((x) => x.reminderId !== item.reminderId);
  }
}

onMounted(async () => {
  await Promise.all([loadList(), loadReminders(true)]);
});
</script>

<template>
  <section>
    <PageIntro kicker="✦ MY REMINDERS" lead="我的" mark="追蹤" tail="清單" description="追蹤的特典會出現在行事曆上，快開始或快截止時這裡會提醒你。"
      :icon-left="bellIcon" :icon-right="ticketIcon" :icon-top="calendarStarIcon" />

    <StatePanel v-if="loading" variant="loading" message="正在載入追蹤清單…" :icon="bellIcon" />
    <StatePanel v-else-if="loadFailed" variant="error" title="目前無法取得追蹤清單" message="請確認連線後再試一次。" @retry="loadList" />

    <template v-else>
      <!-- 摘要列 -->
      <div class="summary-bar">
        <p class="summary-text">
          <Icon :icon="bellIcon" aria-hidden="true" />
          追蹤中 <strong>{{ benefitCount }}</strong> 款特典<template v-if="ongoingCount">，其中
            <strong>{{ ongoingCount }}</strong> 款正在發放</template>
        </p>
        <RouterLink class="summary-link" :to="{ path: '/calendar', query: { mode: 'mine' } }">
          <Icon :icon="calendarIcon" aria-hidden="true" />
          在我的日曆上看
          <Icon :icon="arrowIcon" aria-hidden="true" />
        </RouterLink>
      </div>

      <!-- 沒有追蹤任何東西 -->
      <div v-if="!items.length" class="empty-card">
        <Icon :icon="bellOffIcon" aria-hidden="true" />
        <h2>還沒有追蹤的特典</h2>
        <p>在特典頁按下「追蹤」，開始日和截止日就會出現在你的行事曆，快到的時候這裡也會提醒你。</p>
        <RouterLink class="empty-btn" to="/benefits">
          去逛逛特典
          <Icon :icon="arrowIcon" aria-hidden="true" />
        </RouterLink>
      </div>

      <template v-else>
        <!-- 近期提醒 -->
        <section v-if="soonItems.length" class="soon-panel" aria-labelledby="soon-title">
          <header class="soon-head">
            <Icon :icon="alarmIcon" class="soon-icon" aria-hidden="true" />
            <div>
              <h2 id="soon-title">近期提醒</h2>
              <p>{{ SOON_DAYS }} 天內開始或截止的追蹤</p>
            </div>
          </header>
          <ul class="soon-list">
            <li v-for="entry in soonItems" :key="`${entry.kind}-${entry.item.reminderId}`">
              <RouterLink class="soon-item" :class="`kind-${entry.kind}`"
                :to="isMovie(entry.item) ? '/calendar' : `/benefits/${entry.item.benefitId}`">
                <span class="soon-day">{{ soonDayText(entry.days) }}</span>
                <span class="soon-body">
                  <span class="soon-kind">{{ soonKindText(entry.kind) }}</span>
                  <span class="soon-name">{{ titleOf(entry.item) }}</span>
                </span>
                <Icon :icon="arrowIcon" class="soon-arrow" aria-hidden="true" />
              </RouterLink>
            </li>
          </ul>
        </section>

        <!-- 分組清單 -->
        <section v-for="group in grouped" :key="group.key" class="group" :class="group.tone">
          <header class="group-head">
            <h2>{{ group.label }}</h2>
            <span class="group-count">{{ group.items.length }}</span>
            <span class="group-hint">{{ group.hint }}</span>
          </header>

          <ul class="card-grid">
            <li v-for="item in group.items" :key="item.reminderId" class="follow-card">
              <RouterLink class="card-image" :to="isMovie(item) ? '/calendar' : `/benefits/${item.benefitId}`"
                :aria-label="`查看 ${titleOf(item)}`">
                <img v-if="imageOf(item)" :src="imageOf(item)" :alt="titleOf(item)" referrerpolicy="no-referrer"
                  loading="lazy" @error="markBroken(item)" />
                <Icon v-else :icon="isMovie(item) ? filmIcon : giftIcon" aria-hidden="true" />
                <span class="countdown">{{ countdownText(item) }}</span>
              </RouterLink>

              <div class="card-body">
                <h3 class="card-name">{{ titleOf(item) }}</h3>
                <template v-if="!isMovie(item)">
                  <p class="card-movie">
                    <Icon :icon="filmIcon" aria-hidden="true" />
                    {{ item.movieName ? `《${item.movieName}》` : "電影未提供" }}
                  </p>
                  <p class="card-meta">
                    <Icon :icon="calendarIcon" aria-hidden="true" />
                    {{ periodText(item) }}
                  </p>
                  <p class="card-tags">
                    <span class="type-tag">{{ BENEFIT_TYPES[item.benefitType] ?? "未分類" }}</span>
                    <span v-if="item.isMemberOnly" class="member-tag">
                      <Icon :icon="accountIcon" aria-hidden="true" />
                      會員限定
                    </span>
                  </p>
                </template>
                <p v-else class="card-meta">
                  <Icon :icon="calendarIcon" aria-hidden="true" />
                  上映日 {{ shortDate(item.releaseDate) || "未定" }}
                </p>

                <div class="card-actions">
                  <a v-if="group.key !== 'ended' && googleCalendarUrl(item)" class="mini-btn"
                    :href="googleCalendarUrl(item)" target="_blank" rel="noopener noreferrer">
                    <Icon :icon="googleIcon" aria-hidden="true" />
                    加入 Google 日曆
                  </a>
                  <button type="button" class="mini-btn is-cancel" @click="cancelFollow(item)">
                    <Icon :icon="bellOffIcon" aria-hidden="true" />
                    取消追蹤
                  </button>
                </div>
              </div>
            </li>
          </ul>
        </section>
      </template>
    </template>
  </section>
</template>

<style scoped>
/* ── 摘要列 ── */
.summary-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin: 1.25rem 0 1rem;
}

.summary-text {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  margin: 0;
  color: #3e4450;
  font-size: var(--text-control);
  font-weight: 700;
}

.summary-text :deep(svg) {
  color: #ff7049;
  font-size: 1.35rem;
}

.summary-text strong {
  color: #ff7049;
  font-size: 1.25rem;
}

.summary-link {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  min-height: 2.6rem;
  padding: 0.35rem 1.1rem;
  border-radius: 999px;
  background: #1e2b45;
  color: #fff;
  font-size: var(--text-control);
  font-weight: 800;
  text-decoration: none;
  box-shadow: 0 6px 16px rgb(30 43 69 / 18%);
  transition: background-color var(--ease);
}

.summary-link:hover {
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
  max-width: 28rem;
  margin: 0;
  color: #6c7380;
  line-height: 1.7;
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

/* ── 近期提醒 ── */
.soon-panel {
  margin-bottom: 1.75rem;
  padding: 1.1rem 1.2rem 1.2rem;
  border-radius: 20px;
  background: linear-gradient(135deg, #1e2b45 0%, #2c3d5f 100%);
  color: #fff;
  box-shadow: 0 10px 30px rgb(30 43 69 / 22%);
}

.soon-head {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.9rem;
}

.soon-icon {
  color: #ffc857;
  font-size: 2.2rem;
  animation: wiggle 2.4s ease-in-out infinite;
  transform-origin: 50% 20%;
}

.soon-head h2 {
  margin: 0;
  color: #fff;
  font-size: 1.35rem;
  font-weight: 900;
}

.soon-head p {
  margin: 0;
  color: #cfd6e4;
  font-size: var(--text-min);
}

.soon-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr));
  gap: 0.65rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.soon-item {
  --kind: #ff7049;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.6rem 0.8rem;
  border-radius: 14px;
  background: #fff;
  color: #292b35;
  text-decoration: none;
  transition:
    transform var(--ease),
    box-shadow var(--ease);
}

.soon-item.kind-end {
  --kind: #e0457b;
}

.soon-item.kind-movie {
  --kind: #4a74c9;
}

.soon-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgb(0 0 0 / 25%);
  color: #292b35;
}

.soon-day {
  display: grid;
  flex: none;
  place-items: center;
  min-width: 3.6rem;
  height: 3.6rem;
  padding: 0 0.4rem;
  border-radius: 12px;
  background: var(--kind);
  color: #fff;
  font-size: 1.05rem;
  font-weight: 900;
}

.soon-body {
  display: grid;
  min-width: 0;
}

.soon-kind {
  color: var(--kind);
  font-size: var(--text-min);
  font-weight: 800;
}

.soon-name {
  overflow: hidden;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.soon-arrow {
  flex: none;
  margin-left: auto;
  color: #b0b6c2;
}

/* ── 分組 ── */
.tone-ongoing {
  --tone: #ff7049;
}

.tone-upcoming {
  --tone: #f0a020;
}

.tone-movie {
  --tone: #4a74c9;
}

.tone-ended {
  --tone: #9aa1ad;
}

.group {
  margin-bottom: 1.75rem;
}

.group-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.8rem;
  padding-left: 0.75rem;
  border-left: 5px solid var(--tone);
}

.group-head h2 {
  margin: 0;
  color: #1e2b45;
  font-size: 1.3rem;
  font-weight: 900;
}

.group-count {
  min-width: 1.8rem;
  padding: 0 0.5rem;
  border-radius: 999px;
  background: var(--tone);
  color: #fff;
  font-weight: 800;
  text-align: center;
}

.group-hint {
  color: #6c7380;
  font-size: var(--text-min);
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(15rem, 1fr));
  gap: 1rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.follow-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 8px 22px rgb(41 43 53 / 9%);
  transition:
    transform var(--ease),
    box-shadow var(--ease);
}

.follow-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 14px 30px rgb(41 43 53 / 14%);
}

.tone-ended .follow-card {
  opacity: 0.72;
}

.tone-ended .card-image img {
  filter: grayscale(0.6);
}

.card-image {
  position: relative;
  display: grid;
  place-items: center;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: color-mix(in srgb, var(--tone) 12%, white);
  color: var(--tone);
  font-size: 3rem;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.follow-card:hover .card-image img {
  transform: scale(1.04);
}

.countdown {
  position: absolute;
  top: 0.6rem;
  right: 0.6rem;
  padding: 0.2rem 0.75rem;
  border-radius: 999px;
  background: var(--tone);
  color: #fff;
  font-size: var(--text-min);
  font-weight: 900;
  box-shadow: 0 4px 10px rgb(0 0 0 / 20%);
}

.card-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 0.3rem;
  padding: 0.85rem 1rem 1rem;
}

.card-name {
  margin: 0;
  color: #1e2b45;
  font-size: var(--text-card-title, 1.25rem);
  font-weight: 900;
  overflow-wrap: anywhere;
}

.card-movie,
.card-meta {
  display: flex;
  align-items: flex-start;
  gap: 0.35rem;
  margin: 0;
  color: #3e4450;
  font-size: var(--text-min);
  font-weight: 700;
  overflow-wrap: anywhere;
}

.card-movie :deep(svg),
.card-meta :deep(svg) {
  flex: none;
  margin-top: 0.2em;
  color: var(--tone);
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  margin: 0.15rem 0 0;
}

.type-tag,
.member-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  padding: 0.05rem 0.6rem;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 800;
}

.type-tag {
  background: #f3f0ea;
  color: #5a5f6b;
}

.member-tag {
  background: #fff0f6;
  color: #a33b6b;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-top: auto;
  padding-top: 0.6rem;
}

.mini-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  min-height: 2.3rem;
  padding: 0.2rem 0.8rem;
  border: 1.5px solid #ffd3c2;
  border-radius: 999px;
  background: #fff;
  color: #b2421f;
  font-size: var(--text-min);
  font-weight: 800;
  text-decoration: none;
  cursor: pointer;
  transition: background-color var(--ease);
}

.mini-btn:hover {
  background: #fff1ea;
  color: #b2421f;
}

.mini-btn.is-cancel {
  border-color: #dfe3ea;
  color: #6c7380;
}

.mini-btn.is-cancel:hover {
  background: #f4f6f9;
  color: #a33b42;
}

.mini-btn:focus-visible,
.summary-link:focus-visible,
.soon-item:focus-visible,
.card-image:focus-visible {
  outline: 2px solid #ff7049;
  outline-offset: 3px;
}

@keyframes wiggle {

  0%,
  70%,
  100% {
    transform: rotate(0);
  }

  76% {
    transform: rotate(14deg);
  }

  84% {
    transform: rotate(-10deg);
  }

  92% {
    transform: rotate(6deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .soon-icon {
    animation: none;
  }
}
</style>
