<script setup>
import { computed, reactive, ref, useId } from "vue";
import formIcon from "@iconify-icons/mdi/form-textbox";
import plusIcon from "@iconify-icons/mdi/plus";
import { openAppAlert } from "@/components/ui/Alert.vue";
import { COUNTY_LABELS } from "@/constants/county";
import Form from "@/components/ui/Form.vue";
import Switch from "@/components/ui/Switch.vue";

const fieldId = useId();

const props = defineProps({
  mode: {
    type: String,
    default: "create",
    validator: (value) => value === "create" || value === "edit",
  },
  cinema: { type: Object, default: null },
  submitting: { type: Boolean, default: false },
  plain: { type: Boolean, default: false },
  hideSubmit: { type: Boolean, default: false },
  formId: { type: String, default: undefined },
  /** 關閉時不顯示或送出狀態欄位，由外層頁面獨立管理。 */
  showActive: { type: Boolean, default: true },
  showVisibility: { type: Boolean, default: true },
  submitLabel: { type: String, default: "" },
  submitIcon: { type: [Object, String], default: null },
});

const emit = defineEmits(["submit"]);

const fieldErrors = ref({});
const form = reactive(emptyForm());

const resolvedSubmitLabel = computed(() => {
  if (props.submitLabel) return props.submitLabel;
  return props.mode === "edit" ? "儲存" : "新增影城";
});

const resolvedSubmitIcon = computed(() => props.submitIcon ?? plusIcon);

const countyOptions = computed(() =>
  Object.entries(COUNTY_LABELS).map(([value, label]) => ({ value, label })),
);

applyCinema(props.cinema);

function emptyForm() {
  return {
    sourceId: "",
    cinemaName: "",
    county: "",
    cinemaAddress: "",
    latitude: "",
    longitude: "",
    officialWebsiteUrl: "",
    isActive: true,
    isVisible: true,
  };
}

function textOf(value) {
  return value == null ? "" : String(value);
}

function applyCinema(cinema) {
  // 詳情頁以 key 重掛表單，避免外部更新覆蓋使用者正在輸入的草稿。
  const next = props.mode === "edit"
    ? {
      sourceId: textOf(cinema?.sourceId),
      cinemaName: textOf(cinema?.cinemaName),
      county: textOf(cinema?.county),
      cinemaAddress: textOf(cinema?.cinemaAddress),
      latitude: textOf(cinema?.latitude),
      longitude: textOf(cinema?.longitude),
      officialWebsiteUrl: textOf(cinema?.officialWebsiteUrl),
      isActive: cinema?.isActive !== false,
      isVisible: cinema?.isVisible !== false,
    }
    : emptyForm();
  form.sourceId = next.sourceId;
  form.cinemaName = next.cinemaName;
  form.county = next.county;
  form.cinemaAddress = next.cinemaAddress;
  form.latitude = next.latitude;
  form.longitude = next.longitude;
  form.officialWebsiteUrl = next.officialWebsiteUrl;
  form.isActive = next.isActive;
  form.isVisible = next.isVisible;
  fieldErrors.value = {};
}

function coordError(value, label, min, max) {
  const text = String(value ?? "").trim();
  if (!text) return "";
  if (!/^-?\d{1,3}(\.\d{1,7})?$/.test(text)) {
    return `${label}須為小數點前最多3位、小數點後最多7位`;
  }
  const number = Number(text);
  if (number < min) return `${label}不可小於${min}`;
  if (number > max) return `${label}不可大於${max}`;
  return "";
}

function validate() {
  const errors = {};
  const cinemaName = form.cinemaName.trim();
  const sourceId = form.sourceId.trim();
  const cinemaAddress = form.cinemaAddress.trim();
  const officialWebsiteUrl = form.officialWebsiteUrl.trim();

  if (!cinemaName) errors.cinemaName = "影城名稱不能為空";
  else if ([...cinemaName].length > 256) errors.cinemaName = "影城名稱長度不可超過256字元";

  if (!form.county) errors.county = "縣市不能為空";
  else if (!Object.hasOwn(COUNTY_LABELS, form.county)) errors.county = "縣市不能為空";

  if ([...sourceId].length > 64) errors.sourceId = "來源ID長度不可超過64字元";
  if ([...cinemaAddress].length > 256) errors.cinemaAddress = "地址長度不可超過256字元";
  if ([...officialWebsiteUrl].length > 512) {
    errors.officialWebsiteUrl = "官方網站長度不可超過512字元";
  }

  const latitude = coordError(form.latitude, "緯度", -90, 90);
  const longitude = coordError(form.longitude, "經度", -180, 180);
  if (latitude) errors.latitude = latitude;
  if (longitude) errors.longitude = longitude;

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

function decimalOrNull(value) {
  const text = String(value ?? "").trim();
  return text ? Number(text) : null;
}

function payload() {
  const body = {
    sourceId: form.sourceId.trim() || null,
    cinemaName: form.cinemaName.trim(),
    county: form.county,
    cinemaAddress: form.cinemaAddress.trim(),
    latitude: decimalOrNull(form.latitude),
    longitude: decimalOrNull(form.longitude),
    officialWebsiteUrl: form.officialWebsiteUrl.trim(),
  };
  // 詳情頁另有即時開關，編輯其他欄位時不得連帶覆蓋狀態。
  if (props.showActive) body.isActive = Boolean(form.isActive);
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
  <Form :title="plain ? '' : (mode === 'edit' ? '修改影城資料' : '新增影城資料')" :title-icon="plain ? null : formIcon"
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
        <label class="field-label" :for="inputId('cinemaName')">
          影城名稱 <span class="field-required" aria-hidden="true">*</span>
        </label>
        <input :id="inputId('cinemaName')" v-model="form.cinemaName" class="form-control"
          :class="{ 'is-invalid': fieldErrors.cinemaName }" type="text" maxlength="256" autocomplete="off" required
          :aria-invalid="fieldErrors.cinemaName ? 'true' : 'false'" :aria-describedby="describedBy('cinemaName')"
          @input="clearFieldError('cinemaName')" />
        <p v-if="fieldErrors.cinemaName" :id="errorId('cinemaName')" class="field-error">
          {{ fieldErrors.cinemaName }}
        </p>
      </div>

      <div class="form-field">
        <label class="field-label" :for="inputId('county')">
          所在縣市 <span class="field-required" aria-hidden="true">*</span>
        </label>
        <select :id="inputId('county')" v-model="form.county" class="form-select"
          :class="{ 'is-invalid': fieldErrors.county }" required :aria-invalid="fieldErrors.county ? 'true' : 'false'"
          :aria-describedby="describedBy('county')" @change="clearFieldError('county')">
          <option value="">請選擇縣市</option>
          <option v-for="option in countyOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
        <p v-if="fieldErrors.county" :id="errorId('county')" class="field-error">
          {{ fieldErrors.county }}
        </p>
      </div>

      <div class="form-field">
        <label class="field-label" :for="inputId('sourceId')">來源編號</label>
        <input :id="inputId('sourceId')" v-model="form.sourceId" class="form-control"
          :class="{ 'is-invalid': fieldErrors.sourceId }" type="text" maxlength="64" autocomplete="off"
          :aria-invalid="fieldErrors.sourceId ? 'true' : 'false'" :aria-describedby="describedBy('sourceId', true)"
          @input="clearFieldError('sourceId')" />
        <p :id="hintId('sourceId')" class="field-hint">爬蟲來源的識別碼，不是影城在系統內的編號。</p>
        <p v-if="fieldErrors.sourceId" :id="errorId('sourceId')" class="field-error">
          {{ fieldErrors.sourceId }}
        </p>
      </div>
    </section>

    <section class="form-section">
      <h3 class="form-section-title">位置與聯絡資訊</h3>

      <div class="form-field is-wide">
        <label class="field-label" :for="inputId('cinemaAddress')">影城地址</label>
        <input :id="inputId('cinemaAddress')" v-model="form.cinemaAddress" class="form-control"
          :class="{ 'is-invalid': fieldErrors.cinemaAddress }" type="text" maxlength="256" autocomplete="street-address"
          :aria-invalid="fieldErrors.cinemaAddress ? 'true' : 'false'" :aria-describedby="describedBy('cinemaAddress')"
          @input="clearFieldError('cinemaAddress')" />
        <p v-if="fieldErrors.cinemaAddress" :id="errorId('cinemaAddress')" class="field-error">
          {{ fieldErrors.cinemaAddress }}
        </p>
      </div>

      <div class="form-field">
        <label class="field-label" :for="inputId('latitude')">緯度</label>
        <input :id="inputId('latitude')" v-model="form.latitude" class="form-control"
          :class="{ 'is-invalid': fieldErrors.latitude }" type="text" inputmode="decimal" autocomplete="off"
          placeholder="例如 25.033964" :aria-invalid="fieldErrors.latitude ? 'true' : 'false'"
          :aria-describedby="describedBy('latitude')" @input="clearFieldError('latitude')" />
        <p v-if="fieldErrors.latitude" :id="errorId('latitude')" class="field-error">
          {{ fieldErrors.latitude }}
        </p>
      </div>

      <div class="form-field">
        <label class="field-label" :for="inputId('longitude')">經度</label>
        <input :id="inputId('longitude')" v-model="form.longitude" class="form-control"
          :class="{ 'is-invalid': fieldErrors.longitude }" type="text" inputmode="decimal" autocomplete="off"
          placeholder="例如 121.564468" :aria-invalid="fieldErrors.longitude ? 'true' : 'false'"
          :aria-describedby="describedBy('longitude')" @input="clearFieldError('longitude')" />
        <p v-if="fieldErrors.longitude" :id="errorId('longitude')" class="field-error">
          {{ fieldErrors.longitude }}
        </p>
      </div>

      <div class="form-field is-wide">
        <label class="field-label" :for="inputId('officialWebsiteUrl')">官方網站</label>
        <input :id="inputId('officialWebsiteUrl')" v-model="form.officialWebsiteUrl" class="form-control"
          :class="{ 'is-invalid': fieldErrors.officialWebsiteUrl }" type="text" maxlength="512" inputmode="url"
          autocomplete="url" placeholder="https://" :aria-invalid="fieldErrors.officialWebsiteUrl ? 'true' : 'false'"
          :aria-describedby="describedBy('officialWebsiteUrl')" @input="clearFieldError('officialWebsiteUrl')" />
        <p v-if="fieldErrors.officialWebsiteUrl" :id="errorId('officialWebsiteUrl')" class="field-error">
          {{ fieldErrors.officialWebsiteUrl }}
        </p>
      </div>
    </section>

    <section v-if="showActive || showVisibility" class="form-section">
      <h3 class="form-section-title">狀態設定</h3>

      <div v-if="showActive" class="form-field">
        <span :id="inputId('isActive-label')" class="field-label">營業狀態</span>
        <Switch v-model="form.isActive" :aria-labelledby="inputId('isActive-label')">
          {{ form.isActive ? "營業中" : "已歇業" }}
        </Switch>
      </div>

      <div v-if="showVisibility" class="form-field">
        <span :id="inputId('isVisible-label')" class="field-label">前台顯示</span>
        <Switch v-model="form.isVisible" :aria-labelledby="inputId('isVisible-label')">
          {{ form.isVisible ? "顯示" : "隱藏" }}
        </Switch>
      </div>
    </section>
  </Form>
</template>
