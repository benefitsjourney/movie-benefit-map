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
.section-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    margin-bottom: 1.2rem;
}

.section-heading h2 {
    margin: 0;
    font-size: 1.35rem;
    font-weight: 750;
}

.section-heading p {
    margin: .4rem 0 0;
    color: #64748b;
}

.count-pill {
    border-radius: 999px;
    background: #f1f5f9;
    color: #475569;
    padding: .35rem .75rem;
    white-space: nowrap;
    font-size: .85rem;
}

.search-panel {
    display: grid;
    grid-template-columns: minmax(0, 2fr) minmax(180px, 1fr) auto;
    align-items: end;
    gap: .8rem;
    margin-bottom: 1.2rem;
    padding: 1rem;
    border: 1px solid #e2e8f0;
    border-radius: .8rem;
    background: #f8fafc;
}

.search-field {
    min-width: 0;
}

.search-field .form-label {
    display: block;
}

.clear-button {
    min-height: 38px;
}

.search-hint {
    grid-column: 1 / -1;
    margin: 0;
    color: #64748b;
    font-size: .85rem;
}

.listing-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(100%, 270px), 1fr));
    gap: 1rem;
}

.listing-card {
    position: relative;
    min-width: 0;
    border: 1px solid #e2e8f0;
    border-radius: 1rem;
    background: #fff;
    padding: 1.15rem;
    box-shadow: 0 5px 18px rgb(15 23 42 / 4%);
}

.heart-button {
    position: absolute;
    top: .75rem;
    right: .8rem;
    border: 0;
    background: #fff;
    color: #c2415d;
    font-size: 1.8rem;
    line-height: 1;
    border-radius: 50%;
    width: 2.5rem;
    height: 2.5rem;
}

.heart-button:hover {
    background: #fff1f2;
}

.card-topline {
    display: flex;
    align-items: center;
    gap: .5rem;
    padding-right: 2.4rem;
    margin-bottom: .8rem;
}

.type-label,
.status-label {
    display: inline-flex;
    align-items: center;
    border-radius: 999px;
    padding: .25rem .55rem;
    font-size: .76rem;
    font-weight: 700;
}

.ticket-label {
    background: #f3e8ff;
    color: #6b21a8;
}

.status-label {
    background: #f1f5f9;
    color: #475569;
}

.listing-card h3 {
    font-size: 1.15rem;
    font-weight: 750;
    padding-right: 1.5rem;
    overflow-wrap: anywhere;
}

.listing-description {
    color: #475569;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
    min-height: 3rem;
}

.ticket-details {
    display: grid;
    gap: .55rem;
    margin: 1rem 0 0;
}

.ticket-details div {
    display: grid;
    grid-template-columns: 5.2rem minmax(0, 1fr);
    gap: .5rem;
}

.ticket-details dt {
    color: #64748b;
    font-weight: 500;
}

.ticket-details dd {
    margin: 0;
    font-weight: 650;
    overflow-wrap: anywhere;
}

.empty-state {
    padding: 2rem 1rem;
    text-align: center;
    border: 1px dashed #cbd5e1;
    border-radius: .9rem;
    color: #64748b;
    background: #f8fafc;
}

.pagination {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-wrap: wrap;
    gap: .8rem;
    margin-top: 1.5rem;
}

.page-info {
    color: #475569;
    font-size: .9rem;
}

@media (max-width: 600px) {
    .search-panel {
        grid-template-columns: minmax(0, 1fr);
    }

    .search-hint {
        grid-column: auto;
    }
}
</style>