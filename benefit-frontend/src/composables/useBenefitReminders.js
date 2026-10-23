import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import http from "@/plugins/axios";
import { openAppAlert } from "@/components/ui/Alert.vue";

/**
 * 追蹤特典（顯示在行事曆、我的追蹤頁）。
 * 追蹤狀態放在檔案層級，所以同一頁有好幾顆追蹤按鈕時會一起更新。
 *
 * loadReminders()：GET /reminders/me，記下我追蹤了哪些特典（沒登入就當作都沒追蹤）
 * isFollowing(benefitId)：有沒有追蹤
 * toggleFollow(benefit)：沒追蹤→追蹤（POST /reminders）；已追蹤→取消（DELETE /reminders/{id}）
 * benefit 要有 benefitId、benefitName
 */

// benefitId → reminderId
const followMap = ref(new Map());
// 正在送出的 benefitId，避免連點
const busyIds = ref(new Set());
let loadingPromise = null;
let loadedToken = null;

const loggedIn = () => Boolean(localStorage.getItem("token"));

function apiMessage(error, fallback) {
  const message = error?.response?.data?.message;
  return typeof message === "string" && message.trim() ? message.trim() : fallback;
}

export function useBenefitReminders() {
  const router = useRouter();
  const route = useRoute();

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

  /** force：一定重新查一次（例如在別的地方新增或取消了追蹤） */
  async function loadReminders(force = false) {
    const token = localStorage.getItem("token");
    if (!token) {
      followMap.value = new Map();
      loadedToken = null;
      return;
    }
    // 同一個登入身分已經查過，就不用重查
    if (!force && loadedToken === token) return;
    if (loadingPromise) return loadingPromise;

    loadingPromise = (async () => {
      try {
        const { data } = await http.get("/reminders/me");
        const next = new Map();
        for (const item of Array.isArray(data) ? data : []) {
          if (item.benefitId != null) next.set(item.benefitId, item.reminderId);
        }
        followMap.value = next;
        loadedToken = token;
      } catch {
        // 載入失敗不擋畫面，先全部顯示「追蹤」
        followMap.value = new Map();
      } finally {
        loadingPromise = null;
      }
    })();
    return loadingPromise;
  }

  function isFollowing(benefitId) {
    return followMap.value.has(benefitId);
  }

  function isBusy(benefitId) {
    return busyIds.value.has(benefitId);
  }

  function setBusy(benefitId, on) {
    const next = new Set(busyIds.value);
    if (on) next.add(benefitId);
    else next.delete(benefitId);
    busyIds.value = next;
  }

  function setFollow(benefitId, reminderId) {
    const next = new Map(followMap.value);
    if (reminderId == null) next.delete(benefitId);
    else next.set(benefitId, reminderId);
    followMap.value = next;
  }

  async function follow(benefit) {
    try {
      const { data } = await http.post("/reminders", {
        reminderType: "BENEFIT",
        benefitId: benefit.benefitId,
      });
      setFollow(benefit.benefitId, data.reminderId);
      return true;
    } catch (error) {
      const code = error?.response?.status;
      if (code === 409) {
        // 其實已經追蹤過了（例如在別的分頁按的），重新同步一次
        await loadReminders(true);
        return true;
      }
      if (code === 401) {
        await askLogin("登入已過期，請重新登入後再追蹤。");
      } else {
        await openAppAlert({
          variant: "error",
          title: "追蹤失敗",
          text: apiMessage(error, "請稍後再試一次。"),
          confirmText: "關閉",
          cancelText: "",
        });
      }
      return false;
    }
  }

  async function unfollow(benefit, { confirm = false } = {}) {
    const reminderId = followMap.value.get(benefit.benefitId);
    if (reminderId == null) return false;

    if (confirm) {
      const name = benefit.benefitName ? `「${benefit.benefitName}」` : "這個特典";
      const result = await openAppAlert({
        variant: "warning",
        title: "取消追蹤？",
        text: `${name}的日期就不會再出現在你的行事曆上。`,
        confirmText: "取消追蹤",
        cancelText: "保留",
      });
      if (!result.isConfirmed) return false;
    }

    try {
      await http.delete(`/reminders/${reminderId}`);
      setFollow(benefit.benefitId, null);
      return true;
    } catch (error) {
      const code = error?.response?.status;
      if (code === 404) {
        // 已經不存在了，當作取消成功
        setFollow(benefit.benefitId, null);
        return true;
      }
      if (code === 401) {
        await askLogin("登入已過期，請重新登入。");
      } else {
        await openAppAlert({
          variant: "error",
          title: "取消追蹤失敗",
          text: apiMessage(error, "請稍後再試一次。"),
          confirmText: "關閉",
          cancelText: "",
        });
      }
      return false;
    }
  }

  /** 回傳 true 代表狀態有改變 */
  async function toggleFollow(benefit, options = {}) {
    if (!benefit?.benefitId || isBusy(benefit.benefitId)) return false;
    if (!loggedIn()) {
      await askLogin("登入後就能追蹤特典，開始日和截止日會出現在你的行事曆上。");
      return false;
    }
    setBusy(benefit.benefitId, true);
    try {
      return isFollowing(benefit.benefitId)
        ? await unfollow(benefit, options)
        : await follow(benefit);
    } finally {
      setBusy(benefit.benefitId, false);
    }
  }

  return {
    loadReminders,
    isFollowing,
    isBusy,
    toggleFollow,
    unfollow,
  };
}
