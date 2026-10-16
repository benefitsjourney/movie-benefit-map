<script setup>
import { onUnmounted, reactive } from "vue";
import { useRouter } from "vue-router";
import { Icon } from "@iconify/vue";
import movieIcon from "@iconify-icons/mdi/movie-open-outline";
import clapperboardIcon from "@iconify-icons/mdi/clapperboard-outline";
import databaseIcon from "@iconify-icons/mdi/database-sync-outline";

import http from "@/plugins/axios";
import PageIntro from "@/components/layout/PageIntro.vue";
import TicketCard from "@/components/ui/TicketCard.vue";
import {
  closeAppAlert,
  openAppAlert,
  updateAppAlert,
} from "@/components/ui/Alert.vue";

const jobs = reactive({
  cinema: createJob(),
  movie: createJob(),
});

const crawlers = [
  {
    key: "cinema",
    title: "影城同步",
    description: "更新影城名稱、地址及位置等資訊。",
    endpoint: "/crawler/cinemas",
    stamp: "影城",
    icon: movieIcon,
    resultTo: "/cinemas",
    resultLabel: "查看影城",
  },
  {
    key: "movie",
    title: "電影同步",
    description: "更新最新、上映中及即將上映的電影。",
    endpoint: "/crawler/cinemas/movies",
    stamp: "電影",
    icon: clapperboardIcon,
    resultTo: "/movies",
    resultLabel: "查看電影",
  },
];

const anyRunning = () =>
  crawlers.some((crawler) => jobs[crawler.key].status === "running");

const router = useRouter();
let elapsedTimer = 0;

function createJob() {
  return {
    status: "idle",
    error: "",
    items: [],
    elapsedSeconds: 0,
  };
}

function errorMessage(error) {
  return error.response?.data?.message || "更新失敗，請檢查連線。";
}

function formatSeconds(seconds) {
  return String(Math.floor(seconds));
}

function startElapsed(job, crawler) {
  job.startedAt = Date.now();
  job.elapsedSeconds = 0;
  window.clearInterval(elapsedTimer);
  openAppAlert({
    variant: "loading",
    title: `正在更新${crawler.stamp}資料`,
    text: "已進行 0 秒，請勿離開頁面。",
  });
  elapsedTimer = window.setInterval(() => {
    job.elapsedSeconds = (Date.now() - job.startedAt) / 1000;
    updateAppAlert({
      text: `已進行 ${formatSeconds(job.elapsedSeconds)} 秒，請勿離開頁面。`,
    });
  }, 200);
}

function stopElapsed(job) {
  window.clearInterval(elapsedTimer);
  elapsedTimer = 0;
  job.elapsedSeconds = (Date.now() - job.startedAt) / 1000;
  return job.elapsedSeconds;
}

onUnmounted(() => {
  window.clearInterval(elapsedTimer);
  closeAppAlert();
});

async function runCrawler(crawler) {
  if (anyRunning()) return;

  const result = await openAppAlert({
    variant: "confirm",
    title: `確定要更新${crawler.stamp}資料嗎？`,
    text: "同步期間請勿離開頁面。",
    confirmText: "立即同步",
    cancelText: "取消",
  });
  if (!result.isConfirmed) return;

  await syncCrawler(crawler);
}

async function syncCrawler(crawler) {
  if (anyRunning()) return;

  const job = jobs[crawler.key];
  job.status = "running";
  job.error = "";
  job.items = [];
  startElapsed(job, crawler);

  try {
    const { data } = await http.get(crawler.endpoint);
    job.items = Array.isArray(data) ? data : [];
    job.status = "success";
    const seconds = formatSeconds(stopElapsed(job));
    const countText = job.items.length
      ? `共取得 ${job.items.length} 筆，耗時 ${seconds} 秒。`
      : `執行完成，無回傳資料。耗時 ${seconds} 秒。`;
    const done = await openAppAlert({
      variant: "success",
      title: `${crawler.stamp}更新完成`,
      text: countText,
      confirmText: crawler.resultLabel,
      cancelText: "關閉",
    });
    if (done.isConfirmed) router.push(crawler.resultTo);
  } catch (error) {
    job.status = "error";
    job.error = errorMessage(error);
    const seconds = formatSeconds(stopElapsed(job));
    const failed = await openAppAlert({
      variant: "error",
      title: "更新失敗",
      text: `${job.error.replace(/。$/, "")}，耗時 ${seconds} 秒。`,
      confirmText: "重試",
      cancelText: "關閉",
    });
    if (failed.isConfirmed) syncCrawler(crawler);
  }
}
</script>

<template>
  <section>
    <PageIntro kicker="✦ DATA SYNC" lead="資料" mark="同步" description="更新影城與電影資訊，執行期間請勿離開頁面。" :icon-left="movieIcon"
      :icon-right="clapperboardIcon" :icon-top="databaseIcon" theme="purple" />

    <div class="sync-panel">
      <h2 class="sync-panel-title">
        <Icon :icon="databaseIcon" aria-hidden="true" />
        同步項目
      </h2>

      <div class="ticket-grid">
        <TicketCard v-for="crawler in crawlers" :key="crawler.key" color="purple" :stamp="crawler.stamp"
          :title="crawler.title" :detail="crawler.description">
          <template #action>
            <button type="button" class="ticket-link" :disabled="anyRunning()" @click="runCrawler(crawler)">
              <Icon :icon="databaseIcon" aria-hidden="true" />
              {{ jobs[crawler.key].status === "running" ? "同步中…" : "立即同步" }}
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
</style>