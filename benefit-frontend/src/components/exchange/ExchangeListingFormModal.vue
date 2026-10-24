<!-- ========== components/exchange/ExchangeListingFormModal.vue ========== -->
<!-- 這個元件接收主頁傳入的表單、持有特典清單、編輯 ID 和儲存狀態。它只負責顯示表單及通知主頁送出或關閉。 -->
<!-- 負責新增、修改交換刊登的彈窗，包含持有特典選擇及圖片預覽。 -->
<!-- 顯示新增／修改交換刊登表單，包括標題、敘述及持有特典選擇。送出時發出事件，由父元件處理實際儲存。 -->

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
    useBenefits: {
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
    getUseBenefitId: {
        type: Function,
        required: true,
    },
    useBenefitTitle: {
        type: Function,
        required: true,
    },
    useBenefitImage: {
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

const selectedUseBenefit = computed(() =>
    props.useBenefits.find(
        (item) =>
            String(props.getUseBenefitId(item)) ===
            String(props.form.useBenefitId),
    ),
);

/**
 * 返回按鈕沿用原本的 close 事件。
 * 儲存期間不可關閉視窗。
 */
function closeModal() {
    if (props.isSaving) return;

    emit("close");
}

/**
 * 票券右上角的 X 按鈕沿用 AppModal 原本的關閉方式：
 * 透過 v-model 通知父元件關閉。
 */
function requestClose() {
    if (props.isSaving) return;

    emit("update:modelValue", false);
}
</script>

<template>
    <AppModal :model-value="modelValue" :title="isEditing ? '修改交換刊登' : '新增特典刊登'" :busy="isSaving"
        :close-on-backdrop="false" width="42rem" @update:model-value="emit('update:modelValue', $event)">
        <div class="exchange-form-modal">
            <form id="exchange-listing-form" class="exchange-listing-form" @submit.prevent="emit('submit')">
                <TicketCard appear color="green" class="exchange-form-ticket" :title="isEditing ? '修改交換刊登' : '新增特典刊登'">
                    <template #title>
                        <div class="exchange-ticket-heading">
                            <span class="exchange-ticket-title">
                                {{ isEditing ? "修改交換刊登" : "新增特典刊登" }}
                            </span>

                            <button type="button" class="exchange-ticket-close" aria-label="關閉" :disabled="isSaving"
                                @click="requestClose">
                                <span aria-hidden="true">×</span>
                            </button>
                        </div>
                    </template>

                    <div class="exchange-form-content">
                        <div class="exchange-form-field">
                            <label for="exchange-title" class="form-label">
                                刊登標題
                            </label>

                            <input id="exchange-title" v-model="form.title" class="form-control" type="text"
                                maxlength="100" required :disabled="isSaving" />
                        </div>

                        <div class="exchange-form-field">
                            <label for="exchange-description" class="form-label">
                                刊登敘述
                            </label>

                            <textarea id="exchange-description" v-model="form.description" class="form-control" rows="4"
                                maxlength="500" :disabled="isSaving" />
                        </div>

                        <div class="exchange-form-field">
                            <label for="use-benefit-select" class="form-label">
                                選擇自己的持有特典
                            </label>

                            <select id="use-benefit-select" v-model="form.useBenefitId" class="form-select" required
                                :disabled="isEditing || isSaving">
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

                            <p v-if="isEditing" class="form-text">
                                修改刊登時，持有特典不可更換。
                            </p>
                        </div>

                        <div v-if="selectedUseBenefit" class="benefit-preview">
                            <img v-if="useBenefitImage(selectedUseBenefit)" :src="useBenefitImage(selectedUseBenefit)"
                                alt="持有特典圖片" />

                            <div v-else class="image-placeholder">
                                尚無圖片
                            </div>

                            <div class="benefit-preview-info">
                                <strong>
                                    {{ useBenefitTitle(selectedUseBenefit) }}
                                </strong>

                                <p>
                                    持有特典編號：
                                    {{ getUseBenefitId(selectedUseBenefit) }}
                                </p>
                            </div>
                        </div>
                    </div>

                    <template #action>
                        <div class="exchange-form-actions">
                            <button class="ticket-link exchange-action-button" type="button" :disabled="isSaving"
                                @click="closeModal">
                                取消
                            </button>

                            <button class="ticket-link is-dark exchange-action-button exchange-submit-button"
                                type="submit" form="exchange-listing-form" :disabled="isSaving">
                                {{
                                    isSaving
                                        ? "儲存中…"
                                        : isEditing
                                            ? "儲存修改"
                                            : "新增特典刊登"
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
/*
 * 表單外層。
 * TicketCard 的票券樣式沿用組員原有設計。
 */
.exchange-form-modal {
    width: 100%;
    min-width: 0;
}

.exchange-listing-form {
    width: 100%;
    min-width: 0;
    margin: 0;
}

/*
 * 票券標題列。
 * 不修改 TicketCard.vue，只調整目前表單內的標題排列。
 */
.exchange-form-modal :deep(.ticket-body h2) {
    width: 100%;
    min-width: 0;
}

.exchange-ticket-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.75rem;
    width: 100%;
    min-width: 0;
}

.exchange-ticket-title {
    flex: 1;
    min-width: 0;
    overflow-wrap: anywhere;
}

.exchange-ticket-close {
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

.exchange-ticket-close:hover:not(:disabled) {
    background: var(--accent);
    color: var(--c-ink);
}

.exchange-ticket-close:focus-visible {
    outline: 2px solid var(--ticket-ink);
    outline-offset: 3px;
}

.exchange-ticket-close:disabled {
    cursor: not-allowed;
    opacity: 0.45;
}

/*
 * 表單內容區。
 */
.exchange-form-content {
    display: flex;
    flex-direction: column;
    gap: 1.1rem;
    min-width: 0;
    padding-top: 0.25rem;
}

.exchange-form-field {
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
    border-color: #f2a58d;
    outline: none;
    box-shadow: 0 0 0 3px rgb(242 165 141 / 22%);
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
 * 已選擇特典的預覽區。
 */
.benefit-preview {
    display: flex;
    align-items: center;
    gap: 1rem;
    min-width: 0;
    margin-top: 0.15rem;
    padding: 0.95rem;
    border: 1px solid #f0ded6;
    border-radius: 0.85rem;
    background: #fff8f4;
}

.benefit-preview img,
.image-placeholder {
    display: grid;
    flex-shrink: 0;
    place-items: center;
    width: 96px;
    height: 96px;
    overflow: hidden;
    border-radius: 0.7rem;
    background: #f3efeb;
}

.benefit-preview img {
    object-fit: cover;
    object-position: center;
}

.image-placeholder {
    padding: 0.4rem;
    border: 1px dashed #d8c9bf;
    color: #7c818b;
    font-size: 0.8rem;
    text-align: center;
}

.benefit-preview-info {
    flex: 1;
    min-width: 0;
}

.benefit-preview-info strong {
    display: block;
    color: #292b35;
    font-size: 0.95rem;
    font-weight: 800;
    line-height: 1.6;
    overflow-wrap: anywhere;
}

.benefit-preview-info p {
    margin: 0.4rem 0 0;
    color: var(--c-muted, #7c818b);
    font-size: 0.82rem;
    line-height: 1.6;
    overflow-wrap: anywhere;
}

/*
 * 底部操作區沿用 TicketCard 的 ticket-stub。
 * 按鈕沿用 TicketCard 的 ticket-link。
 */
.exchange-form-modal :deep(.ticket-stub) {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: flex-end;
    gap: 0.65rem;
    padding: 0.85rem 1.1rem;
}

.exchange-form-actions {
    display: flex;
    flex: 1;
    flex-wrap: wrap;
    align-items: center;
    justify-content: flex-end;
    gap: 0.65rem;
    min-width: 0;
}

.exchange-form-modal :deep(.exchange-action-button) {
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

.exchange-form-modal :deep(.exchange-submit-button) {
    border-color: var(--ticket-ink);
    background: var(--ticket-ink);
    color: #fff;
}

.exchange-form-modal :deep(.exchange-submit-button:hover:not(:disabled)),
.exchange-form-modal :deep(.exchange-submit-button:focus-visible) {
    border-color: var(--ticket-ink);
    background: color-mix(in srgb, var(--ticket-ink) 78%, white);
    color: #fff;
}

.exchange-form-modal :deep(.exchange-action-button:disabled) {
    cursor: not-allowed;
    opacity: 0.45;
}

/*
 * 只調整包含本表單的 AppModal。
 * 使用 :has() 限定目標，不修改 AppModal.vue 的共用樣式。
 */
:global(.modal-dialog-box:has(.exchange-form-modal)) {
    border: none;
    border-radius: 20px;
    background: transparent;
    box-shadow: none;
    overflow: visible;
}

:global(.modal-dialog-box:has(.exchange-form-modal) .modal-head) {
    display: none;
}

:global(.modal-dialog-box:has(.exchange-form-modal) .modal-body) {
    padding: 0;
}

:global(.modal-dialog-box:has(.exchange-form-modal) .modal-foot) {
    display: none;
}

/*
 * 防止固定在畫面中央的票券因滑鼠移入而位移。
 * 只影響本表單，不影響其他 TicketCard。
 */
.exchange-form-modal :deep(.ticket-frame),
.exchange-form-modal :deep(.ticket-frame:hover) {
    filter: drop-shadow(0 8px 12px color-mix(in srgb, var(--c-ink) 20%, transparent));
    transition: none;
}

.exchange-form-modal :deep(.ticket),
.exchange-form-modal :deep(.ticket:hover) {
    min-height: 0;
    transform: none;
}

/*
 * 手機版排版。
 */
@media (max-width: 600px) {
    .exchange-form-content {
        gap: 0.95rem;
    }

    .exchange-form-modal :deep(.ticket-body) {
        padding: 0.9rem 0.85rem 1rem;
    }

    .exchange-ticket-heading {
        gap: 0.5rem;
    }

    .exchange-ticket-close {
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

    .benefit-preview {
        align-items: flex-start;
        gap: 0.75rem;
        padding: 0.75rem;
    }

    .benefit-preview img,
    .image-placeholder {
        width: 76px;
        height: 76px;
    }

    .exchange-form-modal :deep(.ticket-stub) {
        align-items: stretch;
        justify-content: center;
        padding: 0.8rem;
    }

    .exchange-form-actions {
        flex-direction: column;
        align-items: stretch;
        width: 100%;
    }

    .exchange-form-modal :deep(.exchange-action-button) {
        width: 100%;
        min-height: 44px;
    }

    :global(.modal-backdrop:has(.exchange-form-modal)) {
        padding: 0.75rem;
    }

    :global(.modal-dialog-box:has(.exchange-form-modal)) {
        width: 100% !important;
        max-width: 100%;
        max-height: calc(100dvh - 1.5rem);
        overflow-x: hidden;
        overflow-y: auto;
        overscroll-behavior: contain;
    }
}
</style>
