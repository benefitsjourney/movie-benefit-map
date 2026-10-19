<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import movieIcon from "@iconify-icons/mdi/movie-open-outline";
import pinIcon from "@iconify-icons/mdi/map-marker-outline";
import plusIcon from "@iconify-icons/mdi/plus";
import formIcon from "@iconify-icons/mdi/form-textbox";
import { openAppAlert } from "@/components/ui/Alert.vue";
import PageIntro from "@/components/layout/PageIntro.vue";
import DetailCard from "@/components/layout/DetailCard.vue";
import AppButton from "@/components/ui/Button.vue";
import CinemaForm from "@/components/forms/CinemaForm.vue";
import http from "@/plugins/axios";

const router = useRouter();
const submitting = ref(false);

function apiMessage(error, fallback) {
  const message = error?.response?.data?.message;
  return typeof message === "string" && message.trim() ? message.trim() : fallback;
}

function isUnauthorized(error) {
  return error?.response?.status === 401;
}

async function createCinema(payload) {
  if (submitting.value) return;

  submitting.value = true;

  try {
    await http.post("/cinemas", payload);
    await openAppAlert({
      variant: "success",
      title: "已新增影城",
      text: "新的影城已建立。",
      confirmText: "返回列表",
    });
    await router.push({ name: "admin-cinemas" });
  } catch (error) {
    if (isUnauthorized(error)) {
      await openAppAlert({
        variant: "error",
        title: "無法儲存",
        text: "請登入管理員帳號",
        confirmText: "關閉",
        cancelText: "",
      });
      return;
    }

    const message = apiMessage(error, "新增影城失敗，請稍後再試。");
    await openAppAlert({
      variant: "error",
      title: "新增影城失敗",
      text: message,
      confirmText: "關閉",
      cancelText: "",
    });
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <section>
    <PageIntro kicker="✦ NEW CINEMA" lead="新增" mark="影城" description="填寫影城基本資料，新增影城至資料庫。" :icon-left="movieIcon"
      :icon-right="pinIcon" :icon-top="plusIcon" theme="purple" />

    <DetailCard title="新增影城資料" :title-icon="formIcon" :back-to="{ name: 'admin-cinemas' }" back-label="返回影城一覽">
      <template #actions>
        <AppButton type="submit" form="cinema-create-form" color="purple" :icon="plusIcon" :disabled="submitting">
          {{ submitting ? "新增中…" : "新增影城" }}
        </AppButton>
      </template>

      <CinemaForm mode="create" plain hide-submit form-id="cinema-create-form" :submitting="submitting"
        @submit="createCinema" />
    </DetailCard>
  </section>
</template>
