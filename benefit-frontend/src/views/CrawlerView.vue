<script setup>
import { reactive } from "vue";
import { Icon } from "@iconify/vue";
import movieIcon from "@iconify-icons/mdi/movie-open-outline";
import clapperboardIcon from "@iconify-icons/mdi/clapperboard-outline";
import databaseIcon from "@iconify-icons/mdi/database-sync-outline";
import filmIcon from "@iconify-icons/mdi/filmstrip";

import http from "@/plugins/axios";
import PageIntro from "@/components/PageIntro.vue";
import TicketCard from "@/components/TicketCard.vue";
import StatePanel from "@/components/StatePanel.vue";
import ResultBanner from "@/components/ResultBanner.vue";

const jobs = reactive({
  cinema: createJob(),
  movie: createJob(),
});

const crawlers = [
  {
    key: "cinema",
    title: "影城同步",
    description: "更新影城名稱、地址及位置資訊。",
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

const STATUS_LABEL = {
  idle: "尚未執行",
  running: "同步中",
  success: "已完成",
  error: "執行失敗",
};

const anyRunning = () =>
  crawlers.some((crawler) => jobs[crawler.key].status === "running");

function createJob() {
  return {
    status: "idle",
    error: "",
    items: [],
  };
}

function errorMessage(error) {
  return error.response?.data?.message || "更新失敗，請檢查連線。";
}

async function runCrawler(crawler) {
  if (anyRunning()) return;

  const confirmed = window.confirm(
    `確定要更新${crawler.stamp}資料嗎？`,
  );
  if (!confirmed) return;

  const job = jobs[crawler.key];
  job.status = "running";
  job.error = "";
  job.items = [];

  try {
    const { data } = await http.get(crawler.endpoint);
    job.items = Array.isArray(data) ? data : [];
    job.status = "success";
  } catch (error) {
    job.status = "error";
    job.error = errorMessage(error);
  }
}
</script>

<template>
  <section>
    <PageIntro kicker="✦ DATA SYNC" lead="資料" mark="同步" description="更新影城與電影資訊，執行期間請勿離開頁面。" :icon-left="movieIcon"
      :icon-right="clapperboardIcon" :icon-top="databaseIcon" theme="purple" />

    <div class="ticket-grid">
      <TicketCard v-for="crawler in crawlers" :key="crawler.key" color="purple" :stamp="crawler.stamp"
        :title="crawler.title" :detail="crawler.description">
        <template #action>
          <button type="button" class="ticket-link" :disabled="anyRunning()" @click="runCrawler(crawler)">
            <Icon :icon="databaseIcon" aria-hidden="true" />
            {{ jobs[crawler.key].status === "running" ? "同步中…" : "立即同步" }}
          </button>
          <span class="ticket-empty">
            {{ STATUS_LABEL[jobs[crawler.key].status] }}
          </span>
        </template>
      </TicketCard>
    </div>

    <template v-for="crawler in crawlers" :key="`${crawler.key}-status`">
      <StatePanel v-if="jobs[crawler.key].status === 'running'" variant="loading" color="purple"
        :message="`正在更新${crawler.stamp}資料，請勿離開頁面。`" :icon="filmIcon" />
      <StatePanel v-else-if="jobs[crawler.key].status === 'error'" variant="error" color="purple" title="更新失敗"
        :message="jobs[crawler.key].error" retry-label="重試" @retry="runCrawler(crawler)" />
      <ResultBanner v-else-if="jobs[crawler.key].status === 'success'" :icon="crawler.icon" :title="crawler.title"
        :count="jobs[crawler.key].items.length" unit-text="筆" empty-text="執行完成，無回傳資料。" :action-to="crawler.resultTo"
        :action-icon="crawler.icon" :action-label="crawler.resultLabel" theme="purple" />
    </template>
  </section>
</template>

<style scoped>
@media (min-width: 768px) {
  .ticket-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>