
<script setup>
    import { ref, computed, onMounted, watch } from 'vue'
    import { useRoute, useRouter } from 'vue-router'
    import http from '@/plugins/axios'

    const route = useRoute()

    const router = useRouter()

    // 會員基本資料
    const member = ref(null)

    // 會員公開刊登
    const exchangeListings = ref([])
    const ticketListings = ref([])

    // 載入與錯誤狀態
    const loading = ref(true)
    const errorMessage = ref('')

    // 取得網址上的會員 ID，例如 /members/3
    const memberId = computed(() => route.params.id)

    // 格式化加入日期
    const formatDate = (value) => {
    if (!value) return '尚無資料'

    const date = new Date(value)

    if (Number.isNaN(date.getTime())) return '尚無資料'

    return date.toLocaleDateString('zh-TW', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
    })
    }

    // 傳送私訊：取得或建立與該會員的聊天室
    async function sendPrivateMessage() {
    // 未登入時，先前往登入頁，登入後回到這位會員的公開頁
    const token = localStorage.getItem('token')

    if (!token) {
        router.push({
        path: '/login',
        query: {
            redirect: route.fullPath,
        },
        })
        return
    }

    // 不允許傳訊息給自己
    const currentUser = JSON.parse(
        localStorage.getItem('user') || 'null'
    )

    const currentUserId =
        currentUser?.usersId ??
        currentUser?.users_id ??
        currentUser?.userId ??
        currentUser?.id

    if (String(currentUserId) === String(memberId.value)) {
        alert('這是你自己的會員頁面，無法傳送私訊。')
        return
    }

    try {
        // 後端會先尋找既有聊天室，沒有才建立
        const response = await http.post(
        `/conversations/${memberId.value}`
        )

        // 前往既有的完整聊天室頁面
        router.push({
        name: 'message-conversation',
        params: {
            conversationId: response.data.conversationId,
        },
        })
    } catch (error) {
        alert(
        error.response?.data?.message ||
        '目前無法開啟聊天室，請稍後再試。'
        )
    }
    }

    // 載入會員資料及公開刊登
    async function loadMemberPage() {
    loading.value = true
    errorMessage.value = ''

    member.value = null
    exchangeListings.value = []
    ticketListings.value = []

    try {
        // 1. 會員基本資料
        const memberResponse = await http.get(`/users/${memberId.value}`)
        member.value = memberResponse.data

        // 2. 交換刊登
        const exchangeResponse = await http.get(
        `/exchange-listings/member/${memberId.value}`
        )

        exchangeListings.value = Array.isArray(exchangeResponse.data)
        ? exchangeResponse.data
        : exchangeResponse.data?.content || []

        // 3. 票券刊登
        const ticketResponse = await http.get(
        `/ticket-listings/member/${memberId.value}`
        )

        ticketListings.value = Array.isArray(ticketResponse.data)
        ? ticketResponse.data
        : ticketResponse.data?.content || []
    } catch (error) {
        errorMessage.value =
        error.response?.data?.message ||
        '無法載入會員資料，請稍後再試。'
    } finally {
        loading.value = false
    }
    }

    // 網址中的會員 ID 改變時，重新載入
    watch(memberId, () => {
    if (memberId.value) {
        loadMemberPage()
    }
    })

    onMounted(() => {
    if (memberId.value) {
        loadMemberPage()
    } else {
        loading.value = false
        errorMessage.value = '找不到會員編號。'
    }
    })
</script>

<template>
    <div class="member-public-page container py-4">
        <!-- 載入中 -->
        <div v-if="loading" class="text-center py-5">
        <div class="spinner-border" role="status" aria-label="載入中"></div>
        <p class="mt-3">正在載入會員資料...</p>
        </div>

        <!-- 載入失敗 -->
        <div v-else-if="errorMessage" class="alert alert-warning">
        {{ errorMessage }}
        <button
            type="button"
            class="btn btn-outline-secondary btn-sm ms-2"
            @click="loadMemberPage"
        >
            重新載入
        </button>
        </div>

        <!-- 會員公開頁 -->
        <template v-else-if="member">
        <!-- 會員基本資料 -->
        <section class="member-card card border-0 shadow-sm mb-4">
            <div class="card-body p-4">
            <div class="d-flex align-items-center gap-3">
                <div class="member-avatar">
                <i class="bi bi-person-fill"></i>
                </div>

                <div>
                <h2 class="mb-1">
                    {{ member.name || member.account || '會員' }}
                </h2>

                <p class="text-secondary mb-1">
                    帳號：{{ member.account || '尚無資料' }}
                </p>

                <p class="text-secondary small mb-0">
                    加入日期：{{ formatDate(member.createdAt) }}
                </p>
                <button type="button"  class="btn btn-success mt-3" @click="sendPrivateMessage" >
                    <i class="bi bi-chat-dots me-2"></i> 傳送私訊 </button>
                </div>
            </div>
            </div>
    </section>

     <!-- 交換刊登 -->
        <section class="listing-section mb-4">
            <div class="section-heading">
            <h3>交換刊登</h3>
            <span>{{ exchangeListings.length }} 筆</span>
            </div>

            <div v-if="exchangeListings.length === 0" class="empty-state">
            這位會員目前沒有公開的交換刊登。
            </div>

            <div v-else class="row g-3">
            <div
                v-for="(item, index) in exchangeListings"
                :key="item.exchangeListingId ?? item.id ?? index"
                class="col-12 col-md-6 col-lg-4"
            >
                <article class="listing-card card h-100">
                <div class="card-body">
                    <h5 class="card-title">
                    {{ item.title || item.listingTitle || '交換刊登' }}
                    </h5>

                    <p class="card-text">
                    {{ item.description || '尚無刊登說明。' }}
                    </p>

                    <p
                    v-if="item.status"
                    class="small text-secondary mb-0"
                    >
                    狀態：{{ item.status }}
                    </p>
                </div>
                </article>
            </div>
            </div>
    </section>

    <!-- 票券刊登 -->
        <section class="listing-section">
            <div class="section-heading">
            <h3>票券刊登</h3>
            <span>{{ ticketListings.length }} 筆</span>
            </div>

            <div v-if="ticketListings.length === 0" class="empty-state">
            這位會員目前沒有公開的票券刊登。
            </div>

            <div v-else class="row g-3">
            <div
                v-for="(item, index) in ticketListings"
                :key="item.ticketListingId ?? item.id ?? index"
                class="col-12 col-md-6 col-lg-4"
            >
                <article class="listing-card card h-100">
                <div class="card-body">
                    <h5 class="card-title">
                    {{ item.title || item.listingTitle || '票券刊登' }}
                    </h5>

                    <p class="card-text">
                    {{ item.description || '尚無刊登說明。' }}
                    </p>

                    <p
                    v-if="item.status"
                    class="small text-secondary mb-0"
                    >
                    狀態：{{ item.status }}
                    </p>
                </div>
                </article>
            </div>
            </div>
        </section>
        </template>
    </div>
</template>

<style scoped>
.member-public-page {
    max-width: 1100px;
    color: #405b49;
    }

.member-card,
.listing-card {
    border-radius: 1rem;
    }

.member-avatar {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 76px;
    height: 76px;
    border-radius: 50%;
    background: #edf6f0;
    color: #365844;
    font-size: 2.5rem;
    }

.section-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    margin-bottom: 1rem;
    }

.section-heading h3 {
    margin: 0;
    font-size: 1.3rem;
    font-weight: 800;
    }

.section-heading span {
    color: #84968b;
    font-size: 0.9rem;
    }

.listing-card {
    border: 1px solid #e0ece3;
    transition: box-shadow 0.2s ease;
    }

.listing-card:hover {
    box-shadow: 0 4px 14px rgb(87 130 99 / 10%);
    }

.listing-card .card-title {
    color: #365844;
    font-weight: 700;
    }

.listing-card .card-text {
    color: #66786c;
    white-space: pre-line;
    }

.empty-state {
    padding: 2rem 1rem;
    border: 1px dashed #d5e4d9;
    border-radius: 1rem;
    background: #fbfdfb;
    color: #84968b;
    text-align: center;
    }

@media (max-width: 575.98px) {
.member-avatar {
        width: 60px;
        height: 60px;
        font-size: 2rem;
    }

.section-heading h3 {
        font-size: 1.15rem;
    }
    }
</style>
