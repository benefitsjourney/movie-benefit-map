<script setup>
import { computed, onUnmounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import filmIcon from "@iconify-icons/mdi/filmstrip";
import calendarIcon from "@iconify-icons/mdi/calendar-blank-outline";
import eyeIcon from "@iconify-icons/mdi/eye-outline";
import pencilIcon from "@iconify-icons/mdi/pencil-outline";
import contentSaveIcon from "@iconify-icons/mdi/content-save-outline";
import closeIcon from "@iconify-icons/mdi/close";
import { openAppAlert } from "@/components/ui/Alert.vue";
import PageIntro from "@/components/layout/PageIntro.vue";
import DetailCard from "@/components/layout/DetailCard.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import AppButton from "@/components/ui/Button.vue";
import Switch from "@/components/ui/Switch.vue";
import MovieForm from "@/components/forms/MovieForm.vue";
import http from "@/plugins/axios";
import { dateTimeText, displayText, httpUrl, textOf } from "@/components/ui/DataTable.vue";

const route = useRoute();

const movieId = computed(() => {
  const id = Number(route.params.id);
  return Number.isInteger(id) && id > 0 ? id : null;
});

const loading = ref(false);
const loadFailed = ref(false);
const loadErrorMessage = ref("請確認連線後再試一次。");
const movie = ref(null);
const editing = ref(false);
const editSession = ref(0);
const submitting = ref(false);
const visibilityPending = ref(false);
// 路由快速切換時，只允許最後一次詳情請求更新畫面。
let requestSerial = 0;

const basicFields = computed(() => {
  const current = movie.value;
  if (!current) return [];
  return [
    { label: "編號", value: textOf(current.movieId) },
    { label: "來源編號", value: textOf(current.sourceId) },
    { label: "電影名稱", value: textOf(current.movieName) },
    { label: "上映日期", value: dateTimeText(current.releaseDate) },
  ];
});

const timeFields = computed(() => {
  const current = movie.value;
  if (!current) return [];
  return [
    { label: "建立時間", value: dateTimeText(current.createdAt) },
    { label: "更新時間", value: dateTimeText(current.updatedAt) },
  ];
});

const imageUrl = computed(() => textOf(movie.value?.imageUrl));
const imageHref = computed(() => httpUrl(imageUrl.value));
const sourceUrl = computed(() => textOf(movie.value?.sourceUrl));
const sourceHref = computed(() => httpUrl(sourceUrl.value));

function apiMessage(error, fallback) {
  const message = error?.response?.data?.message;
  return typeof message === "string" && message.trim() ? message.trim() : fallback;
}

function isUnauthorized(error) {
  return error?.response?.status === 401;
}

async function loadMovie() {
  const serial = ++requestSerial;
  editing.value = false;
  movie.value = null;

  if (movieId.value == null) {
    loading.value = false;
    loadFailed.value = true;
    loadErrorMessage.value = "網址中的電影編號不正確。";
    return;
  }

  loading.value = true;
  loadFailed.value = false;

  try {
    const { data } = await http.get(`/movies/${movieId.value}`);
    if (serial !== requestSerial) return;
    movie.value = data;
  } catch (error) {
    if (serial !== requestSerial) return;
    loadFailed.value = true;
    loadErrorMessage.value = apiMessage(error, "請確認連線後再試一次。");
  } finally {
    if (serial === requestSerial) loading.value = false;
  }
}

function startEdit() {
  if (!movie.value || submitting.value) return;
  // MovieForm 保存本地草稿；每次開始編輯都重掛以載入最新資料。
  editSession.value += 1;
  editing.value = true;
}

function cancelEdit() {
  if (submitting.value) return;
  editing.value = false;
}

async function saveMovie(payload) {
  if (submitting.value || movieId.value == null) return;

  submitting.value = true;

  try {
    const { data } = await http.patch(`/movies/${movieId.value}`, payload);
    movie.value = data;
    editing.value = false;
    await openAppAlert({
      variant: "success",
      title: "已更新電影",
      text: "電影資料已儲存。",
      confirmText: "關閉",
      cancelText: "",
    });
  } catch (error) {
    if (isUnauthorized(error)) {
      await openAppAlert({
        variant: "error",
        title: "無法儲存",
        text: "請登入管理員帳號",
        confirmText: "關閉",
        cancelText: "",
      });
      return;
    }

    const message = apiMessage(error, "修改電影失敗，請稍後再試。");
    await openAppAlert({
      variant: "error",
      title: "修改電影失敗",
      text: message,
      confirmText: "關閉",
      cancelText: "",
    });
  } finally {
    submitting.value = false;
  }
}

async function toggleVisible(next) {
  if (!movie.value || visibilityPending.value || movieId.value == null) return;

  const previous = movie.value.isVisible;
  movie.value = { ...movie.value, isVisible: next };
  visibilityPending.value = true;

  try {
    const { data } = await http.patch(`/movies/${movieId.value}`, { isVisible: next });
    if (!editing.value) {
      movie.value = data;
    } else {
      // 編輯中不可用 API 回應覆蓋 MovieForm 尚未送出的草稿。
      movie.value = { ...movie.value, isVisible: data?.isVisible, updatedAt: data?.updatedAt };
    }
  } catch (error) {
    movie.value = { ...movie.value, isVisible: previous };
    await openAppAlert({
      variant: "error",
      title: "無法更新顯示狀態",
      text: isUnauthorized(error) ? "請登入管理員帳號" : apiMessage(error, "請確認連線後再試一次。"),
      confirmText: "關閉",
      cancelText: "",
    });
  } finally {
    visibilityPending.value = false;
  }
}

watch(() => route.params.id, loadMovie, { immediate: true });

onUnmounted(() => {
  requestSerial += 1;
});
</script>

<template>
  <section>
    <PageIntro kicker="✦ MOVIE DETAIL" lead="電影" mark="詳情" description="查看及編輯電影資料、上映日期與圖片。" :icon-left="filmIcon"
      :icon-right="calendarIcon" :icon-top="eyeIcon" theme="purple" />

    <StatePanel v-if="loading" variant="loading" message="正在載入電影資料…" :icon="filmIcon" color="purple" />
    <StatePanel v-else-if="loadFailed" variant="error" title="目前無法取得電影資料" :message="loadErrorMessage" color="purple"
      @retry="loadMovie" />

    <DetailCard v-else-if="movie" :title="displayText(movie.movieName)" :title-icon="filmIcon"
      :back-to="{ name: 'admin-movies' }" back-label="返回電影一覽">
      <template #actions>
        <template v-if="editing">
          <AppButton type="button" variant="outline" color="purple" :icon="closeIcon" :disabled="submitting"
            @click="cancelEdit">
            取消
          </AppButton>
          <AppButton type="submit" form="movie-edit-form" color="purple" :icon="contentSaveIcon"
            :disabled="submitting">
            {{ submitting ? "儲存中…" : "儲存" }}
          </AppButton>
        </template>
        <AppButton v-else type="button" color="purple" :icon="pencilIcon" @click="startEdit">
          編輯資料
        </AppButton>
      </template>

      <div v-if="editing">
        <MovieForm :key="editSession" mode="edit" plain hide-submit form-id="movie-edit-form" :movie="movie"
          :show-visibility="false" :submitting="submitting" @submit="saveMovie" />
      </div>

      <template v-if="!editing">
        <section class="detail-section">
          <h3>基本資料</h3>
          <dl>
            <div v-for="field in basicFields" :key="field.label">
              <dt>{{ field.label }}</dt>
              <dd>{{ displayText(field.value) }}</dd>
            </div>
          </dl>
        </section>

        <section class="detail-section">
          <h3>圖片</h3>
          <dl>
            <div>
              <dt>電影圖片</dt>
              <dd>
                <a v-if="imageHref" class="detail-link" :href="imageHref" target="_blank" rel="noopener noreferrer">
                  {{ imageUrl }}
                </a>
                <template v-else>{{ displayText(imageUrl) }}</template>
                <img v-if="imageHref" class="detail-poster" :src="imageHref" alt="電影圖片" />
              </dd>
            </div>
          </dl>
        </section>
      </template>

      <section class="detail-section" :class="{ 'is-after-edit': editing }">
        <h3>來源</h3>
        <dl>
          <div>
            <dt>來源網址</dt>
            <dd>
              <a v-if="sourceHref" class="detail-link" :href="sourceHref" target="_blank" rel="noopener noreferrer">
                {{ sourceUrl }}
              </a>
              <template v-else>{{ displayText(sourceUrl) }}</template>
            </dd>
          </div>
        </dl>
      </section>

      <section class="detail-section">
        <h3>狀態與時間</h3>
        <dl>
          <div>
            <dt>前台顯示</dt>
            <dd>
              <Switch :model-value="movie.isVisible === true" :disabled="visibilityPending"
                @update:model-value="toggleVisible">
                {{ movie.isVisible === true ? "顯示" : "隱藏" }}
              </Switch>
            </dd>
          </div>
          <div v-for="field in timeFields" :key="field.label">
            <dt>{{ field.label }}</dt>
            <dd>{{ displayText(field.value) }}</dd>
          </div>
        </dl>
      </section>
    </DetailCard>
  </section>
</template>

<style scoped>
.detail-poster {
  display: block;
  max-width: min(100%, 16rem);
  max-height: 22rem;
  margin-top: 0.75rem;
  border: 1.5px solid var(--c-border);
  border-radius: 16px;
  background: #f8f4ff;
  object-fit: contain;
}
</style>
