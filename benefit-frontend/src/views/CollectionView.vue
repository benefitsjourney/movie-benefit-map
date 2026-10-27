<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { Icon } from "@iconify/vue";
import heartIcon from "@iconify-icons/mdi/heart-outline";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import cameraIcon from "@iconify-icons/mdi/camera-plus-outline";
import trashIcon from "@iconify-icons/mdi/trash-can-outline";
import closeIcon from "@iconify-icons/mdi/close";
import arrowIcon from "@iconify-icons/mdi/arrow-right";
import calendarIcon from "@iconify-icons/mdi/calendar-blank-outline";
import filmIcon from "@iconify-icons/mdi/filmstrip";
import searchIcon from "@iconify-icons/mdi/magnify";
import compassIcon from "@iconify-icons/mdi/compass-outline";

import http from "@/plugins/axios";
import { openAppAlert } from "@/components/ui/Alert.vue";
import PageIntro from "@/components/layout/PageIntro.vue";
import ResultBanner from "@/components/layout/ResultBanner.vue";
import SearchFilter from "@/components/layout/SearchFilter.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import TicketCard from "@/components/ui/TicketCard.vue";
import Pagination from "@/components/ui/Pagination.vue";
import ImageViewer from "@/components/ui/ImageViewer.vue";

// 跟後端 UseBenefitService 的限制一樣，先在前端擋，少打一次 API
const MAX_PHOTOS = 5;
const MAX_BYTES = 10 * 1024 * 1024;
const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/webp", "image/gif"];
// 一頁幾張，跟特典一覽一樣
const PAGE_SIZE = 20;

const BENEFIT_TYPES = {
  REGULAR: { label: "一般場次", color: "yellow" },
  SPECIAL_BY_DATE: { label: "入場贈送", color: "pink" },
  SPECIAL_UNTIL_OUT: { label: "限量兌換", color: "purple" },
};

const loggedIn = ref(Boolean(localStorage.getItem("token")));
const loading = ref(false);
const loadFailed = ref(false);
const items = ref([]);
// 正在上傳／刪除的收藏 ID，避免連點
const busyId = ref(null);

function apiMessage(error, fallback) {
  const message = error?.response?.data?.message;
  return typeof message === "string" && message.trim() ? message.trim() : fallback;
}

function showError(title, text) {
  return openAppAlert({ variant: "error", title, text, confirmText: "關閉", cancelText: "" });
}

// GET /use-benefits/me：回傳的是陣列（不是分頁），已經排除軟刪除的
async function loadCollection() {
  if (!loggedIn.value) return;
  loading.value = true;
  loadFailed.value = false;
  try {
    const { data } = await http.get("/use-benefits/me");
    items.value = Array.isArray(data) ? data : [];
  } catch (error) {
    if (error?.response?.status === 401) {
      loggedIn.value = false;
      return;
    }
    loadFailed.value = true;
  } finally {
    loading.value = false;
  }
}

function typeOf(type) {
  return BENEFIT_TYPES[type] ?? { label: "特典", color: "yellow" };
}

// 封面：有自己拍的照片就用第一張，沒有就用官方圖
function coverUrl(item) {
  return item.photos?.[0]?.photoUrl || item.officialImageUrl || "";
}

function dateText(value) {
  const matched = typeof value === "string" && value.match(/^(\d{4})-(\d{2})-(\d{2})/);
  return matched ? `${matched[1]}/${matched[2]}/${matched[3]}` : "";
}

// 後端一次只收一張，所以選多張時一張一張依序傳
async function uploadPhoto(item, event) {
  const input = event.target;
  const files = [...(input.files ?? [])];
  input.value = ""; // 清空，同一張照片才能再選一次
  if (!files.length || busyId.value) return;

  // 最多還能傳幾張
  const remaining = MAX_PHOTOS - (item.photos?.length ?? 0);
  if (remaining <= 0) {
    await showError("無法上傳", `每筆收藏最多只能有 ${MAX_PHOTOS} 張照片。`);
    return;
  }

  // 先挑掉格式或大小不對的
  const valid = files.filter((f) => ALLOWED_TYPES.includes(f.type) && f.size <= MAX_BYTES);
  const skipped = files.length - valid.length;
  // 超過上限的部分不傳
  const toUpload = valid.slice(0, remaining);
  const overLimit = valid.length - toUpload.length;

  busyId.value = item.useBenefitId;
  let failed = 0;
  try {
    for (const file of toUpload) {
      try {
        const form = new FormData();
        form.append("file", file);
        const { data } = await http.post(`/use-benefits/${item.useBenefitId}/photos`, form);
        // 傳完一張就先加進畫面
        item.photos = [...(item.photos ?? []), data];
      } catch {
        failed += 1;
      }
    }
  } finally {
    busyId.value = null;
  }

  // 有沒傳成功的，最後一次告訴使用者
  const notes = [];
  if (skipped) notes.push(`${skipped} 張格式不符或超過 10MB`);
  if (overLimit) notes.push(`每筆最多 ${MAX_PHOTOS} 張，多選的 ${overLimit} 張沒有上傳`);
  if (failed) notes.push(`${failed} 張上傳失敗`);
  if (notes.length) {
    await showError("部分照片沒有上傳", `${notes.join("、")}，其餘已上傳完成。`);
  }
}

// ── 刪除一張照片：DELETE /use-benefits/{id}/photos/{photoId} ──
async function deletePhoto(item, photo) {
  if (busyId.value) return;
  const result = await openAppAlert({
    variant: "warning",
    title: "刪除這張照片？",
    text: "刪除後無法復原。",
    confirmText: "刪除",
    cancelText: "取消",
  });
  if (!result.isConfirmed) return;

  busyId.value = item.useBenefitId;
  try {
    await http.delete(`/use-benefits/${item.useBenefitId}/photos/${photo.useBenefitPhotoId}`);
    item.photos = item.photos.filter((p) => p.useBenefitPhotoId !== photo.useBenefitPhotoId);
  } catch (error) {
    await showError("刪除失敗", apiMessage(error, "請稍後再試一次。"));
  } finally {
    busyId.value = null;
  }
}

// ── 刪除收藏：DELETE /use-benefits/{id} ──
// 有交換刊登（還沒刪除）的會回 409，直接顯示後端的訊息
async function deleteItem(item) {
  if (busyId.value) return;
  const result = await openAppAlert({
    variant: "warning",
    title: "從收藏庫移除？",
    text: `「${item.benefitName}」會從你的收藏庫移除。`,
    confirmText: "移除",
    cancelText: "取消",
  });
  if (!result.isConfirmed) return;

  busyId.value = item.useBenefitId;
  try {
    await http.delete(`/use-benefits/${item.useBenefitId}`);
    items.value = items.value.filter((i) => i.useBenefitId !== item.useBenefitId);
  } catch (error) {
    await showError("無法移除", apiMessage(error, "請稍後再試一次。"));
  } finally {
    busyId.value = null;
  }
}
// ── 看大圖：點縮圖或封面打開，可以左右切換 ──
const viewerOpen = ref(false);
const viewerItem = ref(null);
const viewerIndex = ref(0);

// 有自己拍的照片就看照片，沒有就看官方圖
const viewerImages = computed(() => {
  const item = viewerItem.value;
  if (!item) return [];
  if (item.photos?.length) return item.photos.map((p) => p.photoUrl);
  return item.officialImageUrl ? [item.officialImageUrl] : [];
});

function openViewer(item, index = 0) {
  viewerItem.value = item;
  viewerIndex.value = index;
  viewerOpen.value = true;
}

const count = computed(() => items.value.length);

// ── 搜尋與分頁：後端一次回傳全部，在前端篩選和切頁 ──
const filters = ref({ keyword: "" });
const page = ref(1);
const listTop = ref(null);

// 搜尋條件改變時回到第1頁（寫法同特典一覽）
const appliedFilters = computed({
  get: () => filters.value,
  set: (next) => {
    filters.value = { keyword: next?.keyword ?? "" };
    page.value = 1;
  },
});

const filterFields = [
  {
    key: "keyword",
    label: "關鍵字",
    type: "search",
    placeholder: "搜尋特典或電影名稱",
    icon: searchIcon,
  },
];

const keyword = computed(() => filters.value.keyword.trim().toLowerCase());

// 特典名稱或電影名稱包含關鍵字就留下，英文不分大小寫
const filteredItems = computed(() => {
  if (!keyword.value) return items.value;
  return items.value.filter((item) =>
    [item.benefitName, item.movieName].some((text) => String(text ?? "").toLowerCase().includes(keyword.value)),
  );
});

const totalPages = computed(() => Math.max(1, Math.ceil(filteredItems.value.length / PAGE_SIZE)));
const pagedItems = computed(() => filteredItems.value.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE));

// 移除收藏後頁數變少，目前頁超過最後一頁就退回最後一頁
watch(totalPages, (last) => {
  if (page.value > last) page.value = last;
});

function goToPage(next) {
  if (!Number.isInteger(next) || next < 1 || next > totalPages.value || next === page.value) return;
  page.value = next;
  listTop.value?.scrollIntoView({
    behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth",
    block: "start",
  });
}

onMounted(loadCollection);
</script>

<template>
  <section>
    <PageIntro kicker="✦ MY COLLECTION" lead="我的" mark="收藏庫" description="記錄你領到的特典，拍照留念，也可以拿去交換。"
      :icon-left="heartIcon" :icon-right="giftIcon" :icon-top="cameraIcon" />

    <!-- 沒登入 -->
    <div v-if="!loggedIn" class="login-hint">
      <Icon :icon="heartIcon" aria-hidden="true" />
      <p>登入會員後才能使用收藏庫。</p>
    </div>

    <template v-else>
      <StatePanel v-if="loading" variant="loading" message="正在載入收藏…" :icon="heartIcon" />
      <StatePanel v-else-if="loadFailed" variant="error" title="目前無法取得收藏" message="請確認連線後再試一次。"
        @retry="loadCollection" />

      <template v-else>
        <ResultBanner :icon="heartIcon" title="我的收藏庫" :count="keyword ? filteredItems.length : count"
          :unit-text="keyword ? '款符合條件的特典' : '款特典'"
          :empty-text="keyword ? '沒有找到符合條件的收藏，換個關鍵字試試吧！' : '還沒有收藏，到特典頁按「加入收藏」吧！'"
          action-to="/benefits" :action-icon="giftIcon" action-label="去逛特典" />

        <SearchFilter v-if="items.length" v-model="appliedFilters" title="找找我的收藏" :title-icon="compassIcon"
          submit-label="搜尋收藏" :fields="filterFields">
        <div v-if="pagedItems.length" ref="listTop" class="ticket-grid">
          <TicketCard v-for="item in pagedItems" :key="item.useBenefitId" :color="typeOf(item.benefitType).color"
            :stamp="typeOf(item.benefitType).label" :title="item.benefitName">
            <template #poster>
              <img v-if="coverUrl(item)" :src="coverUrl(item)" :alt="`${item.benefitName} 圖片`" loading="lazy"
                referrerpolicy="no-referrer" class="poster-click" @click="openViewer(item)" />
              <span v-else class="poster-fallback">
                <Icon :icon="giftIcon" aria-hidden="true" />
              </span>
            </template>

            <template #detail>
              <Icon :icon="calendarIcon" aria-hidden="true" />
              <span>收藏於 {{ dateText(item.createdAt) || "—" }}</span>
            </template>

            <p class="movie-name">
              <Icon :icon="filmIcon" aria-hidden="true" />
              <span>{{ item.movieName || "電影未提供" }}</span>
            </p>

            <span v-if="item.benefitStatus === 'EXPIRED'" class="ended-tag">特典已結束發放</span>

            <!-- 照片：縮圖＋刪除，最後一格是上傳按鈕 -->
            <div class="photo-strip">
              <div v-for="(photo, index) in item.photos" :key="photo.useBenefitPhotoId" class="photo-thumb">
                <button type="button" class="photo-open" aria-label="查看大圖" @click="openViewer(item, index)">
                  <img :src="photo.photoUrl" alt="收藏照片" loading="lazy" />
                </button>
                <button type="button" class="photo-delete" aria-label="刪除這張照片"
                  :disabled="busyId === item.useBenefitId" @click="deletePhoto(item, photo)">
                  <Icon :icon="closeIcon" aria-hidden="true" />
                </button>
              </div>

              <label v-if="(item.photos?.length ?? 0) < MAX_PHOTOS" class="photo-add"
                :class="{ 'is-busy': busyId === item.useBenefitId }">
                <Icon :icon="cameraIcon" aria-hidden="true" />
                <span>{{ busyId === item.useBenefitId ? "處理中…" : "加照片" }}</span>
                <input type="file" multiple accept="image/jpeg,image/png,image/webp,image/gif"
                  :disabled="busyId === item.useBenefitId" @change="uploadPhoto(item, $event)" />
              </label>
            </div>

            <template #action>
              <RouterLink class="ticket-link" :to="`/benefits/${item.benefitId}`">
                特典資訊
                <Icon :icon="arrowIcon" aria-hidden="true" />
              </RouterLink>
              <button type="button" class="ticket-link" :disabled="busyId === item.useBenefitId"
                @click="deleteItem(item)">
                <Icon :icon="trashIcon" aria-hidden="true" />
                移除
              </button>
            </template>
          </TicketCard>
        </div>

        <Pagination v-if="totalPages > 1" :page="page" :total-pages="totalPages" label="收藏庫分頁" @update:page="goToPage" />
        </SearchFilter>
      </template>
    </template>
    <!-- 大圖 -->
    <ImageViewer v-model="viewerOpen" v-model:index="viewerIndex" :images="viewerImages"
          :title="viewerItem?.benefitName ?? ''" />
  </section>
</template>

<style scoped>
.ticket-grid {
  gap: 1.125rem;
  scroll-margin-top: 5.5rem;
}

@media (min-width: 640px) {
  .ticket-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1100px) {
  .ticket-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (min-width: 1400px) {
  .ticket-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

.poster-fallback :deep(svg) {
  font-size: var(--text-page-title);
}

/* 電影名稱，樣式跟特典一覽一樣 */
.movie-name {
  display: flex;
  align-items: flex-start;
  gap: 0.4rem;
  margin: 0.3rem 0 0;
  color: #3e4450;
  font-size: var(--text-control);
  font-weight: 700;
  overflow-wrap: anywhere;
}

.movie-name :deep(svg) {
  flex: none;
  margin-top: 0.2em;
}

.login-hint {
  display: grid;
  justify-items: center;
  gap: 0.6rem;
  padding: 2.5rem 1.25rem;
  border: 1.5px solid var(--c-border);
  border-radius: 22px;
  background: var(--c-card);
  color: var(--c-muted);
  font-weight: 700;
  text-align: center;
}

.login-hint :deep(svg) {
  color: #ff7049;
  font-size: var(--text-page-title);
}

.login-hint p {
  margin: 0;
}

.ended-tag {
  align-self: flex-start;
  margin-top: 0.4rem;
  padding: 0.1rem 0.6rem;
  border-radius: 999px;
  background: #f1f1f4;
  color: #6c7380;
  font-size: var(--text-min);
  font-weight: 800;
}

/* ── 照片列 ── */
.photo-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-top: 0.75rem;
}

.photo-thumb,
.photo-add {
  position: relative;
  width: 4.5rem;
  height: 4.5rem;
  border-radius: 10px;
}

.photo-thumb img {
  width: 100%;
  height: 100%;
  border: 1px solid var(--c-border);
  border-radius: 10px;
  object-fit: cover;
}

.photo-delete {
  position: absolute;
  top: -0.4rem;
  right: -0.4rem;
  display: grid;
  place-items: center;
  width: 1.4rem;
  height: 1.4rem;
  border: 0;
  border-radius: 50%;
  background: #a33b42;
  color: #fff;
  font-size: 0.9rem;
  cursor: pointer;
}

.photo-delete:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.photo-add {
  display: grid;
  place-items: center;
  align-content: center;
  border: 1.5px dashed #c9b8a4;
  background: #fffaf5;
  color: #8a6400;
  font-size: var(--text-min);
  font-weight: 800;
  line-height: 1.2;
  cursor: pointer;
}

.photo-add :deep(svg) {
  font-size: 1.2rem;
}

.photo-add.is-busy {
  opacity: 0.6;
  cursor: wait;
}

/* 真正的 input 藏起來，點整個方塊就會開檔案選擇 */
.photo-add input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.photo-add input:disabled {
  cursor: wait;
}

/* ── 看大圖 ── */
.poster-click {
  cursor: zoom-in;
}

.photo-open {
  display: block;
  width: 100%;
  height: 100%;
  padding: 0;
  border: 0;
  background: none;
  cursor: zoom-in;
}
</style>
