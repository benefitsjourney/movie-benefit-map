<!-- ========== components/exchange/TicketListingSection.vue ========== -->
<!-- 負責公開票券刊登、電影及影城名稱顯示，以及票券收藏按鈕。 -->
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
    cinemaName: {
        type: Function,
        required: true,
    },
    movieName: {
        type: Function,
        required: true,
    },
});

const emit = defineEmits(["toggle-favorite"]);

// 搜尋及分頁
const keyword = ref("");
const showDate = ref("");
const currentPage = ref(1);
const pageSize = 8;

function listingId(item) {
    return (
        item?.ticketListingId ??
        item?.ticket_listing_id ??
        item?.id ??
        ""
    );
}

function favoriteListing(favorite) {
    return (
        favorite?.ticketListing ??
        favorite?.ticketListingBean ??
        favorite?.listing ??
        favorite
    );
}

function isFavorited(id) {
    return props.favorites.some((favorite) => {
        const listing = favoriteListing(favorite);

        const favoriteId =
            listing?.ticketListingId ??
            listing?.ticket_listing_id ??
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

function searchableText(item) {
    return [
        item?.title,
        item?.description,
        item?.usersName,
        item?.userName,
        item?.username,
        item?.memberName,
        props.cinemaName(item),
        props.movieName(item),
    ]
        .filter((value) => value !== null && value !== undefined)
        .join(" ")
        .toLocaleLowerCase();
}

function showDateValue(item) {
    return String(
        item?.ticketShowtime ??
        item?.ticket_showtime ??
        "",
    ).slice(0, 10);
}

// 關鍵字與放映日期同時符合才會顯示
const filteredListings = computed(() => {
    const search = keyword.value.trim().toLocaleLowerCase();

    return props.listings.filter((item) => {
        const matchesKeyword =
            !search || searchableText(item).includes(search);

        const matchesDate =
            !showDate.value ||
            showDateValue(item) === showDate.value;

        return matchesKeyword && matchesDate;
    });
});

watch([keyword, showDate], () => {
    currentPage.value = 1;
});

watch(
    () => props.listings,
    () => {
        currentPage.value = 1;
    },
);

const listingCount = computed(
    () => filteredListings.value.length,
);

const totalPages = computed(() =>
    Math.max(1, Math.ceil(listingCount.value / pageSize)),
);

watch(totalPages, (total) => {
    if (currentPage.value > total) {
        currentPage.value = total;
    }
});

const paginatedListings = computed(() => {
    const start = (currentPage.value - 1) * pageSize;

    return filteredListings.value.slice(
        start,
        start + pageSize,
    );
});

function changePage(page) {
    if (page < 1 || page > totalPages.value) return;

    currentPage.value = page;
}

function clearSearch() {
    keyword.value = "";
    showDate.value = "";
    currentPage.value = 1;
}
</script>

<template>
    <section class="listing-section">
        <div class="section-heading">
            <div>
                <h2>票券刊登</h2>
                <p>顯示標題、敘述、影城名稱、電影名稱與放映日期。</p>
            </div>

            <span class="count-pill">
                {{ listingCount }} 筆
            </span>
        </div>

        <!-- 關鍵字及日期搜尋 -->
        <div class="search-panel">
            <div class="search-field">
                <label for="ticket-keyword" class="form-label">
                    搜尋票券刊登
                </label>

                <input id="ticket-keyword" v-model="keyword" type="search" class="form-control"
                    placeholder="輸入標題、敘述、電影或影城名稱" />
            </div>

            <div class="search-field">
                <label for="ticket-show-date" class="form-label">
                    放映日期
                </label>

                <input id="ticket-show-date" v-model="showDate" type="date" class="form-control" />
            </div>

            <button type="button" class="btn btn-outline-secondary clear-button" @click="clearSearch">
                清除搜尋
            </button>

            <p class="search-hint">
                搜尋結果：{{ listingCount }} 筆
            </p>
        </div>

        <div v-if="!filteredListings.length && !loading" class="empty-state">
            {{ keyword.trim() || showDate
                ? "找不到符合搜尋條件的票券刊登。"
                : "目前沒有公開的票券刊登。" }}
        </div>

        <div v-if="loading && !listings.length" class="empty-state">
            正在載入票券刊登……
        </div>

        <div class="listing-grid">
            <article v-for="item in paginatedListings" :key="listingId(item)" class="listing-card">
                <button class="heart-button" type="button" :aria-label="isFavorited(listingId(item))
                    ? '取消收藏'
                    : '加入收藏'" :title="isLoggedIn
                        ? (isFavorited(listingId(item))
                            ? '取消收藏'
                            : '加入收藏')
                        : '登入後才能收藏'" @click="emit('toggle-favorite', item)">
                    {{ isFavorited(listingId(item)) ? "♥" : "♡" }}
                </button>

                <div class="card-topline">
                    <span class="type-label ticket-label">
                        票券交換
                    </span>

                    <span class="status-label">
                        {{ item.status || "狀態未提供" }}
                    </span>
                </div>

                <h3>{{ item.title || "未命名刊登" }}</h3>

                <p class="listing-description">
                    {{ item.description || "刊登者尚未提供敘述。" }}
                </p>

                <dl class="ticket-details">
                    <div>
                        <dt>影城</dt>
                        <dd>{{ cinemaName(item) }}</dd>
                    </div>

                    <div>
                        <dt>電影</dt>
                        <dd>{{ movieName(item) }}</dd>
                    </div>

                    <div>
                        <dt>放映日期</dt>
                        <dd>
                            {{ formatDate(item.ticketShowtime || item.ticket_showtime) }}
                        </dd>
                    </div>
                </dl>
            </article>
        </div>

        <!-- 分頁 -->
        <nav v-if="listingCount > pageSize" class="pagination" aria-label="票券刊登分頁">
            <button type="button" class="btn btn-outline-secondary" :disabled="currentPage === 1"
                @click="changePage(currentPage - 1)">
                上一頁
            </button>

            <span class="page-info">
                第 {{ currentPage }} / {{ totalPages }} 頁
            </span>

            <button type="button" class="btn btn-outline-secondary" :disabled="currentPage === totalPages"
                @click="changePage(currentPage + 1)">
                下一頁
            </button>
        </nav>
    </section>
</template>

<style scoped>
/* ========== 票券刊登區域 ========== */
.listing-section {
    width: 100%;
    min-width: 0;
    color: #1e293b;
}

/* ========== 區塊標題 ========== */
.section-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    margin-bottom: 1.4rem;
}

.section-heading h2 {
    margin: 0;
    color: #1e293b;
    font-size: 1.4rem;
    font-weight: 800;
    letter-spacing: .02em;
}

.section-heading p {
    margin: .45rem 0 0;
    color: #64748b;
    font-size: .92rem;
    line-height: 1.7;
}

.count-pill {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    min-height: 2rem;
    padding: .4rem .85rem;
    border: 1px solid #dbeafe;
    border-radius: 999px;
    background: #eff6ff;
    color: #1d4ed8;
    font-size: .85rem;
    font-weight: 750;
    white-space: nowrap;
}

/* ========== 搜尋區域 ========== */
.search-panel {
    display: grid;
    grid-template-columns: minmax(0, 2fr) minmax(180px, 1fr) auto;
    align-items: end;
    gap: 1rem;
    margin-bottom: 1.5rem;
    padding: 1.15rem;
    border: 1px solid #e2e8f0;
    border-radius: 1rem;
    background: #f8fafc;
}

.search-field {
    min-width: 0;
}

.search-field .form-label {
    display: block;
    margin-bottom: .5rem;
    color: #334155;
    font-size: .88rem;
    font-weight: 700;
}

.search-field .form-control {
    display: block;
    width: 100%;
    min-height: 42px;
    padding: .65rem .8rem;
    border: 1px solid #cbd5e1;
    border-radius: .65rem;
    background: #fff;
    color: #1e293b;
    font-size: .92rem;
    box-shadow: none;
    transition:
        border-color .2s ease,
        box-shadow .2s ease;
}

.search-field .form-control::placeholder {
    color: #94a3b8;
}

.search-field .form-control:hover {
    border-color: #94a3b8;
}

.search-field .form-control:focus {
    border-color: #647fe5;
    outline: none;
    box-shadow: 0 0 0 3px rgb(100 127 229 / 15%);
}

.clear-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 42px;
    padding: .65rem 1rem;
    border: 1px solid #cbd5e1;
    border-radius: .65rem;
    background: #fff;
    color: #475569;
    font-size: .9rem;
    font-weight: 650;
    white-space: nowrap;
    transition:
        background-color .2s ease,
        border-color .2s ease,
        color .2s ease;
}

.clear-button:hover {
    border-color: #94a3b8;
    background: #e2e8f0;
    color: #1e293b;
}

.search-hint {
    grid-column: 1 / -1;
    margin: -.15rem 0 0;
    color: #64748b;
    font-size: .84rem;
}

/* ========== 票券卡片排列 ========== */
.listing-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit,
            minmax(min(100%, 270px), 1fr));
    align-items: stretch;
    gap: 1.15rem;
}

.listing-card {
    position: relative;
    display: flex;
    flex-direction: column;
    min-width: 0;
    padding: 1.25rem;
    overflow: hidden;
    border: 1px solid #e2e8f0;
    border-radius: 1rem;
    background: #fff;
    box-shadow: 0 4px 14px rgb(15 23 42 / 4%);
    transition:
        transform .2s ease,
        border-color .2s ease,
        box-shadow .2s ease;
}

.listing-card:hover {
    transform: translateY(-3px);
    border-color: #cbd5e1;
    box-shadow: 0 10px 26px rgb(15 23 42 / 8%);
}

/* ========== 收藏按鈕 ========== */
.heart-button {
    position: absolute;
    top: .85rem;
    right: .85rem;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 2.45rem;
    height: 2.45rem;
    padding: 0;
    border: 1px solid #f1f5f9;
    border-radius: 50%;
    background: #f8fafc;
    color: #c2415d;
    font-size: 1.65rem;
    line-height: 1;
    cursor: pointer;
    transition:
        transform .2s ease,
        background-color .2s ease,
        border-color .2s ease;
}

.heart-button:hover {
    transform: scale(1.07);
    border-color: #fecdd3;
    background: #fff1f2;
}

.heart-button:focus-visible {
    outline: 3px solid rgb(194 65 93 / 35%);
    outline-offset: 2px;
}

.heart-button:active {
    transform: scale(.95);
}

/* ========== 卡片類型與狀態 ========== */
.card-topline {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: .5rem;
    min-height: 2rem;
    padding-right: 2.4rem;
    margin-bottom: .85rem;
}

.type-label,
.status-label {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 1.65rem;
    padding: .25rem .6rem;
    border-radius: 999px;
    font-size: .76rem;
    font-weight: 750;
    line-height: 1.4;
}

.ticket-label {
    background: #f3e8ff;
    color: #6b21a8;
}

.status-label {
    background: #f1f5f9;
    color: #475569;
    overflow-wrap: anywhere;
}

/* ========== 卡片標題與敘述 ========== */
.listing-card h3 {
    margin: 0 0 .7rem;
    padding-right: 1.4rem;
    color: #0f172a;
    font-size: 1.12rem;
    font-weight: 800;
    line-height: 1.6;
    overflow-wrap: anywhere;
}

.listing-description {
    margin: 0;
    min-height: 3.2rem;
    color: #475569;
    font-size: .92rem;
    line-height: 1.8;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
}

/* ========== 影城、電影與放映日期 ========== */
.ticket-details {
    display: grid;
    gap: .65rem;
    margin: 1.1rem 0 0;
    padding-top: 1rem;
    border-top: 1px solid #f1f5f9;
}

.ticket-details div {
    display: grid;
    grid-template-columns: 5rem minmax(0, 1fr);
    align-items: start;
    gap: .6rem;
}

.ticket-details dt {
    margin: 0;
    color: #64748b;
    font-size: .86rem;
    font-weight: 600;
}

.ticket-details dd {
    margin: 0;
    color: #334155;
    font-size: .9rem;
    font-weight: 700;
    line-height: 1.6;
    overflow-wrap: anywhere;
}

/* ========== 空白、搜尋無結果及載入狀態 ========== */
.empty-state {
    padding: 2rem 1rem;
    margin-bottom: 1rem;
    border: 1px dashed #cbd5e1;
    border-radius: 1rem;
    background: #f8fafc;
    color: #64748b;
    font-size: .95rem;
    line-height: 1.8;
    text-align: center;
}

/* ========== 分頁 ========== */
.pagination {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-wrap: wrap;
    gap: .8rem;
    margin-top: 1.6rem;
}

.pagination .btn {
    min-height: 38px;
    padding: .5rem .9rem;
    border-radius: .65rem;
    font-size: .88rem;
    font-weight: 650;
    transition:
        background-color .2s ease,
        border-color .2s ease;
}

.pagination .btn:disabled {
    opacity: .45;
    cursor: not-allowed;
}

.page-info {
    color: #475569;
    font-size: .9rem;
    font-weight: 650;
    white-space: nowrap;
}

/* ========== 鍵盤操作與無障礙 ========== */
.clear-button:focus-visible,
.pagination .btn:focus-visible {
    outline: 3px solid rgb(100 127 229 / 35%);
    outline-offset: 2px;
}

/* ========== 平板與手機 ========== */
@media (max-width: 768px) {
    .search-panel {
        grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    }

    .clear-button {
        width: 100%;
    }

    .search-hint {
        grid-column: 1 / -1;
    }

    .listing-grid {
        grid-template-columns: repeat(auto-fit,
                minmax(min(100%, 250px), 1fr));
    }
}

@media (max-width: 600px) {
    .section-heading {
        align-items: flex-start;
        gap: .65rem;
    }

    .section-heading h2 {
        font-size: 1.25rem;
    }

    .section-heading p {
        font-size: .85rem;
    }

    .count-pill {
        padding: .35rem .65rem;
        font-size: .8rem;
    }

    .search-panel {
        grid-template-columns: minmax(0, 1fr);
        gap: .85rem;
        padding: .9rem;
    }

    .search-hint {
        grid-column: auto;
    }

    .listing-grid {
        grid-template-columns: minmax(0, 1fr);
        gap: .9rem;
    }

    .listing-card {
        padding: 1rem;
        border-radius: .85rem;
    }

    .listing-card h3 {
        font-size: 1.05rem;
    }

    .listing-description {
        min-height: 0;
        font-size: .88rem;
    }

    .ticket-details div {
        grid-template-columns: 4.7rem minmax(0, 1fr);
        gap: .45rem;
    }

    .pagination {
        gap: .55rem;
    }

    .pagination .btn {
        padding: .45rem .7rem;
    }

    .page-info {
        font-size: .84rem;
    }
}

/* ========== 尊重使用者的減少動態效果設定 ========== */
@media (prefers-reduced-motion: reduce) {

    .listing-card,
    .heart-button,
    .clear-button,
    .search-field .form-control,
    .pagination .btn {
        transition: none;
    }

    .listing-card:hover,
    .heart-button:hover,
    .heart-button:active {
        transform: none;
    }
}
</style>