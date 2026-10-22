import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import http from "@/plugins/axios";
import { openAppAlert } from "@/components/ui/Alert.vue";

/**
 * 特典一覽、特典詳細頁共用的「愛心收藏」。
 * loadFavorites()：GET /use-benefits/me，記下我收藏過哪些特典（沒登入就當作都沒收藏）
 * isFavorite(benefitId)：有沒有收藏
 * toggleFavorite(benefit)：空心→收藏（POST /use-benefits）；實心→確認後移除（DELETE /use-benefits/{id}）
 * benefit 要有 benefitId、benefitName
 */
export function useFavoriteBenefits() {
  const router = useRouter();
  const route = useRoute();

  // benefitId → { useBenefitId, photoCount }
  const favorites = ref(new Map());
  // 正在送出的 benefitId，避免連點
  const busyIds = ref(new Set());

  const loggedIn = () => Boolean(localStorage.getItem("token"));

  function apiMessage(error, fallback) {
    const message = error?.response?.data?.message;
    return typeof message === "string" && message.trim() ? message.trim() : fallback;
  }

  function showError(title, text) {
    return openAppAlert({ variant: "error", title, text, confirmText: "關閉", cancelText: "" });
  }

  async function askLogin(text) {
    const result = await openAppAlert({
      variant: "info",
      title: "請先登入",
      text,
      confirmText: "前往登入",
      cancelText: "取消",
    });
    if (result.isConfirmed) {
      router.push({ name: "login", query: { redirect: route.fullPath } });
    }
  }

  async function loadFavorites() {
    if (!loggedIn()) {
      favorites.value = new Map();
      return;
    }
    try {
      const { data } = await http.get("/use-benefits/me");
      const next = new Map();
      for (const item of Array.isArray(data) ? data : []) {
        next.set(item.benefitId, {
          useBenefitId: item.useBenefitId,
          photoCount: item.photos?.length ?? 0,
        });
      }
      favorites.value = next;
    } catch {
      // 載入失敗不擋畫面，愛心先全部顯示空心
      favorites.value = new Map();
    }
  }

  function isFavorite(benefitId) {
    return favorites.value.has(benefitId);
  }

  function isBusy(benefitId) {
    return busyIds.value.has(benefitId);
  }

  async function addFavorite(benefit) {
    try {
      const { data } = await http.post("/use-benefits", { benefitId: benefit.benefitId });
      favorites.value.set(benefit.benefitId, { useBenefitId: data.useBenefitId, photoCount: 0 });
    } catch (error) {
      const code = error?.response?.status;
      if (code === 409) {
        // 其實已經收藏過了（例如在別的分頁收藏的），重新同步一次就好
        await loadFavorites();
      } else if (code === 401) {
        await askLogin("登入已過期，請重新登入後再收藏。");
      } else {
        await showError("收藏失敗", apiMessage(error, "請稍後再試一次。"));
      }
    }
  }

  async function removeFavorite(benefit) {
    const saved = favorites.value.get(benefit.benefitId);
    if (!saved) return;

    const name = benefit.benefitName ? `「${benefit.benefitName}」` : "這個特典";
    const text = saved.photoCount
      ? `${name}會從收藏庫移除，你上傳的 ${saved.photoCount} 張照片也會一起刪除，無法復原。`
      : `${name}會從收藏庫移除。`;
    const result = await openAppAlert({
      variant: "warning",
      title: "取消收藏？",
      text,
      confirmText: "移除",
      cancelText: "保留",
    });
    if (!result.isConfirmed) return;

    try {
      await http.delete(`/use-benefits/${saved.useBenefitId}`);
      favorites.value.delete(benefit.benefitId);
    } catch (error) {
      const code = error?.response?.status;
      if (code === 404) {
        // 已經被刪掉了，畫面跟著改成空心
        favorites.value.delete(benefit.benefitId);
      } else if (code === 401) {
        await askLogin("登入已過期，請重新登入。");
      } else {
        // 409：有交換刊登時不能刪，直接顯示後端的說明
        await showError("無法取消收藏", apiMessage(error, "請稍後再試一次。"));
      }
    }
  }

  async function toggleFavorite(benefit) {
    const id = benefit?.benefitId;
    if (id == null || isBusy(id)) return;

    if (!loggedIn()) {
      await askLogin("登入會員後就能收藏特典，之後可以在「我的收藏」上傳照片。");
      return;
    }

    busyIds.value.add(id);
    try {
      if (isFavorite(id)) await removeFavorite(benefit);
      else await addFavorite(benefit);
    } finally {
      busyIds.value.delete(id);
    }
  }

  return { loadFavorites, isFavorite, isBusy, toggleFavorite };
}
