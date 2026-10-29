<script setup>
    import { ref, onMounted } from 'vue'
    import http from '@/plugins/axios'
    import PageIntro from '@/components/layout/PageIntro.vue'

    import messageIcon from '@iconify-icons/mdi/message-text-outline'
    import lightbulbIcon from '@iconify-icons/mdi/lightbulb-on-outline'
    import heartIcon from '@iconify-icons/mdi/heart-outline'

    const categories = [
    { value: 'FEATURE_SUGGESTION', label: '功能建議' },
    { value: 'BUG_REPORT', label: '問題回報' },
    { value: 'QUESTION', label: '使用問題' },
    { value: 'OTHER', label: '其他' },
    ]

    const statusOptions = {
    PENDING: { label: '待處理', className: 'feedback-status-pending' },
    PROCESSING: { label: '處理中', className: 'feedback-status-processing' },
    COMPLETED: { label: '已完成', className: 'feedback-status-completed' },
    }

    const form = ref({
    category: 'FEATURE_SUGGESTION',
    title: '',
    content: '',
    })

    const feedbackList = ref([])
    const page = ref(0)
    const totalPages = ref(0)
    const loading = ref(false)
    const submitting = ref(false)
    const errorMessage = ref('')
    const successMessage = ref('')

    function categoryLabel(value) {
    return categories.find(item => item.value === value)?.label || value
    }

    function statusInfo(value) {
    return statusOptions[value] || { label: value || '未知', className: '' }
    }

    function formatDate(value) {
    if (!value) return '—'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return value
    return date.toLocaleString('zh-TW', { hour12: false })
    }

    async function loadFeedback() {
    loading.value = true
    errorMessage.value = ''

    try {
        const response = await http.get('/feedback/me', {
        params: { page: page.value, size: 10 },
        })

        const data = response.data
        feedbackList.value = Array.isArray(data)
        ? data
        : (data?.content || [])
        totalPages.value = data?.totalPages ?? 1
    } catch (error) {
        errorMessage.value =
        error.response?.data?.message ||
        '目前無法載入回饋紀錄，請稍後再試。'
    } finally {
        loading.value = false
    }
    }

    async function submitFeedback() {
    errorMessage.value = ''
    successMessage.value = ''

    const title = form.value.title.trim()
    const content = form.value.content.trim()

    if (!title || !content) {
        errorMessage.value = '請填寫回饋標題與內容。'
        return
    }

    submitting.value = true

    try {
        await http.post('/feedback', {
        category: form.value.category,
        title,
        content,
        })

        successMessage.value = '回饋已成功送出，謝謝你的建議！'
        form.value = {
        category: 'FEATURE_SUGGESTION',
        title: '',
        content: '',
        }

        page.value = 0
        await loadFeedback()
    } catch (error) {
        errorMessage.value =
        error.response?.data?.message ||
        '回饋送出失敗，請稍後再試。'
    } finally {
        submitting.value = false
    }
    }

    async function changePage(nextPage) {
    if (nextPage < 0 || nextPage >= totalPages.value) return
    page.value = nextPage
    await loadFeedback()
    }

    onMounted(loadFeedback)
</script>

<template>
  <main class="account-feedback-page">
    <PageIntro
      kicker="YOUR VOICE MATTERS"
      lead="讓特典地圖"
      mark="更好"
      tail="一點"
      description="遇到問題、想提出建議，或有任何想告訴我們的事，都歡迎在這裡留下訊息。"
      :icon-left="messageIcon"
      :icon-right="heartIcon"
      :icon-top="lightbulbIcon"
      theme="yellow"
    />

    <section class="feedback-panel">
      <div class="feedback-section-heading">
        <div>
          <span class="feedback-eyebrow">SEND FEEDBACK</span>
          <h2>留下你的回饋</h2>
          <p>我們會仔細閱讀每一則回饋，並在處理後更新狀態。</p>
        </div>
        <span class="feedback-heading-icon">
          <i class="bi bi-chat-heart"></i>
        </span>
      </div>

      <div v-if="successMessage" class="alert alert-success" role="status">
        {{ successMessage }}
      </div>
      <div v-if="errorMessage" class="alert alert-danger" role="alert">
        {{ errorMessage }}
      </div>

      <form @submit.prevent="submitFeedback">
        <div class="mb-3">
          <label for="feedback-category" class="form-label">
            回饋類型 <span class="required-star">*</span>
          </label>
          <select
            id="feedback-category"
            v-model="form.category"
            class="form-select"
            required
          >
            <option
              v-for="category in categories"
              :key="category.value"
              :value="category.value"
            >
              {{ category.label }}
            </option>
          </select>
        </div>

        <div class="mb-3">
          <label for="feedback-title" class="form-label">
            回饋標題 <span class="required-star">*</span>
          </label>
          <input
            id="feedback-title"
            v-model="form.title"
            class="form-control"
            type="text"
            maxlength="100"
            placeholder="請用簡短的文字描述你的回饋"
            required
          />
          <div class="feedback-field-hint">
            {{ form.title.length }}/100 字
          </div>
        </div>

        <div class="mb-4">
          <label for="feedback-content" class="form-label">
            詳細內容 <span class="required-star">*</span>
          </label>
          <textarea
            id="feedback-content"
            v-model="form.content"
            class="form-control"
            rows="5"
            maxlength="2000"
            placeholder="請描述遇到的問題或希望新增的功能..."
            required
          ></textarea>
          <div class="feedback-field-hint">
            {{ form.content.length }}/2000 字
          </div>
        </div>

        <div class="feedback-submit-row">
          <span class="feedback-note">標示 <b>*</b> 的欄位為必填</span>
          <button
            type="submit"
            class="btn feedback-submit-btn"
            :disabled="submitting"
          >
            <span
              v-if="submitting"
              class="spinner-border spinner-border-sm me-2"
              aria-hidden="true"
            ></span>
            <i v-else class="bi bi-send me-2"></i>
            {{ submitting ? '送出中...' : '送出回饋' }}
          </button>
        </div>
      </form>
    </section>

    <section class="feedback-panel feedback-history-panel">
      <div class="feedback-section-heading">
        <div>
          <span class="feedback-eyebrow">MY FEEDBACK</span>
          <h2>我的回饋紀錄</h2>
          <p>查看每一則回饋的處理進度與管理員回覆。</p>
        </div>
        <span class="feedback-count">{{ feedbackList.length }} 則</span>
      </div>

      <div v-if="loading" class="feedback-empty">
        <span class="spinner-border spinner-border-sm me-2"></span>
        正在載入回饋紀錄...
      </div>

      <div v-else-if="feedbackList.length === 0" class="feedback-empty">
        <i class="bi bi-inbox"></i>
        <h3>目前還沒有回饋紀錄</h3>
        <p>有任何建議或問題，都可以透過上方表單告訴我們。</p>
      </div>

      <article
        v-for="item in feedbackList"
        v-else
        :key="item.feedbackId"
        class="feedback-record"
      >
        <div class="feedback-record-top">
          <div class="feedback-record-title">
            <span class="feedback-category">
              {{ categoryLabel(item.category) }}
            </span>
            <h3>{{ item.title }}</h3>
          </div>
          <span
            class="feedback-status"
            :class="statusInfo(item.status).className"
          >
            {{ statusInfo(item.status).label }}
          </span>
        </div>

        <p class="feedback-record-content">{{ item.content }}</p>

        <div class="feedback-record-date">
          <i class="bi bi-clock me-1"></i>
          送出時間：{{ formatDate(item.createdAt) }}
        </div>

        <div v-if="item.adminReply" class="feedback-reply">
          <div class="feedback-reply-title">
            <i class="bi bi-chat-left-text me-2"></i>
            管理員回覆
          </div>
          <p>{{ item.adminReply }}</p>
          <small v-if="item.updatedAt">
            更新時間：{{ formatDate(item.updatedAt) }}
          </small>
        </div>
      </article>

      <nav
        v-if="!loading && totalPages > 1"
        class="feedback-pagination"
        aria-label="回饋紀錄分頁"
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
  </main>
</template>

<style scoped>
.account-feedback-page {
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  padding-bottom: 2.5rem;
  color: var(--c-ink, #292b35);
}

.feedback-panel {
  margin-bottom: 1.25rem;
  padding: clamp(1.1rem, 3vw, 1.8rem);
  border: 1.5px solid var(--c-border, #e8e2d8);
  border-radius: 20px;
  background: var(--c-card, #fff);
  box-shadow: var(--shadow-soft, 0 8px 24px rgb(41 43 53 / 6%));
}

.feedback-section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.feedback-eyebrow {
  color: #d46b42;
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.1em;
}

.feedback-section-heading h2 {
  margin: 0.3rem 0 0.4rem;
  font-size: 1.35rem;
  font-weight: 850;
}

.feedback-section-heading p {
  margin: 0;
  color: var(--c-muted, #777985);
  font-size: 0.92rem;
  line-height: 1.7;
}

.feedback-heading-icon {
  display: grid;
  flex: 0 0 46px;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 15px;
  background: #fff1cc;
  color: #a9651d;
  font-size: 1.35rem;
}

.form-label {
  font-weight: 750;
}

.required-star,
.feedback-note b {
  color: #d94747;
}

.form-control,
.form-select {
  min-height: 46px;
  border-color: var(--c-border, #e3ded5);
  border-radius: 11px;
  background-color: var(--c-card, #fff);
}

textarea.form-control {
  min-height: 140px;
  resize: vertical;
}

.form-control:focus,
.form-select:focus {
  border-color: #ffb45c;
  box-shadow: 0 0 0 0.2rem rgb(255 200 87 / 22%);
}

.feedback-field-hint {
  margin-top: 0.35rem;
  color: var(--c-muted, #777985);
  font-size: 0.8rem;
  text-align: right;
}

.feedback-submit-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.8rem;
}

.feedback-note {
  color: var(--c-muted, #777985);
  font-size: 0.85rem;
}

.feedback-submit-btn {
  min-height: 44px;
  padding: 0.65rem 1.25rem;
  border: 0;
  border-radius: 12px;
  background: #ff7049;
  color: #fff;
  font-weight: 800;
}

.feedback-submit-btn:hover:not(:disabled) {
  background: #e95e38;
  color: #fff;
}

.feedback-submit-btn:disabled {
  opacity: 0.7;
}

.feedback-count {
  flex-shrink: 0;
  padding: 0.4rem 0.75rem;
  border-radius: 999px;
  background: #fff1cc;
  color: #865500;
  font-size: 0.82rem;
  font-weight: 800;
}

.feedback-empty {
  padding: 2.25rem 1rem;
  color: var(--c-muted, #777985);
  text-align: center;
}

.feedback-empty > i {
  display: block;
  margin-bottom: 0.75rem;
  color: #e5b85e;
  font-size: 2.2rem;
}

.feedback-empty h3 {
  color: var(--c-ink, #292b35);
  font-size: 1.05rem;
  font-weight: 800;
}

.feedback-empty p {
  margin: 0;
  font-size: 0.9rem;
}

.feedback-record {
  padding: 1.2rem 0;
  border-top: 1px solid var(--c-border, #ece6dd);
}

.feedback-record-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.feedback-record-title h3 {
  margin: 0.5rem 0 0;
  font-size: 1.05rem;
  font-weight: 800;
  overflow-wrap: anywhere;
}

.feedback-category {
  display: inline-block;
  padding: 0.25rem 0.6rem;
  border-radius: 7px;
  background: #fff3dc;
  color: #87591b;
  font-size: 0.78rem;
  font-weight: 750;
}

.feedback-status {
  flex-shrink: 0;
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 800;
}

.feedback-status-pending {
  background: #fff1d2;
  color: #875b00;
}

.feedback-status-processing {
  background: #e7efff;
  color: #345da8;
}

.feedback-status-completed {
  background: #e2f5e9;
  color: #287344;
}

.feedback-record-content {
  margin: 0.9rem 0;
  line-height: 1.8;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.feedback-record-date {
  color: var(--c-muted, #777985);
  font-size: 0.82rem;
}

.feedback-reply {
  margin-top: 1rem;
  padding: 1rem;
  border-left: 4px solid #ffb35c;
  border-radius: 0 12px 12px 0;
  background: #fff9ee;
}

.feedback-reply-title {
  margin-bottom: 0.5rem;
  color: #8b5a20;
  font-weight: 850;
}

.feedback-reply p {
  margin: 0;
  line-height: 1.8;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.feedback-reply small {
  display: block;
  margin-top: 0.6rem;
  color: #8a8175;
}

.feedback-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 1rem;
  padding-top: 1.25rem;
}

.feedback-pagination span {
  color: var(--c-muted, #777985);
  font-size: 0.9rem;
}

.feedback-pagination .btn {
  border-radius: 10px;
}

@media (max-width: 520px) {
  .feedback-section-heading h2 {
    font-size: 1.15rem;
  }

  .feedback-section-heading p {
    font-size: 0.85rem;
  }

  .feedback-record-top {
    flex-direction: column;
  }

  .feedback-submit-row {
    align-items: stretch;
    flex-direction: column;
  }

  .feedback-submit-btn {
    width: 100%;
  }
}
</style>
