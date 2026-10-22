<!-- ========== views/ExchangeView.vue ========== -->
<!-- 這是交易專區的主頁，負責管理資料、頁籤、Token、表單及各子元件之間的資料傳遞。

下方程式碼會使用四個子元件。子元件只負責自己的畫面與事件，API 呼叫及資料更新則由主頁統一處理。 -->
<script setup>
// import { computed, onMounted, reactive, ref } from "vue";
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";

import Swal from "sweetalert2";

import ExchangeListingSection from "@/components/exchange/ExchangeListingSection.vue";
import TicketListingSection from "@/components/exchange/TicketListingSection.vue";
import ExchangeFavoriteSection from "@/components/exchange/ExchangeFavoriteSection.vue";
import TicketFavoriteSection from "@/components/exchange/TicketFavoriteSection.vue";

import {
    getExchangeListings,
    getMyExchangeListings,
    createExchangeListing,
    updateExchangeListing,
    updateExchangeListingStatus,
    getMyExchangeFavorites,
    addExchangeFavorite,
    removeExchangeFavorite,
    getTicketListings,
    getMyTicketListings,
    createTicketListing,
    updateTicketListing,
    updateTicketListingStatus,
    getMyTicketFavorites,
    addTicketFavorite,
    removeTicketFavorite,
    getCinemas,
    getMovies,
    getMyUseBenefits,
} from "@/api/exchangeApi.js";

// ============================================================
// 頁籤與狀態
// ============================================================

// const mainTab = ref("exchange");

// const memberTab = ref("myExchange");
// ===============
// 頁籤與路由同步
// ===============

const route = useRoute();

// 依網址的 query 參數決定顯示哪個公開刊登頁籤。
// /exchange              -> 特典交換
// /exchange?type=ticket  -> 票券交換
const mainTab = ref("exchange");

watch(
    () => route.query.type,
    (type) => {
        mainTab.value = type === "ticket"
            ? "ticket"
            : "exchange";
    },
    { immediate: true },
);

const memberTab = ref("myExchange");

const loading = ref(false);

const errorMessage = ref("");

const successMessage = ref("");

const tokenInput = ref("");

const token = ref(readSavedToken());

const isLoggedIn = computed(() => Boolean(token.value));

// ============================================================
// 各類資料
// ============================================================

const exchangeListings = ref([]);

const ticketListings = ref([]);

const myExchangeListings = ref([]);

const myTicketListings = ref([]);

const myExchangeFavorites = ref([]);

const myTicketFavorites = ref([]);

const cinemas = ref([]);

const movies = ref([]);

const useBenefits = ref([]);

// ============================================================
// 編輯中的刊登 ID
// ============================================================

const editingExchangeId = ref(null);

const editingTicketId = ref(null);

// ============================================================
// 交換表單
// ============================================================

const exchangeForm = reactive({
    title: "",
    description: "",
    useBenefitId: "",
});

// ============================================================
// 票券表單
// ============================================================

const ticketForm = reactive({
    title: "",
    description: "",
    cinemaId: "",
    movieId: "",
    ticketShowtime: "",
});

// ============================================================
// 刊登狀態選項
// ============================================================

const statusOptions = [
    { value: "ACTIVE", label: "刊登中" },
    { value: "HIDDEN", label: "已隱藏" },
    { value: "CLOSED", label: "已結束" },
    { value: "DELETED", label: "已刪除" },
];

// ============================================================
// Token 管理
// ============================================================

function readSavedToken() {
    return (
        sessionStorage.getItem("token") ||
        sessionStorage.getItem("accessToken") ||
        sessionStorage.getItem("jwtToken") ||
        localStorage.getItem("token") ||
        localStorage.getItem("accessToken") ||
        localStorage.getItem("jwtToken") ||
        ""
    );
}

async function saveToken() {
    const value = tokenInput.value.trim();

    if (!value) {
        await Swal.fire(
            "請輸入 Token",
            "請貼上有效的 JWT Token。",
            "warning",
        );
        return;
    }

    token.value = value;

    sessionStorage.setItem("token", value);
    sessionStorage.removeItem("accessToken");
    sessionStorage.removeItem("jwtToken");

    successMessage.value =
        "Token 已暫存。Token 是否有效仍須由後端 API 驗證。";

    await loadMemberData();
}

function clearToken() {
    token.value = "";
    tokenInput.value = "";

    sessionStorage.removeItem("token");
    sessionStorage.removeItem("accessToken");
    sessionStorage.removeItem("jwtToken");

    localStorage.removeItem("token");
    localStorage.removeItem("accessToken");
    localStorage.removeItem("jwtToken");

    myExchangeListings.value = [];
    myTicketListings.value = [];
    myExchangeFavorites.value = [];
    myTicketFavorites.value = [];
    useBenefits.value = [];

    successMessage.value = "";
    errorMessage.value = "";
}

// ============================================================
// 通用資料處理
// ============================================================

function unwrapList(response) {
    const data = response?.data ?? response;

    if (Array.isArray(data)) return data;
    if (Array.isArray(data?.content)) return data.content;
    if (Array.isArray(data?.list)) return data.list;
    if (Array.isArray(data?.data)) return data.data;
    if (Array.isArray(data?.items)) return data.items;

    return [];
}

function firstValue(object, keys, fallback = "") {
    for (const key of keys) {
        const value = object?.[key];

        if (
            value !== undefined &&
            value !== null &&
            value !== ""
        ) {
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

function getUseBenefitId(item) {
    return firstValue(item, [
        "useBenefitId",
        "use_benefit_id",
        "useBenefit_id",
        "id",
    ]);
}

function useBenefitTitle(item) {
    return firstValue(
        item,
        ["title", "benefitName", "name", "benefitTitle"],
        `持有特典 #${getUseBenefitId(item)}`,
    );
}

function useBenefitImage(item) {
    return firstValue(item, [
        "imageUrl",
        "image_url",
        "benefitImageUrl",
        "benefit_image_url",
        "image",
    ]);
}

const selectedUseBenefit = computed(() =>
    useBenefits.value.find(
        (item) =>
            String(getUseBenefitId(item)) ===
            String(exchangeForm.useBenefitId),
    ),
);

function statusLabel(status) {
    return (
        statusOptions.find((item) => item.value === status)?.label ??
        status ??
        "未知"
    );
}

function formatDate(value) {
    if (!value) return "未提供";

    const text = String(value);

    return text.includes("T")
        ? text.split("T")[0]
        : text;
}

function toDateInputValue(value) {
    if (!value) return "";

    return String(value).slice(0, 10);
}

function toLocalDateTime(value) {
    const date = String(value ?? "").trim();

    const match = date.match(
        /^(\d{4})-(\d{2})-(\d{2})$/,
    );

    if (!match) {
        throw new Error("放映日期格式錯誤，請重新選擇日期。");
    }

    const year = Number(match[1]);
    const month = Number(match[2]);
    const day = Number(match[3]);

    if (month < 1 || month > 12) {
        throw new Error("放映月份無效，請重新選擇日期。");
    }

    const isLeapYear =
        year % 400 === 0 ||
        (year % 4 === 0 && year % 100 !== 0);

    const daysInMonth = [
        31,
        isLeapYear ? 29 : 28,
        31,
        30,
        31,
        30,
        31,
        31,
        30,
        31,
        30,
        31,
    ];

    if (day < 1 || day > daysInMonth[month - 1]) {
        throw new Error("放映日期不存在，請重新選擇日期。");
    }

    return `${date} 00:00:00`;
}

// ============================================================
// 影城、電影資料
// ============================================================

function cinemaId(item) {
    return firstValue(item, [
        "cinemaId",
        "cinema_id",
        "id",
    ]);
}

function movieId(item) {
    return firstValue(item, [
        "movieId",
        "movie_id",
        "id",
    ]);
}

function cinemaOptionName(item) {
    return firstValue(
        item,
        ["cinemaName", "name", "cinema_name", "title"],
        `影城 #${cinemaId(item)}`,
    );
}

function movieOptionName(item) {
    return firstValue(
        item,
        ["movieName", "name", "movie_name", "title"],
        `電影 #${movieId(item)}`,
    );
}

function cinemaName(item) {
    const nested = item?.cinemaBean ?? item?.cinema;

    const directName = firstValue(
        item,
        ["cinemaName", "cinema_name"],
        firstValue(
            nested,
            ["cinemaName", "name"],
            "",
        ),
    );

    if (directName) return directName;

    const id = firstValue(
        item,
        ["cinemaId", "cinema_id"],
        firstValue(
            nested,
            ["cinemaId", "cinema_id", "id"],
            "",
        ),
    );

    const match = cinemas.value.find(
        (cinema) =>
            String(cinemaId(cinema)) === String(id),
    );

    if (match) return cinemaOptionName(match);

    return id
        ? `影城編號 ${id}`
        : "影城資料未提供";
}

function movieName(item) {
    const nested = item?.movieBean ?? item?.movie;

    const directName = firstValue(
        item,
        ["movieName", "movie_name"],
        firstValue(
            nested,
            ["movieName", "name"],
            "",
        ),
    );

    if (directName) return directName;

    const id = firstValue(
        item,
        ["movieId", "movie_id"],
        firstValue(
            nested,
            ["movieId", "movie_id", "id"],
            "",
        ),
    );

    const match = movies.value.find(
        (movie) =>
            String(movieId(movie)) === String(id),
    );

    if (match) return movieOptionName(match);

    return id
        ? `電影編號 ${id}`
        : "電影資料未提供";
}

// ============================================================
// 收藏資料處理
// ============================================================

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

function isFavorited(id, kind) {
    const favorites =
        kind === "exchange"
            ? myExchangeFavorites.value
            : myTicketFavorites.value;

    return favorites.some((favorite) => {
        const listing = favoriteListing(favorite, kind);

        return (
            String(listingId(listing, kind)) === String(id)
        );
    });
}

// ============================================================
// 錯誤訊息
// ============================================================

function errorText(error) {
    const status = error?.response?.status;

    if (status === 401 || status === 403) {
        return "登入驗證失敗或 Token 已過期，請確認 Token 是否有效。";
    }

    if (status === 404) {
        return "找不到 API 路徑，請確認後端 Controller 與 Axios proxy 設定。";
    }

    return (
        error?.response?.data?.message ||
        error?.response?.data?.detail ||
        error?.message ||
        "發生未知錯誤。"
    );
}

// ============================================================
// 載入公開刊登
// ============================================================

async function loadPublicListings() {
    loading.value = true;
    errorMessage.value = "";

    try {
        const [
            exchangeResponse,
            ticketResponse,
        ] = await Promise.all([
            getExchangeListings(),
            getTicketListings(),
        ]);

        exchangeListings.value =
            unwrapList(exchangeResponse);

        ticketListings.value =
            unwrapList(ticketResponse);
    } catch (error) {
        errorMessage.value =
            `公開刊登載入失敗：${errorText(error)}`;
    } finally {
        loading.value = false;
    }
}

// ============================================================
// 載入會員資料
// ============================================================

async function loadMemberData() {
    if (!token.value) return;

    loading.value = true;
    errorMessage.value = "";

    try {
        const results = await Promise.allSettled([
            getMyExchangeListings(token.value),
            getMyTicketListings(token.value),
            getMyExchangeFavorites(token.value),
            getMyTicketFavorites(token.value),
            getMyUseBenefits(token.value),
        ]);

        const setters = [
            (response) => {
                myExchangeListings.value = unwrapList(response);
            },
            (response) => {
                myTicketListings.value = unwrapList(response);
            },
            (response) => {
                myExchangeFavorites.value = unwrapList(response);
            },
            (response) => {
                myTicketFavorites.value = unwrapList(response);
            },
            (response) => {
                useBenefits.value = unwrapList(response);
            },
        ];

        const labels = [
            "我的交換刊登",
            "我的票券刊登",
            "我的交換收藏",
            "我的票券收藏",
            "我的持有特典",
        ];

        const failures = [];

        results.forEach((result, index) => {
            if (result.status === "fulfilled") {
                setters[index](result.value);
            } else {
                failures.push(
                    `${labels[index]}：${errorText(result.reason)}`,
                );
            }
        });

        if (failures.length > 0) {
            errorMessage.value =
                "部分會員資料無法載入：\n" +
                failures.join("\n") +
                "\n請確認失敗 API 的路徑、登入權限與回傳格式。";
        }
    } finally {
        loading.value = false;
    }
}

async function loadSelectionOptions() {
    const results = await Promise.allSettled([
        getCinemas(),
        getMovies(),
    ]);

    if (results[0].status === "fulfilled") {
        cinemas.value = unwrapList(results[0].value);
    }

    if (results[1].status === "fulfilled") {
        movies.value = unwrapList(results[1].value);
    }
}

async function refreshAll() {
    await loadPublicListings();

    if (token.value) {
        await loadMemberData();
    }
}

// ============================================================
// 重設表單
// ============================================================

function resetExchangeForm() {
    editingExchangeId.value = null;

    exchangeForm.title = "";
    exchangeForm.description = "";
    exchangeForm.useBenefitId = "";
}

function resetTicketForm() {
    editingTicketId.value = null;

    ticketForm.title = "";
    ticketForm.description = "";
    ticketForm.cinemaId = "";
    ticketForm.movieId = "";
    ticketForm.ticketShowtime = "";
}

// ============================================================
// 開始修改刊登
// ============================================================

function beginEditExchange(item) {
    editingExchangeId.value =
        listingId(item, "exchange");

    exchangeForm.title = item?.title ?? "";
    exchangeForm.description = item?.description ?? "";

    exchangeForm.useBenefitId = String(
        firstValue(
            item,
            ["useBenefitId", "use_benefit_id"],
            "",
        ),
    );

    memberTab.value = "addExchange";
}

function beginEditTicket(item) {
    editingTicketId.value =
        listingId(item, "ticket");

    ticketForm.title = item?.title ?? "";
    ticketForm.description = item?.description ?? "";

    ticketForm.cinemaId = String(
        firstValue(
            item,
            ["cinemaId", "cinema_id"],
            "",
        ),
    );

    ticketForm.movieId = String(
        firstValue(
            item,
            ["movieId", "movie_id"],
            "",
        ),
    );

    ticketForm.ticketShowtime = toDateInputValue(
        firstValue(
            item,
            ["ticketShowtime", "ticket_showtime"],
            "",
        ),
    );

    memberTab.value = "addTicket";
}

// ============================================================
// 新增／修改交換刊登
// ============================================================

async function submitExchangeForm() {
    if (!token.value) {
        await Swal.fire(
            "請先登入",
            "新增或修改刊登需要有效的登入 Token。",
            "warning",
        );
        return;
    }

    if (!exchangeForm.title.trim()) {
        await Swal.fire(
            "請輸入標題",
            "交換刊登標題不可空白。",
            "warning",
        );
        return;
    }

    if (!exchangeForm.useBenefitId) {
        await Swal.fire(
            "請選擇持有特典",
            "請從自己的持有特典中選擇一筆資料。",
            "warning",
        );
        return;
    }

    const isEditing =
        editingExchangeId.value !== null;

    const payload = {
        title: exchangeForm.title.trim(),
        description: exchangeForm.description.trim(),
        useBenefitId: Number(exchangeForm.useBenefitId),
    };

    try {
        if (isEditing) {
            await updateExchangeListing(
                editingExchangeId.value,
                payload,
                token.value,
            );
        } else {
            await createExchangeListing(
                payload,
                token.value,
            );
        }

        await Swal.fire(
            "完成",
            isEditing
                ? "交換刊登修改成功。"
                : "交換刊登新增成功。",
            "success",
        );

        resetExchangeForm();
        memberTab.value = "myExchange";

        await loadPublicListings();
        await loadMemberData();
    } catch (error) {
        await Swal.fire(
            "操作失敗",
            errorText(error),
            "error",
        );
    }
}

// ============================================================
// 新增／修改票券刊登
// ============================================================

async function submitTicketForm() {
    if (!token.value) {
        await Swal.fire(
            "請先登入",
            "新增或修改刊登需要有效的登入 Token。",
            "warning",
        );
        return;
    }

    if (!ticketForm.title.trim()) {
        await Swal.fire(
            "請輸入標題",
            "票券刊登標題不可空白。",
            "warning",
        );
        return;
    }

    if (
        !ticketForm.cinemaId ||
        !ticketForm.movieId ||
        !ticketForm.ticketShowtime
    ) {
        await Swal.fire(
            "資料尚未完整",
            "請選擇影城、電影及放映日期。",
            "warning",
        );
        return;
    }

    const isEditing =
        editingTicketId.value !== null;

    try {
        const payload = {
            title: ticketForm.title.trim(),
            description: ticketForm.description.trim(),
            cinemaId: Number(ticketForm.cinemaId),
            movieId: Number(ticketForm.movieId),
            ticketShowtime: toLocalDateTime(
                ticketForm.ticketShowtime,
            ),
        };

        if (isEditing) {
            await updateTicketListing(
                editingTicketId.value,
                payload,
                token.value,
            );
        } else {
            await createTicketListing(
                payload,
                token.value,
            );
        }

        await Swal.fire(
            "完成",
            isEditing
                ? "票券刊登修改成功。"
                : "票券刊登新增成功。",
            "success",
        );

        resetTicketForm();
        memberTab.value = "myTicket";

        await loadPublicListings();
        await loadMemberData();
    } catch (error) {
        await Swal.fire(
            "操作失敗",
            errorText(error),
            "error",
        );
    }
}

// ============================================================
// 修改刊登狀態
// ============================================================

async function changeStatus(item, kind, status) {
    if (!token.value) return;

    const id = listingId(item, kind);

    if (!id) {
        await Swal.fire(
            "無法修改",
            "找不到這筆刊登的 ID。",
            "error",
        );
        return;
    }

    const confirmed = await Swal.fire({
        title: "確定要變更刊登狀態嗎？",
        text: `新狀態：${statusLabel(status)}`,
        icon: "question",
        showCancelButton: true,
        confirmButtonText: "確定",
        cancelButtonText: "取消",
    });

    if (!confirmed.isConfirmed) {
        return;
    }

    try {
        if (kind === "exchange") {
            await updateExchangeListingStatus(
                id,
                status,
                token.value,
            );
        } else {
            await updateTicketListingStatus(
                id,
                status,
                token.value,
            );
        }

        await loadPublicListings();
        await loadMemberData();

        await Swal.fire(
            "完成",
            "刊登狀態已更新。",
            "success",
        );
    } catch (error) {
        await Swal.fire(
            "修改失敗",
            errorText(error),
            "error",
        );
    }
}

// ============================================================
// 加入收藏／取消收藏
// ============================================================

async function toggleFavorite(item, kind) {
    if (!token.value) {
        await Swal.fire(
            "請先登入",
            "收藏功能需要有效的登入 Token。",
            "warning",
        );
        return;
    }

    const id = listingId(item, kind);

    if (!id) {
        await Swal.fire(
            "無法收藏",
            "找不到刊登 ID，請確認後端回傳資料。",
            "error",
        );
        return;
    }

    const favorited = isFavorited(id, kind);

    try {
        if (kind === "exchange") {
            if (favorited) {
                await removeExchangeFavorite(
                    id,
                    token.value,
                );
            } else {
                await addExchangeFavorite(
                    id,
                    token.value,
                );
            }
        } else {
            if (favorited) {
                await removeTicketFavorite(
                    id,
                    token.value,
                );
            } else {
                await addTicketFavorite(
                    id,
                    token.value,
                );
            }
        }

        await loadMemberData();
    } catch (error) {
        await Swal.fire(
            "收藏操作失敗",
            errorText(error),
            "error",
        );
    }
}

// ============================================================
// 頁面初始化
// ============================================================

onMounted(async () => {
    await Promise.all([
        loadPublicListings(),
        loadSelectionOptions(),
    ]);

    if (token.value) {
        tokenInput.value = token.value;
        await loadMemberData();
    }
});
</script>

<template>
    <section class="exchange-page">
        <header class="page-heading">
            <div>
                <p class="eyebrow">BENEFITS JOURNEY</p>
                <h1>交換區</h1>
                <p class="heading-note">
                    瀏覽交換刊登與票券刊登，登入後管理自己的刊登及收藏。
                </p>
            </div>

            <button class="btn btn-outline-secondary" type="button" :disabled="loading" @click="refreshAll">
                {{ loading ? "載入中…" : "重新整理" }}
            </button>
        </header>

        <div v-if="errorMessage" class="alert alert-warning alert-preline" role="alert">
            {{ errorMessage }}
        </div>

        <div v-if="successMessage" class="alert alert-success" role="status">
            {{ successMessage }}
        </div>

        <nav class="main-tabs" aria-label="交易專區分類">
            <button type="button" :class="{ active: mainTab === 'exchange' }" @click="mainTab = 'exchange'">
                交換刊登
            </button>

            <button type="button" :class="{ active: mainTab === 'ticket' }" @click="mainTab = 'ticket'">
                票券刊登
            </button>

            <button type="button" :class="{ active: mainTab === 'member' }" @click="mainTab = 'member'">
                會員資料區
            </button>
        </nav>

        <ExchangeListingSection v-if="mainTab === 'exchange'" :listings="exchangeListings"
            :favorites="myExchangeFavorites" :is-logged-in="isLoggedIn" :loading="loading"
            @toggle-favorite="toggleFavorite($event, 'exchange')" />

        <TicketListingSection v-else-if="mainTab === 'ticket'" :listings="ticketListings" :favorites="myTicketFavorites"
            :is-logged-in="isLoggedIn" :loading="loading" :cinema-name="cinemaName" :movie-name="movieName"
            @toggle-favorite="toggleFavorite($event, 'ticket')" />

        <section v-else class="member-section">
            <div class="section-heading">
                <div>
                    <h2>會員資料區</h2>
                    <p>管理自己的刊登、收藏，以及新增刊登。</p>
                </div>

                <span :class="[
                    'login-pill',
                    isLoggedIn ? 'is-logged-in' : '',
                ]">
                    {{ isLoggedIn ? "已提供 Token" : "尚未登入" }}
                </span>
            </div>

            <div v-if="!isLoggedIn" class="login-panel">
                <h3>登入後使用會員功能</h3>

                <p>
                    會員登入流程尚未整合時，可以先貼上有效的 JWT Token 進行測試。
                    此欄位不會建立會員，也不會自行產生 Token。
                </p>

                <label for="jwt-token" class="form-label">
                    JWT Token
                </label>

                <textarea id="jwt-token" v-model="tokenInput" class="form-control" rows="3"
                    placeholder="貼上有效的 JWT Token，不需要加上 Bearer 前綴" />

                <button class="btn btn-primary mt-3" type="button" @click="saveToken">
                    暫存 Token 並載入會員資料
                </button>
            </div>

            <div v-else class="token-tools">
                <p>
                    已從瀏覽器暫存區取得 Token。
                    若會員 API 回傳 401 或 403，請確認 Token 是否有效。
                </p>

                <div class="token-actions">
                    <button class="btn btn-outline-primary btn-sm" type="button" @click="loadMemberData">
                        重新載入會員資料
                    </button>

                    <button class="btn btn-outline-danger btn-sm" type="button" @click="clearToken">
                        清除 Token
                    </button>
                </div>

                <label for="replace-token" class="form-label mt-3">
                    更換 Token
                </label>

                <div class="token-replace">
                    <input id="replace-token" v-model="tokenInput" class="form-control" placeholder="貼上新的 JWT Token" />

                    <button class="btn btn-primary" type="button" @click="saveToken">
                        更新
                    </button>
                </div>
            </div>

            <template v-if="isLoggedIn">
                <nav class="member-tabs" aria-label="會員交易功能">
                    <button type="button" :class="{ active: memberTab === 'myExchange' }"
                        @click="memberTab = 'myExchange'">
                        我的交換刊登
                    </button>

                    <button type="button" :class="{ active: memberTab === 'myTicket' }" @click="memberTab = 'myTicket'">
                        我的票券刊登
                    </button>

                    <button type="button" :class="{ active: memberTab === 'exchangeFavorites' }"
                        @click="memberTab = 'exchangeFavorites'">
                        我的交換收藏
                    </button>

                    <button type="button" :class="{ active: memberTab === 'ticketFavorites' }"
                        @click="memberTab = 'ticketFavorites'">
                        我的票券收藏
                    </button>

                    <button type="button" :class="{ active: memberTab === 'addExchange' }"
                        @click="resetExchangeForm(); memberTab = 'addExchange'">
                        新增交換刊登
                    </button>

                    <button type="button" :class="{ active: memberTab === 'addTicket' }"
                        @click="resetTicketForm(); memberTab = 'addTicket'">
                        新增票券刊登
                    </button>
                </nav>

                <!-- 我的交換刊登 -->
                <section v-if="memberTab === 'myExchange'" class="member-content">
                    <h3>我的交換刊登</h3>

                    <div v-if="!myExchangeListings.length" class="empty-state">
                        目前沒有自己的交換刊登。
                    </div>

                    <article v-for="item in myExchangeListings" :key="listingId(item, 'exchange')" class="member-row">
                        <div class="member-row-main">
                            <h4>{{ item.title || "未命名刊登" }}</h4>
                            <p>{{ item.description || "沒有補充敘述" }}</p>
                            <span class="status-label">
                                {{ statusLabel(item.status) }}
                            </span>
                        </div>

                        <div class="row-actions">
                            <button class="btn btn-sm btn-outline-primary" type="button"
                                :disabled="item.status === 'DELETED'" @click="beginEditExchange(item)">
                                修改
                            </button>

                            <select class="form-select form-select-sm status-select" :value="item.status"
                                :disabled="item.status === 'DELETED'"
                                @change="changeStatus(item, 'exchange', $event.target.value)">
                                <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                                    {{ option.label }}
                                </option>
                            </select>
                        </div>
                    </article>
                </section>

                <!-- 我的票券刊登 -->
                <section v-else-if="memberTab === 'myTicket'" class="member-content">
                    <h3>我的票券刊登</h3>

                    <div v-if="!myTicketListings.length" class="empty-state">
                        目前沒有自己的票券刊登。
                    </div>

                    <article v-for="item in myTicketListings" :key="listingId(item, 'ticket')" class="member-row">
                        <div class="member-row-main">
                            <h4>{{ item.title || "未命名刊登" }}</h4>
                            <p>{{ item.description || "沒有補充敘述" }}</p>

                            <p class="small-meta">
                                {{ cinemaName(item) }}・
                                {{ movieName(item) }}・
                                {{ formatDate(item.ticketShowtime || item.ticket_showtime) }}
                            </p>

                            <span class="status-label">
                                {{ statusLabel(item.status) }}
                            </span>
                        </div>

                        <div class="row-actions">
                            <button class="btn btn-sm btn-outline-primary" type="button"
                                :disabled="item.status === 'DELETED'" @click="beginEditTicket(item)">
                                修改
                            </button>

                            <select class="form-select form-select-sm status-select" :value="item.status"
                                :disabled="item.status === 'DELETED'"
                                @change="changeStatus(item, 'ticket', $event.target.value)">
                                <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                                    {{ option.label }}
                                </option>
                            </select>
                        </div>
                    </article>
                </section>

                <!-- 我的交換收藏 -->
                <section v-else-if="memberTab === 'exchangeFavorites'" class="member-content">
                    <h3>我的交換收藏</h3>

                    <ExchangeFavoriteSection :favorites="myExchangeFavorites"
                        @remove-favorite="toggleFavorite($event, 'exchange')" />
                </section>

                <!-- 我的票券收藏 -->
                <section v-else-if="memberTab === 'ticketFavorites'" class="member-content">
                    <h3>我的票券收藏</h3>

                    <TicketFavoriteSection :favorites="myTicketFavorites" :cinema-name="cinemaName"
                        :movie-name="movieName" @remove-favorite="toggleFavorite($event, 'ticket')" />
                </section>

                <!-- 新增／修改交換刊登 -->
                <section v-else-if="memberTab === 'addExchange'" class="member-content form-panel">
                    <h3>
                        {{ editingExchangeId !== null ? "修改交換刊登" : "新增交換刊登" }}
                    </h3>

                    <form @submit.prevent="submitExchangeForm">
                        <div class="mb-3">
                            <label for="exchange-title" class="form-label">
                                刊登標題
                            </label>

                            <input id="exchange-title" v-model="exchangeForm.title" class="form-control" maxlength="100"
                                required />
                        </div>

                        <div class="mb-3">
                            <label for="exchange-description" class="form-label">
                                刊登敘述
                            </label>

                            <textarea id="exchange-description" v-model="exchangeForm.description" class="form-control"
                                rows="4" maxlength="500" />
                        </div>

                        <div class="mb-3">
                            <label for="use-benefit-select" class="form-label">
                                選擇自己的持有特典
                            </label>

                            <select id="use-benefit-select" v-model="exchangeForm.useBenefitId" class="form-select"
                                required :disabled="editingExchangeId !== null">
                                <option value="" disabled>
                                    請選擇持有特典
                                </option>

                                <option v-for="benefit in useBenefits" :key="getUseBenefitId(benefit)"
                                    :value="String(getUseBenefitId(benefit))">
                                    {{ useBenefitTitle(benefit) }}
                                </option>
                            </select>

                            <p v-if="!useBenefits.length" class="form-text">
                                尚未載入持有特典。請確認持有特典 API 路徑與回傳格式。
                            </p>
                        </div>

                        <div v-if="selectedUseBenefit" class="benefit-preview">
                            <img v-if="useBenefitImage(selectedUseBenefit)" :src="useBenefitImage(selectedUseBenefit)"
                                alt="持有特典圖片" />

                            <div v-else class="image-placeholder">
                                尚無圖片
                            </div>

                            <div>
                                <strong>{{ useBenefitTitle(selectedUseBenefit) }}</strong>
                                <p>
                                    持有特典編號：
                                    {{ getUseBenefitId(selectedUseBenefit) }}
                                </p>
                            </div>
                        </div>

                        <div class="form-actions">
                            <button class="btn btn-primary" type="submit">
                                {{ editingExchangeId !== null ? "儲存修改" : "新增交換刊登" }}
                            </button>

                            <button class="btn btn-outline-secondary" type="button" @click="resetExchangeForm">
                                清除
                            </button>
                        </div>
                    </form>
                </section>

                <!-- 新增／修改票券刊登 -->
                <section v-else-if="memberTab === 'addTicket'" class="member-content form-panel">
                    <h3>
                        {{ editingTicketId !== null ? "修改票券刊登" : "新增票券刊登" }}
                    </h3>

                    <form @submit.prevent="submitTicketForm">
                        <div class="mb-3">
                            <label for="ticket-title" class="form-label">
                                刊登標題
                            </label>

                            <input id="ticket-title" v-model="ticketForm.title" class="form-control" maxlength="100"
                                required />
                        </div>

                        <div class="mb-3">
                            <label for="ticket-description" class="form-label">
                                刊登敘述
                            </label>

                            <textarea id="ticket-description" v-model="ticketForm.description" class="form-control"
                                rows="4" maxlength="500" />
                        </div>

                        <div class="mb-3">
                            <label for="cinema-select" class="form-label">
                                影城名稱
                            </label>

                            <select id="cinema-select" v-model="ticketForm.cinemaId" class="form-select" required>
                                <option value="" disabled>
                                    請選擇影城
                                </option>

                                <option v-for="cinema in cinemas" :key="cinemaId(cinema)"
                                    :value="String(cinemaId(cinema))">
                                    {{ cinemaOptionName(cinema) }}
                                </option>
                            </select>

                            <p v-if="!cinemas.length" class="form-text">
                                影城選項尚未載入，請確認影城 API。
                            </p>
                        </div>

                        <div class="mb-3">
                            <label for="movie-select" class="form-label">
                                電影名稱
                            </label>

                            <select id="movie-select" v-model="ticketForm.movieId" class="form-select" required>
                                <option value="" disabled>
                                    請選擇電影
                                </option>

                                <option v-for="movie in movies" :key="movieId(movie)" :value="String(movieId(movie))">
                                    {{ movieOptionName(movie) }}
                                </option>
                            </select>

                            <p v-if="!movies.length" class="form-text">
                                電影選項尚未載入，請確認電影 API。
                            </p>
                        </div>

                        <div class="mb-3">
                            <label for="ticket-date" class="form-label">
                                放映日期
                            </label>

                            <input id="ticket-date" v-model="ticketForm.ticketShowtime" class="form-control" type="date"
                                required />

                            <div class="form-text">
                                目前將選取日期轉成 YYYY-MM-DD 00:00:00；
                                請確認後端欄位是否使用 LocalDateTime。
                            </div>
                        </div>

                        <div class="form-actions">
                            <button class="btn btn-primary" type="submit">
                                {{ editingTicketId !== null ? "儲存修改" : "新增票券刊登" }}
                            </button>

                            <button class="btn btn-outline-secondary" type="button" @click="resetTicketForm">
                                清除
                            </button>
                        </div>
                    </form>
                </section>
            </template>
        </section>

        <p class="chat-placeholder">
            聊聊／訊息功能預留位置：待訊息功能完成後，再整合至刊登畫面。
        </p>
    </section>
</template>

<style scoped>
.exchange-page {
    color: #263238;
}

.page-heading,
.section-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
}

.page-heading {
    margin-bottom: 1.5rem;
}

.eyebrow {
    margin: 0 0 .35rem;
    color: #64748b;
    font-size: .75rem;
    letter-spacing: .16em;
    font-weight: 700;
}

h1 {
    margin: 0;
    font-size: clamp(1.8rem, 4vw, 2.5rem);
    font-weight: 750;
}

.heading-note,
.section-heading p {
    margin: .4rem 0 0;
    color: #64748b;
}

.main-tabs,
.member-tabs {
    display: flex;
    flex-wrap: wrap;
    gap: .5rem;
    border-bottom: 1px solid #e2e8f0;
    margin-bottom: 1.4rem;
}

.main-tabs button,
.member-tabs button {
    border: 0;
    background: transparent;
    color: #64748b;
    padding: .8rem 1rem;
    border-bottom: 3px solid transparent;
    font-weight: 650;
}

.main-tabs button.active,
.member-tabs button.active {
    color: #174e72;
    border-bottom-color: #287ca8;
}

.section-heading {
    margin-bottom: 1.2rem;
}

.section-heading h2,
.member-content h3 {
    margin: 0;
    font-size: 1.35rem;
    font-weight: 750;
}

.login-pill {
    border-radius: 999px;
    background: #f1f5f9;
    color: #475569;
    padding: .35rem .75rem;
    white-space: nowrap;
    font-size: .85rem;
}

.login-pill.is-logged-in {
    background: #dcfce7;
    color: #166534;
}

.member-section {
    border: 1px solid #e2e8f0;
    border-radius: 1rem;
    padding: 1.2rem;
    background: #fff;
}

.login-panel {
    max-width: 760px;
    margin: 1rem auto;
    padding: 1.5rem;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: .9rem;
}

.login-panel h3 {
    font-size: 1.2rem;
    font-weight: 750;
}

.login-panel p,
.token-tools {
    color: #64748b;
}

.token-tools {
    padding: 1rem;
    border: 1px solid #e2e8f0;
    border-radius: .8rem;
    margin-bottom: 1.2rem;
}

.token-actions,
.token-replace {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: .6rem;
    margin-top: .7rem;
}

.token-replace input {
    flex: 1;
    min-width: 180px;
}

.member-content {
    padding-top: .3rem;
}

.member-content>h3 {
    margin-bottom: 1rem;
}

.member-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    padding: 1rem 0;
    border-bottom: 1px solid #e2e8f0;
}

.member-row:last-child {
    border-bottom: 0;
}

.member-row-main {
    min-width: 0;
}

.member-row h4 {
    margin: 0 0 .3rem;
    font-size: 1.05rem;
    font-weight: 750;
    overflow-wrap: anywhere;
}

.member-row p {
    margin: 0 0 .5rem;
    color: #64748b;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
}

.small-meta {
    font-size: .85rem;
}

.row-actions {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: .5rem;
}

.status-select {
    min-width: 112px;
    max-width: 150px;
}

.status-label {
    display: inline-flex;
    align-items: center;
    border-radius: 999px;
    padding: .25rem .55rem;
    font-size: .76rem;
    font-weight: 700;
    background: #f1f5f9;
    color: #475569;
}

.form-panel {
    max-width: 780px;
}

.form-actions {
    display: flex;
    flex-wrap: wrap;
    gap: .6rem;
}

.benefit-preview {
    display: flex;
    align-items: center;
    gap: 1rem;
    border: 1px solid #e2e8f0;
    border-radius: .8rem;
    padding: .8rem;
    margin-bottom: 1rem;
}

.benefit-preview img,
.image-placeholder {
    width: 96px;
    height: 96px;
    object-fit: cover;
    border-radius: .6rem;
    background: #f1f5f9;
}

.image-placeholder {
    display: grid;
    place-items: center;
    color: #64748b;
    font-size: .8rem;
    text-align: center;
}

.benefit-preview p {
    margin: .3rem 0 0;
    color: #64748b;
    font-size: .85rem;
}

.chat-placeholder {
    margin-top: 1.5rem;
    padding: .85rem 1rem;
    border-radius: .7rem;
    color: #64748b;
    background: #f8fafc;
    font-size: .9rem;
}

.alert-preline {
    white-space: pre-line;
}

@media (max-width: 600px) {

    .page-heading,
    .section-heading,
    .member-row {
        align-items: flex-start;
        flex-direction: column;
    }

    .row-actions {
        justify-content: flex-start;
    }

    .main-tabs button,
    .member-tabs button {
        padding: .65rem .55rem;
        font-size: .9rem;
    }
}
</style>