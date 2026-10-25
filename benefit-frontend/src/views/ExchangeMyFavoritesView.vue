<!-- ========== src/views/ExchangeMyFavoritesView.vue ========== -->
<!-- 會員收藏管理頁面 -->
<script setup>
import { onMounted, ref } from "vue";
import Swal from "sweetalert2";
import { useAuth } from "@/composables/useAuth";

import ExchangeFavoriteSection from "@/components/exchange/ExchangeFavoriteSection.vue";
import TicketFavoriteSection from "@/components/exchange/TicketFavoriteSection.vue";

import {
    getMyExchangeFavorites,
    removeExchangeFavorite,
    getMyTicketFavorites,
    removeTicketFavorite,
    getCinemas,
    getMovies,
} from "@/api/exchangeApi.js";

const { token, isLoggedIn, refreshAuth } = useAuth();

const activeTab = ref("exchange");
const loading = ref(false);
const errorMessage = ref("");

const exchangeFavorites = ref([]);
const ticketFavorites = ref([]);
const cinemas = ref([]);
const movies = ref([]);

function unwrapList(response) {
    const data = response?.data ?? response;

    if (Array.isArray(data)) return data;
    if (Array.isArray(data?.content)) return data.content;
    if (Array.isArray(data?.list)) return data.list;
    if (Array.isArray(data?.data)) return data.data;
    if (Array.isArray(data?.items)) return data.items;

    return [];
}

function firstValue(item, keys, fallback = "") {
    for (const key of keys) {
        const value = item?.[key];

        if (value !== undefined && value !== null && value !== "") {
            return value;
        }
    }

    return fallback;
}

function listingId(item, kind) {
    if (kind === "exchange") {
        return firstValue(item, [
            "exchangeListingId",
            "exchange_listing_id",
            "id",
        ]);
    }

    return firstValue(item, [
        "ticketListingId",
        "ticket_listing_id",
        "id",
    ]);
}

function favoriteListing(favorite, kind) {
    if (kind === "exchange") {
        return (
            favorite?.exchangeListing ??
            favorite?.exchangeListingBean ??
            favorite?.listing ??
            favorite
        );
    }

    return (
        favorite?.ticketListing ??
        favorite?.ticketListingBean ??
        favorite?.listing ??
        favorite
    );
}

function errorText(error) {
    const status = error?.response?.status;

    if (status === 401 || status === 403) {
        return "登入驗證失敗或目前帳號沒有此操作的權限，請重新登入。";
    }

    if (status === 404) {
        return "找不到 API 路徑，請確認後端 Controller 與 Axios 設定。";
    }

    return (
        error?.response?.data?.message ||
        error?.response?.data?.detail ||
        error?.message ||
        "發生未知錯誤。"
    );
}

function cinemaId(item) {
    return firstValue(item, ["cinemaId", "cinema_id", "id"]);
}

function movieId(item) {
    return firstValue(item, ["movieId", "movie_id", "id"]);
}

function cinemaName(item) {
    const nested = item?.cinemaBean ?? item?.cinema;

    const directName = firstValue(
        item,
        ["cinemaName", "cinema_name"],
        firstValue(nested, ["cinemaName", "name"], ""),
    );

    if (directName) return directName;

    const id = firstValue(
        item,
        ["cinemaId", "cinema_id"],
        firstValue(nested, ["cinemaId", "cinema_id", "id"], ""),
    );

    const matched = cinemas.value.find(
        (cinema) => String(cinemaId(cinema)) === String(id),
    );

    if (matched) {
        return firstValue(
            matched,
            ["cinemaName", "cinema_name", "name", "title"],
            `影城 #${id}`,
        );
    }

    return id ? `影城編號 ${id}` : "影城資料未提供";
}

function movieName(item) {
    const nested = item?.movieBean ?? item?.movie;

    const directName = firstValue(
        item,
        ["movieName", "movie_name"],
        firstValue(nested, ["movieName", "name"], ""),
    );

    if (directName) return directName;

    const id = firstValue(
        item,
        ["movieId", "movie_id"],
        firstValue(nested, ["movieId", "movie_id", "id"], ""),
    );

    const matched = movies.value.find(
        (movie) => String(movieId(movie)) === String(id),
    );

    if (matched) {
        return firstValue(
            matched,
            ["movieName", "movie_name", "name", "title"],
            `電影 #${id}`,
        );
    }

    return id ? `電影編號 ${id}` : "電影資料未提供";
}

async function loadFavorites() {
    if (!token.value) {
        errorMessage.value = "請先登入，才能查看自己的收藏。";
        return;
    }

    loading.value = true;
    errorMessage.value = "";

    const results = await Promise.allSettled([
        getMyExchangeFavorites(token.value),
        getMyTicketFavorites(token.value),
        getCinemas(),
        getMovies(),
    ]);

    const failures = [];

    if (results[0].status === "fulfilled") {
        exchangeFavorites.value = unwrapList(results[0].value);
    } else {
        failures.push(`我的特典收藏：${errorText(results[0].reason)}`);
    }

    if (results[1].status === "fulfilled") {
        ticketFavorites.value = unwrapList(results[1].value);
    } else {
        failures.push(`我的票券收藏：${errorText(results[1].reason)}`);
    }

    if (results[2].status === "fulfilled") {
        cinemas.value = unwrapList(results[2].value);
    } else {
        failures.push(`影城選項：${errorText(results[2].reason)}`);
    }

    if (results[3].status === "fulfilled") {
        movies.value = unwrapList(results[3].value);
    } else {
        failures.push(`電影選項：${errorText(results[3].reason)}`);
    }

    if (failures.length) {
        errorMessage.value = failures.join("\n");
    }

    loading.value = false;
}

async function removeFavorite(item, kind) {
    if (!token.value) {
        await Swal.fire("請先登入", "取消收藏需要有效的登入 Token。", "warning");
        return;
    }

    const listing = favoriteListing(item, kind);
    const id = listingId(listing, kind);

    if (!id) {
        await Swal.fire(
            "無法取消收藏",
            "找不到刊登 ID，請確認收藏 API 的回傳格式。",
            "error",
        );
        return;
    }

    const confirmed = await Swal.fire({
        title: "確定取消收藏？",
        text: "取消後，這筆刊登會從你的收藏清單移除。",
        icon: "question",
        showCancelButton: true,
        confirmButtonText: "取消收藏",
        cancelButtonText: "保留收藏",
    });

    if (!confirmed.isConfirmed) return;

    try {
        if (kind === "exchange") {
            await removeExchangeFavorite(id, token.value);
        } else {
            await removeTicketFavorite(id, token.value);
        }

        await loadFavorites();

        await Swal.fire("完成", "已取消收藏。", "success");
    } catch (error) {
        await Swal.fire("操作失敗", errorText(error), "error");
    }
}

onMounted(async () => {
    refreshAuth();

    if (isLoggedIn.value && token.value) {
        await loadFavorites();
    } else {
        errorMessage.value = "請先登入，才能查看自己的收藏。";
    }
});
</script>

<template>
    <section class="my-favorites">
        <header class="page-heading">
            <div>
                <h2>我的收藏</h2>
                <p>查看你收藏的特典交換與票券刊登。</p>
            </div>

            <button class="btn btn-outline-secondary" type="button" :disabled="loading || !isLoggedIn"
                @click="loadFavorites">
                重新載入
            </button>
        </header>

        <div v-if="!isLoggedIn" class="empty-state">
            請先登入，再查看自己的收藏。
        </div>

        <template v-else>
            <div v-if="errorMessage" class="alert alert-warning" role="alert">
                {{ errorMessage }}
            </div>

            <nav class="favorite-tabs" aria-label="我的收藏分類">
                <button type="button" :class="{ active: activeTab === 'exchange' }" @click="activeTab = 'exchange'">
                    我的特典收藏
                </button>

                <button type="button" :class="{ active: activeTab === 'ticket' }" @click="activeTab = 'ticket'">
                    我的票券收藏
                </button>
            </nav>

            <p v-if="loading">收藏載入中……</p>

            <template v-else>
                <ExchangeFavoriteSection v-if="activeTab === 'exchange'" :favorites="exchangeFavorites"
                    @remove-favorite="removeFavorite($event, 'exchange')" />

                <TicketFavoriteSection v-else :favorites="ticketFavorites" :cinema-name="cinemaName"
                    :movie-name="movieName" @remove-favorite="removeFavorite($event, 'ticket')" />
            </template>
        </template>
    </section>
</template>

<style scoped>
.my-favorites {
    min-width: 0;
}

.page-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    margin-bottom: 1.25rem;
}

.page-heading h2 {
    margin: 0;
    font-size: 1.35rem;
    font-weight: 800;
}

.page-heading p {
    margin: 0.4rem 0 0;
    color: #687386;
}

.favorite-tabs {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    margin-bottom: 1rem;
}

.favorite-tabs button {
    padding: 0.65rem 0.9rem;
    border: 1px solid #e8e5e1;
    border-radius: 0.65rem;
    background: #fff;
    cursor: pointer;
}

.favorite-tabs button.active {
    border-color: #ff7049;
    background: #fff0e9;
    color: #9a4632;
}

.empty-state {
    padding: 1.5rem 1rem;
    border: 1px dashed #d9d2cc;
    border-radius: 0.8rem;
    background: #faf8f6;
    color: #687386;
    text-align: center;
}

@media (max-width: 650px) {
    .page-heading {
        align-items: stretch;
        flex-direction: column;
    }
}
</style>