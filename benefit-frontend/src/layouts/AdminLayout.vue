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
import giftIcon from "@iconify-icons/mdi/gift-outline";
import megaphoneIcon from "@iconify-icons/mdi/bullhorn-outline";
import excelIcon from "@iconify-icons/mdi/microsoft-excel";
import accountGroupIcon from "@iconify-icons/mdi/account-group-outline";
import feedbackIcon from "@iconify-icons/mdi/comment-text-outline";

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
    label: "特典",
    icon: giftIcon,
    children: [
      { to: "/admin/benefit-reports", label: "特典審核", icon: giftIcon },
      { to: "/admin/benefits", label: "特典管理", icon: giftIcon },
      {
        to: "/admin/inventory-reports",
        label: "庫存回報管理",
        icon: megaphoneIcon,
      },
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
  {
    label: "資料匯出",
    icon: excelIcon,
    to: "/admin/excel",
  },
  {
    label: "會員管理",
    icon: accountGroupIcon,
    children: [
      {
        to: "/admin/users",
        label: "會員列表",
        icon: accountGroupIcon,
      },
      {
        to: "/admin/login-logs",
        label: "登入日誌",
        icon: historyIcon,
      },
      {
      to: "/admin/feedback",
      label: "意見回饋管理",
      icon: feedbackIcon,
    },
    ],
  },
];
</script>

<template>
  <div class="app-shell theme-admin">
    <AppNavbar nav-label="後台導覽" brand-to="/admin" brand-name="後台管理" :links="links" switch-to="/map" switch-label="返回前台"
      switch-first :switch-icon="arrowLeftIcon" theme="purple" />
    <main class="page-main">
      <RouterView />
    </main>
  </div>
</template>
