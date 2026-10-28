<!-- ========== components/exchange/ExchangeModeSwitch.vue ========== -->
<!-- 這個元件只處理頁籤與新增按鈕。點擊後透過 emit 通知主頁，不直接操作路由或 API。 -->
<!-- 負責特典交換／票券交換頁籤，以及新增刊登按鈕。 -->
<!-- 切換元件 -->
<script setup>
import { Icon } from "@iconify/vue";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import ticketIcon from "@iconify-icons/mdi/ticket-outline";

defineProps({
    mainTab: {
        type: String,
        required: true,
    },
    isLoggedIn: {
        type: Boolean,
        default: false,
    },
});

const emit = defineEmits([
    "change-tab",
    "add-exchange",
    "add-ticket",
]);
</script>

<template>
    <div class="mode-switch-container">
        <nav class="mode-switch" role="tablist" aria-label="刊登內容">
            <button type="button" role="tab" class="mode-btn" :class="{ 'is-on': mainTab === 'exchange' }"
                :aria-selected="mainTab === 'exchange'" @click="emit('change-tab', 'exchange')">
                <Icon :icon="giftIcon" class="mode-icon" aria-hidden="true" />
                <span>特典交換區</span>
            </button>

            <button type="button" role="tab" class="mode-btn" :class="{ 'is-on': mainTab === 'ticket' }"
                :aria-selected="mainTab === 'ticket'" @click="emit('change-tab', 'ticket')">
                <Icon :icon="ticketIcon" class="mode-icon" aria-hidden="true" />
                <span>票券交換區</span>
            </button>
        </nav>

        <div v-if="isLoggedIn" class="page-heading-actions">
            <button v-if="mainTab === 'exchange'" class="btn btn-primary" type="button" @click="emit('add-exchange')">
                <span class="action-icon" aria-hidden="true">＋</span>
                新增特典刊登
            </button>

            <button v-if="mainTab === 'ticket'" class="btn btn-primary" type="button" @click="emit('add-ticket')">
                <span class="action-icon" aria-hidden="true">＋</span>
                新增票券刊登
            </button>
        </div>
    </div>
</template>

<style scoped>
.mode-switch-container {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 0.85rem;
    width: 100%;
    min-width: 0;
    margin-bottom: 1.25rem;
}

.mode-switch {
    display: inline-flex;
    align-items: center;
    flex-wrap: nowrap;
    gap: 0.25rem;
    flex: 0 1 auto;
    min-width: 0;
    max-width: 100%;
    padding: 0.3rem;
    border-radius: 999px;
    background: #1e2b45;
    box-shadow: 0 6px 16px rgb(30 43 69 / 18%);
}

.mode-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex: 1 1 0;
    gap: 0.35rem;
    min-width: 0;
    min-height: 2.5rem;
    padding: 0.45rem 0.65rem;
    border: 0;
    border-radius: 999px;
    background: transparent;
    color: #cfd6e4;
    font-size: 0.95rem;
    font-weight: 800;
    white-space: normal;
    overflow-wrap: anywhere;
    text-align: center;
    line-height: 1.4;
    cursor: pointer;
    transition: background-color 0.2s ease, color 0.2s ease;
}

.mode-icon {
    width: 1.15rem;
    height: 1.15rem;
    flex-shrink: 0;
}

.mode-btn:hover:not(.is-on) {
    color: #fff;
}

.mode-btn.is-on {
    background: #ff7049;
    color: #fff;
}

.mode-btn:focus-visible {
    outline: 2px solid #ffc857;
    outline-offset: 2px;
}

.page-heading-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 0.5rem;
    min-width: 0;
    margin: 4px 0 0 auto;
}

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
    border: 1px solid #ff7049;
    border-radius: 0.7rem;
    background: #ff7049;
    color: #fff;
    font-size: 0.9rem;
    font-weight: 750;
    white-space: normal;
    overflow-wrap: anywhere;
    transition: transform 0.2s ease, box-shadow 0.2s ease,
        background-color 0.2s ease, border-color 0.2s ease;
}

.page-heading-actions .btn:hover:not(:disabled) {
    border-color: #c94f32;
    background: #c94f32;
    color: #fff;
    transform: translateY(-2px);
    box-shadow: 0 5px 14px rgb(201 79 50 / 20%);
}

.action-icon {
    flex-shrink: 0;
    font-size: 1.15rem;
    line-height: 1;
}

.mode-switch-container button:disabled {
    cursor: not-allowed;
    opacity: 0.58;
}

@media (max-width: 900px) {
    .mode-switch-container {
        align-items: flex-start;
        gap: 0.75rem;
    }

    .mode-switch {
        flex: 1 1 100%;
        width: 100%;
    }

    .mode-btn {
        flex: 1 1 0;
        gap: 0.3rem;
        padding-right: 0.5rem;
        padding-left: 0.5rem;
        font-size: 0.88rem;
    }

    .page-heading-actions {
        flex: 1 1 100%;
        width: 100%;
        justify-content: flex-end;
        margin: 0;
    }

    .page-heading-actions .btn {
        flex: 0 1 auto;
        padding-right: 0.85rem;
        padding-left: 0.85rem;
        font-size: 0.86rem;
    }
}

@media (max-width: 640px) {
    .mode-switch-container {
        flex-direction: column;
        align-items: stretch;
        gap: 0.75rem;
    }

    .mode-switch {
        display: flex;
        flex: 0 0 auto;
        width: 100%;
    }

    .mode-btn {
        flex: 1 1 0;
        min-width: 0;
        gap: 0.2rem;
        padding: 0.45rem 0.25rem;
        font-size: 0.82rem;
        line-height: 1.35;
        white-space: normal;
        overflow-wrap: anywhere;
        text-align: center;
    }

    .mode-icon {
        width: 1.15rem;
        height: 1.15rem;
        flex-shrink: 0;
    }

    .page-heading-actions {
        flex: 0 0 auto;
        width: 100%;
        max-width: 100%;
        justify-content: flex-end;
        margin: 0;
    }

    .page-heading-actions .btn {
        flex: 0 1 auto;
        width: auto;
        max-width: 100%;
        min-height: 40px;
        padding: 0.55rem 0.8rem;
        font-size: 0.84rem;
    }
}

@media (prefers-reduced-motion: reduce) {

    .mode-switch-container *,
    .mode-switch-container *::before,
    .mode-switch-container *::after {
        transition-duration: 0.01ms !important;
        animation-duration: 0.01ms !important;
        animation-iteration-count: 1 !important;
    }
}
</style>
