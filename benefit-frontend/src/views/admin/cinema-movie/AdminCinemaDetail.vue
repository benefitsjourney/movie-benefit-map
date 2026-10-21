<script setup>
import { computed, onUnmounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import movieIcon from "@iconify-icons/mdi/movie-open-outline";
import pinIcon from "@iconify-icons/mdi/map-marker-outline";
import eyeIcon from "@iconify-icons/mdi/eye-outline";
import pencilIcon from "@iconify-icons/mdi/pencil-outline";
import contentSaveIcon from "@iconify-icons/mdi/content-save-outline";
import closeIcon from "@iconify-icons/mdi/close";
import { openAppAlert } from "@/components/ui/Alert.vue";
import { countyLabel } from "@/constants/county";
import PageIntro from "@/components/layout/PageIntro.vue";
import DetailCard from "@/components/layout/DetailCard.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import AppButton from "@/components/ui/Button.vue";
import Switch from "@/components/ui/Switch.vue";
import CinemaForm from "@/components/forms/CinemaForm.vue";
import http from "@/plugins/axios";
import { dateTimeText, displayText, httpUrl, textOf } from "@/components/ui/DataTable.vue";

const route = useRoute();

const cinemaId = computed(() => {
  const id = Number(route.params.id);
  return Number.isInteger(id) && id > 0 ? id : null;
});

const loading = ref(false);
const loadFailed = ref(false);
const loadErrorMessage = ref("請確認連線後再試一次。");
const cinema = ref(null);
const editing = ref(false);
const editSession = ref(0);
const submitting = ref(false);
const pendingFlags = ref({});
// 路由快速切換時，只允許最後一次詳情請求更新畫面。
let requestSerial = 0;

const basicFields = computed(() => {
  const current = cinema.value;
  if (!current) return [];
  return [
    { label: "編號", value: textOf(current.cinemaId) },
    { label: "來源編號", value: textOf(current.sourceId) },
    { label: "影城名稱", value: textOf(current.cinemaName) },
    { label: "縣市", value: current.county ? countyLabel(current.county) : "" },
  ];
});

const locationFields = computed(() => {
  const current = cinema.value;
  if (!current) return [];
  return [
    { label: "地址", value: textOf(current.cinemaAddress) },
    { label: "緯度", value: coordText(current.latitude) },
    { label: "經度", value: coordText(current.longitude) },
  ];
});

const timeFields = computed(() => {
  const current = cinema.value;
  if (!current) return [];
  return [
    { label: "建立時間", value: dateTimeText(current.createdAt) },
    { label: "更新時間", value: dateTimeText(current.updatedAt) },
  ];
});

const websiteUrl = computed(() => textOf(cinema.value?.officialWebsiteUrl));
const websiteHref = computed(() => httpUrl(websiteUrl.value));
const sourceUrl = computed(() => textOf(cinema.value?.sourceUrl));
const sourceHref = computed(() => httpUrl(sourceUrl.value));

function coordText(value) {
  if (value == null || value === "") return "";
  const number = Number(value);
  return Number.isFinite(number) ? number.toFixed(6) : String(value);
}

function apiMessage(error, fallback) {
  const message = error?.response?.data?.message;
  return typeof message === "string" && message.trim() ? message.trim() : fallback;
}

function isUnauthorized(error) {
  return error?.response?.status === 401;
}

async function loadCinema() {
  const serial = ++requestSerial;
  editing.value = false;
  cinema.value = null;

  if (cinemaId.value == null) {
    loading.value = false;
    loadFailed.value = true;
    loadErrorMessage.value = "網址中的影城編號不正確。";
    return;
  }

  loading.value = true;
  loadFailed.value = false;

  try {
    const { data } = await http.get(`/cinemas/${cinemaId.value}`);
    if (serial !== requestSerial) return;
    cinema.value = data;
  } catch (error) {
    if (serial !== requestSerial) return;
    loadFailed.value = true;
    loadErrorMessage.value = apiMessage(error, "請確認連線後再試一次。");
  } finally {
    if (serial === requestSerial) loading.value = false;
  }
}

function startEdit() {
  if (!cinema.value || submitting.value) return;
  // CinemaForm 保存本地草稿；每次開始編輯都重掛以載入最新資料。
  editSession.value += 1;
  editing.value = true;
}

function cancelEdit() {
  if (submitting.value) return;
  editing.value = false;
}

async function saveCinema(payload) {
  if (submitting.value || cinemaId.value == null) return;

  submitting.value = true;

  try {
    const { data } = await http.patch(`/cinemas/${cinemaId.value}`, payload);
    cinema.value = data;
    editing.value = false;
    await openAppAlert({
      variant: "success",
      title: "已更新影城",
      text: "影城資料已儲存。",
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

    const message = apiMessage(error, "修改影城失敗，請稍後再試。");
    await openAppAlert({
      variant: "error",
      title: "修改影城失敗",
      text: message,
      confirmText: "關閉",
      cancelText: "",
    });
  } finally {
    submitting.value = false;
  }
}

async function toggleFlag(field, next) {
  if (!cinema.value || pendingFlags.value[field] || cinemaId.value == null) return;

  const previous = cinema.value[field];
  cinema.value = { ...cinema.value, [field]: next };
  pendingFlags.value = { ...pendingFlags.value, [field]: true };

  try {
    const { data } = await http.patch(`/cinemas/${cinemaId.value}`, { [field]: next });
    if (!editing.value) {
      cinema.value = data;
    } else {
      // 編輯中不可用 API 回應覆蓋 CinemaForm 尚未送出的草稿。
      cinema.value = { ...cinema.value, [field]: data?.[field], updatedAt: data?.updatedAt };
    }
  } catch (error) {
    cinema.value = { ...cinema.value, [field]: previous };
    await openAppAlert({
      variant: "error",
      title: field === "isActive" ? "無法更新營業狀態" : "無法更新顯示狀態",
      text: isUnauthorized(error) ? "請登入管理員帳號" : apiMessage(error, "請確認連線後再試一次。"),
      confirmText: "關閉",
      cancelText: "",
    });
  } finally {
    const nextPending = { ...pendingFlags.value };
    delete nextPending[field];
    pendingFlags.value = nextPending;
  }
}

watch(() => route.params.id, loadCinema, { immediate: true });

onUnmounted(() => {
  requestSerial += 1;
});
</script>

<template>
  <section>
    <PageIntro kicker="✦ CINEMA DETAIL" lead="影城" mark="詳情" description="查看及編輯影城資料、地址與顯示狀態。" :icon-left="movieIcon"
      :icon-right="pinIcon" :icon-top="eyeIcon" theme="purple" />

    <StatePanel v-if="loading" variant="loading" message="正在載入影城資料…" :icon="movieIcon" color="purple" />
    <StatePanel v-else-if="loadFailed" variant="error" title="目前無法取得影城資料" :message="loadErrorMessage" color="purple"
      @retry="loadCinema" />

    <DetailCard v-else-if="cinema" :title="displayText(cinema.cinemaName)" :title-icon="movieIcon"
      :back-to="{ name: 'admin-cinemas' }" back-label="返回影城一覽" theme="purple">
      <template #actions>
        <template v-if="editing">
          <AppButton type="button" variant="outline" color="purple" :icon="closeIcon" :disabled="submitting"
            @click="cancelEdit">
            取消
          </AppButton>
          <AppButton type="submit" form="cinema-edit-form" color="purple" :icon="contentSaveIcon"
            :disabled="submitting">
            {{ submitting ? "儲存中…" : "儲存" }}
          </AppButton>
        </template>
        <AppButton v-else type="button" color="purple" :icon="pencilIcon" @click="startEdit">
          編輯資料
        </AppButton>
      </template>

      <div v-if="editing">
        <CinemaForm :key="editSession" mode="edit" plain hide-submit form-id="cinema-edit-form" :cinema="cinema"
          :show-active="false" :show-visibility="false" :submitting="submitting" @submit="saveCinema" />
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
          <h3>位置與聯絡資訊</h3>
          <dl>
            <div v-for="field in locationFields" :key="field.label">
              <dt>{{ field.label }}</dt>
              <dd>{{ displayText(field.value) }}</dd>
            </div>
            <div>
              <dt>官方網站</dt>
              <dd>
                <a v-if="websiteHref" class="detail-link" :href="websiteHref" target="_blank" rel="noopener noreferrer">
                  {{ websiteUrl }}
                </a>
                <template v-else>{{ displayText(websiteUrl) }}</template>
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
            <dt>營業狀態</dt>
            <dd>
              <Switch :model-value="cinema.isActive === true" :disabled="Boolean(pendingFlags.isActive)"
                @update:model-value="toggleFlag('isActive', $event)">
                {{ cinema.isActive === true ? "營業中" : "已歇業" }}
              </Switch>
            </dd>
          </div>
          <div>
            <dt>前台顯示</dt>
            <dd>
              <Switch :model-value="cinema.isVisible === true" :disabled="Boolean(pendingFlags.isVisible)"
                @update:model-value="toggleFlag('isVisible', $event)">
                {{ cinema.isVisible === true ? "顯示" : "隱藏" }}
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

