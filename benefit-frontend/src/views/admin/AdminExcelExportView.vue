<script setup>
import { ref } from "vue";
import { Icon } from "@iconify/vue";
import excelIcon from "@iconify-icons/mdi/microsoft-excel";
import downloadIcon from "@iconify-icons/mdi/download";
import movieIcon from "@iconify-icons/mdi/movie-open-outline";
import filmIcon from "@iconify-icons/mdi/filmstrip";
import historyIcon from "@iconify-icons/mdi/history";
import reviewIcon from "@iconify-icons/mdi/clipboard-check-outline";

import http from "@/plugins/axios";
import { apiMessage, authText } from "@/utils/apiError";
import PageIntro from "@/components/layout/PageIntro.vue";
import TicketCard from "@/components/ui/TicketCard.vue";
import { openAppAlert } from "@/components/ui/Alert.vue";

const EXPORTS = [
  {
    type: "CINEMA",
    title: "影城資料",
    description: "匯出全部影城資料。",
    stamp: "影城",
    color: "purple",
    icon: movieIcon,
    fileName: "cinema.xlsx",
  },
  {
    type: "MOVIE",
    title: "電影資料",
    description: "匯出全部電影資料。",
    stamp: "電影",
    color: "purple",
    icon: filmIcon,
    fileName: "movie.xlsx",
  },
  {
    type: "CRAWLER_LOG",
    title: "爬蟲紀錄",
    description: "匯出全部爬蟲執行紀錄。",
    stamp: "紀錄",
    color: "pink",
    icon: historyIcon,
    fileName: "crawler_log.xlsx",
  },
  {
    type: "CINEMA_MOVIE_REVIEW",
    title: "爬蟲審核",
    description: "匯出全部爬蟲審核資料。",
    stamp: "審核",
    color: "pink",
    icon: reviewIcon,
    fileName: "cinema_movie_review.xlsx",
  },
];

const downloading = ref("");

async function readError(error) {
  const data = error?.response?.data;
  if (data instanceof Blob) {
    try {
      const text = await data.text();
      const json = JSON.parse(text);
      if (typeof json?.message === "string" && json.message.trim()) {
        return json.message.trim();
      }
    } catch {
      // 錯誤本文不是 JSON 時，改用狀態碼對應的提示。
    }
  }
  return (
    authText(error) || apiMessage(error, "下載失敗，請確認連線後再試一次。")
  );
}

function downloadName(response, fallback) {
  const header = response.headers?.["content-disposition"] ?? "";
  const matched = /filename\*?=(?:UTF-8''|"?)([^";]+)"?/i.exec(header);
  if (!matched?.[1]) return fallback;
  try {
    return decodeURIComponent(matched[1].replaceAll('"', ""));
  } catch {
    return fallback;
  }
}

async function downloadExcel(item) {
  if (downloading.value) return;
  downloading.value = item.type;
  try {
    const response = await http.get("/cinema-movie/export", {
      params: { type: item.type },
      responseType: "blob",
    });
    const blob = new Blob([response.data], {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = downloadName(response, item.fileName);
    document.body.append(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  } catch (error) {
    await openAppAlert({
      variant: "error",
      title: "下載失敗",
      text: await readError(error),
      confirmText: "關閉",
      cancelText: "",
    });
  } finally {
    downloading.value = "";
  }
}
</script>

<template>
  <section>
    <PageIntro
      kicker="✦ EXCEL"
      lead="Excel"
      mark="下載"
      description="匯出資料紀錄，檔案會直接下載到本機"
      :icon-left="movieIcon"
      :icon-right="filmIcon"
      :icon-top="excelIcon"
      theme="purple"
    />

    <div class="export-panel">
      <h2 class="export-panel-title">
        <Icon :icon="excelIcon" aria-hidden="true" />
        可下載項目
      </h2>

      <div class="ticket-grid">
        <TicketCard
          v-for="item in EXPORTS"
          :key="item.type"
          :color="item.color"
          :stamp="item.stamp"
          :title="item.title"
          :detail="item.description"
        >
          <p class="export-file">{{ item.fileName }}</p>
          <template #action>
            <button
              type="button"
              class="ticket-link"
              :disabled="Boolean(downloading)"
              @click="downloadExcel(item)"
            >
              <Icon :icon="downloadIcon" aria-hidden="true" />
              {{ downloading === item.type ? "下載中…" : "下載 Excel" }}
            </button>
          </template>
        </TicketCard>
      </div>
    </div>
  </section>
</template>

<style scoped>
.export-panel {
  padding: 1.15rem 1.35rem 1.35rem;
  border: 1.5px solid var(--c-border);
  border-radius: 22px;
  background: var(--c-card);
  box-shadow: var(--shadow-soft);
}

.export-panel-title {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin: 0 0 1.5rem;
  color: var(--c-ink);
  font-size: var(--text-section-title);
  line-height: 1.3;
  font-weight: 900;
}

.export-panel-title svg {
  color: #8d72ed;
  font-size: var(--text-card-title);
}

.ticket-grid :deep(.ticket-body h2) {
  font-size: var(--text-feature-title);
}

.export-file {
  margin: 0.65rem 0 0;
  color: var(--ticket-ink);
  font-size: var(--text-control);
  font-weight: 800;
  line-height: 1.4;
}

@media (max-width: 519px) {
  .export-panel-title {
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
