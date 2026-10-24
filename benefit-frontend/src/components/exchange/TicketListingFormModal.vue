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
</script>

<template>
    <AppModal :model-value="modelValue" :title="isEditing ? '修改票券刊登' : '新增票券刊登'" :busy="isSaving"
        :close-on-backdrop="false" width="42rem" @update:model-value="emit('update:modelValue', $event)">
        <div class="ticket-form-modal">
            <form id="ticket-listing-form" class="ticket-listing-form" @submit.prevent="emit('submit')">
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

                    <template #detail>
                        <span>票券交換</span>
                    </template>

                    <div class="ticket-form-content">
                        <div class="ticket-form-field">
                            <label for="ticket-title" class="form-label">
                                刊登標題
                            </label>

                            <input id="ticket-title" v-model="form.title" class="form-control" type="text"
                                maxlength="100" required :disabled="isSaving" />
                        </div>

                        <div class="ticket-form-field">
                            <label for="ticket-description" class="form-label">
                                刊登敘述
                            </label>

                            <textarea id="ticket-description" v-model="form.description" class="form-control" rows="4"
                                maxlength="500" :disabled="isSaving" />
                        </div>

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

                        <div class="ticket-form-field">
                            <label for="ticket-date" class="form-label">
                                放映日期
                            </label>

                            <input id="ticket-date" v-model="form.ticketShowtime" class="form-control" type="date"
                                required :disabled="isSaving" />

                            <p class="form-text">
                                請選擇票券的放映日期。送出時仍由原本的父元件處理日期格式及 API 請求。
                            </p>
                        </div>

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
</style>
