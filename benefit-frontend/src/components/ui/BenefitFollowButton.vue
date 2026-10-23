<script setup>
/**
 * 追蹤特典按鈕（特典詳細頁、行事曆視窗共用）。
 * 用法：<BenefitFollowButton :benefit="benefit" @change="重新載入" />
 * benefit 要有 benefitId、benefitName
 * size="small"：比較小的版本（放在卡片裡用）
 */
import { onMounted } from "vue";
import { Icon } from "@iconify/vue";
import bellOutlineIcon from "@iconify-icons/mdi/bell-plus-outline";
import bellRingIcon from "@iconify-icons/mdi/bell-ring";
import { useBenefitReminders } from "@/composables/useBenefitReminders";

const props = defineProps({
  benefit: { type: Object, required: true },
  size: { type: String, default: "normal" },
});

const emit = defineEmits(["change"]);

const { loadReminders, isFollowing, isBusy, toggleFollow } = useBenefitReminders();

onMounted(() => {
  loadReminders();
});

async function onClick() {
  const changed = await toggleFollow(props.benefit);
  if (changed) emit("change", isFollowing(props.benefit.benefitId));
}
</script>

<template>
  <button type="button" class="follow-btn" :class="{
    'is-on': isFollowing(benefit.benefitId),
    'is-small': size === 'small',
  }" :disabled="isBusy(benefit.benefitId)" :aria-pressed="isFollowing(benefit.benefitId)" @click="onClick">
    <Icon :icon="isFollowing(benefit.benefitId) ? bellRingIcon : bellOutlineIcon" aria-hidden="true" />
    {{ isFollowing(benefit.benefitId) ? "已追蹤" : "追蹤" }}
  </button>
</template>

<style scoped>
/* 空心＝未追蹤（白底藍框）、實心＝已追蹤（深藍底黃字） */
.follow-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  min-height: 2.75rem;
  padding: 0.4rem 1.1rem;
  border: 1.5px solid #1e2b45;
  border-radius: 999px;
  background: #fff;
  color: #1e2b45;
  font-size: var(--text-control);
  font-weight: 800;
  cursor: pointer;
  transition:
    background-color var(--ease),
    color var(--ease);
}

.follow-btn :deep(svg) {
  font-size: 1.3rem;
}

.follow-btn:hover:not(:disabled) {
  background: #eef2f9;
}

.follow-btn.is-on {
  background: #1e2b45;
  color: #ffc857;
}

.follow-btn.is-on:hover:not(:disabled) {
  background: #2c3d5f;
}

/* 追蹤的那一下，鈴鐺搖一下 */
.follow-btn.is-on :deep(svg) {
  animation: bell-ring 0.6s ease;
  transform-origin: 50% 10%;
}

.follow-btn:disabled {
  opacity: 0.6;
  cursor: wait;
}

.follow-btn:focus-visible {
  outline: 2px solid #1e2b45;
  outline-offset: 3px;
}

.follow-btn.is-small {
  min-height: 2.3rem;
  padding: 0.25rem 0.85rem;
  font-size: var(--text-min);
}

.follow-btn.is-small :deep(svg) {
  font-size: 1.1rem;
}

@keyframes bell-ring {
  20% {
    transform: rotate(18deg);
  }

  40% {
    transform: rotate(-14deg);
  }

  60% {
    transform: rotate(9deg);
  }

  80% {
    transform: rotate(-5deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .follow-btn.is-on :deep(svg) {
    animation: none;
  }
}
</style>
