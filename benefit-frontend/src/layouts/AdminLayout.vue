<script setup>
import AppNavbar from "@/components/layout/AppNavbar.vue";
import movieIcon from "@iconify-icons/mdi/movie-open-outline";
import filmIcon from "@iconify-icons/mdi/filmstrip";
import libraryIcon from "@iconify-icons/mdi/movie-open-star-outline";
import spiderIcon from "@iconify-icons/mdi/spider-outline";
import syncIcon from "@iconify-icons/mdi/database-sync-outline";
import historyIcon from "@iconify-icons/mdi/history";
import reviewIcon from "@iconify-icons/mdi/clipboard-check-outline";
import arrowLeftIcon from "@iconify-icons/mdi/arrow-left";

const links = [
  {
    label: "爬蟲",
    icon: spiderIcon,
    children: [
      { to: "/admin/crawler", label: "資料同步", icon: syncIcon },
      { to: "/admin/crawler-logs", label: "爬蟲紀錄", icon: historyIcon },
    ],
  },
  {
    label: "電影 / 影城",
    icon: libraryIcon,
    children: [
      { to: "/admin/cinemas", label: "影城資料", icon: movieIcon },
      { to: "/admin/movies", label: "電影資料", icon: filmIcon },
      {
        to: "/admin/cinemas-movies/reviews",
        label: "爬蟲審核",
        icon: reviewIcon,
      },
    ],
  },
];
</script>

<template>
  <div class="app-shell theme-admin">
    <AppNavbar
      nav-label="後台導覽"
      brand-to="/admin"
      brand-name="後台管理"
      :links="links"
      switch-to="/map"
      switch-label="返回前台"
      :switch-icon="arrowLeftIcon"
      theme="purple"
    />
    <main class="page-main">
      <RouterView />
    </main>
  </div>
</template>
