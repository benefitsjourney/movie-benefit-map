<script setup>
import { RouterView, useRoute, useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import { computed } from 'vue'
import { useAuth } from '@/composables/useAuth'
import AppNavbar from '@/components/layout/AppNavbar.vue'
import PageIntro from '@/components/layout/PageIntro.vue'

import mapIcon from '@iconify-icons/mdi/map-outline'
import giftIcon from '@iconify-icons/mdi/gift-outline'
import movieIcon from '@iconify-icons/mdi/movie-open-outline'
import clapperboardIcon from '@iconify-icons/mdi/clapperboard-outline'
import accountIcon from '@iconify-icons/mdi/account-circle-outline'
import shieldIcon from '@iconify-icons/mdi/shield-account-outline'
import clipboardListIcon from '@iconify-icons/mdi/clipboard-list-outline'
import heartIcon from '@iconify-icons/mdi/heart-outline'
import messageIcon from '@iconify-icons/mdi/message-text-outline'

const auth = useAuth()
const route = useRoute()
const router = useRouter()

// 共用前台導覽列連結
const links = computed(() => {
  const items = [
    { to: '/map', label: '地圖查詢', icon: mapIcon },
    { to: '/benefits', label: '特典一覽', icon: giftIcon },
    { to: '/cinemas', label: '影城探索', icon: movieIcon },
    { to: '/movies', label: '電影探索', icon: clapperboardIcon },
  ]

  if (auth.isLoggedIn.value) {
    items.push({
      to: '/account',
      label: '關於我',
      icon: accountIcon,
    })
  }

  return items
})

// 會員中心側邊選單
const accountMenus = [
  { to: '/account', label: '關於我', icon: accountIcon },
  { to: '/account/listings', label: '我的刊登', icon: clipboardListIcon },
  { to: '/account/favorites', label: '我的收藏', icon: heartIcon },
  { to: '/account/benefits', label: '我的特典', icon: giftIcon },
  { to: '/account/messages', label: '訊息中心', icon: messageIcon },
]

// 關於我首頁保留原本版面，其他會員功能頁顯示側邊選單
const showSidebar = computed(() => route.path !== '/account')

// 判斷選單是否為目前頁面
function isCurrentMenu(menu) {
  if (menu.to === '/account/messages') {
    return route.path === '/account/messages' ||
      route.path.startsWith('/account/messages/')
  }

  return route.path === menu.to
}

// 點選選單前往對應頁面
function goToMenu(menu) {
  if (isCurrentMenu(menu)) return
  router.push(menu.to)
}
</script>

<template>
  <div class="app-shell">
    <AppNavbar
      nav-label="主要導覽"
      brand-to="/map"
      brand-name="特典地圖"
      :links="links"
      switch-to="/admin"
      switch-label="後台管理"
      :switch-icon="shieldIcon"
      :show-switch="auth.isAdmin.value"
    />

    <main class="page-main">
          <div class="account-layout">
            <PageIntro
              kicker="✦ MEMBER CENTER"
              lead="會員"
              mark="中心"
              description="管理你的會員資料，整理屬於你的特典收藏旅程。"
              :icon-left="giftIcon"
              :icon-right="accountIcon"
              :icon-top="heartIcon"
            />
            <RouterView v-if="!showSidebar" />

        <div v-else class="account-content">
          <aside class="account-sidebar">
            <h3 class="sidebar-title">會員功能</h3>

            <nav aria-label="會員中心選單" class="sidebar-menu">
              <button
                v-for="menu in accountMenus"
                :key="menu.to"
                type="button"
                class="sidebar-item"
                :class="{ active: isCurrentMenu(menu) }"
                :disabled="isCurrentMenu(menu)"
                :aria-current="isCurrentMenu(menu) ? 'page' : undefined"
                @click="goToMenu(menu)"
              >
                <span class="sidebar-icon">
                  <Icon :icon="menu.icon" />
                </span>

                <span>{{ menu.label }}</span>
              </button>
            </nav>
          </aside>

          <div class="account-panel">
            <RouterView />
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.account-layout {
  width: min(1120px, calc(100% - 32px));
  margin: 0 auto;
  padding: 8px 0 40px;
}

.account-content {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.account-sidebar,
.account-panel {
  min-width: 0;
  border: 1px solid #f4d9b9;
  border-radius: 16px;
  background: #fffdf9;
  padding: 20px;
}

.sidebar-title {
  margin: 0 0 16px;
  font-size: 1.05rem;
  font-weight: 700;
}

.sidebar-menu {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: #344054;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.2s, border-color 0.2s;
}

.sidebar-item:hover:not(:disabled) {
  background: #fff4dc;
}

.sidebar-item.active,
.sidebar-item:disabled {
  background: #fff0c2;
  border-color: #ffda8a;
  color: #805b16;
  font-weight: 700;
  cursor: default;
}

.sidebar-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
}

.current-indicator {
  margin-left: auto;
  font-size: 0.7rem;
  font-weight: 400;
  white-space: nowrap;
}

.account-panel {
  min-height: 300px;
}

@media (max-width: 768px) {
  .account-content {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .account-sidebar,
  .account-panel {
    padding: 16px;
  }

  .sidebar-menu {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .sidebar-item {
    padding: 10px;
    font-size: 0.9rem;
  }

  .current-indicator {
    display: none;
  }
}
</style>