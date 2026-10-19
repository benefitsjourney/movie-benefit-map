<script setup>
/**
 * 共用詳情卡。
 * 負責外框、返回列與標題列。
 * 區塊內容由各詳情頁填入。
 */
import { Icon } from "@iconify/vue";
import BackLink from "@/components/layout/BackLink.vue";

defineProps({
  title: { type: String, default: "" },
  titleIcon: { type: [Object, String], default: null },
  backTo: { type: [String, Object], default: null },
  backLabel: { type: String, default: "返回" },
});
</script>

<template>
  <article class="detail-card">
    <div v-if="backTo" class="detail-toolbar">
      <BackLink :to="backTo">
        {{ backLabel }}
      </BackLink>
    </div>

    <div v-if="title || $slots.actions" class="detail-head">
      <h2 v-if="title">
        <Icon v-if="titleIcon" :icon="titleIcon" aria-hidden="true" />
        {{ title }}
      </h2>

      <div v-if="$slots.actions" class="detail-actions">
        <slot name="actions" />
      </div>
    </div>

    <slot />
  </article>
</template>

<style scoped>
.detail-card {
  margin-bottom: 1rem;
  padding: 1.15rem 1.35rem 1.35rem;
  border: 1.5px solid var(--c-border);
  border-radius: 22px;
  background: var(--c-card);
  box-shadow: var(--shadow-soft);
}

.detail-toolbar {
  margin-bottom: 0.85rem;
  padding-bottom: 0.75rem;
  border-bottom: 1.5px solid var(--c-border);
}

.detail-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem 1rem;
  margin-bottom: 1rem;
}

.detail-head h2 {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
}

.detail-head h2 svg {
  flex: none;
  color: #8d72ed;
  font-size: var(--text-card-title);
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  margin-left: auto;
}

@media (max-width: 719px) {
  .detail-actions {
    flex-basis: 100%;
    margin-left: 0;
  }

  .detail-actions :deep(.app-button) {
    width: 100%;
  }
}
</style>

<!-- 非 scoped，讓各詳情頁插入的區塊沿用同一套欄位樣式。 -->
<style>
.detail-card .detail-section + .detail-section,
.detail-card .detail-section.is-after-edit {
  margin-top: 1.2rem;
  padding-top: 1.2rem;
  border-top: 1.5px solid var(--c-border);
}

.detail-card .detail-section .app-switch {
  min-height: 0;
  --form-accent: #8d72ed;
}

.detail-card .detail-section h3 {
  margin: 0 0 0.85rem;
  font-size: var(--text-card-title);
  line-height: 1.3;
  font-weight: 900;
}

.detail-card .detail-section dl {
  display: grid;
  gap: 0.85rem 1.25rem;
  margin: 0;
}

.detail-card .detail-section dl > div {
  display: grid;
  grid-template-columns: 7.5rem minmax(0, 1fr);
  gap: 0.35rem 1rem;
  align-items: start;
}

.detail-card .detail-section dt {
  margin: 0;
  color: var(--c-muted);
  font-size: var(--text-control);
  font-weight: 700;
}

.detail-card .detail-section dd {
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
  font-size: var(--text-body);
  line-height: var(--leading-body);
}

.detail-card .detail-link {
  color: #8d72ed;
  font-weight: 700;
  overflow-wrap: anywhere;
  text-underline-offset: 0.18em;
}

.detail-card .detail-link:hover {
  color: #5c4696;
}

.detail-card .detail-link:focus-visible {
  border-radius: 4px;
  outline: 2px solid #8d72ed;
  outline-offset: 3px;
}

@media (max-width: 719px) {
  .detail-card .detail-section dl > div {
    grid-template-columns: 1fr;
    gap: 0.15rem;
  }
}
</style>
