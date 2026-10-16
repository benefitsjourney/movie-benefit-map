<script setup>
import { computed, reactive, ref, useId } from "vue";
import formIcon from "@iconify-icons/mdi/form-textbox";
import plusIcon from "@iconify-icons/mdi/plus";
import { openAppAlert } from "@/components/ui/Alert.vue";
import Form from "@/components/ui/Form.vue";
import Switch from "@/components/ui/Switch.vue";

const fieldId = useId();

const props = defineProps({
  mode: {
    type: String,
    default: "create",
    validator: (value) => value === "create" || value === "edit",
  },
  movie: { type: Object, default: null },
  submitting: { type: Boolean, default: false },
  plain: { type: Boolean, default: false },
  hideSubmit: { type: Boolean, default: false },
  formId: { type: String, default: undefined },
  /** 關閉時不顯示或送出 isVisible，由外層頁面獨立管理。 */
  showVisibility: { type: Boolean, default: true },
  submitLabel: { type: String, default: "" },
  submitIcon: { type: [Object, String], default: null },
});

const emit = defineEmits(["submit"]);

const fieldErrors = ref({});
const form = reactive(emptyForm());

const resolvedSubmitLabel = computed(() => {
  if (props.submitLabel) return props.submitLabel;
  return props.mode === "edit" ? "儲存" : "新增電影";
});

const resolvedSubmitIcon = computed(() => props.submitIcon ?? plusIcon);

const releaseHint = computed(() =>
  props.mode === "edit" ? "留空則維持原本的上映時間。" : "留空則不填上映時間。",
);

applyMovie(props.movie);

function emptyForm() {
  return {
    sourceId: "",
    movieName: "",
    imageUrl: "",
    releaseDate: "",
    isVisible: true,
  };
}

function textOf(value) {
  return value == null ? "" : String(value);
}

// datetime-local 與 API 使用不同的日期分隔格式。
function toDateTimeLocal(value) {
  const matched = textOf(value)
    .trim()
    .match(/^(\d{4}-\d{2}-\d{2})[ T](\d{2}:\d{2})(?::(\d{2}))?/);
  return matched ? `${matched[1]}T${matched[2]}:${matched[3] ?? "00"}` : "";
}

function toApiDateTime(value) {
  const matched = String(value ?? "")
    .trim()
    .match(/^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2})(?::(\d{2}))?$/);
  return matched ? `${matched[1]} ${matched[2]}:${matched[3] ?? "00"}` : null;
}

function applyMovie(movie) {
  // 詳情頁以 key 重掛表單，避免外部更新覆蓋使用者正在輸入的草稿。
  const next = props.mode === "edit"
    ? {
      sourceId: textOf(movie?.sourceId),
      movieName: textOf(movie?.movieName),
      imageUrl: textOf(movie?.imageUrl),
      releaseDate: toDateTimeLocal(movie?.releaseDate),
      isVisible: movie?.isVisible !== false,
    }
    : emptyForm();
  form.sourceId = next.sourceId;
  form.movieName = next.movieName;
  form.imageUrl = next.imageUrl;
  form.releaseDate = next.releaseDate;
  form.isVisible = next.isVisible;
  fieldErrors.value = {};
}

function releaseDateError(value) {
  const text = String(value ?? "").trim();
  if (!text) return "";
  const matched = text.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/);
  if (!matched) return "日期時間格式錯誤，應為 yyyy-MM-dd HH:mm:ss";

  const year = Number(matched[1]);
  const month = Number(matched[2]);
  const day = Number(matched[3]);
  const hour = Number(matched[4]);
  const minute = Number(matched[5]);
  const second = Number(matched[6] ?? "0");
  const date = new Date(year, month - 1, day);
  if (
    date.getFullYear() !== year ||
    date.getMonth() !== month - 1 ||
    date.getDate() !== day
  ) {
    return "日期不存在";
  }
  if (hour > 23 || minute > 59 || second > 59) return "時間不存在";
  return "";
}

function validate() {
  const errors = {};
  const movieName = form.movieName.trim();
  const sourceId = form.sourceId.trim();
  const imageUrl = form.imageUrl.trim();

  if (!movieName) errors.movieName = "電影名稱不能為空";
  else if ([...movieName].length > 256) errors.movieName = "電影名稱長度不可超過256字元";

  if ([...sourceId].length > 64) errors.sourceId = "來源ID長度不可超過64字元";
  if ([...imageUrl].length > 512) errors.imageUrl = "電影圖片長度不可超過512字元";

  const releaseDate = releaseDateError(form.releaseDate);
  if (releaseDate) errors.releaseDate = releaseDate;

  return errors;
}

function inputId(key) {
  return `${fieldId}-${key}`;
}

function errorId(key) {
  return `${fieldId}-${key}-error`;
}

function hintId(key) {
  return `${fieldId}-${key}-hint`;
}

function describedBy(key, hint = false) {
  const ids = [];
  if (hint) ids.push(hintId(key));
  if (fieldErrors.value[key]) ids.push(errorId(key));
  return ids.join(" ") || undefined;
}

function clearFieldError(key) {
  if (!fieldErrors.value[key]) return;
  const next = { ...fieldErrors.value };
  delete next[key];
  fieldErrors.value = next;
}

function payload() {
  const body = {
    sourceId: form.sourceId.trim() || null,
    movieName: form.movieName.trim(),
    imageUrl: form.imageUrl.trim(),
    releaseDate: toApiDateTime(form.releaseDate),
  };
  // 詳情頁另有即時開關，編輯其他欄位時不得連帶覆蓋顯示狀態。
  if (props.showVisibility) body.isVisible = Boolean(form.isVisible);
  return body;
}

async function save() {
  if (props.submitting) return;

  const errors = validate();
  fieldErrors.value = errors;
  const firstInvalid = Object.keys(errors)[0];
  if (firstInvalid) {
    await openAppAlert({
      variant: "error",
      title: "無法儲存",
      text: Object.values(errors).join("、"),
      confirmText: "關閉",
      cancelText: "",
    });
    document.getElementById(inputId(firstInvalid))?.focus();
    return;
  }

  emit("submit", payload());
}
</script>

<template>
  <Form :title="plain ? '' : (mode === 'edit' ? '修改電影資料' : '新增電影資料')" :title-icon="plain ? null : formIcon"
    :submitting="submitting" :submit-label="resolvedSubmitLabel" :submit-icon="resolvedSubmitIcon" :plain="plain"
    :hide-submit="hideSubmit" :form-id="formId" theme="purple" @submit="save">
    <template v-if="$slots.back" #back>
      <slot name="back" />
    </template>
    <template v-if="$slots.actions" #actions>
      <slot name="actions" />
    </template>

    <section class="form-section">
      <h3 class="form-section-title">基本資料</h3>

      <div class="form-field is-wide">
        <label class="field-label" :for="inputId('movieName')">
          電影名稱 <span class="field-required" aria-hidden="true">*</span>
        </label>
        <input :id="inputId('movieName')" v-model="form.movieName" class="form-control"
          :class="{ 'is-invalid': fieldErrors.movieName }" type="text" maxlength="256" autocomplete="off" required
          :aria-invalid="fieldErrors.movieName ? 'true' : 'false'" :aria-describedby="describedBy('movieName')"
          @input="clearFieldError('movieName')" />
        <p v-if="fieldErrors.movieName" :id="errorId('movieName')" class="field-error">
          {{ fieldErrors.movieName }}
        </p>
      </div>

      <div class="form-field">
        <label class="field-label" :for="inputId('sourceId')">來源編號</label>
        <input :id="inputId('sourceId')" v-model="form.sourceId" class="form-control"
          :class="{ 'is-invalid': fieldErrors.sourceId }" type="text" maxlength="64" autocomplete="off"
          :aria-invalid="fieldErrors.sourceId ? 'true' : 'false'" :aria-describedby="describedBy('sourceId', true)"
          @input="clearFieldError('sourceId')" />
        <p :id="hintId('sourceId')" class="field-hint">爬蟲來源的識別碼，不是電影在系統內的編號。</p>
        <p v-if="fieldErrors.sourceId" :id="errorId('sourceId')" class="field-error">
          {{ fieldErrors.sourceId }}
        </p>
      </div>

      <div class="form-field">
        <label class="field-label" :for="inputId('releaseDate')">上映日期</label>
        <input :id="inputId('releaseDate')" v-model="form.releaseDate" class="form-control"
          :class="{ 'is-invalid': fieldErrors.releaseDate }" type="datetime-local" step="1"
          :aria-invalid="fieldErrors.releaseDate ? 'true' : 'false'"
          :aria-describedby="describedBy('releaseDate', true)" @input="clearFieldError('releaseDate')" />
        <p :id="hintId('releaseDate')" class="field-hint">{{ releaseHint }}</p>
        <p v-if="fieldErrors.releaseDate" :id="errorId('releaseDate')" class="field-error">
          {{ fieldErrors.releaseDate }}
        </p>
      </div>
    </section>

    <section class="form-section">
      <h3 class="form-section-title">圖片</h3>

      <div class="form-field is-wide">
        <label class="field-label" :for="inputId('imageUrl')">電影圖片</label>
        <input :id="inputId('imageUrl')" v-model="form.imageUrl" class="form-control"
          :class="{ 'is-invalid': fieldErrors.imageUrl }" type="text" maxlength="512" inputmode="url" autocomplete="off"
          placeholder="https://" :aria-invalid="fieldErrors.imageUrl ? 'true' : 'false'"
          :aria-describedby="describedBy('imageUrl')" @input="clearFieldError('imageUrl')" />
        <p v-if="fieldErrors.imageUrl" :id="errorId('imageUrl')" class="field-error">
          {{ fieldErrors.imageUrl }}
        </p>
      </div>
    </section>

    <section v-if="showVisibility" class="form-section">
      <h3 class="form-section-title">狀態設定</h3>

      <div class="form-field">
        <span :id="inputId('isVisible-label')" class="field-label">前台顯示</span>
        <Switch v-model="form.isVisible" :aria-labelledby="inputId('isVisible-label')">
          {{ form.isVisible ? "顯示" : "隱藏" }}
        </Switch>
      </div>
    </section>
  </Form>
</template>
