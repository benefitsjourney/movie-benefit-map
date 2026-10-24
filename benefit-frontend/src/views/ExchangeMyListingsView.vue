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
    return kind === "exchange"
        ? firstValue(item, ["exchangeListingId", "exchange_listing_id", "id"])
        : firstValue(item, ["ticketListingId", "ticket_listing_id", "id"]);
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

function cinemaId(item) {
    return firstValue(item, ["cinemaId", "cinema_id", "id"]);
}

function movieId(item) {
    return firstValue(item, ["movieId", "movie_id", "id"]);
}

function cinemaName(item) {
    const nested = item?.cinemaBean ?? item?.cinema;

    return firstValue(
        item,
        ["cinemaName", "cinema_name"],
        firstValue(nested, ["cinemaName", "name"], "影城資料未提供"),
    );
}

function movieName(item) {
    const nested = item?.movieBean ?? item?.movie;

    return firstValue(
        item,
        ["movieName", "movie_name"],
        firstValue(nested, ["movieName", "name"], "電影資料未提供"),
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

function dateOnly(value) {
    return value ? String(value).slice(0, 10) : "";
}

function toLocalDateTime(value) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(String(value ?? ""))) {
        throw new Error("放映日期格式錯誤，請重新選擇日期。");
    }

    return `${value} 00:00:00`;
}

async function loadListings() {
    if (!token.value) {
        errorMessage.value = "請先登入，才能查看自己的刊登。";
        return;
    }

    loading.value = true;
    errorMessage.value = "";

    const results = await Promise.allSettled([
        getMyExchangeListings(token.value),
        getMyTicketListings(token.value),
    ]);

    const failures = [];

    if (results[0].status === "fulfilled") {
        exchangeListings.value = unwrapList(results[0].value);
    } else {
        failures.push(`我的交換刊登：${errorText(results[0].reason)}`);
    }

    if (results[1].status === "fulfilled") {
        ticketListings.value = unwrapList(results[1].value);
    } else {
        failures.push(`我的票券刊登：${errorText(results[1].reason)}`);
    }

    if (failures.length) errorMessage.value = failures.join("\n");

    loading.value = false;
}

async function loadFormOptions() {
    const results = await Promise.allSettled([
        getCinemas(),
        getMovies(),
        token.value ? getMyUseBenefits(token.value) : Promise.resolve([]),
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

    if (!editingId.value) {
        Swal.fire("無法修改", "找不到這筆刊登的 ID。", "error");
        return;
    }

    if (kind === "exchange") {
        Object.assign(exchangeForm, {
            title: item?.title ?? "",
            description: item?.description ?? "",
            useBenefitId: String(
                firstValue(item, ["useBenefitId", "use_benefit_id"], ""),
            ),
        });
    } else {
        Object.assign(ticketForm, {
            title: item?.title ?? "",
            description: item?.description ?? "",
            cinemaId: String(firstValue(item, ["cinemaId", "cinema_id"], "")),
            movieId: String(firstValue(item, ["movieId", "movie_id"], "")),
            ticketShowtime: dateOnly(
                firstValue(item, ["ticketShowtime", "ticket_showtime"], ""),
            ),
        });
    }

    isModalOpen.value = true;
}

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
                    description: exchangeForm.description.trim(),
                    useBenefitId: Number(exchangeForm.useBenefitId),
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
                throw new Error("請完整填寫標題、影城、電影及放映日期。");
            }

            await updateTicketListing(
                editingId.value,
                {
                    title: ticketForm.title.trim(),
                    description: ticketForm.description.trim(),
                    cinemaId: Number(ticketForm.cinemaId),
                    movieId: Number(ticketForm.movieId),
                    ticketShowtime: toLocalDateTime(ticketForm.ticketShowtime),
                },
                token.value,
            );
        }

        isModalOpen.value = false;
        resetForms();

        await loadListings();
        await Swal.fire("完成", "刊登修改成功。", "success");
    } catch (error) {
        await Swal.fire("修改失敗", errorText(error), "error");
    } finally {
        saving.value = false;
    }
}

async function changeStatus(item, kind, status) {
    if (!token.value) return;

    const id = listingId(item, kind);

    if (!id) {
        await Swal.fire("無法修改", "找不到這筆刊登的 ID。", "error");
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
            await updateExchangeListingStatus(id, status, token.value);
        } else {
            await updateTicketListingStatus(id, status, token.value);
        }

        await loadListings();
        await Swal.fire("完成", "刊登狀態已更新。", "success");
    } catch (error) {
        await Swal.fire("修改失敗", errorText(error), "error");
    }
}

onMounted(async () => {
    refreshAuth();

    if (token.value) {
        await Promise.all([loadListings(), loadFormOptions()]);
    }
});
</script>

<template>
    <section class="my-listings">
        <header class="page-heading">
            <div>
                <h2>我的刊登</h2>
                <p>查看及管理自己刊登的特典與票券。</p>
            </div>

            <button class="btn btn-outline-secondary" type="button" :disabled="loading" @click="loadListings">
                重新載入
            </button>
        </header>

        <div v-if="!isLoggedIn" class="empty-state">
            請先登入，再查看自己的刊登。
        </div>

        <template v-else>
            <div v-if="errorMessage" class="alert alert-warning" role="alert">
                {{ errorMessage }}
            </div>

            <nav class="listing-tabs" aria-label="我的刊登分類">
                <button type="button" :class="{ active: activeTab === 'exchange' }" @click="activeTab = 'exchange'">
                    我的交換刊登
                </button>

                <button type="button" :class="{ active: activeTab === 'ticket' }" @click="activeTab = 'ticket'">
                    我的票券刊登
                </button>
            </nav>

            <p v-if="loading">資料載入中……</p>

            <div v-else-if="!currentListings.length" class="empty-state">
                目前沒有{{ activeTab === "exchange" ? "交換" : "票券" }}刊登。
            </div>

            <article v-for="item in currentListings" :key="listingId(item, activeTab)" class="listing-row">
                <div class="listing-main">
                    <h3>{{ item.title || "未命名刊登" }}</h3>
                    <p>{{ item.description || "沒有補充敘述" }}</p>

                    <p v-if="activeTab === 'ticket'" class="listing-meta">
                        {{ cinemaName(item) }}・{{ movieName(item) }}・
                        {{ dateOnly(item.ticketShowtime || item.ticket_showtime) || "未提供日期" }}
                    </p>

                    <span class="status-label">{{ statusLabel(item.status) }}</span>
                </div>

                <div class="listing-actions">
                    <button class="btn btn-outline-primary" type="button" :disabled="item.status === 'DELETED'"
                        @click="beginEdit(item, activeTab)">
                        修改
                    </button>

                    <select class="form-select" :value="item.status" :disabled="item.status === 'DELETED'"
                        aria-label="變更刊登狀態" @change="changeStatus(item, activeTab, $event.target.value)">
                        <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                            {{ option.label }}
                        </option>
                    </select>
                </div>
            </article>
        </template>

        <AppModal v-model="isModalOpen" :title="editingKind === 'exchange' ? '修改交換刊登' : '修改票券刊登'" :busy="saving"
            :close-on-backdrop="false" width="42rem">
            <form id="my-listing-edit-form" @submit.prevent="saveEdit">
                <div class="mb-3">
                    <template v-if="editingKind === 'exchange'">
                        <div class="mb-3">
                            <label for="my-exchange-title" class="form-label">
                                刊登標題
                            </label>
                            <input id="my-exchange-title" v-model="exchangeForm.title" class="form-control"
                                maxlength="100" required :disabled="saving" />
                        </div>

                        <div class="mb-3">
                            <label for="my-exchange-description" class="form-label">
                                刊登敘述
                            </label>
                            <textarea id="my-exchange-description" v-model="exchangeForm.description"
                                class="form-control" rows="4" maxlength="500" :disabled="saving" />
                        </div>
                    </template>

                    <template v-else>
                        <div class="mb-3">
                            <label for="my-ticket-title" class="form-label">
                                刊登標題
                            </label>
                            <input id="my-ticket-title" v-model="ticketForm.title" class="form-control" maxlength="100"
                                required :disabled="saving" />
                        </div>

                        <div class="mb-3">
                            <label for="my-ticket-description" class="form-label">
                                刊登敘述
                            </label>
                            <textarea id="my-ticket-description" v-model="ticketForm.description" class="form-control"
                                rows="4" maxlength="500" :disabled="saving" />
                        </div>
                    </template>
                </div>

                <template v-if="editingKind === 'exchange'">
                    <div class="mb-3">
                        <label for="my-listing-benefit" class="form-label">持有特典</label>
                        <select id="my-listing-benefit" v-model="exchangeForm.useBenefitId" class="form-select" required
                            disabled>
                            <option value="">請選擇持有特典</option>
                            <option v-for="benefit in useBenefits"
                                :key="firstValue(benefit, ['useBenefitId', 'use_benefit_id', 'id'])"
                                :value="String(firstValue(benefit, ['useBenefitId', 'use_benefit_id', 'id']))">
                                {{ firstValue(benefit, ['title', 'benefitName', 'name'], '持有特典') }}
                            </option>
                        </select>
                        <p class="form-text">修改刊登時，持有特典不可更換。</p>
                    </div>
                </template>

                <template v-else>
                    <div class="mb-3">
                        <label for="my-listing-cinema" class="form-label">影城</label>
                        <select id="my-listing-cinema" v-model="ticketForm.cinemaId" class="form-select" required
                            :disabled="saving">
                            <option value="" disabled>請選擇影城</option>
                            <option v-for="cinema in cinemas" :key="cinemaId(cinema)" :value="String(cinemaId(cinema))">
                                {{ optionCinemaName(cinema) }}
                            </option>
                        </select>
                    </div>

                    <div class="mb-3">
                        <label for="my-listing-movie" class="form-label">電影</label>
                        <select id="my-listing-movie" v-model="ticketForm.movieId" class="form-select" required
                            :disabled="saving">
                            <option value="" disabled>請選擇電影</option>
                            <option v-for="movie in movies" :key="movieId(movie)" :value="String(movieId(movie))">
                                {{ optionMovieName(movie) }}
                            </option>
                        </select>
                    </div>

                    <div class="mb-3">
                        <label for="my-listing-showtime" class="form-label">放映日期</label>
                        <input id="my-listing-showtime" v-model="ticketForm.ticketShowtime" class="form-control"
                            type="date" required :disabled="saving" />
                    </div>
                </template>
            </form>

            <template #footer>
                <div class="form-actions">
                    <button class="btn btn-outline-secondary" type="button" :disabled="saving"
                        @click="isModalOpen = false; resetForms()">
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
.my-listings {
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

.listing-tabs {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    margin-bottom: 1rem;
}

.listing-tabs button {
    padding: 0.65rem 0.9rem;
    border: 1px solid #e8e5e1;
    border-radius: 0.65rem;
    background: #fff;
    cursor: pointer;
}

.listing-tabs button.active {
    border-color: #ff7049;
    background: #fff0e9;
    color: #9a4632;
}

.listing-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 1rem;
    margin-bottom: 0.85rem;
    padding: 1rem;
    border: 1px solid #e8e5e1;
    border-radius: 0.85rem;
    background: #fff;
}

.listing-main {
    min-width: 0;
    flex: 1;
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
}

.listing-meta {
    font-size: 0.85rem;
}

.status-label {
    display: inline-block;
    padding: 0.25rem 0.65rem;
    border-radius: 999px;
    background: #f1f5f9;
    color: #475569;
    font-size: 0.8rem;
}

.listing-actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.5rem;
}

.listing-actions .form-select {
    width: auto;
    min-width: 120px;
}

.empty-state {
    padding: 1.5rem 1rem;
    border: 1px dashed #d9d2cc;
    border-radius: 0.8rem;
    background: #faf8f6;
    color: #687386;
    text-align: center;
}

.form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.6rem;
}

@media (max-width: 650px) {

    .page-heading,
    .listing-row {
        align-items: stretch;
        flex-direction: column;
    }

    .listing-actions .form-select {
        flex: 1;
    }

    .form-actions {
        flex-direction: column;
    }

    .form-actions .btn {
        width: 100%;
    }
}
</style>