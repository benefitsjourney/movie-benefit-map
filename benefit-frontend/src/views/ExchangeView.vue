<!-- ========== views/ExchangeView.vue ========== -->
<!-- 交易專區主頁：管理路由、頁面初始化及子元件資料傳遞。 -->
<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuth } from "@/composables/useAuth";
import Swal from "sweetalert2";
import AppModal from "@/components/ui/AppModal.vue";
import PageIntro from "@/components/layout/PageIntro.vue";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import { Icon } from "@iconify/vue";
import ticketIcon from "@iconify-icons/mdi/ticket-outline";
import swapIcon from "@iconify-icons/mdi/swap-horizontal";
import starIcon from "@iconify-icons/mdi/star-outline";
import filmIcon from "@iconify-icons/mdi/film-outline";
import calendarIcon from "@iconify-icons/mdi/calendar-outline";

import ExchangeListingSection from "@/components/exchange/ExchangeListingSection.vue";
import TicketListingSection from "@/components/exchange/TicketListingSection.vue";
import ExchangeFavoriteSection from "@/components/exchange/ExchangeFavoriteSection.vue";
import TicketFavoriteSection from "@/components/exchange/TicketFavoriteSection.vue";
import ExchangeModeSwitch from "@/components/exchange/ExchangeModeSwitch.vue";
import ExchangeListingFormModal from "@/components/exchange/ExchangeListingFormModal.vue";
import TicketListingFormModal from "@/components/exchange/TicketListingFormModal.vue";

import { useExchangeData } from "@/composables/useExchangeData.js";
import { useExchangeActions } from "@/composables/useExchangeActions.js";
import { getOrCreateExchangeConversation } from "@/api/exchangeconversationApi";

// ========== 頁籤與路由同步 ==========
const route = useRoute();
const router = useRouter();

// 使用組員既有的登入狀態，不另外建立 Token 狀態。
const {
    token,
    user,
    isLoggedIn,
    refreshAuth,
    logout,
} = useAuth();

// 防止同一批 API 的 401 重複跳出提示。
const authExpiredHandled = ref(false);

// 依網址決定顯示哪個頁籤。
// /exchange -> 特典交換
// /ticket   -> 票券交換
// /exchange?type=member -> 會員資料區
const mainTab = ref("exchange");

watch(
    () => [route.path, route.query.type],
    ([path, type]) => {
        if (type === "ticket" || path === "/ticket") {
            mainTab.value = "ticket";
        } else {
            mainTab.value = "exchange";
        }
    },
    { immediate: true },
);

const memberTab = ref("myExchange");
const successMessage = ref("");

// ========== 登入導向 ==========
function goToLogin() {
    return router.push({
        name: "login",
        query: {
            redirect: route.fullPath,
        },
    });
}

// ========== 統一處理登入失效 ==========
async function handleAuthExpired(error) {
    // 同一批 API 可能同時回傳 401，只處理一次。
    if (authExpiredHandled.value) return;

    authExpiredHandled.value = true;

    console.warn(
        "交易專區收到 HTTP 401，清除本機登入狀態。",
        error?.response?.data?.message || "",
    );

    // 使用組員既有的 logout()，不修改共用登入程式。
    logout();

    await Swal.fire({
        icon: "warning",
        title: "登入狀態已失效",
        text: "登入憑證可能已過期或無效，請重新登入後再繼續操作。",
        confirmButtonText: "前往登入",
        allowOutsideClick: false,
        allowEscapeKey: false,
    });

    await goToLogin();
}

// ========== 資料管理 ==========
const exchangeData = useExchangeData({
    token,
    onAuthExpired: handleAuthExpired,
});

const {
    exchangeListings,
    ticketListings,
    myExchangeListings,
    myTicketListings,
    myExchangeFavorites,
    myTicketFavorites,
    cinemas,
    movies,
    useBenefits,
    loading,
    errorMessage,
    statusOptions,

    unwrapList,
    firstValue,
    listingId,
    getUseBenefitId,
    useBenefitTitle,
    useBenefitImage,
    statusLabel,
    formatDate,
    toDateInputValue,
    toLocalDateTime,

    cinemaId,
    movieId,
    cinemaOptionName,
    movieOptionName,
    cinemaName,
    movieName,

    favoriteListing,
    isFavorited,
    errorText,

    loadPublicListings,
    loadMemberData,
    loadSelectionOptions,
} = exchangeData;

/** ========== 聯絡交換刊登者 ========== */
async function openChat(listing) {
    // 1. 未登入時，不呼叫聊天室 API。
    if (!isLoggedIn.value || !token.value) {
        const result = await Swal.fire({
            icon: "info",
            title: "請先登入",
            text: "登入會員後才能聯絡刊登者。",
            confirmButtonText: "前往登入",
            showCancelButton: true,
            cancelButtonText: "取消",
        });

        if (result.isConfirmed) {
            await goToLogin();
        }

        return;
    }

    // 2. 取得刊登者會員 ID。
    const targetUserId = listing?.usersId;

    if (
        targetUserId === null ||
        targetUserId === undefined ||
        targetUserId === ""
    ) {
        await Swal.fire({
            icon: "error",
            title: "無法開啟聊天室",
            text: "交換刊登資料缺少刊登者會員 ID，請確認後端回傳資料。",
            confirmButtonText: "確定",
        });

        return;
    }

    try {
        // 3. 呼叫後端，建立或取得既有聊天室。
        const response = await getOrCreateExchangeConversation(
            targetUserId,
            token.value,
        );

        // 4. 讀取聊天室資料。
        // 支援 response.data.data 或 response.data 兩種包裝格式。
        const payload = response?.data?.data ?? response?.data;
        const conversationId = payload?.conversationId;

        if (
            conversationId === null ||
            conversationId === undefined ||
            conversationId === ""
        ) {
            throw new Error("後端沒有回傳有效的聊天室 ID。");
        }

        // 5. 準備浮動聊天室需要的對話資料。
        // conversationId 是必要欄位；
        // 其他名稱欄位僅在後端有回傳時才會沿用。
        const conversation = {
            ...payload,
            conversationId,
            otherUserName:
                payload?.otherUserName ||
                listing?.usersName ||
                listing?.userName ||
                listing?.publisherName ||
                "刊登者",
        };

        // 6. 通知已掛載的 FloatingChat 開啟指定聊天室。
        // 不再跳轉至 /account/messages/:conversationId。
        window.dispatchEvent(
            new CustomEvent("open-floating-chat", {
                detail: {
                    conversation,
                },
            }),
        );
    } catch (error) {
        // 聊天室 API 也可能遇到已失效的 Token。
        if (error?.response?.status === 401) {
            await handleAuthExpired(error);
            return;
        }

        console.error("開啟聊天室失敗：", error);

        await Swal.fire({
            icon: "error",
            title: "無法開啟聊天室",
            text:
                error?.response?.data?.message ||
                error?.message ||
                "請稍後再試。",
            confirmButtonText: "確定",
        });
    }
}

// ========== 操作管理 ==========
const exchangeActions = useExchangeActions({
    token,
    isLoggedIn,
    goToLogin,
    onAuthExpired: handleAuthExpired,
    data: exchangeData,
});

const {
    editingExchangeId,
    editingTicketId,

    isExchangeModalOpen,
    isSavingExchange,
    isTicketModalOpen,
    isSavingTicket,

    exchangeForm,
    ticketForm,

    resetExchangeForm,
    resetTicketForm,
    openCreateExchangeModal,
    closeExchangeModal,
    closeTicketModal,
    beginEditExchange,
    beginEditTicket,

    submitExchangeForm,
    submitTicketForm,
    changeStatus,
    toggleFavorite,
} = exchangeActions;

// ========== 表單選取的持有特典 ==========
const selectedUseBenefit = computed(() =>
    useBenefits.value.find(
        (item) =>
            String(getUseBenefitId(item)) ===
            String(exchangeForm.useBenefitId),
    ),
);

// ========== 新增特典刊登可選擇的持有特典 ==========
// 規則：只要曾經建立過 ExchangeListing，就不能再次刊登。
// ACTIVE、CLOSED、DELETED 均視為已刊登。
// 修改模式則保留目前選取的持有特典，避免原有預覽消失。
const availableUseBenefits = computed(() => {
    const listedUseBenefitIds = new Set(
        myExchangeListings.value
            .map((listing) => getUseBenefitId(listing))
            .filter((id) => id !== null && id !== undefined && id !== "")
            .map((id) => String(id)),
    );

    return useBenefits.value.filter((benefit) => {
        const benefitId = getUseBenefitId(benefit);

        if (
            benefitId === null ||
            benefitId === undefined ||
            benefitId === ""
        ) {
            return false;
        }

        const id = String(benefitId);

        // 修改模式：保留目前刊登所使用的持有特典。
        if (
            editingExchangeId.value !== null &&
            id === String(exchangeForm.useBenefitId)
        ) {
            return true;
        }

        // 新增模式：只允許從未建立過特典刊登的持有特典。
        return !listedUseBenefitIds.has(id);
    });
});

// ========== 同步網址 ==========
function changeMainTab(tab) {
    // 保留其他 query 參數，只移除用來判斷舊頁籤的 type。
    const query = { ...route.query };
    delete query.type;

    if (tab === "exchange") {
        router.push({
            path: "/exchange",
            query,
        });
        return;
    }

    if (tab === "ticket") {
        router.push({
            path: "/ticket",
            query,
        });
    }
}

// ========== 特典／票券刊登按鈕 ==========
function openAddExchange() {
    if (!isLoggedIn.value || !token.value) {
        goToLogin();
        return;
    }

    resetExchangeForm();
    isExchangeModalOpen.value = true;
}

function openAddTicket() {
    if (!isLoggedIn.value || !token.value) {
        goToLogin();
        return;
    }

    resetTicketForm();
    isTicketModalOpen.value = true;
}

// ========== 頁面初始化 ==========
onMounted(async () => {
    // 重新讀取共用登入狀態。
    refreshAuth();

    // 公開刊登與表單選項不要求登入。
    await Promise.all([
        loadPublicListings(),
        loadSelectionOptions(),
    ]);

    // 如果載入公開資料時已觸發登入失效處理，就不再載入會員資料。
    if (authExpiredHandled.value) return;

    // console.log(
    //     "[除錯] ticketListings：",
    //     ticketListings.value,
    // );

    // console.log(
    //     "[除錯] ticketListings 筆數：",
    //     ticketListings.value?.length,
    // );

    // console.log("[除錯] 目前登入會員資料：", user.value);

    // console.log(
    //     "[除錯] 登入會員欄位名稱：",
    //     Object.keys(user.value || {}),
    // );

    // 只有登入後才載入會員資料。
    if (isLoggedIn.value && token.value) {
        await loadMemberData();
    }
});
</script>

<template>
    <section class="exchange-page">
        <PageIntro v-if="mainTab === 'exchange'" kicker="✦ EXCHANGE COLLECTION" mark="特典" tail="交換！"
            description="發現心儀的電影特典，開啟更多交換可能。" :icon-left="giftIcon" :icon-right="ticketIcon" :icon-top="starIcon" />

        <PageIntro v-else-if="mainTab === 'ticket'" kicker="✦ TICKET EXCHANGE" mark="票券" tail="交換！"
            description="正在尋找想看的電影票券嗎？來逛逛大家的刊登，遇見理想的票券交換機會！" :icon-left="ticketIcon" :icon-right="filmIcon"
            :icon-top="calendarIcon" />
        <!-- 右側純裝飾圖案，不影響交易功能 -->
        <div v-if="errorMessage" class="alert alert-warning alert-preline" role="alert">
            {{ errorMessage }}
        </div>

        <div v-if="successMessage" class="alert alert-success" role="status">
            {{ successMessage }}
        </div>

        <!-- 主要交易頁籤與新增刊登按鈕 -->
        <ExchangeModeSwitch :main-tab="mainTab" :is-logged-in="isLoggedIn" @change-tab="changeMainTab"
            @add-exchange="openAddExchange" @add-ticket="openAddTicket" />

        <ExchangeListingSection v-if="mainTab === 'exchange'" :listings="exchangeListings"
            :favorites="myExchangeFavorites" :is-logged-in="isLoggedIn" :current-user-id="user?.usersId"
            :loading="loading" @toggle-favorite="toggleFavorite($event, 'exchange')" @open-chat="openChat" />

        <TicketListingSection v-if="mainTab === 'ticket'" :listings="ticketListings" :favorites="myTicketFavorites"
            :is-logged-in="isLoggedIn" :current-user-id="user?.usersId" :loading="loading" :cinema-name="cinemaName"
            :movie-name="movieName" @toggle-favorite="toggleFavorite($event, 'ticket')" @open-chat="openChat" />

        <!-- 新增／修改特典刊登彈窗 -->
        <ExchangeListingFormModal v-model="isExchangeModalOpen" :form="exchangeForm"
            :use-benefits="availableUseBenefits" :editing-id="editingExchangeId" :is-saving="isSavingExchange"
            :get-use-benefit-id="getUseBenefitId" :use-benefit-title="useBenefitTitle"
            :use-benefit-image="useBenefitImage" @submit="submitExchangeForm" @close="closeExchangeModal" />

        <TicketListingFormModal v-model="isTicketModalOpen" :form="ticketForm" :cinemas="cinemas" :movies="movies"
            :editing-id="editingTicketId" :is-saving="isSavingTicket" :cinema-id="cinemaId" :movie-id="movieId"
            :cinema-option-name="cinemaOptionName" :movie-option-name="movieOptionName" @submit="submitTicketForm"
            @close="closeTicketModal" />
    </section>
</template>

<style scoped>
/* ========== 交易專區：統一品牌色與基礎樣式，參考 DetailCard.vue、SearchFilter.vue、PageIntro.vue，本區只調整視覺，不修改交易功能 ========== */
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

/* ========== 新增刊登按鈕基本樣式 ========== */
.page-heading-actions .btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.45rem;
    width: auto;
    max-width: 100%;
    min-width: 0;
    min-height: 44px;
    padding: 0.7rem 1.1rem;
    border: 1px solid transparent;
    border-radius: 0.7rem;
    font-size: 0.9rem;
    font-weight: 750;
    white-space: normal;
    overflow-wrap: anywhere;
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

/* ========== 主要交易頁籤與新增刊登按鈕 ========== */
/* 滑鼠移到未選取的頁籤 */
.mode-btn:hover:not(.is-on) {
    color: #ffffff;
}

/* 目前選取的頁籤 */
.mode-btn.is-on {
    background: var(--exchange-primary);
    color: #ffffff;
}

/* 鍵盤操作焦點 */
.mode-btn:focus-visible {
    outline: 2px solid #ffc857;
    outline-offset: 2px;
}

/* 頁籤旁的新增刊登按鈕 */
.mode-switch-container .page-heading-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    flex: 0 1 auto;
    flex-wrap: wrap;
    gap: 0.5rem;
    min-width: 0;
    margin: 4px 0 0 auto;
}

.mode-switch-container .page-heading-actions .btn {
    flex: 0 1 auto;
    width: auto;
    max-width: 100%;
    min-width: 0;
    white-space: normal;
}

/* ========== 中等寬度：避免頁籤與按鈕互相擠壓 ========== */
@media (max-width: 900px) {
    .mode-switch-container {
        align-items: flex-start;
        gap: 0.75rem;
    }

    .mode-switch-container .mode-switch {
        flex: 1 1 100%;
        width: 100%;
    }

    .mode-switch-container .mode-btn {
        flex: 1 1 0;
        min-width: 0;
        gap: 0.3rem;
        padding-right: 0.5rem;
        padding-left: 0.5rem;
        font-size: 0.88rem;
    }

    .mode-switch-container .page-heading-actions {
        flex: 1 1 100%;
        width: 100%;
        justify-content: flex-end;
        margin: 0;
    }

    .mode-switch-container .page-heading-actions .btn {
        flex: 0 1 auto;
        padding-right: 0.85rem;
        padding-left: 0.85rem;
        font-size: 0.86rem;
    }
}

/* ========== 窄版畫面：頁籤與新增按鈕分成上下兩列 ========== */
@media (max-width: 640px) {
    .mode-switch-container {
        flex-direction: column;
        align-items: stretch;
        gap: 0.75rem;
    }

    .mode-switch-container .mode-switch {
        display: flex;
        flex: 0 0 auto;
        width: 100%;
    }

    .mode-switch-container .mode-btn {
        flex: 1 1 0;
        min-width: 0;
        gap: 0.25rem;
        padding: 0.5rem 0.35rem;
        font-size: 0.84rem;
    }

    .mode-switch-container .mode-icon {
        width: 1rem;
        height: 1rem;
    }

    .mode-switch-container .page-heading-actions {
        flex: 0 0 auto;
        width: 100%;
        max-width: 100%;
        justify-content: flex-end;
        margin: 0;
    }

    .mode-switch-container .page-heading-actions .btn {
        flex: 0 1 auto;
        width: auto;
        max-width: 100%;
        min-height: 40px;
        padding: 0.55rem 0.8rem;
        font-size: 0.84rem;
    }
}

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

/* ========== 會員資料區 ========== */
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

/* ========== 尚未登入提示 ========== */
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

/* ========== 會員功能子頁籤 ========== */
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

/* ========== 會員刊登項目 ========== */
.member-row:hover {
    border-color: #e8c8bd;
    box-shadow: 0 4px 15px rgba(41, 43, 53, 0.055);
}

.member-row:last-child {
    margin-bottom: 0;
}

/* ========== 刊登操作按鈕與狀態選單 ========== */
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

.status-select:focus {
    border-color: #f2a58d;
    box-shadow: 0 0 0 0.2rem rgba(255, 112, 73, 0.12);
}

/* ========== 新增／修改表單 ========== */
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

/* ========== 選取的持有特典預覽 ========== */
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

/* ========== 鍵盤操作的焦點樣式 ========== */
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

/* ========== 平板與較窄的視窗 ========== */
@media (max-width: 900px) {
    .member-row {
        align-items: flex-start;
    }

    .row-actions {
        justify-content: flex-start;
    }
}

/* ========== 手機版 ========== */
@media (max-width: 768px) {
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
}

/* ========== 尊重使用者的減少動態效果設定 ========== */
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