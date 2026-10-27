<script setup>
    import { ref, computed, onMounted } from 'vue'

    const selectedPage = ref('users')

    // 會員管理
    const users = ref([])
    const loadingUsers = ref(false)
    const userError = ref('')
    const statusFilter = ref('ALL')
    const updatingUserId = ref(null)

    // 登入日誌
    const loginLogs = ref([])
    const loadingLogs = ref(false)
    const logError = ref('')
    const logPage = ref(0)
    const logTotalPages = ref(0)

    const pageOptions = [
    { value: 'users', label: '會員管理' },
    { value: 'logs', label: '登入日誌' },
    ]

    const statusOptions = [
    { value: 'ALL', label: '全部會員（不含已刪除）' },
    { value: 'ACTIVE', label: '正常' },
    { value: 'INACTIVE', label: '停用' },
    { value: 'BANNED', label: '封鎖' },
    { value: 'DELETED', label: '已刪除' },
    ]

    const filteredUsers = computed(() => {
    if (statusFilter.value === 'ALL') return users.value
    return users.value.filter(user => user.status === statusFilter.value)
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


    // 取得登入日誌
    async function loadLoginLogs(page = 0) {
    loadingLogs.value = true
    logError.value = ''

    try {
        const result = await apiRequest(`/admin/login-logs?page=${page}`)

        loginLogs.value = result.content || []
        logPage.value = result.number ?? page
        logTotalPages.value = result.totalPages ?? 0
    } catch (error) {
        logError.value = error.message || '讀取登入日誌失敗'
    } finally {
        loadingLogs.value = false
    }
    }

    function changePage(page) {
    if (page < 0 || page >= logTotalPages.value) return
    loadLoginLogs(page)
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
        ACTIVE: 'bg-success',
        INACTIVE: 'bg-secondary',
        BANNED: 'bg-danger',
        DELETED: 'bg-dark',
    }
    return classes[status] || 'bg-secondary'
    }

    function loginStatusLabel(status) {
    const value = String(status ?? '').toUpperCase()

    if (['SUCCESS', 'SUCCEEDED', '成功', 'TRUE'].includes(value)) return '成功'
    if (['FAIL', 'FAILED', '失敗', 'FALSE'].includes(value)) return '失敗'

    return status || '—'
    }

    function loginStatusClass(status) {
    return loginStatusLabel(status) === '成功'
        ? 'text-success'
        : 'text-danger'
    }

    function switchPage() {
    if (selectedPage.value === 'users') {
        loadUsers()
    } else {
        loadLoginLogs(0)
    }
    }

    onMounted(() => {
    loadUsers()
    })
</script>

<template>
    <div class="container-fluid py-4">
        <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
        <div>
            <h2 class="mb-1">會員管理</h2>
            <p class="text-muted mb-0">管理會員資料與查看登入紀錄</p>
        </div>

        <div class="d-flex align-items-center gap-2">
            <label for="adminPageSelect" class="form-label mb-0 text-nowrap">
            管理功能
            </label>

            <select
            id="adminPageSelect"
            v-model="selectedPage"
            class="form-select"
            @change="switchPage"
            >
            <option
                v-for="option in pageOptions"
                :key="option.value"
                :value="option.value"
            >
                {{ option.label }}
            </option>
            </select>
        </div>
        </div>

        <!-- 會員管理 -->
        <template v-if="selectedPage === 'users'">
        <div class="card shadow-sm border-0">
            <div class="card-body">
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-3">
                <h5 class="mb-0">會員列表</h5>

                <div class="d-flex align-items-center gap-2">
                <label for="statusFilter" class="form-label mb-0 text-nowrap">
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

            <div v-if="userError" class="alert alert-danger">
                {{ userError }}
            </div>

            <div v-if="loadingUsers" class="text-center py-5">
                <div class="spinner-border" role="status">
                <span class="visually-hidden">載入中</span>
                </div>
                <p class="text-muted mt-2 mb-0">正在載入會員資料…</p>
            </div>

            <div v-else class="table-responsive">
                <table class="table table-hover align-middle">
                <thead class="table-light">
                    <tr>
                    <th>會員編號</th>
                    <th>帳號</th>
                    <th>暱稱</th>
                    <th>電子郵件</th>
                    <th>角色</th>
                    <th>狀態</th>
                    <th>註冊時間</th>
                    <th>操作</th>
                    </tr>
                </thead>

                <tbody>
                    <tr v-for="user in filteredUsers" :key="user.usersId">
                    <td>{{ user.usersId }}</td>
                    <td>{{ user.account || '—' }}</td>
                    <td>{{ user.name || '—' }}</td>
                    <td>{{ user.email || '—' }}</td>
                    <td>{{ user.role || '—' }}</td>
                    <td>
                        <span class="badge" :class="statusClass(user.status)">
                        {{ statusLabel(user.status) }}
                        </span>
                    </td>
                    <td>{{ formatDate(user.createdAt) }}</td>
                    <td>
                        <button
                            v-if="['ACTIVE', 'BANNED', 'INACTIVE'].includes(user.status)"
                            class="btn btn-sm"
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
                        <span v-else class="text-muted small">無可用操作</span>
                    </td>
                    </tr>

                    <tr v-if="filteredUsers.length === 0">
                    <td colspan="8" class="text-center text-muted py-4">
                        沒有符合條件的會員資料
                    </td>
                    </tr>
                </tbody>
                </table>
            </div>

            <div class="text-muted small">
                共 {{ filteredUsers.length }} 位會員
            </div>
            </div>
        </div>
        </template>

        <!-- 登入日誌 -->
        <template v-else>
        <div class="card shadow-sm border-0">
            <div class="card-body">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h5 class="mb-0">登入日誌</h5>
                <button
                class="btn btn-outline-secondary btn-sm"
                :disabled="loadingLogs"
                @click="loadLoginLogs(logPage)"
                >
                重新整理
                </button>
            </div>

            <div v-if="logError" class="alert alert-danger">
                {{ logError }}
            </div>

            <div v-if="loadingLogs" class="text-center py-5">
                <div class="spinner-border" role="status">
                <span class="visually-hidden">載入中</span>
                </div>
                <p class="text-muted mt-2 mb-0">正在載入登入紀錄…</p>
            </div>

            <div v-else class="table-responsive">
                <table class="table table-hover align-middle">
                <thead class="table-light">
                    <tr>
                    <th>紀錄編號</th>
                    <th>會員編號</th>
                    <th>登入時間</th>
                    <th>登入結果</th>
                    <th>IP 位址</th>
                    <th>瀏覽器資訊</th>
                    </tr>
                </thead>

                <tbody>
                    <tr v-for="log in loginLogs" :key="log.loginLogId">
                    <td>{{ log.loginLogId }}</td>
                    <td>{{ log.usersId ?? '—' }}</td>
                    <td>{{ formatDate(log.loginTime) }}</td>
                    <td :class="loginStatusClass(log.loginStatus)">
                        {{ loginStatusLabel(log.loginStatus) }}
                    </td>
                    <td>{{ log.ipAddress || '—' }}</td>
                    <td class="small text-break" style="min-width: 220px; max-width: 360px;">
                        {{ log.userAgent || '—' }}
                    </td>
                    </tr>

                    <tr v-if="loginLogs.length === 0">
                    <td colspan="6" class="text-center text-muted py-4">
                        目前沒有登入紀錄
                    </td>
                    </tr>
                </tbody>
                </table>
            </div>

            <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
                <span class="text-muted small">
                第 {{ logTotalPages === 0 ? 0 : logPage + 1 }} / {{ logTotalPages }} 頁
                </span>

                <div class="btn-group">
                <button
                    class="btn btn-outline-secondary btn-sm"
                    :disabled="loadingLogs || logPage <= 0"
                    @click="changePage(logPage - 1)"
                >
                    上一頁
                </button>
                <button
                    class="btn btn-outline-secondary btn-sm"
                    :disabled="loadingLogs || logPage + 1 >= logTotalPages"
                    @click="changePage(logPage + 1)"
                >
                    下一頁
                </button>
                </div>
            </div>
            </div>
        </div>
</template>
    </div>
</template>