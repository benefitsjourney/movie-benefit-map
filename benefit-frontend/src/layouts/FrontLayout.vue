<script setup>
import AppNavbar from "@/components/layout/AppNavbar.vue";
import { useAuth } from "@/composables/useAuth";
import mapIcon from "@iconify-icons/mdi/map-outline";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import movieIcon from "@iconify-icons/mdi/movie-open-outline";
import clapperboardIcon from "@iconify-icons/mdi/clapperboard-outline";
import shieldIcon from "@iconify-icons/mdi/shield-account-outline";
import heartIcon from "@iconify-icons/mdi/heart-outline";
import megaphoneIcon from "@iconify-icons/mdi/bullhorn-outline";

const auth = useAuth();

const links = [
  { to: "/map", label: "地圖查詢", icon: mapIcon },
  { to: "/benefits", label: "特典一覽", icon: giftIcon },
  { to: "/collection", label: "我的收藏", icon: heartIcon },
  { to: "/my-reports", label: "我的回報", icon: megaphoneIcon },
  { to: "/cinemas", label: "影城探索", icon: movieIcon },
  { to: "/movies", label: "電影探索", icon: clapperboardIcon },
];
</script>

<template>
  <div class="app-shell">
    <AppNavbar nav-label="主要導覽" brand-to="/map" brand-name="特典地圖" :links="links" switch-to="/admin" switch-label="後台管理"
      :switch-icon="shieldIcon" :show-switch="auth.isAdmin.value" />
    <main class="page-main">
      <RouterView />
    </main>
  </div>
</template>
