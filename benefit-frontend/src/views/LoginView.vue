<script setup>
    import { ref } from 'vue'
    import { useRouter } from 'vue-router'
    import { Icon } from '@iconify/vue'
    import { useAuth } from '@/composables/useAuth'
    import filmstripIcon from '@iconify-icons/mdi/filmstrip'
    import Swal from 'sweetalert2'
    import http from '@/plugins/axios'

    const router = useRouter()
    const auth = useAuth()

    const account = ref('')
    const password = ref('')
    const isLoading = ref(false)

    const handleLogin = async () => {
    if (!account.value.trim() || !password.value) {
        await Swal.fire({
        icon: 'warning',
        title: '請填寫完整',
        text: '請輸入帳號與密碼',
        confirmButtonText: '確定',
        })
        return
    }

    try {
        isLoading.value = true

        const response = await http.post('/auth/login', {
        account: account.value.trim(),
        password: password.value,
        })

        const data = response.data

        // 儲存 JWT Token，讓 Axios 自動附加到後續請求
        auth.login(data.token, data.user)

        await Swal.fire({
        icon: 'success',
        title: '登入成功',
        text: `歡迎回來，${data.user?.name || data.user?.account || '會員'}！`,
        confirmButtonText: '開始使用',
        })

        // 目前先導向地圖頁
        await router.push('/map')
    } catch (error) {
        const message =
        error.response?.data?.message ||
        error.response?.data?.detail ||
        '帳號或密碼錯誤，請確認後再試一次'

        await Swal.fire({
        icon: 'error',
        title: '登入失敗',
        text: message,
        confirmButtonText: '重新輸入',
        })
    } finally {
        isLoading.value = false
    }
    }
</script>

<template>
    <div class="login-page">
        <section class="login-card">
        <div class="login-heading">
            <div class="login-icon">
            <Icon :icon="filmstripIcon" />
            </div>

            <h1>會員登入</h1>
            <p>登入特典地圖，繼續探索你的收藏旅程</p>
        </div>

        <form @submit.prevent="handleLogin">
            <div class="mb-3">
            <label for="account" class="form-label">帳號</label>
            <input
                id="account"
                v-model="account"
                type="text"
                class="form-control login-input"
                placeholder="請輸入帳號"
                autocomplete="username"
                :disabled="isLoading"
                required
            />
            </div>

            <div class="mb-4">
            <label for="password" class="form-label">密碼</label>
            <input
                id="password"
                v-model="password"
                type="password"
                class="form-control login-input"
                placeholder="請輸入密碼"
                autocomplete="current-password"
                :disabled="isLoading"
                required
            />
            </div>

            <div class="forgot-link">
            <RouterLink to="/forgot-password">
                忘記密碼？
            </RouterLink>
            </div>

            <button
            type="submit"
            class="btn login-button w-100"
            :disabled="isLoading"
            >
            {{ isLoading ? '登入中...' : '登入' }}
            </button>
        </form>

        <div class="login-footer">
            <span>還沒有帳號嗎？</span>
            <RouterLink to="/register">立即註冊</RouterLink>
        </div>

        <RouterLink to="/map" class="back-link">
            返回地圖
        </RouterLink>
        </section>
    </div>
</template>

<style scoped>
    .login-page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 2rem 1rem;
    }

    .login-card {
    width: 100%;
    max-width: 460px;
    padding: 2.5rem;
    background: var(--c-card);
    border: 1px solid var(--c-border);
    border-radius: 1.5rem;
    box-shadow: var(--shadow-soft);
    }

    .login-heading {
    text-align: center;
    margin-bottom: 2rem;
    }

    .login-icon {
    width: 64px;
    height: 64px;
    margin: 0 auto 1rem;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #805b16;
    background: #fff0c9;
    border-radius: 1.25rem;
    font-size: 2rem;
    }

    .login-heading h1 {
    margin-bottom: 0.5rem;
    }

    .login-heading p {
    margin: 0;
    color: var(--c-muted);
    font-size: var(--text-min);
    }

    .form-label {
    font-weight: 700;
    }

    .login-input {
    min-height: 48px;
    border-color: var(--c-border);
    border-radius: 0.75rem;
    font-size: var(--text-control);
    }

    .login-input:focus {
    border-color: #d9a441;
    box-shadow: 0 0 0 0.2rem rgb(217 164 65 / 18%);
    }

    .forgot-link {
    text-align: right;
    margin-top: -0.75rem;
    margin-bottom: 1.25rem;
    }

    .forgot-link a,
    .login-footer a,
    .back-link {
    color: #805b16;
    font-weight: 700;
    text-decoration: none;
    }

    .login-button {
    min-height: 48px;
    color: #292b35;
    background: #ffcf70;
    border: none;
    border-radius: 0.75rem;
    font-size: var(--text-control);
    font-weight: 800;
    }

    .login-button:hover:not(:disabled) {
    background: #f6bd50;
    }

    .login-footer {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 0.4rem;
    margin-top: 1.5rem;
    font-size: var(--text-min);
    }

    .back-link {
    display: block;
    margin-top: 1.25rem;
    text-align: center;
    font-size: var(--text-min);
    }

    @media (max-width: 480px) {
    .login-card {
        padding: 1.5rem;
    }
    }
</style>