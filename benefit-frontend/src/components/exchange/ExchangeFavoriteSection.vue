<!-- ========== components/exchange/ExchangeFavoriteSection.vue ========== -->
<!-- 負責收藏的交換刊登卡片、詳細內容視窗、取消收藏及聯絡刊登者。 -->

<script setup>
import { computed, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { Icon } from '@iconify/vue'
import { useRouter } from 'vue-router'

import giftIcon from '@iconify-icons/mdi/gift-outline'
import accountIcon from '@iconify-icons/mdi/account'
import calendarIcon from '@iconify-icons/mdi/calendar-blank-outline'
import heartIcon from '@iconify-icons/mdi/heart'
import chatIcon from '@iconify-icons/mdi/chat-outline'

const props = defineProps({
    favorites: {
        type: Array,
        default: () => []
    },
    isLoggedIn: {
        type: Boolean,
        default: false
    },
    currentUserId: {
        type: [Number, String],
        default: null
    }
})

const emit = defineEmits([
    'remove-favorite',
    'open-chat'
])

const router = useRouter()

// 詳細視窗目前選取的交換刊登。
const selectedListing = ref(null)

// 詳細視窗目前顯示的圖片索引。
const detailImageIndex = ref(0)

// 各張收藏卡片目前顯示的圖片索引，與詳細視窗分開管理。
const cardImageIndexes = ref({})

// 取得收藏卡片的唯一識別鍵。
function cardImageKey(favorite, index) {
    return String(
        favorite?.exchangeFavoriteId ??
        favorite?.exchange_favorite_id ??
        listingId(favorite) ??
        index
    )
}

// 取得收藏卡片目前顯示的圖片。
function currentCardImage(favorite, index) {
    const images = imageCandidates(favorite)
    const key = cardImageKey(favorite, index)
    const imageIndex = cardImageIndexes.value[key] ?? 0

    return images[imageIndex] ?? ''
}

// 切換收藏卡片的圖片。
function changeCardImage(favorite, index, direction) {
    const images = imageCandidates(favorite)

    if (images.length <= 1) return

    const key = cardImageKey(favorite, index)
    const currentIndex = cardImageIndexes.value[key] ?? 0

    cardImageIndexes.value = {
        ...cardImageIndexes.value,
        [key]: (currentIndex + direction + images.length) % images.length
    }
}

// 開始觸控滑動時，記錄起始位置。
function handleCardTouchStart(event) {
    event.currentTarget.dataset.touchStartX = String(
        event.changedTouches[0].clientX
    )
}

// 結束觸控滑動時，依方向切換圖片。
function handleCardTouchEnd(event, favorite, index) {
    const startX = Number(event.currentTarget.dataset.touchStartX)
    const endX = event.changedTouches[0].clientX
    const distance = endX - startX

    // 滑動距離至少 30px 才切換，避免輕微觸碰造成誤判。
    if (Math.abs(distance) < 30) return

    changeCardImage(favorite, index, distance < 0 ? 1 : -1)
}

// 卡片圖片載入失敗時，嘗試顯示下一張圖片。
function handleCardImageError(favorite, index) {
    const images = imageCandidates(favorite)

    if (images.length <= 1) return

    changeCardImage(favorite, index, 1)
}

// 記錄詳細視窗中載入失敗的圖片。
const failedDetailImages = ref([])

// 取得收藏對應的交換刊登資料。
function favoriteListing(favorite) {
    return (
        favorite?.exchangeListing ??
        favorite?.exchangeListingBean ??
        favorite?.listing ??
        favorite
    )
}

// 取得交換刊登 ID。
function listingId(favorite) {
    const listing = favoriteListing(favorite)

    return (
        listing?.exchangeListingId ??
        listing?.exchange_listing_id ??
        listing?.id ??
        favorite?.exchangeListingId ??
        favorite?.exchange_listing_id ??
        null
    )
}

// 取得刊登標題。
function listingTitle(favorite) {
    const listing = favoriteListing(favorite)

    return listing?.title || '未提供刊登標題'
}

// 取得刊登者 ID。
function listingMemberId(item) {
    return (
        item?.usersId ??
        item?.users_id ??
        item?.user?.usersId ??
        item?.user?.users_id ??
        item?.users?.usersId ??
        item?.users?.users_id ??
        item?.usersBean?.usersId ??
        item?.usersBean?.users_id ??
        null
    )
}

// 取得刊登者名稱。
function listingMemberName(favorite) {
    const listing = favoriteListing(favorite)

    return (
        listing?.usersName ??
        listing?.userName ??
        listing?.username ??
        listing?.memberName ??
        listing?.user?.usersName ??
        listing?.users?.usersName ??
        listing?.usersBean?.usersName ??
        ''
    )
}

// 取得特典名稱。
function listingBenefitName(favorite) {
    const listing = favoriteListing(favorite)

    return (
        listing?.benefitName ??
        listing?.benefit_name ??
        listing?.benefit?.benefitName ??
        '未提供'
    )
}

// 取得特典類型。
function listingBenefitType(favorite) {
    const listing = favoriteListing(favorite)

    return (
        listing?.benefitType ??
        listing?.benefit_type ??
        listing?.benefit?.benefitType ??
        ''
    )
}

// 取得刊登說明。
function listingDescription(favorite) {
    const listing = favoriteListing(favorite)

    return (
        listing?.description ||
        '刊登者尚未提供敘述。'
    )
}

// 取得刊登建立日期。
function listingCreatedDate(favorite) {
    const listing = favoriteListing(favorite)

    const value =
        listing?.createdAt ??
        listing?.created_at ??
        listing?.listingCreatedAt ??
        listing?.listing_created_at

    return formatDate(value)
}

// 日期只顯示 yyyy-mm-dd。
function formatDate(value) {
    if (!value) return '未提供'

    const dateString = String(value).trim().split(/[T ]/)[0]

    return dateString || '未提供'
}

// 取得會員持有特典照片，最多 5 張。
function memberPhotoCandidates(favorite) {
    const listing = favoriteListing(favorite)

    const photos =
        listing?.useBenefitPhotoUrls ??
        listing?.use_benefit_photo_urls ??
        []

    if (!Array.isArray(photos)) return []

    return [
        ...new Set(
            photos
                .filter(url => typeof url === 'string')
                .map(url => url.trim())
                .filter(Boolean)
        )
    ].slice(0, 5)
}

// 取得官方特典照片。
function officialPhotoCandidate(favorite) {
    const listing = favoriteListing(favorite)

    const url =
        listing?.officialImageUrl ??
        listing?.official_image_url ??
        ''

    return typeof url === 'string' ? url.trim() : ''
}

// 有會員持有照片時優先使用；沒有才使用官方照片。
function imageCandidates(favorite) {
    const memberPhotos = memberPhotoCandidates(favorite)

    if (memberPhotos.length > 0) {
        return memberPhotos
    }

    const officialPhoto = officialPhotoCandidate(favorite)

    return officialPhoto ? [officialPhoto] : []
}

// 取得收藏資料對應的交換刊登 ID，供取消收藏事件使用。
function removeFavorite(favorite) {
    emit('remove-favorite', favoriteListing(favorite))
}

// 前往刊登者的公開會員頁。
function goToMember(item) {
    const memberId = listingMemberId(item)

    if (
        memberId === null ||
        memberId === undefined ||
        String(memberId).trim() === ''
    ) {
        console.warn('無法開啟會員頁：這筆交換刊登沒有可用的會員 ID。')
        return
    }

    closeDetail()

    router.push({
        name: 'member-public',
        params: {
            id: String(memberId)
        }
    })
}

// 判斷目前登入會員是否為刊登者。
function isOwnListing(item) {
    const currentUserId = props.currentUserId
    const targetUserId = listingMemberId(item)

    if (
        currentUserId === null ||
        currentUserId === undefined ||
        currentUserId === '' ||
        targetUserId === null ||
        targetUserId === undefined ||
        targetUserId === ''
    ) {
        return false
    }

    return String(currentUserId) === String(targetUserId)
}

// 開啟詳細內容視窗。
function openDetail(favorite) {
    const listing = favoriteListing(favorite)

    if (!listing) return

    selectedListing.value = listing
    detailImageIndex.value = 0
    failedDetailImages.value = []
}

// 關閉詳細內容視窗。
function closeDetail() {
    selectedListing.value = null
    detailImageIndex.value = 0
    failedDetailImages.value = []
}

// 詳細視窗中仍可顯示的圖片。
const availableDetailImages = computed(() => {
    if (!selectedListing.value) return []

    return imageCandidates(selectedListing.value).filter(
        url => !failedDetailImages.value.includes(url)
    )
})

// 目前顯示的圖片。
const currentDetailImage = computed(
    () => availableDetailImages.value[detailImageIndex.value] ?? ''
)

// 切換上一張圖片。
function previousDetailImage() {
    const total = availableDetailImages.value.length

    if (total <= 1) return

    detailImageIndex.value =
        (detailImageIndex.value - 1 + total) % total
}

// 切換下一張圖片。
function nextDetailImage() {
    const total = availableDetailImages.value.length

    if (total <= 1) return

    detailImageIndex.value =
        (detailImageIndex.value + 1) % total
}

// 圖片載入失敗時排除該圖片。
function handleDetailImageError(url) {
    if (!failedDetailImages.value.includes(url)) {
        failedDetailImages.value = [
            ...failedDetailImages.value,
            url
        ]

        const remaining = availableDetailImages.value.length

        if (remaining === 0 || detailImageIndex.value >= remaining) {
            detailImageIndex.value = 0
        }
    }
}

// 詳細視窗中的取消收藏。
function removeFavoriteFromDetail() {
    if (!selectedListing.value) return

    const listing = selectedListing.value

    closeDetail()
    emit('remove-favorite', listing)
}

// 詳細視窗中的聯絡刊登者。
function openChatFromDetail() {
    if (
        !selectedListing.value ||
        !props.isLoggedIn ||
        isOwnListing(selectedListing.value)
    ) {
        return
    }

    const listing = selectedListing.value

    closeDetail()
    emit('open-chat', listing)
}

// Escape 鍵關閉詳細視窗。
function handleEscape(event) {
    if (event.key === 'Escape' && selectedListing.value) {
        closeDetail()
    }
}

// 註冊及移除鍵盤事件。
onMounted(() => {
    window.addEventListener('keydown', handleEscape)
})

onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleEscape)
})

// 收藏資料更新後，如果目前選取的刊登已不存在，關閉視窗。
watch(
    () => props.favorites,
    () => {
        if (!selectedListing.value) return

        const selectedId = listingId(selectedListing.value)

        const stillExists = props.favorites.some(favorite => {
            return String(listingId(favorite)) === String(selectedId)
        })

        if (!stillExists) {
            closeDetail()
        }
    }
)
</script>

<template>
    <section class="exchange-favorites">
        <div v-if="favorites.length === 0" class="empty-state">
            目前沒有收藏的交換刊登
        </div>

        <div v-else class="favorite-list">
            <article v-for="(favorite, index) in favorites" :key="favorite?.exchangeFavoriteId ??
                favorite?.exchange_favorite_id ??
                listingId(favorite) ??
                index
                " class="favorite-card" role="button" tabindex="0" :aria-label="`查看${listingTitle(favorite)}的詳細內容`"
                @click="openDetail(favorite)" @keydown.enter.prevent="openDetail(favorite)"
                @keydown.space.prevent="openDetail(favorite)">

                <div class="favorite-images" @touchstart="handleCardTouchStart"
                    @touchend="handleCardTouchEnd($event, favorite, index)">
                    <template v-if="imageCandidates(favorite).length > 0">
                        <img :key="currentCardImage(favorite, index)" :src="currentCardImage(favorite, index)"
                            :alt="`${listingTitle(favorite)} 的照片 ${(cardImageIndexes[cardImageKey(favorite, index)] ?? 0) + 1}`"
                            class="favorite-image" @error="handleCardImageError(favorite, index)" />

                        <template v-if="imageCandidates(favorite).length > 1">
                            <button type="button" class="card-image-arrow card-image-previous" aria-label="上一張特典圖片"
                                title="上一張圖片" @click.stop="changeCardImage(favorite, index, -1)">
                                ‹
                            </button>

                            <span class="card-image-count">
                                {{ (cardImageIndexes[cardImageKey(favorite, index)] ?? 0) + 1 }}
                                /
                                {{ imageCandidates(favorite).length }}
                            </span>

                            <button type="button" class="card-image-arrow card-image-next" aria-label="下一張特典圖片"
                                title="下一張圖片" @click.stop="changeCardImage(favorite, index, 1)">
                                ›
                            </button>
                        </template>
                    </template>

                    <div v-else class="no-image" aria-label="沒有特典圖片">
                        <Icon :icon="giftIcon" class="gift-icon" />
                    </div>
                </div>


                <div class="favorite-content">
                    <h3 class="favorite-title" :title="listingTitle(favorite)">
                        {{ listingTitle(favorite) }}
                    </h3>

                    <p class="favorite-member">
                        刊登者：{{ listingMemberName(favorite) || '未知會員' }}
                    </p>

                    <p class="favorite-benefit">
                        特典名稱 ：{{ listingBenefitName(favorite) }}
                        <span v-if="listingBenefitType(favorite)">
                            （{{ listingBenefitType(favorite) }}）
                        </span>
                    </p>

                    <p class="favorite-description">
                        {{ listingDescription(favorite) }}
                    </p>

                    <p class="favorite-date">
                        刊登日期：{{ listingCreatedDate(favorite) }}
                    </p>

                    <button type="button" class="remove-favorite-button" :disabled="!listingId(favorite)"
                        @click.stop="removeFavorite(favorite)" @keydown.stop>
                        <Icon :icon="heartIcon" aria-hidden="true" />
                        取消收藏
                    </button>
                </div>
            </article>
        </div>

        <!-- 詳細內容視窗 -->
        <Teleport to="body">
            <div v-if="selectedListing" class="detail-modal-overlay" @click.self="closeDetail">
                <section class="detail-modal" role="dialog" aria-modal="true" aria-labelledby="detail-modal-title">
                    <div class="detail-modal-card">
                        <!-- 標題及關閉按鈕 -->
                        <header class="detail-modal-header">
                            <h3 id="detail-modal-title" class="detail-modal-title">
                                {{ listingTitle(selectedListing) }}
                            </h3>

                            <button type="button" class="detail-ticket-close" aria-label="關閉詳細內容" title="關閉"
                                @click="closeDetail">
                                ×
                            </button>
                        </header>

                        <div class="detail-modal-content">
                            <!-- 刊登者 -->
                            <p class="detail-modal-info">
                                <Icon :icon="accountIcon" aria-hidden="true" />
                                <strong>刊登者</strong>

                                <button v-if="listingMemberName(selectedListing)" type="button" class="member-name-link"
                                    @click="goToMember(selectedListing)">
                                    {{ listingMemberName(selectedListing) }}
                                </button>

                                <span v-else>未提供</span>
                            </p>

                            <!-- 特典名稱 -->
                            <p class="detail-modal-info">
                                <!-- <Icon :icon="giftIcon" aria-hidden="true" /> -->
                                <strong>特典名稱</strong>

                                <span>
                                    {{ listingBenefitName(selectedListing) }}

                                    <template v-if="listingBenefitType(selectedListing)">
                                        （{{ listingBenefitType(selectedListing) }}）
                                    </template>
                                </span>
                            </p>

                            <!-- 刊登敘述 -->
                            <section class="detail-modal-section">
                                <h4 class="detail-modal-heading">
                                    刊登敘述
                                </h4>

                                <p class="detail-modal-description">
                                    {{ listingDescription(selectedListing) }}
                                </p>
                            </section>

                            <!-- 刊登日期 -->
                            <p class="detail-modal-info">
                                <!-- <Icon :icon="calendarIcon" aria-hidden="true" /> -->
                                <strong>刊登日期</strong>
                                <span>{{ listingCreatedDate(selectedListing) }}</span>
                            </p>

                            <!-- 特典圖片 -->
                            <section class="detail-modal-images">
                                <h4 class="detail-modal-heading">
                                    特典圖片
                                </h4>

                                <div v-if="currentDetailImage" class="detail-image-viewer">
                                    <img :key="currentDetailImage" :src="currentDetailImage"
                                        :alt="`特典圖片 ${detailImageIndex + 1}`" class="detail-modal-image"
                                        @error="handleDetailImageError(currentDetailImage)" />

                                    <div v-if="availableDetailImages.length > 1" class="detail-image-controls">
                                        <button type="button" class="detail-image-arrow" aria-label="上一張圖片"
                                            title="上一張圖片" @click="previousDetailImage">
                                            ‹
                                        </button>

                                        <span class="detail-image-count">
                                            {{ detailImageIndex + 1 }}
                                            /
                                            {{ availableDetailImages.length }}
                                        </span>

                                        <button type="button" class="detail-image-arrow" aria-label="下一張圖片"
                                            title="下一張圖片" @click="nextDetailImage">
                                            ›
                                        </button>
                                    </div>
                                </div>

                                <div v-else class="detail-modal-no-image">
                                    <Icon :icon="giftIcon" aria-hidden="true" />
                                    <span>目前沒有可顯示的圖片</span>
                                </div>
                            </section>
                        </div>

                        <!-- 詳細視窗底部操作 -->
                        <footer class="detail-modal-footer">
                            <button type="button" class="detail-favorite-button" :disabled="!listingId(selectedListing)"
                                @click="removeFavoriteFromDetail">
                                <Icon :icon="heartIcon" aria-hidden="true" />
                                取消收藏
                            </button>

                            <!--<button type="button" class="detail-chat-button" :disabled="!isLoggedIn ||
                                isOwnListing(selectedListing)
                                " :title="!isLoggedIn
                                    ? '登入後才能聯絡刊登者'
                                    : isOwnListing(selectedListing)
                                        ? '這是你自己的刊登，無法聯絡自己'
                                        : '聯絡刊登者'
                                    " @click="openChatFromDetail">
                                <Icon :icon="chatIcon" aria-hidden="true" />
                                聯絡刊登者
                            </button>-->
                        </footer>
                    </div>
                </section>
            </div>
        </Teleport>
    </section>
</template>

<style scoped>
.exchange-favorites {
    width: 100%;
}

.favorite-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.favorite-card {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    height: 240px;
    max-height: 240px;
    padding: 16px;
    box-sizing: border-box;
    overflow: hidden;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    background: #fff;
    cursor: pointer;
}

.favorite-card:focus-visible {
    outline: 2px solid #43866b;
    outline-offset: 3px;
}


.favorite-images {
    position: relative;
    display: flex;
    flex-wrap: wrap;
    align-content: flex-start;
    flex-shrink: 0;
    gap: 8px;
    width: 120px;
    height: 100%;
    overflow: hidden;
}

.favorite-image {
    display: block;
    width: 100px;
    height: 100px;
    flex-shrink: 0;
    object-fit: cover;
    border-radius: 6px;
    background: #f3f4f6;
}

/* 收藏卡片圖片左右切換按鈕 */
.card-image-arrow {
    position: absolute;
    top: 50%;
    display: grid;
    place-items: center;
    width: 24px;
    height: 30px;
    padding: 0;
    transform: translateY(-50%);
    border: 1px solid rgb(255 255 255 / 80%);
    border-radius: 6px;
    background: rgb(15 23 42 / 65%);
    color: #fff;
    font-family: inherit;
    font-size: 22px;
    line-height: 1;
    cursor: pointer;
}

.card-image-arrow:hover {
    background: rgb(15 23 42 / 85%);
}

.card-image-previous {
    left: 2px;
}

.card-image-next {
    right: 2px;
}

/* 顯示目前圖片編號 */
.card-image-count {
    position: absolute;
    right: 0;
    bottom: 2px;
    left: 0;
    width: fit-content;
    margin: 0 auto;
    padding: 2px 6px;
    border-radius: 10px;
    background: rgb(15 23 42 / 70%);
    color: #fff;
    font-size: 11px;
    line-height: 1.3;
    font-variant-numeric: tabular-nums;
    pointer-events: none;
}


.no-image {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100px;
    height: 100px;
    flex-shrink: 0;
    border-radius: 6px;
    background: #f3f4f6;
    color: #9ca3af;
}

.gift-icon {
    width: 42px;
    height: 42px;
}

.favorite-content {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-width: 0;
    height: 100%;
    overflow: hidden;
    gap: 2px;
}

.favorite-title {
    display: -webkit-box;
    overflow: hidden;
    margin: 0 0 4px;
    font-size: 18px;
    font-weight: 700;
    line-height: 1.4;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 1;
}

.favorite-member,
.favorite-benefit,
.favorite-date {
    flex-shrink: 0;
    overflow: hidden;
    margin: 0 0 2px;
    color: #4b5563;
    font-size: 14px;
    line-height: 1.4;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.favorite-description {
    display: -webkit-box;
    flex: 1;
    min-height: 0;
    overflow: hidden;
    margin: 0 0 6px;
    color: #4b5563;
    font-size: 14px;
    line-height: 1.4;
    overflow-wrap: anywhere;
    white-space: pre-wrap;
    padding-left: 1em;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
}

/* 卡片內取消收藏按鈕：沿用 TicketFavoriteSection.vue 樣式 */
.remove-favorite-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    align-self: flex-end;
    flex-shrink: 0;
    gap: 6px;
    margin-top: auto;
    padding: 0.35rem 0.7rem;
    border: 1px solid #dc2626;
    border-radius: 6px;
    background: #fff;
    color: #dc2626;
    font-family: inherit;
    font-size: 0.875rem;
    font-weight: 600;
    line-height: 1.4;
    cursor: pointer;
}

.remove-favorite-button :deep(svg) {
    width: 16px;
    height: 16px;
}

.remove-favorite-button:hover:not(:disabled) {
    background: #dc2626;
    color: #fff;
}

.remove-favorite-button:disabled {
    cursor: not-allowed;
    opacity: 0.5;
}

.empty-state {
    padding: 32px 16px;
    color: #6b7280;
    text-align: center;
}

/* 詳細內容視窗 */

.detail-modal-overlay {
    position: fixed;
    inset: 0;
    z-index: 1000;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 1rem;
    background: rgb(15 23 42 / 62%);
    overflow-y: auto;
}

.detail-modal {
    width: min(100%, 42rem);
    min-width: 0;
    max-height: min(90vh, 56rem);
    max-height: min(90dvh, 56rem);
    overflow-x: hidden;
    overflow-y: auto;
    overscroll-behavior: contain;
}

.detail-modal-card {
    overflow: hidden;
    border: 1px solid #d1d5db;
    border-radius: 12px;
    background: #fff;
    box-shadow: 0 20px 60px rgb(0 0 0 / 25%);
}

.detail-modal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.75rem;
    padding: 1rem 1.25rem;
    border-bottom: 1px solid #e5e7eb;
    background: #f8fafc;
}

.detail-modal-title {
    min-width: 0;
    margin: 0;
    color: #334155;
    font-size: 1.15rem;
    font-weight: 750;
    overflow-wrap: anywhere;
}

.detail-ticket-close {
    display: inline-grid;
    flex-shrink: 0;
    place-items: center;
    width: 2.25rem;
    height: 2.25rem;
    padding: 0;
    border: 1.5px solid #334155;
    border-radius: 0.7rem;
    background: #fff;
    color: #334155;
    font-family: inherit;
    font-size: 1.65rem;
    line-height: 1;
    cursor: pointer;
}

.detail-ticket-close:hover {
    background: #e5e7eb;
}

.detail-modal-content {
    min-width: 0;
    max-height: 60vh;
    max-height: 60dvh;
    padding: 1rem 1.25rem;
    overflow-x: hidden;
    overflow-y: auto;
    overscroll-behavior: contain;
}

.detail-modal-info {
    display: flex;
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 0.45rem;
    margin: 0.5rem 0 0;
    min-width: 0;
    color: #475569;
    font-size: 1rem;
    line-height: 1.6;
    overflow-wrap: anywhere;
}

.detail-modal-info :deep(svg) {
    flex-shrink: 0;
    width: 1.1rem;
    height: 1.1rem;
    margin-top: 0.2rem;
}

.detail-modal-info strong {
    flex-shrink: 0;
    color: #334155;
    font-weight: 600;
    white-space: nowrap;
}

.detail-modal-info span {
    flex: 1 1 0;
    min-width: 0;
    color: #334155;
    overflow-wrap: anywhere;
}

.member-name-link {
    display: inline;
    min-width: 0;
    padding: 0;
    border: 0;
    background: transparent;
    color: #334155;
    font: inherit;
    font-weight: 750;
    text-align: left;
    text-decoration: underline;
    text-underline-offset: 3px;
    overflow-wrap: anywhere;
    cursor: pointer;
    transition: color 0.2s ease;
}

.member-name-link:hover {
    color: #43866b;
}

.member-name-link:focus-visible {
    outline: 2px solid #15803d;
    outline-offset: 3px;
    border-radius: 3px;
}

.detail-modal-section {
    margin-top: 1rem;
}

.detail-modal-heading {
    margin: 0 0 0.65rem;
    color: #334155;
    font-size: 1rem;
    font-weight: 700;
    line-height: 1.6;
}

.detail-modal-description {
    margin: 0 0 0 1.55rem;
    padding: 0;
    color: #3e4450;
    font-size: 1rem;
    line-height: 1.8;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
}

.detail-modal-images {
    margin: 1rem 0 0.5rem;
    padding-top: 1rem;
    border-top: 1px solid #e5e7eb;
}

.detail-image-viewer,
.detail-modal-no-image {
    margin-left: 1.55rem;
}

.detail-image-viewer {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.75rem;
}

.detail-modal-image {
    display: block;
    width: 100%;
    max-height: 25rem;
    aspect-ratio: 2 / 3;
    border-radius: 0.75rem;
    background: #f1f5f9;
    object-fit: contain;
}

.detail-image-controls {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 1.25rem;
    width: 100%;
}

.detail-image-arrow {
    display: inline-grid;
    place-items: center;
    width: 2.5rem;
    height: 2.5rem;
    padding: 0;
    border: 1px solid #cbd5e1;
    border-radius: 50%;
    background: #fff;
    color: #334155;
    font-size: 1.8rem;
    line-height: 1;
    cursor: pointer;
}

.detail-image-arrow:hover {
    background: #f1f5f9;
}

.detail-image-count {
    min-width: 3rem;
    color: #475569;
    font-size: 0.9rem;
    text-align: center;
    font-variant-numeric: tabular-nums;
}

.detail-modal-no-image {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    min-height: 10rem;
    border: 1px dashed #cbd5e1;
    border-radius: 0.75rem;
    background: #f8fafc;
    color: #64748b;
    text-align: center;
}

.detail-modal-no-image :deep(svg) {
    width: 2.5rem;
    height: 2.5rem;
}

.detail-modal-footer {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: flex-end;
    gap: 0.65rem;
    padding: 0.85rem 1.1rem;
    border-top: 1px solid #e5e7eb;
    background: #f8fafc;
}

.detail-favorite-button,
.detail-chat-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.45rem;
    min-height: 42px;
    padding: 0.6rem 1rem;
    border: 1px solid #cbd5e1;
    border-radius: 6px;
    background: #fff;
    color: #334155;
    font-family: inherit;
    font-size: 0.88rem;
    font-weight: 750;
    line-height: 1.4;
    text-align: center;
    cursor: pointer;
}

.detail-favorite-button {
    border-color: #dc2626;
    color: #dc2626;
}

.detail-favorite-button:hover:not(:disabled) {
    background: #dc2626;
    color: #fff;
}

.detail-chat-button {
    border-color: #334155;
    background: #334155;
    color: #fff;
}

.detail-chat-button:hover:not(:disabled) {
    background: #1e293b;
}

.detail-favorite-button:disabled,
.detail-chat-button:disabled {
    cursor: not-allowed;
    opacity: 0.45;
}

/* 手機版 */

@media (max-width: 640px) {
    .favorite-card {
        height: 240px;
        gap: 10px;
        padding: 12px;
    }

    .favorite-images {
        width: 100px;
        flex: 0 0 100px;
    }

    .favorite-image,
    .no-image {
        width: 100px;
        height: 100px;
    }

    .favorite-title {
        font-size: 16px;
    }

    .favorite-member,
    .favorite-benefit,
    .favorite-description,
    .favorite-date {
        font-size: 13px;
    }

    /* 手機版取消收藏按鈕：沿用 TicketFavoriteSection.vue 樣式 */
    .remove-favorite-button {
        padding: 6px 10px;
        font-size: 0.8rem;
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

    .detail-modal-header {
        padding: 0.9rem;
    }

    .detail-modal-content {
        max-height: 58dvh;
        padding: 0.9rem;
    }

    .detail-modal-image {
        max-height: 20rem;
    }

    .detail-modal-footer {
        flex-direction: column;
        align-items: stretch;
        padding: 0.8rem;
    }

    .detail-favorite-button,
    .detail-chat-button {
        width: 100%;
        min-height: 44px;
    }
}

@media (prefers-reduced-motion: reduce) {
    .member-name-link {
        transition: none;
    }
}
</style>
