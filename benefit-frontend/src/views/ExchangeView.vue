<!-- ========== views/ExchangeView.vue ========== -->
<!-- 這是交易專區的主頁，負責管理資料、頁籤、Token、表單及各子元件之間的資料傳遞。
下方程式碼會使用四個子元件。子元件只負責自己的畫面與事件，API 呼叫及資料更新則由主頁統一處理。 -->
<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuth } from "@/composables/useAuth";
import Swal from "sweetalert2";
import AppModal from "@/components/ui/AppModal.vue";
import PageIntro from "@/components/layout/PageIntro.vue";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import ticketIcon from "@iconify-icons/mdi/ticket-outline";
import swapIcon from "@iconify-icons/mdi/swap-horizontal";
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

// ===============
// 頁籤與路由同步
// ===============

const route = useRoute();
const router = useRouter();

// 使用組員既有的登入狀態，不另外建立 Token 狀態。
const {
    token,
    isLoggedIn,
    refreshAuth,
} = useAuth();

// 依網址決定顯示哪個頁籤。
// /exchange         -> 特典交換
// /ticket           -> 票券交換
// /exchange?type=member -> 會員資料區

const mainTab = ref("exchange");

watch(
    () => [route.path, route.query.type],
    ([path, type]) => {
        if (type === "member") {
            mainTab.value = "member";
        } else if (path === "/ticket") {
            mainTab.value = "ticket";
        } else {
            mainTab.value = "exchange";
        }
    },
    { immediate: true },
);

const memberTab = ref("myExchange");
const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");

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
// 交換刊登彈窗狀態
// ============================================================

// 控制交換刊登新增／修改彈窗是否開啟
const isExchangeModalOpen = ref(false);

// 防止交換刊登表單重複送出
const isSavingExchange = ref(false);

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
    { value: "CLOSED", label: "已結束" },
    { value: "DELETED", label: "已刪除" },
];

// ============================================================
// 登入導向
// ============================================================

function goToLogin() {
    router.push({
        name: "login",
        query: {
            redirect: route.fullPath,
        },
    });
}

// ============================================================
// 同步網址
// ============================================================

function changeMainTab(tab) {
    // 保留其他 query 參數，只移除用來判斷舊頁籤的 type。
    const query = { ...route.query };
    delete query.type;

    if (tab === "exchange") {
        // 特典交換
        router.push({
            path: "/exchange",
            query,
        });
        return;
    }

    if (tab === "ticket") {
        // 票券交換
        router.push({
            path: "/ticket",
            query,
        });
        return;
    }

    if (tab === "member") {
        // 會員資料區沿用原本的 query 切換方式。
        router.push({
            path: "/exchange",
            query: {
                ...query,
                type: "member",
            },
        });
    }
}

// ============================================================
// ---
// ============================================================

function openAddExchange() {
    resetExchangeForm();
    memberTab.value = "addExchange";
    changeMainTab("member");
}

function openAddTicket() {
    resetTicketForm();
    memberTab.value = "addTicket";
    changeMainTab("member");
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
    const matchedStatus = statusOptions.find(
        (item) => item.value === status,
    );

    if (matchedStatus) {
        return matchedStatus.label;
    }

    // 舊資料若仍回傳已移除的 HIDDEN，依照目前的狀態設計，顯示為「已結束」。
    if (status === "HIDDEN") {
        return "已結束";
    }

    return status || "未知";
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

    const daysInMonth = [31, isLeapYear ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31,];

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
        return "登入驗證失敗或目前帳號沒有此操作的權限，請重新登入或確認帳號權限。";
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

// ============================================================
// 重設表單
// ============================================================

function resetExchangeForm() {
    editingExchangeId.value = null;

    exchangeForm.title = "";
    exchangeForm.description = "";
    exchangeForm.useBenefitId = "";
}
// ============================================================
// 開啟新增交換刊登彈窗
// ============================================================

function openCreateExchangeModal() {
    // 新增模式：清空表單與編輯 ID
    resetExchangeForm();

    // 開啟彈窗
    isExchangeModalOpen.value = true;
}

// ============================================================
// 關閉交換刊登彈窗
// ============================================================

function closeExchangeModal() {
    // 儲存期間不允許手動關閉
    if (isSavingExchange.value) {
        return;
    }

    isExchangeModalOpen.value = false;

    // 清除未儲存的表單內容
    resetExchangeForm();
}

// ============================================================
// 彈窗被關閉時，清理交換表單
// ============================================================

watch(isExchangeModalOpen, (isOpen) => {
    if (!isOpen && !isSavingExchange.value) {
        resetExchangeForm();
    }
});

function resetTicketForm() {
    editingTicketId.value = null;

    ticketForm.title = "";
    ticketForm.description = "";
    ticketForm.cinemaId = "";
    ticketForm.movieId = "";
    ticketForm.ticketShowtime = "";
}

// ============================================================
// 開啟修改交換刊登彈窗
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

    // 不切換會員頁籤，直接在原畫面開啟彈窗
    isExchangeModalOpen.value = true;
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
    if (isSavingExchange.value) {
        return;
    }

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

    isSavingExchange.value = true;

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

        // 儲存成功：先關閉彈窗並清除表單
        resetExchangeForm();
        isExchangeModalOpen.value = false;

        await Swal.fire(
            "完成",
            isEditing
                ? "交換刊登修改成功。"
                : "交換刊登新增成功。",
            "success",
        );

        // 重新載入公開刊登及會員資料
        await loadPublicListings();
        await loadMemberData();
    } catch (error) {
        // 儲存失敗時不關閉彈窗，保留使用者輸入內容
        await Swal.fire(
            "操作失敗",
            errorText(error),
            "error",
        );
    } finally {
        isSavingExchange.value = false;
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
    if (!isLoggedIn.value || !token.value) {
        await goToLogin();
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
    // 重新讀取共用登入狀態。
    refreshAuth();

    // 公開刊登與表單選項不要求登入。
    await Promise.all([
        loadPublicListings(),
        loadSelectionOptions(),
    ]);

    // 只有登入後才載入會員資料。
    if (isLoggedIn.value && token.value) {
        await loadMemberData();
    }
});
</script>

<template>
    <section class="exchange-page">

        <!-- 使用組員既有的 PageIntro 共用元件 -->
        <PageIntro kicker="✦ BENEFITS JOURNEY" lead="交換" mark="專區" tail="！" description="尋找喜愛的特典與票券，探索交換機會。"
            :icon-left="giftIcon" :icon-right="ticketIcon" :icon-top="swapIcon" />

        <!-- 新增刊登按鈕保留在標題下方 -->
        <div v-if="isLoggedIn && mainTab !== 'member'" class="page-heading-actions">
            <button v-if="mainTab === 'exchange'" class="btn btn-primary" type="button" @click="openAddExchange">
                <span class="action-icon" aria-hidden="true">＋</span>
                新增交換刊登
            </button>

            <button v-if="mainTab === 'ticket'" class="btn btn-outline-primary" type="button" @click="openAddTicket">
                <span class="action-icon" aria-hidden="true">＋</span>
                新增票券刊登
            </button>
        </div>

        <!-- 右側純裝飾圖案，不影響交易功能 -->
        <div v-if="errorMessage" class="alert alert-warning alert-preline" role="alert">
            {{ errorMessage }}
        </div>

        <div v-if="successMessage" class="alert alert-success" role="status">
            {{ successMessage }}
        </div>

        <nav class="main-tabs" aria-label="交易專區分類">
            <button type="button" :class="{ active: mainTab === 'exchange' }" @click="changeMainTab('exchange')">
                交換刊登
            </button>

            <button type="button" :class="{ active: mainTab === 'ticket' }" @click="changeMainTab('ticket')">
                票券刊登
            </button>

            <button type="button" :class="{ active: mainTab === 'member' }" @click="changeMainTab('member')">
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
                    {{ isLoggedIn ? "已登入" : "尚未登入" }}
                </span>
            </div>

            <div v-if="!isLoggedIn" class="login-panel">
                <h3>登入後使用會員功能</h3>
                <p> 公開刊登可以直接瀏覽。 如需新增、修改自己的刊登或管理收藏，請先使用網站既有的會員登入功能。 </p>
                <button class="btn btn-primary mt-3" type="button" @click="goToLogin"> 前往登入 </button>
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

                    <button type="button" :class="{ active: isExchangeModalOpen }" @click="openCreateExchangeModal">
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
                                rows="4" maxlength="500"></textarea>
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
        <!-- =========================================================
     新增／修改交換刊登彈窗
========================================================== -->
        <AppModal v-model="isExchangeModalOpen" :title="editingExchangeId !== null
                ? '修改交換刊登'
                : '新增交換刊登'
            " :busy="isSavingExchange" :close-on-backdrop="false" width="42rem">
            <form id="exchange-listing-form" @submit.prevent="submitExchangeForm">
                <div class="mb-3">
                    <label for="exchange-title" class="form-label">
                        刊登標題
                    </label>

                    <input id="exchange-title" v-model="exchangeForm.title" class="form-control" maxlength="100"
                        required :disabled="isSavingExchange" />
                </div>

                <div class="mb-3">
                    <label for="exchange-description" class="form-label">
                        刊登敘述
                    </label>

                    <textarea id="exchange-description" v-model="exchangeForm.description" class="form-control" rows="4"
                        maxlength="500" :disabled="isSavingExchange" />
                </div>

                <div class="mb-3">
                    <label for="use-benefit-select" class="form-label">
                        選擇自己的持有特典
                    </label>

                    <select id="use-benefit-select" v-model="exchangeForm.useBenefitId" class="form-select" required
                        :disabled="editingExchangeId !== null ||
                            isSavingExchange
                            ">
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

                    <p v-if="editingExchangeId !== null" class="form-text">
                        修改刊登時，持有特典不可更換。
                    </p>
                </div>

                <div v-if="selectedUseBenefit" class="benefit-preview">
                    <img v-if="useBenefitImage(selectedUseBenefit)" :src="useBenefitImage(selectedUseBenefit)"
                        alt="持有特典圖片" />

                    <div v-else class="image-placeholder">
                        尚無圖片
                    </div>

                    <div>
                        <strong>
                            {{ useBenefitTitle(selectedUseBenefit) }}
                        </strong>

                        <p>
                            持有特典編號：
                            {{ getUseBenefitId(selectedUseBenefit) }}
                        </p>
                    </div>
                </div>
            </form>

            <!-- AppModal 的 footer 插槽 -->
            <template #footer>
                <div class="form-actions">
                    <button class="btn btn-outline-secondary" type="button" :disabled="isSavingExchange"
                        @click="closeExchangeModal">
                        返回
                    </button>

                    <button class="btn btn-primary" type="submit" form="exchange-listing-form"
                        :disabled="isSavingExchange">
                        {{
                            isSavingExchange
                                ? "儲存中…"
                                : editingExchangeId !== null
                                    ? "儲存修改"
                                    : "新增交換刊登"
                        }}
                    </button>
                </div>
            </template>
        </AppModal>
        <p class="chat-placeholder">
            聊聊／訊息功能預留位置：待訊息功能完成後，再整合至刊登畫面。
        </p>
    </section>
</template>


<style scoped>
/* ============================================================
    交易專區：統一品牌色與基礎樣式
    參考 DetailCard.vue、SearchFilter.vue、PageIntro.vue
    本區只調整視覺，不修改交易功能
   ============================================================ */

.exchange-page {
    --exchange-primary: #ff7049;
    --exchange-primary-dark: #c94f32;
    --exchange-primary-deep: #9a4632;
    --exchange-accent: #fff0e9;
    --exchange-accent-soft: #fff8f4;

    --exchange-navy: #1e2b45;
    --exchange-ink: #292b35;
    --exchange-muted: #687386;

    --exchange-border: #e8e5e1;
    --exchange-border-strong: #d9d3ce;
    --exchange-surface: #ffffff;
    --exchange-background: #faf8f6;

    min-width: 0;
    color: var(--exchange-ink);
}

.exchange-page *,
.exchange-page *::before,
.exchange-page *::after {
    box-sizing: border-box;
}

.exchange-page button,
.exchange-page input,
.exchange-page select,
.exchange-page textarea {
    font: inherit;
}

.exchange-page button:disabled,
.exchange-page select:disabled,
.exchange-page input:disabled,
.exchange-page textarea:disabled {
    cursor: not-allowed;
    opacity: 0.58;
}

/* ============================================================
    標題右側新增按鈕
   ============================================================ */
/* PageIntro 下方的新增刊登按鈕 */
.page-heading-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 0.65rem;

    margin: -0.5rem 0 1.5rem;
}

.page-heading-actions .btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.45rem;

    min-height: 44px;
    padding: 0.7rem 1.1rem;

    border: 1px solid transparent;
    border-radius: 0.7rem;

    font-size: 0.9rem;
    font-weight: 750;

    transition:
        transform 0.2s ease,
        box-shadow 0.2s ease,
        background-color 0.2s ease,
        border-color 0.2s ease;
}

.page-heading-actions .btn-primary {
    border-color: var(--exchange-primary);
    background: var(--exchange-primary);
    color: #ffffff;
}

.page-heading-actions .btn-primary:hover:not(:disabled) {
    border-color: var(--exchange-primary-dark);
    background: var(--exchange-primary-dark);
    color: #ffffff;

    transform: translateY(-2px);
    box-shadow: 0 5px 14px rgba(201, 79, 50, 0.2);
}

.page-heading-actions .btn-outline-primary {
    border-color: var(--exchange-primary);
    background: #ffffff;
    color: var(--exchange-primary-dark);
}

.page-heading-actions .btn-outline-primary:hover:not(:disabled) {
    border-color: var(--exchange-primary);
    background: var(--exchange-accent);
    color: var(--exchange-primary-deep);

    transform: translateY(-2px);
    box-shadow: 0 5px 14px rgba(201, 79, 50, 0.1);
}

.action-icon {
    font-size: 1.15rem;
    line-height: 1;
}

.exchange-page .alert {
    margin-bottom: 1.15rem;
    padding: 0.9rem 1rem;

    border-radius: 0.75rem;
    line-height: 1.7;
}

.exchange-page .alert-preline {
    white-space: pre-line;
    overflow-wrap: anywhere;
}

.exchange-page .alert-warning {
    border-color: #f0d8a9;
    background: #fff9ec;
    color: #805d19;
}

.exchange-page .alert-success {
    border-color: #b9e4ca;
    background: #f0faf3;
    color: #24633b;
}

/* 主要交易頁籤 */
.main-tabs {
    display: flex;
    align-items: stretch;
    flex-wrap: wrap;
    gap: 0.45rem;

    margin-bottom: 1.5rem;
    padding: 0.45rem;

    border: 1px solid var(--exchange-border);
    border-radius: 0.9rem;

    background: #f4f1ee;
}

.main-tabs button {
    position: relative;

    flex: 1 1 150px;
    min-height: 48px;
    padding: 0.75rem 1rem;

    border: 1px solid transparent;
    border-radius: 0.65rem;

    background: transparent;
    color: var(--exchange-muted);

    font-size: 0.94rem;
    font-weight: 750;
    line-height: 1.5;

    cursor: pointer;

    transition:
        color 0.2s ease,
        background-color 0.2s ease,
        border-color 0.2s ease,
        box-shadow 0.2s ease;
}

.main-tabs button:hover {
    background: rgba(255, 255, 255, 0.75);
    color: var(--exchange-primary-dark);
}

.main-tabs button.active {
    border-color: #f0d8cf;
    background: #ffffff;
    color: var(--exchange-primary-dark);
    box-shadow: 0 2px 7px rgba(41, 43, 53, 0.055);
}

.main-tabs button.active::after {
    position: absolute;
    right: 22%;
    bottom: -0.45rem;
    left: 22%;

    height: 3px;
    border-radius: 999px;

    background: var(--exchange-primary);
    content: "";
}

/* 會員資料區 */
.member-section {
    min-width: 0;
    padding: clamp(1rem, 2.5vw, 1.6rem);

    border: 1px solid var(--exchange-border);
    border-radius: 1rem;

    background: var(--exchange-surface);
    box-shadow: 0 5px 20px rgba(41, 43, 53, 0.035);
}

.section-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;

    margin-bottom: 1.25rem;
    padding-bottom: 1.1rem;

    border-bottom: 1px solid #f0ece8;
}

.section-heading h2 {
    margin: 0;

    color: var(--exchange-navy);
    font-size: 1.35rem;
    font-weight: 800;
    line-height: 1.45;
}

.section-heading p {
    margin: 0.4rem 0 0;

    color: var(--exchange-muted);
    font-size: 0.92rem;
    line-height: 1.75;
}

.login-pill {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.4rem;

    flex-shrink: 0;

    padding: 0.4rem 0.8rem;

    border: 1px solid #e4e1dd;
    border-radius: 999px;

    background: #f6f4f2;
    color: #646b78;

    font-size: 0.82rem;
    font-weight: 750;
    white-space: nowrap;
}

.login-pill.is-logged-in {
    border-color: #b9e4ca;
    background: #effaf2;
    color: #24633b;
}

/* ============================================================
   尚未登入提示
   ============================================================ */

.login-panel {
    max-width: 760px;
    margin: 1.2rem auto;
    padding: clamp(1.2rem, 3vw, 1.8rem);

    border: 1px solid #f0ded6;
    border-radius: 0.9rem;

    background: linear-gradient(135deg, #fffdfb, #fff5ef);
}

.login-panel h3 {
    margin: 0;

    color: var(--exchange-navy);
    font-size: 1.2rem;
    font-weight: 800;
    line-height: 1.5;
}

.login-panel p {
    margin: 0.7rem 0 0;

    color: var(--exchange-muted);
    line-height: 1.85;
}

/* 會員功能子頁籤 */
.member-tabs {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;

    margin-bottom: 1.3rem;
    padding-bottom: 1rem;

    border-bottom: 1px solid var(--exchange-border);
}

.member-tabs button {
    min-height: 40px;
    padding: 0.6rem 0.9rem;

    border: 1px solid var(--exchange-border);
    border-radius: 0.65rem;

    background: #ffffff;
    color: var(--exchange-muted);

    font-size: 0.88rem;
    font-weight: 700;
    line-height: 1.5;

    cursor: pointer;

    transition:
        color 0.2s ease,
        background-color 0.2s ease,
        border-color 0.2s ease,
        box-shadow 0.2s ease;
}

.member-tabs button:hover {
    border-color: #f1c6b7;
    background: var(--exchange-accent-soft);
    color: var(--exchange-primary-dark);
}

.member-tabs button.active {
    border-color: #f2c3b3;
    background: var(--exchange-accent);
    color: var(--exchange-primary-deep);
    box-shadow: 0 1px 4px rgba(201, 79, 50, 0.06);
}

/* ============================================================
   會員內容與空資料提示
   ============================================================ */

.member-content {
    min-width: 0;
    padding-top: 0.25rem;
}

.member-content>h3 {
    margin: 0 0 1rem;

    color: var(--exchange-navy);
    font-size: 1.18rem;
    font-weight: 800;
    line-height: 1.5;
}

.empty-state {
    display: flex;
    align-items: center;
    justify-content: center;

    min-height: 100px;
    padding: 1.5rem 1rem;

    border: 1px dashed #d9d2cc;
    border-radius: 0.8rem;

    background: var(--exchange-background);
    color: var(--exchange-muted);

    line-height: 1.8;
    text-align: center;
}

/* 會員刊登項目 */
.member-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1.2rem;

    margin-bottom: 0.85rem;
    padding: 1.05rem;

    border: 1px solid var(--exchange-border);
    border-radius: 0.85rem;

    background: #ffffff;

    transition:
        border-color 0.2s ease,
        box-shadow 0.2s ease,
        transform 0.2s ease;
}

.member-row:hover {
    border-color: #e8c8bd;
    box-shadow: 0 4px 15px rgba(41, 43, 53, 0.055);
}

.member-row:last-child {
    margin-bottom: 0;
}

.member-row-main {
    min-width: 0;
    flex: 1;
}

.member-row h4 {
    margin: 0 0 0.4rem;

    color: var(--exchange-navy);
    font-size: 1.02rem;
    font-weight: 800;
    line-height: 1.55;

    overflow-wrap: anywhere;
}

.member-row p {
    margin: 0 0 0.6rem;

    color: var(--exchange-muted);
    font-size: 0.9rem;
    line-height: 1.75;

    white-space: pre-wrap;
    overflow-wrap: anywhere;
}

.member-row .small-meta {
    margin-top: 0.25rem;
    font-size: 0.84rem;
    line-height: 1.7;
}

.status-label {
    display: inline-flex;
    align-items: center;
    justify-content: center;

    margin-top: 0.15rem;
    padding: 0.3rem 0.7rem;

    border: 1px solid #e5e1dc;
    border-radius: 999px;

    background: #f7f5f2;
    color: #626977;

    font-size: 0.76rem;
    font-weight: 750;
    line-height: 1.4;
}

/* ============================================================
   刊登操作按鈕與狀態選單
   ============================================================ */

.row-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 0.5rem;

    flex-shrink: 0;
}

.row-actions .btn {
    min-height: 36px;
    padding: 0.45rem 0.75rem;

    border-radius: 0.55rem;
    font-size: 0.84rem;
    font-weight: 700;

    transition:
        background-color 0.2s ease,
        border-color 0.2s ease,
        color 0.2s ease;
}

.row-actions .btn-outline-primary {
    border-color: #edb8a6;
    background: #ffffff;
    color: var(--exchange-primary-dark);
}

.row-actions .btn-outline-primary:hover:not(:disabled) {
    border-color: var(--exchange-primary);
    background: var(--exchange-accent);
    color: var(--exchange-primary-deep);
}

.status-select {
    min-width: 112px;
    max-width: 160px;
    min-height: 36px;

    border-color: #e0dbd6;
    border-radius: 0.55rem;

    background-color: #ffffff;
    color: var(--exchange-ink);

    font-size: 0.84rem;
}

.status-select:focus {
    border-color: #f2a58d;
    box-shadow: 0 0 0 0.2rem rgba(255, 112, 73, 0.12);
}

/* 新增／修改表單 */
.form-panel {
    width: 100%;
    max-width: 820px;
}

.form-panel form {
    padding: clamp(1rem, 2.5vw, 1.5rem);

    border: 1px solid var(--exchange-border);
    border-radius: 0.9rem;

    background: #fffdfb;
}

.form-panel .mb-3 {
    margin-bottom: 1.2rem !important;
}

.form-panel .form-label {
    display: block;

    margin-bottom: 0.5rem;

    color: var(--exchange-ink);
    font-size: 0.9rem;
    font-weight: 750;
    line-height: 1.6;
}

.form-panel .form-control,
.form-panel .form-select {
    display: block;

    width: 100%;
    min-height: 44px;
    padding: 0.65rem 0.8rem;

    border: 1px solid #ded8d2;
    border-radius: 0.65rem;

    background-color: #ffffff;
    color: var(--exchange-ink);

    font-size: 0.92rem;
    line-height: 1.5;

    transition:
        border-color 0.2s ease,
        box-shadow 0.2s ease;
}

.form-panel textarea.form-control {
    min-height: 112px;
    resize: vertical;
}

.form-panel .form-control::placeholder,
.form-panel textarea::placeholder {
    color: #9a9eaa;
}

.form-panel .form-control:focus,
.form-panel .form-select:focus {
    border-color: #f2a58d;
    outline: none;
    box-shadow: 0 0 0 0.2rem rgba(255, 112, 73, 0.13);
}

.form-panel .form-control:disabled,
.form-panel .form-select:disabled {
    background-color: #f5f2ef;
    color: #7c818b;
}

.form-text {
    display: block;

    margin-top: 0.45rem;

    color: var(--exchange-muted);
    font-size: 0.82rem;
    line-height: 1.75;
}

.form-actions {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.65rem;

    margin-top: 1.3rem;
    padding-top: 1rem;

    border-top: 1px solid #f0ece8;
}

.form-actions .btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;

    min-height: 42px;
    padding: 0.65rem 1.05rem;

    border-radius: 0.65rem;

    font-size: 0.88rem;
    font-weight: 750;

    transition:
        background-color 0.2s ease,
        border-color 0.2s ease,
        color 0.2s ease,
        box-shadow 0.2s ease;
}

.form-actions .btn-primary {
    border-color: var(--exchange-primary);
    background: var(--exchange-primary);
    color: #ffffff;
}

.form-actions .btn-primary:hover:not(:disabled) {
    border-color: var(--exchange-primary-dark);
    background: var(--exchange-primary-dark);
    color: #ffffff;
    box-shadow: 0 3px 10px rgba(201, 79, 50, 0.16);
}

.form-actions .btn-outline-secondary {
    border-color: #d8d3ce;
    background: #ffffff;
    color: #5f6571;
}

.form-actions .btn-outline-secondary:hover:not(:disabled) {
    border-color: #b9b2ac;
    background: #f5f2ef;
    color: var(--exchange-ink);
}

/* 選取的持有特典預覽 */
.benefit-preview {
    display: flex;
    align-items: center;
    gap: 1rem;

    margin-bottom: 1rem;
    padding: 0.95rem;

    border: 1px solid #f0ded6;
    border-radius: 0.85rem;

    background: #fff8f4;
}

.benefit-preview img,
.benefit-preview .image-placeholder {
    width: 96px;
    height: 96px;

    flex-shrink: 0;

    border-radius: 0.7rem;
    background: #f3efeb;

    object-fit: cover;
}

.benefit-preview .image-placeholder {
    display: grid;
    place-items: center;

    border: 1px dashed #d9d2cc;

    color: var(--exchange-muted);
    font-size: 0.8rem;
    text-align: center;
}

.benefit-preview strong {
    display: block;

    color: var(--exchange-navy);
    font-size: 0.94rem;
    line-height: 1.65;

    overflow-wrap: anywhere;
}

.benefit-preview p {
    margin: 0.35rem 0 0;

    color: var(--exchange-muted);
    font-size: 0.83rem;
    line-height: 1.65;
}

/* ============================================================
   聊聊／訊息預留區
   ============================================================ */

.chat-placeholder {
    margin-top: 1.5rem;
    padding: 1rem 1.1rem;

    border: 1px dashed #ded6cf;
    border-radius: 0.8rem;

    background: #faf8f6;
    color: var(--exchange-muted);

    font-size: 0.87rem;
    line-height: 1.8;
}

/* 鍵盤操作的焦點樣式 */
.exchange-page button:focus-visible,
.exchange-page a:focus-visible,
.exchange-page select:focus-visible,
.exchange-page input:focus-visible,
.exchange-page textarea:focus-visible {
    outline: 3px solid rgba(255, 112, 73, 0.38);
    outline-offset: 2px;
}

.exchange-page button:focus:not(:focus-visible),
.exchange-page select:focus:not(:focus-visible),
.exchange-page input:focus:not(:focus-visible),
.exchange-page textarea:focus:not(:focus-visible) {
    outline: none;
}

/* 平板與較窄的視窗 */
@media (max-width: 900px) {
    .member-row {
        align-items: flex-start;
    }

    .row-actions {
        justify-content: flex-start;
    }
}

/* 手機版 */
@media (max-width: 768px) {
    .page-heading-actions {
        justify-content: flex-start;
        width: 100%;
    }

    .page-heading-actions .btn {
        width: 100%;
    }

    .section-heading {
        align-items: flex-start;
    }

    .member-row {
        flex-direction: column;
        gap: 0.9rem;
    }

    .member-row-main {
        width: 100%;
    }

    .row-actions {
        justify-content: flex-start;
        width: 100%;
    }

    .status-select {
        flex: 1 1 130px;
    }

    .form-panel {
        max-width: 100%;
    }
}

@media (max-width: 600px) {
    .main-tabs {
        gap: 0.3rem;
        padding: 0.3rem;
        border-radius: 0.8rem;
    }

    .main-tabs button {
        flex: 1 1 100%;
        min-height: 43px;
        padding: 0.65rem 0.5rem;
        font-size: 0.9rem;
    }

    .main-tabs button.active::after {
        right: 30%;
        left: 30%;
    }

    .member-section {
        padding: 0.9rem;
        border-radius: 0.85rem;
    }

    .section-heading {
        flex-direction: column;
        gap: 0.7rem;
    }

    .section-heading h2 {
        font-size: 1.22rem;
    }

    .member-tabs {
        gap: 0.4rem;
        padding-bottom: 0.85rem;
    }

    .member-tabs button {
        flex: 1 1 calc(50% - 0.4rem);
        min-width: 0;
        padding: 0.6rem 0.45rem;
        font-size: 0.82rem;
        overflow-wrap: anywhere;
    }

    .member-content>h3 {
        font-size: 1.08rem;
    }

    .member-row {
        padding: 0.85rem;
    }

    .row-actions .btn {
        flex: 1 1 auto;
    }

    .status-select {
        min-width: 0;
        max-width: none;
    }

    .benefit-preview {
        align-items: flex-start;
        gap: 0.75rem;
        padding: 0.8rem;
    }

    .benefit-preview img,
    .benefit-preview .image-placeholder {
        width: 76px;
        height: 76px;
    }

    .form-panel form {
        padding: 0.9rem;
    }

    .form-panel .form-control,
    .form-panel .form-select {
        font-size: 16px;
    }

    .form-actions {
        flex-direction: column;
        align-items: stretch;
    }

    .form-actions .btn {
        width: 100%;
    }

    .chat-placeholder {
        padding: 0.85rem;
        font-size: 0.83rem;
    }
}

/* 尊重使用者的減少動態效果設定 */
@media (prefers-reduced-motion: reduce) {

    .exchange-page *,
    .exchange-page *::before,
    .exchange-page *::after {
        scroll-behavior: auto !important;
        transition-duration: 0.01ms !important;
        animation-duration: 0.01ms !important;
        animation-iteration-count: 1 !important;
    }
}
</style>