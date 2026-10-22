<!-- ========== components/exchange/ExchangeListingSection.vue ========== -->
<!-- 負責公開的交換刊登卡片及收藏按鈕。不直接呼叫 API，而是透過 emit 通知主頁進行收藏操作。 -->
<script setup>
import { computed, ref, watch } from "vue";

const props = defineProps({
    listings: {
        type: Array,
        default: () => [],
    },
    favorites: {
        type: Array,
        default: () => [],
    },
    isLoggedIn: {
        type: Boolean,
        default: false,
    },
    loading: {
        type: Boolean,
        default: false,
    },
});

const emit = defineEmits(["toggle-favorite"]);

// 搜尋及分頁
const keyword = ref("");
const currentPage = ref(1);
const pageSize = 8;

function listingId(item) {
    return (
        item?.exchangeListingId ??
        item?.exchange_listing_id ??
        item?.id ??
        ""
    );
}

function favoriteListing(favorite) {
    return (
        favorite?.exchangeListing ??
        favorite?.exchangeListingBean ??
        favorite?.listing ??
        favorite
    );
}

function isFavorited(id) {
    return props.favorites.some((favorite) => {
        const listing = favoriteListing(favorite);

        const favoriteId =
            listing?.exchangeListingId ??
            listing?.exchange_listing_id ??
            listing?.id ??
            "";

        return String(favoriteId) === String(id);
    });
}

function formatDate(value) {
    if (!value) return "未提供";

    const text = String(value);

    return text.includes("T")
        ? text.split("T")[0]
        : text;
}

// 整理可能存在的刊登文字欄位，供搜尋使用
function searchableText(item) {
    return [
        item?.title,
        item?.description,
        item?.usersName,
        item?.userName,
        item?.username,
        item?.memberName,
        item?.benefitName,
        item?.benefitType,
        item?.useBenefitName,
    ]
        .filter((value) => value !== null && value !== undefined)
        .join(" ")
        .toLocaleLowerCase();
}

// 關鍵字搜尋
const filteredListings = computed(() => {
    const search = keyword.value.trim().toLocaleLowerCase();

    if (!search) {
        return props.listings;
    }

    return props.listings.filter((item) =>
        searchableText(item).includes(search),
    );
});

// 搜尋條件改變時回到第一頁
watch(keyword, () => {
    currentPage.value = 1;
});

watch(
    () => props.listings,
    () => {
        currentPage.value = 1;
    },
);

// 分頁總數
const totalPages = computed(() =>
    Math.max(1, Math.ceil(filteredListings.value.length / pageSize)),
);

// 避免資料減少後停留在不存在的頁數
watch(totalPages, (total) => {
    if (currentPage.value > total) {
        currentPage.value = total;
    }
});

// 目前頁面顯示的資料
const paginatedListings = computed(() => {
    const start = (currentPage.value - 1) * pageSize;

    return filteredListings.value.slice(
        start,
        start + pageSize,
    );
});

const listingCount = computed(
    () => filteredListings.value.length,
);

function changePage(page) {
    if (page < 1 || page > totalPages.value) return;

    currentPage.value = page;
}

function clearSearch() {
    keyword.value = "";
    currentPage.value = 1;
}
</script>

<template>
    <section class="listing-section">
        <!-- 區塊標題 -->
        <div class="section-heading">
            <div class="heading-content">
                <span class="section-eyebrow">
                    EXCHANGE COLLECTION
                </span>

                <h2>交換刊登</h2>

                <p>
                    尋找喜愛的特典，探索更多交換機會。
                </p>
            </div>

            <span class="count-pill">
                <span class="count-number">{{ listingCount }}</span>
                <span>筆刊登</span>
            </span>
        </div>

        <!-- 關鍵字搜尋 -->
        <div class="search-panel">
            <label for="exchange-keyword" class="form-label">
                搜尋特典刊登
            </label>

            <div class="search-controls">
                <div class="search-input-wrap">
                    <span class="search-icon" aria-hidden="true">
                        ⌕
                    </span>

                    <input id="exchange-keyword" v-model="keyword" type="search" class="form-control"
                        placeholder="輸入標題、敘述或相關名稱" />
                </div>

                <button type="button" class="clear-button" :disabled="!keyword" @click="clearSearch">
                    清除搜尋
                </button>
            </div>

            <p class="search-hint">
                共找到
                <strong>{{ listingCount }}</strong>
                筆符合條件的刊登
            </p>
        </div>

        <!-- 載入中 -->
        <div v-if="loading" class="empty-state loading-state" role="status" aria-live="polite">
            <span class="loading-spinner" aria-hidden="true"></span>
            <span>正在載入交換刊登……</span>
        </div>

        <!-- 沒有搜尋結果 -->
        <div v-else-if="!filteredListings.length" class="empty-state">
            <span class="empty-icon" aria-hidden="true">◇</span>

            <h3>
                {{ keyword.trim()
                    ? "找不到符合條件的刊登"
                    : "目前沒有公開的交換刊登" }}
            </h3>

            <p>
                {{ keyword.trim()
                    ? "試試其他關鍵字，或清除搜尋條件。"
                    : "目前還沒有可瀏覽的交換刊登，請稍後再來看看。" }}
            </p>

            <button v-if="keyword.trim()" type="button" class="clear-search-button" @click="clearSearch">
                清除搜尋條件
            </button>
        </div>

        <!-- 交換刊登卡片 -->
        <div v-else class="listing-grid">
            <article v-for="(item, index) in paginatedListings" :key="listingId(item) || index" class="listing-card">
                <div class="card-topline">
                    <span class="type-label exchange-label">
                        <span aria-hidden="true">✦</span>
                        交換特典
                    </span>

                    <span class="status-label">
                        {{ item.status || "狀態未提供" }}
                    </span>
                </div>

                <button class="heart-button" :class="{ favorited: isFavorited(listingId(item)) }" type="button"
                    :aria-label="isFavorited(listingId(item))
                        ? '取消收藏'
                        : '加入收藏'" :aria-pressed="isFavorited(listingId(item))" :title="isLoggedIn
                            ? (isFavorited(listingId(item))
                                ? '取消收藏'
                                : '加入收藏')
                            : '登入後才能收藏'" @click="emit('toggle-favorite', item)">
                    {{ isFavorited(listingId(item)) ? "♥" : "♡" }}
                </button>

                <h3 class="listing-title">
                    {{ item.title || "未命名刊登" }}
                </h3>

                <p class="listing-description">
                    {{ item.description || "刊登者尚未提供敘述。" }}
                </p>

                <div class="listing-meta">
                    <span class="meta-label">
                        <span aria-hidden="true">◷</span>
                        刊登日期
                    </span>

                    <strong>
                        {{ formatDate(item.createdAt || item.created_at) }}
                    </strong>
                </div>
            </article>
        </div>

        <!-- 分頁 -->
        <nav v-if="listingCount > pageSize" class="pagination" aria-label="交換刊登分頁">
            <button type="button" class="page-button" :disabled="currentPage === 1"
                @click="changePage(currentPage - 1)">
                <span aria-hidden="true">‹</span>
                上一頁
            </button>

            <span class="page-info">
                <strong>{{ currentPage }}</strong>
                <span class="page-divider">/</span>
                {{ totalPages }}
                <span class="page-unit">頁</span>
            </span>

            <button type="button" class="page-button" :disabled="currentPage === totalPages"
                @click="changePage(currentPage + 1)">
                下一頁
                <span aria-hidden="true">›</span>
            </button>
        </nav>
    </section>
</template>

<style scoped>
.listing-section {
    --exchange-accent: var(--exchange-primary, #6476c8);
    --exchange-accent-soft: #eef2ff;
    --exchange-text: #253047;
    --exchange-muted: #718096;
    --exchange-border: #e5eaf2;

    min-width: 0;
    color: var(--exchange-text);
}

/* 區塊標題 */
.section-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    margin-bottom: 1.35rem;
}

.heading-content {
    min-width: 0;
}

.section-eyebrow {
    display: inline-block;
    margin-bottom: 0.4rem;
    color: var(--exchange-accent);
    font-size: 0.72rem;
    font-weight: 800;
    letter-spacing: 0.12em;
}

.section-heading h2 {
    margin: 0;
    color: var(--exchange-text);
    font-size: clamp(1.3rem, 2.5vw, 1.65rem);
    font-weight: 800;
    letter-spacing: 0.025em;
}

.section-heading p {
    margin: 0.45rem 0 0;
    color: var(--exchange-muted);
    font-size: 0.92rem;
    line-height: 1.7;
}

.count-pill {
    display: inline-flex;
    align-items: baseline;
    justify-content: center;
    gap: 0.35rem;
    flex-shrink: 0;
    padding: 0.5rem 0.9rem;
    border: 1px solid #dfe5fb;
    border-radius: 999px;
    background: var(--exchange-accent-soft);
    color: #5364ad;
    font-size: 0.8rem;
    font-weight: 700;
    white-space: nowrap;
}

.count-number {
    font-size: 1.05rem;
    font-weight: 850;
    font-variant-numeric: tabular-nums;
}

/* 搜尋區 */
.search-panel {
    margin-bottom: 1.5rem;
    padding: 1.1rem 1.2rem;
    border: 1px solid var(--exchange-border);
    border-radius: 1rem;
    background: #f8faff;
}

.form-label {
    display: block;
    margin-bottom: 0.6rem;
    color: #374151;
    font-size: 0.88rem;
    font-weight: 750;
}

.search-controls {
    display: flex;
    align-items: stretch;
    gap: 0.65rem;
}

.search-input-wrap {
    position: relative;
    flex: 1;
    min-width: 0;
}

.search-icon {
    position: absolute;
    top: 50%;
    left: 0.85rem;
    z-index: 1;
    color: #8792a7;
    font-size: 1.35rem;
    line-height: 1;
    transform: translateY(-53%);
    pointer-events: none;
}

.search-input-wrap input {
    width: 100%;
    min-width: 0;
    min-height: 2.8rem;
    padding: 0.65rem 0.85rem 0.65rem 2.5rem;
    border: 1px solid #dce3ef;
    border-radius: 0.7rem;
    background: #fff;
    color: var(--exchange-text);
    font-size: 0.9rem;
    box-shadow: none;
}

.search-input-wrap input:focus {
    border-color: var(--exchange-accent);
    outline: 3px solid rgb(100 118 200 / 12%);
}

.clear-button,
.clear-search-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 2.8rem;
    padding: 0.6rem 1rem;
    border: 1px solid #dce3ef;
    border-radius: 0.7rem;
    background: #fff;
    color: #526078;
    font-size: 0.85rem;
    font-weight: 700;
    white-space: nowrap;
    cursor: pointer;
    transition:
        background 0.18s ease,
        border-color 0.18s ease,
        color 0.18s ease;
}

.clear-button:hover:not(:disabled),
.clear-search-button:hover {
    border-color: var(--exchange-accent);
    background: var(--exchange-accent-soft);
    color: #5364ad;
}

.clear-button:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.search-hint {
    margin: 0.7rem 0 0;
    color: var(--exchange-muted);
    font-size: 0.82rem;
}

.search-hint strong {
    color: var(--exchange-accent);
    font-weight: 800;
}

/* 刊登卡片排列 */
.listing-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(100%, 260px), 1fr));
    gap: 1.1rem;
    align-items: stretch;
}

.listing-card {
    position: relative;
    display: flex;
    flex-direction: column;
    min-width: 0;
    padding: 1.2rem;
    border: 1px solid var(--exchange-border);
    border-radius: 1rem;
    background: #fff;
    box-shadow: 0 4px 16px rgb(31 45 75 / 4%);
    transition:
        transform 0.2s ease,
        box-shadow 0.2s ease,
        border-color 0.2s ease;
}

.listing-card:hover {
    transform: translateY(-3px);
    border-color: #cfd7f4;
    box-shadow: 0 10px 26px rgb(31 45 75 / 8%);
}

/* 卡片標籤 */
.card-topline {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.45rem;
    padding-right: 2.3rem;
    margin-bottom: 1rem;
}

.type-label,
.status-label {
    display: inline-flex;
    align-items: center;
    gap: 0.3rem;
    max-width: 100%;
    border-radius: 999px;
    padding: 0.3rem 0.6rem;
    font-size: 0.73rem;
    font-weight: 750;
    line-height: 1.4;
    overflow-wrap: anywhere;
}

.exchange-label {
    background: #e9f3ff;
    color: #315f96;
}

.status-label {
    background: #f2f4f8;
    color: #69758a;
}

/* 收藏按鈕 */
.heart-button {
    position: absolute;
    top: 0.85rem;
    right: 0.85rem;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 2.35rem;
    height: 2.35rem;
    padding: 0;
    border: 1px solid transparent;
    border-radius: 50%;
    background: #f8f9fc;
    color: #8a95a8;
    font-size: 1.55rem;
    line-height: 1;
    cursor: pointer;
    transition:
        transform 0.18s ease,
        background 0.18s ease,
        color 0.18s ease;
}

.heart-button:hover {
    transform: scale(1.08);
    background: #fff0f3;
    color: #d14e70;
}

.heart-button.favorited {
    background: #fff0f3;
    color: #d14e70;
}

.heart-button:focus-visible,
.page-button:focus-visible,
.clear-button:focus-visible,
.clear-search-button:focus-visible {
    outline: 3px solid rgb(100 118 200 / 35%);
    outline-offset: 2px;
}

/* 卡片文字 */
.listing-title {
    margin: 0 0 0.7rem;
    padding-right: 1.6rem;
    color: var(--exchange-text);
    font-size: 1.08rem;
    font-weight: 800;
    line-height: 1.55;
    overflow-wrap: anywhere;
}

.listing-description {
    flex: 1;
    min-height: 3.2rem;
    margin: 0 0 1.1rem;
    color: #69758a;
    font-size: 0.9rem;
    line-height: 1.8;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
}

/* 刊登日期 */
.listing-meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 0.5rem;
    padding-top: 0.85rem;
    border-top: 1px solid #edf0f5;
    color: var(--exchange-muted);
    font-size: 0.8rem;
}

.meta-label {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
}

.listing-meta strong {
    color: #46536a;
    font-size: 0.82rem;
    font-weight: 700;
    overflow-wrap: anywhere;
}

/* 空清單與載入狀態 */
.empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 0.55rem;
    min-height: 12rem;
    padding: 2rem 1rem;
    border: 1px dashed #d5ddeb;
    border-radius: 1rem;
    background: #f9faff;
    color: var(--exchange-muted);
    text-align: center;
}

.empty-icon {
    color: #a0aac0;
    font-size: 2rem;
    line-height: 1.2;
}

.empty-state h3 {
    margin: 0;
    color: #46536a;
    font-size: 1rem;
    font-weight: 750;
}

.empty-state p {
    max-width: 28rem;
    margin: 0;
    font-size: 0.88rem;
    line-height: 1.7;
}

.clear-search-button {
    min-height: auto;
    margin-top: 0.45rem;
    border-color: #dfe5fb;
    background: #fff;
    color: #5364ad;
}

.loading-state {
    flex-direction: row;
    min-height: 8rem;
    border-style: solid;
}

.loading-spinner {
    width: 1.15rem;
    height: 1.15rem;
    border: 2px solid #d9def3;
    border-top-color: var(--exchange-accent);
    border-radius: 50%;
    animation: exchange-spin 0.8s linear infinite;
}

@keyframes exchange-spin {
    to {
        transform: rotate(360deg);
    }
}

/* 分頁 */
.pagination {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-wrap: wrap;
    gap: 1rem;
    margin-top: 1.6rem;
}

.page-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.4rem;
    min-height: 2.55rem;
    padding: 0.55rem 0.9rem;
    border: 1px solid #dce3ef;
    border-radius: 0.7rem;
    background: #fff;
    color: #526078;
    font-size: 0.85rem;
    font-weight: 700;
    cursor: pointer;
    transition:
        border-color 0.18s ease,
        background 0.18s ease,
        color 0.18s ease;
}

.page-button:hover:not(:disabled) {
    border-color: var(--exchange-accent);
    background: var(--exchange-accent-soft);
    color: #5364ad;
}

.page-button:disabled {
    background: #f7f8fb;
    color: #aab2c0;
    cursor: not-allowed;
}

.page-info {
    display: inline-flex;
    align-items: baseline;
    gap: 0.35rem;
    color: #647087;
    font-size: 0.88rem;
    font-variant-numeric: tabular-nums;
}

.page-info strong {
    color: var(--exchange-accent);
    font-size: 1.05rem;
    font-weight: 850;
}

.page-divider {
    color: #aab2c0;
}

.page-unit {
    margin-left: 0.1rem;
}

/* 手機版 */
@media (max-width: 600px) {
    .section-heading {
        align-items: flex-start;
    }

    .section-heading p {
        font-size: 0.85rem;
    }

    .count-pill {
        padding: 0.4rem 0.65rem;
    }

    .search-panel {
        padding: 0.9rem;
    }

    .search-controls {
        flex-direction: column;
    }

    .clear-button {
        width: 100%;
    }

    .listing-grid {
        grid-template-columns: minmax(0, 1fr);
        gap: 0.85rem;
    }

    .listing-card {
        padding: 1rem;
    }

    .pagination {
        gap: 0.6rem;
    }

    .page-button {
        padding: 0.5rem 0.7rem;
    }
}

/* 尊重使用者的減少動態效果設定 */
@media (prefers-reduced-motion: reduce) {

    .listing-card,
    .heart-button,
    .clear-button,
    .clear-search-button,
    .page-button {
        transition: none;
    }

    .listing-card:hover,
    .heart-button:hover {
        transform: none;
    }

    .loading-spinner {
        animation: none;
    }
}
</style>