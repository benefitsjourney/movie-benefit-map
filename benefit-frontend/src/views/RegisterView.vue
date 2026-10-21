<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import filmstripIcon from '@iconify-icons/mdi/filmstrip'
import Swal from 'sweetalert2'
import http from '@/plugins/axios'

const router = useRouter()

const account = ref('')
const password = ref('')
const confirmPassword = ref('')
const name = ref('')
const email = ref('')
const birthDate = ref('')
const isLoading = ref(false)

async function handleRegister() {
    // 檢查必填欄位
    const missingFields = []

    if (!account.value.trim()) missingFields.push('帳號')
    if (!password.value) missingFields.push('密碼')
    if (!confirmPassword.value) missingFields.push('確認密碼')
    if (!email.value.trim()) missingFields.push('電子郵件')

    if (missingFields.length > 0) {
        await Swal.fire({
            icon: 'warning',
            title: '尚有必填欄位未完成',
            text: `${missingFields.join('、')}還沒填寫完成，請補齊後再送出。`,
            confirmButtonText: '返回填寫',
        })
        return
    }

    // 密碼長度限制：8～16 碼
    if (password.value.length < 8 || password.value.length > 16) {
        await Swal.fire({
            icon: 'warning',
            title: '密碼長度不符',
            text: '密碼長度須為 8～16 碼。',
            confirmButtonText: '重新輸入',
        })
        return
    }

    
    // 確認密碼必須與密碼一致
    if (password.value !== confirmPassword.value) {
        await Swal.fire({
            icon: 'warning',
            title: '密碼不一致',
            text: '兩次輸入的密碼不相同，請重新確認。',
            confirmButtonText: '重新輸入',
        })
        return
    }


    try {
        isLoading.value = true

        await http.post('/auth/register', {
            account: account.value.trim(),
            password: password.value,
            email: email.value.trim(),
            name: name.value.trim() || account.value.trim(),
            birthDate: birthDate.value || null,
        })

        await Swal.fire({
            icon: 'success',
            title: '註冊成功',
            text: '現在可以使用帳號登入了！',
            confirmButtonText: '前往登入',
        })

        await router.push('/login')
    } catch (error) {
        const message =
            error.response?.data?.message ||
            error.response?.data?.detail ||
            '註冊失敗，請確認資料是否正確或帳號是否已被使用'

        await Swal.fire({
            icon: 'error',
            title: '註冊失敗',
            text: message,
            confirmButtonText: '返回修改',
        })
    } finally {
        isLoading.value = false
    }
}
</script>

<template>
    <div class="register-page">
        <section class="register-card">
            <div class="register-heading">
                <div class="register-icon">
                    <Icon :icon="filmstripIcon" />
                </div>

                <h1>建立會員帳號</h1>
                <p><span class="required-star">*</span> 號欄位必填</p>
            </div>

            <form @submit.prevent="handleRegister">
                <!-- 帳號 -->
                <div class="mb-3">
                    <label for="account" class="form-label">
                        帳號 <span class="required-star">*</span>
                    </label>
                    <input
                        id="account"
                        v-model="account"
                        type="text"
                        class="form-control register-input"
                        placeholder="請設定登入帳號"
                        autocomplete="username"
                        :disabled="isLoading"
                        required
                    />
                </div>

                <!-- 密碼 -->
                <div class="mb-3">
                    <label for="password" class="form-label">
                        密碼 <span class="required-star">*</span>
                    </label>
                    <input
                        id="password"
                        v-model="password"
                        type="password"
                        class="form-control register-input"
                        placeholder="請輸入 8～16 碼密碼"
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

                <!-- 確認密碼 -->
                <div class="mb-3">
                    <label for="confirmPassword" class="form-label">
                        確認密碼 <span class="required-star">*</span>
                    </label>
                    <input  id="confirmPassword"  v-model="confirmPassword" type="password"
                        class="form-control register-input"  placeholder="請再次輸入密碼"
                        autocomplete="new-password" minlength="8" maxlength="16" :disabled="isLoading"
                        required  />
                </div>

                <!-- 電子郵件 -->
                <div class="mb-3">
                    <label for="email" class="form-label">
                        電子郵件 <span class="required-star">*</span>
                    </label>
                    <input
                        id="email"
                        v-model="email"
                        type="email"
                        class="form-control register-input"
                        placeholder="請輸入電子郵件"
                        autocomplete="email"
                        :disabled="isLoading"
                        required
                    />
                </div>

                <!-- 暱稱 -->
                <div class="mb-3">
                    <label for="name" class="form-label">
                        暱稱
                    </label>
                    <input
                        id="name"
                        v-model="name"
                        type="text"
                        class="form-control register-input"
                        placeholder="請輸入顯示名稱（選填）"
                        autocomplete="name"
                        :disabled="isLoading"
                    />
                </div>

                <!-- 生日 -->
                <div class="mb-4">
                    <label for="birthDate" class="form-label">
                        生日
                    </label>
                    <input
                        id="birthDate"
                        v-model="birthDate"
                        type="date"
                        class="form-control register-input"
                        autocomplete="bday"
                        :disabled="isLoading"
                    />
                </div>

                <!-- 送出註冊 -->
                <button
                    type="submit"
                    class="btn register-button w-100"
                    :disabled="isLoading"
                >
                    {{ isLoading ? '註冊中...' : '建立帳號' }}
                </button>
            </form>

            <div class="register-footer">
                <span>已經有帳號了嗎？</span>
                <RouterLink to="/login">返回登入</RouterLink>
            </div>

            <RouterLink to="/map" class="back-link">
                返回地圖
            </RouterLink>
        </section>
    </div>
</template>

<style scoped>
.register-page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 2rem 1rem;
}

.register-card {
    width: 100%;
    max-width: 460px;
    padding: 2.5rem;
    background: var(--c-card);
    border: 1px solid var(--c-border);
    border-radius: 1.5rem;
    box-shadow: var(--shadow-soft);
}

.register-heading {
    text-align: center;
    margin-bottom: 2rem;
}

.register-icon {
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

.register-heading h1 {
    margin-bottom: 0.5rem;
}

.register-heading p {
    margin: 0;
    color: var(--c-muted);
    font-size: var(--text-min);
}

.required-star {
    color: #dc3545;
    font-weight: 800;
}

.form-label {
    font-weight: 700;
}

.register-input {
    min-height: 48px;
    border-color: var(--c-border);
    border-radius: 0.75rem;
    font-size: var(--text-control);
}

.register-input:focus {
    border-color: #d9a441;
    box-shadow: 0 0 0 0.2rem rgb(217 164 65 / 18%);
}

.password-hint {
    margin-top: 0.4rem;
    color: var(--c-muted);
    font-size: var(--text-min);
}

.register-button {
    min-height: 48px;
    color: #292b35;
    background: #ffcf70;
    border: none;
    border-radius: 0.75rem;
    font-size: var(--text-control);
    font-weight: 800;
}

.register-button:hover:not(:disabled) {
    background: #f6bd50;
}

.register-footer {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 0.4rem;
    margin-top: 1.5rem;
    font-size: var(--text-min);
}

.register-footer a,
.back-link {
    color: #805b16;
    font-weight: 700;
    text-decoration: none;
}

.back-link {
    display: block;
    margin-top: 1.25rem;
    text-align: center;
    font-size: var(--text-min);
}

@media (max-width: 480px) {
    .register-card {
        padding: 1.5rem;
    }
}
</style>
