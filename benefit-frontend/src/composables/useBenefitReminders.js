import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import http from "@/plugins/axios";
import { openAppAlert } from "@/components/ui/Alert.vue";

/**
 * 追蹤特典／電影（顯示在行事曆、我的追蹤頁）。
 * 追蹤狀態放在檔案層級，所以同一頁有好幾顆追蹤按鈕時會一起更新。
 *
 * loadReminders()：GET /reminders/me，記下我追蹤了哪些特典、哪些電影（沒登入就當作都沒追蹤）
 *
 * 特典：
 *   isFollowing(benefitId)、isBusy(benefitId)
 *   toggleFollow(benefit)：沒追蹤→追蹤（POST /reminders）；已追蹤→取消（DELETE /reminders/{id}）
 *   benefit 要有 benefitId、benefitName
 *
 * 電影：
 *   isMovieFollowing(movieId)、isMovieBusy(movieId)
 *   toggleMovieFollow(movie)：同上，movie 要有 movieId、movieName
 */

// benefitId → reminderId
const followMap = ref(new Map());
// movieId → reminderId
const movieFollowMap = ref(new Map());
// 正在送出的 id，避免連點
const busyIds = ref(new Set());
const movieBusyIds = ref(new Set());
let loadingPromise = null;
let loadedToken = null;

// 特典、電影各自的設定（共用下面同一套追蹤邏輯）
const TARGETS = {
  BENEFIT: {
    map: followMap,
    busy: busyIds,
    idKey: "benefitId",
    nameOf: (item) => (item.benefitName ? `「${item.benefitName}」` : "這個特典"),
    dateText: "日期",
    loginText: "登入後就能追蹤特典，開始日和截止日會出現在你的行事曆上。",
  },
  MOVIE: {
    map: movieFollowMap,
    busy: movieBusyIds,
    idKey: "movieId",
    nameOf: (item) => (item.movieName ? `《${item.movieName}》` : "這部電影"),
    dateText: "上映日",
    loginText: "登入後就能追蹤電影，上映日會出現在你的行事曆上。",
  },
};

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
      movieFollowMap.value = new Map();
      loadedToken = null;
      return;
    }
    // 同一個登入身分已經查過，就不用重查
    if (!force && loadedToken === token) return;
    // 同時有好幾顆按鈕要查時，共用同一次查詢
    if (loadingPromise) return loadingPromise;

    loadingPromise = (async () => {
      try {
        const { data } = await http.get("/reminders/me");
        const benefits = new Map();
        const movies = new Map();
        for (const item of Array.isArray(data) ? data : []) {
          if (item.reminderType === "MOVIE") {
            if (item.movieId != null) movies.set(item.movieId, item.reminderId);
          } else if (item.benefitId != null) {
            benefits.set(item.benefitId, item.reminderId);
          }
        }
        followMap.value = benefits;
        movieFollowMap.value = movies;
        loadedToken = token;
      } catch {
        // 載入失敗不擋畫面，先全部顯示「追蹤」
        followMap.value = new Map();
        movieFollowMap.value = new Map();
      } finally {
        loadingPromise = null;
      }
    })();
    return loadingPromise;
  }

  // ── 共用邏輯（type 是 BENEFIT 或 MOVIE） ──
  function has(type, id) {
    return TARGETS[type].map.value.has(id);
  }

  function busy(type, id) {
    return TARGETS[type].busy.value.has(id);
  }

  function setBusy(type, id, on) {
    const target = TARGETS[type];
    const next = new Set(target.busy.value);
    if (on) next.add(id);
    else next.delete(id);
    target.busy.value = next;
  }

  function setFollow(type, id, reminderId) {
    const target = TARGETS[type];
    const next = new Map(target.map.value);
    if (reminderId == null) next.delete(id);
    else next.set(id, reminderId);
    target.map.value = next;
  }

  async function followTarget(type, item) {
    const id = item[TARGETS[type].idKey];
    try {
      const body = type === "MOVIE"
        ? { reminderType: "MOVIE", movieId: id }
        : { reminderType: "BENEFIT", benefitId: id };
      const { data } = await http.post("/reminders", body);
      setFollow(type, id, data.reminderId);
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

  async function unfollowTarget(type, item, { confirm = false } = {}) {
    const target = TARGETS[type];
    const id = item[target.idKey];
    const reminderId = target.map.value.get(id);
    if (reminderId == null) return false;

    if (confirm) {
      const result = await openAppAlert({
        variant: "warning",
        title: "取消追蹤？",
        text: `${target.nameOf(item)}的${target.dateText}就不會再出現在你的行事曆上。`,
        confirmText: "取消追蹤",
        cancelText: "保留",
      });
      if (!result.isConfirmed) return false;
    }

    try {
      await http.delete(`/reminders/${reminderId}`);
      setFollow(type, id, null);
      return true;
    } catch (error) {
      const code = error?.response?.status;
      if (code === 404) {
        // 已經不存在了，當作取消成功
        setFollow(type, id, null);
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
  async function toggleTarget(type, item, options = {}) {
    const target = TARGETS[type];
    const id = item?.[target.idKey];
    if (id == null || busy(type, id)) return false;
    if (!loggedIn()) {
      await askLogin(target.loginText);
      return false;
    }
    setBusy(type, id, true);
    try {
      return has(type, id)
        ? await unfollowTarget(type, item, options)
        : await followTarget(type, item);
    } finally {
      setBusy(type, id, false);
    }
  }

  // ── 特典（原本的名稱不變，其他頁面照舊使用） ──
  const isFollowing = (benefitId) => has("BENEFIT", benefitId);
  const isBusy = (benefitId) => busy("BENEFIT", benefitId);
  const toggleFollow = (benefit, options) => toggleTarget("BENEFIT", benefit, options);
  const unfollow = (benefit, options) => unfollowTarget("BENEFIT", benefit, options);

  // ── 電影 ──
  const isMovieFollowing = (movieId) => has("MOVIE", movieId);
  const isMovieBusy = (movieId) => busy("MOVIE", movieId);
  const toggleMovieFollow = (movie, options) => toggleTarget("MOVIE", movie, options);

  return {
    loadReminders,
    isFollowing,
    isBusy,
    toggleFollow,
    unfollow,
    isMovieFollowing,
    isMovieBusy,
    toggleMovieFollow,
  };
}
