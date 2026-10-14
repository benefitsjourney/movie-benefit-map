<script setup>
import AppNavbar from "@/components/AppNavbar.vue";
import syncIcon from "@iconify-icons/mdi/database-sync-outline";
import arrowLeftIcon from "@iconify-icons/mdi/arrow-left";

const links = [
  { to: "/admin/crawler", label: "爬蟲管理", icon: syncIcon },
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
