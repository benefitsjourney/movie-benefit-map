<!-- ========== components/exchange/ExchangeListingFormModal.vue ========== -->
<!-- 負責新增、修改特典刊登的彈窗，包含持有特典選擇及圖片預覽。 -->
<script setup>
import { computed, ref, watch, onBeforeUnmount } from "vue";
import AppModal from "@/components/ui/AppModal.vue";
import TicketCard from "@/components/ui/TicketCard.vue";
import { Icon } from "@iconify/vue";
import giftIcon from "@iconify-icons/mdi/gift-outline";

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

// 目前預覽圖片的網址。初始優先使用 useBenefitImage() 回傳的會員持有照片。
const previewImageUrl = ref("");

// 是否開啟圖片放大預覽視窗。
const isImagePreviewOpen = ref(false);

// 切換持有特典時，重新設定預覽圖片，並關閉上一張特典的放大預覽。
watch(
    selectedUseBenefit,
    (item) => {
        isImagePreviewOpen.value = false;

        previewImageUrl.value = item
            ? props.useBenefitImage(item) || ""
            : "";
    },
    { immediate: true },
);

// 圖片載入失敗時：
// 1. 優先嘗試官方特典圖片。
// 2. 如果官方圖片也失敗，或沒有官方圖片，
//    清空網址並顯示圖片佔位區塊。
function handleBenefitImageError() {
    const item = selectedUseBenefit.value;

    if (!item) {
        previewImageUrl.value = "";
        isImagePreviewOpen.value = false;
        return;
    }

    const officialImageUrl =
        item.officialImageUrl ||
        item.official_image_url ||
        "";

    if (
        officialImageUrl &&
        previewImageUrl.value !== officialImageUrl
    ) {
        previewImageUrl.value = officialImageUrl;
        return;
    }

    previewImageUrl.value = "";
    isImagePreviewOpen.value = false;
}

// 開啟圖片放大預覽。沒有可用圖片時不開啟視窗。
function openImagePreview() {
    if (!previewImageUrl.value) return;

    isImagePreviewOpen.value = true;
}

// 關閉圖片放大預覽。
function closeImagePreview() {
    isImagePreviewOpen.value = false;
}

// 按下 Esc 時關閉圖片放大預覽。
function handlePreviewKeydown(event) {
    if (event.key === "Escape") {
        closeImagePreview();
    }
}

// 只有在圖片預覽開啟期間才監聽 Esc。
watch(isImagePreviewOpen, (isOpen) => {
    if (typeof window === "undefined") return;

    if (isOpen) {
        window.addEventListener(
            "keydown",
            handlePreviewKeydown,
        );
    } else {
        window.removeEventListener(
            "keydown",
            handlePreviewKeydown,
        );
    }
});

// 元件卸載時移除事件監聽，避免殘留。
onBeforeUnmount(() => {
    if (typeof window !== "undefined") {
        window.removeEventListener(
            "keydown",
            handlePreviewKeydown,
        );
    }
});

// 返回按鈕沿用原本的 close 事件。儲存期間不可關閉視窗。
function closeModal() {
    if (props.isSaving) return;

    emit("close");
}

// 票券右上角的 X 按鈕沿用原本的關閉方式：透過 v-model 通知父元件關閉。
function requestClose() {
    if (props.isSaving) return;

    emit("update:modelValue", false);
}
</script>

<template>
    <AppModal :model-value="modelValue" :title="isEditing ? '修改特典刊登' : '新增特典刊登'" :busy="isSaving"
        :close-on-backdrop="false" width="42rem" @update:model-value="emit('update:modelValue', $event)">
        <div class="exchange-form-modal">
            <form id="exchange-listing-form" class="exchange-listing-form" @submit.prevent="emit('submit')">
                <TicketCard appear color="green" class="exchange-form-ticket" :title="isEditing ? '修改特典刊登' : '新增特典刊登'">
                    <template #title>
                        <div class="exchange-ticket-heading">
                            <span class="exchange-ticket-title">
                                {{ isEditing ? "修改特典刊登" : "新增特典刊登" }}
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

                            <!-- 沒有可選特典時，欄位不可點擊 -->
                            <div v-if="!isEditing && useBenefits.length === 0" class="form-select" aria-disabled="true"
                                style="display: flex; align-items: center; color: #7c818b; cursor: not-allowed;">
                                目前沒有可選擇的持有特典。
                            </div>

                            <!-- 有特典或修改模式時，顯示原本的選單 -->
                            <select v-else id="use-benefit-select" v-model="form.useBenefitId" class="form-select"
                                required :disabled="isEditing || isSaving">
                                <option v-for="benefit in useBenefits" :key="getUseBenefitId(benefit)"
                                    :value="String(getUseBenefitId(benefit))">
                                    {{ useBenefitTitle(benefit) }}
                                </option>
                            </select>

                            <p v-if="isEditing" class="form-text">
                                修改刊登時，持有特典不可更換。
                            </p>
                        </div>

                        <!-- 持有特典圖片預覽 -->
                        <div v-if="selectedUseBenefit" class="benefit-preview">
                            <!-- 有圖片時，顯示可點擊的小圖 -->
                            <button v-if="previewImageUrl" type="button" class="benefit-preview-image-button"
                                aria-label="放大查看特典圖片" title="點擊放大圖片" @click="openImagePreview">
                                <img :src="previewImageUrl" alt="持有特典圖片" @error="handleBenefitImageError" />
                            </button>

                            <!-- 沒有可用圖片時，顯示禮物圖示 -->
                            <!-- 沒有可用圖片時，顯示網站原本的禮物圖示 -->
                            <div v-else class="image-placeholder">
                                <span class="image-placeholder-icon">
                                    <Icon :icon="giftIcon" aria-hidden="true" />
                                </span>

                                <span class="image-placeholder-text">
                                    暫無可用圖片
                                </span>
                            </div>

                            <div class="benefit-preview-info">
                                <strong>
                                    {{ useBenefitTitle(selectedUseBenefit) }}
                                </strong>
                                <!-- <p>
                                    持有特典編號：
                                    {{ getUseBenefitId(selectedUseBenefit) }}
                                </p> -->
                                <p v-if="previewImageUrl" class="image-preview-hint">
                                    點擊圖片可放大查看
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

    <!-- 圖片放大預覽視窗 -->
    <Teleport to="body">
        <div v-if="isImagePreviewOpen && previewImageUrl" class="image-preview-overlay" role="dialog" aria-modal="true"
            aria-label="特典圖片放大預覽" @click.self="closeImagePreview">
            <button type="button" class="image-preview-close" aria-label="關閉圖片預覽" @click="closeImagePreview">
                <span aria-hidden="true">×</span>
            </button>

            <div class="image-preview-content">
                <img :src="previewImageUrl"
                    :alt="`${selectedUseBenefit ? useBenefitTitle(selectedUseBenefit) : '持有特典'}－放大預覽`"
                    class="image-preview-large" @error="handleBenefitImageError" />

                <p v-if="selectedUseBenefit" class="image-preview-caption">
                    {{ useBenefitTitle(selectedUseBenefit) }}
                </p>
            </div>
        </div>
    </Teleport>
</template>

<style scoped>
/* 表單外層。TicketCard 的票券樣式沿用原有設計。 */
.exchange-form-modal {
    width: 100%;
    min-width: 0;
}

.exchange-listing-form {
    width: 100%;
    min-width: 0;
    margin: 0;
}

/* 票券標題列。 */
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

/* 表單內容區。 */
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

/* 已選擇特典的預覽區。 */
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

/* 小圖外層按鈕及圖片佔位區塊。 */
.benefit-preview-image-button,
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

.benefit-preview-image-button {
    padding: 0;
    border: none;
    cursor: zoom-in;
}

.benefit-preview-image-button img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: center;
    transition: opacity 0.2s ease;
}

.benefit-preview-image-button:hover img {
    opacity: 0.82;
}

.benefit-preview-image-button:focus-visible {
    outline: 3px solid #f2a58d;
    outline-offset: 3px;
}

/* 沒有可用圖片時的禮物佔位圖。 */
.image-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 0.35rem;
    padding: 0.4rem;
    border: 1px dashed #d8c9bf;
    color: #7c818b;
    text-align: center;
}

.image-placeholder-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 1.8rem;
    line-height: 1.2;
}

.image-placeholder-icon :deep(svg) {
    display: block;
    width: 1em;
    height: 1em;
}

.image-placeholder-text {
    font-size: 0.75rem;
    line-height: 1.4;
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

.benefit-preview-info .image-preview-hint {
    margin-top: 0.3rem;
    color: #b66b52;
    font-size: 0.76rem;
}

/* 圖片放大預覽視窗。Teleport 將預覽層放到 body，避免被表單或 AppModal 的 overflow、transform 等樣式裁切。 */
.image-preview-overlay {
    position: fixed;
    inset: 0;
    z-index: 10000;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 3.5rem 1.5rem 1.5rem;
    background: rgb(20 20 25 / 88%);
    animation: image-preview-fade-in 0.18s ease;
}

.image-preview-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.75rem;
    width: 100%;
    max-width: 1100px;
    max-height: 100%;
    min-width: 0;
}

.image-preview-large {
    display: block;
    width: auto;
    height: auto;
    max-width: 100%;
    max-height: calc(100dvh - 8rem);
    object-fit: contain;
    border-radius: 0.5rem;
    box-shadow: 0 8px 32px rgb(0 0 0 / 30%);
}

.image-preview-caption {
    max-width: 100%;
    margin: 0;
    color: #fff;
    font-size: 0.9rem;
    line-height: 1.5;
    text-align: center;
    overflow-wrap: anywhere;
}

.image-preview-close {
    position: absolute;
    top: 1rem;
    right: 1rem;
    display: grid;
    place-items: center;
    width: 2.75rem;
    height: 2.75rem;
    padding: 0;
    border: 1px solid rgb(255 255 255 / 55%);
    border-radius: 50%;
    background: rgb(255 255 255 / 12%);
    color: #fff;
    font-family: inherit;
    font-size: 2rem;
    font-weight: 400;
    line-height: 1;
    cursor: pointer;
    transition:
        background-color 0.2s ease,
        transform 0.2s ease;
}

.image-preview-close:hover {
    background: rgb(255 255 255 / 25%);
    transform: scale(1.05);
}

.image-preview-close:focus-visible {
    outline: 2px solid #fff;
    outline-offset: 3px;
}

@keyframes image-preview-fade-in {
    from {
        opacity: 0;
    }

    to {
        opacity: 1;
    }
}

/* 底部操作區沿用 TicketCard 的 ticket-stub。 */
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

/* 只調整包含本表單的 AppModal。不修改 AppModal.vue 的共用樣式。 */
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

/* 防止固定在畫面中央的票券因滑鼠移入而位移。只影響本表單，不影響其他 TicketCard。 */
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

/* 手機版排版。 */
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

    .benefit-preview-image-button,
    .image-placeholder {
        width: 76px;
        height: 76px;
    }

    .image-placeholder-icon {
        font-size: 1.5rem;
    }

    .image-placeholder-text {
        font-size: 0.68rem;
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

    .image-preview-overlay {
        padding: 3.5rem 0.75rem 1rem;
    }

    .image-preview-large {
        max-width: 100%;
        max-height: calc(100dvh - 7rem);
    }

    .image-preview-close {
        top: 0.65rem;
        right: 0.65rem;
        width: 2.5rem;
        height: 2.5rem;
    }
}

/* 使用者若偏好減少動畫，關閉預覽淡入效果。 */
@media (prefers-reduced-motion: reduce) {
    .image-preview-overlay {
        animation: none;
    }

    .benefit-preview-image-button img,
    .image-preview-close {
        transition: none;
    }
}
</style>