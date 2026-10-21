const FORBIDDEN_TITLE = "您沒有權限使用此功能";
const FORBIDDEN_MESSAGE = "此功能僅限管理員帳號使用";
const LOGIN_TITLE = "請先登入";
const LOGIN_MESSAGE = "請先登入後再使用此功能。";
const RETRY_MESSAGE = "請確認連線後再試一次。";

function hasToken() {
  return Boolean(localStorage.getItem("token"));
}

/** 取出 API 回傳的 message；沒有可用文字時改用 fallback。 */
export function apiMessage(error, fallback) {
  const message = error?.response?.data?.message;
  return typeof message === "string" && message.trim() ? message.trim() : fallback;
}

/** 401／403 的提示文字；其他狀態回傳空字串，讓呼叫端改顯示 API 訊息。 */
export function authText(error) {
  const status = error?.response?.status;
  if (status === 403) return FORBIDDEN_TITLE;
  if (status === 401) return LOGIN_TITLE;
  return "";
}

function authCopy(status) {
  if (status === 403) return { title: FORBIDDEN_TITLE, message: FORBIDDEN_MESSAGE };
  if (status === 401 || !hasToken()) return { title: LOGIN_TITLE, message: LOGIN_MESSAGE };
  return null;
}

/** 列表載入失敗。fallbackTitle 依頁面資源而不同，例如「目前無法取得電影資料」。 */
export function collectionLoadError(status, fallbackTitle) {
  return authCopy(status) ?? { title: fallbackTitle, message: RETRY_MESSAGE };
}

/** 單筆載入失敗。一般錯誤的說明會優先使用 API 回傳訊息。 */
export function detailLoadError(error, fallbackTitle) {
  return (
    authCopy(error?.response?.status) ?? {
      title: fallbackTitle,
      message: apiMessage(error, RETRY_MESSAGE),
    }
  );
}
