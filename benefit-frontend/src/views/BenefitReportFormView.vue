<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { Icon } from "@iconify/vue";
import clipboardIcon from "@iconify-icons/mdi/clipboard-text-outline";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import searchIcon from "@iconify-icons/mdi/magnify";
import sendIcon from "@iconify-icons/mdi/send-outline";
import closeIcon from "@iconify-icons/mdi/close";
import filmIcon from "@iconify-icons/mdi/filmstrip";
import cameraIcon from "@iconify-icons/mdi/camera-plus-outline";

import http from "@/plugins/axios";
import { CINEMA_SORT_DEFAULT } from "@/constants/sort";
import { openAppAlert } from "@/components/ui/Alert.vue";
import { httpUrl } from "@/components/ui/DataTable.vue";
import PageIntro from "@/components/layout/PageIntro.vue";
import Form from "@/components/ui/Form.vue";
import AppButton from "@/components/ui/Button.vue";

const router = useRouter();

const BENEFIT_TYPES = [
  { value: "REGULAR", label: "一般場次（固定一週）" },
  { value: "SPECIAL_BY_DATE", label: "入場贈送（指定日期入場就送）" },
  { value: "SPECIAL_UNTIL_OUT", label: "限量兌換（送完為止）" },
];

const MAX_CINEMAS = 100; // 跟後端 BenefitReportCreateRequest 的上限一樣

// 圖片限制跟收藏庫、後端 BenefitReportImageService 一樣
const MAX_IMAGE_BYTES = 10 * 1024 * 1024;
const IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp", "image/gif"];

// ── 電影：打字搜尋後點選（會員不能用 GET /movies，改用前台的 /movies/explore）──
const moviePicker = reactive({
  keyword: "",
  options: [],
  loading: false,
  searched: false,
  selected: null, // { id, label, sub }
  custom: false, // true＝資料庫找不到，自己填名稱
  customName: "",
});

let movieTimer = 0;
let movieSerial = 0; // 打字很快時，只採用最後一次的結果

watch(
  () => moviePicker.keyword,
  (word) => {
    window.clearTimeout(movieTimer);
    const text = word.trim();
    if (!text) {
      movieSerial += 1;
      moviePicker.options = [];
      moviePicker.loading = false;
      moviePicker.searched = false;
      return;
    }
    // 停止打字 0.3 秒後才搜尋，避免每打一個字就打一次 API
    movieTimer = window.setTimeout(async () => {
      const mine = ++movieSerial;
      moviePicker.loading = true;
      try {
        const { data } = await http.get("/movies/explore", {
          params: { keyword: text, page: 1, size: 8, orderBy: "RELEASE_DATE", sort: "DESC" },
        });
        if (mine !== movieSerial) return;
        moviePicker.options = (Array.isArray(data?.content) ? data.content : []).map((m) => ({
          id: m.movieId,
          label: m.movieName,
          sub: m.releaseDate ? `${String(m.releaseDate).slice(0, 4)} 年上映` : "",
        }));
      } catch {
        if (mine === movieSerial) moviePicker.options = [];
      } finally {
        if (mine === movieSerial) {
          moviePicker.loading = false;
          moviePicker.searched = true;
        }
      }
    }, 300);
  },
);

function chooseMovie(option) {
  moviePicker.selected = option;
  moviePicker.options = [];
}

// ── 影城：跟審核頁一樣，搜尋＋勾選（可多選）──
// 會員不能用 GET /cinemas，改用前台的 /cinemas/explore，一頁一頁拿到最後一頁
const cinemas = ref([]);
const cinemasLoading = ref(true);
const cinemasFailed = ref(false);
const cinemaKeyword = ref("");
const selectedCinemaIds = ref([]);
const customCinemaName = ref("");

async function loadCinemas() {
  cinemasLoading.value = true;
  cinemasFailed.value = false;
  try {
    const all = [];
    let current = 1;
    let pages = 1;
    do {
      const { data } = await http.get("/cinemas/explore", {
        params: {
          page: current,
          size: 100,
          orderBy: CINEMA_SORT_DEFAULT.orderBy, // 依縣市北到南
          sort: CINEMA_SORT_DEFAULT.sort,
        },
      });
      all.push(...(Array.isArray(data?.content) ? data.content : []));
      pages = Number(data?.totalPages) || 1;
      current += 1;
    } while (current <= pages);
    cinemas.value = all;
  } catch {
    cinemasFailed.value = true;
  } finally {
    cinemasLoading.value = false;
  }
}

const filteredCinemas = computed(() => {
  const word = cinemaKeyword.value.trim();
  return word ? cinemas.value.filter((c) => String(c.cinemaName ?? "").includes(word)) : cinemas.value;
});

// 已勾選的影城名稱，方便一眼確認勾了哪些
const selectedCinemaNames = computed(() =>
  cinemas.value.filter((c) => selectedCinemaIds.value.includes(c.cinemaId)).map((c) => c.cinemaName),
);

// 把目前搜尋結果全部勾起來（例如搜「威秀」再按這個＝全台威秀）
function selectFiltered() {
  const ids = new Set(selectedCinemaIds.value);
  filteredCinemas.value.forEach((c) => ids.add(c.cinemaId));
  selectedCinemaIds.value = [...ids];
  clearError("cinema");
}

function clearCinemas() {
  selectedCinemaIds.value = [];
}

// ── 特典圖片（選填，一張）──
// 選圖時只在瀏覽器預覽，按「送出回報」時才真的上傳，避免選了又不送出卻在 Cloudinary 留下沒用的圖
const imageFile = ref(null);
const imagePreview = ref(""); // 瀏覽器暫時網址（blob:），只給預覽用
const imageError = ref("");
// 同一張圖已經上傳過就記下來；送出失敗再按一次時直接沿用，不重複上傳
let uploadedImage = null; // { url, publicId }

function clearImage() {
  if (imagePreview.value) URL.revokeObjectURL(imagePreview.value);
  imageFile.value = null;
  imagePreview.value = "";
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

  clearImage();
  imageFile.value = file;
  imagePreview.value = URL.createObjectURL(file);
}

// POST /benefit-reports/images：回傳 { url, publicId }
async function uploadImage() {
  if (!imageFile.value) return null;
  if (uploadedImage) return uploadedImage;
  const data = new FormData();
  data.append("file", imageFile.value);
  const { data: result } = await http.post("/benefit-reports/images", data);
  uploadedImage = { url: result.url, publicId: result.publicId };
  return uploadedImage;
}

// ── 其他欄位 ──
const form = reactive(emptyForm());

function emptyForm() {
  return {
    benefitName: "",
    isMemberOnly: false,
    benefitType: "",
    startDate: "",
    endDate: "",
    sourceUrl: "",
  };
}

const fieldErrors = ref({});

function clearError(key) {
  if (!fieldErrors.value[key]) return;
  const next = { ...fieldErrors.value };
  delete next[key];
  fieldErrors.value = next;
}

function validate() {
  const errors = {};
  const name = form.benefitName.trim();
  if (!name) errors.benefitName = "請填寫特典名稱";
  else if ([...name].length > 100) errors.benefitName = "特典名稱不可超過 100 字";

  if (moviePicker.custom) {
    const movieName = moviePicker.customName.trim();
    if (!movieName) errors.movie = "請填寫電影名稱";
    else if ([...movieName].length > 100) errors.movie = "電影名稱不可超過 100 字";
  } else if (!moviePicker.selected) {
    errors.movie = "請選擇電影，找不到可以勾選「自己填名稱」";
  }

  const otherCinema = customCinemaName.value.trim();
  if (!selectedCinemaIds.value.length && !otherCinema) {
    errors.cinema = "請勾選至少一間影城，或填寫清單裡沒有的影城名稱";
  } else if (selectedCinemaIds.value.length > MAX_CINEMAS) {
    errors.cinema = `影城最多選 ${MAX_CINEMAS} 間`;
  } else if ([...otherCinema].length > 100) {
    errors.cinema = "影城名稱不可超過 100 字";
  }

  if (!form.startDate) errors.startDate = "請選擇開始日期";
  // 日期格式都是 yyyy-MM-dd，可以直接比字串大小
  if (form.startDate && form.endDate && form.endDate < form.startDate) {
    errors.endDate = "結束日期不可早於開始日期";
  }

  const url = form.sourceUrl.trim();
  if (url && !httpUrl(url)) errors.sourceUrl = "請輸入 http:// 或 https:// 開頭的網址";
  else if (url.length > 300) errors.sourceUrl = "網址不可超過 300 字";

  return errors;
}

// 後端要 "2026-10-16 00:00:00"；結束日用當天最後一刻（跟審核頁一樣）
function toApiDate(value, endOfDay = false) {
  if (!value) return null;
  return `${value} ${endOfDay ? "23:59:59" : "00:00:00"}`;
}

function payload(image) {
  return {
    benefitName: form.benefitName.trim(),
    isMemberOnly: form.isMemberOnly,
    benefitType: form.benefitType || null,
    startDate: toApiDate(form.startDate),
    endDate: toApiDate(form.endDate, true),
    // 電影二選一：選了就帶 id，自己填就帶名稱
    movieId: moviePicker.custom ? null : moviePicker.selected?.id ?? null,
    customMovieName: moviePicker.custom ? moviePicker.customName.trim() : null,
    // 影城：勾選的放 cinemaIds（可多間），清單沒有的放 customCinemaName
    cinemaIds: selectedCinemaIds.value,
    customCinemaName: customCinemaName.value.trim() || null,
    sourceUrl: form.sourceUrl.trim() || null,
    // 圖片：沒選就是 null
    officialImageUrl: image?.url ?? null,
    publicId: image?.publicId ?? null,
  };
}

function resetAll() {
  Object.assign(form, emptyForm());
  moviePicker.keyword = "";
  moviePicker.selected = null;
  moviePicker.custom = false;
  moviePicker.customName = "";
  cinemaKeyword.value = "";
  selectedCinemaIds.value = [];
  customCinemaName.value = "";
  clearImage();
  fieldErrors.value = {};
}

function errorText(error, fallback) {
  const code = error?.response?.status;
  const message = error?.response?.data?.message;
  if (code === 401) return "登入已過期，請重新登入。";
  if (code === 403) return "此帳號目前無法回報。";
  return typeof message === "string" && message.trim() ? message.trim() : fallback;
}

// ── 送出：先上傳圖片（有選的話），再 POST /benefit-reports ──
const submitting = ref(false);
const submitStep = ref(""); // 按鈕上顯示目前在做什麼

async function submit() {
  if (submitting.value) return;

  const errors = validate();
  fieldErrors.value = errors;
  if (Object.keys(errors).length) {
    await openAppAlert({
      variant: "error",
      title: "還有欄位沒填好",
      text: Object.values(errors).join("、"),
      confirmText: "關閉",
      cancelText: "",
    });
    return;
  }

  submitting.value = true;
  try {
    // 1. 上傳圖片（失敗就停在這裡，不送出回報）
    let image = null;
    if (imageFile.value) {
      submitStep.value = "上傳圖片中…";
      try {
        image = await uploadImage();
      } catch (error) {
        await openAppAlert({
          variant: "error",
          title: "圖片上傳失敗",
          text: errorText(error, "請稍後再試，或先移除圖片再送出。"),
          confirmText: "關閉",
          cancelText: "",
        });
        return;
      }
    }

    // 2. 送出回報
    submitStep.value = "送出中…";
    await http.post("/benefit-reports", payload(image));
    const result = await openAppAlert({
      variant: "success",
      title: "感謝你的回報！",
      text: "管理員審核通過後，這個特典就會出現在特典一覽。審核結果可以在「我的回報」查看。",
      confirmText: "查看我的回報",
      cancelText: "再回報一筆",
    });
    resetAll();
    if (result.isConfirmed) {
      router.push({ path: "/my-reports", query: { tab: "benefit" } });
    }
  } catch (error) {
    await openAppAlert({
      variant: "error",
      title: "無法送出",
      text: errorText(error, "送出失敗，請稍後再試。"),
      confirmText: "關閉",
      cancelText: "",
    });
  } finally {
    submitting.value = false;
    submitStep.value = "";
  }
}

onMounted(loadCinemas);
onBeforeUnmount(() => {
  window.clearTimeout(movieTimer);
  if (imagePreview.value) URL.revokeObjectURL(imagePreview.value);
});
</script>

<template>
  <section>
    <PageIntro kicker="✦ REPORT A BENEFIT" lead="發現" mark="新特典" tail="了嗎？" description="特典一覽裡找不到的特典，告訴我們，審核通過後就會上架。"
      :icon-left="giftIcon" :icon-right="clipboardIcon" :icon-top="searchIcon" />

    <Form title="缺漏特典回報" :title-icon="clipboardIcon" :submitting="submitting" submit-label="送出回報"
      :submitting-label="submitStep || '送出中…'" :submit-icon="sendIcon" @submit="submit">
      <!-- 特典資訊 -->
      <section class="form-section">
        <h3 class="form-section-title">特典資訊</h3>

        <div class="form-field is-wide">
          <label class="field-label" for="report-benefit-name">
            特典名稱 <span class="field-required" aria-hidden="true">*</span>
          </label>
          <input id="report-benefit-name" v-model="form.benefitName" class="form-control"
            :class="{ 'is-invalid': fieldErrors.benefitName }" type="text" maxlength="100"
            placeholder="例如：第二週 A5 透卡" @input="clearError('benefitName')" />
          <p v-if="fieldErrors.benefitName" class="field-error">{{ fieldErrors.benefitName }}</p>
        </div>

        <div class="form-field">
          <span class="field-label">
            會員限定 <span class="field-required" aria-hidden="true">*</span>
          </span>
          <div class="radio-row" role="radiogroup" aria-label="會員限定">
            <label class="radio-option">
              <input v-model="form.isMemberOnly" type="radio" :value="false" />
              否，一般觀眾都能領
            </label>
            <label class="radio-option">
              <input v-model="form.isMemberOnly" type="radio" :value="true" />
              是，要影城會員
            </label>
          </div>
        </div>

        <div class="form-field">
          <label class="field-label" for="report-benefit-type">特典類型</label>
          <select id="report-benefit-type" v-model="form.benefitType" class="form-select">
            <option value="">不確定</option>
            <option v-for="type in BENEFIT_TYPES" :key="type.value" :value="type.value">{{ type.label }}</option>
          </select>
          <p class="field-hint">不確定可以不選，管理員審核時會補上。</p>
        </div>

        <!-- 特典圖片（選填） -->
        <div class="form-field is-wide">
          <span class="field-label">特典圖片</span>

          <div v-if="imagePreview" class="image-preview">
            <img :src="imagePreview" alt="已選擇的特典圖片預覽" />
            <div class="image-preview-info">
              <span class="image-name">{{ imageFile?.name }}</span>
              <button type="button" class="picked-clear" :disabled="submitting" @click="clearImage">
                <Icon :icon="closeIcon" aria-hidden="true" />
                移除
              </button>
            </div>
          </div>

          <label v-else class="image-pick">
            <Icon :icon="cameraIcon" aria-hidden="true" />
            <span>選擇圖片</span>
            <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" @change="onImageChange" />
          </label>

          <p v-if="imageError" class="field-error">{{ imageError }}</p>
          <p v-else class="field-hint">選填，一張。JPG、PNG、WEBP、GIF，10MB 以內，例如官方公告的特典圖。</p>
        </div>
      </section>

      <!-- 電影 -->
      <section class="form-section">
        <h3 class="form-section-title">哪部電影</h3>

        <div class="form-field is-wide">
          <span class="field-label">
            電影 <span class="field-required" aria-hidden="true">*</span>
          </span>

          <!-- 自己填名稱 -->
          <input v-if="moviePicker.custom" v-model="moviePicker.customName" class="form-control"
            :class="{ 'is-invalid': fieldErrors.movie }" type="text" maxlength="100" placeholder="請輸入電影名稱"
            aria-label="電影名稱" @input="clearError('movie')" />

          <!-- 已選好 -->
          <div v-else-if="moviePicker.selected" class="picked">
            <Icon :icon="filmIcon" aria-hidden="true" />
            <span class="picked-name">{{ moviePicker.selected.label }}</span>
            <span v-if="moviePicker.selected.sub" class="picked-sub">{{ moviePicker.selected.sub }}</span>
            <button type="button" class="picked-clear" @click="moviePicker.selected = null">
              <Icon :icon="closeIcon" aria-hidden="true" />
              更換
            </button>
          </div>

          <!-- 搜尋中 -->
          <template v-else>
            <div class="search-box">
              <Icon :icon="searchIcon" aria-hidden="true" />
              <!-- Enter 不要送出整張表單 -->
              <input v-model="moviePicker.keyword" class="form-control" :class="{ 'is-invalid': fieldErrors.movie }"
                type="search" placeholder="輸入電影名稱搜尋" aria-label="搜尋電影" @keydown.enter.prevent
                @input="clearError('movie')" />
            </div>

            <p v-if="moviePicker.loading" class="search-status">搜尋中…</p>
            <ul v-else-if="moviePicker.options.length" class="search-results">
              <li v-for="option in moviePicker.options" :key="option.id">
                <button type="button" class="search-option" @click="chooseMovie(option)">
                  <span class="option-name">{{ option.label }}</span>
                  <span v-if="option.sub" class="option-sub">{{ option.sub }}</span>
                </button>
              </li>
            </ul>
            <p v-else-if="moviePicker.searched" class="search-status">
              找不到符合的電影，可以勾選下方「自己填名稱」。
            </p>
          </template>

          <label class="custom-toggle">
            <input v-model="moviePicker.custom" type="checkbox" @change="clearError('movie')" />
            資料庫裡找不到，自己填名稱
          </label>
          <p v-if="fieldErrors.movie" class="field-error">{{ fieldErrors.movie }}</p>
        </div>
      </section>

      <!-- 影城：跟審核頁同樣的勾選方式 -->
      <section class="form-section">
        <h3 class="form-section-title">
          哪些影城有發 <span class="title-count">（已選 {{ selectedCinemaIds.length }} 間）</span>
        </h3>

        <div class="form-field is-wide">
          <p v-if="cinemasLoading" class="search-status">正在載入影城清單…</p>
          <div v-else-if="cinemasFailed" class="load-failed">
            <span>影城清單載入失敗。</span>
            <AppButton type="button" variant="outline" @click="loadCinemas">重新載入</AppButton>
          </div>

          <template v-else>
            <div class="inline-search">
              <div class="search-box">
                <Icon :icon="searchIcon" aria-hidden="true" />
                <input v-model="cinemaKeyword" type="search" class="form-control" placeholder="例如輸入「威秀」"
                  aria-label="搜尋影城" @keydown.enter.prevent />
              </div>
              <AppButton type="button" variant="outline" @click="selectFiltered">全選搜尋結果</AppButton>
              <AppButton type="button" variant="outline" @click="clearCinemas">清除</AppButton>
            </div>
            <p class="field-hint">全台都有發的話，可以搜「威秀」再按「全選搜尋結果」。</p>

            <p v-if="selectedCinemaNames.length" class="selected-cinemas">
              已勾選：{{ selectedCinemaNames.join("、") }}
            </p>

            <div class="cinema-checks" :class="{ 'is-invalid': fieldErrors.cinema }">
              <label v-for="cinema in filteredCinemas" :key="cinema.cinemaId" class="cinema-check">
                <input v-model="selectedCinemaIds" type="checkbox" :value="cinema.cinemaId"
                  @change="clearError('cinema')" />
                {{ cinema.cinemaName }}
              </label>
              <p v-if="!filteredCinemas.length" class="search-status">找不到符合的影城。</p>
            </div>
          </template>
        </div>

        <div class="form-field is-wide">
          <label class="field-label" for="report-other-cinema">清單裡沒有的影城（選填）</label>
          <input id="report-other-cinema" v-model="customCinemaName" class="form-control" type="text" maxlength="100"
            placeholder="例如：某某獨立影院" @input="clearError('cinema')" />
          <p v-if="fieldErrors.cinema" class="field-error">{{ fieldErrors.cinema }}</p>
        </div>
      </section>

      <!-- 日期與來源 -->
      <section class="form-section">
        <h3 class="form-section-title">發放期間與來源</h3>

        <div class="form-field">
          <label class="field-label" for="report-start">
            開始日期 <span class="field-required" aria-hidden="true">*</span>
          </label>
          <input id="report-start" v-model="form.startDate" class="form-control"
            :class="{ 'is-invalid': fieldErrors.startDate }" type="date" @input="clearError('startDate')" />
          <p v-if="fieldErrors.startDate" class="field-error">{{ fieldErrors.startDate }}</p>
        </div>

        <div class="form-field">
          <label class="field-label" for="report-end">結束日期</label>
          <input id="report-end" v-model="form.endDate" class="form-control" :class="{ 'is-invalid': fieldErrors.endDate }"
            type="date" :min="form.startDate || undefined" @input="clearError('endDate')" />
          <p v-if="fieldErrors.endDate" class="field-error">{{ fieldErrors.endDate }}</p>
          <p v-else class="field-hint">送完為止或不確定可以不填。</p>
        </div>

        <div class="form-field is-wide">
          <label class="field-label" for="report-source">資料來源網址</label>
          <input id="report-source" v-model="form.sourceUrl" class="form-control"
            :class="{ 'is-invalid': fieldErrors.sourceUrl }" type="url" maxlength="300" placeholder="https://"
            @input="clearError('sourceUrl')" />
          <p v-if="fieldErrors.sourceUrl" class="field-error">{{ fieldErrors.sourceUrl }}</p>
          <p v-else class="field-hint">例如影城官方 FB 貼文，方便管理員確認。</p>
        </div>
      </section>
    </Form>
  </section>
</template>

<style scoped>
/* ── 會員限定 ── */
.radio-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 1.25rem;
  min-height: 3rem;
  align-items: center;
}

.radio-option,
.custom-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  font-size: var(--text-control);
  font-weight: 700;
  cursor: pointer;
}

.radio-option input,
.custom-toggle input,
.cinema-check input {
  width: 1.1rem;
  height: 1.1rem;
  accent-color: #ff7049;
}

.custom-toggle {
  margin-top: 0.5rem;
  color: var(--c-muted);
}

/* ── 特典圖片 ── */
.image-pick {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  min-height: 3rem;
  padding: 0.5rem 1.1rem;
  border: 1.5px dashed #c9b8a4;
  border-radius: 14px;
  background: #fffaf5;
  color: #8a6400;
  font-size: var(--text-control);
  font-weight: 800;
  cursor: pointer;
}

.image-pick:hover,
.image-pick:focus-within {
  border-color: #ff7049;
  color: #ff7049;
}

.image-pick > :deep(svg) {
  font-size: var(--text-card-title);
}

/* 真正的 input 藏起來，點整個方塊就會開檔案選擇 */
.image-pick input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.image-preview {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 0.75rem 1rem;
}

.image-preview img {
  width: 10rem;
  max-height: 14rem;
  border: 1.5px solid var(--c-border);
  border-radius: 14px;
  background: #fffbeb;
  object-fit: contain;
}

.image-preview-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.45rem;
  min-width: 0;
}

.image-name {
  color: var(--c-muted);
  font-size: var(--text-min);
  font-weight: 700;
  overflow-wrap: anywhere;
}

/* ── 搜尋框 ── */
.search-box {
  position: relative;
  flex: 1 1 16rem;
}

.search-box > :deep(svg) {
  position: absolute;
  top: 50%;
  left: 0.85rem;
  color: #ff7049;
  font-size: var(--text-card-title);
  transform: translateY(-50%);
  pointer-events: none;
}

.search-box .form-control {
  padding-left: 2.7rem;
}

.search-status {
  margin: 0.45rem 0 0;
  color: var(--c-muted);
  font-size: var(--text-min);
  font-weight: 700;
}

.search-results {
  display: grid;
  gap: 0.35rem;
  margin: 0.45rem 0 0;
  padding: 0.4rem;
  border: 1.5px solid var(--c-border);
  border-radius: 14px;
  background: #fff;
  list-style: none;
}

.search-option {
  display: flex;
  width: 100%;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.5rem 0.75rem;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--c-ink);
  font-size: var(--text-control);
  text-align: left;
  cursor: pointer;
}

.search-option:hover,
.search-option:focus-visible {
  background: #fff1e8;
  outline: none;
}

.option-name {
  font-weight: 800;
  overflow-wrap: anywhere;
}

.option-sub {
  flex: none;
  color: var(--c-muted);
  font-size: var(--text-min);
}

/* ── 已選好的電影 ── */
.picked {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  min-height: 3rem;
  padding: 0.45rem 0.9rem;
  border: 1.5px solid #ff7049;
  border-radius: 14px;
  background: #fff8f5;
}

.picked > :deep(svg) {
  color: #ff7049;
  font-size: var(--text-card-title);
}

.picked-name {
  font-weight: 800;
  overflow-wrap: anywhere;
}

.picked-sub {
  color: var(--c-muted);
  font-size: var(--text-min);
}

.picked-clear {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  margin-left: auto;
  padding: 0.2rem 0.7rem;
  border: 1.5px solid #ff7049;
  border-radius: 999px;
  background: #fff;
  color: #ff7049;
  font-size: var(--text-min);
  font-weight: 800;
  cursor: pointer;
}

.picked-clear:hover:not(:disabled) {
  background: #ff7049;
  color: #fff;
}

.picked-clear:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.image-preview-info .picked-clear {
  margin-left: 0;
}

/* ── 影城勾選 ── */
.title-count {
  color: #ff7049;
  font-size: var(--text-control);
}

.inline-search {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.6rem;
}

.selected-cinemas {
  margin: 0.6rem 0 0;
  padding: 0.5rem 0.8rem;
  border-radius: 12px;
  background: #fff1e8;
  color: #9a4632;
  font-size: var(--text-min);
  font-weight: 700;
  line-height: 1.6;
}

.cinema-checks {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(12rem, 1fr));
  gap: 0.35rem 1rem;
  max-height: 18rem;
  margin-top: 0.6rem;
  padding: 0.75rem;
  overflow-y: auto;
  border: 1.5px solid var(--c-border);
  border-radius: 14px;
  background: #fff;
}

.cinema-checks.is-invalid {
  border-color: #e11d48;
}

.cinema-check {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  font-size: var(--text-control);
  cursor: pointer;
}

.load-failed {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  color: #a33b42;
  font-weight: 700;
}
</style>
