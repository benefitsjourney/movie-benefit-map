<script setup>
import { computed, ref, watch } from "vue";
import { Icon } from "@iconify/vue";
import clipboardIcon from "@iconify-icons/mdi/clipboard-check-outline";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import tableIcon from "@iconify-icons/mdi/table-large";
import filterIcon from "@iconify-icons/mdi/filter-variant";
import checkIcon from "@iconify-icons/mdi/check";
import closeIcon from "@iconify-icons/mdi/close";
import searchIcon from "@iconify-icons/mdi/magnify";
import linkIcon from "@iconify-icons/mdi/open-in-new";
import pencilIcon from "@iconify-icons/mdi/pencil-outline";
import saveIcon from "@iconify-icons/mdi/content-save-outline";
import arrowLeftIcon from "@iconify-icons/mdi/arrow-left";
import alertIcon from "@iconify-icons/mdi/information-outline";
import cameraIcon from "@iconify-icons/mdi/camera-plus-outline";
import undoIcon from "@iconify-icons/mdi/undo";

import http from "@/plugins/axios";
import { useApiCollection } from "@/composables/useApiCollection";
import { openAppAlert } from "@/components/ui/Alert.vue";
import PageIntro from "@/components/layout/PageIntro.vue";
import ResultBanner from "@/components/layout/ResultBanner.vue";
import SearchFilter from "@/components/layout/SearchFilter.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import DataTable, { dateTimeText, httpUrl } from "@/components/ui/DataTable.vue";
import Pagination from "@/components/ui/Pagination.vue";
import AppButton from "@/components/ui/Button.vue";
import Switch from "@/components/ui/Switch.vue";
import AppModal from "@/components/ui/AppModal.vue";

const PAGE_SIZE = 20;
const STATUS_OPTIONS = [
  { value: "PENDING", label: "待審核" },
  { value: "APPROVED", label: "已上架" },
  { value: "REJECTED", label: "不通過" },
];
const BENEFIT_TYPES = {
  REGULAR: "一般場次",
  SPECIAL_BY_DATE: "入場贈送",
  SPECIAL_UNTIL_OUT: "限量兌換",
};

// 圖片限制跟會員回報、後端 BenefitReportImageService 一樣
const MAX_IMAGE_BYTES = 10 * 1024 * 1024;
const IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp", "image/gif"];

function apiMessage(error, fallback) {
  const message = error?.response?.data?.message;
  return typeof message === "string" && message.trim() ? message.trim() : fallback;
}

function showError(title, error, fallback = "請確認連線後再試一次。") {
  const code = error?.response?.status;
  const text = code === 401 ? "請先登入管理員帳號。" : code === 403 ? "這個帳號沒有管理員權限。" : apiMessage(error, fallback);
  return openAppAlert({ variant: "error", title, text, confirmText: "關閉", cancelText: "" });
}

function showInfo(title, text) {
  return openAppAlert({ variant: "info", title, text, confirmText: "知道了", cancelText: "" });
}

// ── 列表：GET /benefit-reports?status=&page=&size= ──
const filters = ref({ status: "PENDING" });
const page = ref(1);
const listTop = ref(null);

// 寫法同組員的 AdminCrawlerReviewView：條件改變時回到第 1 頁
const appliedFilters = computed({
  get: () => filters.value,
  set: (next) => {
    filters.value = { status: next?.status || "PENDING" };
    page.value = 1;
  },
});

const filterFields = [
  {
    key: "status",
    label: "審核狀態",
    type: "select",
    emptyLabel: null, // 後端一定要帶狀態，所以沒有「全部」
    icon: clipboardIcon,
    options: STATUS_OPTIONS,
  },
];

const requestParams = computed(() => ({
  status: filters.value.status,
  page: page.value,
  size: PAGE_SIZE,
}));

const { items: reports, totalElements, totalPages, loading, error, retry } =
  useApiCollection("/benefit-reports", requestParams, { page });

const isPending = computed(() => filters.value.status === "PENDING");

// 電影：有對應到資料庫就顯示編號，沒有就顯示回報單上的自訂名稱
function movieText(row) {
  if (row.movieId) return row.movieName || `#${row.movieId}`;
  return row.customMovieName ? `${row.customMovieName}（新）` : "";
}

const columns = [
  { key: "benefitReportId", label: "編號", minWidth: "5.5rem", nowrap: true, align: "center" },
  { key: "benefitName", label: "特典名稱", minWidth: "16rem", emphasis: true },
  { key: "movie", label: "電影", minWidth: "10rem", format: (_, row) => movieText(row) },
  { key: "benefitType", label: "類型", minWidth: "7rem", nowrap: true, align: "center", format: (v) => BENEFIT_TYPES[v] ?? "" },
  { key: "startDate", label: "開始日", minWidth: "11rem", nowrap: true, align: "center", format: dateTimeText },
  { key: "source", label: "來源", minWidth: "6.5rem", nowrap: true, align: "center", format: (_, row) => (row.userId ? `會員#${row.userId}` : "爬蟲") },
  { key: "reviewNote", label: "備註", minWidth: "16rem" },
  { key: "actions", label: "操作", minWidth: "10rem", align: "center" },
];

function goToPage(next) {
  if (!Number.isInteger(next) || next < 1 || next > totalPages.value || next === page.value) return;
  page.value = next;
  listTop.value?.scrollIntoView({ behavior: "smooth", block: "start" });
}

// ── 日期轉換 ──
// API 給 "2026-10-16T00:00:00" 或 "2026-10-16 00:00:00"，<input type="date"> 要 "2026-10-16"
function toInputDate(value) {
  return value ? String(value).slice(0, 10) : "";
}

// 送回 API 要 "2026-10-16 00:00:00"；結束日用當天最後一刻
function toApiDate(value, endOfDay = false) {
  if (!value) return null;
  return `${value} ${endOfDay ? "23:59:59" : "00:00:00"}`;
}

// Date物件 → "2026-10-16"
function toDateText(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

// ── 影城清單（審核時勾選用），第一次打開審核視窗才載入 ──
const cinemas = ref([]);
let cinemasLoaded = false;

async function loadCinemas() {
  if (cinemasLoaded) return;
  // GET /cinemas 是分頁的，一頁一頁拿到最後一頁
  const all = [];
  let current = 1;
  let pages = 1;
  do {
    const { data } = await http.get("/cinemas", { params: { page: current, size: 100, isActive: true } });
    all.push(...(data?.content ?? []));
    pages = data?.totalPages ?? 1;
    current += 1;
  } while (current <= pages);
  cinemas.value = all;
  cinemasLoaded = true;
}

// ── 審核視窗（編輯＋核准＋駁回都在這裡）──
const reviewOpen = ref(false);
const target = ref(null);
const submitting = ref(false);
const submitStep = ref(""); // 按鈕上顯示目前在做什麼
// false＝編輯／核准畫面，true＝駁回畫面
const rejectMode = ref(false);
const rejectNote = ref("");

const form = ref({});
const movieKeyword = ref("");
const movieResults = ref([]);
const selectedMovie = ref(null); // { movieId, movieName }
const releaseDate = ref(""); // 電影庫沒有這部、要新建時才需要
const cinemaKeyword = ref("");
const selectedCinemaIds = ref([]);

// ── 圖片：補圖／換圖 ──
// 選圖時只在瀏覽器預覽，按「只儲存」或「儲存並核准」時才上傳，再跟其他欄位一起 PATCH
const newImageFile = ref(null);
const newImagePreview = ref(""); // 瀏覽器暫時網址（blob:），只給預覽用
const imageError = ref("");
// 同一張圖已經上傳過就記下來；儲存失敗再按一次時直接沿用，不重複上傳
let uploadedImage = null; // { url, publicId }

// 畫面上要顯示的圖：有選新圖就顯示新圖，沒有就顯示回報單原本的圖
const shownImage = computed(() => newImagePreview.value || httpUrl(target.value?.officialImageUrl));

function clearNewImage() {
  if (newImagePreview.value) URL.revokeObjectURL(newImagePreview.value);
  newImageFile.value = null;
  newImagePreview.value = "";
  imageError.value = "";
  uploadedImage = null;
}

function onImageChange(event) {
  const input = event.target;
  const file = input.files?.[0] ?? null;
  input.value = ""; // 清空，同一張圖才能再選一次
  if (!file) return;

  if (!IMAGE_TYPES.includes(file.type)) {
    imageError.value = "只能上傳 JPG、PNG、WEBP、GIF 格式的圖片";
    return;
  }
  if (file.size > MAX_IMAGE_BYTES) {
    imageError.value = "圖片不可超過 10MB";
    return;
  }

  clearNewImage();
  newImageFile.value = file;
  newImagePreview.value = URL.createObjectURL(file);
}

// POST /benefit-reports/images：回傳 { url, publicId }；沒選新圖就回 null
async function uploadNewImage() {
  if (!newImageFile.value) return null;
  if (uploadedImage) return uploadedImage;
  const data = new FormData();
  data.append("file", newImageFile.value);
  const { data: result } = await http.post("/benefit-reports/images", data);
  uploadedImage = { url: result.url, publicId: result.publicId };
  return uploadedImage;
}

// 視窗關掉時把暫時的預覽清掉
watch(reviewOpen, (open) => {
  if (!open) clearNewImage();
});

// 一般場次（週特典）最長 7 天：結束日最晚是開始日＋6天
const endDateMax = computed(() => {
  if (form.value.benefitType !== "REGULAR" || !form.value.startDate) return "";
  const date = new Date(`${form.value.startDate}T00:00:00`);
  date.setDate(date.getDate() + 6);
  return toDateText(date);
});

// 一般場次選好開始日、結束日還空著時，自動帶入第7天
watch(endDateMax, (max) => {
  if (max && !form.value.endDate) form.value.endDate = max;
});

// ── 相似特典：審核時查資料庫已有的特典，判斷是不是重複 ──
// 用 GET /benefits/admin?keyword=，會同時比對特典名稱和電影名稱，上下架都查得到
const similarKeyword = ref("");
const similarList = ref([]);
const similarLoading = ref(false);
const similarSearched = ref(false);

async function searchSimilar() {
  const keyword = similarKeyword.value.trim();
  if (!keyword) return;
  similarLoading.value = true;
  try {
    const { data } = await http.get("/benefits/admin", { params: { keyword, page: 1, size: 8 } });
    similarList.value = data?.content ?? [];
  } catch {
    similarList.value = [];
  } finally {
    similarLoading.value = false;
    similarSearched.value = true;
  }
}

async function openReview(row) {
  target.value = row;
  rejectMode.value = false;
  rejectNote.value = "";
  clearNewImage();
  form.value = {
    benefitName: row.benefitName ?? "",
    benefitType: row.benefitType ?? "",
    isMemberOnly: Boolean(row.isMemberOnly),
    startDate: toInputDate(row.startDate),
    endDate: toInputDate(row.endDate),
    customMovieName: row.customMovieName ?? "",
  };
  movieKeyword.value = row.customMovieName ?? "";
  movieResults.value = [];
  selectedMovie.value = null;
  releaseDate.value = "";
  cinemaKeyword.value = "";

  // 先用爬蟲從原文判斷出的影城；沒有的話用回報單上的那一間
  const matched = String(row.matchedCinemaIds ?? "")
    .split(/[,\s]+/)
    .map(Number)
    .filter((id) => Number.isInteger(id) && id > 0);
  selectedCinemaIds.value = matched.length ? matched : row.cinemaId ? [row.cinemaId] : [];

  // 預設用電影名稱查，沒有就用特典名稱
  similarKeyword.value = row.customMovieName || row.benefitName || "";
  similarList.value = [];
  similarSearched.value = false;
  searchSimilar();
  reviewOpen.value = true;
  try {
    await loadCinemas();
  } catch (e) {
    await showError("無法載入影城清單", e);
  }
}

// GET /movies?keyword=：找電影讓管理員指定
async function searchMovies() {
  const keyword = movieKeyword.value.trim();
  if (!keyword) return;
  try {
    const { data } = await http.get("/movies", { params: { keyword, page: 1, size: 10 } });
    movieResults.value = data?.content ?? [];
    if (!movieResults.value.length) {
      await showInfo("找不到電影", "電影庫沒有符合的電影，可以直接在下方填「新電影名稱」和上映日期。");
    }
  } catch (e) {
    await showError("搜尋電影失敗", e);
  }
}

function pickMovie(movie) {
  // 再點一次同一部＝取消選擇
  selectedMovie.value = selectedMovie.value?.movieId === movie.movieId ? null : movie;
}

const filteredCinemas = computed(() => {
  const word = cinemaKeyword.value.trim();
  return word ? cinemas.value.filter((c) => String(c.cinemaName ?? "").includes(word)) : cinemas.value;
});

// 已勾選的影城名稱，方便一眼確認勾了哪些
const selectedCinemaNames = computed(() =>
  cinemas.value
    .filter((c) => selectedCinemaIds.value.includes(c.cinemaId))
    .map((c) => c.cinemaName),
);

// 把目前搜尋結果全部勾起來（例如搜「威秀」再按這個＝全台威秀）
function selectFiltered() {
  const ids = new Set(selectedCinemaIds.value);
  filteredCinemas.value.forEach((c) => ids.add(c.cinemaId));
  selectedCinemaIds.value = [...ids];
}

// 電影：管理員指定 > 回報單原本的 > 用新電影名稱新建（要上映日）
const hasMovie = computed(() => Boolean(selectedMovie.value || target.value?.movieId));
const needsNewMovie = computed(() => !hasMovie.value);

const movieSummary = computed(() => {
  if (selectedMovie.value) return `${selectedMovie.value.movieName}（#${selectedMovie.value.movieId}）`;
  if (target.value?.movieId) return `${target.value.movieName ?? "資料庫電影"}（#${target.value.movieId}）`;
  return "尚未對應到資料庫的電影";
});

// PATCH /benefit-reports/{id}：只送有內容的欄位（後端是「沒帶就不改」）
// image：有換圖時傳 { url, publicId }，沒換圖就不帶，後端維持原本的圖
function buildUpdateBody(image) {
  const f = form.value;
  const body = {
    benefitName: f.benefitName.trim(),
    isMemberOnly: f.isMemberOnly,
  };
  if (f.benefitType) body.benefitType = f.benefitType;
  if (f.startDate) body.startDate = toApiDate(f.startDate);
  if (f.endDate) body.endDate = toApiDate(f.endDate, true);
  if (needsNewMovie.value && f.customMovieName.trim()) body.customMovieName = f.customMovieName.trim();
  // 有從搜尋選電影就一起存，下次打開會記得
  if (selectedMovie.value) body.movieId = selectedMovie.value.movieId;
  // 勾選的影城也先存起來（核准時才真正綁定）
  body.cinemaIds = selectedCinemaIds.value;
  // 圖片網址和 publicId 要一起帶（後端會檢查）
  if (image) {
    body.officialImageUrl = image.url;
    body.publicId = image.publicId;
  }
  return body;
}

// 有選新圖就先上傳；上傳失敗會跳提示並回傳 false（呼叫的地方就停下來）
async function prepareImage() {
  if (!newImageFile.value) return { ok: true, image: null };
  submitStep.value = "上傳圖片中…";
  try {
    return { ok: true, image: await uploadNewImage() };
  } catch (e) {
    await showError("圖片上傳失敗", e, "請稍後再試，或按「取消更換」用原本的圖。");
    return { ok: false, image: null };
  }
}

// 核准前先在前端檢查一次，少打幾次會失敗的 API
function approveProblems() {
  const f = form.value;
  const problems = [];
  if (!f.benefitName.trim()) problems.push("特典名稱");
  if (!f.benefitType) problems.push("特典類型");
  if (!f.startDate) problems.push("開始日期");
  if (needsNewMovie.value) {
    if (!f.customMovieName.trim()) problems.push("電影（搜尋選一部，或填新電影名稱）");
    else if (!releaseDate.value) problems.push("新電影的上映日期");
  }
  if (!selectedCinemaIds.value.length && !target.value?.cinemaId) problems.push("影城（至少勾一間）");
  if (f.startDate && f.endDate && f.endDate < f.startDate) problems.push("結束日期不能早於開始日期");
  if (endDateMax.value && f.endDate && f.endDate > endDateMax.value) {
    problems.push(`一般場次最多 7 天，結束日最晚是 ${endDateMax.value}`);
  }
  return problems;
}

// 只儲存修改，不核准（例如資料還不確定，先存起來之後再審）
async function saveOnly() {
  const row = target.value;
  if (!row || submitting.value) return;
  if (!form.value.benefitName.trim()) {
    await showInfo("請填特典名稱", "特典名稱不能空白。");
    return;
  }
  submitting.value = true;
  try {
    const { ok, image } = await prepareImage();
    if (!ok) return;
    submitStep.value = "儲存中…";
    await http.patch(`/benefit-reports/${row.benefitReportId}`, buildUpdateBody(image));
    reviewOpen.value = false;
    await openAppAlert({ variant: "success", title: "已儲存", text: "修改已儲存，這筆仍在待審核。", confirmText: "關閉", cancelText: "" });
    retry();
  } catch (e) {
    await showError("儲存失敗", e);
  } finally {
    submitting.value = false;
    submitStep.value = "";
  }
}

// 核准：（有新圖先上傳）→ PATCH 存修改 → PATCH /approve
async function submitApprove() {
  const row = target.value;
  if (!row || submitting.value) return;

  const problems = approveProblems();
  if (problems.length) {
    await showInfo("還有資料要補", `請先補上：${problems.join("、")}。`);
    return;
  }

  const approveBody = {};
  if (selectedMovie.value) approveBody.movieId = selectedMovie.value.movieId;
  if (needsNewMovie.value) approveBody.releaseDate = toApiDate(releaseDate.value);
  if (selectedCinemaIds.value.length) approveBody.cinemaIds = selectedCinemaIds.value;

  submitting.value = true;
  let saved = false;
  try {
    const { ok, image } = await prepareImage();
    if (!ok) return;
    submitStep.value = "儲存中…";
    await http.patch(`/benefit-reports/${row.benefitReportId}`, buildUpdateBody(image));
    saved = true;
    submitStep.value = "核准中…";
    await http.patch(`/benefit-reports/${row.benefitReportId}/approve`, approveBody);
    reviewOpen.value = false;
    await openAppAlert({ variant: "success", title: "已核准", text: `「${form.value.benefitName.trim()}」已建立為正式特典。`, confirmText: "關閉", cancelText: "" });
    retry();
  } catch (e) {
    // 修改已經存進去、只是核准失敗的話，也要讓管理員知道
    await showError("核准失敗", e, saved ? "修改已儲存，但核准沒有成功，請再試一次。" : undefined);
    if (saved) retry();
  } finally {
    submitting.value = false;
    submitStep.value = "";
  }
}

// 駁回：PATCH /reject（理由沒填的話，後端會自動寫「審核未通過」）
async function submitReject() {
  const row = target.value;
  if (!row || submitting.value) return;
  submitting.value = true;
  try {
    const note = rejectNote.value.trim();
    await http.patch(`/benefit-reports/${row.benefitReportId}/reject`, note ? { reviewNote: note } : {});
    reviewOpen.value = false;
    await openAppAlert({ variant: "success", title: "已駁回", text: `回報 #${row.benefitReportId} 已駁回。`, confirmText: "關閉", cancelText: "" });
    retry();
  } catch (e) {
    await showError("駁回失敗", e);
  } finally {
    submitting.value = false;
  }
}

const modalTitle = computed(() => {
  if (!target.value) return "";
  return rejectMode.value ? `駁回回報 #${target.value.benefitReportId}` : `審核回報 #${target.value.benefitReportId}`;
});
</script>

<template>
  <section>
    <PageIntro kicker="✦ BENEFIT REVIEW" lead="特典" mark="審核" description="審核爬蟲沒能自動上架、以及會員回報的特典。可以先修改資料再核准。"
      :icon-left="clipboardIcon" :icon-right="giftIcon" :icon-top="tableIcon" theme="purple" />

    <ResultBanner v-if="!(loading && !reports.length) && !error" :icon="clipboardIcon" title="特典回報"
      :count="totalElements" unit-text="筆" empty-text="目前沒有符合條件的回報。" theme="purple" />

    <SearchFilter v-if="!(loading && !reports.length) && !error" v-model="appliedFilters" title="篩選回報"
      :title-icon="filterIcon" submit-label="查詢" :fields="filterFields" theme="purple">
      <div ref="listTop" class="report-sheet">
        <DataTable caption="特典回報資料表" :columns="columns" :rows="reports" row-key="benefitReportId"
          empty-text="目前沒有符合條件的回報。" :loading="loading" theme="purple">
          <template #cell-actions="{ row }">
            <div class="row-actions">
              <AppButton v-if="isPending" type="button" color="purple" :icon="pencilIcon" @click="openReview(row)">
                審核
              </AppButton>
              <a v-if="httpUrl(row.sourceUrl)" class="source-link" :href="httpUrl(row.sourceUrl)" target="_blank"
                rel="noopener noreferrer" title="查看原文">
                <Icon :icon="linkIcon" aria-hidden="true" />
                原文
              </a>
            </div>
          </template>
        </DataTable>
      </div>

      <Pagination v-if="totalPages > 1" :page="page" :total-pages="totalPages" label="特典回報分頁" color="purple"
        @update:page="goToPage" />
    </SearchFilter>

    <StatePanel v-if="loading && !reports.length" variant="loading" message="正在載入特典回報…" :icon="clipboardIcon"
      color="purple" />
    <StatePanel v-else-if="error" variant="error" title="目前無法取得特典回報" message="請確認已用管理員帳號登入，或稍後再試。"
      color="purple" @retry="retry" />

    <!-- 審核視窗 -->
    <AppModal v-model="reviewOpen" :title="modalTitle" :busy="submitting" width="48rem" :close-on-backdrop="false">
      <template v-if="target">
        <!-- 上方：圖片＋來源＋爬蟲備註 -->
        <div class="report-summary">
          <div class="report-image">
            <!-- 點圖用新分頁看原圖 -->
            <a v-if="shownImage" :href="shownImage" target="_blank" rel="noopener noreferrer" class="image-frame"
              title="點擊看原圖">
              <img :src="shownImage" alt="特典圖片" referrerpolicy="no-referrer" />
              <span v-if="newImageFile" class="image-badge">新圖・尚未儲存</span>
            </a>
            <span v-else class="image-frame image-empty">無圖片</span>

            <!-- 駁回畫面不用改圖 -->
            <template v-if="!rejectMode">
              <label class="image-pick" :class="{ 'is-disabled': submitting }">
                <Icon :icon="cameraIcon" aria-hidden="true" />
                {{ shownImage ? "更換圖片" : "補上圖片" }}
                <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" :disabled="submitting"
                  @change="onImageChange" />
              </label>
              <button v-if="newImageFile" type="button" class="image-undo" :disabled="submitting"
                @click="clearNewImage">
                <Icon :icon="undoIcon" aria-hidden="true" />
                取消更換
              </button>
            </template>
          </div>

          <div class="summary-text">
            <p class="summary-source">
              來源：{{ target.userId ? `會員 #${target.userId}` : "爬蟲" }}
              <a v-if="httpUrl(target.sourceUrl)" :href="httpUrl(target.sourceUrl)" target="_blank"
                rel="noopener noreferrer">
                查看原文
                <Icon :icon="linkIcon" aria-hidden="true" />
              </a>
            </p>
            <p v-if="target.reviewNote" class="summary-note">
              <Icon :icon="alertIcon" aria-hidden="true" />
              <span>{{ target.reviewNote }}</span>
            </p>
            <p v-if="imageError" class="image-error">{{ imageError }}</p>
            <p v-else-if="!rejectMode" class="field-hint">
              圖片：JPG、PNG、WEBP、GIF，10MB 以內。選好後按「只儲存」或「儲存並核准」才會真的上傳。
            </p>
          </div>
        </div>

        <!-- 駁回畫面 -->
        <div v-if="rejectMode" class="field">
          <label class="field-label" for="reject-note">駁回理由（選填，會員看得到）</label>
          <textarea id="reject-note" v-model="rejectNote" class="form-control" rows="3" maxlength="100"
            placeholder="例如：非電影特典、與現有特典重複"></textarea>
          <p class="field-hint">沒填會顯示「審核未通過」。最多 100 字。</p>
        </div>

        <!-- 編輯／核准畫面 -->
        <template v-else>
          <h3 class="form-section">相似特典（判斷是否重複）</h3>
          <div class="field">
            <div class="inline-search">
              <input v-model="similarKeyword" type="search" class="form-control" placeholder="輸入電影或特典名稱"
                @keydown.enter.prevent="searchSimilar" />
              <AppButton type="button" color="purple" variant="outline" :icon="searchIcon" @click="searchSimilar">查詢</AppButton>
            </div>
            <p v-if="similarLoading" class="field-hint">查詢中…</p>
            <p v-else-if="similarSearched && !similarList.length" class="similar-empty">
              資料庫沒有相似的特典，應該是新的。
            </p>
            <ul v-else-if="similarList.length" class="similar-list">
              <li v-for="item in similarList" :key="item.benefitId">
                <img v-if="httpUrl(item.officialImageUrl)" :src="httpUrl(item.officialImageUrl)" alt=""
                  referrerpolicy="no-referrer" />
                <span v-else class="similar-noimg">無圖</span>
                <div class="similar-text">
                  <a :href="`/benefits/${item.benefitId}`" target="_blank" rel="noopener noreferrer">
                    {{ item.benefitName }}
                  </a>
                  <span>
                    {{ item.movieName || "—" }}・{{ dateTimeText(item.startDate) || "—" }}・{{ item.status === "APPROVED" ? "上架中" : "已下架" }}
                  </span>
                </div>
              </li>
            </ul>
          </div>
          <h3 class="form-section">特典資料</h3>

          <div class="field">
            <label class="field-label" for="review-name">特典名稱 *</label>
            <input id="review-name" v-model="form.benefitName" type="text" class="form-control" maxlength="100" />
          </div>

          <div class="field-row">
            <div class="field">
              <label class="field-label" for="review-type">特典類型 *</label>
              <select id="review-type" v-model="form.benefitType" class="form-select">
                <option value="" disabled>請選擇</option>
                <option v-for="(label, value) in BENEFIT_TYPES" :key="value" :value="value">{{ label }}</option>
              </select>
            </div>
            <div class="field switch-field">
              <Switch v-model="form.isMemberOnly">會員限定</Switch>
            </div>
          </div>

          <div class="field-row">
            <div class="field">
              <label class="field-label" for="review-start">開始日期 *</label>
              <input id="review-start" v-model="form.startDate" type="date" class="form-control" />
            </div>
            <div class="field">
              <label class="field-label" for="review-end">結束日期</label>
              <input id="review-end" v-model="form.endDate" type="date" class="form-control"
                :min="form.startDate || undefined" :max="endDateMax || undefined" />
            </div>
          </div>
          <p class="field-hint date-hint">開始日從當天 00:00 起算，結束日算到當天 23:59。限量兌換（送完為止）可以不填結束日。</p>

          <h3 class="form-section">電影</h3>
          <div class="field">
            <p class="movie-current" :class="{ 'is-missing': !hasMovie }">目前：{{ movieSummary }}</p>
            <div class="inline-search">
              <input v-model="movieKeyword" type="search" class="form-control" placeholder="搜尋電影名稱"
                @keydown.enter.prevent="searchMovies" />
              <AppButton type="button" color="purple" variant="outline" :icon="searchIcon" @click="searchMovies">搜尋</AppButton>
            </div>
            <div v-if="movieResults.length" class="choice-list">
              <button v-for="movie in movieResults" :key="movie.movieId" type="button" class="choice"
                :class="{ active: selectedMovie?.movieId === movie.movieId }" @click="pickMovie(movie)">
                {{ movie.movieName }}
              </button>
            </div>
          </div>

          <!-- 沒有對應到電影：用新電影名稱建立 -->
          <div v-if="needsNewMovie" class="field-row new-movie">
            <div class="field">
              <label class="field-label" for="review-movie-name">新電影名稱</label>
              <input id="review-movie-name" v-model="form.customMovieName" type="text" class="form-control"
                maxlength="100" />
            </div>
            <div class="field">
              <label class="field-label" for="review-release">上映日期</label>
              <input id="review-release" v-model="releaseDate" type="date" class="form-control" />
            </div>
          </div>
          <p v-if="needsNewMovie" class="field-hint">電影庫找不到的話，核准時會用這個名稱建立新電影。先搜尋看看，避免重複建立。上映日期只在核准時使用，按「只儲存」不會保留。</p>

          <h3 class="form-section">影城（已選 {{ selectedCinemaIds.length }} 間）</h3>
          <div class="field">
            <div class="inline-search">
              <input v-model="cinemaKeyword" type="search" class="form-control" placeholder="例如輸入「威秀」" />
              <AppButton type="button" color="purple" variant="outline" @click="selectFiltered">全選搜尋結果</AppButton>
              <AppButton type="button" color="purple" variant="outline" @click="selectedCinemaIds = []">清除</AppButton>
            </div>
            <p v-if="target.matchedCinemaIds" class="field-hint">已預先勾選爬蟲從原文判斷的影城，請對照原文確認。備註裡「未對應影城」的要自己找來勾。</p>
            <p v-if="selectedCinemaNames.length" class="selected-cinemas">
              已勾選：{{ selectedCinemaNames.join("、") }}
            </p>
            <div class="cinema-checks">
              <label v-for="cinema in filteredCinemas" :key="cinema.cinemaId" class="cinema-check">
                <input v-model="selectedCinemaIds" type="checkbox" :value="cinema.cinemaId" />
                {{ cinema.cinemaName }}
              </label>
            </div>
          </div>
        </template>
      </template>

      <!-- 按鈕：駁回放左邊、核准放右邊，分開避免按錯 -->
      <template #footer>
        <div v-if="rejectMode" class="footer-split">
          <AppButton type="button" color="purple" variant="outline" :icon="arrowLeftIcon" :disabled="submitting"
            @click="rejectMode = false">
            返回
          </AppButton>
          <button type="button" class="danger-button" :disabled="submitting" @click="submitReject">
            <Icon :icon="closeIcon" aria-hidden="true" />
            {{ submitting ? "處理中…" : "確定駁回" }}
          </button>
        </div>
        <div v-else class="footer-split">
          <button type="button" class="danger-link" :disabled="submitting" @click="rejectMode = true">
            <Icon :icon="closeIcon" aria-hidden="true" />
            駁回這筆
          </button>
          <div class="footer-right">
            <AppButton type="button" color="purple" variant="outline" :icon="saveIcon" :disabled="submitting"
              @click="saveOnly">
              只儲存
            </AppButton>
            <AppButton type="button" color="purple" :icon="checkIcon" :disabled="submitting" @click="submitApprove">
              {{ submitting ? submitStep || "處理中…" : "儲存並核准" }}
            </AppButton>
          </div>
        </div>
      </template>
    </AppModal>
  </section>
</template>

<style scoped>
.report-sheet {
  scroll-margin-top: 5.5rem;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
}

.row-actions :deep(.app-button) {
  min-height: 2.25rem;
  padding: 0.3rem 0.9rem;
  box-shadow: none;
}

.source-link {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  color: #8d72ed;
  font-weight: 800;
}

/* ── 審核視窗：上方摘要 ── */
.report-summary {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1.5px dashed var(--c-border);
}

/* ── 審核視窗：圖片＋補圖／換圖 ── */
.report-image {
  display: flex;
  flex: none;
  flex-direction: column;
  align-items: stretch;
  gap: 0.4rem;
  width: 7rem;
}

.image-frame {
  position: relative;
  display: block;
  width: 7rem;
  height: 10rem;
  overflow: hidden;
  border: 1.5px solid var(--c-border);
  border-radius: 12px;
  background: #f8f6ff;
}

.image-frame img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-empty {
  display: grid;
  place-items: center;
  color: var(--c-muted);
  font-size: var(--text-min);
  font-weight: 700;
}

.image-badge {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  padding: 0.15rem 0.3rem;
  background: rgb(141 114 237 / 90%);
  color: #fff;
  font-size: 0.85rem;
  font-weight: 800;
  text-align: center;
}

.image-pick {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  padding: 0.3rem 0.4rem;
  border: 1.5px solid #8d72ed;
  border-radius: 999px;
  background: #fff;
  color: #5c4696;
  font-size: var(--text-min);
  font-weight: 800;
  cursor: pointer;
}

.image-pick:hover,
.image-pick:focus-within {
  background: #8d72ed;
  color: #fff;
}

.image-pick.is-disabled {
  opacity: 0.5;
  cursor: wait;
}

/* 真正的 input 藏起來，點整個按鈕就會開檔案選擇 */
.image-pick input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.image-undo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.2rem;
  padding: 0.2rem;
  border: 0;
  background: none;
  color: var(--c-muted);
  font-size: var(--text-min);
  font-weight: 800;
  cursor: pointer;
}

.image-undo:hover:not(:disabled) {
  color: #a33b42;
  text-decoration: underline;
}

.image-error {
  color: #9f1239;
  font-size: var(--text-min);
  font-weight: 700;
}

.summary-text {
  display: grid;
  align-content: start;
  gap: 0.5rem;
  min-width: 0;
}

.summary-text p {
  margin: 0;
}

.summary-source {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  color: var(--c-muted);
  font-weight: 700;
}

.summary-source a {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  color: #8d72ed;
  font-weight: 800;
}

.summary-note {
  display: flex;
  gap: 0.4rem;
  padding: 0.5rem 0.75rem;
  border-radius: 12px;
  background: #f4f0ff;
  color: #5c4696;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.summary-note :deep(svg) {
  flex: none;
  margin-top: 0.2rem;
}

/* ── 表單 ── */
.form-section {
  margin: 1.1rem 0 0.6rem;
  padding-top: 0.9rem;
  border-top: 1.5px solid var(--c-border);
  color: #5c4696;
  font-size: var(--text-control);
  font-weight: 900;
}

.form-section:first-of-type {
  margin-top: 0;
  padding-top: 0;
  border-top: 0;
}

.field-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(13rem, 1fr));
  gap: 0 0.9rem;
}

.switch-field {
  display: flex;
  align-items: flex-end;
}

.date-hint {
  margin-top: -0.6rem !important;
  margin-bottom: 0.6rem !important;
}

.movie-current {
  margin: 0 0 0.5rem;
  font-weight: 800;
}

.movie-current.is-missing {
  color: #a33b42;
}

.new-movie {
  padding: 0.75rem 0.9rem 0;
  border-radius: 14px;
  background: #fffaf0;
}

.inline-search {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.inline-search .form-control {
  flex: 1 1 14rem;
}

.inline-search :deep(.app-button) {
  min-height: 2.9rem;
  box-shadow: none;
}

.choice-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-top: 0.6rem;
}

.choice {
  padding: 0.3rem 0.8rem;
  border: 1.5px solid #8d72ed;
  border-radius: 999px;
  background: #fff;
  color: #5c4696;
  font-size: var(--text-min);
  font-weight: 800;
  cursor: pointer;
}

.choice.active {
  background: #8d72ed;
  color: #fff;
}

.cinema-checks {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(13rem, 1fr));
  gap: 0.3rem 0.8rem;
  max-height: 14rem;
  margin-top: 0.6rem;
  padding: 0.6rem 0.8rem;
  overflow: auto;
  border: 1.5px solid var(--c-border);
  border-radius: 14px;
}

.cinema-check {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: var(--text-min);
  cursor: pointer;
}

/* ── 底部按鈕：駁回在左、核准在右 ── */
.footer-split {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.footer-right {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.danger-link {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.4rem 0.2rem;
  border: 0;
  background: none;
  color: #a33b42;
  font-size: var(--text-control);
  font-weight: 800;
  cursor: pointer;
}

.danger-link:hover:not(:disabled) {
  text-decoration: underline;
}

.danger-button {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  min-height: 2.9rem;
  padding: 0.4rem 1.2rem;
  border: 0;
  border-radius: 999px;
  background: #a33b42;
  color: #fff;
  font-size: var(--text-control);
  font-weight: 800;
  cursor: pointer;
}

.danger-link:disabled,
.danger-button:disabled,
.image-undo:disabled {
  opacity: 0.6;
  cursor: wait;
}

/* ── 相似特典 ── */
.similar-empty {
  margin: 0.5rem 0 0;
  color: #2f7a4f;
  font-weight: 800;
}

.similar-list {
  display: grid;
  gap: 0.5rem;
  max-height: 16rem;
  margin: 0.6rem 0 0;
  padding: 0;
  overflow: auto;
  list-style: none;
}

.similar-list li {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  padding: 0.4rem 0.6rem;
  border: 1.5px solid var(--c-border);
  border-radius: 12px;
}

.similar-list img,
.similar-noimg {
  flex: none;
  width: 3rem;
  height: 4.5rem;
  border-radius: 8px;
  object-fit: cover;
}

.similar-noimg {
  display: grid;
  place-items: center;
  background: #f1f1f4;
  color: var(--c-muted);
  font-size: var(--text-min);
}

.similar-text {
  display: grid;
  gap: 0.15rem;
  min-width: 0;
}

.similar-text a {
  color: #5c4696;
  font-weight: 800;
  overflow-wrap: anywhere;
}

.similar-text span {
  color: var(--c-muted);
  font-size: var(--text-min);
}

.selected-cinemas {
  margin: 0.4rem 0 0;
  padding: 0.5rem 0.75rem;
  border-radius: 12px;
  background: #f4f0ff;
  color: #5c4696;
  font-size: var(--text-min);
  font-weight: 700;
  overflow-wrap: anywhere;
}
</style>
