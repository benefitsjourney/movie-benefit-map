<script setup>
import { computed, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Icon } from "@iconify/vue";
import clipboardIcon from "@iconify-icons/mdi/clipboard-check-outline";
import sourceBranchIcon from "@iconify-icons/mdi/source-branch";
import eyeIcon from "@iconify-icons/mdi/eye-outline";
import checkIcon from "@iconify-icons/mdi/check";
import closeIcon from "@iconify-icons/mdi/close";
import refreshIcon from "@iconify-icons/mdi/refresh";
import deleteIcon from "@iconify-icons/mdi/trash-can-outline";
import http from "@/plugins/axios";
import { countyLabel } from "@/constants/county";
import { openAppAlert } from "@/components/ui/Alert.vue";
import PageIntro from "@/components/layout/PageIntro.vue";
import BackLink from "@/components/layout/BackLink.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import AppButton from "@/components/ui/Button.vue";
import { dateTimeText, displayText, textOf } from "@/components/ui/DataTable.vue";

const route = useRoute();
const router = useRouter();
const CRAWLER_TYPE_OPTIONS = [
  { value: "CINEMA", label: "影城" },
  { value: "MOVIE", label: "電影" },
  { value: "BENEFIT", label: "優惠" },
];
const REVIEW_STATUS_OPTIONS = [
  { value: "PENDING", label: "待審核" },
  { value: "APPROVED", label: "已接受" },
  { value: "REJECTED", label: "已拒絕" },
];
const REVIEW_FIELD_LABELS = {
  cinema_name: "影城名稱",
  cinema_address: "地址",
  county: "縣市",
  latitude: "緯度",
  longitude: "經度",
  official_website_url: "官網",
  is_active: "營業狀態",
  is_visible: "前台顯示",
  movie_name: "電影名稱",
  image_url: "電影圖片",
  release_date: "上映日期",
};
const ENTITY_ROUTES = {
  CINEMA: "admin-cinema-detail",
  MOVIE: "admin-movie-detail",
};

function crawlerTypeLabel(value) {
  return CRAWLER_TYPE_OPTIONS.find((option) => option.value === value)?.label ?? value ?? "";
}

function reviewStatusLabel(value) {
  return REVIEW_STATUS_OPTIONS.find((option) => option.value === value)?.label ?? value ?? "";
}

function reviewFieldLabel(value) {
  return REVIEW_FIELD_LABELS[value] ?? value ?? "";
}

function reviewValueText(value, row) {
  if (value == null || value === "") return "";
  if (row?.fieldName === "county") return countyLabel(value);
  if (row?.fieldName === "is_active") {
    if (value === "true") return "營業中";
    if (value === "false") return "已歇業";
  }
  if (row?.fieldName === "is_visible") {
    if (value === "true") return "顯示";
    if (value === "false") return "隱藏";
  }
  return dateTimeText(value);
}

function reviewEntityRoute(row) {
  const name = ENTITY_ROUTES[row?.crawlerType];
  const id = Number(row?.entityId);
  if (!name || !Number.isInteger(id) || id <= 0) return null;
  return { name, params: { id } };
}

const reviewId = computed(() => {
  const id = Number(route.params.id);
  return Number.isInteger(id) && id > 0 ? id : null;
});

const loading = ref(false);
const loadFailed = ref(false);
const loadErrorMessage = ref("請確認連線後再試一次。");
const review = ref(null);
const acting = ref(false);
// 路由快速切換時，只允許最後一次詳情請求更新畫面。
let requestSerial = 0;

const entityLink = computed(() => reviewEntityRoute(review.value));

const subjectFields = computed(() => {
  const current = review.value;
  if (!current) return [];
  return [
    { label: "編號", value: textOf(current.crawlerReviewId) },
    { label: "類型", value: crawlerTypeLabel(current.crawlerType) },
    { label: "欄位", value: reviewFieldLabel(current.fieldName) },
    { label: "審查狀態", value: reviewStatusLabel(current.reviewStatus) },
  ];
});

const timeFields = computed(() => {
  const current = review.value;
  if (!current) return [];
  return [
    { label: "爬取時間", value: dateTimeText(current.crawledAt) },
    { label: "審核時間", value: dateTimeText(current.reviewedAt) },
    { label: "解鎖時間", value: dateTimeText(current.unlockedAt) },
    { label: "建立時間", value: dateTimeText(current.createdAt) },
    { label: "更新時間", value: dateTimeText(current.updatedAt) },
  ];
});

function apiMessage(error, fallback) {
  const message = error?.response?.data?.message;
  return typeof message === "string" && message.trim() ? message.trim() : fallback;
}

async function loadReview() {
  const serial = ++requestSerial;
  review.value = null;

  if (reviewId.value == null) {
    loading.value = false;
    loadFailed.value = true;
    loadErrorMessage.value = "網址中的審核編號不正確。";
    return;
  }

  loading.value = true;
  loadFailed.value = false;

  try {
    const { data } = await http.get(`/crawler-reviews/${reviewId.value}`);
    if (serial !== requestSerial) return;
    review.value = data;
  } catch (error) {
    if (serial !== requestSerial) return;
    loadFailed.value = true;
    loadErrorMessage.value = apiMessage(error, "請確認連線後再試一次。");
  } finally {
    if (serial === requestSerial) loading.value = false;
  }
}

async function changeStatus(reviewStatus) {
  const current = review.value;
  if (!current || acting.value) return;

  const prompts = {
    APPROVED: {
      title: "確定接受這筆差異？",
      text: "接受後會把爬蟲值寫入正式資料。",
      confirmText: "接受",
      successTitle: "已接受",
      successText: "爬蟲值已寫入正式資料。",
      errorTitle: "接受失敗",
    },
    REJECTED: {
      title: "確定拒絕這筆差異？",
      text: "拒絕後正式資料會維持原值。",
      confirmText: "拒絕",
      successTitle: "已拒絕",
      successText: "正式資料維持原值。",
      errorTitle: "拒絕失敗",
    },
    PENDING: {
      title: "確定要重新審核？",
      text: "會另建一筆待審核，原來的紀錄會保留。",
      confirmText: "重新審核",
      successTitle: "已建立待審核",
      successText: "新的待審核紀錄已建立，原紀錄仍保留。",
      errorTitle: "重新審核失敗",
    },
  };
  const prompt = prompts[reviewStatus];
  const confirmed = await openAppAlert({
    variant: "confirm",
    title: prompt.title,
    text: `${reviewFieldLabel(current.fieldName)}：${reviewValueText(current.previousValue, current) || "（空）"} → ${reviewValueText(current.crawlerValue, current) || "（空）"}。${prompt.text}`,
    confirmText: prompt.confirmText,
    cancelText: "取消",
  });
  if (!confirmed.isConfirmed) return;

  acting.value = true;
  try {
    const { data } = await http.patch(`/crawler-reviews/${current.crawlerReviewId}`, { reviewStatus });
    if (reviewStatus !== "PENDING" && data && typeof data === "object" && data.crawlerReviewId != null) {
      review.value = data;
    }
    await openAppAlert({
      variant: "success",
      title: prompt.successTitle,
      text: prompt.successText,
      confirmText: "關閉",
      cancelText: "",
    });
    // 重新審核會另建一筆紀錄，回應不一定是目前這筆，因此改為重新讀取。
    if (reviewStatus === "PENDING") await loadReview();
  } catch (requestError) {
    await openAppAlert({
      variant: "error",
      title: prompt.errorTitle,
      text: apiMessage(requestError, "請確認連線後再試一次。"),
      confirmText: "關閉",
      cancelText: "",
    });
  } finally {
    acting.value = false;
  }
}

async function removeReview() {
  const current = review.value;
  if (!current || acting.value) return;

  const confirmed = await openAppAlert({
    variant: "confirm",
    title: "確定刪除這筆審核？",
    text: "刪除後無法從列表恢復這筆紀錄。",
    confirmText: "刪除",
    cancelText: "取消",
  });
  if (!confirmed.isConfirmed) return;

  acting.value = true;
  try {
    await http.delete(`/crawler-reviews/${current.crawlerReviewId}`);
    await openAppAlert({
      variant: "success",
      title: "已刪除審核",
      text: "這筆審核紀錄已移除。",
      confirmText: "關閉",
      cancelText: "",
    });
    await router.push({ name: "admin-crawler-reviews" });
  } catch (requestError) {
    await openAppAlert({
      variant: "error",
      title: "刪除失敗",
      text: apiMessage(requestError, "請確認連線後再試一次。"),
      confirmText: "關閉",
      cancelText: "",
    });
  } finally {
    acting.value = false;
  }
}

watch(() => route.params.id, loadReview, { immediate: true });

onUnmounted(() => {
  requestSerial += 1;
});
</script>

<template>
  <section>
    <PageIntro kicker="✦ REVIEW DETAIL" lead="審核" mark="詳情" description="核對這筆爬蟲差異。接受會寫入正式資料，拒絕則保留原值。"
      :icon-left="clipboardIcon" :icon-right="sourceBranchIcon" :icon-top="eyeIcon" theme="purple" />

    <StatePanel v-if="loading" variant="loading" message="正在載入爬蟲審核…" :icon="clipboardIcon" color="purple" />
    <StatePanel v-else-if="loadFailed" variant="error" title="目前無法取得爬蟲審核" :message="loadErrorMessage" color="purple"
      @retry="loadReview" />

    <article v-else-if="review" class="review-detail">
      <div class="detail-toolbar">
        <BackLink :to="{ name: 'admin-crawler-reviews' }">
          返回審核列表
        </BackLink>
      </div>

      <div class="detail-head">
        <h2>
          <Icon :icon="clipboardIcon" aria-hidden="true" />
          {{ displayText(reviewFieldLabel(review.fieldName)) }}
        </h2>
        <div class="detail-actions">
          <template v-if="review.reviewStatus === 'PENDING'">
            <AppButton type="button" color="purple" :icon="checkIcon" :disabled="acting"
              @click="changeStatus('APPROVED')">
              接受
            </AppButton>
            <AppButton type="button" color="purple" variant="outline" :icon="closeIcon" :disabled="acting"
              @click="changeStatus('REJECTED')">
              拒絕
            </AppButton>
          </template>
          <AppButton v-else type="button" color="purple" variant="outline" :icon="refreshIcon" :disabled="acting"
            @click="changeStatus('PENDING')">
            重新審核
          </AppButton>
          <AppButton type="button" color="purple" variant="outline" :icon="deleteIcon" :disabled="acting"
            @click="removeReview">
            刪除
          </AppButton>
        </div>
      </div>

      <section class="detail-section">
        <h3>審核對象</h3>
        <dl>
          <div v-for="field in subjectFields" :key="field.label">
            <dt>{{ field.label }}</dt>
            <dd>{{ displayText(field.value) }}</dd>
          </div>
          <div>
            <dt>資料編號</dt>
            <dd>
              <RouterLink v-if="entityLink" class="detail-link" :to="entityLink">
                {{ review.entityId }}
              </RouterLink>
              <template v-else>{{ displayText(review.entityId) }}</template>
            </dd>
          </div>
        </dl>
      </section>

      <section class="detail-section">
        <h3>差異內容</h3>
        <dl>
          <div>
            <dt>爬蟲值</dt>
            <dd>{{ displayText(reviewValueText(review.crawlerValue, review)) }}</dd>
          </div>
          <div>
            <dt>原值</dt>
            <dd>{{ displayText(reviewValueText(review.previousValue, review)) }}</dd>
          </div>
        </dl>
      </section>

      <section class="detail-section">
        <h3>時間</h3>
        <dl>
          <div v-for="field in timeFields" :key="field.label">
            <dt>{{ field.label }}</dt>
            <dd>{{ displayText(field.value) }}</dd>
          </div>
        </dl>
      </section>
    </article>
  </section>
</template>

<style scoped>
.review-detail {
  margin-bottom: 1rem;
  padding: 0 1.35rem 1.35rem;
  border: 1.5px solid var(--c-border);
  border-radius: 22px;
  background: var(--c-card);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
}

.detail-toolbar {
  margin: 0 -1.35rem 1.15rem;
  padding: 0.85rem 1.35rem;
  border-bottom: 1.5px solid #e4d8fb;
  background: #f3ecff;
}

.detail-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem 1rem;
  margin-bottom: 1rem;
}

.detail-head h2 {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
}

.detail-head h2 svg {
  flex: none;
  color: #8d72ed;
  font-size: var(--text-card-title);
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin-left: auto;
}

.detail-section+.detail-section {
  margin-top: 1.2rem;
  padding-top: 1.2rem;
  border-top: 1.5px solid var(--c-border);
}

.detail-section h3 {
  margin: 0 0 0.85rem;
  font-size: var(--text-card-title);
  line-height: 1.3;
  font-weight: 900;
}

.detail-section dl {
  display: grid;
  gap: 0.85rem 1.25rem;
  margin: 0;
}

.detail-section dl>div {
  display: grid;
  grid-template-columns: 7.5rem minmax(0, 1fr);
  gap: 0.35rem 1rem;
  align-items: start;
}

.detail-section dt {
  margin: 0;
  color: var(--c-muted);
  font-size: var(--text-control);
  font-weight: 700;
}

.detail-section dd {
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
  font-size: var(--text-body);
  line-height: var(--leading-body);
}

.detail-link {
  color: #8d72ed;
  font-weight: 700;
  overflow-wrap: anywhere;
  text-underline-offset: 0.18em;
}

.detail-link:hover {
  color: #5c4696;
}

.detail-link:focus-visible {
  border-radius: 4px;
  outline: 2px solid #8d72ed;
  outline-offset: 3px;
}

@media (max-width: 719px) {
  .detail-actions {
    flex-basis: 100%;
    margin-left: 0;
  }

  .detail-actions :deep(.app-button) {
    width: 100%;
  }

  .detail-section dl>div {
    grid-template-columns: 1fr;
    gap: 0.15rem;
  }
}
</style>
