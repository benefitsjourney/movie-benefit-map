
<script setup>
import { ref, onMounted } from 'vue'
import PageIntro from '@/components/layout/PageIntro.vue'
import { Icon } from '@iconify/vue'
import historyIcon from '@iconify-icons/mdi/history'
import refreshIcon from '@iconify-icons/mdi/refresh'
import accountIcon from '@iconify-icons/mdi/account'
import checkCircleIcon from '@iconify-icons/mdi/check-circle'
import closeCircleIcon from '@iconify-icons/mdi/close-circle'
import clockOutlineIcon from '@iconify-icons/mdi/clock-outline'

const loginLogs = ref([])
const loadingLogs = ref(false)
const logError = ref('')
const logPage = ref(0)
const logTotalPages = ref(0)
const logTotalElements = ref(0)

// 三個獨立的搜尋欄位
const memberKeyword = ref('')
const deviceKeyword = ref('')
const ipKeyword = ref('')

// 已套用的搜尋條件：翻頁時沿用
const activeMemberKeyword = ref('')
const activeDeviceKeyword = ref('')
const activeIpKeyword = ref('')

// 是否有套用搜尋條件
function hasActiveSearch() {
    return Boolean(
        activeMemberKeyword.value ||
        activeDeviceKeyword.value ||
        activeIpKeyword.value
    )
}

// 取得 Token
function getToken() {
    return localStorage.getItem('token')
}

// 共用 API 請求
async function apiRequest(url) {
    const token = getToken()

    const response = await fetch(`${import.meta.env.VITE_API_URL}${url}`, {
        headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
    })

    if (!response.ok) {
        const message = await response.text()
        throw new Error(message || `請求失敗（${response.status}）`)
    }

    return response.json()
}

// 取得登入日誌
async function loadLoginLogs(page = 0) {
    loadingLogs.value = true
    logError.value = ''

    try {
        let url

        if (hasActiveSearch()) {
            const params = new URLSearchParams()
            params.set('page', String(page))

            // 只傳送有填寫的條件
            if (activeMemberKeyword.value) {
                params.set('memberKeyword', activeMemberKeyword.value)
            }

            if (activeDeviceKeyword.value) {
                params.set('deviceKeyword', activeDeviceKeyword.value)
            }

            if (activeIpKeyword.value) {
                params.set('ipKeyword', activeIpKeyword.value)
            }

            url = `/admin/login-logs/search?${params.toString()}`
        } else {
            url = `/admin/login-logs?page=${page}`
        }

        const result = await apiRequest(url)

        loginLogs.value = result.content || []
        logPage.value = result.number ?? page
        logTotalPages.value = result.totalPages ?? 0
        logTotalElements.value = result.totalElements ?? 0
    } catch (error) {
        logError.value = error.message || '讀取登入日誌失敗'
    } finally {
        loadingLogs.value = false
    }
}

// 執行搜尋
function searchLoginLogs() {
    activeMemberKeyword.value = memberKeyword.value.trim()
    activeDeviceKeyword.value = deviceKeyword.value.trim()
    activeIpKeyword.value = ipKeyword.value.trim()

    loadLoginLogs(0)
}

// 清除所有搜尋條件
function clearSearch() {
    memberKeyword.value = ''
    deviceKeyword.value = ''
    ipKeyword.value = ''

    activeMemberKeyword.value = ''
    activeDeviceKeyword.value = ''
    activeIpKeyword.value = ''

    loadLoginLogs(0)
}

// 切換頁數
function changePage(page) {
    if (page < 0 || page >= logTotalPages.value) return
    loadLoginLogs(page)
}

// 格式化日期
function formatDate(value) {
    if (!value) return '—'

    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return value

    return date.toLocaleString('zh-TW', { hour12: false })
}


/**
 * 將完整 User-Agent 轉成容易閱讀的設備資訊
 * 例如：Windows · Chrome 155
 */
function formatDevice(userAgent) {
    if (!userAgent) return '未知設備'

    // 判斷作業系統
    let os = '未知系統'

    if (/iPhone|iPad|iPod/i.test(userAgent)) {
        os = 'iOS'
    } else if (/Android/i.test(userAgent)) {
        os = 'Android'
    } else if (/Windows/i.test(userAgent)) {
        os = 'Windows'
    } else if (/Mac OS X|Macintosh/i.test(userAgent)) {
        os = 'macOS'
    } else if (/Linux/i.test(userAgent)) {
        os = 'Linux'
    }

    // 判斷瀏覽器：注意順序，避免 Edge 被誤判成 Chrome
    let browser = '未知瀏覽器'
    let version = ''

    const browserPatterns = [
        { name: 'Edge', regex: /Edg(?:e|A|iOS)?\/([\d.]+)/ },
        { name: 'Opera', regex: /OPR\/([\d.]+)/ },
        { name: 'Samsung Internet', regex: /SamsungBrowser\/([\d.]+)/ },
        { name: 'Firefox', regex: /Firefox\/([\d.]+)/ },
        { name: 'Chrome', regex: /Chrome\/([\d.]+)/ },
        { name: 'Safari', regex: /Version\/([\d.]+).*Safari\// },
    ]

    for (const item of browserPatterns) {
        const match = userAgent.match(item.regex)

        if (match) {
            browser = item.name
            version = match[1]
            break
        }
    }

    // 只顯示主要版本，避免版本字串太長
    const majorVersion = version
        ? ` ${version.split('.')[0]}`
        : ''

    return `${os} · ${browser}${majorVersion}`
}


// 登入結果文字
function loginStatusLabel(status) {
    const value = String(status ?? '').toUpperCase()

    if (['SUCCESS', 'SUCCEEDED', '成功', 'TRUE'].includes(value)) {
        return '成功'
    }

    if (['FAIL', 'FAILED', '失敗', 'FALSE'].includes(value)) {
        return '失敗'
    }

    return status || '未知'
}

// 登入結果樣式
function loginStatusClass(status) {
    const label = loginStatusLabel(status)

    if (label === '成功') return 'status-success'
    if (label === '失敗') return 'status-failed'

    return 'status-unknown'
}

// 登入結果圖示
function loginStatusIcon(status) {
    const label = loginStatusLabel(status)

    if (label === '成功') return checkCircleIcon
    if (label === '失敗') return closeCircleIcon

    return clockOutlineIcon
}

onMounted(() => {
    loadLoginLogs()
})
</script>

<template>
    <div class="login-logs-page container-fluid py-4 px-3 px-lg-4">
        <!-- 頁面標題 -->
        <PageIntro
            kicker="✦ LOGIN ACTIVITY"
            lead="登入"
            mark="日誌"
            description="查看會員登入時間、登入結果與連線資訊。"
            :icon-left="historyIcon"
            :icon-right="accountIcon"
            :icon-top="clockOutlineIcon"
            theme="purple"
        />

        <!-- 搜尋工具列 -->
        <div class="search-toolbar">
            <div class="search-field">
                <label for="memberKeyword" class="form-label">
                    會員
                </label>

                <input
                    id="memberKeyword"
                    v-model="memberKeyword"
                    type="text"
                    class="form-control"
                    placeholder="會員編號、帳號或姓名"
                    @keyup.enter="searchLoginLogs"
                />
            </div>

            <div class="search-field">
                <label for="deviceKeyword" class="form-label">
                    設備
                </label>

                <input
                    id="deviceKeyword"
                    v-model="deviceKeyword"
                    type="text"
                    class="form-control"
                    placeholder="瀏覽器或作業系統"
                    @keyup.enter="searchLoginLogs"
                />
            </div>

            <div class="search-field">
                <label for="ipKeyword" class="form-label">
                    IP 位址
                </label>

                <input
                    id="ipKeyword"
                    v-model="ipKeyword"
                    type="text"
                    class="form-control"
                    placeholder="完整或部分 IP"
                    @keyup.enter="searchLoginLogs"
                />
            </div>

            <div class="search-actions">
                <button
                    type="button"
                    class="btn search-btn"
                    :disabled="loadingLogs"
                    @click="searchLoginLogs"
                >
                    <Icon icon="mdi:magnify" />
                    搜尋
                </button>

                <button
                    type="button"
                    class="btn clear-btn"
                    :disabled="loadingLogs"
                    @click="clearSearch"
                >
                    <Icon icon="mdi:refresh" />
                    清除
                </button>
            </div>
        </div>

        <!-- 日誌卡片 -->
        <section class="logs-card">
            <div
                class="logs-card-header d-flex flex-wrap justify-content-between align-items-center gap-3"
            >
                <div class="d-flex align-items-center gap-3">
                    <div class="header-icon">
                        <Icon :icon="historyIcon" />
                    </div>

                    <div>
                        <h5 class="mb-1">會員登入紀錄</h5>

                        <p class="mb-0 text-muted small">
                            共 {{ logTotalElements }} 筆紀錄 · 每頁顯示 15 筆
                            <span v-if="hasActiveSearch()">
                                · 已套用搜尋條件
                            </span>
                        </p>
                    </div>
                </div>

                <button
                    type="button"
                    class="btn refresh-btn d-inline-flex align-items-center gap-2"
                    :disabled="loadingLogs"
                    @click="loadLoginLogs(logPage)"
                >
                    <span
                        v-if="loadingLogs"
                        class="spinner-border spinner-border-sm"
                        role="status"
                    ></span>

                    <Icon v-else :icon="refreshIcon" />
                    重新整理
                </button>
            </div>

            <div class="logs-card-body">
                <!-- 錯誤訊息 -->
                <div v-if="logError" class="error-panel mb-4">
                    <Icon :icon="closeCircleIcon" />

                    <div>
                        <strong>操作失敗</strong>
                        <p class="mb-0">{{ logError }}</p>
                    </div>

                    <button
                        type="button"
                        class="btn btn-sm btn-outline-danger ms-auto"
                        :disabled="loadingLogs"
                        @click="loadLoginLogs(logPage)"
                    >
                        重試
                    </button>
                </div>

                <!-- 載入中 -->
                <div v-if="loadingLogs" class="loading-panel">
                    <div
                        class="spinner-border loading-spinner"
                        role="status"
                    >
                        <span class="visually-hidden">載入中</span>
                    </div>

                    <p class="mb-0 mt-3">正在載入登入紀錄…</p>
                </div>

                <!-- 資料表格 -->
                <div v-else class="table-responsive logs-table-wrap">
                    <table class="table logs-table align-middle mb-0">
                        <thead>
                            <tr>
                                <th class="id-column">紀錄編號</th>
                                <th class="id-column">會員編號</th>
                                <th class="name-column">會員姓名</th>
                                <th class="time-column">登入時間</th>
                                <th class="status-column">登入結果</th>
                                <th class="ip-column">IP 位址</th>
                                <th class="browser-column">瀏覽器資訊</th>
                            </tr>
                        </thead>

                        <tbody>
                            <tr
                                v-for="log in loginLogs"
                                :key="log.loginLogId"
                            >
                                <td>
                                    <span class="record-id">
                                        #{{ log.loginLogId ?? '—' }}
                                    </span>
                                </td>

                                <td>
                                    <span class="member-id">
                                        <Icon :icon="accountIcon" />
                                        {{ log.usersId ?? '—' }}
                                    </span>
                                </td>

                                <td>
                                    <span class="member-name">
                                        {{ log.userName || '—' }}
                                    </span>
                                </td>

                                <td class="time-cell">
                                    {{ formatDate(log.loginTime) }}
                                </td>

                                <td>
                                    <span
                                        class="status-badge"
                                        :class="loginStatusClass(log.loginStatus)"
                                    >
                                        <Icon
                                            :icon="loginStatusIcon(log.loginStatus)"
                                        />
                                        {{ loginStatusLabel(log.loginStatus) }}
                                    </span>
                                </td>

                                <td>
                                    <span class="ip-address">
                                        {{ log.ipAddress || '—' }}
                                    </span>
                                </td>

                                <td>
                                    <div class="browser-info" :title="log.userAgent || '沒有設備資訊'">
                                        {{ formatDevice(log.userAgent)}}
                                    </div>
                                </td>
                            </tr>

                            <tr v-if="loginLogs.length === 0">
                                <td colspan="7">
                                    <div class="empty-state">
                                        <div class="empty-icon">
                                            <Icon :icon="historyIcon" />
                                        </div>

                                        <h6>
                                            {{
                                                hasActiveSearch()
                                                    ? '找不到符合的登入紀錄'
                                                    : '目前沒有登入紀錄'
                                            }}
                                        </h6>

                                        <p class="mb-0">
                                            {{
                                                hasActiveSearch()
                                                    ? '請確認搜尋條件，或嘗試其他搜尋方式。'
                                                    : '目前沒有可顯示的資料。'
                                            }}
                                        </p>
                                    </div>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <!-- 分頁 -->
                <div
                    class="pagination-area d-flex flex-wrap justify-content-between align-items-center gap-3"
                >
                    <div class="page-info">
                        <span class="text-muted">目前頁數</span>

                        <strong>
                            {{ logTotalPages === 0 ? 0 : logPage + 1 }}
                        </strong>

                        <span class="text-muted">
                            / {{ logTotalPages }} 頁
                        </span>
                    </div>

                    <div class="d-flex align-items-center gap-2">
                        <button
                            type="button"
                            class="btn page-btn"
                            :disabled="loadingLogs || logPage <= 0"
                            @click="changePage(logPage - 1)"
                        >
                            <Icon icon="mdi:chevron-left" />
                            上一頁
                        </button>

                        <button
                            type="button"
                            class="btn page-btn"
                            :disabled="
                                loadingLogs ||
                                logTotalPages === 0 ||
                                logPage + 1 >= logTotalPages
                            "
                            @click="changePage(logPage + 1)"
                        >
                            下一頁
                            <Icon icon="mdi:chevron-right" />
                        </button>
                    </div>
                </div>
            </div>
        </section>
    </div>
</template>

<style scoped>
.login-logs-page {
    color: #292b35;
}

/* 搜尋工具列 */
.search-toolbar {
    display: flex;
    align-items: flex-end;
    gap: 14px;
    margin-bottom: 22px;
    padding: 20px;
    border: 1px solid #ece5f7;
    border-radius: 16px;
    background: #fff;
    box-shadow: 0 6px 22px rgba(43, 34, 73, 0.05);
}

.search-field {
    flex: 1;
    min-width: 180px;
}

.search-toolbar .form-label {
    display: block;
    margin-bottom: 7px;
    color: #625879;
    font-size: 0.85rem;
    font-weight: 700;
}

.search-toolbar .form-control {
    min-height: 42px;
    border-color: #ded5ed;
    border-radius: 9px;
    font-size: 0.9rem;
}

.search-toolbar .form-control:focus {
    border-color: #9573d1;
    box-shadow: 0 0 0 0.2rem rgba(121, 82, 189, 0.12);
}

.search-actions {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
}

.search-btn,
.clear-btn {
    display: inline-flex;
    min-height: 42px;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 8px 14px;
    border-radius: 9px;
    font-size: 0.9rem;
    font-weight: 600;
    white-space: nowrap;
}

.search-btn {
    border: 1px solid #7952bd;
    background: #7952bd;
    color: #fff;
}

.search-btn:hover:not(:disabled) {
    border-color: #62409f;
    background: #62409f;
    color: #fff;
}

.clear-btn {
    border: 1px solid #ddd4ee;
    background: #fff;
    color: #654a91;
}

.clear-btn:hover:not(:disabled) {
    border-color: #7952bd;
    background: #f4effc;
    color: #654a91;
}

.search-btn:disabled,
.clear-btn:disabled {
    cursor: not-allowed;
    opacity: 0.6;
}

/* 主要卡片 */
.logs-card {
    overflow: hidden;
    border: 1px solid #ece8f5;
    border-radius: 18px;
    background: #fff;
    box-shadow: 0 8px 28px rgba(43, 34, 73, 0.06);
}

.logs-card-header {
    padding: 22px 24px;
    border-bottom: 1px solid #eeeaf5;
    background: linear-gradient(135deg, #fff 0%, #faf8ff 100%);
}

.logs-card-header h5 {
    color: #29243b;
    font-size: 1.05rem;
    font-weight: 700;
}

.header-icon {
    display: grid;
    width: 46px;
    height: 46px;
    flex-shrink: 0;
    place-items: center;
    border-radius: 14px;
    background: #eee7ff;
    color: #7450bd;
    font-size: 1.45rem;
}

.refresh-btn {
    padding: 9px 15px;
    border: 1px solid #d9c9fa;
    border-radius: 10px;
    background: #fff;
    color: #7046b8;
    font-weight: 600;
    transition: 0.2s ease;
}

.refresh-btn:hover:not(:disabled) {
    border-color: #7046b8;
    background: #7046b8;
    color: #fff;
}

.refresh-btn:disabled {
    cursor: not-allowed;
    opacity: 0.65;
}

/* 卡片內容 */
.logs-card-body {
    padding: 24px;
}

/* 表格 */
.logs-table-wrap {
    border: 1px solid #eeeaf5;
    border-radius: 12px;
}

.logs-table {
    min-width: 1050px;
}

.logs-table thead th {
    padding: 15px 14px;
    border-bottom: 1px solid #e9e2f5;
    background: #f7f4fc;
    color: #625879;
    font-size: 0.82rem;
    font-weight: 700;
    white-space: nowrap;
}

.logs-table tbody td {
    padding: 15px 14px;
    border-bottom-color: #f0edf5;
    color: #464052;
    font-size: 0.9rem;
}

.logs-table tbody tr:last-child td {
    border-bottom: 0;
}

.logs-table tbody tr {
    transition: background-color 0.15s ease;
}

.logs-table tbody tr:hover {
    background: #fbf9ff;
}

.id-column {
    width: 100px;
}

.name-column {
    min-width: 130px;
}

.time-column {
    min-width: 180px;
}

.status-column {
    width: 110px;
}

.ip-column {
    min-width: 130px;
}

.browser-column {
    min-width: 220px;
}

.record-id {
    color: #6e4bab;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
}

.member-id {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    color: #655b77;
    white-space: nowrap;
}

.member-id :deep(svg) {
    font-size: 1rem;
}

.member-name {
    color: #393147;
    font-weight: 600;
}

.time-cell {
    white-space: nowrap;
    font-variant-numeric: tabular-nums;
}

.ip-address {
    font-family: Consolas, Monaco, monospace;
    font-size: 0.85rem;
    white-space: nowrap;
}

/* 登入結果標籤 */
.status-badge {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 6px 10px;
    border-radius: 999px;
    font-size: 0.8rem;
    font-weight: 700;
    white-space: nowrap;
}

.status-success {
    background: #e5f7ed;
    color: #218653;
}

.status-failed {
    background: #fff0f0;
    color: #c83e4d;
}

.status-unknown {
    background: #f0edf5;
    color: #746a83;
}

.status-badge :deep(svg) {
    font-size: 1rem;
}

/* 瀏覽器資訊 */
.browser-info {
    min-width: 180px;
    max-width: 360px;
    overflow-wrap: anywhere;
    color: #756e81;
    font-size: 0.8rem;
    line-height: 1.6;
}

/* 載入、空資料與錯誤 */
.loading-panel {
    display: flex;
    min-height: 260px;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #786b91;
}

.loading-spinner {
    color: #7952bd;
}

.empty-state {
    padding: 48px 16px;
    text-align: center;
    color: #81778f;
}

.empty-icon {
    display: grid;
    width: 58px;
    height: 58px;
    margin: 0 auto 14px;
    place-items: center;
    border-radius: 18px;
    background: #f1ebfc;
    color: #7952bd;
    font-size: 1.8rem;
}

.empty-state h6 {
    color: #393147;
    font-weight: 700;
}

.empty-state p {
    font-size: 0.9rem;
}

.error-panel {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 16px;
    border: 1px solid #f4c9ce;
    border-radius: 12px;
    background: #fff6f6;
    color: #b83243;
}

.error-panel > :deep(svg) {
    flex-shrink: 0;
    font-size: 1.4rem;
}

.error-panel p {
    margin-top: 3px;
    font-size: 0.9rem;
    overflow-wrap: anywhere;
}

/* 分頁 */
.pagination-area {
    margin-top: 22px;
    padding-top: 20px;
    border-top: 1px solid #eeeaf5;
}

.page-info {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 0.9rem;
}

.page-info strong {
    display: inline-grid;
    min-width: 30px;
    height: 30px;
    padding: 0 7px;
    place-items: center;
    border-radius: 8px;
    background: #eee7ff;
    color: #7046b8;
}

.page-btn {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 8px 13px;
    border: 1px solid #ddd4ee;
    border-radius: 9px;
    background: #fff;
    color: #654a91;
    font-size: 0.88rem;
    font-weight: 600;
    transition: 0.2s ease;
}

.page-btn:hover:not(:disabled) {
    border-color: #7952bd;
    background: #7952bd;
    color: #fff;
}

.page-btn:disabled {
    background: #f8f7fa;
    color: #b5afbf;
    cursor: not-allowed;
}

/* 平板與手機 */
@media (max-width: 900px) {
    .search-toolbar {
        flex-wrap: wrap;
    }

    .search-field {
        flex: 1 1 calc(50% - 14px);
        min-width: 0;
    }

    .search-actions {
        width: 100%;
    }
}

@media (max-width: 768px) {
    .logs-card-header {
        padding: 18px;
    }

    .logs-card-body {
        padding: 16px;
    }

    .refresh-btn {
        justify-content: center;
        width: 100%;
    }

    .pagination-area {
        align-items: stretch !important;
    }

    .page-info {
        width: 100%;
    }

    .pagination-area > div:last-child {
        width: 100%;
    }

    .page-btn {
        flex: 1;
        justify-content: center;
    }
}

@media (max-width: 650px) {
    .search-toolbar {
        flex-direction: column;
        align-items: stretch;
        padding: 16px;
    }

    .search-field {
        width: 100%;
        min-width: 0;
    }

    .search-actions {
        display: grid;
        grid-template-columns: 1fr 1fr;
    }
}
</style>
