<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import filmstripIcon from '@iconify-icons/mdi/filmstrip'
import Swal from 'sweetalert2'
import http from '@/plugins/axios'

    const route = useRoute()
    const router = useRouter()

    // 自動讀取郵件連結中的 Token
    const token = computed(() => {
    const value = route.query.token
    return typeof value === 'string' ? value : ''
    })

    const password = ref('')
    const confirmPassword = ref('')
    const isLoading = ref(false)

    async function handleResetPassword() {
    if (!token.value) {
        await Swal.fire({
        icon: 'error',
        title: '重設連結無效',
        text: '網址中沒有重設密碼 Token，請重新申請密碼重設。',
        confirmButtonText: '前往忘記密碼',
        })

        await router.push('/forgot-password')
        return
    }

    if (!password.value || !confirmPassword.value) {
        const missingFields = []

        if (!password.value) missingFields.push('新密碼')
        if (!confirmPassword.value) missingFields.push('確認密碼')

        await Swal.fire({
        icon: 'warning',
        title: '尚有欄位未填寫',
        text: `${missingFields.join('、')}還沒填寫完成。`,
        confirmButtonText: '返回填寫',
        })
        return
    }

    if (
        password.value.length < 8 ||
        password.value.length > 16
    ) {
        await Swal.fire({
        icon: 'warning',
        title: '密碼長度不符',
        text: '新密碼長度須為 8～16 碼。',
        confirmButtonText: '重新輸入',
        })
        return
    }

    if (password.value !== confirmPassword.value) {
        await Swal.fire({
        icon: 'warning',
        title: '密碼不一致',
        text: '兩次輸入的密碼不同，請重新確認。',
        confirmButtonText: '重新輸入',
        })
        return
    }

    try {
        isLoading.value = true

        await http.post('/auth/password/reset', {
        token: token.value,
        newPassword: password.value,
        })

        await Swal.fire({
        icon: 'success',
        title: '密碼重設成功',
        text: '請使用新密碼重新登入。',
        confirmButtonText: '前往登入',
        })

        await router.push('/login')
    } catch (error) {
        const message =
        error.response?.data?.message ||
        error.response?.data?.detail ||
        '重設密碼失敗，連結可能已過期或已經使用，請重新申請。'

        await Swal.fire({
        icon: 'error',
        title: '重設密碼失敗',
        text: message,
        confirmButtonText: '我知道了',
        })
    } finally {
        isLoading.value = false
    }
}
</script>

    <template>
    <div class="reset-page">
        <section class="reset-card">
        <div class="reset-heading">
            <div class="reset-icon">
            <Icon :icon="filmstripIcon" />
            </div>

            <h1>重設密碼</h1>
            <p>請設定新的登入密碼，完成後即可重新登入。</p>
        </div>

        <form @submit.prevent="handleResetPassword">
            <div class="mb-3">
            <label for="password" class="form-label">
                新密碼 <span class="required-star">*</span>
            </label>

            <input
                id="password"
                v-model="password"
                type="password"
                class="form-control reset-input"
                placeholder="請輸入 8～16 碼新密碼"
                autocomplete="new-password"
                minlength="8"
                maxlength="16"
                :disabled="isLoading"
                required
            />

            <div class="password-hint">
                密碼長度須為 8～16 碼。
            </div>
            </div>

            <div class="mb-4">
            <label for="confirmPassword" class="form-label">
                確認新密碼 <span class="required-star">*</span>
            </label>

            <input
                id="confirmPassword"
                v-model="confirmPassword"
                type="password"
                class="form-control reset-input"
                placeholder="請再次輸入新密碼"
                autocomplete="new-password"
                minlength="8"
                maxlength="16"
                :disabled="isLoading"
                required
            />
            </div>

            <button
            type="submit"
            class="btn reset-button w-100"
            :disabled="isLoading"
            >
            {{ isLoading ? '重設中...' : '確認重設密碼' }}
            </button>
        </form>

        <RouterLink to="/login" class="back-link">
            返回登入
        </RouterLink>
        </section>
    </div>
</template>

    <style scoped>
    .reset-page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 2rem 1rem;
    }

    .reset-card {
    width: 100%;
    max-width: 460px;
    padding: 2.5rem;
    background: var(--c-card);
    border: 1px solid var(--c-border);
    border-radius: 1.5rem;
    box-shadow: var(--shadow-soft);
    }

    .reset-heading {
    text-align: center;
    margin-bottom: 2rem;
    }

    .reset-icon {
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

    .reset-heading h1 {
    margin-bottom: 0.5rem;
    }

    .reset-heading p {
    margin: 0;
    color: var(--c-muted);
    font-size: var(--text-min);
    }

    .form-label {
    font-weight: 700;
    }

    .required-star {
    color: #dc3545;
    font-weight: 800;
    }

    .reset-input {
    min-height: 48px;
    border-color: var(--c-border);
    border-radius: 0.75rem;
    font-size: var(--text-control);
    }

    .reset-input:focus {
    border-color: #d9a441;
    box-shadow: 0 0 0 0.2rem rgb(217 164 65 / 18%);
    }

    .password-hint {
    margin-top: 0.4rem;
    color: var(--c-muted);
    font-size: var(--text-min);
    }

    .reset-button {
    min-height: 48px;
    color: #292b35;
    background: #ffcf70;
    border: none;
    border-radius: 0.75rem;
    font-size: var(--text-control);
    font-weight: 800;
    }

    .reset-button:hover:not(:disabled) {
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
    .reset-card {
        padding: 1.5rem;
    }
    }
</style>