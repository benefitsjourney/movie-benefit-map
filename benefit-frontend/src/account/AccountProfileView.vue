<script setup>
import { ref, onMounted } from 'vue'
import http from '@/plugins/axios'
import Swal from 'sweetalert2'

// 會員資料
const form = ref({
  name: '',
  email: '',
  birthDate: '',
})

// 載入及儲存狀態
const loading = ref(true)
const saving = ref(false)
const errorMessage = ref('')

// 取得目前登入會員的資料
async function loadProfile() {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await http.get('/users/me')
    const data = response.data

    form.value = {
      name: data.name ?? '',
      email: data.email ?? '',
      birthDate: data.birthDate ?? '',
    }
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message ||
      '無法載入會員資料，請稍後再試。'
  } finally {
    loading.value = false
  }
}

// 儲存修改後的會員資料
async function saveProfile() {
  if (saving.value) return

  if (!form.value.name.trim()) {
    await Swal.fire({
      icon: 'warning',
      title: '請輸入姓名或暱稱',
    })
    return
  }

  if (!form.value.email.trim()) {
    await Swal.fire({
      icon: 'warning',
      title: '請輸入電子郵件',
    })
    return
  }

  saving.value = true

  try {
    await http.put('/users/me', {
      name: form.value.name.trim(),
      email: form.value.email.trim(),
      birthDate: form.value.birthDate || null,
    })

    await Swal.fire({
      icon: 'success',
      title: '個人資料已更新',
      text: '你的會員資料已成功儲存。',
      confirmButtonText: '確定',
    })

    // 重新讀取後端資料，確認儲存結果
    await loadProfile()
  } catch (error) {
    await Swal.fire({
      icon: 'error',
      title: '更新失敗',
      text:
        error.response?.data?.message ||
        '儲存個人資料時發生錯誤，請稍後再試。',
      confirmButtonText: '確定',
    })
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="container py-4">
    <div class="row justify-content-center">
      <div class="col-12 col-lg-8">
        <div class="card border-0 shadow-sm">
          <div class="card-body p-4 p-md-5">
            <h3 class="mb-2">修改個人資料</h3>
            <p class="text-muted mb-4">
              管理你的會員基本資料。
            </p>

            <!-- 載入中 -->
            <div v-if="loading" class="text-center py-5">
              <div
                class="spinner-border"
                role="status"
                aria-label="載入中"
              ></div>
              <p class="text-muted mt-3 mb-0">
                正在載入會員資料...
              </p>
            </div>

            <!-- 載入失敗 -->
            <div v-else-if="errorMessage" class="alert alert-danger">
              {{ errorMessage }}
              <button
                type="button"
                class="btn btn-outline-danger btn-sm ms-2"
                @click="loadProfile"
              >
                重新載入
              </button>
            </div>

            <!-- 會員資料表單 -->
            <form v-else @submit.prevent="saveProfile">

              <div class="mb-3">
                <label for="name" class="form-label">
                  姓名／暱稱 <span class="text-danger">*</span>
                </label>
                <input
                  id="name"
                  v-model="form.name"
                  type="text"
                  class="form-control"
                  maxlength="100"
                  required
                  placeholder="請輸入姓名或暱稱"
                />
              </div>

              <div class="mb-3">
                <label for="email" class="form-label">
                  電子郵件 <span class="text-danger">*</span>
                </label>
                <input
                  id="email"
                  v-model="form.email"
                  type="email"
                  class="form-control"
                  maxlength="255"
                  required
                  placeholder="請輸入電子郵件"
                />
              </div>

              <div class="mb-4">
                <label for="birthDate" class="form-label">
                  生日
                </label>
                <input
                  id="birthDate"
                  v-model="form.birthDate"
                  type="date"
                  class="form-control"
                />
                <div class="form-text">
                  生日可以不填。
                </div>
              </div>

              <div
                class="d-flex flex-column flex-sm-row gap-2 justify-content-end"
              >
                <button
                  type="button"
                  class="btn btn-outline-secondary"
                  :disabled="saving"
                  @click="loadProfile"
                >
                  重新載入
                </button>

                <button
                  type="submit"
                  class="btn btn-primary"
                  :disabled="saving"
                >
                  <span
                    v-if="saving"
                    class="spinner-border spinner-border-sm me-2"
                    aria-hidden="true"
                  ></span>
                  {{ saving ? '儲存中...' : '儲存修改' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card {
  border: 1px solid #e0ece3;
  border-radius: 1rem;
  box-shadow: 0 4px 16px rgb(87 130 99 / 8%);
}

.form-label {
  font-weight: 600;
  color: #405b49;
}

.form-control {
  border-color: #d5e4d9;
  border-radius: 0.65rem;
}

.form-control:focus {
  border-color: #a8d5ba;
  box-shadow: 0 0 0 0.2rem rgb(168 213 186 / 25%);
}

.form-control:disabled {
  background-color: #f5f5f5;
}

/* 主要按鈕：淺薄荷綠 */
.btn-primary {
  border-color: #a8d5ba;
  border-radius: 0.75rem;
  background-color: #a8d5ba;
  color: #294b39;
  font-weight: 700;
}

.btn-primary:hover:not(:disabled) {
  border-color: #8bc5a2;
  background-color: #8bc5a2;
  color: #203d2e;
}

/* 次要按鈕：白底淡綠框 */
.btn-outline-secondary {
  border-color: #c8ddcf;
  border-radius: 0.75rem;
  background-color: #ffffff;
  color: #587363;
  font-weight: 600;
}

.btn-outline-secondary:hover:not(:disabled) {
  border-color: #d7e9dc;
  background-color: #edf6f0;
  color: #365844;
}

/* 停用按鈕 */
.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 載入圖示 */
.spinner-border {
  color: currentColor;
}
</style>