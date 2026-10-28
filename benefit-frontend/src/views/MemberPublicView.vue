<!-- src/views/MemberPublicView.vue -->
<script setup>
import {
    ref,
    computed,
    onMounted,
    onBeforeUnmount,
    watch,
} from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import giftIcon from '@iconify-icons/mdi/gift-outline'
import ticketIcon from '@iconify-icons/mdi/ticket'
import accountIcon from '@iconify-icons/mdi/account'
import calendarIcon from '@iconify-icons/mdi/calendar'
import cinemaIcon from '@iconify-icons/mdi/filmstrip'
import filmIcon from '@iconify-icons/mdi/movie-open'
import heartIcon from '@iconify-icons/mdi/heart'
import heartOutlineIcon from '@iconify-icons/mdi/heart-outline'
import chatIcon from '@iconify-icons/mdi/chat-outline'
import closeIcon from '@iconify-icons/mdi/close'
import TicketCard from '@/components/ui/TicketCard.vue'
import http from '@/plugins/axios'
import {
    getMyExchangeFavorites,
    addExchangeFavorite,
    removeExchangeFavorite,
    getMyTicketFavorites,
    addTicketFavorite,
    removeTicketFavorite,
} from '@/api/exchangeApi.js'

const route = useRoute()
const router = useRouter()

const member = ref(null)
const exchangeListings = ref([])
const ticketListings = ref([])
const loading = ref(true)
const errorMessage = ref('')
// 目前登入會員的收藏 ID
const exchangeFavoriteIds = ref([])
const ticketFavoriteIds = ref([])
// 分別追蹤兩種收藏的載入狀態
const exchangeFavoritesLoaded = ref(false)
const ticketFavoritesLoaded = ref(false)
const exchangeFavoritesLoadFailed = ref(false)
const ticketFavoritesLoadFailed = ref(false)
// 正在執行收藏操作的刊登，避免重複點擊
const favoriteActionKeys = ref([])
// 詳細內容視窗
const selectedExchangeListing = ref(null)
const selectedTicketListing = ref(null)
// 特典卡片目前使用的圖片索引
const exchangeImageIndexes = ref({})
// 特典詳細視窗的圖片索引及載入失敗紀錄
const detailImageIndex = ref(0)
const failedDetailImages = ref([])

const memberId = computed(() => route.params.id)

const selectedExchangeImages = computed(() => {
    if (!selectedExchangeListing.value) return []

    return getExchangeImageCandidates(
        selectedExchangeListing.value,
    ).filter((url) => !failedDetailImages.value.includes(url))
})

const currentDetailImage = computed(() => {
    return selectedExchangeImages.value[
        detailImageIndex.value
    ] || ''
})

watch(selectedExchangeImages, (images) => {
    if (detailImageIndex.value >= images.length) {
        detailImageIndex.value = Math.max(0, images.length - 1)
    }
})

const formatDate = (value) => {
    if (!value) return '尚無資料'

    const text = String(value).trim()
    const datePart = text.split(/[T ]/)[0]

    if (!datePart) return '尚無資料'

    const date = new Date(datePart)

    if (Number.isNaN(date.getTime())) return '尚無資料'

    return date.toLocaleDateString('zh-TW', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
    })
}

// 取得目前登入會員的 Token
function getToken() {
    return localStorage.getItem('token') || ''
}

function getCurrentUserId() {
    const currentUser = JSON.parse(
        localStorage.getItem('user') || 'null',
    )

    return (
        currentUser?.usersId
        ?? currentUser?.users_id
        ?? currentUser?.userId
        ?? currentUser?.id
        ?? null
    )
}

// 判斷這筆刊登是否屬於目前登入的會員。
function isOwnListing(item) {
    const currentUserId = getCurrentUserId()

    // 支援不同 API 回傳的會員 ID 欄位。
    const listingUserId =
        item?.usersId
        ?? item?.users_id
        ?? item?.user?.usersId
        ?? item?.users?.usersId
        ?? item?.usersBean?.usersId
        ?? memberId.value

    if (
        currentUserId === null ||
        currentUserId === undefined ||
        currentUserId === '' ||
        listingUserId === null ||
        listingUserId === undefined ||
        listingUserId === ''
    ) {
        return false
    }

    return String(currentUserId) === String(listingUserId)
}

// 將 API 回傳資料轉成陣列
function getResponseList(response) {
    if (Array.isArray(response.data)) {
        return response.data
    }
    if (Array.isArray(response.data?.content)) {
        return response.data.content
    }
    return []
}

// 支援不同欄位名稱
function getExchangeListingId(item) {
    return item?.exchangeListingId
        ?? item?.exchange_listing_id
        ?? item?.id
        ?? null
}

function getTicketListingId(item) {
    return item?.ticketListingId
        ?? item?.ticket_listing_id
        ?? item?.id
        ?? null
}

// 比對 ID 時統一轉成字串
function hasFavorite(favoriteIds, listingId) {
    if (listingId === null || listingId === undefined) {
        return false
    }
    return favoriteIds.some(
        (favoriteId) => String(favoriteId) === String(listingId),
    )
}

function isExchangeFavorited(item) {
    return hasFavorite(
        exchangeFavoriteIds.value,
        getExchangeListingId(item),
    )
}

function isTicketFavorited(item) {
    return hasFavorite(
        ticketFavoriteIds.value,
        getTicketListingId(item),
    )
}

function isFavoriteActionLoading(type, listingId) {
    if (listingId === null || listingId === undefined) {
        return false
    }

    const key = `${type}-${listingId}`
    return favoriteActionKeys.value.includes(key)
}

function startFavoriteAction(type, listingId) {
    const key = `${type}-${listingId}`

    if (!favoriteActionKeys.value.includes(key)) {
        favoriteActionKeys.value = [
            ...favoriteActionKeys.value,
            key,
        ]
    }
}

function finishFavoriteAction(type, listingId) {
    const key = `${type}-${listingId}`

    favoriteActionKeys.value =
        favoriteActionKeys.value.filter((item) => item !== key)
}

// 載入目前登入會員的收藏 ID，收藏 API 失敗時，不影響公開會員資料與刊登的瀏覽
async function loadFavoriteIds() {
    const token = getToken()

    if (!token) {
        exchangeFavoritesLoaded.value = true
        ticketFavoritesLoaded.value = true
        return
    }

    const [exchangeResult, ticketResult] = await Promise.allSettled([
        getMyExchangeFavorites(token),
        getMyTicketFavorites(token),
    ])

    if (exchangeResult.status === 'fulfilled') {
        const favorites = getResponseList(exchangeResult.value)

        exchangeFavoriteIds.value = favorites
            .map((item) => item?.exchangeListingId)
            .filter((id) => id !== null && id !== undefined)

        exchangeFavoritesLoaded.value = true
        exchangeFavoritesLoadFailed.value = false
    } else {
        exchangeFavoritesLoaded.value = false
        exchangeFavoritesLoadFailed.value = true
    }

    if (ticketResult.status === 'fulfilled') {
        const favorites = getResponseList(ticketResult.value)

        ticketFavoriteIds.value = favorites
            .map((item) => item?.ticketListingId)
            .filter((id) => id !== null && id !== undefined)

        ticketFavoritesLoaded.value = true
        ticketFavoritesLoadFailed.value = false
    } else {
        ticketFavoritesLoaded.value = false
        ticketFavoritesLoadFailed.value = true
    }
}

// 登入檢查：保留目前頁面，登入後可以返回
function requireLogin() {
    if (getToken()) {
        return true
    }

    router.push({
        path: '/login',
        query: {
            redirect: route.fullPath,
        },
    })

    return false
}

// 切換特典交換刊登的收藏狀態
async function toggleExchangeFavorite(item) {
    if (!requireLogin()) return

    if (!exchangeFavoritesLoaded.value) {
        alert(
            exchangeFavoritesLoadFailed.value
                ? '目前無法確認收藏狀態，請重新載入頁面後再試。'
                : '收藏狀態載入中，請稍後再試。',
        )
        return
    }

    const listingId = getExchangeListingId(item)

    if (listingId === null || listingId === undefined) {
        alert('找不到交換刊登編號，無法執行收藏。')
        return
    }

    if (isFavoriteActionLoading('exchange', listingId)) {
        return
    }

    startFavoriteAction('exchange', listingId)

    try {
        if (isExchangeFavorited(item)) {
            await removeExchangeFavorite(listingId, getToken())

            exchangeFavoriteIds.value =
                exchangeFavoriteIds.value.filter(
                    (id) => String(id) !== String(listingId),
                )
        } else {
            await addExchangeFavorite(listingId, getToken())

            if (!isExchangeFavorited(item)) {
                exchangeFavoriteIds.value = [
                    ...exchangeFavoriteIds.value,
                    listingId,
                ]
            }
        }
    } catch (error) {
        alert(
            error.response?.data?.message
            || error.response?.data?.detail
            || error.message
            || '交換刊登收藏操作失敗，請稍後再試。',
        )
    } finally {
        finishFavoriteAction('exchange', listingId)
    }
}

// 切換票券刊登的收藏狀態
async function toggleTicketFavorite(item) {
    if (!requireLogin()) return

    if (!ticketFavoritesLoaded.value) {
        alert(
            ticketFavoritesLoadFailed.value
                ? '目前無法確認收藏狀態，請重新載入頁面後再試。'
                : '收藏狀態載入中，請稍後再試。',
        )
        return
    }

    const listingId = getTicketListingId(item)

    if (listingId === null || listingId === undefined) {
        alert('找不到票券刊登編號，無法執行收藏。')
        return
    }

    if (isFavoriteActionLoading('ticket', listingId)) {
        return
    }

    startFavoriteAction('ticket', listingId)

    try {
        if (isTicketFavorited(item)) {
            await removeTicketFavorite(listingId, getToken())

            ticketFavoriteIds.value =
                ticketFavoriteIds.value.filter(
                    (id) => String(id) !== String(listingId),
                )
        } else {
            await addTicketFavorite(listingId, getToken())

            if (!isTicketFavorited(item)) {
                ticketFavoriteIds.value = [
                    ...ticketFavoriteIds.value,
                    listingId,
                ]
            }
        }
    } catch (error) {
        alert(
            error.response?.data?.message
            || error.response?.data?.detail
            || error.message
            || '票券刊登收藏操作失敗，請稍後再試。',
        )
    } finally {
        finishFavoriteAction('ticket', listingId)
    }
}

// 通用欄位取得：支援駝峰式及 snake_case 欄位
function firstValue(object, keys, fallback = '') {
    for (const key of keys) {
        const value = object?.[key]

        if (
            value !== undefined
            && value !== null
            && value !== ''
        ) {
            return value
        }
    }

    return fallback
}

function listingUserName(item) {
    const name = firstValue(item, [
        'usersName',
        'userName',
        'username',
        'memberName',
        'users_name',
        'user_name',
    ])

    return name !== null && name !== undefined && String(name).trim()
        ? String(name).trim()
        : '未提供'
}

// 取得票券的影城名稱
function getCinemaName(item) {
    return item?.cinemaName
        ?? item?.cinema_name
        ?? item?.cinemaBean?.cinemaName
        ?? item?.cinema?.cinemaName
        ?? '尚無資料'
}

// 取得票券的電影名稱
function getMovieName(item) {
    return item?.movieName
        ?? item?.movie_name
        ?? item?.movieBean?.movieName
        ?? item?.movie?.movieName
        ?? '尚無資料'
}

// 特典圖片候選清單：會員持有照片優先，官方圖片備援
function getExchangeImageCandidates(item) {
    const memberPhotos = Array.isArray(item?.useBenefitPhotoUrls)
        ? item.useBenefitPhotoUrls
        : Array.isArray(item?.use_benefit_photo_urls)
            ? item.use_benefit_photo_urls
            : []

    const memberPhotoUrls = memberPhotos
        .filter((url) => typeof url === 'string')
        .map((url) => url.trim())
        .filter(Boolean)

    const officialImageUrl = (
        typeof item?.officialImageUrl === 'string'
            ? item.officialImageUrl
            : typeof item?.official_image_url === 'string'
                ? item.official_image_url
                : ''
    ).trim()

    return [
        ...new Set([
            ...memberPhotoUrls,
            ...(officialImageUrl ? [officialImageUrl] : []),
        ]),
    ]
}

// 取得每張特典卡片目前應顯示的圖片
function currentExchangeImage(item, index) {
    const listingId = getExchangeListingId(item)
    const key = listingId !== null && listingId !== undefined
        ? String(listingId)
        : `exchange-index-${index}`

    const candidates = getExchangeImageCandidates(item)
    const imageIndex = exchangeImageIndexes.value[key] ?? 0

    return candidates[imageIndex] ?? ''
}

// 卡片圖片載入失敗時，嘗試下一張候選圖片
function handleExchangeImageError(item, index) {
    const listingId = getExchangeListingId(item)
    const key = listingId !== null && listingId !== undefined
        ? String(listingId)
        : `exchange-index-${index}`

    const candidates = getExchangeImageCandidates(item)
    const currentIndex = exchangeImageIndexes.value[key] ?? 0

    if (currentIndex < candidates.length) {
        exchangeImageIndexes.value = {
            ...exchangeImageIndexes.value,
            [key]: currentIndex + 1,
        }
    }
}

// 開啟特典詳細內容視窗
function openExchangeDetail(item) {
    selectedExchangeListing.value = item
    detailImageIndex.value = 0
    failedDetailImages.value = []
}

// 關閉特典詳細內容視窗
function closeExchangeDetail() {
    selectedExchangeListing.value = null
    detailImageIndex.value = 0
    failedDetailImages.value = []
}

// 詳細視窗圖片載入失敗時，排除該圖片
function handleDetailImageError(url) {
    if (!failedDetailImages.value.includes(url)) {
        failedDetailImages.value = [
            ...failedDetailImages.value,
            url,
        ]
    }
}

// 切換詳細視窗中的上一張圖片
function previousDetailImage() {
    const total = selectedExchangeImages.value.length

    if (total <= 1) return

    detailImageIndex.value =
        (detailImageIndex.value - 1 + total) % total
}

// 切換詳細視窗中的下一張圖片
function nextDetailImage() {
    const total = selectedExchangeImages.value.length

    if (total <= 1) return

    detailImageIndex.value =
        (detailImageIndex.value + 1) % total
}

// 開啟票券詳細內容視窗
function openTicketDetail(item) {
    selectedTicketListing.value = item
}

// 關閉票券詳細內容視窗
function closeTicketDetail() {
    selectedTicketListing.value = null
}


/**
 * 私訊目前公開會員。
 * 傳入 item 時，額外確認該筆刊登是否屬於自己。
 * 成功取得聊天室後，開啟右下角 FloatingChat 浮動聊天室。
 */
async function sendPrivateMessage(item = null) {
    const token = getToken()

    if (!token) {
        router.push({
            path: '/login',
            query: {
                redirect: route.fullPath,
            },
        })
        return
    }

    // 卡片或詳細視窗的私訊按鈕。
    if (item && isOwnListing(item)) {
        alert('這是你自己的刊登，無法聯絡自己。')
        return
    }

    // 會員基本資料區的私訊按鈕。
    const currentUserId = getCurrentUserId()

    if (
        currentUserId !== null &&
        currentUserId !== undefined &&
        currentUserId !== '' &&
        String(currentUserId) === String(memberId.value)
    ) {
        alert('這是你自己的會員頁面，無法傳送私訊。')
        return
    }

    try {
        // 建立或取得與目標會員的聊天室。
        const response = await http.post(
            `/conversations/${memberId.value}`,
        )

        const conversation = response.data

        // 確認 API 有回傳聊天室 ID，避免開啟無效聊天室。
        if (
            conversation?.conversationId === null ||
            conversation?.conversationId === undefined
        ) {
            alert('目前無法取得聊天室資料，請稍後再試。')
            return
        }

        // 通知 FloatingChat 開啟浮動視窗並進入指定聊天室。
        window.dispatchEvent(
            new CustomEvent('open-floating-chat', {
                detail: {
                    conversation,
                },
            }),
        )
    } catch (error) {
        alert(
            error.response?.data?.message
            || '目前無法開啟聊天室，請稍後再試。',
        )
    }
}

// 載入公開會員資料與該會員的刊登
async function loadMemberPage() {
    loading.value = true
    errorMessage.value = ''
    member.value = null
    exchangeListings.value = []
    ticketListings.value = []
    exchangeImageIndexes.value = {}

    try {
        const memberResponse = await http.get(
            `/users/${memberId.value}`,
        )

        member.value = memberResponse.data

        const exchangeResponse = await http.get(
            `/exchange-listings/member/${memberId.value}`,
        )

        exchangeListings.value = Array.isArray(exchangeResponse.data)
            ? exchangeResponse.data
            : exchangeResponse.data?.content || []

        const ticketResponse = await http.get(
            `/ticket-listings/member/${memberId.value}`,
        )

        ticketListings.value = Array.isArray(ticketResponse.data)
            ? ticketResponse.data
            : ticketResponse.data?.content || []
    } catch (error) {
        errorMessage.value =
            error.response?.data?.message
            || '無法載入會員資料，請稍後再試。'
    } finally {
        loading.value = false
    }
}

function handleKeydown(event) {
    if (event.key !== 'Escape') return

    if (selectedExchangeListing.value) {
        closeExchangeDetail()
    }

    if (selectedTicketListing.value) {
        closeTicketDetail()
    }
}

watch(memberId, () => {
    if (memberId.value) {
        loadMemberPage()
    } else {
        loading.value = false
        errorMessage.value = '找不到會員編號。'
    }
})

onMounted(() => {
    if (memberId.value) {
        loadMemberPage()
    } else {
        loading.value = false
        errorMessage.value = '找不到會員編號。'
    }

    loadFavoriteIds()
    window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
    <div class="member-public-page container py-4">
        <div v-if="loading" class="text-center py-5">
            <div class="spinner-border" role="status" aria-label="載入中"></div>
            <p class="mt-3">正在載入會員資料...</p>
        </div>

        <div v-else-if="errorMessage" class="alert alert-warning">
            {{ errorMessage }}

            <button type="button" class="btn btn-outline-secondary btn-sm ms-2" @click="loadMemberPage">
                重新載入
            </button>
        </div>

        <template v-else-if="member">
            <!-- 會員基本資料：保留原本版型 -->
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

                            <button type="button" class="btn btn-success mt-3" @click="sendPrivateMessage">
                                <i class="bi bi-chat-dots me-2"></i>
                                傳送私訊
                            </button>
                        </div>
                    </div>
                </div>
            </section>

            <!-- 特典交換刊登 -->
            <section class="listing-section mb-4">
                <div class="section-heading">
                    <h3>特典刊登</h3>
                    <span>{{ exchangeListings.length }} 筆</span>
                </div>

                <div v-if="exchangeListings.length === 0" class="empty-state">
                    這位會員目前沒有公開的交換刊登。
                </div>

                <div v-else class="ticket-grid exchange-ticket-grid">
                    <TicketCard v-for="(item, index) in exchangeListings" :key="getExchangeListingId(item) ?? index"
                        class="exchange-public-card" color="green" :appear="index < 6">

                        <!-- 特典圖片：沿用交換刊登卡片的 poster 插槽 -->
                        <template #poster>
                            <div class="poster-wrap poster-clickable" role="button" tabindex="0"
                                :aria-label="`查看${item.title || '未命名刊登'}的詳細內容`" @click="openExchangeDetail(item)"
                                @keydown.enter.prevent="openExchangeDetail(item)"
                                @keydown.space.prevent="openExchangeDetail(item)">
                                <img v-if="currentExchangeImage(item, index)" :key="currentExchangeImage(item, index)"
                                    class="poster-image" :src="currentExchangeImage(item, index)" :alt="item.benefitName
                                        ? `${item.benefitName}圖片`
                                        : '交換特典圖片'" loading="lazy" @error="handleExchangeImageError(item, index)" />

                                <span v-else class="poster-fallback" aria-label="目前沒有可顯示的圖片">
                                    <Icon :icon="giftIcon" aria-hidden="true" />
                                </span>

                                <!-- 收藏按鈕 -->
                                <button type="button" class="favorite-btn" :class="{
                                    'is-on': isExchangeFavorited(item),
                                }" :disabled="!exchangeFavoritesLoaded
                                    || isFavoriteActionLoading(
                                        'exchange',
                                        getExchangeListingId(item),
                                    )" :aria-pressed="isExchangeFavorited(item)" :aria-label="isExchangeFavorited(item)
                                        ? '取消收藏'
                                        : '加入收藏'" @click.stop="toggleExchangeFavorite(item)">
                                    <span v-if="isFavoriteActionLoading(
                                        'exchange',
                                        getExchangeListingId(item),
                                    )">
                                        …
                                    </span>
                                    <Icon v-else :icon="isExchangeFavorited(item)
                                        ? heartIcon
                                        : heartOutlineIcon" aria-hidden="true" />
                                </button>
                            </div>
                        </template>

                        <!-- 刊登內容 -->
                        <template #detail>
                            <div class="listing-detail-trigger" role="button" tabindex="0"
                                :aria-label="`查看${item.title || '未命名刊登'}的詳細內容`" @click="openExchangeDetail(item)"
                                @keydown.enter.prevent="openExchangeDetail(item)"
                                @keydown.space.prevent="openExchangeDetail(item)">
                                <!-- 刊登者 -->
                                <div class="listing-publisher">
                                    <Icon :icon="accountIcon" aria-hidden="true" />
                                    <span class="listing-label">刊登者</span>
                                    <span class="listing-publisher-name">
                                        {{ listingUserName(item) }}
                                    </span>
                                </div>

                                <!-- 特典名稱 -->
                                <div class="listing-benefit">
                                    <Icon :icon="giftIcon" aria-hidden="true" />
                                    <span class="listing-label">特典名稱</span>
                                    <span class="listing-benefit-name">
                                        {{ item.benefitName || item.benefit_name || '未提供' }}
                                    </span>
                                </div>

                                <!-- 刊登說明 -->
                                <p class="listing-description">
                                    {{ item.description || '刊登者尚未提供敘述。' }}
                                </p>

                                <!-- 刊登日期 -->
                                <div class="listing-date">
                                    <Icon :icon="calendarIcon" aria-hidden="true" />
                                    <span class="listing-label">刊登日期</span>
                                    <span class="listing-date-value">
                                        {{ formatDate(item.createdAt || item.created_at) }}
                                    </span>
                                </div>
                            </div>
                        </template>

                        <template #action>
                            <div class="listing-actions">
                                <button type="button" class="ticket-link chat-link"
                                    :disabled="!getToken() || isOwnListing(item)" :title="!getToken()
                                        ? '登入後才能聯絡刊登者'
                                        : isOwnListing(item)
                                            ? '這是你自己的刊登，無法聯絡自己'
                                            : '聯絡刊登者'" @click.stop="sendPrivateMessage(item)">
                                    <Icon :icon="chatIcon" aria-hidden="true" />
                                    聯絡刊登者
                                </button>
                            </div>
                        </template>
                    </TicketCard>
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

                <div v-else class="ticket-grid ticket-public-grid">
                    <TicketCard v-for="(item, index) in ticketListings" :key="getTicketListingId(item) ?? index"
                        color="blue" :appear="index < 6" class="ticket-listing-card" role="button" tabindex="0"
                        :aria-label="`查看票券刊登：${item.title || '未命名刊登'}`" @click="openTicketDetail(item)"
                        @keydown.enter.self.prevent="openTicketDetail(item)"
                        @keydown.space.self.prevent="openTicketDetail(item)">
                        <template #title>
                            {{ item.title || item.listingTitle || '未命名刊登' }}
                        </template>

                        <template #stamp>
                            <div class="ticket-stamp-row">
                                <span class="ticket-stamp-text">
                                    票券交換
                                </span>

                                <button type="button" class="favorite-btn" :class="{
                                    'is-on': isTicketFavorited(item),
                                }" :disabled="!ticketFavoritesLoaded
                                    || isFavoriteActionLoading(
                                        'ticket',
                                        getTicketListingId(item),
                                    )" :aria-label="isTicketFavorited(item)
                                        ? '取消收藏'
                                        : '加入收藏'" :aria-pressed="isTicketFavorited(item)"
                                    @click.stop="toggleTicketFavorite(item)">
                                    <span v-if="isFavoriteActionLoading(
                                        'ticket',
                                        getTicketListingId(item),
                                    )">
                                        …
                                    </span>
                                    <Icon v-else :icon="isTicketFavorited(item)
                                        ? heartIcon
                                        : heartOutlineIcon" aria-hidden="true" />
                                </button>
                            </div>
                        </template>

                        <!-- 票券詳細資料：沿用 TicketListingSection 的結構 -->
                        <div class="ticket-detail-content">
                            <div class="ticket-publisher" @click.stop>
                                <Icon :icon="accountIcon" aria-hidden="true" />

                                <span class="ticket-publisher-label">
                                    刊登者
                                </span>

                                <span class="ticket-publisher-name">
                                    {{ listingUserName(item) }}
                                </span>
                            </div>

                            <p class="ticket-description">
                                {{ item.description || '刊登者尚未提供敘述。' }}
                            </p>

                            <dl class="ticket-details">
                                <div class="ticket-detail-item">
                                    <dt>
                                        <Icon :icon="cinemaIcon" aria-hidden="true" />
                                        <span>影城</span>
                                    </dt>
                                    <dd>{{ getCinemaName(item) }}</dd>
                                </div>

                                <div class="ticket-detail-item">
                                    <dt>
                                        <Icon :icon="filmIcon" aria-hidden="true" />
                                        <span>電影</span>
                                    </dt>
                                    <dd>{{ getMovieName(item) }}</dd>
                                </div>

                                <div class="ticket-detail-item">
                                    <dt>
                                        <Icon :icon="calendarIcon" aria-hidden="true" />
                                        <span>放映日期</span>
                                    </dt>
                                    <dd>
                                        {{ formatDate(item.ticketShowtime || item.ticket_showtime) }}
                                    </dd>
                                </div>
                            </dl>

                            <p class="ticket-view-hint">
                                點擊卡片查看詳細資訊
                            </p>
                        </div>

                        <template #action>
                            <button type="button" class="ticket-link" :disabled="!getToken() || isOwnListing(item)"
                                :title="!getToken()
                                    ? '登入後才能聯絡刊登者'
                                    : isOwnListing(item)
                                        ? '這是你自己的刊登，無法聯絡自己'
                                        : '聯絡刊登者'" @click.stop="sendPrivateMessage(item)">
                                <Icon :icon="chatIcon" aria-hidden="true" />
                                <span>聯絡刊登者</span>
                            </button>
                        </template>
                    </TicketCard>
                </div>
            </section>
        </template>

        <!-- 特典詳細內容視窗：沿用綠色 TicketCard -->
        <Teleport to="body">
            <Transition name="exchange-detail-modal">
                <div v-if="selectedExchangeListing" class="detail-modal-overlay" @click.self="closeExchangeDetail">
                    <section class="detail-modal" role="dialog" aria-modal="true"
                        :aria-label="selectedExchangeListing.title || '特典刊登詳細內容'">
                        <TicketCard appear color="green" class="detail-modal-ticket">
                            <template #title>
                                <div class="detail-ticket-heading">
                                    <Icon :icon="giftIcon" aria-hidden="true" />

                                    <h2>
                                        {{ selectedExchangeListing.title || '未命名刊登' }}
                                    </h2>

                                    <button type="button" class="detail-ticket-close" aria-label="關閉詳細內容" title="關閉"
                                        @click.stop="closeExchangeDetail">
                                        <Icon :icon="closeIcon" aria-hidden="true" />
                                    </button>
                                </div>
                            </template>

                            <div class="detail-modal-content">
                                <div class="detail-modal-publisher">
                                    <Icon :icon="accountIcon" aria-hidden="true" />
                                    <span class="detail-modal-label">刊登者</span>
                                    <span class="detail-modal-value">
                                        {{ listingUserName(selectedExchangeListing) }}
                                    </span>
                                </div>

                                <section class="detail-modal-section">
                                    <h3>刊登敘述</h3>
                                    <p class="detail-modal-description">
                                        {{ selectedExchangeListing.description || '刊登者尚未提供敘述。' }}
                                    </p>
                                </section>

                                <!-- 刊登日期：標題與日期內容顯示在同一行 -->
                                <section class="detail-modal-section detail-modal-date-section">
                                    <div class="detail-modal-date">
                                        <h3 class="detail-modal-date-label">刊登日期</h3>
                                        <p class="detail-modal-date-value">
                                            {{ formatDate(
                                                selectedExchangeListing.createdAt
                                                || selectedExchangeListing.created_at,
                                            ) }}
                                        </p>
                                    </div>
                                </section>

                                <!-- 特典圖片：移到刊登日期之後 -->
                                <section class="detail-modal-section">
                                    <h3>特典圖片</h3>

                                    <div v-if="currentDetailImage" class="detail-image-viewer">
                                        <img :key="currentDetailImage" :src="currentDetailImage"
                                            :alt="`特典圖片 ${detailImageIndex + 1}`" class="detail-modal-image"
                                            @error="handleDetailImageError(currentDetailImage)" />

                                        <div v-if="selectedExchangeImages.length > 1" class="detail-image-controls">
                                            <button type="button" class="detail-image-button" aria-label="上一張圖片"
                                                @click="previousDetailImage">
                                                上一張
                                            </button>

                                            <span>
                                                {{ detailImageIndex + 1 }}
                                                /
                                                {{ selectedExchangeImages.length }}
                                            </span>

                                            <button type="button" class="detail-image-button" aria-label="下一張圖片"
                                                @click="nextDetailImage">
                                                下一張
                                            </button>
                                        </div>
                                    </div>

                                    <div v-else class="detail-modal-no-image">
                                        <Icon :icon="giftIcon" aria-hidden="true" />
                                        <span>目前沒有可顯示的圖片</span>
                                    </div>
                                </section>
                            </div>

                            <template #action>
                                <div class="detail-modal-footer">
                                    <button type="button" class="ticket-link detail-modal-action" :class="{
                                        'is-favorited': isExchangeFavorited(selectedExchangeListing),
                                    }" :disabled="!exchangeFavoritesLoaded
                                        || isFavoriteActionLoading(
                                            'exchange',
                                            getExchangeListingId(selectedExchangeListing),
                                        )" @click.stop="toggleExchangeFavorite(selectedExchangeListing)">

                                        <Icon :icon="isExchangeFavorited(selectedExchangeListing)
                                            ? heartIcon
                                            : heartOutlineIcon" aria-hidden="true" />
                                        <span>
                                            {{ isExchangeFavorited(selectedExchangeListing)
                                                ? '取消收藏'
                                                : '加入收藏' }}
                                        </span>
                                    </button>

                                    <button type="button" class="ticket-link is-dark detail-modal-action" :disabled="!getToken() ||
                                        isOwnListing(selectedExchangeListing)
                                        " :title="!getToken()
                                            ? '登入後才能聯絡刊登者'
                                            : isOwnListing(selectedExchangeListing)
                                                ? '這是你自己的刊登，無法聯絡自己'
                                                : '聯絡刊登者'" @click.stop="sendPrivateMessage(selectedExchangeListing)">
                                        <Icon :icon="chatIcon" aria-hidden="true" />
                                        <span>聯絡刊登者</span>
                                    </button>
                                </div>
                            </template>
                        </TicketCard>
                    </section>
                </div>
            </Transition>
        </Teleport>

        <!-- 票券詳細內容視窗：沿用藍色 TicketCard -->
        <Teleport to="body">
            <Transition name="ticket-detail-modal">
                <div v-if="selectedTicketListing" class="detail-modal-overlay" @click.self="closeTicketDetail">
                    <section class="detail-modal" role="dialog" aria-modal="true"
                        :aria-label="selectedTicketListing.title || '票券刊登詳細資訊'">
                        <TicketCard appear color="blue" class="detail-modal-ticket">
                            <template #title>
                                <div class="detail-ticket-heading">
                                    <Icon :icon="ticketIcon" aria-hidden="true" />

                                    <h2>
                                        {{ selectedTicketListing.title
                                            || selectedTicketListing.listingTitle
                                            || '未命名刊登' }}
                                    </h2>

                                    <button type="button" class="detail-ticket-close" aria-label="關閉詳細資訊" title="關閉"
                                        @click.stop="closeTicketDetail">
                                        <Icon :icon="closeIcon" aria-hidden="true" />
                                    </button>
                                </div>
                            </template>

                            <div class="detail-modal-content">
                                <div class="detail-modal-publisher">
                                    <Icon :icon="accountIcon" aria-hidden="true" />
                                    <span class="detail-modal-label">刊登者</span>
                                    <span class="detail-modal-value">
                                        {{ listingUserName(selectedTicketListing) }}
                                    </span>
                                </div>

                                <section class="detail-modal-section">
                                    <h3>刊登敘述</h3>
                                    <p class="detail-modal-description">
                                        {{ selectedTicketListing.description
                                            || '刊登者尚未提供敘述。' }}
                                    </p>
                                </section>

                                <section class="detail-modal-section">
                                    <h3>票券資訊</h3>

                                    <dl class="detail-modal-details">
                                        <div class="detail-modal-detail-item">
                                            <dt>
                                                <Icon :icon="cinemaIcon" aria-hidden="true" />
                                                <span>影城</span>
                                            </dt>
                                            <dd>{{ getCinemaName(selectedTicketListing) }}</dd>
                                        </div>

                                        <div class="detail-modal-detail-item">
                                            <dt>
                                                <Icon :icon="filmIcon" aria-hidden="true" />
                                                <span>電影</span>
                                            </dt>
                                            <dd>{{ getMovieName(selectedTicketListing) }}</dd>
                                        </div>

                                        <div class="detail-modal-detail-item">
                                            <dt>
                                                <Icon :icon="calendarIcon" aria-hidden="true" />
                                                <span>放映日期</span>
                                            </dt>
                                            <dd>
                                                {{ formatDate(
                                                    selectedTicketListing.ticketShowtime
                                                    || selectedTicketListing.ticket_showtime,
                                                ) }}
                                            </dd>
                                        </div>
                                    </dl>
                                </section>
                            </div>

                            <template #action>
                                <div class="detail-modal-footer">
                                    <button type="button" class="ticket-link detail-modal-action" :class="{
                                        'is-favorited': isTicketFavorited(selectedTicketListing),
                                    }" :disabled="!ticketFavoritesLoaded
                                        || isFavoriteActionLoading(
                                            'ticket',
                                            getTicketListingId(selectedTicketListing),
                                        )" @click.stop="toggleTicketFavorite(selectedTicketListing)">
                                        <Icon :icon="isTicketFavorited(selectedTicketListing)
                                            ? heartIcon
                                            : heartOutlineIcon" aria-hidden="true" />
                                        <span>
                                            {{ isTicketFavorited(selectedTicketListing)
                                                ? '取消收藏'
                                                : '加入收藏' }}
                                        </span>
                                    </button>

                                    <button type="button" class="ticket-link is-dark detail-modal-action" :disabled="!getToken() ||
                                        isOwnListing(selectedTicketListing)
                                        " :title="!getToken()
                                            ? '登入後才能聯絡刊登者'
                                            : isOwnListing(selectedTicketListing)
                                                ? '這是你自己的刊登，無法聯絡自己'
                                                : '聯絡刊登者'" @click.stop="sendPrivateMessage(selectedTicketListing)">
                                        <Icon :icon="chatIcon" aria-hidden="true" />
                                        <span>聯絡刊登者</span>
                                    </button>
                                </div>
                            </template>
                        </TicketCard>
                    </section>
                </div>
            </Transition>
        </Teleport>
    </div>
</template>

<style scoped>
.member-public-page {
    max-width: 1100px;
    color: #405b49;
}

.member-card {
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

.empty-state {
    padding: 2rem 1rem;
    border: 1px dashed #d5e4d9;
    border-radius: 1rem;
    background: #fbfdfb;
    color: #84968b;
    text-align: center;
}

/* 卡片排列：使用 TicketCard 的既有版型 */
.ticket-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 1.125rem;
}

/* 僅調整公開會員頁面的特典卡片順序 */
.exchange-public-card :deep(.ticket-reel) {
    order: 0;
}

/* 卡片內容移到海報上方 */
.exchange-public-card :deep(.ticket-body) {
    order: 1;
}

/* 海報移到卡片內容下方 */
.exchange-public-card :deep(.ticket-poster) {
    order: 2;
    margin: 0.65rem 0.65rem 0.65rem;
}

/* 聯絡刊登者操作區維持在卡片最底部 */
.exchange-public-card :deep(.ticket-stub) {
    order: 3;
}

.poster-wrap {
    position: relative;
    display: grid;
    place-items: center;
    width: 100%;
    height: 100%;
    min-height: 8rem;
}

.poster-clickable {
    cursor: pointer;
}

.poster-clickable:focus-visible {
    outline: 2px solid #43866b;
    outline-offset: 3px;
    border-radius: 0.5rem;
}

.poster-image {
    display: block;
    width: 100%;
    height: 100%;
    min-height: 8rem;
    object-fit: cover;
}

.poster-fallback {
    display: grid;
    place-items: center;
    width: 100%;
    height: 100%;
    min-height: 8rem;
}

.poster-fallback :deep(svg) {
    width: 3rem;
    height: 3rem;
}

/* 收藏按鈕 */
.favorite-btn {
    position: absolute;
    top: 0.5rem;
    right: 0.5rem;
    z-index: 1;
    display: grid;
    place-items: center;
    width: 2.6rem;
    height: 2.6rem;
    padding: 0;
    border: 0;
    border-radius: 50%;
    background: rgb(255 255 255 / 88%);
    color: #a33b6b;
    font-size: 1.45rem;
    cursor: pointer;
    box-shadow: 0 4px 10px rgb(41 43 53 / 18%);
    transition:
        transform var(--ease),
        background-color var(--ease),
        opacity var(--ease);
}

.favorite-btn :deep(svg) {
    width: 1.45rem;
    height: 1.45rem;
}

.favorite-btn:hover {
    background: #fff;
    transform: scale(1.08);
}

.favorite-btn.is-on {
    color: #e0457b;
}

.favorite-btn.is-on :deep(svg) {
    animation: heart-pop 0.35s ease;
}

.favorite-btn:disabled {
    cursor: not-allowed;
    opacity: 0.55;
}

.favorite-btn:focus-visible {
    outline: 2px solid #e0457b;
    outline-offset: 2px;
}

@media (hover: hover) and (pointer: fine) {
    .poster-wrap .favorite-btn:not(.is-on) {
        opacity: 0;
        transition:
            opacity var(--ease),
            transform var(--ease),
            background-color var(--ease);
    }

    .poster-wrap:hover .favorite-btn,
    .poster-wrap .favorite-btn:focus-visible {
        opacity: 1;
    }
}

@keyframes heart-pop {
    50% {
        transform: scale(1.3);
    }
}

/* 特典卡片文字區域：比照 ExchangeListingSection.vue */
.listing-detail-trigger {
    min-width: 0;
    cursor: pointer;
    border-radius: 0.35rem;
}

.listing-detail-trigger:focus-visible {
    outline: 2px solid #43866b;
    outline-offset: 3px;
}

/* 刊登者 */
.listing-publisher {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.4rem;
    margin: 0 0 0.7rem;
    padding-bottom: 0.7rem;
    border-bottom: 1px solid #f1f5f9;
    color: #475569;
    font-size: 1rem;
    line-height: 1.6;
}

.listing-publisher :deep(svg) {
    flex-shrink: 0;
    width: 1rem;
    height: 1rem;
}

.listing-label {
    color: #64748b;
    font-weight: 600;
}

.listing-publisher-name {
    min-width: 0;
    color: #334155;
    font-weight: 750;
    overflow-wrap: anywhere;
}

/* 特典名稱 */
.listing-benefit {
    display: flex;
    align-items: center;
    flex-wrap: nowrap;
    gap: 0.4rem;
    min-width: 0;
    margin: 0.5rem 0 0;
    color: #475569;
    font-size: 1rem;
    line-height: 1.6;
}

.listing-benefit :deep(svg) {
    flex-shrink: 0;
    width: 1rem;
    height: 1rem;
}

.listing-benefit-name {
    display: block;
    flex: 1 1 0;
    min-width: 0;
    overflow: hidden;
    color: #334155;
    font-weight: 750;
    white-space: nowrap;
    text-overflow: ellipsis;
}

/* 刊登說明 */
.listing-description {
    display: -webkit-box;
    height: calc(0.92rem * 1.8 * 2);
    min-height: calc(0.92rem * 1.8 * 2);
    margin: 0.5rem 0.5rem 0;
    padding: 0 1rem;
    box-sizing: border-box;
    overflow: hidden;
    color: #3e4450;
    font-size: 0.92rem;
    line-height: 1.8;
    overflow-wrap: anywhere;
    white-space: pre-line;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    line-clamp: 2;
}

/* 刊登日期 */
.listing-date {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.4rem;
    margin: 0.5rem 0 0;
    color: #475569;
    font-size: 1rem;
    line-height: 1.6;
}

.listing-date :deep(svg) {
    flex-shrink: 0;
    width: 1rem;
    height: 1rem;
}

.listing-date-value {
    min-width: 0;
    color: #334155;
    overflow-wrap: anywhere;
}

/* 票券卡片 */
.ticket-listing-card {
    cursor: pointer;
}

.ticket-listing-card:focus-visible {
    outline: 3px solid #6689d8;
    outline-offset: 4px;
    border-radius: 0.75rem;
}

.ticket-detail-content {
    min-width: 0;
    padding-top: 0.75rem;
}

.ticket-public-grid :deep(.ticket-stamp) {
    display: block;
    width: 100%;
    padding: 0;
    border: 0;
    border-radius: 0;
    transform: none;
}

.ticket-stamp-row {
    display: flex;
    align-items: center;
    width: 100%;
    gap: 0.65rem;
}

.ticket-stamp-row .favorite-btn {
    position: static;
    margin-left: auto;
}

.ticket-stamp-text {
    display: inline-block;
    padding: 0.1rem 0.5rem;
    border: 2px solid var(--ticket-ink, #1e2b45);
    border-radius: 7px;
    color: var(--ticket-ink, #1e2b45);
    font-size: var(--text-control, 1.0625rem);
    font-weight: 800;
    line-height: 1.4;
    transform: rotate(-5deg);
}

.ticket-publisher {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.4rem;
    margin: 0 0 0.7rem;
    padding-bottom: 0.7rem;
    border-bottom: 1px solid #f1f5f9;
    color: #475569;
    font-size: 1rem;
    line-height: 1.6;
}

.ticket-publisher :deep(svg) {
    flex-shrink: 0;
    width: 1rem;
    height: 1rem;
}

.ticket-publisher-label {
    color: #64748b;
    font-weight: 600;
}

.ticket-publisher-name {
    min-width: 0;
    color: #334155;
    font-weight: 750;
    overflow-wrap: anywhere;
}

.ticket-description {
    display: -webkit-box;
    min-height: calc(0.92rem * 1.8 * 2);
    margin: 0 0 0.75rem;
    padding-left: 1.5rem;
    overflow: hidden;
    color: #475569;
    font-size: 0.92rem;
    line-height: 1.8;
    overflow-wrap: anywhere;
    white-space: pre-line;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
}

.ticket-details {
    display: grid;
    gap: 0.65rem;
    margin: 1.1rem 0 0;
    padding-top: 1rem;
    border-top: 1px solid #f1f5f9;
}

.ticket-detail-item {
    display: grid;
    grid-template-columns: minmax(5rem, auto) minmax(0, 1fr);
    align-items: start;
    gap: 0.6rem;
}

.ticket-detail-item dt {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    margin: 0;
    color: #64748b;
    font-size: 0.95rem;
    font-weight: 600;
}

.ticket-detail-item dt :deep(svg) {
    flex-shrink: 0;
    width: 1rem;
    height: 1rem;
}

.ticket-detail-item dd {
    min-width: 0;
    margin: 0;
    padding-left: 0.5rem;
    color: #334155;
    font-size: 1rem;
    font-weight: 700;
    line-height: 1.6;
    overflow-wrap: anywhere;
}

.ticket-view-hint {
    margin: 0.8rem 0 0;
    color: #64748b;
    font-size: 0.78rem;
    text-align: right;
}

.ticket-link {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.45rem;
    max-width: 100%;
    cursor: pointer;
}

.ticket-link:disabled {
    opacity: 0.55;
    cursor: not-allowed;
}

.ticket-link :deep(svg) {
    flex-shrink: 0;
    width: 1.1rem;
    height: 1.1rem;
}

.listing-actions {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.5rem;
}

.chat-link {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.35rem;
    cursor: pointer;
}

/* 詳細視窗：沿用參考元件的 TicketCard modal 結構 */
.detail-modal-overlay {
    position: fixed;
    z-index: 2000;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 1rem;
    overflow-y: auto;
    background: rgb(15 23 42 / 62%);
    backdrop-filter: blur(4px);
    overscroll-behavior: contain;
}

.detail-modal {
    width: min(100%, 42rem);
    max-height: min(90vh, 56rem);
    max-height: min(90dvh, 56rem);
    min-width: 0;
    min-height: 0;
    overflow-x: hidden;
    overflow-y: auto;
    overscroll-behavior: contain;
}

.detail-modal-ticket {
    width: 100%;
    min-width: 0;
}

.detail-modal-ticket :deep(.ticket-frame),
.detail-modal-ticket :deep(.ticket-frame:hover) {
    filter: drop-shadow(0 8px 12px color-mix(in srgb, var(--c-ink) 20%, transparent));
    transition: none;
}

.detail-modal-ticket :deep(.ticket),
.detail-modal-ticket :deep(.ticket:hover) {
    min-height: 0;
    transform: none;
}

.detail-modal-ticket :deep(.ticket-body h2) {
    width: 100%;
    min-width: 0;
}

.detail-ticket-heading {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    width: 100%;
    min-width: 0;
}

.detail-ticket-heading> :deep(svg) {
    flex-shrink: 0;
    width: 1.4rem;
    height: 1.4rem;
}

.detail-ticket-heading h2 {
    flex: 1;
    min-width: 0;
    margin: 0;
    font-size: 1.15rem;
    font-weight: 800;
    line-height: 1.5;
    overflow-wrap: anywhere;
}

.detail-ticket-close {
    display: inline-grid;
    flex-shrink: 0;
    place-items: center;
    width: 2.25rem;
    height: 2.25rem;
    padding: 0;
    border: 1.5px solid var(--ticket-ink);
    border-radius: 0.7rem;
    background: #fff;
    color: var(--ticket-ink);
    cursor: pointer;
    transition:
        background-color 0.2s ease,
        color 0.2s ease,
        opacity 0.2s ease;
}

.detail-ticket-close :deep(svg) {
    width: 1.25rem;
    height: 1.25rem;
}

.detail-ticket-close:hover {
    background: var(--accent);
    color: var(--c-ink);
}

.detail-ticket-close:focus-visible {
    outline: 2px solid var(--ticket-ink);
    outline-offset: 3px;
}

.detail-modal-content {
    max-height: min(60vh, 60dvh);
    min-height: 0;
    overflow-y: auto;
    padding: 0.25rem 0;
    overscroll-behavior: contain;
    -webkit-overflow-scrolling: touch;
}

.detail-modal-publisher {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.5rem;
    margin-bottom: 1.1rem;
    padding: 0.85rem 0;
    border-top: 1px solid #e2e8f0;
    border-bottom: 1px solid #e2e8f0;
    color: #334155;
}

.detail-modal-publisher> :deep(svg) {
    flex-shrink: 0;
    width: 1.15rem;
    height: 1.15rem;
    color: #64748b;
}

.detail-modal-label {
    color: #64748b;
    font-size: 0.92rem;
    font-weight: 650;
}

.detail-modal-value {
    min-width: 0;
    font-weight: 800;
    overflow-wrap: anywhere;
}

.detail-modal-section+.detail-modal-section {
    margin-top: 1.35rem;
    padding-top: 1.2rem;
    border-top: 1px solid #e2e8f0;
}

.detail-modal-section h3 {
    margin: 0 0 0.75rem;
    color: #1e293b;
    font-size: 1rem;
    font-weight: 800;
}

.detail-modal-description {
    margin: 0;
    padding-left: 1em;
    color: #475569;
    font-size: 0.95rem;
    line-height: 1.9;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
}

/* 特典詳細視窗：刊登日期標題與日期內容顯示在同一行，不額外縮排 */
.detail-modal-date-section {
    padding-left: 0;
    margin-left: 0;
}

.detail-modal-section .detail-modal-date {
    display: flex;
    align-items: baseline;
    flex-wrap: nowrap;
    gap: 0.5rem;
    min-width: 0;
    margin-left: 0;
    padding-left: 0;
}

.detail-modal-date-label {
    flex-shrink: 0;
    margin: 0;
    padding: 0;
    text-indent: 0;
    white-space: nowrap;
}

.detail-modal-date-value {
    min-width: 0;
    margin: 0;
    overflow-wrap: anywhere;
}

.detail-modal-details {
    display: grid;
    gap: 0.9rem;
    margin: 0;
    padding-left: 1em;
}

.detail-modal-detail-item {
    display: grid;
    grid-template-columns: minmax(6rem, 0.35fr) minmax(0, 1fr);
    align-items: start;
    gap: 0.8rem;
}

.detail-modal-detail-item dt {
    display: inline-flex;
    align-items: center;
    gap: 0.45rem;
    margin: 0;
    color: #64748b;
    font-size: 0.9rem;
    font-weight: 650;
}

.detail-modal-detail-item dt :deep(svg) {
    flex-shrink: 0;
    width: 1.1rem;
    height: 1.1rem;
}

.detail-modal-detail-item dd {
    min-width: 0;
    margin: 0;
    color: #334155;
    font-size: 0.95rem;
    font-weight: 750;
    line-height: 1.7;
    overflow-wrap: anywhere;
}

.detail-image-viewer {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.75rem;
    padding-left: 1em;
}

.detail-modal-image {
    display: block;
    max-width: 100%;
    max-height: 45dvh;
    object-fit: contain;
    border-radius: 0.5rem;
}

.detail-image-controls {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 1rem;
}

.detail-image-button {
    padding: 0.4rem 0.75rem;
    border: 1px solid #d5e4d9;
    border-radius: 0.5rem;
    background: #fff;
    color: #365844;
    cursor: pointer;
}

.detail-image-button:hover {
    background: #edf6f0;
}

.detail-modal-no-image {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.6rem;
    padding: 1.5rem;
    color: #64748b;
}

.detail-modal-no-image :deep(svg) {
    width: 1.5rem;
    height: 1.5rem;
}

.detail-modal-ticket :deep(.ticket-stub) {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: flex-end;
    gap: 0.65rem;
    padding: 0.85rem 1.1rem;
}

.detail-modal-footer {
    display: flex;
    flex: 1;
    flex-wrap: wrap;
    align-items: center;
    justify-content: flex-end;
    gap: 0.65rem;
    min-width: 0;
}

.detail-modal-ticket :deep(.detail-modal-action) {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.45rem;
    min-height: 42px;
    padding: 0.6rem 1rem;
    font-family: inherit;
    font-size: 0.88rem;
    font-weight: 750;
    line-height: 1.4;
    text-align: center;
    white-space: normal;
    overflow-wrap: anywhere;
    cursor: pointer;
    transition:
        background-color 0.2s ease,
        color 0.2s ease;
}

.detail-modal-ticket :deep(.detail-modal-action svg) {
    flex-shrink: 0;
    width: 1.1rem;
    height: 1.1rem;
}

.detail-modal-ticket :deep(.detail-modal-action:not(.is-dark)) {
    border: 1px solid #ded8d2;
    background: #fff;
    color: #334155;
}


/* 詳細視窗：已收藏時，按鈕文字與愛心圖示使用粉紅色 */
.detail-modal-ticket :deep(.detail-modal-action.is-favorited) {
    border-color: #f2b8cf;
    color: #c7376b;
}

/* 明確指定已收藏按鈕內的 SVG 愛心顏色 */
.detail-modal-ticket :deep(.detail-modal-action.is-favorited svg) {
    color: #c7376b;
    fill: currentColor;
    stroke: currentColor;
}


.detail-modal-ticket :deep(.detail-modal-action:not(.is-dark):hover) {
    background: #f5f2ef;
}

.detail-modal-ticket :deep(.detail-modal-action.is-dark) {
    border-color: var(--ticket-ink);
    background: var(--ticket-ink);
    color: #fff;
}

.detail-modal-ticket :deep(.detail-modal-action.is-dark:hover:not(:disabled)),
.detail-modal-ticket :deep(.detail-modal-action.is-dark:focus-visible) {
    border-color: var(--ticket-ink);
    background: color-mix(in srgb, var(--ticket-ink) 78%, white);
    color: #fff;
}

.detail-modal-ticket :deep(.detail-modal-action:disabled) {
    cursor: not-allowed;
    opacity: 0.45;
}

.detail-modal-ticket :deep(.detail-modal-action:focus-visible) {
    outline: 2px solid var(--ticket-ink);
    outline-offset: 3px;
}

.exchange-detail-modal-enter-active,
.exchange-detail-modal-leave-active,
.ticket-detail-modal-enter-active,
.ticket-detail-modal-leave-active {
    transition: opacity 0.2s ease;
}

.exchange-detail-modal-enter-active .detail-modal,
.exchange-detail-modal-leave-active .detail-modal,
.ticket-detail-modal-enter-active .detail-modal,
.ticket-detail-modal-leave-active .detail-modal {
    transition:
        opacity 0.2s ease,
        transform 0.2s ease;
}

.exchange-detail-modal-enter-from,
.exchange-detail-modal-leave-to,
.ticket-detail-modal-enter-from,
.ticket-detail-modal-leave-to {
    opacity: 0;
}

.exchange-detail-modal-enter-from .detail-modal,
.exchange-detail-modal-leave-to .detail-modal,
.ticket-detail-modal-enter-from .detail-modal,
.ticket-detail-modal-leave-to .detail-modal {
    opacity: 0;
    transform: translateY(12px) scale(0.98);
}

@media (max-width: 991px) {
    .ticket-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 1rem;
    }
}

@media (max-width: 639px) {
    .ticket-grid {
        grid-template-columns: minmax(0, 1fr);
        gap: 1rem;
    }

    .detail-modal-overlay {
        align-items: flex-end;
        padding: 0.75rem;
    }

    .detail-modal {
        width: 100%;
        max-width: 100%;
        max-height: calc(100dvh - 1.5rem);
    }

    .detail-modal-content {
        max-height: 58dvh;
        padding-bottom: 0.5rem;
    }

    .detail-ticket-heading {
        gap: 0.5rem;
    }

    .detail-ticket-close {
        width: 2rem;
        height: 2rem;
    }

    .detail-modal-detail-item {
        grid-template-columns: 5.2rem minmax(0, 1fr);
        gap: 0.5rem;
    }

    .detail-modal-ticket :deep(.ticket-body) {
        padding: 0.9rem 0.85rem 1rem;
    }

    .detail-modal-ticket :deep(.ticket-stub) {
        align-items: stretch;
        justify-content: center;
        padding: 0.8rem;
    }

    .detail-modal-footer {
        flex-direction: column;
        align-items: stretch;
        width: 100%;
    }

    .detail-modal-ticket :deep(.detail-modal-action) {
        width: 100%;
        min-height: 44px;
    }

    .member-avatar {
        width: 60px;
        height: 60px;
        font-size: 2rem;
    }

    .section-heading h3 {
        font-size: 1.15rem;
    }
}

@media (prefers-reduced-motion: reduce) {

    .detail-ticket-close,
    .exchange-detail-modal-enter-active,
    .exchange-detail-modal-leave-active,
    .ticket-detail-modal-enter-active,
    .ticket-detail-modal-leave-active,
    .exchange-detail-modal-enter-active .detail-modal,
    .exchange-detail-modal-leave-active .detail-modal,
    .ticket-detail-modal-enter-active .detail-modal,
    .ticket-detail-modal-leave-active .detail-modal {
        transition: none;
    }
}
</style>