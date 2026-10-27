
<script setup>
import { ref, computed, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import accountGroupIcon from '@iconify-icons/mdi/account-group-outline'
import magnifyIcon from '@iconify-icons/mdi/magnify'
import filterIcon from '@iconify-icons/mdi/filter-variant'

import PageIntro from '@/components/layout/PageIntro.vue'

const users = ref([])
const loadingUsers = ref(false)
const userError = ref('')
const statusFilter = ref('ALL')
const searchKeyword = ref('')
const updatingUserId = ref(null)

const statusOptions = [
    { value: 'ALL', label: '全部會員（不含已刪除）' },
    { value: 'ACTIVE', label: '正常' },
    { value: 'INACTIVE', label: '停用' },
    { value: 'BANNED', label: '封鎖' },
    { value: 'DELETED', label: '已刪除' },
]

const filteredUsers = computed(() => {
    let result = users.value

    if (statusFilter.value !== 'ALL') {
        result = result.filter(
            user => user.status === statusFilter.value
        )
    }

    const keyword = searchKeyword.value.trim().toLowerCase()

    if (keyword) {
        result = result.filter(user =>
            (user.account || '').toLowerCase().includes(keyword) ||
            (user.email || '').toLowerCase().includes(keyword)
        )
    }

    return result
})

function getToken() {
    return localStorage.getItem('token')
}

async function apiRequest(url, options = {}) {
    const token = getToken()

    const response = await fetch(`/api${url}`, {
        ...options,
        headers: {
            ...(options.body ? { 'Content-Type': 'application/json' } : {}),
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
            ...options.headers,
        },
    })

    if (!response.ok) {
        const message = await response.text()
        throw new Error(message || `請求失敗（${response.status}）`)
    }

    if (response.status === 204) return null
    return response.json()
}

// 取得會員列表
async function loadUsers() {
    loadingUsers.value = true
    userError.value = ''

    try {
        const query = statusFilter.value === 'ALL'
            ? ''
            : `?status=${encodeURIComponent(statusFilter.value)}`

        users.value = await apiRequest(`/admin/users${query}`)
    } catch (error) {
        userError.value = error.message || '讀取會員列表失敗'
    } finally {
        loadingUsers.value = false
    }
}

// 調整會員狀態
async function updateUserStatus(user) {
    let nextStatus
    let actionText

    if (user.status === 'ACTIVE') {
        nextStatus = 'BANNED'
        actionText = '封鎖'
    } else if (user.status === 'BANNED') {
        nextStatus = 'ACTIVE'
        actionText = '解除封鎖'
    } else if (user.status === 'INACTIVE') {
        nextStatus = 'ACTIVE'
        actionText = '重新啟用'
    } else {
        return
    }

    if (!window.confirm(`確定要${actionText}會員「${user.account}」嗎？`)) {
        return
    }

    updatingUserId.value = user.usersId
    userError.value = ''

    try {
        await apiRequest(`/admin/users/${user.usersId}/status`, {
            method: 'PATCH',
            body: JSON.stringify({ status: nextStatus }),
        })

        await loadUsers()
        window.alert(`已${actionText}會員。`)
    } catch (error) {
        userError.value = error.message || '更新會員狀態失敗'
    } finally {
        updatingUserId.value = null
    }
}

function formatDate(value) {
    if (!value) return '—'

    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return value

    return date.toLocaleString('zh-TW', { hour12: false })
}

function statusLabel(status) {
    const labels = {
        ACTIVE: '正常',
        INACTIVE: '停用',
        BANNED: '封鎖',
        DELETED: '已刪除',
    }

    return labels[status] || status || '—'
}

function statusClass(status) {
    const classes = {
        ACTIVE: 'status-active',
        INACTIVE: 'status-inactive',
        BANNED: 'status-banned',
        DELETED: 'status-deleted',
    }

    return classes[status] || 'status-inactive'
}

onMounted(() => {
    loadUsers()
})
</script>

<template>
    <section class="admin-users-page">
        <PageIntro
            kicker="✦ MEMBER MANAGEMENT"
            lead="會員"
            mark="管理"
            description="管理會員資料、搜尋帳號與電子郵件，並調整會員狀態。"
            :icon-left="accountGroupIcon"
            :icon-right="filterIcon"
            :icon-top="magnifyIcon"
            theme="purple"
        />

        <div class="users-panel">
            <div class="users-panel-heading">
                <div>
                    <h3 class="users-title">會員列表</h3>
                    <p class="users-subtitle">依帳號、電子郵件或會員狀態快速查找</p>
                </div>

                <span class="users-count">
                    共 {{ filteredUsers.length }} 位
                </span>
            </div>

            <div class="users-toolbar">
                <div class="search-field">
                    <label for="userSearch" class="toolbar-label">
                        搜尋會員
                    </label>

                    <div class="input-with-icon">
                        <Icon :icon="magnifyIcon" aria-hidden="true" />

                        <input
                            id="userSearch"
                            v-model="searchKeyword"
                            type="search"
                            class="form-control"
                            placeholder="輸入帳號或電子郵件"
                            autocomplete="off"
                        />
                    </div>
                </div>

                <div class="status-field">
                    <label for="statusFilter" class="toolbar-label">
                        <Icon :icon="filterIcon" aria-hidden="true" />
                        會員狀態
                    </label>

                    <select
                        id="statusFilter"
                        v-model="statusFilter"
                        class="form-select"
                        @change="loadUsers"
                    >
                        <option
                            v-for="option in statusOptions"
                            :key="option.value"
                            :value="option.value"
                        >
                            {{ option.label }}
                        </option>
                    </select>
                </div>
            </div>

            <div v-if="userError" class="alert alert-danger mx-3 mt-3">
                {{ userError }}
            </div>

            <div v-if="loadingUsers" class="state-message">
                <div class="spinner-border text-secondary" role="status">
                    <span class="visually-hidden">載入中</span>
                </div>
                <p>正在載入會員資料…</p>
            </div>

            <template v-else>
                <div class="table-responsive users-table-wrap">
                    <table class="table table-hover align-middle users-table">
                        <thead>
                            <tr>
                                <th>會員編號</th>
                                <th>帳號</th>
                                <th>暱稱</th>
                                <th>電子郵件</th>
                                <th>角色</th>
                                <th>狀態</th>
                                <th>註冊時間</th>
                                <th class="text-center">操作</th>
                            </tr>
                        </thead>

                        <tbody>
                            <tr
                                v-for="user in filteredUsers"
                                :key="user.usersId"
                            >
                                <td class="user-id">#{{ user.usersId }}</td>

                                <td class="user-account">
                                    {{ user.account || '—' }}
                                </td>

                                <td>{{ user.name || '—' }}</td>

                                <td class="user-email">
                                    {{ user.email || '—' }}
                                </td>

                                <td>
                                    <span class="role-label">
                                        {{ user.role || '—' }}
                                    </span>
                                </td>

                                <td>
                                    <span
                                        class="status-badge"
                                        :class="statusClass(user.status)"
                                    >
                                        <span class="status-dot"></span>
                                        {{ statusLabel(user.status) }}
                                    </span>
                                </td>

                                <td class="date-cell">
                                    {{ formatDate(user.createdAt) }}
                                </td>

                                <td class="text-center">
                                    <button
                                        v-if="['ACTIVE', 'BANNED', 'INACTIVE'].includes(user.status)"
                                        type="button"
                                        class="btn btn-sm user-action-btn"
                                        :class="{
                                            'btn-outline-danger': user.status === 'ACTIVE',
                                            'btn-outline-success': ['BANNED', 'INACTIVE'].includes(user.status),
                                        }"
                                        :disabled="updatingUserId === user.usersId"
                                        @click="updateUserStatus(user)"
                                    >
                                        {{
                                            updatingUserId === user.usersId
                                                ? '處理中…'
                                                : user.status === 'ACTIVE'
                                                    ? '封鎖'
                                                    : user.status === 'BANNED'
                                                        ? '解除封鎖'
                                                        : '重新啟用'
                                        }}
                                    </button>

                                    <span v-else class="text-muted small">
                                        無可用操作
                                    </span>
                                </td>
                            </tr>

                            <tr v-if="filteredUsers.length === 0">
                                <td colspan="8" class="empty-cell">
                                    <Icon :icon="magnifyIcon" aria-hidden="true" />
                                    <strong>找不到符合條件的會員</strong>
                                    <span>請嘗試其他帳號、電子郵件或會員狀態。</span>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <div class="users-panel-footer">
                    <span>
                        目前顯示 {{ filteredUsers.length }} 位會員
                    </span>

                    <button
                        type="button"
                        class="btn btn-sm btn-outline-secondary refresh-btn"
                        :disabled="loadingUsers"
                        @click="loadUsers"
                    >
                        重新整理列表
                    </button>
                </div>
            </template>
        </div>
    </section>
</template>

<style scoped>
.admin-users-page {
    --member-purple: #8d72ed;
    --member-purple-dark: #5c4696;
    --member-border: #e9e5f2;
    --member-muted: #777487;
    padding-bottom: 2rem;
}

.users-panel {
    overflow: hidden;
    margin-top: 1.5rem;
    border: 1px solid var(--member-border);
    border-radius: 1rem;
    background: #fff;
    box-shadow: 0 0.35rem 1.5rem rgb(46 35 77 / 5%);
}

.users-panel-heading {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 1rem;
    padding: 1.35rem 1.5rem;
    border-bottom: 1px solid var(--member-border);
}

.users-title {
    margin: 0;
    color: #302b40;
    font-size: 1.15rem;
    font-weight: 800;
}

.users-subtitle {
    margin: 0.4rem 0 0;
    color: var(--member-muted);
    font-size: 0.875rem;
}

.users-count {
    padding: 0.4rem 0.8rem;
    border: 1px solid #e4dcff;
    border-radius: 999px;
    background: #f5f1ff;
    color: var(--member-purple-dark);
    font-size: 0.85rem;
    font-weight: 800;
    white-space: nowrap;
}

.users-toolbar {
    display: flex;
    align-items: flex-end;
    gap: 1rem;
    padding: 1.25rem 1.5rem;
    border-bottom: 1px solid var(--member-border);
    background: #fcfbfe;
}

.search-field {
    flex: 1;
    min-width: 220px;
}

.status-field {
    width: min(100%, 260px);
    flex-shrink: 0;
}

.toolbar-label {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    margin-bottom: 0.5rem;
    color: #494459;
    font-size: 0.875rem;
    font-weight: 750;
}

.toolbar-label :deep(svg) {
    color: var(--member-purple-dark);
    font-size: 1.05rem;
}

.input-with-icon {
    position: relative;
}

.input-with-icon > :deep(svg) {
    position: absolute;
    z-index: 1;
    top: 50%;
    left: 0.85rem;
    transform: translateY(-50%);
    color: #9288aa;
    font-size: 1.1rem;
    pointer-events: none;
}

.input-with-icon .form-control {
    min-height: 42px;
    padding-left: 2.6rem;
    border-color: #ded9e9;
    border-radius: 0.65rem;
    background: #fff;
}

.status-field .form-select {
    min-height: 42px;
    border-color: #ded9e9;
    border-radius: 0.65rem;
}

.input-with-icon .form-control:focus,
.status-field .form-select:focus {
    border-color: var(--member-purple);
    box-shadow: 0 0 0 0.2rem rgb(141 114 237 / 15%);
}

.users-table-wrap {
    padding: 0 0.5rem;
}

.users-table {
    margin-bottom: 0;
    white-space: nowrap;
}

.users-table thead th {
    padding: 1rem 0.9rem;
    border-bottom: 1px solid var(--member-border);
    background: #f8f6fc;
    color: #655e76;
    font-size: 0.8rem;
    font-weight: 800;
}

.users-table tbody td {
    padding: 1rem 0.9rem;
    border-color: #f0edf5;
    color: #464253;
    font-size: 0.875rem;
}

.users-table tbody tr:last-child td {
    border-bottom: 0;
}

.user-id {
    color: #817993 !important;
    font-variant-numeric: tabular-nums;
}

.user-account {
    color: #302b40 !important;
    font-weight: 750;
}

.user-email {
    white-space: normal !important;
    overflow-wrap: anywhere;
    min-width: 180px;
}

.date-cell {
    min-width: 155px;
    color: #777487 !important;
    font-size: 0.8rem !important;
}

.role-label {
    display: inline-block;
    padding: 0.25rem 0.55rem;
    border-radius: 0.4rem;
    background: #f1eef8;
    color: #655783;
    font-size: 0.75rem;
    font-weight: 800;
}

.status-badge {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.3rem 0.65rem;
    border-radius: 999px;
    font-size: 0.78rem;
    font-weight: 800;
    white-space: nowrap;
}

.status-dot {
    width: 0.42rem;
    height: 0.42rem;
    border-radius: 50%;
    background: currentColor;
}

.status-active {
    background: #e7f7ef;
    color: #17734b;
}

.status-inactive {
    background: #f0eff3;
    color: #686474;
}

.status-banned {
    background: #fff0f0;
    color: #b63842;
}

.status-deleted {
    background: #e9e7ec;
    color: #4c4855;
}

.user-action-btn {
    min-width: 76px;
    border-radius: 0.5rem;
    font-weight: 750;
    white-space: nowrap;
}

.users-panel-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.75rem;
    padding: 1rem 1.5rem;
    border-top: 1px solid var(--member-border);
    background: #fcfbfe;
    color: var(--member-muted);
    font-size: 0.82rem;
}

.refresh-btn {
    border-radius: 0.5rem;
}

.state-message {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.75rem;
    padding: 3rem 1rem;
    color: var(--member-muted);
}

.state-message p {
    margin: 0;
}

.empty-cell {
    padding: 3rem 1rem !important;
    text-align: center;
}

.empty-cell > :deep(svg) {
    display: block;
    margin: 0 auto 0.6rem;
    color: #a69abb;
    font-size: 1.8rem;
}

.empty-cell strong,
.empty-cell span {
    display: block;
}

.empty-cell strong {
    margin-bottom: 0.35rem;
    color: #494459;
}

.empty-cell span {
    color: var(--member-muted);
    font-size: 0.85rem;
    font-weight: 400;
}

@media (max-width: 700px) {
    .users-panel-heading,
    .users-toolbar,
    .users-panel-footer {
        padding-right: 1rem;
        padding-left: 1rem;
    }

    .users-toolbar {
        align-items: stretch;
        flex-direction: column;
    }

    .search-field,
    .status-field {
        width: 100%;
        min-width: 0;
    }
}
</style>
