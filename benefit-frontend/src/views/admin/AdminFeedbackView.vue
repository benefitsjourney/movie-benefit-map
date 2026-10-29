
<script setup>
import { ref, onMounted } from 'vue'
import http from '@/plugins/axios'
import PageIntro from '@/components/layout/PageIntro.vue'

import messageIcon from '@iconify-icons/mdi/message-text-outline'
import clipboardIcon from '@iconify-icons/mdi/clipboard-check-outline'
import shieldIcon from '@iconify-icons/mdi/shield-check-outline'

const categories = {
  FEATURE_SUGGESTION: '功能建議',
  BUG_REPORT: '問題回報',
  QUESTION: '使用問題',
  OTHER: '其他',
}

const statuses = {
  PENDING: '待處理',
  PROCESSING: '處理中',
  COMPLETED: '已完成',
}

const statusClass = {
  PENDING: 'admin-status-pending',
  PROCESSING: 'admin-status-processing',
  COMPLETED: 'admin-status-completed',
}

const selectedStatus = ref('')
const feedbackList = ref([])
const selectedFeedback = ref(null)
const replyForm = ref({ status: 'PENDING', adminReply: '' })

const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const loading = ref(false)
const detailLoading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-TW', { hour12: false })
}

function categoryLabel(value) {
  return categories[value] || value || '—'
}

function statusLabel(value) {
  return statuses[value] || value || '未知'
}

function getStatusClass(value) {
  return statusClass[value] || ''
}

function getMemberName(item) {
  return item.memberName || item.account || `會員 #${item.userId ?? '—'}`
}

async function loadFeedback() {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const params = { page: page.value, size: 10 }
    if (selectedStatus.value) params.status = selectedStatus.value

    const response = await http.get('/admin/feedback', { params })
    const data = response.data

    feedbackList.value = Array.isArray(data)
      ? data
      : (data?.content || [])
    totalPages.value = data?.totalPages ?? 1
    totalElements.value = data?.totalElements ?? feedbackList.value.length

    if (
      selectedFeedback.value &&
      !feedbackList.value.some(
        item => item.feedbackId === selectedFeedback.value.feedbackId
      )
    ) {
      selectedFeedback.value = null
    }
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message ||
      '無法載入回饋清單，請確認管理員登入狀態後重試。'
  } finally {
    loading.value = false
  }
}

async function applyFilter() {
  page.value = 0
  selectedFeedback.value = null
  await loadFeedback()
}

async function changePage(nextPage) {
  if (nextPage < 0 || nextPage >= totalPages.value) return
  page.value = nextPage
  selectedFeedback.value = null
  await loadFeedback()
}

async function openFeedback(item) {
  selectedFeedback.value = null
  errorMessage.value = ''
  successMessage.value = ''
  detailLoading.value = true

  try {
    const response = await http.get(`/admin/feedback/${item.feedbackId}`)
    selectedFeedback.value = response.data
    replyForm.value = {
      status: response.data.status || 'PENDING',
      adminReply: response.data.adminReply || '',
    }
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message ||
      '無法載入這則回饋的詳細資料。'
  } finally {
    detailLoading.value = false
  }
}

async function saveFeedback() {
  if (!selectedFeedback.value || saving.value) return

  saving.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const payload = {
      status: replyForm.value.status,
      adminReply: replyForm.value.adminReply.trim() || null,
    }

    const response = await http.put(
      `/admin/feedback/${selectedFeedback.value.feedbackId}`,
      payload
    )

    selectedFeedback.value = response.data
    replyForm.value = {
      status: response.data.status || payload.status,
      adminReply: response.data.adminReply || '',
    }

    successMessage.value = '回饋處理結果已更新。'
    await loadFeedback()

    // 保留剛更新的詳細資料，避免清單刷新後看不到回覆。
    selectedFeedback.value = response.data
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message ||
      '更新失敗，請稍後再試。'
  } finally {
    saving.value = false
  }
}

onMounted(loadFeedback)
</script>

<template>
  <main class="admin-feedback-page">
    <PageIntro
      kicker="FEEDBACK MANAGEMENT"
      lead="會員回饋"
      mark="管理"
      tail="中心"
      description="集中查看會員的建議與問題回報，更新處理狀態，並留下清楚的處理回覆。"
      :icon-left="messageIcon"
      :icon-right="clipboardIcon"
      :icon-top="shieldIcon"
      theme="purple"
    />

    <div class="admin-feedback-summary">
      <div class="admin-feedback-summary-icon">
        <i class="bi bi-chat-square-heart"></i>
      </div>
      <div>
        <span>目前符合條件的回饋</span>
        <strong>{{ totalElements }}</strong>
      </div>
    </div>

    <section class="admin-feedback-panel">
      <div class="admin-feedback-toolbar">
        <div>
          <span class="admin-feedback-eyebrow">FEEDBACK INBOX</span>
          <h2>回饋清單</h2>
          <p>選擇一筆回饋，即可查看內容並編輯處理結果。</p>
        </div>

        <button
          type="button"
          class="btn admin-refresh-btn"
          :disabled="loading"
          @click="loadFeedback"
        >
          <i class="bi bi-arrow-clockwise me-1"></i>
          重新整理
        </button>
      </div>

      <div class="admin-feedback-filter">
        <label for="feedback-status-filter" class="form-label">
          處理狀態
        </label>
        <select
          id="feedback-status-filter"
          v-model="selectedStatus"
          class="form-select"
          @change="applyFilter"
        >
          <option value="">全部狀態</option>
          <option value="PENDING">待處理</option>
          <option value="PROCESSING">處理中</option>
          <option value="COMPLETED">已完成</option>
        </select>
      </div>

      <div v-if="errorMessage" class="alert alert-danger" role="alert">
        {{ errorMessage }}
      </div>
      <div v-if="successMessage" class="alert alert-success" role="status">
        {{ successMessage }}
      </div>

      <div v-if="loading" class="admin-feedback-empty">
        <span class="spinner-border spinner-border-sm me-2"></span>
        正在載入回饋清單...
      </div>

      <div v-else-if="feedbackList.length === 0" class="admin-feedback-empty">
        <i class="bi bi-inbox"></i>
        <h3>目前沒有符合條件的回饋</h3>
        <p>可以切換其他狀態，或稍後重新整理。</p>
      </div>

      <div v-else class="admin-feedback-table-wrap">
        <table class="table admin-feedback-table align-middle">
          <thead>
            <tr>
              <th>回饋內容</th>
              <th>會員</th>
              <th>狀態</th>
              <th>送出時間</th>
              <th class="text-end">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="item in feedbackList"
              :key="item.feedbackId"
              :class="{
                'admin-feedback-row-active':
                  selectedFeedback?.feedbackId === item.feedbackId,
              }"
            >
              <td>
                <div class="admin-feedback-title">{{ item.title }}</div>
                <span class="admin-feedback-category">
                  {{ categoryLabel(item.category) }}
                </span>
              </td>
              <td>{{ getMemberName(item) }}</td>
              <td>
                <span
                  class="admin-feedback-status"
                  :class="getStatusClass(item.status)"
                >
                  {{ statusLabel(item.status) }}
                </span>
              </td>
              <td class="admin-feedback-date">
                {{ formatDate(item.createdAt) }}
              </td>
              <td class="text-end">
                <button
                  type="button"
                  class="btn btn-sm admin-view-btn"
                  :disabled="detailLoading"
                  @click="openFeedback(item)"
                >
                  查看／處理
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <nav
        v-if="!loading && totalPages > 1"
        class="admin-feedback-pagination"
        aria-label="回饋清單分頁"
      >
        <button
          type="button"
          class="btn btn-outline-secondary"
          :disabled="page === 0"
          @click="changePage(page - 1)"
        >
          上一頁
        </button>
        <span>第 {{ page + 1 }} / {{ totalPages }} 頁</span>
        <button
          type="button"
          class="btn btn-outline-secondary"
          :disabled="page + 1 >= totalPages"
          @click="changePage(page + 1)"
        >
          下一頁
        </button>
      </nav>
    </section>

    <section v-if="detailLoading" class="admin-feedback-panel">
      <div class="admin-feedback-empty">
        <span class="spinner-border spinner-border-sm me-2"></span>
        正在載入回饋詳細資料...
      </div>
    </section>

    <section v-if="selectedFeedback" class="admin-feedback-panel">
      <div class="admin-feedback-toolbar">
        <div>
          <span class="admin-feedback-eyebrow">
            FEEDBACK #{{ selectedFeedback.feedbackId }}
          </span>
          <h2>回饋詳細資料</h2>
          <p>查看會員原始內容，並更新處理狀態與管理員回覆。</p>
        </div>
        <button
          type="button"
          class="btn btn-outline-secondary admin-close-detail"
          @click="selectedFeedback = null"
        >
          收合
        </button>
      </div>

      <div class="admin-feedback-detail">
        <div class="admin-detail-meta">
          <span>
            <i class="bi bi-person me-1"></i>
            {{ getMemberName(selectedFeedback) }}
          </span>
          <span>
            <i class="bi bi-tag me-1"></i>
            {{ categoryLabel(selectedFeedback.category) }}
          </span>
          <span>
            <i class="bi bi-clock me-1"></i>
            {{ formatDate(selectedFeedback.createdAt) }}
          </span>
        </div>

        <h3 class="admin-detail-title">{{ selectedFeedback.title }}</h3>
        <div class="admin-detail-content">{{ selectedFeedback.content }}</div>

        <hr />

        <form @submit.prevent="saveFeedback">
          <div class="mb-3">
            <label for="feedback-admin-status" class="form-label">
              處理狀態 <span class="required-star">*</span>
            </label>
            <select
              id="feedback-admin-status"
              v-model="replyForm.status"
              class="form-select"
              required
            >
              <option value="PENDING">待處理</option>
              <option value="PROCESSING">處理中</option>
              <option value="COMPLETED">已完成</option>
            </select>
          </div>

          <div class="mb-3">
            <label for="feedback-admin-reply" class="form-label">
              管理員回覆
            </label>
            <textarea
              id="feedback-admin-reply"
              v-model="replyForm.adminReply"
              class="form-control"
              rows="5"
              maxlength="2000"
              placeholder="可填寫處理結果或回覆；若不需要回覆，可以留空。"
            ></textarea>
            <div class="admin-reply-hint">
              {{ replyForm.adminReply.length }}/2000 字，回覆可留空。
            </div>
          </div>

          <div class="admin-feedback-save-row">
            <button
              type="submit"
              class="btn admin-save-btn"
              :disabled="saving"
            >
              <span
                v-if="saving"
                class="spinner-border spinner-border-sm me-2"
              ></span>
              <i v-else class="bi bi-check2-circle me-2"></i>
              {{ saving ? '儲存中...' : '儲存處理結果' }}
            </button>
          </div>
        </form>
      </div>
    </section>
  </main>
</template>

<style scoped>
.admin-feedback-page {
  width: 100%;
  max-width: 1240px;
  margin: 0 auto;
  padding-bottom: 2.5rem;
  color: var(--c-ink, #292b35);
}

.admin-feedback-summary,
.admin-feedback-panel {
  margin-bottom: 1.25rem;
  border: 1.5px solid var(--c-border, #e8e2ed);
  border-radius: 20px;
  background: var(--c-card, #fff);
  box-shadow: var(--shadow-soft, 0 8px 24px rgb(41 43 53 / 6%));
}

.admin-feedback-summary {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.15rem 1.4rem;
}

.admin-feedback-summary-icon {
  display: grid;
  flex: 0 0 48px;
  width: 48px;
  height: 48px;
  place-items: center;
  border-radius: 15px;
  background: #f2e5ff;
  color: #7545a3;
  font-size: 1.4rem;
}

.admin-feedback-summary span {
  display: block;
  color: var(--c-muted, #777985);
  font-size: 0.85rem;
}

.admin-feedback-summary strong {
  display: block;
  margin-top: 0.15rem;
  font-size: 1.55rem;
  font-weight: 900;
}

.admin-feedback-panel {
  padding: clamp(1rem, 3vw, 1.6rem);
}

.admin-feedback-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.35rem;
}

.admin-feedback-eyebrow {
  color: #9b64cc;
  font-size: 0.75rem;
  font-weight: 850;
  letter-spacing: 0.1em;
}

.admin-feedback-toolbar h2 {
  margin: 0.3rem 0 0.4rem;
  font-size: 1.3rem;
  font-weight: 850;
}

.admin-feedback-toolbar p {
  margin: 0;
  color: var(--c-muted, #777985);
  font-size: 0.9rem;
  line-height: 1.7;
}

.admin-refresh-btn,
.admin-close-detail {
  flex-shrink: 0;
  border-radius: 10px;
  font-weight: 700;
}

.admin-refresh-btn {
  border: 1px solid #ddc5f6;
  background: #f8f1ff;
  color: #7545a3;
}

.admin-refresh-btn:hover:not(:disabled) {
  background: #efe0ff;
  color: #63378f;
}

.admin-feedback-filter {
  display: flex;
  align-items: center;
  gap: 1rem;
  max-width: 440px;
  margin-bottom: 1.2rem;
}

.admin-feedback-filter .form-label {
  flex-shrink: 0;
  margin: 0;
  font-weight: 750;
}

.form-select,
.form-control {
  border-color: var(--c-border, #e3dce9);
  border-radius: 10px;
}

.form-select:focus,
.form-control:focus {
  border-color: #c99bff;
  box-shadow: 0 0 0 0.2rem rgb(201 155 255 / 22%);
}

.admin-feedback-table-wrap {
  overflow-x: auto;
}

.admin-feedback-table {
  min-width: 760px;
  margin-bottom: 0;
}

.admin-feedback-table thead th {
  padding: 0.9rem 0.75rem;
  border-bottom: 1px solid #e9e0f0;
  background: #faf6ff;
  color: #65556f;
  font-size: 0.82rem;
  font-weight: 850;
  white-space: nowrap;
}

.admin-feedback-table tbody td {
  padding: 1rem 0.75rem;
  border-color: var(--c-border, #eee8f1);
  font-size: 0.88rem;
}

.admin-feedback-row-active td {
  background: #fbf6ff;
}

.admin-feedback-title {
  max-width: 300px;
  font-weight: 800;
  overflow-wrap: anywhere;
}

.admin-feedback-category {
  display: inline-block;
  margin-top: 0.4rem;
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
  background: #f2e5ff;
  color: #7545a3;
  font-size: 0.75rem;
  font-weight: 750;
}

.admin-feedback-status {
  display: inline-block;
  padding: 0.35rem 0.65rem;
  border-radius: 999px;
  font-size: 0.77rem;
  font-weight: 800;
  white-space: nowrap;
}

.admin-status-pending {
  background: #fff1d2;
  color: #875b00;
}

.admin-status-processing {
  background: #e7efff;
  color: #345da8;
}

.admin-status-completed {
  background: #e2f5e9;
  color: #287344;
}

.admin-feedback-date {
  min-width: 150px;
  color: var(--c-muted, #777985);
  font-size: 0.82rem !important;
}

.admin-view-btn {
  border: 1px solid #d8b8f5;
  border-radius: 9px;
  background: #f7efff;
  color: #7545a3;
  font-weight: 750;
  white-space: nowrap;
}

.admin-view-btn:hover:not(:disabled) {
  background: #ead7ff;
  color: #63378f;
}

.admin-feedback-empty {
  padding: 2.2rem 1rem;
  color: var(--c-muted, #777985);
  text-align: center;
}

.admin-feedback-empty > i {
  display: block;
  margin-bottom: 0.7rem;
  color: #c99bff;
  font-size: 2.2rem;
}

.admin-feedback-empty h3 {
  color: var(--c-ink, #292b35);
  font-size: 1.05rem;
  font-weight: 800;
}

.admin-feedback-empty p {
  margin-bottom: 0;
  font-size: 0.9rem;
}

.admin-feedback-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 1rem;
  padding-top: 1.25rem;
}

.admin-feedback-pagination span {
  color: var(--c-muted, #777985);
  font-size: 0.9rem;
}

.admin-feedback-pagination .btn {
  border-radius: 10px;
}

.admin-feedback-detail {
  padding: clamp(1rem, 2.5vw, 1.5rem);
  border: 1px solid #eee2f8;
  border-radius: 16px;
  background: #fdfaff;
}

.admin-detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem 1.25rem;
  color: var(--c-muted, #777985);
  font-size: 0.82rem;
}

.admin-detail-title {
  margin: 1.25rem 0 0.8rem;
  font-size: 1.2rem;
  font-weight: 850;
  overflow-wrap: anywhere;
}

.admin-detail-content {
  line-height: 1.9;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.admin-feedback-detail hr {
  margin: 1.5rem 0;
  border-color: #e7d9f3;
  opacity: 1;
}

.admin-feedback-detail .form-label {
  font-weight: 800;
}

.required-star {
  color: #d94747;
}

.admin-reply-hint {
  margin-top: 0.35rem;
  color: var(--c-muted, #777985);
  font-size: 0.8rem;
  text-align: right;
}

.admin-feedback-save-row {
  display: flex;
  justify-content: flex-end;
}

.admin-save-btn {
  min-height: 44px;
  padding: 0.65rem 1.15rem;
  border: 0;
  border-radius: 11px;
  background: #8b5ac2;
  color: #fff;
  font-weight: 800;
}

.admin-save-btn:hover:not(:disabled) {
  background: #7545a3;
  color: #fff;
}

@media (max-width: 560px) {
  .admin-feedback-toolbar {
    flex-direction: column;
  }

  .admin-refresh-btn,
  .admin-close-detail {
    align-self: flex-start;
  }

  .admin-feedback-filter {
    align-items: stretch;
    flex-direction: column;
    gap: 0.5rem;
  }

  .admin-feedback-save-row .btn {
    width: 100%;
  }
}
</style>
