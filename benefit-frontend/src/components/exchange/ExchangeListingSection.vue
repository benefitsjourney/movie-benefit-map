<!-- ========== components/exchange/ExchangeListingSection.vue ========== -->
<!-- 負責公開的交換刊登卡片及收藏按鈕。它不直接呼叫 API，而是透過 emit 通知主頁進行收藏操作。 -->
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

// 將可能存在的刊登文字欄位整理成搜尋文字
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

// 避免資料數量減少後停留在不存在的頁數
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
        <div class="section-heading">
            <div>
                <h2>交換刊登</h2>
                <p>公開顯示目前可瀏覽的交換刊登。</p>
            </div>

            <span class="count-pill">
                {{ listingCount }} 筆
            </span>
        </div>

        <!-- 關鍵字搜尋 -->
        <div class="search-panel">
            <label for="exchange-keyword" class="form-label">
                搜尋特典刊登
            </label>

            <div class="search-controls">
                <input id="exchange-keyword" v-model="keyword" type="search" class="form-control"
                    placeholder="輸入標題、敘述或相關名稱" />

                <button type="button" class="btn btn-outline-secondary" @click="clearSearch">
                    清除搜尋
                </button>
            </div>

            <p class="search-hint">
                搜尋結果：{{ listingCount }} 筆
            </p>
        </div>

        <div v-if="!filteredListings.length && !loading" class="empty-state">
            {{ keyword.trim()
                ? "找不到符合關鍵字的交換刊登。"
                : "目前沒有公開的交換刊登。" }}
        </div>

        <div v-if="loading && !listings.length" class="empty-state">
            正在載入交換刊登……
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
                    <span class="type-label exchange-label">
                        交換特典
                    </span>

                    <span class="status-label">
                        {{ item.status || "狀態未提供" }}
                    </span>
                </div>

                <h3>{{ item.title || "未命名刊登" }}</h3>

                <p class="listing-description">
                    {{ item.description || "刊登者尚未提供敘述。" }}
                </p>

                <div class="listing-meta">
                    <span>刊登日期</span>

                    <strong>
                        {{ formatDate(item.createdAt || item.created_at) }}
                    </strong>
                </div>
            </article>
        </div>

        <!-- 分頁 -->
        <nav v-if="listingCount > pageSize" class="pagination" aria-label="特典刊登分頁">
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
    margin-bottom: 1.2rem;
    padding: 1rem;
    border: 1px solid #e2e8f0;
    border-radius: .8rem;
    background: #f8fafc;
}

.search-controls {
    display: flex;
    gap: .6rem;
}

.search-controls input {
    min-width: 0;
    flex: 1;
}

.search-hint {
    margin: .6rem 0 0;
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

.exchange-label {
    background: #e0f2fe;
    color: #075985;
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

.listing-meta {
    border-top: 1px solid #f1f5f9;
    padding-top: .8rem;
    display: flex;
    justify-content: space-between;
    gap: .5rem;
    color: #64748b;
    font-size: .85rem;
}

.listing-meta strong {
    color: #334155;
    font-weight: 600;
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
    .search-controls {
        flex-direction: column;
    }

    .pagination {
        gap: .5rem;
    }
}
</style>