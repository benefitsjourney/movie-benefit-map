<!-- ========== src/views/ExchangeMyListingsView.vue ========== -->
<!-- 會員刊登管理頁面 -->

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { Icon } from "@iconify/vue";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import ticketIcon from "@iconify-icons/mdi/ticket";
import cinemaIcon from "@iconify-icons/mdi/filmstrip";
import filmIcon from "@iconify-icons/mdi/movie-open";
import calendarIcon from "@iconify-icons/mdi/calendar";
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

// ========== 特典卡片資料處理 ==========

// 取得刊登者 ID。
function listingMemberId(item) {
    return firstValue(
        item,
        [
            "usersId",
            "users_id",
        ],
        firstValue(
            item?.user ?? item?.users ?? item?.usersBean,
            [
                "usersId",
                "users_id",
                "id",
            ],
            "",
        ),
    );
}

// 取得刊登者名稱。
function listingMemberName(item) {
    return firstValue(
        item,
        [
            "usersName",
            "userName",
            "username",
            "memberName",
        ],
        firstValue(
            item?.user ?? item?.users ?? item?.usersBean,
            [
                "usersName",
                "userName",
                "username",
                "name",
            ],
            "",
        ),
    );
}

// 取得特典名稱。
// 兼容直接回傳特典欄位及巢狀物件的資料結構。
function listingBenefitName(item) {
    const useBenefit =
        item?.useBenefitBean ??
        item?.useBenefit ??
        item?.use_benefit ??
        {};

    const benefit =
        item?.benefitBean ??
        item?.benefit ??
        useBenefit?.benefitBean ??
        useBenefit?.benefit ??
        {};

    return firstValue(
        item,
        [
            "benefitName",
            "benefit_name",
        ],
        firstValue(
            benefit,
            [
                "benefitName",
                "benefit_name",
                "name",
                "title",
            ],
            "未提供",
        ),
    );
}

// 取得特典類型。
function listingBenefitType(item) {
    const useBenefit =
        item?.useBenefitBean ??
        item?.useBenefit ??
        item?.use_benefit ??
        {};

    const benefit =
        item?.benefitBean ??
        item?.benefit ??
        useBenefit?.benefitBean ??
        useBenefit?.benefit ??
        {};

    return firstValue(
        item,
        [
            "benefitType",
            "benefit_type",
        ],
        firstValue(
            benefit,
            [
                "benefitType",
                "benefit_type",
                "type",
            ],
            "",
        ),
    );
}

// 取得刊登日期，只顯示 yyyy-mm-dd。
function listingCreatedDate(item) {
    const value = firstValue(
        item,
        [
            "createdAt",
            "created_at",
            "listingCreatedAt",
            "listing_created_at",
        ],
        "",
    );

    if (!value) return "未提供";

    return String(value).trim().split(/[T ]/)[0] || "未提供";
}

// 取得會員持有特典照片。
// 沿用收藏卡片使用的欄位名稱，最多顯示 5 張。
function memberPhotoCandidates(item) {
    const useBenefit =
        item?.useBenefitBean ??
        item?.useBenefit ??
        item?.use_benefit ??
        {};

    const photos =
        item?.useBenefitPhotoUrls ??
        item?.use_benefit_photo_urls ??
        useBenefit?.useBenefitPhotoUrls ??
        useBenefit?.use_benefit_photo_urls ??
        [];

    if (!Array.isArray(photos)) return [];

    return [
        ...new Set(
            photos
                .filter(url => typeof url === "string")
                .map(url => url.trim())
                .filter(Boolean),
        ),
    ].slice(0, 5);
}

// 取得官方特典照片。
function officialPhotoCandidate(item) {
    const useBenefit =
        item?.useBenefitBean ??
        item?.useBenefit ??
        item?.use_benefit ??
        {};

    const benefit =
        item?.benefitBean ??
        item?.benefit ??
        useBenefit?.benefitBean ??
        useBenefit?.benefit ??
        {};

    const url = firstValue(
        item,
        [
            "officialImageUrl",
            "official_image_url",
        ],
        firstValue(
            benefit,
            [
                "officialImageUrl",
                "official_image_url",
                "imageUrl",
                "image_url",
            ],
            "",
        ),
    );

    return typeof url === "string" ? url.trim() : "";
}

// 有會員持有照片時優先使用；沒有才使用官方照片。
function imageCandidates(item) {
    const memberPhotos = memberPhotoCandidates(item);

    if (memberPhotos.length > 0) {
        return memberPhotos;
    }

    const officialPhoto = officialPhotoCandidate(item);

    return officialPhoto ? [officialPhoto] : [];
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

            <!-- ========== 我的特典刊登：維持原本卡片版型 ========== -->
            <template v-else-if="activeTab === 'exchange'">
                <div class="my-exchange-list">
                    <article v-for="(item, index) in exchangeListings" :key="listingId(item, 'exchange') ?? index"
                        class="favorite-card">
                        <!-- 特典照片 -->
                        <div class="favorite-images">
                            <template v-if="imageCandidates(item).length > 0">
                                <img v-for="(imageUrl, imageIndex) in imageCandidates(item)"
                                    :key="`${imageUrl}-${imageIndex}`" :src="imageUrl"
                                    :alt="`${item.title || '特典刊登'} 的照片 ${imageIndex + 1}`" class="favorite-image"
                                    @error="$event.target.style.display = 'none'" />
                            </template>

                            <div v-else class="no-image" aria-label="沒有特典圖片">
                                <Icon :icon="giftIcon" class="gift-icon" />
                            </div>
                        </div>

                        <!-- 特典刊登內容 -->
                        <div class="favorite-content">
                            <h3 class="favorite-title" :title="item.title || '未命名刊登'">
                                {{ item.title || "未命名刊登" }}
                            </h3>

                            <p class="favorite-member">
                                刊登者：{{ listingMemberName(item) || "未知會員" }}
                            </p>

                            <p class="favorite-benefit">
                                特典名稱：{{ listingBenefitName(item) }}
                                <span v-if="listingBenefitType(item)">
                                    （{{ listingBenefitType(item) }}）
                                </span>
                            </p>

                            <p class="favorite-description">
                                {{ item.description || "刊登者尚未提供敘述。" }}
                            </p>

                            <p class="favorite-date">
                                刊登日期：{{ listingCreatedDate(item) }}
                            </p>

                            <div class="exchange-card-bottom">
                                <span class="status-label">
                                    {{ statusLabel(item.status) }}
                                </span>

                                <!-- 保留原本的修改與狀態變更功能 -->
                                <div class="exchange-card-actions">
                                    <button class="btn btn-outline-primary" type="button"
                                        :disabled="item.status === 'DELETED'" @click="beginEdit(item, 'exchange')">
                                        修改
                                    </button>

                                    <select class="form-select status-select" :value="item.status"
                                        :disabled="item.status === 'DELETED'" aria-label="變更特典刊登狀態" @change="
                                            changeStatus(
                                                item,
                                                'exchange',
                                                $event.target.value
                                            )
                                            ">
                                        <option v-for="option in statusOptions" :key="option.value"
                                            :value="option.value">
                                            {{ option.label }}
                                        </option>
                                    </select>
                                </div>
                            </div>
                        </div>
                    </article>
                </div>
            </template>

            <!-- ========== 我的票券刊登：套用票券收藏卡片版型 ========== -->
            <template v-else>
                <div class="my-ticket-list">
                    <article v-for="(item, index) in ticketListings" :key="listingId(item, 'ticket') ?? index"
                        class="favorite-card ticket-listing-card">
                        <!-- 票券圖示區 -->
                        <div class="favorite-images">
                            <div class="no-image" aria-label="票券圖示">
                                <Icon :icon="ticketIcon" class="ticket-icon" aria-hidden="true" />
                            </div>
                        </div>

                        <!-- 票券刊登內容 -->
                        <div class="favorite-content">
                            <h3 class="favorite-title" :title="item.title || '未命名刊登'">
                                {{ item.title || "未命名刊登" }}
                            </h3>

                            <p class="favorite-member">
                                刊登者：{{ listingMemberName(item) || "未提供" }}
                            </p>

                            <p class="favorite-description ticket-description">
                                {{ item.description || "刊登者尚未提供敘述。" }}
                            </p>

                            <!-- 影城、電影及放映日期 -->
                            <div class="favorite-ticket-details">
                                <p class="favorite-detail-item">
                                    <Icon :icon="cinemaIcon" aria-hidden="true" />

                                    <span class="favorite-detail-label">
                                        影城
                                    </span>

                                    <span class="favorite-detail-value" :title="cinemaName(item)">
                                        {{ cinemaName(item) }}
                                    </span>
                                </p>

                                <p class="favorite-detail-item">
                                    <Icon :icon="filmIcon" aria-hidden="true" />

                                    <span class="favorite-detail-label">
                                        電影
                                    </span>

                                    <span class="favorite-detail-value" :title="movieName(item)">
                                        {{ movieName(item) }}
                                    </span>
                                </p>

                                <p class="favorite-detail-item">
                                    <Icon :icon="calendarIcon" aria-hidden="true" />

                                    <span class="favorite-detail-label">
                                        放映日期
                                    </span>

                                    <span class="favorite-detail-value" :title="dateOnly(
                                        item.ticketShowtime ||
                                        item.ticket_showtime
                                    ) || '未提供'
                                        ">
                                        {{
                                            dateOnly(
                                                item.ticketShowtime ||
                                                item.ticket_showtime
                                            ) || "未提供"
                                        }}
                                    </span>
                                </p>
                            </div>

                            <!-- 卡片底部：保留原本的狀態與管理操作 -->
                            <div class="exchange-card-bottom ticket-card-bottom">
                                <span class="status-label">
                                    {{ statusLabel(item.status) }}
                                </span>

                                <div class="exchange-card-actions">
                                    <button class="btn btn-outline-primary" type="button"
                                        :disabled="item.status === 'DELETED'" @click="beginEdit(item, 'ticket')">
                                        修改
                                    </button>

                                    <select class="form-select status-select" :value="item.status"
                                        :disabled="item.status === 'DELETED'" aria-label="變更票券刊登狀態" @change="
                                            changeStatus(
                                                item,
                                                'ticket',
                                                $event.target.value
                                            )
                                            ">
                                        <option v-for="option in statusOptions" :key="option.value"
                                            :value="option.value">
                                            {{ option.label }}
                                        </option>
                                    </select>
                                </div>
                            </div>
                        </div>
                    </article>
                </div>
            </template>
        </template>

        <!-- 修改刊登彈窗：保留原本功能 -->
        <AppModal v-model="isModalOpen" :title="editingKind === 'exchange'
            ? '修改特典刊登'
            : '修改票券刊登'" :busy="saving" :close-on-backdrop="false" width="42rem">
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
                            )" :value="String(
                                    firstValue(
                                        benefit,
                                        [
                                            'useBenefitId',
                                            'use_benefit_id',
                                            'id'
                                        ]
                                    )
                                )">
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
                        <label class="form-label">
                            放映日期
                        </label>

                        <div class="my-ticket-date-picker">
                            <input type="text" class="form-control my-ticket-date-display" :value="displayTicketDate(
                                ticketForm.ticketShowtime
                            )" placeholder="年/月/日" readonly tabindex="-1" aria-hidden="true" :disabled="saving" />

                            <svg class="my-ticket-date-calendar" xmlns="http://www.w3.org/2000/svg" width="20"
                                height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                                stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <rect x="3" y="5" width="18" height="16" rx="2" />
                                <path d="M16 3v4" />
                                <path d="M8 3v4" />
                                <path d="M3 11h18" />
                            </svg>

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
                        {{ saving ? "儲存中……" : "儲存修改" }}
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

/* ========== 特典卡片：維持原本版型 ========== */

.my-exchange-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

/* ========== 票券卡片清單 ========== */

.my-ticket-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

/* ========== 特典與票券共用卡片外觀 ========== */

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
}

/* 票券卡片保留收藏卡片的滑鼠及鍵盤操作視覺慣例。
   這個管理頁面沒有將整張卡片設為可點擊。 */
.ticket-listing-card {
    cursor: default;
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

.favorite-image {
    width: 100px;
    height: 100px;
    flex-shrink: 0;
    object-fit: cover;
    border-radius: 6px;
    background: #f3f4f6;
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

.gift-icon,
.ticket-icon {
    width: 42px;
    height: 42px;
}

/* ========== 卡片文字區 ========== */

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

.favorite-member,
.favorite-benefit,
.favorite-date {
    flex-shrink: 0;
    overflow: hidden;
    margin: 0 0 2px;
    color: #4b5563;
    font-size: 14px;
    line-height: 1.4;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.favorite-description {
    display: -webkit-box;
    flex: 1;
    min-height: 0;
    overflow: hidden;
    margin: 0 0 6px;
    padding-left: 1em;
    color: #4b5563;
    font-size: 14px;
    line-height: 1.4;
    overflow-wrap: anywhere;
    white-space: pre-wrap;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
}

/* 票券收藏卡片的敘述區。
   保留參考元件的兩行高度，讓下方票券資訊保持整齊。 */
.ticket-description {
    display: -webkit-box;
    flex: 0 0 auto;
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

/* ========== 票券資訊列 ========== */

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

/* ========== 卡片底部：狀態及管理操作 ========== */

.exchange-card-bottom {
    display: flex;
    flex-shrink: 0;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-top: auto;
}

.exchange-card-bottom .status-label {
    flex-shrink: 0;
}

.exchange-card-actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    min-width: 0;
}

.exchange-card-actions .btn {
    min-height: 36px;
    padding: 0.35rem 0.7rem;
}

.exchange-card-actions .status-select {
    width: auto;
    min-width: 112px;
    max-width: 100%;
    min-height: 36px;
    font-size: 0.85rem;
}

/* ========== 狀態標籤 ========== */

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

/* 只調整包含本頁修改表單的 AppModal，不修改共用元件。 */

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

    .favorite-card {
        height: 240px;
        gap: 10px;
        padding: 12px;
    }

    .favorite-images {
        width: 100px;
        flex: 0 0 100px;
    }

    .favorite-image,
    .no-image {
        width: 100px;
        height: 100px;
    }

    .favorite-title {
        font-size: 16px;
    }

    .favorite-member,
    .favorite-benefit,
    .favorite-description,
    .favorite-date {
        font-size: 13px;
    }

    .ticket-description {
        font-size: 12px;
        line-height: 1.5;
    }

    .favorite-detail-item {
        grid-template-columns: 14px auto minmax(0, 1fr);
        gap: 4px;
        font-size: 12px;
    }

    .favorite-detail-item :deep(svg) {
        width: 13px;
        height: 13px;
    }

    .exchange-card-bottom {
        align-items: flex-start;
        flex-direction: column;
        gap: 6px;
    }

    .exchange-card-actions {
        width: 100%;
        gap: 6px;
    }

    .exchange-card-actions .btn {
        flex: 0 0 auto;
    }

    .exchange-card-actions .status-select {
        flex: 1;
        min-width: 0;
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