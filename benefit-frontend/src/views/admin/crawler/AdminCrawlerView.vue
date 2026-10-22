<script setup>
import { onMounted, onUnmounted, reactive } from "vue";
import { useRouter } from "vue-router";
import { Icon } from "@iconify/vue";
import movieIcon from "@iconify-icons/mdi/movie-open-outline";
import clapperboardIcon from "@iconify-icons/mdi/clapperboard-outline";
import databaseIcon from "@iconify-icons/mdi/database-sync-outline";
import fileUploadIcon from "@iconify-icons/mdi/file-upload-outline";
import webIcon from "@iconify-icons/mdi/web";
import refreshIcon from "@iconify-icons/mdi/refresh";
import clockIcon from "@iconify-icons/mdi/clock-outline";

import http from "@/plugins/axios";
import { dateTimeText } from "@/components/ui/DataTable.vue";
import { apiMessage, authText } from "@/utils/apiError";
import PageIntro from "@/components/layout/PageIntro.vue";
import TicketCard from "@/components/ui/TicketCard.vue";
import {
  closeAppAlert,
  openAppAlert,
  updateAppAlert,
} from "@/components/ui/Alert.vue";

/** 集中定義每種爬蟲的 API、提交方式與完成後導向位置，供畫面及流程共用 */
const crawlers = [
  {
    key: "cinema",
    title: "影城同步",
    description: "更新影城名稱、地址及位置等資訊",
    endpoint: "/cinema-movie/crawler/cinemas",
    mode: "get",
    verb: "更新",
    subject: "影城",
    stamp: "影城",
    logType: "CINEMA",
    color: "purple",
    icon: movieIcon,
    resultTo: "/admin/cinemas",
    resultLabel: "查看影城",
    reviewTo: "admin-reviews",
  },
  {
    key: "movie",
    title: "電影同步",
    description: "更新最新、上映中及即將上映的電影",
    endpoint: "/cinema-movie/crawler/movies",
    mode: "get",
    verb: "更新",
    subject: "電影",
    stamp: "電影",
    logType: "MOVIE",
    color: "purple",
    icon: clapperboardIcon,
    resultTo: "/admin/movies",
    resultLabel: "查看電影",
    reviewTo: "admin-reviews",
  },
  {
    key: "benefit-apify",
    title: "特典同步",
    description: "抓取網址貼文，解析後更新特典",
    endpoint: "/crawler/benefits/import-apify",
    mode: "apify",
    verb: "同步",
    subject: "特典",
    stamp: "特典",
    logType: "BENEFIT",
    color: "pink",
    icon: webIcon,
    resultTo: "/map",
    resultLabel: "查看地圖",
  },
  {
    key: "benefit-json",
    title: "特典JSON匯入",
    description: "上傳貼文JSON檔案，解析後更新特典",
    endpoint: "/crawler/benefits/import-json",
    mode: "json",
    verb: "匯入",
    subject: "特典",
    stamp: "特典",
    logType: "BENEFIT",
    color: "pink",
    icon: fileUploadIcon,
    resultTo: "/map",
    resultLabel: "查看地圖",
  },
    {
    key: "benefit-rematch",
    title: "特典重新比對",
    description: "電影資料更新後，讓找不到電影的待審特典重新比對並上架",
    endpoint: "/crawler/benefits/rematch",
    mode: "rematch",
    verb: "比對",
    subject: "特典",
    stamp: "特典",
    logType: "BENEFIT",
    color: "pink",
    icon: refreshIcon,
    resultTo: "/admin/benefit-reports",
    resultLabel: "前往特典審核",
  },
];

/** 依爬蟲設定建立各自的執行狀態，避免為每張卡片重複宣告狀態 */
const jobs = reactive(Object.fromEntries(crawlers.map(({ key }) => [key, createJob()])));

/** 各分類最新一筆爬蟲紀錄的時間；同一分類的卡片共用 */
const LOG_TYPES = [...new Set(crawlers.map((crawler) => crawler.logType))];
const latestUpdatedAt = reactive(
  Object.fromEntries(LOG_TYPES.map((logType) => [logType, { status: "loading", text: "" }])),
);

/** 將紀錄時間轉成畫面上的日期時間，去掉小數秒 */
function formatUpdatedAt(value) {
  return dateTimeText(value).replace(/\.\d+$/, "");
}

/** 向爬蟲紀錄 API 取該分類最新一筆，並顯示其結束時間 */
async function loadLatestUpdatedAt(logType) {
  try {
    const { data } = await http.get("/crawler-logs", {
      params: {
        crawlerType: logType,
        orderBy: "EXECUTED_AT",
        sort: "DESC",
        page: 1,
        size: 1,
      },
    });
    const latest = data?.content?.[0];
    latestUpdatedAt[logType] = {
      status: "ready",
      text: formatUpdatedAt(latest?.finishedAt || latest?.executedAt),
    };
  } catch {
    if (latestUpdatedAt[logType]?.status === "loading") {
      latestUpdatedAt[logType] = { status: "error", text: "" };
    }
  }
}

function loadAllLatestUpdatedAt() {
  return Promise.all(LOG_TYPES.map((logType) => loadLatestUpdatedAt(logType)));
}

/** 卡片上的最後更新時間文字 */
function updatedLabel(crawler) {
  const record = latestUpdatedAt[crawler.logType];
  if (!record || record.status === "loading") return "讀取最後更新時間…";
  if (record.status === "error") return "無法取得更新時間";
  return record.text ? `最後更新 ${record.text}` : "尚無更新紀錄";
}

/** 只要有任務正在執行，就禁止同時啟動其他同步 */
const anyRunning = () =>
  crawlers.some((crawler) => jobs[crawler.key].status === "running");

const router = useRouter();
let elapsedTimer = 0;

/** 建立單一任務的初始狀態 */
function createJob() {
  return {
    status: "idle",
    error: "",
    elapsedSeconds: 0,
  };
}

/** 將計時秒數顯示為整數 */
function formatSeconds(seconds) {
  return String(Math.floor(seconds));
}

/** 定義一般影城／電影爬蟲的結果欄位 */
const SUMMARY_FIELDS = [
  ["本次抓到", "fetchedCount"],
  ["新增", "createdCount"],
  ["略過", "skippedCount"],
  ["更新", "updatedCount"],
  ["納入審核", "reviewCount"],
  ["耗時", "elapsedSeconds", "秒"],
];

/** 將一般爬蟲回傳值轉成 Alert 統計列 */
function summaryRows(summary) {
  return SUMMARY_FIELDS.map(([label, key, unit = "筆"]) => ({
    label,
    value: `${summary?.[key] ?? 0} ${unit}`,
  }));
}

/** 將特典匯入回傳值整理成新增、上架、待審核等統計列 */
function benefitSummaryRows(summary, elapsedSeconds) {
  const created = summary?.created ?? 0;
  const autoApproved = summary?.autoApproved ?? 0;
  return [
    ["新增", created],
    ["直接上架", autoApproved],
    ["待審核", Math.max(created - autoApproved, 0)],
    ["重複略過", summary?.duplicated ?? 0],
    ["非特典", summary?.ignored ?? 0],
    ["耗時", elapsedSeconds, "秒"],
  ].map(([label, value, unit = "筆"]) => ({ label, value: `${value} ${unit}` }));
}

/** 將重新比對回傳值整理成統計列 */
function rematchSummaryRows(summary, elapsedSeconds) {
  return [
    ["檢查", summary?.checked ?? 0],
    ["找到電影", summary?.movieMatched ?? 0],
    ["直接上架", summary?.approved ?? 0],
    ["仍待審核", summary?.stillPending ?? 0],
    ["耗時", elapsedSeconds, "秒"],
  ].map(([label, value, unit = "筆"]) => ({ label, value: `${value} ${unit}` }));
}

/** 宣告 Apify 彈窗輸入欄位，避免個別流程重複定義表單 */
const APIFY_FIELDS = [
  { key: "pageUrl", label: "貼文網址", type: "text", inputmode: "url", value: "https://www.facebook.com/vieshow" },
  { key: "limit", label: "抓取篇數", type: "text", inputmode: "numeric", value: "30" },
  { key: "sinceDays", label: "抓取最近天數(含今天)", type: "text", inputmode: "numeric", value: "0", hint: "0 代表不限時間" },
];

/** 按提交模式配置 JSON 選檔或 Apify 輸入彈窗 */
const INPUT_OPTIONS = {
  json: {
    title: "上傳特典 JSON",
    text: "請選擇JSON檔案",
    file: { buttonText: "選擇檔案" },
    confirmText: "開始同步",
  },
  apify: {
    title: "同步特典",
    text: "請填寫貼文網址、抓取篇數與天數",
    fields: APIFY_FIELDS,
    confirmText: "開始同步",
  },
  rematch: {
    title: "重新比對特典？",
    text: "會把「找不到電影」的待審核特典再比對一次，資料齊全的會直接上架。",
    confirmText: "開始比對",
  },
};

/** 顯示確認視窗，依模式收集檔案或欄位值；取消則回傳 null */
async function confirmCrawler(crawler) {
  const options = INPUT_OPTIONS[crawler.mode] ?? {
    title: `確定要更新${crawler.subject}資料嗎？`,
    text: "同步期間請勿離開頁面。",
    confirmText: "立即同步",
  };
  const result = await openAppAlert({ variant: "confirm", ...options, cancelText: "取消" });
  if (!result.isConfirmed) return null;
  if (crawler.mode === "json") return result.file ? { file: result.file } : null;
  if (crawler.mode === "apify") return result.values;
  return {};
}

/** 任務開始時計時，並定期更新載入中的 Alert */
function startElapsed(job, crawler) {
  job.startedAt = Date.now();
  job.elapsedSeconds = 0;
  window.clearInterval(elapsedTimer);
  openAppAlert({
    variant: "loading",
    title: `正在${crawler.verb}${crawler.subject}資料`,
    text: "已進行 0 秒，請勿離開頁面。",
  });
  elapsedTimer = window.setInterval(() => {
    job.elapsedSeconds = (Date.now() - job.startedAt) / 1000;
    updateAppAlert({
      text: `已進行 ${formatSeconds(job.elapsedSeconds)} 秒，請勿離開頁面。`,
    });
  }, 200);
}

/** 停止計時，回傳此任務實際耗費秒數 */
function stopElapsed(job) {
  window.clearInterval(elapsedTimer);
  elapsedTimer = 0;
  job.elapsedSeconds = (Date.now() - job.startedAt) / 1000;
  return job.elapsedSeconds;
}

/** 離開頁面時清除計時器與全域 Alert，避免殘留 */
onMounted(loadAllLatestUpdatedAt);

onUnmounted(() => {
  window.clearInterval(elapsedTimer);
  closeAppAlert();
});

/** 依爬蟲模式送出請求：JSON 使用 FormData、Apify 帶參數、其餘使用 GET */
async function requestCrawler(crawler, payload) {
  if (crawler.mode === "json") {
    const form = new FormData();
    form.append("file", payload.file);
    form.append("autoApprove", "true");
    const { data } = await http.post(crawler.endpoint, form);
    return data;
  }

  if (crawler.mode === "apify") {
    const { data } = await http.post(crawler.endpoint, null, {
      params: {
        pageUrl: payload.pageUrl,
        limit: payload.limit,
        sinceDays: payload.sinceDays,
        autoApprove: true,
      },
    });
    return data;
  }

  if (crawler.mode === "rematch") {
    const { data } = await http.post(crawler.endpoint);
    return data;
  }

  const { data } = await http.get(crawler.endpoint);
  return data;
}

/** 卡片點擊入口；檢查是否忙碌、詢問確認，再執行同步 */
async function runCrawler(crawler) {
  if (anyRunning()) return;
  const payload = await confirmCrawler(crawler);
  if (payload === null) return;
  await syncCrawler(crawler, payload);
}

/** 執行同步並處理成功統計、頁面導向、失敗提示與重試 */
async function syncCrawler(crawler, payload) {
  if (anyRunning()) return;

  const job = jobs[crawler.key];
  job.status = "running";
  job.error = "";
  startElapsed(job, crawler);

  try {
    const data = await requestCrawler(crawler, payload);
    void loadLatestUpdatedAt(crawler.logType);
    const elapsedSeconds = Math.floor(stopElapsed(job));
    job.status = "success";
    const done = await openAppAlert({
      variant: "success",
      title: `${crawler.subject}${crawler.verb}完成`,
      rows:
        crawler.mode === "get"
          ? summaryRows(data)
          : crawler.mode === "rematch"
            ? rematchSummaryRows(data, elapsedSeconds)
            : benefitSummaryRows(data, elapsedSeconds),
      confirmText: crawler.resultLabel,
      extraText: crawler.reviewTo ? "前往爬蟲審核" : "",
      cancelText: "關閉",
    });
    if (done.isConfirmed) router.push(crawler.resultTo);
    else if (done.isExtra && crawler.reviewTo)
      router.push({ name: crawler.reviewTo });
  } catch (error) {
    void loadLatestUpdatedAt(crawler.logType);
    job.status = "error";
    job.error = authText(error) || apiMessage(error, "更新失敗，請檢查連線。");
    const seconds = formatSeconds(stopElapsed(job));
    const failed = await openAppAlert({
      variant: "error",
      title: `${crawler.verb}失敗`,
      text: `${job.error.replace(/。$/, "")}，耗時 ${seconds} 秒。`,
      confirmText: "重試",
      cancelText: "關閉",
    });
    if (failed.isConfirmed) await syncCrawler(crawler, payload);
  }
}
</script>

<template>
  <!-- 顯示頁面說明與爬蟲操作清單 -->
  <section>
    <PageIntro
      kicker="✦ DATA SYNC"
      lead="資料"
      mark="同步"
      description="更新影城、電影與特典資訊，執行期間請勿離開頁面。"
      :icon-left="movieIcon"
      :icon-right="clapperboardIcon"
      :icon-top="databaseIcon"
      theme="purple"
    />

    <div class="sync-panel">
      <h2 class="sync-panel-title">
        <Icon :icon="databaseIcon" aria-hidden="true" />
        同步項目
      </h2>

      <!-- 依 crawlers 設定產生卡片，並用 jobs 狀態控制按鈕顯示與禁用 -->
      <div class="ticket-grid">
        <TicketCard
          v-for="crawler in crawlers"
          :key="crawler.key"
          :color="crawler.color"
          :stamp="crawler.stamp"
          :title="crawler.title"
          :detail="crawler.description"
        >
          <p class="sync-updated">
            <Icon :icon="clockIcon" aria-hidden="true" />
            <span>{{ updatedLabel(crawler) }}</span>
          </p>
          <template #action>
            <button
              type="button"
              class="ticket-link"
              :disabled="anyRunning()"
              @click="runCrawler(crawler)"
            >
              <Icon :icon="crawler.icon" aria-hidden="true" />
              {{
                jobs[crawler.key].status === "running" ? "同步中…" : "立即同步"
              }}
            </button>
          </template>
        </TicketCard>
      </div>
    </div>
  </section>
</template>

<style scoped>
.sync-panel {
  padding: 1.15rem 1.35rem 1.35rem;
  border: 1.5px solid var(--c-border);
  border-radius: 22px;
  background: var(--c-card);
  box-shadow: var(--shadow-soft);
}

.sync-panel-title {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin: 0 0 1.5rem;
  color: var(--c-ink);
  font-size: var(--text-section-title);
  line-height: 1.3;
  font-weight: 900;
}

.sync-panel-title svg {
  color: #8d72ed;
  font-size: var(--text-card-title);
}

.ticket-grid :deep(.ticket-body h2) {
  font-size: var(--text-feature-title);
}

.sync-updated {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  margin: 0.65rem 0 0;
  color: var(--ticket-ink);
  font-size: var(--text-control);
  font-weight: 800;
  line-height: 1.4;
}

.sync-updated svg {
  flex-shrink: 0;
  font-size: 1.15em;
}

@media (max-width: 519px) {
  .sync-panel-title {
    font-size: var(--text-feature-title);
  }
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
