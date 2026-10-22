import { computed, ref } from 'vue'

// 從 Local Storage 讀取會員資料
function loadUser() {
    try {
        return JSON.parse(localStorage.getItem('user') || 'null')
    } catch {
        return null
    }
}

// 共用登入狀態
const token = ref(localStorage.getItem('token') || '')
const user = ref(loadUser())

// 是否已登入
const isLoggedIn = computed(() => Boolean(token.value))

// 是否為有效狀態的管理員
const isAdmin = computed(() =>
    isLoggedIn.value &&
    user.value?.role === 'ADMIN' &&
    user.value?.status === 'ACTIVE'
)

// 登入成功後儲存 Token 與會員資料
function login(loginToken, loginUser) {
    token.value = loginToken
    user.value = loginUser

    localStorage.setItem('token', loginToken)
    localStorage.setItem('user', JSON.stringify(loginUser))
}

// 登出並清除登入資訊
function logout() {
    token.value = ''
    user.value = null

    localStorage.removeItem('token')
    localStorage.removeItem('user')
}

// 重新讀取 Local Storage
function refreshAuth() {
    token.value = localStorage.getItem('token') || ''
    user.value = loadUser()
}

export function useAuth() {
    return {
        token,
        user,
        isLoggedIn,
        isAdmin,
        login,
        logout,
        refreshAuth,
    }
}
