<script setup>
import { computed, ref } from "vue";
import { Icon } from "@iconify/vue";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import tableIcon from "@iconify-icons/mdi/table-large";
import filterIcon from "@iconify-icons/mdi/filter-variant";
import tagIcon from "@iconify-icons/mdi/tag-outline";
import clipboardIcon from "@iconify-icons/mdi/clipboard-check-outline";
import pencilIcon from "@iconify-icons/mdi/pencil-outline";
import deleteIcon from "@iconify-icons/mdi/trash-can-outline";
import linkIcon from "@iconify-icons/mdi/open-in-new";

import http from "@/plugins/axios";
import { useApiCollection } from "@/composables/useApiCollection";
import { openAppAlert } from "@/components/ui/Alert.vue";
import PageIntro from "@/components/layout/PageIntro.vue";
import ResultBanner from "@/components/layout/ResultBanner.vue";
import SearchFilter from "@/components/layout/SearchFilter.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import DataTable, { dateTimeText } from "@/components/ui/DataTable.vue";
import Pagination from "@/components/ui/Pagination.vue";
import AppButton from "@/components/ui/Button.vue";
import Switch from "@/components/ui/Switch.vue";
import AppModal from "@/components/ui/AppModal.vue";

const PAGE_SIZE = 20;
const BENEFIT_TYPES = {
  REGULAR: "一般場次",
  SPECIAL_BY_DATE: "入場贈送",
  SPECIAL_UNTIL_OUT: "限量兌換",
};
const STATUS_LABELS = {
  APPROVED: "已上架",
  EXPIRED: "已下架",
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

// ── 列表：GET /benefits/admin?status=&keyword=&benefitType=&page=&size= ──
const filters = ref({ keyword: "", benefitType: "", status: "" });
const page = ref(1);
const listTop = ref(null);

const appliedFilters = computed({
  get: () => filters.value,
  set: (next) => {
    filters.value = {
      keyword: next?.keyword ?? "",
      benefitType: next?.benefitType ?? "",
      status: next?.status ?? "",
    };
    page.value = 1;
  },
});

const filterFields = [
  { key: "keyword", label: "關鍵字", type: "search", placeholder: "特典名稱或電影名稱", icon: giftIcon },
  { key: "benefitType", label: "特典類型", type: "select", emptyLabel: "全部類型", icon: tagIcon, options: BENEFIT_TYPES },
  { key: "status", label: "上架狀態", type: "select", emptyLabel: "全部狀態", icon: clipboardIcon, options: STATUS_LABELS },
];

// 空字串不要送，後端收到 null 才會當作「不篩選」
const requestParams = computed(() => {
  const params = { page: page.value, size: PAGE_SIZE };
  const { keyword, benefitType, status } = filters.value;
  if (keyword) params.keyword = keyword;
  if (benefitType) params.benefitType = benefitType;
  if (status) params.status = status;
  return params;
});

const { items: benefits, totalElements, totalPages, loading, error, retry } =
  useApiCollection("/benefits/admin", requestParams, { page });

const columns = [
  { key: "benefitId", label: "編號", minWidth: "5.5rem", nowrap: true, align: "center" },
  { key: "benefitName", label: "特典名稱", minWidth: "16rem", emphasis: true },
  { key: "movieName", label: "電影", minWidth: "10rem" },
  { key: "benefitType", label: "類型", minWidth: "7rem", nowrap: true, align: "center", format: (v) => BENEFIT_TYPES[v] ?? "" },
  { key: "startDate", label: "開始", minWidth: "11rem", nowrap: true, align: "center", format: dateTimeText },
  { key: "endDate", label: "結束", minWidth: "11rem", nowrap: true, align: "center", format: (v) => dateTimeText(v) || "送完為止" },
  { key: "status", label: "上架", minWidth: "7.5rem", nowrap: true, align: "center" },
  { key: "actions", label: "操作", minWidth: "14rem", align: "center" },
];

function goToPage(next) {
  if (!Number.isInteger(next) || next < 1 || next > totalPages.value || next === page.value) return;
  page.value = next;
  listTop.value?.scrollIntoView({ behavior: "smooth", block: "start" });
}

// ── 上下架：PATCH /benefits/{id}/status ──
const togglingId = ref(null);

async function toggleStatus(row, checked) {
  const next = checked ? "APPROVED" : "EXPIRED";
  if (togglingId.value || row.status === next) return;
  togglingId.value = row.benefitId;
  try {
    await http.patch(`/benefits/${row.benefitId}/status`, { status: next });
    row.status = next; // 直接改畫面，不用整頁重抓
  } catch (e) {
    // 例如：已過結束日的特典不能重新上架
    await showError(checked ? "無法上架" : "無法下架", e);
  } finally {
    togglingId.value = null;
  }
}

// ── 編輯視窗：PATCH /benefits/{id} ──
const editOpen = ref(false);
const editTarget = ref(null);
const saving = ref(false);
const form = ref({});

// API 給 "2026-10-16T00:00:00"，<input type="datetime-local"> 要 "2026-10-16T00:00"
function toInputDate(value) {
  return value ? String(value).replace(" ", "T").slice(0, 16) : "";
}

// 送回 API 要 "2026-10-16 00:00:00"
function toApiDate(value) {
  return value ? `${value.replace("T", " ")}:00` : null;
}

function openEdit(row) {
  editTarget.value = row;
  form.value = {
    benefitName: row.benefitName ?? "",
    benefitType: row.benefitType ?? "REGULAR",
    isMemberOnly: Boolean(row.isMemberOnly),
    startDate: toInputDate(row.startDate),
    endDate: toInputDate(row.endDate),
  };
  editOpen.value = true;
}

async function submitEdit() {
  const row = editTarget.value;
  if (!row || saving.value) return;

  const name = form.value.benefitName.trim();
  if (!name) {
    await openAppAlert({ variant: "info", title: "請填特典名稱", text: "特典名稱不能空白。", confirmText: "知道了", cancelText: "" });
    return;
  }

  // 圖片要連 publicId 一起改，這頁先不開放改圖片
  const body = {
    benefitName: name,
    benefitType: form.value.benefitType,
    isMemberOnly: form.value.isMemberOnly,
    startDate: toApiDate(form.value.startDate),
    endDate: toApiDate(form.value.endDate),
  };

  saving.value = true;
  try {
    await http.patch(`/benefits/${row.benefitId}`, body);
    editOpen.value = false;
    retry();
  } catch (e) {
    await showError("儲存失敗", e);
  } finally {
    saving.value = false;
  }
}

// ── 刪除：DELETE /benefits/{id} ──
async function removeBenefit(row) {
  const { isConfirmed } = await openAppAlert({
    variant: "warning",
    title: "確定刪除特典？",
    text: `「${row.benefitName}」刪除後無法復原。如果只是活動結束，建議改用「下架」。`,
    confirmText: "刪除",
    cancelText: "取消",
  });
  if (!isConfirmed) return;

  try {
    await http.delete(`/benefits/${row.benefitId}`);
    retry();
  } catch (e) {
    // 409：已經有會員收藏，不能刪
    await showError("無法刪除", e);
  }
}
</script>

<template>
  <section>
    <PageIntro kicker="✦ BENEFIT ADMIN" lead="特典" mark="管理" description="查看所有正式特典，可以修改資料、上下架或刪除。"
      :icon-left="giftIcon" :icon-right="tagIcon" :icon-top="tableIcon" theme="purple" />

    <ResultBanner v-if="!(loading && !benefits.length) && !error" :icon="giftIcon" title="正式特典"
      :count="totalElements" unit-text="筆" empty-text="目前沒有符合條件的特典。" theme="purple" />

    <SearchFilter v-if="!(loading && !benefits.length) && !error" v-model="appliedFilters" title="篩選特典"
      :title-icon="filterIcon" submit-label="查詢" :fields="filterFields" theme="purple">
      <div ref="listTop" class="benefit-sheet">
        <DataTable caption="特典資料表" :columns="columns" :rows="benefits" row-key="benefitId"
          empty-text="目前沒有符合條件的特典。" :loading="loading" theme="purple">
          <template #cell-status="{ row }">
            <Switch :model-value="row.status === 'APPROVED'" :disabled="togglingId === row.benefitId"
              :aria-label="`${row.benefitName} 上架開關`" @update:model-value="toggleStatus(row, $event)">
              {{ STATUS_LABELS[row.status] ?? row.status }}
            </Switch>
          </template>

          <template #cell-actions="{ row }">
            <div class="row-actions">
              <RouterLink class="view-link" :to="`/benefits/${row.benefitId}`" target="_blank" title="看前台頁面">
                <Icon :icon="linkIcon" aria-hidden="true" />
                前台
              </RouterLink>
              <AppButton type="button" color="purple" variant="outline" :icon="pencilIcon" @click="openEdit(row)">編輯</AppButton>
              <AppButton type="button" color="purple" variant="outline" :icon="deleteIcon" @click="removeBenefit(row)">刪除</AppButton>
            </div>
          </template>
        </DataTable>
      </div>

      <Pagination v-if="totalPages > 1" :page="page" :total-pages="totalPages" label="特典分頁" color="purple"
        @update:page="goToPage" />
    </SearchFilter>

    <StatePanel v-if="loading && !benefits.length" variant="loading" message="正在載入特典…" :icon="giftIcon" color="purple" />
    <StatePanel v-else-if="error" variant="error" title="目前無法取得特典" message="請確認已用管理員帳號登入，或稍後再試。"
      color="purple" @retry="retry" />

    <!-- 編輯視窗 -->
    <AppModal v-model="editOpen" :title="`編輯：#${editTarget?.benefitId ?? ''}`" :busy="saving">
      <div class="field">
        <label class="field-label" for="edit-name">特典名稱</label>
        <input id="edit-name" v-model="form.benefitName" type="text" class="form-control" maxlength="100" />
      </div>

      <div class="field">
        <label class="field-label" for="edit-type">特典類型</label>
        <select id="edit-type" v-model="form.benefitType" class="form-select">
          <option v-for="(label, value) in BENEFIT_TYPES" :key="value" :value="value">{{ label }}</option>
        </select>
      </div>

      <div class="field date-row">
        <div>
          <label class="field-label" for="edit-start">開始時間</label>
          <input id="edit-start" v-model="form.startDate" type="datetime-local" class="form-control" />
        </div>
        <div>
          <label class="field-label" for="edit-end">結束時間</label>
          <input id="edit-end" v-model="form.endDate" type="datetime-local" class="form-control" />
        </div>
      </div>
      <p class="field-hint">結束時間清空不會被清掉（後端「沒帶就不改」），要改成送完為止請直接改資料庫。</p>

      <div class="field">
        <Switch v-model="form.isMemberOnly">會員限定</Switch>
      </div>

      <template #footer>
        <AppButton type="button" color="purple" variant="outline" :disabled="saving" @click="editOpen = false">取消</AppButton>
        <AppButton type="button" color="purple" :icon="pencilIcon" :disabled="saving" @click="submitEdit">
          {{ saving ? "儲存中…" : "儲存" }}
        </AppButton>
      </template>
    </AppModal>
  </section>
</template>

<style scoped>
.benefit-sheet {
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

.view-link {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  color: #8d72ed;
  font-weight: 800;
}

.date-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
  gap: 0.75rem;
}
</style>
