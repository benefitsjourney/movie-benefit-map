<script setup>
import { computed, ref } from "vue";
import { Icon } from "@iconify/vue";
import clipboardIcon from "@iconify-icons/mdi/clipboard-check-outline";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import tableIcon from "@iconify-icons/mdi/table-large";
import filterIcon from "@iconify-icons/mdi/filter-variant";
import checkIcon from "@iconify-icons/mdi/check";
import closeIcon from "@iconify-icons/mdi/close";
import searchIcon from "@iconify-icons/mdi/magnify";
import linkIcon from "@iconify-icons/mdi/open-in-new";

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

function apiMessage(error, fallback) {
  const message = error?.response?.data?.message;
  return typeof message === "string" && message.trim() ? message.trim() : fallback;
}

function showError(title, error, fallback = "請確認連線後再試一次。") {
  const code = error?.response?.status;
  const text = code === 401 ? "請先登入管理員帳號。" : code === 403 ? "這個帳號沒有管理員權限。" : apiMessage(error, fallback);
  return openAppAlert({ variant: "error", title, text, confirmText: "關閉", cancelText: "" });
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

// 電影／影城：有對應到資料庫就顯示編號，沒有就顯示回報單上的自訂名稱
function movieText(row) {
  if (row.movieId) return `#${row.movieId}`;
  return row.customMovieName ? `${row.customMovieName}（新）` : "";
}

function cinemaText(row) {
  if (row.cinemaId) return `#${row.cinemaId}`;
  return row.customCinemaName ? `${row.customCinemaName}（新）` : "";
}

const columns = [
  { key: "benefitReportId", label: "編號", minWidth: "5.5rem", nowrap: true, align: "center" },
  { key: "benefitName", label: "特典名稱", minWidth: "16rem", emphasis: true },
  { key: "movie", label: "電影", minWidth: "10rem", format: (_, row) => movieText(row) },
  { key: "benefitType", label: "類型", minWidth: "7rem", nowrap: true, align: "center", format: (v) => BENEFIT_TYPES[v] ?? "" },
  { key: "startDate", label: "開始日", minWidth: "11rem", nowrap: true, align: "center", format: dateTimeText },
  { key: "source", label: "來源", minWidth: "6.5rem", nowrap: true, align: "center", format: (_, row) => (row.userId ? `會員#${row.userId}` : "爬蟲") },
  { key: "reviewNote", label: "備註", minWidth: "16rem" },
  { key: "actions", label: "操作", minWidth: "14rem", align: "center" },
];

function goToPage(next) {
  if (!Number.isInteger(next) || next < 1 || next > totalPages.value || next === page.value) return;
  page.value = next;
  listTop.value?.scrollIntoView({ behavior: "smooth", block: "start" });
}

// ── 影城清單（核准時勾選用），第一次打開核准視窗才載入 ──
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

// ── 核准視窗 ──
const approveOpen = ref(false);
const approveTarget = ref(null);
const submitting = ref(false);

const movieKeyword = ref("");
const movieResults = ref([]);
const selectedMovie = ref(null); // { movieId, movieName }
const releaseDate = ref(""); // 電影庫沒有這部、要新建時才需要
const cinemaKeyword = ref("");
const selectedCinemaIds = ref([]);

async function openApprove(row) {
  approveTarget.value = row;
  movieKeyword.value = row.customMovieName ?? "";
  movieResults.value = [];
  selectedMovie.value = null;
  releaseDate.value = "";
  cinemaKeyword.value = "";
  // 回報單本來就有對應影城的話，先幫忙勾起來
  selectedCinemaIds.value = row.cinemaId ? [row.cinemaId] : [];
  approveOpen.value = true;
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
  } catch (e) {
    await showError("找不到電影", e);
  }
}

const filteredCinemas = computed(() => {
  const word = cinemaKeyword.value.trim();
  return word ? cinemas.value.filter((c) => String(c.cinemaName ?? "").includes(word)) : cinemas.value;
});

// 把目前搜尋結果全部勾起來（例如搜「威秀」再按這個＝全台威秀）
function selectFiltered() {
  const ids = new Set(selectedCinemaIds.value);
  filteredCinemas.value.forEach((c) => ids.add(c.cinemaId));
  selectedCinemaIds.value = [...ids];
}

// 電影：管理員指定 > 回報單原本的 > 用自訂名稱新建（要上映日）
const needsReleaseDate = computed(
  () => !selectedMovie.value && !approveTarget.value?.movieId && Boolean(approveTarget.value?.customMovieName),
);

// datetime-local 的 "2026-10-16T00:00" → API 要的 "2026-10-16 00:00:00"
function toApiDate(value) {
  return value ? `${value} 00:00:00` : null;
}

async function submitApprove() {
  const row = approveTarget.value;
  if (!row || submitting.value) return;

  if (needsReleaseDate.value && !releaseDate.value) {
    await openAppAlert({
      variant: "info",
      title: "請補上映日期",
      text: "電影庫沒有這部電影，核准時會建立新電影，需要上映日期。也可以改用搜尋指定現有的電影。",
      confirmText: "知道了",
      cancelText: "",
    });
    return;
  }

  // 只帶有填的欄位，沒填的讓後端用回報單原本的資料
  const body = {};
  if (selectedMovie.value) body.movieId = selectedMovie.value.movieId;
  if (needsReleaseDate.value) body.releaseDate = toApiDate(releaseDate.value);
  if (selectedCinemaIds.value.length) body.cinemaIds = selectedCinemaIds.value;

  submitting.value = true;
  try {
    await http.patch(`/benefit-reports/${row.benefitReportId}/approve`, body);
    approveOpen.value = false;
    await openAppAlert({ variant: "success", title: "已核准", text: `「${row.benefitName}」已上架。`, confirmText: "關閉", cancelText: "" });
    retry();
  } catch (e) {
    await showError("核准失敗", e);
  } finally {
    submitting.value = false;
  }
}

// ── 駁回視窗 ──
const rejectOpen = ref(false);
const rejectTarget = ref(null);
const rejectNote = ref("");

function openReject(row) {
  rejectTarget.value = row;
  rejectNote.value = "";
  rejectOpen.value = true;
}

async function submitReject() {
  const row = rejectTarget.value;
  if (!row || submitting.value) return;
  submitting.value = true;
  try {
    // reviewNote 沒填的話，後端會自動寫「審核未通過」
    const note = rejectNote.value.trim();
    await http.patch(`/benefit-reports/${row.benefitReportId}/reject`, note ? { reviewNote: note } : {});
    rejectOpen.value = false;
    await openAppAlert({ variant: "success", title: "已駁回", text: `「${row.benefitName}」已駁回。`, confirmText: "關閉", cancelText: "" });
    retry();
  } catch (e) {
    await showError("駁回失敗", e);
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <section>
    <PageIntro kicker="✦ BENEFIT REVIEW" lead="特典" mark="審核" description="審核爬蟲沒能自動上架、以及會員回報的特典。核准會建立正式特典並綁定影城。"
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
              <a v-if="httpUrl(row.sourceUrl)" class="source-link" :href="httpUrl(row.sourceUrl)" target="_blank"
                rel="noopener noreferrer" title="查看原文">
                <Icon :icon="linkIcon" aria-hidden="true" />
                原文
              </a>
              <template v-if="isPending">
                <AppButton type="button" color="purple" :icon="checkIcon" @click="openApprove(row)">核准</AppButton>
                <AppButton type="button" color="purple" variant="outline" :icon="closeIcon" @click="openReject(row)">駁回</AppButton>
              </template>
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

    <!-- 核准視窗 -->
    <AppModal v-model="approveOpen" :title="`核准：${approveTarget?.benefitName ?? ''}`" :busy="submitting" width="44rem">
      <template v-if="approveTarget">
        <div class="report-summary">
          <img v-if="httpUrl(approveTarget.officialImageUrl)" :src="httpUrl(approveTarget.officialImageUrl)" alt="特典圖片"
            referrerpolicy="no-referrer" />
          <dl>
            <div><dt>類型</dt><dd>{{ BENEFIT_TYPES[approveTarget.benefitType] ?? "—" }}</dd></div>
            <div><dt>期間</dt><dd>{{ dateTimeText(approveTarget.startDate) || "—" }} ～ {{ dateTimeText(approveTarget.endDate) || "送完為止" }}</dd></div>
            <div><dt>備註</dt><dd>{{ approveTarget.reviewNote || "—" }}</dd></div>
          </dl>
        </div>

        <!-- 電影 -->
        <div class="field">
          <span class="field-label">電影</span>
          <p class="field-hint">
            目前：{{ selectedMovie ? `${selectedMovie.movieName}（#${selectedMovie.movieId}）` : movieText(approveTarget) || "沒有電影" }}
          </p>
          <div class="inline-search">
            <input v-model="movieKeyword" type="search" class="form-control" placeholder="搜尋電影名稱，指定成資料庫裡的電影"
              @keydown.enter.prevent="searchMovies" />
            <AppButton type="button" color="purple" variant="outline" :icon="searchIcon" @click="searchMovies">搜尋</AppButton>
          </div>
          <div v-if="movieResults.length" class="choice-list">
            <button v-for="movie in movieResults" :key="movie.movieId" type="button" class="choice"
              :class="{ active: selectedMovie?.movieId === movie.movieId }" @click="selectedMovie = movie">
              {{ movie.movieName }}
            </button>
          </div>
        </div>

        <div v-if="needsReleaseDate" class="field">
          <label class="field-label" for="approve-release">上映日期（新建電影用）</label>
          <input id="approve-release" v-model="releaseDate" type="date" class="form-control" />
          <p class="field-hint">電影庫沒有「{{ approveTarget.customMovieName }}」，核准時會用這個名稱建立新電影。</p>
        </div>

        <!-- 影城 -->
        <div class="field">
          <span class="field-label">綁定影城（已選 {{ selectedCinemaIds.length }} 間）</span>
          <div class="inline-search">
            <input v-model="cinemaKeyword" type="search" class="form-control" placeholder="例如輸入「威秀」" />
            <AppButton type="button" color="purple" variant="outline" @click="selectFiltered">全選搜尋結果</AppButton>
            <AppButton type="button" color="purple" variant="outline" @click="selectedCinemaIds = []">清除</AppButton>
          </div>
          <div class="cinema-checks">
            <label v-for="cinema in filteredCinemas" :key="cinema.cinemaId" class="cinema-check">
              <input v-model="selectedCinemaIds" type="checkbox" :value="cinema.cinemaId" />
              {{ cinema.cinemaName }}
            </label>
          </div>
          <p class="field-hint">不勾選的話，會沿用回報單原本對應的影城；如果原本也沒有，後端會提示錯誤。</p>
        </div>
      </template>

      <template #footer>
        <AppButton type="button" color="purple" variant="outline" :disabled="submitting" @click="approveOpen = false">取消</AppButton>
        <AppButton type="button" color="purple" :icon="checkIcon" :disabled="submitting" @click="submitApprove">
          {{ submitting ? "處理中…" : "確定核准" }}
        </AppButton>
      </template>
    </AppModal>

    <!-- 駁回視窗 -->
    <AppModal v-model="rejectOpen" :title="`駁回：${rejectTarget?.benefitName ?? ''}`" :busy="submitting">
      <div class="field">
        <label class="field-label" for="reject-note">駁回理由（選填，會員看得到）</label>
        <textarea id="reject-note" v-model="rejectNote" class="form-control" rows="3" maxlength="100"
          placeholder="例如：非電影特典、與現有特典重複"></textarea>
        <p class="field-hint">沒填會顯示「審核未通過」。最多 100 字。</p>
      </div>

      <template #footer>
        <AppButton type="button" color="purple" variant="outline" :disabled="submitting" @click="rejectOpen = false">取消</AppButton>
        <AppButton type="button" color="purple" :icon="closeIcon" :disabled="submitting" @click="submitReject">
          {{ submitting ? "處理中…" : "確定駁回" }}
        </AppButton>
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
  gap: 0.4rem;
}

.row-actions :deep(.app-button) {
  min-height: 2.25rem;
  padding: 0.3rem 0.8rem;
  box-shadow: none;
}

.source-link {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  color: #8d72ed;
  font-weight: 800;
}

/* ── 核准視窗 ── */
.report-summary {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.1rem;
  padding-bottom: 1rem;
  border-bottom: 1.5px dashed var(--c-border);
}

.report-summary img {
  flex: none;
  width: 6rem;
  height: 9rem;
  border-radius: 12px;
  object-fit: cover;
}

.report-summary dl {
  display: grid;
  gap: 0.3rem;
  margin: 0;
}

.report-summary dl > div {
  display: grid;
  grid-template-columns: 3.5rem 1fr;
  gap: 0.5rem;
}

.report-summary dt {
  color: var(--c-muted);
  font-weight: 700;
}

.report-summary dd {
  margin: 0;
  overflow-wrap: anywhere;
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
</style>
