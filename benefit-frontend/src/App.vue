<script setup>
import AppNavbar from "@/components/AppNavbar.vue";
</script>

<template>
  <AppNavbar />
  <main class="container page-main">
    <RouterView />
  </main>
</template>

<!-- 全站樣式統一由main.js引入 -->
<style src="./assets/main.css"></style>

<style scoped>
.page-main {
  padding-top: 1.35rem;
  padding-bottom: 3rem;
}
</style>
