<script setup>
import AppNavbar from "@/components/layout/AppNavbar.vue";
import { computed } from "vue";
import { useAuth } from "@/composables/useAuth";
import mapIcon from "@iconify-icons/mdi/map-outline";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import movieIcon from "@iconify-icons/mdi/movie-open-outline";
import clapperboardIcon from "@iconify-icons/mdi/clapperboard-outline";
import accountIcon from "@iconify-icons/mdi/account-circle-outline";
import shieldIcon from "@iconify-icons/mdi/shield-account-outline";
import heartIcon from "@iconify-icons/mdi/heart-outline";
import megaphoneIcon from "@iconify-icons/mdi/bullhorn-outline";
import calendarIcon from "@iconify-icons/mdi/calendar-month-outline";
import trophyIcon from "@iconify-icons/mdi/trophy-outline";
import FloatingChat from '@/components/chat/FloatingChat.vue'

const auth = useAuth();

const links = computed(() => {
  const items = [
    { to: "/map", label: "地圖查詢", icon: mapIcon },
    { to: "/benefits", label: "特典一覽", icon: giftIcon },
    { to: "/calendar", label: "行事曆", icon: calendarIcon },
    { to: "/ranking", label: "排行榜", icon: trophyIcon },
    { to: "/cinemas", label: "影城探索", icon: movieIcon },
    { to: "/movies", label: "電影探索", icon: clapperboardIcon },
    ];

  if (auth.isLoggedIn.value) {
      items.push({
        to: "/account",
        label: "關於我",
        icon: accountIcon,
      });
    }
    return items;
});
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
      switch-first
      :switch-icon="shieldIcon"
      :show-switch="auth.isAdmin.value"
    />
    <main class="page-main">
      <RouterView />
    </main>
      <FloatingChat />
  </div>
</template>
