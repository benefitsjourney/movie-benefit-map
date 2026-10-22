<script setup>
    import { ref, onMounted } from 'vue'
    import { RouterLink } from 'vue-router'
    import { Icon } from '@iconify/vue'
    import Swal from 'sweetalert2'
    import http from '@/plugins/axios'

    const user = ref(null)
    const isLoading = ref(true)

    const accountLinks = [
    {
        title: '修改個人資料',
        description: '更新暱稱、電子郵件等基本資料',
        icon: 'mdi:account-edit-outline',
        to: '/account/profile',
    },
    {
        title: '我的刊登',
        description: '查看自己的刊登內容',
        icon: 'mdi:clipboard-list-outline',
        to: '/account/listings',
    },
    {
        title: '我的收藏',
        description: '查看已收藏的刊登與內容',
        icon: 'mdi:heart-outline',
        to: '/account/favorites',
    },
    {
        title: '我的特典',
        description: '管理自己的特典紀錄',
        icon: 'mdi:gift-outline',
        to: '/account/benefits',
    },
    {
        title: '我的訊息',
        description: '前往會員私訊功能',
        icon: 'mdi:message-text-outline',
        to: '/account/messages',
    },
    ]

    onMounted(async () => {
    try {
        const response = await http.get('/users/me')
        user.value = response.data
    } catch (error) {
        await Swal.fire({
        icon: 'error',
        title: '無法取得會員資料',
        text: error.response?.data?.message || '請重新登入後再試一次。',
        confirmButtonText: '確定',
        })
    } finally {
        isLoading.value = false
    }
    })
</script>

<template>
    <div class="container py-4 py-md-5">
        <div class="mb-4">
        <p class="text-uppercase small fw-bold mb-2 account-eyebrow">
            MY ACCOUNT
        </p>

        <h1 class="fw-bold mb-2">關於我</h1>

        <p class="text-secondary mb-0">
            管理你的會員資料，整理屬於你的特典收藏旅程。
        </p>
        </div>

        <div v-if="isLoading" class="text-center py-5">
        <div class="spinner-border" role="status">
            <span class="visually-hidden">載入中</span>
        </div>
        <p class="text-secondary mt-3">正在載入會員資料...</p>
        </div>

        <template v-else-if="user">
        <section class="card account-profile-card mb-4">
            <div class="card-body p-4 p-md-5">
            <div class="d-flex align-items-center gap-3 mb-4">
                <div class="account-avatar">
                <Icon icon="mdi:account-circle-outline" />
                </div>

                <div>
                <h2 class="h4 fw-bold mb-1">
                    {{ user.name || user.account || '會員' }}
                </h2>
                <span class="badge account-badge">
                    {{ user.role === 'ADMIN' ? '管理員' : '一般會員' }}
                </span>
                </div>
            </div>

            <div class="row g-3">
                <div class="col-12 col-md-6">
                <div class="account-info">
                    <span class="account-label">會員編號</span>
                    <p class="mb-0">{{ user.usersId }}</p>
                </div>
                </div>

                <div class="col-12 col-md-6">
                <div class="account-info">
                    <span class="account-label">登入帳號</span>
                    <p class="mb-0">{{ user.account || '尚未設定' }}</p>
                </div>
                </div>

                <div class="col-12 col-md-6">
                <div class="account-info">
                    <span class="account-label">電子郵件</span>
                    <p class="mb-0 text-break">{{ user.email || '尚未設定' }}</p>
                </div>
                </div>

                <div class="col-12 col-md-6">
                <div class="account-info">
                    <span class="account-label">帳號狀態</span>
                    <p class="mb-0">
                    {{ user.status === 'ACTIVE' ? '使用中' : user.status }}
                    </p>
                </div>
                </div>
            </div>

            <div class="mt-4">
                <RouterLink
                to="/account/profile"
                class="btn account-primary-button"
                >
                <Icon icon="mdi:account-edit-outline" class="me-1" />
                修改個人資料
                </RouterLink>
            </div>
            </div>
        </section>

        <section>
            <h2 class="h5 fw-bold mb-3">會員功能</h2>

            <div class="row g-3">
            <div
                v-for="item in accountLinks"
                :key="item.to"
                class="col-12 col-sm-6 col-lg-4"
            >
                <RouterLink :to="item.to" class="account-link-card">
                <div class="account-link-icon">
                    <Icon :icon="item.icon" />
                </div>

                <div class="flex-grow-1">
                    <h3 class="h6 fw-bold mb-2">{{ item.title }}</h3>
                    <p class="small text-secondary mb-0">
                    {{ item.description }}
                    </p>
                </div>

                <Icon
                    icon="mdi:chevron-right"
                    class="account-chevron"
                />
                </RouterLink>
            </div>
            </div>
        </section>
        </template>
    </div>
</template>

<style scoped>
    .account-eyebrow {
    color: #805b16;
    letter-spacing: 0.12em;
    }

.account-profile-card {
    background: var(--c-card);
    border: 1px solid var(--c-border);
    border-radius: 1.25rem;
    box-shadow: var(--shadow-soft);
    }

.account-avatar {
    width: 64px;
    height: 64px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fff0c9;
    color: #805b16;
    border-radius: 1rem;
    font-size: 2.5rem;
    }

.account-badge {
    color: #805b16;
    background: #fff0c9;
    }

.account-info {
    height: 100%;
    padding: 1rem;
    background: #fffaf1;
    border: 1px solid var(--c-border);
    border-radius: 0.75rem;
    overflow-wrap: anywhere;
    }

.account-label {
    display: block;
    margin-bottom: 0.4rem;
    color: var(--c-muted);
    font-size: 0.875rem;
    }

.account-primary-button {
    padding: 0.7rem 1.1rem;
    color: #292b35;
    background: #ffcf70;
    border: none;
    border-radius: 0.75rem;
    font-weight: 700;
    }

.account-primary-button:hover {
    background: #f6bd50;
    color: #292b35;
    }

.account-link-card {
    height: 100%;
    min-height: 125px;
    display: flex;
    align-items: center;
    gap: 0.85rem;
    padding: 1.1rem;
    color: var(--c-ink);
    text-decoration: none;
    background: var(--c-card);
    border: 1px solid var(--c-border);
    border-radius: 1rem;
    transition: transform 0.2s, box-shadow 0.2s;
    }

.account-link-card:hover {
    transform: translateY(-3px);
    box-shadow: var(--shadow-soft);
    }

.account-link-icon {
    width: 44px;
    height: 44px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #805b16;
    background: #fff0c9;
    border-radius: 0.75rem;
    font-size: 1.5rem;
    }

.account-chevron {
    flex-shrink: 0;
    color: var(--c-muted);
    font-size: 1.25rem;
    }
</style>
