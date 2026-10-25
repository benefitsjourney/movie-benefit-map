<!-- ========== components/exchange/TicketListingFormModal.vue ========== -->
<!-- 負責新增、修改票券刊登的彈窗，包含影城、電影及放映日期欄位。 -->
<!-- 顯示新增／修改票券表單，包含標題、敘述、影城、電影及放映日期。 -->

<script setup>
import { computed } from "vue";
import AppModal from "@/components/ui/AppModal.vue";
import TicketCard from "@/components/ui/TicketCard.vue";

const props = defineProps({
    modelValue: {
        type: Boolean,
        required: true,
    },
    form: {
        type: Object,
        required: true,
    },
    cinemas: {
        type: Array,
        default: () => [],
    },
    movies: {
        type: Array,
        default: () => [],
    },
    editingId: {
        type: [Number, String],
        default: null,
    },
    isSaving: {
        type: Boolean,
        default: false,
    },
    cinemaId: {
        type: Function,
        required: true,
    },
    movieId: {
        type: Function,
        required: true,
    },
    cinemaOptionName: {
        type: Function,
        required: true,
    },
    movieOptionName: {
        type: Function,
        required: true,
    },
});

const emit = defineEmits([
    "update:modelValue",
    "submit",
    "close",
]);

const isEditing = computed(() => props.editingId !== null);

const selectedCinema = computed(() =>
    props.cinemas.find(
        (cinema) =>
            String(props.cinemaId(cinema)) ===
            String(props.form.cinemaId),
    ),
);

const selectedMovie = computed(() =>
    props.movies.find(
        (movie) =>
            String(props.movieId(movie)) ===
            String(props.form.movieId),
    ),
);

function closeModal() {
    if (props.isSaving) return;

    emit("close");
}

function requestClose() {
    if (props.isSaving) return;

    emit("update:modelValue", false);
}

// ========== 放映日期顯示格式 ==========
// 只調整畫面顯示，不修改 form.ticketShowtime 的實際資料。
function displayTicketDate(value) {
    if (!value) return "";

    const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})$/);

    if (!match) return value;

    return `${match[1]}/${match[2]}/${match[3]}`;
}
// ========== 放映日期限制 ==========

// 取得台灣當地日期，格式為 YYYY-MM-DD。
// 不直接使用 toISOString()，避免 UTC 時區造成日期偏差。
function getTodayDate() {
    const now = new Date();

    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");

    return `${year}-${month}-${day}`;
}

// 今天不能選，因此最早可選日期是明天。
function getMinTicketDate() {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);

    const year = tomorrow.getFullYear();
    const month = String(tomorrow.getMonth() + 1).padStart(2, "0");
    const day = String(tomorrow.getDate()).padStart(2, "0");

    return `${year}-${month}-${day}`;
}
// ========== 點擊日期欄位時開啟日期選擇器 ==========
function openDatePicker(event) {
    if (props.isSaving) return;

    const input = event.currentTarget;

    if (!input) return;

    // 優先使用瀏覽器提供的原生日期選擇器。
    if (typeof input.showPicker === "function") {
        try {
            input.showPicker();
            return;
        } catch {
            // 如果瀏覽器不允許程式開啟，
            // 就保留原生日期欄位的操作方式。
        }
    }

    // 不支援 showPicker() 時，仍讓日期欄位取得焦點。
    input.focus();
}
// ========== 表單送出前檢查 ==========

function handleSubmit(event) {
    // 修改既有刊登時，維持原本的送出流程。
    if (isEditing.value) {
        emit("submit");
        return;
    }

    const selectedDate = String(props.form.ticketShowtime ?? "");

    // 新增刊登時，不允許選擇今天或過去的日期。
    if (!selectedDate || selectedDate <= getTodayDate()) {
        const dateInput = event.currentTarget.querySelector("#ticket-date");

        if (dateInput) {
            dateInput.setCustomValidity("放映日期必須選擇明天或之後的日期。");
            dateInput.reportValidity();
        }

        return;
    }

    // 日期合法後，清除可能存在的自訂驗證訊息。
    const dateInput = event.currentTarget.querySelector("#ticket-date");

    if (dateInput) {
        dateInput.setCustomValidity("");
    }

    emit("submit");
}
</script>

<template>
    <AppModal :model-value="modelValue" :title="isEditing ? '修改票券刊登' : '新增票券刊登'" :busy="isSaving"
        :close-on-backdrop="false" width="42rem" @update:model-value="emit('update:modelValue', $event)">
        <div class="ticket-form-modal">
            <form id="ticket-listing-form" class="ticket-listing-form" @submit.prevent="handleSubmit">
                <TicketCard appear color="blue" class="ticket-form-card" :title="isEditing ? '修改票券刊登' : '新增票券刊登'">
                    <template #title>
                        <div class="ticket-form-heading">
                            <span class="ticket-form-title">
                                {{ isEditing ? "修改票券刊登" : "新增票券刊登" }}
                            </span>

                            <button type="button" class="ticket-form-close" aria-label="關閉" :disabled="isSaving"
                                @click="requestClose">
                                <span aria-hidden="true">×</span>
                            </button>
                        </div>
                    </template>

                    <div class="ticket-form-content">
                        <!-- 刊登標題 -->
                        <div class="ticket-form-field">
                            <label for="ticket-title" class="form-label">
                                刊登標題
                            </label>

                            <input id="ticket-title" v-model="form.title" class="form-control" type="text"
                                maxlength="100" required :disabled="isSaving" />
                        </div>

                        <!-- 刊登敘述 -->
                        <div class="ticket-form-field">
                            <label for="ticket-description" class="form-label">
                                刊登敘述
                            </label>

                            <textarea id="ticket-description" v-model="form.description" class="form-control" rows="4"
                                maxlength="500" :disabled="isSaving" />
                        </div>

                        <!-- 影城名稱 -->
                        <div class="ticket-form-field">
                            <label for="cinema-select" class="form-label">
                                影城名稱
                            </label>

                            <select id="cinema-select" v-model="form.cinemaId" class="form-select" required
                                :disabled="isSaving">
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

                        <!-- 電影名稱 -->
                        <div class="ticket-form-field">
                            <label for="movie-select" class="form-label">
                                電影名稱
                            </label>

                            <select id="movie-select" v-model="form.movieId" class="form-select" required
                                :disabled="isSaving">
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

                        <!-- 放映日期 -->
                        <div class="ticket-form-field">
                            <!-- 不使用 for，避免點擊標籤時觸發日期選擇器 -->
                            <label class="form-label">
                                放映日期
                            </label>

                            <div class="ticket-date-picker">
                                <!--
                                    顯示用欄位：
                                    只顯示日期文字，不直接操作原生日期控制項。
                                -->
                                <input type="text" class="form-control ticket-date-display"
                                    :value="displayTicketDate(form.ticketShowtime)" placeholder="年/月/日" readonly
                                    tabindex="-1" aria-hidden="true" :disabled="isSaving" />

                                <!-- 日曆圖示：固定顯示在日期欄位右側 -->
                                <svg class="ticket-date-calendar" xmlns="http://www.w3.org/2000/svg" width="20"
                                    height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                                    stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <rect x="3" y="5" width="18" height="16" rx="2" />
                                    <path d="M16 3v4" />
                                    <path d="M8 3v4" />
                                    <path d="M3 11h18" />
                                </svg>

                                <!--
                                    真正負責日期輸入的欄位：
                                    透明覆蓋整個日期顯示區域。
                                    form.ticketShowtime 仍由此欄位維護。
                                -->
                                <input id="ticket-date" v-model="form.ticketShowtime" class="ticket-date-native"
                                    type="date" aria-label="選擇放映日期" required
                                    :min="isEditing ? undefined : getMinTicketDate()" :disabled="isSaving"
                                    @click="openDatePicker" @change="$event.target.setCustomValidity('')" />
                            </div>

                            <p class="form-text" style="color: #B22222;">
                                請選擇明天或之後的放映日期，今天及過去的日期皆不可選擇。
                            </p>
                        </div>

                        <!-- 刊登資訊預覽 -->
                        <div v-if="selectedCinema || selectedMovie || form.ticketShowtime" class="ticket-preview">
                            <div class="ticket-preview-heading">
                                刊登資訊預覽
                            </div>

                            <div v-if="selectedCinema" class="ticket-preview-row">
                                <span class="ticket-preview-label">影城</span>

                                <span class="ticket-preview-value">
                                    {{ cinemaOptionName(selectedCinema) }}
                                </span>
                            </div>

                            <div v-if="selectedMovie" class="ticket-preview-row">
                                <span class="ticket-preview-label">電影</span>

                                <span class="ticket-preview-value">
                                    {{ movieOptionName(selectedMovie) }}
                                </span>
                            </div>

                            <div v-if="form.ticketShowtime" class="ticket-preview-row">
                                <span class="ticket-preview-label">放映日期</span>

                                <span class="ticket-preview-value">
                                    {{ form.ticketShowtime }}
                                </span>
                            </div>
                        </div>
                    </div>

                    <template #action>
                        <div class="ticket-form-actions">
                            <button class="ticket-link ticket-form-action-button" type="button" :disabled="isSaving"
                                @click="closeModal">
                                取消
                            </button>

                            <button class="ticket-link is-dark ticket-form-action-button ticket-submit-button"
                                type="submit" form="ticket-listing-form" :disabled="isSaving">
                                {{
                                    isSaving
                                        ? "儲存中…"
                                        : isEditing
                                            ? "儲存修改"
                                            : "新增票券刊登"
                                }}
                            </button>
                        </div>
                    </template>
                </TicketCard>
            </form>
        </div>
    </AppModal>
</template>

<style scoped>
.ticket-form-modal {
    width: 100%;
    min-width: 0;
}

.ticket-listing-form {
    width: 100%;
    min-width: 0;
    margin: 0;
}

/*
 * 標題列沿用 TicketCard 的票券標題結構。
 * 只調整本表單，不修改共用 TicketCard.vue。
 */
.ticket-form-modal :deep(.ticket-body h2) {
    width: 100%;
    min-width: 0;
}

.ticket-form-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.75rem;
    width: 100%;
    min-width: 0;
}

.ticket-form-title {
    flex: 1;
    min-width: 0;
    overflow-wrap: anywhere;
}

.ticket-form-close {
    display: inline-grid;
    flex-shrink: 0;
    place-items: center;
    width: 2.25rem;
    height: 2.25rem;
    padding: 0;
    border: 1.5px solid var(--ticket-ink);
    border-radius: 0.7rem;
    background: #fff;
    color: var(--ticket-ink);
    font-family: inherit;
    font-size: 1.65rem;
    font-weight: 500;
    line-height: 1;
    cursor: pointer;
    transition:
        background-color 0.2s ease,
        color 0.2s ease,
        opacity 0.2s ease;
}

.ticket-form-close:hover:not(:disabled) {
    background: var(--accent);
    color: var(--c-ink);
}

.ticket-form-close:focus-visible {
    outline: 2px solid var(--ticket-ink);
    outline-offset: 3px;
}

.ticket-form-close:disabled {
    cursor: not-allowed;
    opacity: 0.45;
}

/*
 * 表單欄位。
 */
.ticket-form-content {
    display: flex;
    flex-direction: column;
    gap: 1.1rem;
    min-width: 0;
    padding-top: 0.25rem;
}

.ticket-form-field {
    display: flex;
    flex-direction: column;
    min-width: 0;
}

.form-label {
    display: block;
    margin-bottom: 0.5rem;
    color: #292b35;
    font-size: 0.9rem;
    font-weight: 750;
    line-height: 1.6;
}

.form-control,
.form-select {
    display: block;
    width: 100%;
    min-width: 0;
    min-height: 44px;
    padding: 0.65rem 0.8rem;
    border: 1px solid #ded8d2;
    border-radius: 0.65rem;
    background: #fff;
    color: #292b35;
    font-family: inherit;
    font-size: 0.92rem;
    line-height: 1.5;
    transition:
        border-color 0.2s ease,
        box-shadow 0.2s ease;
}

/*
 * 放映日期選擇區域。
 * 外層容器提供定位基準，原生日期欄位覆蓋整個區域。
 */
.ticket-date-picker {
    position: relative;
    width: 100%;
    min-width: 0;
    min-height: 48px;
    border-radius: 0.65rem;
}

/*
 * 顯示用日期欄位。
 * 使用一般文字顯示日期，避免直接選取原生日期欄位中的年月日。
 */
.ticket-date-display {
    min-height: 48px;
    padding-right: 3rem;
    cursor: pointer;
    user-select: none;
    -webkit-user-select: none;
    caret-color: transparent;
}

/*
 * 顯示用欄位不可編輯，因此不讓使用者選取日期文字。
 */
.ticket-date-display::selection {
    background: transparent;
}

/*
 * 顯示用欄位的日曆圖示。
 * 圖示不攔截點擊事件，點擊仍由透明的原生日期欄位接收。
 */
.ticket-date-calendar {
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

/*
 * 真正的原生日期欄位。
 * 透明但仍可接收滑鼠、觸控與鍵盤操作。
 */
.ticket-date-native {
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

/*
 * 焦點狀態：
 * 原生日期欄位取得焦點時，讓底下可見的顯示欄位呈現焦點樣式。
 */
.ticket-date-picker:focus-within .ticket-date-display {
    border-color: #a9c4f0;
    outline: none;
    box-shadow: 0 0 0 3px rgb(169 196 240 / 28%);
}

/*
 * 儲存中停用日期欄位。
 */
.ticket-date-picker:has(.ticket-date-native:disabled) {
    cursor: not-allowed;
}

.ticket-date-display:disabled {
    background: #f5f2ef;
    color: #7c818b;
    cursor: not-allowed;
}

.ticket-date-native:disabled {
    cursor: not-allowed;
}

.form-control:focus,
.form-select:focus {
    border-color: #a9c4f0;
    outline: none;
    box-shadow: 0 0 0 3px rgb(169 196 240 / 28%);
}

.form-control:disabled,
.form-select:disabled {
    background: #f5f2ef;
    color: #7c818b;
    cursor: not-allowed;
}

textarea.form-control {
    min-height: 112px;
    resize: vertical;
}

.form-text {
    margin: 0.45rem 0 0;
    color: var(--c-muted, #7c818b);
    font-size: 0.82rem;
    line-height: 1.6;
    overflow-wrap: anywhere;
}

/*
 * 已選擇的影城、電影及日期預覽。
 */
.ticket-preview {
    display: flex;
    flex-direction: column;
    gap: 0.65rem;
    min-width: 0;
    margin-top: 0.15rem;
    padding: 0.95rem;
    border: 1px solid #dce7f8;
    border-radius: 0.85rem;
    background: #f4f8ff;
}

.ticket-preview-heading {
    margin-bottom: 0.15rem;
    color: #2157a8;
    font-size: 0.92rem;
    font-weight: 850;
    line-height: 1.6;
}

.ticket-preview-row {
    display: flex;
    align-items: flex-start;
    gap: 0.75rem;
    min-width: 0;
    font-size: 0.86rem;
    line-height: 1.6;
}

.ticket-preview-label {
    flex-shrink: 0;
    min-width: 4.5rem;
    color: #65748a;
    font-weight: 700;
}

.ticket-preview-value {
    flex: 1;
    min-width: 0;
    color: #292b35;
    overflow-wrap: anywhere;
}

/*
 * 底部操作區沿用 TicketCard 的 ticket-stub 與 ticket-link。
 */
.ticket-form-modal :deep(.ticket-stub) {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: flex-end;
    gap: 0.65rem;
    padding: 0.85rem 1.1rem;
}

.ticket-form-actions {
    display: flex;
    flex: 1;
    flex-wrap: wrap;
    align-items: center;
    justify-content: flex-end;
    gap: 0.65rem;
    min-width: 0;
}

.ticket-form-modal :deep(.ticket-form-action-button) {
    justify-content: center;
    min-height: 42px;
    padding: 0.6rem 1rem;
    font-family: inherit;
    font-size: 0.88rem;
    font-weight: 750;
    line-height: 1.4;
    text-align: center;
    white-space: normal;
    overflow-wrap: anywhere;
}

.ticket-form-modal :deep(.ticket-submit-button) {
    border-color: var(--ticket-ink);
    background: var(--ticket-ink);
    color: #fff;
}

.ticket-form-modal :deep(.ticket-submit-button:hover:not(:disabled)),
.ticket-form-modal :deep(.ticket-submit-button:focus-visible) {
    border-color: var(--ticket-ink);
    background: color-mix(in srgb, var(--ticket-ink) 78%, white);
    color: #fff;
}

.ticket-form-modal :deep(.ticket-form-action-button:disabled) {
    cursor: not-allowed;
    opacity: 0.45;
}

/*
 * 只調整包含本表單的 AppModal。
 * 不改動 AppModal.vue 的共用程式碼。
 */
:global(.modal-dialog-box:has(.ticket-form-modal)) {
    border: none;
    border-radius: 20px;
    background: transparent;
    box-shadow: none;
    overflow: visible;
}

:global(.modal-dialog-box:has(.ticket-form-modal) .modal-head) {
    display: none;
}

:global(.modal-dialog-box:has(.ticket-form-modal) .modal-body) {
    padding: 0;
}

:global(.modal-dialog-box:has(.ticket-form-modal) .modal-foot) {
    display: none;
}

/*
 * 停用彈窗中不必要的票券 hover 位移。
 * 其他頁面使用的 TicketCard 不受影響。
 */
.ticket-form-modal :deep(.ticket-frame),
.ticket-form-modal :deep(.ticket-frame:hover) {
    filter: drop-shadow(0 8px 12px color-mix(in srgb, var(--c-ink) 20%, transparent));
    transition: none;
}

.ticket-form-modal :deep(.ticket),
.ticket-form-modal :deep(.ticket:hover) {
    min-height: 0;
    transform: none;
}

/*
 * 手機版排版。
 */
@media (max-width: 600px) {
    .ticket-form-content {
        gap: 0.95rem;
    }

    .ticket-form-modal :deep(.ticket-body) {
        padding: 0.9rem 0.85rem 1rem;
    }

    .ticket-form-heading {
        gap: 0.5rem;
    }

    .ticket-form-close {
        width: 2rem;
        height: 2rem;
    }

    .form-control,
    .form-select {
        font-size: 16px;
    }

    .ticket-date-display {
        font-size: 16px;
    }

    textarea.form-control {
        min-height: 100px;
    }

    .ticket-preview {
        padding: 0.75rem;
    }

    .ticket-preview-row {
        flex-wrap: wrap;
        gap: 0.25rem 0.65rem;
    }

    .ticket-form-modal :deep(.ticket-stub) {
        align-items: stretch;
        justify-content: center;
        padding: 0.8rem;
    }

    .ticket-form-actions {
        flex-direction: column;
        align-items: stretch;
        width: 100%;
    }

    .ticket-form-modal :deep(.ticket-form-action-button) {
        width: 100%;
        min-height: 44px;
    }

    :global(.modal-backdrop:has(.ticket-form-modal)) {
        padding: 0.75rem;
    }

    :global(.modal-dialog-box:has(.ticket-form-modal)) {
        width: 100% !important;
        max-width: 100%;
        max-height: calc(100dvh - 1.5rem);
        overflow-x: hidden;
        overflow-y: auto;
        overscroll-behavior: contain;
    }
}

/*
 * 平板版排版。
 * 不修改 AppModal.vue，只調整包含票券表單的彈窗。
 */
@media (min-width: 601px) and (max-width: 900px) {
    :global(.modal-backdrop:has(.ticket-form-modal)) {
        padding: 1rem;
    }

    :global(.modal-dialog-box:has(.ticket-form-modal)) {
        width: min(100%, 42rem) !important;
        max-width: calc(100vw - 2rem);
        max-height: calc(100dvh - 2rem);
        overflow-x: hidden;
        overflow-y: auto;
        overscroll-behavior: contain;
    }

    .ticket-form-modal :deep(.ticket-body) {
        padding: 1.1rem 1rem;
    }

    .ticket-form-content {
        gap: 1rem;
    }

    .form-control,
    .form-select {
        min-height: 46px;
    }

    .ticket-date-picker,
    .ticket-date-display {
        min-height: 48px;
    }

    .ticket-date-display {
        font-size: 16px;
    }

    .ticket-form-modal :deep(.ticket-stub) {
        flex-wrap: wrap;
        gap: 0.65rem;
    }

    .ticket-form-actions {
        flex-wrap: wrap;
        gap: 0.65rem;
    }
}
</style>