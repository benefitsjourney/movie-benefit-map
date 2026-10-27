<script setup>
import AppAlert from "@/components/ui/Alert.vue";
import BackendConnectionToast from "@/components/ui/BackendConnectionToast.vue";
</script>

<template>
  <BackendConnectionToast />
  <RouterView />
  <AppAlert />
</template>
