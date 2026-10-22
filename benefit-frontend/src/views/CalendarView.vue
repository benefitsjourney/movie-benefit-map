<script setup>
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Icon } from "@iconify/vue";
import FullCalendar from "@fullcalendar/vue3";
import dayGridPlugin from "@fullcalendar/daygrid";
import interactionPlugin from "@fullcalendar/interaction";
import zhTwLocale from "@fullcalendar/core/locales/zh-tw";

import calendarIcon from "@iconify-icons/mdi/calendar-month-outline";
import calendarStarIcon from "@iconify-icons/mdi/calendar-star";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import ticketIcon from "@iconify-icons/mdi/ticket-outline";
import timerIcon from "@iconify-icons/mdi/timer-sand";
import movieIcon from "@iconify-icons/mdi/movie-open-outline";
import bellIcon from "@iconify-icons/mdi/bell-outline";
import googleIcon from "@iconify-icons/mdi/google";
import arrowIcon from "@iconify-icons/mdi/arrow-right";
import accountIcon from "@iconify-icons/mdi/account-star-outline";
import filmIcon from "@iconify-icons/mdi/filmstrip";

import http from "@/plugins/axios";
import { useAuth } from "@/composables/useAuth";
import { openAppAlert } from "@/components/ui/Alert.vue";
import PageIntro from "@/components/layout/PageIntro.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import AppModal from "@/components/ui/AppModal.vue";

const router = useRouter();
const route = useRoute();
const auth = useAuth();

// 三種事件：顏色、圖示、排序（同一天裡開始→截止→上映）
const EVENT_TYPES = {
  BENEFIT_START: { label: "特典開始", cls: "ev-start", icon: giftIcon, order: 1 },
  BENEFIT_END: { label: "發放截止", cls: "ev-end", icon: timerIcon, order: 2 },
  MOVIE_RELEASE: { label: "電影上映", cls: "ev-movie", icon: movieIcon, order: 3 },
};

const BENEFIT_TYPES = {
  REGULAR: "一般場次",
  SPECIAL_BY_DATE: "入場贈送",
  SPECIAL_UNTIL_OUT: "限量兌換",
};

// ── 狀態 ──
const mode = ref("all"); // all：全部特典（公開）／mine：我的追蹤（要登入）
const currentMonth = ref(""); // "2026-10"，由 FullCalendar 切換月份時填入
const events = ref([]);
const loading = ref(false);
const loadFailed = ref(false);
const selectedDate = ref(toYmd(new Date()));
const shownTypes = ref(new Set(Object.keys(EVENT_TYPES)));
const detail = ref(null); // 點事件打開的視窗內容
const detailOpen = ref(false);
const brokenImages = ref(new Set());
let requestId = 0;

// ── 日期小工具 ──
function pad(n) {
  return String(n).padStart(2, "0");
}

// Date → "2026-10-24"（用本地時間，避免時區差一天）
function toYmd(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

// "2026-10-24 00:00:00" → "2026-10-24"
function dayOf(value) {
  return typeof value === "string" ? value.slice(0, 10) : "";
}

// "2026-10-24" → Date
function parseYmd(ymd) {
  const [y, m, d] = ymd.split("-").map(Number);
  return new Date(y, m - 1, d);
}

function nextDay(ymd) {
  const date = parseYmd(ymd);
  date.setDate(date.getDate() + 1);
  return toYmd(date);
}

// "2026-10-24" → "10/24（六）"
function dayLabel(ymd) {
  if (!ymd) return "";
  const date = parseYmd(ymd);
  const week = "日一二三四五六"[date.getDay()];
  return `${date.getMonth() + 1}/${date.getDate()}（${week}）`;
}

// "2026-10-07 00:00:00" → "10/07"
function shortDate(value) {
  const matched = typeof value === "string" && value.match(/^\d{4}-(\d{2})-(\d{2})/);
  return matched ? `${matched[1]}/${matched[2]}` : "";
}

function periodText(ev) {
  const start = shortDate(ev.startDate);
  const end = shortDate(ev.endDate);
  if (start && end) return `${start} ～ ${end}`;
  if (start) return `${start} 起，送完為止`;
  return "日期未提供";
}

// ── 顯示文字 ──
function isBenefit(ev) {
  return ev.eventType !== "MOVIE_RELEASE";
}

function eventTitle(ev) {
  return isBenefit(ev) ? ev.benefitName || "未命名特典" : ev.movieName || "未命名電影";
}

function eventSubtitle(ev) {
  if (isBenefit(ev)) return ev.movieName ? `《${ev.movieName}》` : "電影未提供";
  return "電影上映";
}

function imageOf(ev) {
  const raw = isBenefit(ev) ? ev.officialImageUrl : ev.movieImageUrl;
  const url = typeof raw === "string" ? raw.trim() : "";
  return url && !brokenImages.value.has(url) ? url : "";
}

function markBroken(ev) {
  const raw = isBenefit(ev) ? ev.officialImageUrl : ev.movieImageUrl;
  if (raw) brokenImages.value.add(raw.trim());
}

// 加入 Google 日曆：用 Google 提供的網址開「新增活動」畫面（整天的活動）
function googleCalendarUrl(ev) {
  const day = dayOf(ev.date);
  if (!day) return "";
  const prefix = { BENEFIT_START: "特典開始", BENEFIT_END: "特典截止", MOVIE_RELEASE: "電影上映" }[ev.eventType];
  const lines = [];
  if (isBenefit(ev)) {
    if (ev.movieName) lines.push(`電影：《${ev.movieName}》`);
    lines.push(`發放期間：${periodText(ev)}`);
    lines.push(`${window.location.origin}/benefits/${ev.benefitId}`);
  } else {
    lines.push(`上映日：${dayLabel(day)}`);
  }
  const params = new URLSearchParams({
    action: "TEMPLATE",
    text: `【${prefix}】${eventTitle(ev)}`,
    dates: `${day.replaceAll("-", "")}/${nextDay(day).replaceAll("-", "")}`,
    details: lines.join("\n"),
  });
  return `https://calendar.google.com/calendar/render?${params.toString()}`;
}

// ── 統計與篩選 ──
const typeOptions = computed(() =>
  Object.entries(EVENT_TYPES)
    // 「發放截止」只有我的追蹤才有
    .filter(([key]) => mode.value === "mine" || key !== "BENEFIT_END")
    .map(([key, value]) => ({
      key,
      ...value,
      count: events.value.filter((ev) => ev.eventType === key).length,
    })),
);

const visibleEvents = computed(() => events.value.filter((ev) => shownTypes.value.has(ev.eventType)));

const dayEvents = computed(() => visibleEvents.value.filter((ev) => dayOf(ev.date) === selectedDate.value));

const monthTitle = computed(() => {
  if (!currentMonth.value) return "";
  const [y, m] = currentMonth.value.split("-");
  return `${y} 年 ${Number(m)} 月`;
});

function toggleType(key) {
  const next = new Set(shownTypes.value);
  if (next.has(key)) next.delete(key);
  else next.add(key);
  shownTypes.value = next;
}

// ── FullCalendar 設定 ──
const fcEvents = computed(() =>
  visibleEvents.value.map((ev, index) => ({
    id: `${ev.eventType}-${ev.benefitId ?? ev.movieId}-${index}`,
    title: eventTitle(ev),
    start: dayOf(ev.date),
    allDay: true,
    classNames: [EVENT_TYPES[ev.eventType]?.cls ?? ""],
    extendedProps: { raw: ev, order: EVENT_TYPES[ev.eventType]?.order ?? 9 },
  })),
);

const calendarOptions = computed(() => {
  const picked = selectedDate.value;
  return {
    plugins: [dayGridPlugin, interactionPlugin],
    initialView: "dayGridMonth",
    locale: zhTwLocale,
    headerToolbar: { left: "today", center: "title", right: "prev,next" },
    height: "auto",
    fixedWeekCount: false,
    showNonCurrentDates: false,
    dayMaxEvents: 3,
    eventOrder: "order,title",
    events: fcEvents.value,
    dayCellClassNames: (arg) => (toYmd(arg.date) === picked ? ["is-picked"] : []),
    datesSet: onDatesSet,
    dateClick: (info) => {
      selectedDate.value = info.dateStr;
    },
    eventClick: (info) => {
      info.jsEvent.preventDefault();
      openDetail(info.event.extendedProps.raw);
    },
  };
});

function onDatesSet(info) {
  const start = info.view.currentStart;
  const month = `${start.getFullYear()}-${pad(start.getMonth() + 1)}`;
  if (month !== currentMonth.value) currentMonth.value = month;
}

// ── 載入資料 ──
async function askLogin(text) {
  const result = await openAppAlert({
    variant: "info",
    title: "請先登入",
    text,
    confirmText: "前往登入",
    cancelText: "取消",
  });
  if (result.isConfirmed) {
    router.push({ name: "login", query: { redirect: route.fullPath } });
  }
}

async function loadEvents() {
  if (!currentMonth.value) return;
  const id = ++requestId;
  loading.value = true;
  loadFailed.value = false;

  const url = mode.value === "mine" ? "/reminders/calendar" : "/benefits/calendar";
  try {
    const { data } = await http.get(url, { params: { month: currentMonth.value } });
    if (id !== requestId) return; // 已經切到別的月份了，這次的結果不要用
    events.value = Array.isArray(data) ? data : [];

    // 選的日期不在這個月 → 改選這個月第一個有事件的日子（沒有就選 1 號）
    if (!selectedDate.value.startsWith(currentMonth.value)) {
      const first = events.value[0];
      selectedDate.value = first ? dayOf(first.date) : `${currentMonth.value}-01`;
    }
  } catch (error) {
    if (id !== requestId) return;
    events.value = [];
    if (error?.response?.status === 401 && mode.value === "mine") {
      mode.value = "all";
      await askLogin("登入已過期，請重新登入後再查看追蹤的行事曆。");
      return;
    }
    loadFailed.value = true;
  } finally {
    if (id === requestId) loading.value = false;
  }
}

watch([currentMonth, mode], loadEvents);

async function switchMode(next) {
  if (next === mode.value) return;
  if (next === "mine" && !auth.isLoggedIn.value) {
    await askLogin("登入後就能在行事曆上看到你追蹤的特典。");
    return;
  }
  mode.value = next;
}

function openDetail(ev) {
  detail.value = ev;
  detailOpen.value = true;
}
</script>

<template>
  <section>
    <PageIntro kicker="✦ BENEFIT CALENDAR" lead="特典" mark="行事曆" tail="" description="哪天開始發特典、哪天有新片上映，一張月曆就看得到。"
      :icon-left="calendarIcon" :icon-right="ticketIcon" :icon-top="calendarStarIcon" />

    <!-- 上方工具列：切換全部／我的追蹤、事件種類篩選 -->
    <div class="calendar-toolbar">
      <div class="mode-switch" role="tablist" aria-label="行事曆內容">
        <button type="button" role="tab" class="mode-btn" :class="{ 'is-on': mode === 'all' }"
          :aria-selected="mode === 'all'" @click="switchMode('all')">
          <Icon :icon="giftIcon" aria-hidden="true" />
          全部特典
        </button>
        <button type="button" role="tab" class="mode-btn" :class="{ 'is-on': mode === 'mine' }"
          :aria-selected="mode === 'mine'" @click="switchMode('mine')">
          <Icon :icon="bellIcon" aria-hidden="true" />
          我的追蹤
        </button>
      </div>

      <div class="type-chips" aria-label="顯示的事件種類">
        <button v-for="type in typeOptions" :key="type.key" type="button" class="type-chip" :class="[type.cls, {
          'is-off': !shownTypes.has(type.key)
        }]" :aria-pressed="shownTypes.has(type.key)" @click="toggleType(type.key)">
          <Icon :icon="type.icon" aria-hidden="true" />
          {{ type.label }}
          <span class="chip-count">{{ type.count }}</span>
        </button>
      </div>
    </div>

    <StatePanel v-if="loadFailed" variant="error" title="目前無法取得行事曆" message="請確認連線後再試一次。" @retry="loadEvents" />

    <div v-show="!loadFailed" class="calendar-layout">
      <!-- 月曆 -->
      <div class="calendar-card" :class="{ 'is-loading': loading }">
        <FullCalendar :options="calendarOptions">
          <template #eventContent="arg">
            <span class="ev-pill" :title="`${arg.event.title}`">
              <Icon :icon="EVENT_TYPES[arg.event.extendedProps.raw.eventType]?.icon ?? giftIcon" aria-hidden="true" />
              <span class="ev-text">{{ arg.event.title }}</span>
            </span>
          </template>
        </FullCalendar>
        <div v-if="loading" class="calendar-loading" aria-live="polite">
          <Icon :icon="calendarIcon" aria-hidden="true" />
          載入{{ monthTitle }}…
        </div>
      </div>

      <!-- 右側：選到的那一天 -->
      <aside class="day-panel" aria-live="polite">
        <header class="day-head">
          <p class="day-kicker">{{ mode === "mine" ? "我的追蹤" : "這一天" }}</p>
          <h2 class="day-title">{{ dayLabel(selectedDate) }}</h2>
        </header>

        <ul v-if="dayEvents.length" class="day-list">
          <li v-for="(ev, index) in dayEvents" :key="`${ev.eventType}-${ev.benefitId ?? ev.movieId}-${index}`">
            <button type="button" class="day-item" :class="EVENT_TYPES[ev.eventType]?.cls" @click="openDetail(ev)">
              <span class="day-thumb">
                <img v-if="imageOf(ev)" :src="imageOf(ev)" :alt="eventTitle(ev)" referrerpolicy="no-referrer"
                  loading="lazy" @error="markBroken(ev)" />
                <Icon v-else :icon="isBenefit(ev) ? giftIcon : filmIcon" aria-hidden="true" />
              </span>
              <span class="day-body">
                <span class="day-tag">
                  <Icon :icon="EVENT_TYPES[ev.eventType]?.icon" aria-hidden="true" />
                  {{ EVENT_TYPES[ev.eventType]?.label }}
                </span>
                <span class="day-name">{{ eventTitle(ev) }}</span>
                <span class="day-sub">{{ eventSubtitle(ev) }}</span>
              </span>
            </button>
          </li>
        </ul>

        <div v-else class="day-empty">
          <Icon :icon="calendarStarIcon" aria-hidden="true" />
          <p v-if="mode === 'mine'">這天沒有追蹤的特典。<br />到特典頁按「追蹤」，日期就會出現在這裡。</p>
          <p v-else>這天沒有特典開始，也沒有電影上映。<br />點月曆上有標記的日子看看吧！</p>
        </div>

        <RouterLink v-if="mode === 'all'" class="day-more" to="/benefits">
          看所有發放中的特典
          <Icon :icon="arrowIcon" aria-hidden="true" />
        </RouterLink>
      </aside>
    </div>

    <!-- 事件詳細視窗 -->
    <AppModal v-model="detailOpen" :title="detail ? EVENT_TYPES[detail.eventType]?.label : ''" width="34rem">
      <div v-if="detail" class="detail" :class="EVENT_TYPES[detail.eventType]?.cls">
        <div class="detail-image">
          <img v-if="imageOf(detail)" :src="imageOf(detail)" :alt="eventTitle(detail)" referrerpolicy="no-referrer"
            @error="markBroken(detail)" />
          <Icon v-else :icon="isBenefit(detail) ? giftIcon : filmIcon" aria-hidden="true" />
        </div>

        <div class="detail-body">
          <span class="day-tag">
            <Icon :icon="EVENT_TYPES[detail.eventType]?.icon" aria-hidden="true" />
            {{ EVENT_TYPES[detail.eventType]?.label }}・{{ dayLabel(dayOf(detail.date)) }}
          </span>
          <h3 class="detail-name">{{ eventTitle(detail) }}</h3>

          <dl v-if="isBenefit(detail)" class="detail-list">
            <div>
              <dt>電影</dt>
              <dd>{{ detail.movieName ? `《${detail.movieName}》` : "—" }}</dd>
            </div>
            <div>
              <dt>發放期間</dt>
              <dd>{{ periodText(detail) }}</dd>
            </div>
            <div>
              <dt>類型</dt>
              <dd>{{ BENEFIT_TYPES[detail.benefitType] ?? "未分類" }}</dd>
            </div>
            <div v-if="detail.isMemberOnly">
              <dt>領取資格</dt>
              <dd class="member-only">
                <Icon :icon="accountIcon" aria-hidden="true" />
                影城會員限定
              </dd>
            </div>
          </dl>
          <dl v-else class="detail-list">
            <div>
              <dt>上映日</dt>
              <dd>{{ dayLabel(dayOf(detail.date)) }}</dd>
            </div>
          </dl>

          <div class="detail-actions">
            <RouterLink v-if="isBenefit(detail)" class="action-btn is-main" :to="`/benefits/${detail.benefitId}`"
              @click="detailOpen = false">
              查看特典與庫存
              <Icon :icon="arrowIcon" aria-hidden="true" />
            </RouterLink>
            <a v-if="googleCalendarUrl(detail)" class="action-btn" :href="googleCalendarUrl(detail)" target="_blank"
              rel="noopener noreferrer">
              <Icon :icon="googleIcon" aria-hidden="true" />
              加入 Google 日曆
            </a>
          </div>
        </div>
      </div>
    </AppModal>
  </section>
</template>

<style scoped>
/* 三種事件的顏色 */
.ev-start {
  --ev: #ff7049;
  --ev-soft: #fff1ea;
  --ev-ink: #b2421f;
}

.ev-end {
  --ev: #e0457b;
  --ev-soft: #fff0f6;
  --ev-ink: #a33b6b;
}

.ev-movie {
  --ev: #4a74c9;
  --ev-soft: #edf3ff;
  --ev-ink: #2f4f94;
}

/* ── 工具列 ── */
.calendar-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.85rem;
  margin: 1.25rem 0 1rem;
}

.mode-switch {
  display: inline-flex;
  padding: 0.3rem;
  border-radius: 999px;
  background: #1e2b45;
  box-shadow: 0 6px 16px rgb(30 43 69 / 18%);
}

.mode-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  min-height: 2.5rem;
  padding: 0.35rem 1.1rem;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #cfd6e4;
  font-size: var(--text-control);
  font-weight: 800;
  cursor: pointer;
  transition:
    background-color var(--ease),
    color var(--ease);
}

.mode-btn :deep(svg) {
  font-size: 1.2rem;
}

.mode-btn:hover:not(.is-on) {
  color: #fff;
}

.mode-btn.is-on {
  background: #ff7049;
  color: #fff;
}

.mode-btn:focus-visible {
  outline: 2px solid #ffc857;
  outline-offset: 2px;
}

.type-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.type-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  min-height: 2.25rem;
  padding: 0.25rem 0.85rem;
  border: 1.5px solid var(--ev);
  border-radius: 999px;
  background: var(--ev-soft);
  color: var(--ev-ink);
  font-size: var(--text-min);
  font-weight: 800;
  cursor: pointer;
  transition:
    opacity var(--ease),
    background-color var(--ease);
}

.type-chip :deep(svg) {
  font-size: 1.1rem;
}

.chip-count {
  min-width: 1.5rem;
  padding: 0 0.4rem;
  border-radius: 999px;
  background: var(--ev);
  color: #fff;
  text-align: center;
}

.type-chip.is-off {
  border-style: dashed;
  background: #fff;
  opacity: 0.55;
}

.type-chip:focus-visible {
  outline: 2px solid var(--ev);
  outline-offset: 2px;
}

/* ── 版面：月曆＋右側當日清單 ── */
.calendar-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 20rem;
  gap: 1.25rem;
  align-items: start;
}

@media (max-width: 991px) {
  .calendar-layout {
    grid-template-columns: 1fr;
  }
}

/* ── 月曆卡片（FullCalendar 外觀調整）── */
.calendar-card {
  position: relative;
  padding: 1.1rem 1.1rem 1.25rem;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 10px 30px rgb(41 43 53 / 10%);
}

.calendar-card.is-loading :deep(.fc-daygrid-body) {
  opacity: 0.45;
  transition: opacity var(--ease);
}

.calendar-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.55rem 1.1rem;
  border-radius: 999px;
  background: #1e2b45;
  color: #fff;
  font-weight: 800;
  transform: translate(-50%, -50%);
  box-shadow: 0 8px 20px rgb(30 43 69 / 25%);
}

.calendar-card :deep(.fc) {
  --fc-border-color: #f1e6dc;
  --fc-today-bg-color: #fff8ec;
  --fc-page-bg-color: #fff;
  --fc-neutral-bg-color: #fbf7f2;
  --fc-button-bg-color: #fff;
  --fc-button-border-color: #ffd3c2;
  --fc-button-text-color: #b2421f;
  --fc-button-hover-bg-color: #fff1ea;
  --fc-button-hover-border-color: #ff7049;
  --fc-button-active-bg-color: #ff7049;
  --fc-button-active-border-color: #ff7049;
  --fc-event-border-color: transparent;
  font-size: var(--text-min);
}

.calendar-card :deep(.fc .fc-toolbar.fc-header-toolbar) {
  margin-bottom: 1rem;
}

.calendar-card :deep(.fc .fc-toolbar-title) {
  color: #1e2b45;
  font-size: 1.45rem;
  font-weight: 900;
  letter-spacing: 0.04em;
}

.calendar-card :deep(.fc .fc-button) {
  border-width: 1.5px;
  border-radius: 999px;
  font-weight: 800;
  box-shadow: none;
}

.calendar-card :deep(.fc .fc-button:focus) {
  box-shadow: 0 0 0 3px rgb(255 112 73 / 25%);
}

.calendar-card :deep(.fc .fc-button-group > .fc-button) {
  margin: 0 0.15rem;
  border-radius: 999px;
}

.calendar-card :deep(.fc .fc-button:disabled) {
  opacity: 0.45;
}

.calendar-card :deep(.fc-theme-standard .fc-scrollgrid) {
  overflow: hidden;
  border-radius: 14px;
}

/* 星期列 */
.calendar-card :deep(.fc .fc-col-header-cell) {
  padding: 0.45rem 0;
  background: #1e2b45;
  border-color: #1e2b45;
}

.calendar-card :deep(.fc .fc-col-header-cell-cushion) {
  color: #fff;
  font-weight: 800;
  text-decoration: none;
}

.calendar-card :deep(.fc .fc-day-sun .fc-col-header-cell-cushion),
.calendar-card :deep(.fc .fc-day-sat .fc-col-header-cell-cushion) {
  color: #ffc857;
}

/* 日期格 */
.calendar-card :deep(.fc .fc-daygrid-day) {
  cursor: pointer;
  transition: background-color var(--ease);
}

.calendar-card :deep(.fc .fc-daygrid-day:hover) {
  background: #fffaf5;
}

.calendar-card :deep(.fc .fc-daygrid-day-frame) {
  min-height: 6.2rem;
}

.calendar-card :deep(.fc .fc-daygrid-day-number) {
  padding: 0.35rem 0.5rem;
  color: #3e4450;
  font-weight: 800;
  text-decoration: none;
}

.calendar-card :deep(.fc .fc-day-today .fc-daygrid-day-number) {
  display: grid;
  place-items: center;
  min-width: 1.9rem;
  height: 1.9rem;
  margin: 0.2rem;
  padding: 0;
  border-radius: 50%;
  background: #ffc857;
  color: #1e2b45;
}

.calendar-card :deep(.fc .fc-daygrid-day.is-picked) {
  background: #fff1ea;
  box-shadow: inset 0 0 0 2px #ff7049;
}

/* 事件條 */
.calendar-card :deep(.fc .fc-daygrid-event) {
  margin: 0.12rem 0.3rem 0;
  border: 0;
  border-radius: 7px;
  cursor: pointer;
  transition:
    transform var(--ease),
    box-shadow var(--ease);
}

.calendar-card :deep(.fc .fc-daygrid-event:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgb(41 43 53 / 18%);
}

.calendar-card :deep(.fc .fc-h-event.ev-start) {
  background: #ff7049;
}

.calendar-card :deep(.fc .fc-h-event.ev-end) {
  background: #e0457b;
}

.calendar-card :deep(.fc .fc-h-event.ev-movie) {
  background: #4a74c9;
}

/* 事件條裡的文字（由 FullCalendar 畫出來，所以用 :deep） */
.calendar-card :deep(.ev-pill) {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  min-width: 0;
  padding: 0.12rem 0.35rem;
  color: #fff;
  font-weight: 700;
  line-height: 1.35;
}

.calendar-card :deep(.ev-pill svg) {
  flex: none;
  font-size: 0.95rem;
}

.calendar-card :deep(.ev-text) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.calendar-card :deep(.fc .fc-daygrid-more-link) {
  margin: 0.15rem 0.35rem 0;
  color: #b2421f;
  font-weight: 800;
}

.calendar-card :deep(.fc .fc-popover) {
  overflow: hidden;
  border: 0;
  border-radius: 14px;
  box-shadow: 0 12px 30px rgb(41 43 53 / 22%);
}

.calendar-card :deep(.fc .fc-popover-header) {
  padding: 0.5rem 0.75rem;
  background: #1e2b45;
  color: #fff;
  font-weight: 800;
}

.calendar-card :deep(.fc .fc-popover-close) {
  color: #fff;
  opacity: 0.85;
}

/* 手機：格子太窄，事件只留圖示當小圓點 */
@media (max-width: 600px) {
  .calendar-card {
    padding: 0.75rem 0.5rem;
  }

  .calendar-card :deep(.fc .fc-toolbar-title) {
    font-size: 1.15rem;
  }

  .calendar-card :deep(.fc .fc-daygrid-day-frame) {
    min-height: 4.2rem;
  }

  .calendar-card :deep(.fc .fc-daygrid-event) {
    margin: 0.1rem 0.15rem 0;
  }

  .calendar-card :deep(.ev-text) {
    display: none;
  }

  .calendar-card :deep(.ev-pill) {
    justify-content: center;
    padding: 0.1rem;
  }
}

/* ── 右側：當日清單 ── */
.day-panel {
  position: sticky;
  top: 1rem;
  padding: 1.1rem;
  border-radius: 20px;
  background: #1e2b45;
  color: #fff;
  box-shadow: 0 10px 30px rgb(30 43 69 / 22%);
}

.day-head {
  margin-bottom: 0.85rem;
}

.day-kicker {
  margin: 0;
  color: #ffc857;
  font-size: var(--text-min);
  font-weight: 800;
  letter-spacing: 0.08em;
}

.day-title {
  margin: 0.1rem 0 0;
  color: #fff;
  font-size: 1.6rem;
  font-weight: 900;
}

.day-list {
  display: grid;
  gap: 0.6rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.day-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  padding: 0.55rem;
  border: 0;
  border-left: 5px solid var(--ev);
  border-radius: 12px;
  background: #fff;
  color: #292b35;
  text-align: left;
  cursor: pointer;
  transition:
    transform var(--ease),
    box-shadow var(--ease);
}

.day-item:hover {
  transform: translateX(3px);
  box-shadow: 0 6px 16px rgb(0 0 0 / 22%);
}

.day-item:focus-visible {
  outline: 2px solid #ffc857;
  outline-offset: 2px;
}

.day-thumb {
  display: grid;
  flex: none;
  place-items: center;
  width: 3.4rem;
  height: 3.4rem;
  overflow: hidden;
  border-radius: 10px;
  background: var(--ev-soft);
  color: var(--ev);
  font-size: 1.6rem;
}

.day-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.day-body {
  display: grid;
  gap: 0.1rem;
  min-width: 0;
}

.day-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  width: fit-content;
  padding: 0.05rem 0.55rem;
  border-radius: 999px;
  background: var(--ev-soft);
  color: var(--ev-ink);
  font-size: 0.8rem;
  font-weight: 800;
}

.day-name {
  overflow: hidden;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.day-sub {
  overflow: hidden;
  color: #6c7380;
  font-size: var(--text-min);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.day-empty {
  display: grid;
  justify-items: center;
  gap: 0.4rem;
  padding: 1.5rem 0.5rem;
  border: 1.5px dashed rgb(255 255 255 / 25%);
  border-radius: 14px;
  color: #cfd6e4;
  text-align: center;
}

.day-empty :deep(svg) {
  color: #ffc857;
  font-size: 2.2rem;
}

.day-empty p {
  margin: 0;
  font-size: var(--text-min);
  line-height: 1.6;
}

.day-more {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  margin-top: 1rem;
  color: #ffc857;
  font-size: var(--text-min);
  font-weight: 800;
  text-underline-offset: 0.18em;
}

@media (max-width: 991px) {
  .day-panel {
    position: static;
  }
}

/* ── 詳細視窗 ── */
.detail {
  display: grid;
  grid-template-columns: 10rem minmax(0, 1fr);
  gap: 1.1rem;
}

@media (max-width: 520px) {
  .detail {
    grid-template-columns: 1fr;
  }
}

.detail-image {
  display: grid;
  place-items: center;
  aspect-ratio: 3 / 4;
  overflow: hidden;
  border-radius: 14px;
  background: var(--ev-soft);
  color: var(--ev);
  font-size: 3rem;
}

.detail-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-body {
  display: grid;
  align-content: start;
  gap: 0.5rem;
}

.detail-name {
  margin: 0;
  color: #1e2b45;
  font-size: 1.3rem;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.detail-list {
  display: grid;
  gap: 0.35rem;
  margin: 0.25rem 0 0;
}

.detail-list > div {
  display: grid;
  grid-template-columns: 4.5rem minmax(0, 1fr);
  gap: 0.5rem;
}

.detail-list dt {
  color: #6c7380;
  font-weight: 700;
}

.detail-list dd {
  margin: 0;
  color: #292b35;
  font-weight: 700;
}

.member-only {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  color: #a33b6b;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin-top: 0.75rem;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  min-height: 2.6rem;
  padding: 0.35rem 1rem;
  border: 1.5px solid #ff7049;
  border-radius: 999px;
  background: #fff;
  color: #b2421f;
  font-size: var(--text-control);
  font-weight: 800;
  text-decoration: none;
  transition: background-color var(--ease);
}

.action-btn:hover {
  background: #fff1ea;
}

.action-btn.is-main {
  background: #ff7049;
  color: #fff;
}

.action-btn.is-main:hover {
  background: #e85f3a;
}

.action-btn:focus-visible {
  outline: 2px solid #ff7049;
  outline-offset: 3px;
}
</style>
