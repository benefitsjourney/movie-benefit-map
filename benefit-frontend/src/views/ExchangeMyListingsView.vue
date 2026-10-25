<!-- ========== src/views/ExchangeMyListingsView.vue ========== -->
<!-- 會員刊登管理頁面 -->

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import Swal from "sweetalert2";
import AppModal from "@/components/ui/AppModal.vue";
import { useAuth } from "@/composables/useAuth";

import {
    getMyExchangeListings,
    updateExchangeListing,
    updateExchangeListingStatus,
    getMyTicketListings,
    updateTicketListing,
    updateTicketListingStatus,
    getCinemas,
    getMovies,
    getMyUseBenefits,
} from "@/api/exchangeApi.js";

const { token, isLoggedIn, refreshAuth } = useAuth();

const activeTab = ref("exchange");
const loading = ref(false);
const saving = ref(false);
const errorMessage = ref("");

const exchangeListings = ref([]);
const ticketListings = ref([]);
const cinemas = ref([]);
const movies = ref([]);
const useBenefits = ref([]);

const editingKind = ref("");
const editingId = ref(null);
const isModalOpen = ref(false);

const exchangeForm = reactive({
    title: "",
    description: "",
    useBenefitId: "",
});

const ticketForm = reactive({
    title: "",
    description: "",
    cinemaId: "",
    movieId: "",
    ticketShowtime: "",
});

const statusOptions = [
    { value: "ACTIVE", label: "刊登中" },
    { value: "CLOSED", label: "已結束" },
    { value: "DELETED", label: "已刪除" },
];

const currentListings = computed(() =>
    activeTab.value === "exchange"
        ? exchangeListings.value
        : ticketListings.value,
);

// ========== 共用資料處理 ==========

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
    return kind === "exchange"
        ? firstValue(item, [
            "exchangeListingId",
            "exchange_listing_id",
            "id",
        ])
        : firstValue(item, [
            "ticketListingId",
            "ticket_listing_id",
            "id",
        ]);
}

function statusLabel(status) {
    if (status === "ACTIVE") return "刊登中";
    if (status === "CLOSED") return "已結束";
    if (status === "DELETED") return "已刪除";

    return status || "未知";
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

// ========== 影城與電影資料 ==========

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

function cinemaName(item) {
    const nested = item?.cinemaBean ?? item?.cinema;

    return firstValue(
        item,
        ["cinemaName", "cinema_name"],
        firstValue(
            nested,
            ["cinemaName", "name"],
            "影城資料未提供",
        ),
    );
}

function movieName(item) {
    const nested = item?.movieBean ?? item?.movie;

    return firstValue(
        item,
        ["movieName", "movie_name"],
        firstValue(
            nested,
            ["movieName", "name"],
            "電影資料未提供",
        ),
    );
}

function optionCinemaName(item) {
    return firstValue(
        item,
        ["cinemaName", "cinema_name", "name", "title"],
        `影城 #${cinemaId(item)}`,
    );
}

function optionMovieName(item) {
    return firstValue(
        item,
        ["movieName", "movie_name", "name", "title"],
        `電影 #${movieId(item)}`,
    );
}

// ========== 日期資料處理 ==========

// 保留原本的日期擷取方式，供既有資料及 API 更新流程使用。
function dateOnly(value) {
    return value ? String(value).slice(0, 10) : "";
}

// 僅調整畫面顯示，不修改實際送往 API 的日期資料。
function displayTicketDate(value) {
    if (!value) return "";

    const match = String(value).match(
        /^(\d{4})-(\d{2})-(\d{2})$/,
    );

    if (!match) return String(value);

    return `${match[1]}/${match[2]}/${match[3]}`;
}

// 開啟原生日期選擇器。
// 優先使用 showPicker()；無法使用時，改由原生欄位取得焦點。
function openDatePicker(event) {
    if (saving.value) return;

    const input = event.currentTarget;

    if (!input) return;

    if (typeof input.showPicker === "function") {
        try {
            input.showPicker();
            return;
        } catch {
            // 部分瀏覽器可能不允許程式開啟日期選擇器。
            // 保留原生日期欄位的操作方式。
        }
    }

    input.focus();
}

// ========== 刊登資料載入 ==========

async function loadListings() {
    if (!token.value) {
        errorMessage.value = "請先登入，才能查看自己的刊登。";
        return;
    }

    loading.value = true;
    errorMessage.value = "";

    try {
        const results = await Promise.allSettled([
            getMyExchangeListings(token.value),
            getMyTicketListings(token.value),
        ]);

        const failures = [];

        if (results[0].status === "fulfilled") {
            exchangeListings.value = unwrapList(
                results[0].value,
            );
        } else {
            failures.push(
                `我的 特典刊登：${errorText(results[0].reason)}`,
            );
        }

        if (results[1].status === "fulfilled") {
            ticketListings.value = unwrapList(
                results[1].value,
            );
        } else {
            failures.push(
                `我的票券刊登：${errorText(results[1].reason)}`,
            );
        }

        if (failures.length) {
            errorMessage.value = failures.join("\n");
        }
    } finally {
        loading.value = false;
    }
}

async function loadFormOptions() {
    const results = await Promise.allSettled([
        getCinemas(),
        getMovies(),
        token.value
            ? getMyUseBenefits(token.value)
            : Promise.resolve([]),
    ]);

    if (results[0].status === "fulfilled") {
        cinemas.value = unwrapList(results[0].value);
    }

    if (results[1].status === "fulfilled") {
        movies.value = unwrapList(results[1].value);
    }

    if (results[2].status === "fulfilled") {
        useBenefits.value = unwrapList(results[2].value);
    }
}

// ========== 表單處理 ==========

function resetForms() {
    editingKind.value = "";
    editingId.value = null;

    Object.assign(exchangeForm, {
        title: "",
        description: "",
        useBenefitId: "",
    });

    Object.assign(ticketForm, {
        title: "",
        description: "",
        cinemaId: "",
        movieId: "",
        ticketShowtime: "",
    });
}

function beginEdit(item, kind) {
    if (item?.status === "DELETED") return;

    editingKind.value = kind;
    editingId.value = listingId(item, kind);

    if (
        editingId.value === null ||
        editingId.value === undefined ||
        editingId.value === ""
    ) {
        Swal.fire(
            "無法修改",
            "找不到這筆刊登的 ID。",
            "error",
        );
        return;
    }

    if (kind === "exchange") {
        Object.assign(exchangeForm, {
            title: item?.title ?? "",
            description: item?.description ?? "",
            useBenefitId: String(
                firstValue(
                    item,
                    ["useBenefitId", "use_benefit_id"],
                    "",
                ),
            ),
        });
    } else {
        Object.assign(ticketForm, {
            title: item?.title ?? "",
            description: item?.description ?? "",
            cinemaId: String(
                firstValue(
                    item,
                    ["cinemaId", "cinema_id"],
                    "",
                ),
            ),
            movieId: String(
                firstValue(
                    item,
                    ["movieId", "movie_id"],
                    "",
                ),
            ),
            ticketShowtime: dateOnly(
                firstValue(
                    item,
                    ["ticketShowtime", "ticket_showtime"],
                    "",
                ),
            ),
        });
    }

    isModalOpen.value = true;
}

// 將 YYYY-MM-DD 轉成原本 API 更新流程使用的格式。
function toLocalDateTime(value) {
    if (
        !/^\d{4}-\d{2}-\d{2}$/.test(
            String(value ?? ""),
        )
    ) {
        throw new Error(
            "放映日期格式錯誤，請重新選擇日期。",
        );
    }

    return `${value} 00:00:00`;
}

// ========== 儲存修改 ==========

async function saveEdit() {
    if (!token.value || saving.value) return;

    saving.value = true;

    try {
        if (editingKind.value === "exchange") {
            if (!exchangeForm.title.trim()) {
                throw new Error("請輸入交換刊登標題。");
            }

            if (!exchangeForm.useBenefitId) {
                throw new Error("請選擇持有特典。");
            }

            await updateExchangeListing(
                editingId.value,
                {
                    title: exchangeForm.title.trim(),
                    description:
                        exchangeForm.description.trim(),
                    useBenefitId: Number(
                        exchangeForm.useBenefitId,
                    ),
                },
                token.value,
            );
        } else {
            if (
                !ticketForm.title.trim() ||
                !ticketForm.cinemaId ||
                !ticketForm.movieId ||
                !ticketForm.ticketShowtime
            ) {
                throw new Error(
                    "請完整填寫標題、影城、電影及放映日期。",
                );
            }

            await updateTicketListing(
                editingId.value,
                {
                    title: ticketForm.title.trim(),
                    description:
                        ticketForm.description.trim(),
                    cinemaId: Number(ticketForm.cinemaId),
                    movieId: Number(ticketForm.movieId),
                    ticketShowtime: toLocalDateTime(
                        ticketForm.ticketShowtime,
                    ),
                },
                token.value,
            );
        }

        isModalOpen.value = false;
        resetForms();

        await loadListings();

        await Swal.fire(
            "完成",
            "刊登修改成功。",
            "success",
        );
    } catch (error) {
        await Swal.fire(
            "修改失敗",
            errorText(error),
            "error",
        );
    } finally {
        saving.value = false;
    }
}

// ========== 刊登狀態修改 ==========

async function changeStatus(item, kind, status) {
    if (!token.value) return;

    const id = listingId(item, kind);

    if (
        id === null ||
        id === undefined ||
        id === ""
    ) {
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

    if (!confirmed.isConfirmed) return;

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

        await loadListings();

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

onMounted(async () => {
    refreshAuth();

    if (token.value) {
        await Promise.all([
            loadListings(),
            loadFormOptions(),
        ]);
    }
});
</script>

<template>
    <section class="my-listings">
        <!-- 頁面標題 -->
        <header class="page-heading">
            <div class="page-heading-content">
                <h2>我的刊登</h2>
                <p>查看及管理自己刊登的特典與票券。</p>
            </div>

            <button class="btn btn-outline-secondary reload-button" type="button" :disabled="loading"
                @click="loadListings">
                重新載入
            </button>
        </header>

        <div v-if="!isLoggedIn" class="empty-state">
            請先登入，再查看自己的刊登。
        </div>

        <template v-else>
            <!-- 錯誤訊息 -->
            <div v-if="errorMessage" class="alert alert-warning listing-error" role="alert">
                {{ errorMessage }}
            </div>

            <!-- 分類切換 -->
            <nav class="listing-tabs" aria-label="我的刊登分類">
                <button type="button" :class="{ active: activeTab === 'exchange' }" @click="activeTab = 'exchange'">
                    我的特典刊登
                </button>

                <button type="button" :class="{ active: activeTab === 'ticket' }" @click="activeTab = 'ticket'">
                    我的票券刊登
                </button>
            </nav>

            <p v-if="loading" class="loading-message">
                資料載入中……
            </p>

            <div v-else-if="!currentListings.length" class="empty-state">
                目前沒有{{
                    activeTab === "exchange"
                        ? "特典"
                        : "票券"
                }}刊登。
            </div>

            <!-- 會員刊登清單 -->
            <article v-for="item in currentListings" :key="listingId(item, activeTab)" class="listing-row">
                <div class="listing-main">
                    <h3>
                        {{ item.title || "未命名刊登" }}
                    </h3>

                    <p>
                        {{ item.description || "沒有補充敘述" }}
                    </p>

                    <p v-if="activeTab === 'ticket'" class="listing-meta">
                        {{ cinemaName(item) }}・{{ movieName(item) }}・
                        {{
                            dateOnly(
                                item.ticketShowtime ||
                                item.ticket_showtime
                            ) || "未提供日期"
                        }}
                    </p>

                    <span class="status-label">
                        {{ statusLabel(item.status) }}
                    </span>
                </div>

                <div class="listing-actions">
                    <button class="btn btn-outline-primary" type="button" :disabled="item.status === 'DELETED'"
                        @click="beginEdit(item, activeTab)">
                        修改
                    </button>

                    <select class="form-select status-select" :value="item.status" :disabled="item.status === 'DELETED'"
                        aria-label="變更刊登狀態" @change="
                            changeStatus(
                                item,
                                activeTab,
                                $event.target.value
                            )
                            ">
                        <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                            {{ option.label }}
                        </option>
                    </select>
                </div>
            </article>
        </template>

        <!-- 修改刊登彈窗 -->
        <AppModal v-model="isModalOpen" :title="editingKind === 'exchange'
            ? '修改特典刊登'
            : '修改票券刊登'
            " :busy="saving" :close-on-backdrop="false" width="42rem">
            <form id="my-listing-edit-form" class="my-listing-edit-form" @submit.prevent="saveEdit">
                <!-- 標題及敘述 -->
                <div class="edit-basic-fields">
                    <template v-if="editingKind === 'exchange'">
                        <div class="edit-field">
                            <label for="my-exchange-title" class="form-label">
                                刊登標題
                            </label>

                            <input id="my-exchange-title" v-model="exchangeForm.title" class="form-control" type="text"
                                maxlength="100" required :disabled="saving" />
                        </div>

                        <div class="edit-field">
                            <label for="my-exchange-description" class="form-label">
                                刊登敘述
                            </label>

                            <textarea id="my-exchange-description" v-model="exchangeForm.description"
                                class="form-control" rows="4" maxlength="500" :disabled="saving" />
                        </div>
                    </template>

                    <template v-else>
                        <div class="edit-field">
                            <label for="my-ticket-title" class="form-label">
                                刊登標題
                            </label>

                            <input id="my-ticket-title" v-model="ticketForm.title" class="form-control" type="text"
                                maxlength="100" required :disabled="saving" />
                        </div>

                        <div class="edit-field">
                            <label for="my-ticket-description" class="form-label">
                                刊登敘述
                            </label>

                            <textarea id="my-ticket-description" v-model="ticketForm.description" class="form-control"
                                rows="4" maxlength="500" :disabled="saving" />
                        </div>
                    </template>
                </div>

                <!-- 修改特典刊登 -->
                <template v-if="editingKind === 'exchange'">
                    <div class="edit-field">
                        <label for="my-listing-benefit" class="form-label">
                            持有特典
                        </label>

                        <select id="my-listing-benefit" v-model="exchangeForm.useBenefitId" class="form-select" required
                            disabled>
                            <option value="">
                                請選擇持有特典
                            </option>

                            <option v-for="benefit in useBenefits" :key="firstValue(
                                benefit,
                                [
                                    'useBenefitId',
                                    'use_benefit_id',
                                    'id'
                                ]
                            )
                                " :value="String(
                                    firstValue(
                                        benefit,
                                        [
                                            'useBenefitId',
                                            'use_benefit_id',
                                            'id'
                                        ]
                                    )
                                )
                                    ">
                                {{
                                    firstValue(
                                        benefit,
                                        [
                                            "title",
                                            "benefitName",
                                            "name"
                                        ],
                                        "持有特典"
                                    )
                                }}
                            </option>
                        </select>

                        <p class="form-text">
                            修改刊登時，持有特典不可更換。
                        </p>
                    </div>
                </template>

                <!-- 修改票券刊登 -->
                <template v-else>
                    <div class="edit-field">
                        <label for="my-listing-cinema" class="form-label">
                            影城
                        </label>

                        <select id="my-listing-cinema" v-model="ticketForm.cinemaId" class="form-select" required
                            :disabled="saving">
                            <option value="" disabled>
                                請選擇影城
                            </option>

                            <option v-for="cinema in cinemas" :key="cinemaId(cinema)" :value="String(cinemaId(cinema))">
                                {{ optionCinemaName(cinema) }}
                            </option>
                        </select>
                    </div>

                    <div class="edit-field">
                        <label for="my-listing-movie" class="form-label">
                            電影
                        </label>

                        <select id="my-listing-movie" v-model="ticketForm.movieId" class="form-select" required
                            :disabled="saving">
                            <option value="" disabled>
                                請選擇電影
                            </option>

                            <option v-for="movie in movies" :key="movieId(movie)" :value="String(movieId(movie))">
                                {{ optionMovieName(movie) }}
                            </option>
                        </select>
                    </div>

                    <!-- 放映日期 -->
                    <div class="edit-field">
                        <!-- 不使用 for，避免點擊標籤直接觸發日期選擇器 -->
                        <label class="form-label">
                            放映日期
                        </label>

                        <div class="my-ticket-date-picker">
                            <!-- 顯示用欄位 -->
                            <input type="text" class="form-control my-ticket-date-display" :value="displayTicketDate(
                                ticketForm.ticketShowtime
                            )
                                " placeholder="年/月/日" readonly tabindex="-1" aria-hidden="true" :disabled="saving" />

                            <!-- 日曆圖示 -->
                            <svg class="my-ticket-date-calendar" xmlns="http://www.w3.org/2000/svg" width="20"
                                height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                                stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <rect x="3" y="5" width="18" height="16" rx="2" />
                                <path d="M16 3v4" />
                                <path d="M8 3v4" />
                                <path d="M3 11h18" />
                            </svg>

                            <!-- 真正維護日期值的原生欄位 -->
                            <input id="my-listing-showtime" v-model="ticketForm.ticketShowtime"
                                class="my-ticket-date-native" type="date" aria-label="選擇放映日期" required
                                :disabled="saving" @click="openDatePicker" />
                        </div>

                        <p class="form-text">
                            請選擇票券的放映日期。
                        </p>
                    </div>
                </template>
            </form>

            <!-- 彈窗操作按鈕 -->
            <template #footer>
                <div class="form-actions">
                    <button class="btn btn-outline-secondary" type="button" :disabled="saving" @click="
                        isModalOpen = false;
                    resetForms()
                        ">
                        取消
                    </button>

                    <button class="btn btn-primary" type="submit" form="my-listing-edit-form" :disabled="saving">
                        {{
                            saving
                                ? "儲存中……"
                                : "儲存修改"
                        }}
                    </button>
                </div>
            </template>
        </AppModal>
    </section>
</template>

<style scoped>
/* ========== 頁面整體 ========== */

.my-listings {
    width: 100%;
    min-width: 0;
}

.page-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    margin-bottom: 1.25rem;
}

.page-heading-content {
    flex: 1;
    min-width: 0;
}

.page-heading h2 {
    margin: 0;
    font-size: 1.35rem;
    font-weight: 800;
    overflow-wrap: anywhere;
}

.page-heading p {
    margin: 0.4rem 0 0;
    color: #687386;
    overflow-wrap: anywhere;
}

.reload-button {
    flex-shrink: 0;
}

.listing-error {
    white-space: pre-line;
    overflow-wrap: anywhere;
}

.loading-message {
    margin: 1rem 0;
}

/* ========== 分類切換 ========== */

.listing-tabs {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    margin-bottom: 1rem;
}

.listing-tabs button {
    min-width: 0;
    max-width: 100%;
    padding: 0.65rem 0.9rem;
    border: 1px solid #e8e5e1;
    border-radius: 0.65rem;
    background: #fff;
    color: inherit;
    font: inherit;
    overflow-wrap: anywhere;
    cursor: pointer;
    transition:
        background-color 0.2s ease,
        border-color 0.2s ease;
}

.listing-tabs button.active {
    border-color: #ff7049;
    background: #fff0e9;
    color: #9a4632;
}

/* ========== 刊登清單 ========== */

.listing-row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 1rem;
    min-width: 0;
    margin-bottom: 0.85rem;
    padding: 1rem;
    border: 1px solid #e8e5e1;
    border-radius: 0.85rem;
    background: #fff;
}

.listing-main {
    flex: 1;
    min-width: 0;
}

.listing-main h3 {
    margin: 0 0 0.45rem;
    font-size: 1rem;
    overflow-wrap: anywhere;
}

.listing-main p {
    margin: 0 0 0.5rem;
    color: #687386;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
    word-break: break-word;
}

.listing-meta {
    font-size: 0.85rem;
}

.status-label {
    display: inline-block;
    max-width: 100%;
    padding: 0.25rem 0.65rem;
    border-radius: 999px;
    background: #f1f5f9;
    color: #475569;
    font-size: 0.8rem;
    overflow-wrap: anywhere;
}

/* ========== 刊登操作區 ========== */

.listing-actions {
    display: flex;
    flex-shrink: 0;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.5rem;
    min-width: 0;
}

.listing-actions .btn {
    min-height: 40px;
}

.listing-actions .status-select {
    width: auto;
    min-width: 120px;
    max-width: 100%;
}

/* ========== 空白狀態 ========== */

.empty-state {
    padding: 1.5rem 1rem;
    border: 1px dashed #d9d2cc;
    border-radius: 0.8rem;
    background: #faf8f6;
    color: #687386;
    text-align: center;
    overflow-wrap: anywhere;
}

/* ========== 修改表單 ========== */

.my-listing-edit-form {
    width: 100%;
    min-width: 0;
    margin: 0;
}

.edit-basic-fields {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    min-width: 0;
    margin-bottom: 1rem;
}

.edit-field {
    display: flex;
    flex-direction: column;
    min-width: 0;
    margin-bottom: 1rem;
}

.edit-basic-fields .edit-field {
    margin-bottom: 0;
}

.edit-field .form-label {
    display: block;
    margin-bottom: 0.5rem;
    color: #292b35;
    font-size: 0.9rem;
    font-weight: 750;
    line-height: 1.6;
}

.edit-field .form-control,
.edit-field .form-select {
    width: 100%;
    min-width: 0;
    min-height: 44px;
    font-family: inherit;
}

.edit-field textarea.form-control {
    min-height: 112px;
    resize: vertical;
}

.edit-field .form-text {
    margin-top: 0.45rem;
    overflow-wrap: anywhere;
}

/* ========== 放映日期欄位 ========== */

.my-ticket-date-picker {
    position: relative;
    width: 100%;
    min-width: 0;
    min-height: 48px;
    border-radius: 0.65rem;
}

/* 顯示日期文字的唯讀欄位 */
.my-ticket-date-display {
    display: block;
    width: 100%;
    min-width: 0;
    min-height: 48px;
    padding: 0.65rem 3rem 0.65rem 0.8rem;
    border: 1px solid #ded8d2;
    border-radius: 0.65rem;
    background: #fff;
    color: #292b35;
    font-family: inherit;
    font-size: 0.92rem;
    line-height: 1.5;
    cursor: pointer;
    user-select: none;
    -webkit-user-select: none;
    caret-color: transparent;
}

.my-ticket-date-display::placeholder {
    color: #7c818b;
    opacity: 1;
}

.my-ticket-date-display:disabled {
    background: #f5f2ef;
    color: #7c818b;
    cursor: not-allowed;
}

/* 日曆圖示不攔截點擊 */
.my-ticket-date-calendar {
    position: absolute;
    top: 50%;
    right: 0.85rem;
    z-index: 1;
    width: 20px;
    height: 20px;
    color: #65748a;
    pointer-events: none;
    transform: translateY(-50%);
}

/* 透明原生日期欄位覆蓋整個日期區域 */
.my-ticket-date-native {
    position: absolute;
    inset: 0;
    z-index: 2;
    display: block;
    width: 100%;
    height: 100%;
    min-width: 0;
    margin: 0;
    padding: 0;
    border: 0;
    border-radius: 0.65rem;
    opacity: 0;
    cursor: pointer;
    touch-action: manipulation;
}

.my-ticket-date-native:disabled {
    cursor: not-allowed;
}

/* 日期欄位取得焦點時顯示可見焦點樣式 */
.my-ticket-date-picker:focus-within .my-ticket-date-display {
    border-color: #a9c4f0;
    outline: none;
    box-shadow: 0 0 0 3px rgb(169 196 240 / 28%);
}

/* ========== 彈窗操作按鈕 ========== */

.form-actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: 0.6rem;
    min-width: 0;
}

.form-actions .btn {
    min-height: 42px;
}

/*
 * 只調整包含本頁修改表單的 AppModal。
 * 不修改共用 AppModal.vue。
 */
:global(.modal-dialog-box:has(#my-listing-edit-form)) {
    width: min(42rem, calc(100vw - 2rem)) !important;
    max-width: calc(100vw - 2rem);
    max-height: calc(100dvh - 2rem);
    overflow-x: hidden;
    overflow-y: auto;
    overscroll-behavior: contain;
}

:global(.modal-dialog-box:has(#my-listing-edit-form) .modal-body) {
    min-width: 0;
}

/* ========== 平板版：651px～900px ========== */

@media (min-width: 651px) and (max-width: 900px) {
    .page-heading {
        align-items: flex-start;
    }

    .listing-row {
        flex-direction: column;
        align-items: stretch;
        gap: 0.85rem;
    }

    .listing-actions {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        width: 100%;
    }

    .listing-actions .btn {
        flex: 0 0 auto;
    }

    .listing-actions .status-select {
        flex: 0 1 180px;
        width: auto;
        min-width: 120px;
    }

    .edit-field .form-control,
    .edit-field .form-select {
        min-height: 46px;
    }

    .my-ticket-date-picker,
    .my-ticket-date-display {
        min-height: 48px;
    }

    .my-ticket-date-display {
        font-size: 16px;
    }

    :global(.modal-backdrop:has(#my-listing-edit-form)) {
        padding: 1rem;
    }

    :global(.modal-dialog-box:has(#my-listing-edit-form)) {
        width: min(42rem, calc(100vw - 2rem)) !important;
        max-width: calc(100vw - 2rem);
        max-height: calc(100dvh - 2rem);
        overflow-x: hidden;
        overflow-y: auto;
        overscroll-behavior: contain;
    }
}

/* ========== 手機版：650px 以下 ========== */

@media (max-width: 650px) {
    .page-heading {
        align-items: stretch;
        flex-direction: column;
        gap: 0.85rem;
    }

    .reload-button {
        width: 100%;
        min-height: 44px;
    }

    .listing-tabs {
        display: grid;
        grid-template-columns: minmax(0, 1fr);
        gap: 0.6rem;
    }

    .listing-tabs button {
        width: 100%;
        min-height: 44px;
        text-align: center;
    }

    .listing-row {
        flex-direction: column;
        align-items: stretch;
        gap: 0.9rem;
        padding: 0.9rem;
    }

    .listing-main {
        width: 100%;
    }

    .listing-actions {
        display: grid;
        grid-template-columns: minmax(0, 1fr);
        width: 100%;
        gap: 0.6rem;
    }

    .listing-actions .btn,
    .listing-actions .status-select {
        width: 100%;
        min-width: 0;
        min-height: 44px;
    }

    .edit-basic-fields {
        gap: 0.95rem;
    }

    .edit-field {
        margin-bottom: 0.95rem;
    }

    .edit-field .form-control,
    .edit-field .form-select {
        min-height: 46px;
        font-size: 16px;
    }

    .edit-field textarea.form-control {
        min-height: 100px;
    }

    .my-ticket-date-picker,
    .my-ticket-date-display {
        min-height: 48px;
    }

    .my-ticket-date-display {
        font-size: 16px;
    }

    .my-ticket-date-calendar {
        right: 0.8rem;
    }

    .form-actions {
        flex-direction: column;
        align-items: stretch;
        width: 100%;
    }

    .form-actions .btn {
        width: 100%;
        min-height: 44px;
    }

    :global(.modal-backdrop:has(#my-listing-edit-form)) {
        padding: 0.75rem;
    }

    :global(.modal-dialog-box:has(#my-listing-edit-form)) {
        width: calc(100vw - 1.5rem) !important;
        max-width: calc(100vw - 1.5rem);
        max-height: calc(100dvh - 1.5rem);
        overflow-x: hidden;
        overflow-y: auto;
        overscroll-behavior: contain;
    }

    :global(.modal-dialog-box:has(#my-listing-edit-form) .modal-body) {
        min-width: 0;
        padding-right: 0.9rem;
        padding-left: 0.9rem;
    }
}
</style>
