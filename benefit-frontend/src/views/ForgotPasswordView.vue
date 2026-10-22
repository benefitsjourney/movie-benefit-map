    <script setup>
    import { ref } from 'vue'
    import { useRouter } from 'vue-router'
    import { Icon } from '@iconify/vue'
    import filmstripIcon from '@iconify-icons/mdi/filmstrip'
    import Swal from 'sweetalert2'
    import http from '@/plugins/axios'

    const router = useRouter()

    const email = ref('')
    const isLoading = ref(false)

    async function handleForgotPassword() {
    if (!email.value.trim()) {
        await Swal.fire({
        icon: 'warning',
        title: '請輸入電子郵件',
        text: '請填寫註冊時使用的電子郵件',
        confirmButtonText: '確定',
        })
        return
    }

    try {
        isLoading.value = true

        await http.post('/auth/password/forgot', {
        email: email.value.trim(),
        })

        await Swal.fire({
        icon: 'success',
        title: '已送出申請',
        text: '如果這個電子郵件符合重設條件，請留意相關通知。',
        confirmButtonText: '返回登入',
        })

        await router.push('/login')
    } catch (error) {
        const message =
        error.response?.data?.message ||
        error.response?.data?.detail ||
        '目前無法處理重設密碼申請，請稍後再試。'

        await Swal.fire({
        icon: 'error',
        title: '申請失敗',
        text: message,
        confirmButtonText: '重新操作',
        })
    } finally {
        isLoading.value = false
    }
    }
    </script>

    <template>
    <div class="forgot-page">
        <section class="forgot-card">
        <div class="forgot-heading">
            <div class="forgot-icon">
            <Icon :icon="filmstripIcon" />
            </div>

            <h1>忘記密碼</h1>
            <p>請輸入註冊時使用的電子郵件，以申請重設密碼。</p>
        </div>

        <form @submit.prevent="handleForgotPassword">
            <div class="mb-4">
            <label for="email" class="form-label">電子郵件</label>
            <input
                id="email"
                v-model="email"
                type="email"
                class="form-control forgot-input"
                placeholder="請輸入電子郵件"
                autocomplete="email"
                :disabled="isLoading"
                required
            />
            </div>

            <button
            type="submit"
            class="btn forgot-button w-100"
            :disabled="isLoading"
            >
            {{ isLoading ? '送出中...' : '申請重設密碼' }}
            </button>
        </form>

        <RouterLink to="/login" class="back-link">
            返回登入
        </RouterLink>
        </section>
    </div>
    </template>

    <style scoped>
    .forgot-page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 2rem 1rem;
    }

    .forgot-card {
    width: 100%;
    max-width: 460px;
    padding: 2.5rem;
    background: var(--c-card);
    border: 1px solid var(--c-border);
    border-radius: 1.5rem;
    box-shadow: var(--shadow-soft);
    }

    .forgot-heading {
    text-align: center;
    margin-bottom: 2rem;
    }

    .forgot-icon {
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

    .forgot-heading h1 {
    margin-bottom: 0.5rem;
    }

    .forgot-heading p {
    margin: 0;
    color: var(--c-muted);
    font-size: var(--text-min);
    }

    .form-label {
    font-weight: 700;
    }

    .forgot-input {
    min-height: 48px;
    border-color: var(--c-border);
    border-radius: 0.75rem;
    font-size: var(--text-control);
    }

    .forgot-input:focus {
    border-color: #d9a441;
    box-shadow: 0 0 0 0.2rem rgb(217 164 65 / 18%);
    }

    .forgot-button {
    min-height: 48px;
    color: #292b35;
    background: #ffcf70;
    border: none;
    border-radius: 0.75rem;
    font-size: var(--text-control);
    font-weight: 800;
    }

    .forgot-button:hover:not(:disabled) {
    background: #f6bd50;
    }

    .back-link {
    display: block;
    margin-top: 1.5rem;
    text-align: center;
    color: #805b16;
    font-weight: 700;
    font-size: var(--text-min);
    text-decoration: none;
    }

    @media (max-width: 480px) {
    .forgot-card {
        padding: 1.5rem;
    }
    }
    </style>