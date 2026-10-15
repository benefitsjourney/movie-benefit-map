<script setup>
import AppNavbar from "@/components/layout/AppNavbar.vue";
import movieIcon from "@iconify-icons/mdi/movie-open-outline";
import syncIcon from "@iconify-icons/mdi/database-sync-outline";
import arrowLeftIcon from "@iconify-icons/mdi/arrow-left";

const links = [
  { to: "/admin/crawler", label: "爬蟲管理", icon: syncIcon },
  { to: "/admin/cinemas", label: "影城資料", icon: movieIcon },
];
</script>

<template>
  <div class="app-shell theme-admin">
    <AppNavbar nav-label="後台導覽" brand-to="/admin" brand-name="後台管理" :links="links" switch-to="/map" switch-label="返回前台"
      :switch-icon="arrowLeftIcon" theme="purple" />
    <main class="page-main">
      <RouterView />
    </main>
  </div>
</template>
