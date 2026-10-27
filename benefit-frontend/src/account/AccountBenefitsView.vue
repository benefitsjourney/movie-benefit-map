<script setup>
import { RouterLink } from "vue-router";
import { Icon } from "@iconify/vue";
import heartIcon from "@iconify-icons/mdi/heart-outline";
import clipboardIcon from "@iconify-icons/mdi/clipboard-text-outline";
import chevronIcon from "@iconify-icons/mdi/chevron-right";

// 我的特典：收藏庫和回報紀錄只有本人看得到，這裡放入口，點了到原本的整頁
const benefitLinks = [
  {
    title: "我的收藏庫",
    description: "記錄領到的特典，拍照留念",
    icon: heartIcon,
    to: "/collection",
  },
  {
    title: "我的回報紀錄",
    description: "庫存回報、缺漏回報和審核結果",
    icon: clipboardIcon,
    to: "/my-reports",
  },
];
</script>

<template>
  <section>
    <h2 class="h5 fw-bold mb-3">我的特典</h2>

    <div class="benefit-links">
      <RouterLink v-for="item in benefitLinks" :key="item.to" :to="item.to" class="benefit-link-card">
        <div class="benefit-link-icon">
          <Icon :icon="item.icon" aria-hidden="true" />
        </div>

        <div class="flex-grow-1">
          <h3 class="h6 fw-bold mb-2">{{ item.title }}</h3>
          <p class="small text-secondary mb-0">{{ item.description }}</p>
        </div>

        <Icon :icon="chevronIcon" class="benefit-chevron" aria-hidden="true" />
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
/* 卡片樣式跟關於我首頁的會員功能卡片一樣 */
.benefit-links {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 1rem;
}

@media (min-width: 640px) {
  .benefit-links {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.benefit-link-card {
  height: 100%;
  min-height: 125px;
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 1.1rem;
  color: var(--c-ink);
  text-decoration: none;
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 1rem;
  transition: transform 0.2s, box-shadow 0.2s;
}

.benefit-link-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-soft);
}

.benefit-link-icon {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #805b16;
  background: #fff0c9;
  border-radius: 0.75rem;
  font-size: 1.5rem;
}

.benefit-chevron {
  flex-shrink: 0;
  color: var(--c-muted);
  font-size: 1.25rem;
}
</style>
