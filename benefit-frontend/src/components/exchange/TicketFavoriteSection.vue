<!-- ========== components/exchange/TicketFavoriteSection.vue ========== -->
<!-- 負責會員的票券收藏卡片、票券詳細資訊視窗及取消收藏事件。 -->

<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from "vue";
import { Icon } from "@iconify/vue";
import { useRouter } from "vue-router";

import ticketIcon from "@iconify-icons/mdi/ticket";
import accountIcon from "@iconify-icons/mdi/account";
import calendarIcon from "@iconify-icons/mdi/calendar";
import cinemaIcon from "@iconify-icons/mdi/filmstrip";
import filmIcon from "@iconify-icons/mdi/movie-open";
import closeIcon from "@iconify-icons/mdi/close";
import heartIcon from "@iconify-icons/mdi/heart";

const props = defineProps({
    favorites: {
        type: Array,
        default: () => [],
    },
    cinemaName: {
        type: Function,
        required: true,
    },
    movieName: {
        type: Function,
        required: true,
    },
});

const emit = defineEmits(["remove-favorite"]);

// 使用 Vue Router，前往刊登者的公開會員頁。
const router = useRouter();

// 目前選取的票券刊登，用於顯示詳細資訊視窗。
const selectedListing = ref(null);

// 取得收藏資料對應的票券刊登。
function favoriteListing(favorite) {
    return (
        favorite?.ticketListing ??
        favorite?.ticketListingBean ??
        favorite?.listing ??
        favorite
    );
}

// 取得票券刊登 ID。
function listingId(favorite) {
    const item = favoriteListing(favorite);

    return (
        item?.ticketListingId ??
        item?.ticket_listing_id ??
        item?.id ??
        ""
    );
}

// 取得票券刊登標題。
function listingTitle(item) {
    return item?.title || "未命名刊登";
}

// 取得刊登者 ID。
// 參考 ExchangeFavoriteSection.vue 的 listingMemberId()。
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
    );
}

// 取得刊登者名稱，沿用 TicketListingSection.vue 已提供的欄位。
function listingUserName(item) {
    const name =
        item?.usersName ??
        item?.userName ??
        item?.username ??
        item?.memberName ??
        item?.user?.usersName ??
        item?.users?.usersName ??
        item?.usersBean?.usersName;

    return name !== null && name !== undefined && String(name).trim()
        ? String(name).trim()
        : "未提供";
}

// 前往刊登者的公開會員頁。
// 參考 ExchangeFavoriteSection.vue 的 goToMember()。
function goToMember(item) {
    const memberId = listingMemberId(item);

    if (
        memberId === null ||
        memberId === undefined ||
        String(memberId).trim() === ""
    ) {
        console.warn(
            "無法開啟會員頁：這筆票券刊登沒有可用的會員 ID。"
        );
        return;
    }

    closeDetail();

    router.push({
        name: "member-public",
        params: {
            id: String(memberId),
        },
    });
}

// 取得票券刊登敘述。
function listingDescription(item) {
    return item?.description || "刊登者尚未提供敘述。";
}

// 日期只顯示 yyyy-mm-dd。
function formatDate(value) {
    if (!value) return "未提供";

    return String(value).trim().slice(0, 10) || "未提供";
}

// 取得放映日期。
function listingShowtime(item) {
    return formatDate(
        item?.ticketShowtime ??
        item?.ticket_showtime
    );
}

// 取得影城名稱，沿用父元件傳入的函式。
function listingCinemaName(item) {
    const value = props.cinemaName(item);

    return value !== null && value !== undefined && String(value).trim()
        ? String(value).trim()
        : "未提供";
}

// 取得電影名稱，沿用父元件傳入的函式。
function listingMovieName(item) {
    const value = props.movieName(item);

    return value !== null && value !== undefined && String(value).trim()
        ? String(value).trim()
        : "未提供";
}

// 開啟票券詳細資訊視窗。
function openDetail(favorite) {
    const listing = favoriteListing(favorite);

    if (!listing) return;

    selectedListing.value = listing;
}

// 關閉票券詳細資訊視窗。
function closeDetail() {
    selectedListing.value = null;
}

// 取消收藏。
// 保留原本的 remove-favorite 事件及傳遞票券刊登資料的方式。
function removeFavorite(favorite) {
    const listing = favoriteListing(favorite);

    emit("remove-favorite", listing);
}

// 在詳細資訊視窗中取消收藏，並關閉視窗。
function removeFavoriteFromDetail() {
    if (!selectedListing.value) return;

    const listing = selectedListing.value;

    closeDetail();
    emit("remove-favorite", listing);
}

// Escape 鍵關閉詳細資訊視窗。
function handleEscape(event) {
    if (event.key === "Escape" && selectedListing.value) {
        closeDetail();
    }
}

// 註冊及移除鍵盤事件。
onMounted(() => {
    window.addEventListener("keydown", handleEscape);
});

onBeforeUnmount(() => {
    window.removeEventListener("keydown", handleEscape);
});

// 收藏清單更新後，如果目前查看的票券已不在清單中，就關閉視窗。
watch(
    () => props.favorites,
    (favorites) => {
        if (!selectedListing.value) return;

        const selectedId = listingId(selectedListing.value);

        const stillExists = favorites.some((favorite) => {
            return String(listingId(favorite)) === String(selectedId);
        });

        if (!stillExists) {
            closeDetail();
        }
    }
);
</script>

<template>
    <section class="ticket-favorites">
        <!-- 沒有收藏資料時顯示空狀態 -->
        <div v-if="!favorites.length" class="empty-state">
            目前沒有票券收藏。
        </div>

        <!-- 票券收藏卡片 -->
        <div v-else class="favorite-list">
            <article v-for="(favorite, index) in favorites" :key="listingId(favorite) || index" class="favorite-card"
                role="button" tabindex="0" :aria-label="`查看${listingTitle(favoriteListing(favorite))}的詳細資訊`"
                @click="openDetail(favorite)" @keydown.enter.prevent="openDetail(favorite)"
                @keydown.space.prevent="openDetail(favorite)">
                <!-- 票券圖示區：目前來源程式碼未提供票券照片欄位 -->
                <div class="favorite-images">
                    <div class="no-image" aria-label="票券圖示">
                        <Icon :icon="ticketIcon" class="ticket-icon" aria-hidden="true" />
                    </div>
                </div>

                <!-- 卡片文字內容 -->
                <div class="favorite-content">
                    <h3 class="favorite-title" :title="listingTitle(favoriteListing(favorite))">
                        {{ listingTitle(favoriteListing(favorite)) }}
                    </h3>

                    <p class="favorite-member">
                        刊登者：{{ listingUserName(favoriteListing(favorite)) }}
                    </p>

                    <p class="favorite-description">
                        {{ listingDescription(favoriteListing(favorite)) }}
                    </p>

                    <div class="favorite-ticket-details">
                        <p class="favorite-detail-item">
                            <Icon :icon="cinemaIcon" aria-hidden="true" />
                            <span class="favorite-detail-label">影城</span>
                            <span class="favorite-detail-value">
                                {{ listingCinemaName(favoriteListing(favorite)) }}
                            </span>
                        </p>

                        <p class="favorite-detail-item">
                            <Icon :icon="filmIcon" aria-hidden="true" />
                            <span class="favorite-detail-label">電影</span>
                            <span class="favorite-detail-value">
                                {{ listingMovieName(favoriteListing(favorite)) }}
                            </span>
                        </p>

                        <p class="favorite-detail-item">
                            <Icon :icon="calendarIcon" aria-hidden="true" />
                            <span class="favorite-detail-label">放映日期</span>
                            <span class="favorite-detail-value">
                                {{ listingShowtime(favoriteListing(favorite)) }}
                            </span>
                        </p>
                    </div>

                    <!-- 保留原本的取消收藏事件 -->
                    <button type="button" class="remove-favorite-button" :disabled="!listingId(favorite)"
                        @click.stop="removeFavorite(favorite)" @keydown.stop>
                        <Icon :icon="heartIcon" aria-hidden="true" />
                        取消收藏
                    </button>
                </div>
            </article>
        </div>

        <!-- 票券詳細資訊視窗 -->
        <Teleport to="body">
            <div v-if="selectedListing" class="detail-modal-overlay" @click.self="closeDetail">
                <section class="detail-modal" role="dialog" aria-modal="true" aria-labelledby="detail-modal-title">
                    <div class="detail-modal-card">
                        <!-- 標題及關閉按鈕 -->
                        <header class="detail-modal-header">
                            <div class="detail-modal-heading">
                                <Icon :icon="ticketIcon" aria-hidden="true" />

                                <h2 id="detail-modal-title" class="detail-modal-title">
                                    {{ listingTitle(selectedListing) }}
                                </h2>
                            </div>

                            <button type="button" class="detail-ticket-close" aria-label="關閉詳細資訊" title="關閉"
                                @click="closeDetail">
                                <Icon :icon="closeIcon" aria-hidden="true" />
                            </button>
                        </header>

                        <div class="detail-modal-content">
                            <!-- 刊登者：比照 ExchangeFavoriteSection.vue -->
                            <div class="detail-modal-publisher">
                                <Icon :icon="accountIcon" aria-hidden="true" />

                                <span class="detail-modal-label">
                                    刊登者
                                </span>

                                <button v-if="listingUserName(selectedListing) !== '未提供'" type="button"
                                    class="member-name-link" @click="goToMember(selectedListing)">
                                    {{ listingUserName(selectedListing) }}
                                </button>

                                <span v-else class="detail-modal-value">
                                    未提供
                                </span>
                            </div>

                            <!-- 刊登敘述 -->
                            <section class="detail-modal-section">
                                <h3>刊登敘述</h3>

                                <p class="detail-modal-description">
                                    {{ listingDescription(selectedListing) }}
                                </p>
                            </section>

                            <!-- 票券資訊 -->
                            <section class="detail-modal-section">
                                <h3>票券資訊</h3>

                                <dl class="detail-modal-details">
                                    <div class="detail-modal-detail-item">
                                        <dt>
                                            <Icon :icon="cinemaIcon" aria-hidden="true" />
                                            <span>影城</span>
                                        </dt>

                                        <dd>
                                            {{ listingCinemaName(selectedListing) }}
                                        </dd>
                                    </div>

                                    <div class="detail-modal-detail-item">
                                        <dt>
                                            <Icon :icon="filmIcon" aria-hidden="true" />
                                            <span>電影</span>
                                        </dt>

                                        <dd>
                                            {{ listingMovieName(selectedListing) }}
                                        </dd>
                                    </div>

                                    <div class="detail-modal-detail-item">
                                        <dt>
                                            <Icon :icon="calendarIcon" aria-hidden="true" />
                                            <span>放映日期</span>
                                        </dt>

                                        <dd>
                                            {{ listingShowtime(selectedListing) }}
                                        </dd>
                                    </div>
                                </dl>
                            </section>
                        </div>

                        <!-- 詳細視窗底部操作：保留取消收藏功能 -->
                        <footer class="detail-modal-footer">
                            <button type="button" class="detail-favorite-button" :disabled="!listingId(selectedListing)"
                                @click="removeFavoriteFromDetail">
                                <Icon :icon="heartIcon" aria-hidden="true" />
                                取消收藏
                            </button>
                        </footer>
                    </div>
                </section>
            </div>
        </Teleport>
    </section>
</template>

<style scoped>
.ticket-favorites {
    width: 100%;
}

.favorite-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

/* 收藏卡片：參考 ExchangeFavoriteSection.vue */
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
    display: flex;
    flex-wrap: wrap;
    align-content: flex-start;
    flex-shrink: 0;
    gap: 8px;
    width: 120px;
    height: 100%;
    overflow: hidden;
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

.ticket-icon {
    width: 42px;
    height: 42px;
}

/* 卡片文字區 */
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
    overflow-wrap: anywhere;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 1;
}

.favorite-member {
    flex-shrink: 0;
    overflow: hidden;
    margin: 0 0 4px;
    color: #4b5563;
    font-size: 14px;
    line-height: 1.4;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.favorite-description {
    display: -webkit-box;
    flex-shrink: 0;
    min-height: calc(14px * 1.5 * 2);
    overflow: hidden;
    margin: 0 0 5px;
    padding-left: 1em;
    color: #475569;
    font-size: 14px;
    line-height: 1.5;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
}

.favorite-ticket-details {
    display: flex;
    flex-direction: column;
    gap: 3px;
    min-width: 0;
    overflow: hidden;
}

.favorite-detail-item {
    display: grid;
    grid-template-columns: 16px auto minmax(0, 1fr);
    align-items: center;
    gap: 6px;
    min-width: 0;
    margin: 0;
    color: #475569;
    font-size: 13px;
    line-height: 1.4;
}

.favorite-detail-item :deep(svg) {
    width: 15px;
    height: 15px;
    flex-shrink: 0;
}

.favorite-detail-label {
    flex-shrink: 0;
    color: #64748b;
    font-weight: 600;
}

.favorite-detail-value {
    min-width: 0;
    overflow: hidden;
    color: #334155;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.remove-favorite-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    align-self: flex-end;
    flex-shrink: 0;
    gap: 6px;
    margin-top: auto;
    padding: 7px 12px;
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
    padding: 2rem 1rem;
    border: 1px dashed #cbd5e1;
    border-radius: 0.9rem;
    background: #f8fafc;
    color: #64748b;
    text-align: center;
}

/* 詳細資訊視窗：外框及排列參考 ExchangeFavoriteSection.vue */
.detail-modal-overlay {
    position: fixed;
    inset: 0;
    z-index: 2000;
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

.detail-modal-heading {
    display: flex;
    align-items: center;
    flex: 1;
    gap: 0.65rem;
    min-width: 0;
}

.detail-modal-heading :deep(svg) {
    flex-shrink: 0;
    width: 1.4rem;
    height: 1.4rem;
    color: #334155;
}

.detail-modal-title {
    min-width: 0;
    margin: 0;
    color: #334155;
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
    border: 1.5px solid #334155;
    border-radius: 0.7rem;
    background: #fff;
    color: #334155;
    cursor: pointer;
}

.detail-ticket-close :deep(svg) {
    width: 1.25rem;
    height: 1.25rem;
}

.detail-ticket-close:hover {
    background: #e5e7eb;
}

.detail-ticket-close:focus-visible {
    outline: 2px solid #334155;
    outline-offset: 3px;
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

/* 刊登者資訊 */
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
    flex-shrink: 0;
    color: #64748b;
    font-size: 0.92rem;
    font-weight: 650;
}

/* 刊登者名稱：參考 ExchangeFavoriteSection.vue */
.member-name-link {
    display: inline;
    min-width: 0;
    padding: 0;
    border: 0;
    background: transparent;
    color: #334155;
    font: inherit;
    font-weight: 800;
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

.detail-modal-value {
    min-width: 0;
    color: #334155;
    font-weight: 800;
    overflow-wrap: anywhere;
}

/* 票券敘述與票券資訊 */
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

.detail-modal-description,
.detail-modal-details {
    padding-left: 1em;
}

.detail-modal-description {
    margin: 0;
    color: #475569;
    font-size: 0.95rem;
    line-height: 1.9;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
}

/* 影城、電影、放映日期 */
.detail-modal-details {
    display: grid;
    gap: 0.9rem;
    margin: 0;
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

/* 詳細視窗底部操作 */
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

.detail-favorite-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.45rem;
    min-height: 42px;
    padding: 0.6rem 1rem;
    border: 1px solid #dc2626;
    border-radius: 6px;
    background: #fff;
    color: #dc2626;
    font-family: inherit;
    font-size: 0.88rem;
    font-weight: 750;
    line-height: 1.4;
    text-align: center;
    cursor: pointer;
}

.detail-favorite-button :deep(svg) {
    flex-shrink: 0;
    width: 1.1rem;
    height: 1.1rem;
}

.detail-favorite-button:hover:not(:disabled) {
    background: #dc2626;
    color: #fff;
}

.detail-favorite-button:disabled {
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

    .no-image {
        width: 100px;
        height: 100px;
    }

    .favorite-title {
        font-size: 16px;
    }

    .favorite-member,
    .favorite-description,
    .favorite-detail-item {
        font-size: 12px;
    }

    .favorite-detail-item {
        grid-template-columns: 14px auto minmax(0, 1fr);
        gap: 4px;
    }

    .favorite-detail-item :deep(svg) {
        width: 13px;
        height: 13px;
    }

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

    .detail-modal-title {
        font-size: 1rem;
    }

    .detail-modal-detail-item {
        grid-template-columns: 5.2rem minmax(0, 1fr);
        gap: 0.5rem;
    }

    .detail-modal-footer {
        justify-content: stretch;
        padding: 0.8rem;
    }

    .detail-favorite-button {
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
