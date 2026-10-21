<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import filmIcon from "@iconify-icons/mdi/filmstrip";
import calendarIcon from "@iconify-icons/mdi/calendar-blank-outline";
import plusIcon from "@iconify-icons/mdi/plus";
import formIcon from "@iconify-icons/mdi/form-textbox";
import { openAppAlert } from "@/components/ui/Alert.vue";
import PageIntro from "@/components/layout/PageIntro.vue";
import DetailCard from "@/components/layout/DetailCard.vue";
import AppButton from "@/components/ui/Button.vue";
import MovieForm from "@/components/forms/MovieForm.vue";
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

async function createMovie(payload) {
  if (submitting.value) return;

  submitting.value = true;

  try {
    await http.post("/movies", payload);
    await openAppAlert({
      variant: "success",
      title: "已新增電影",
      text: "新的電影已建立。",
      confirmText: "返回列表",
    });
    await router.push({ name: "admin-movies" });
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

    const message = apiMessage(error, "新增電影失敗，請稍後再試。");
    await openAppAlert({
      variant: "error",
      title: "新增電影失敗",
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
    <PageIntro kicker="✦ NEW MOVIE" lead="新增" mark="電影" description="填寫電影基本資料，新增電影至資料庫。" :icon-left="filmIcon"
      :icon-right="calendarIcon" :icon-top="plusIcon" theme="purple" />

    <DetailCard title="新增電影資料" :title-icon="formIcon" :back-to="{ name: 'admin-movies' }" back-label="返回電影一覽"
      theme="purple">
      <template #actions>
        <AppButton type="submit" form="movie-create-form" color="purple" :icon="plusIcon" :disabled="submitting">
          {{ submitting ? "新增中…" : "新增電影" }}
        </AppButton>
      </template>

      <MovieForm mode="create" plain hide-submit form-id="movie-create-form" :submitting="submitting"
        @submit="createMovie" />
    </DetailCard>
  </section>
</template>
