// ========== src/api/exchangeconversationApi.js ==========
import http from "@/plugins/axios.js";

// ========== 交易專區聊天室 API ==========
// 提供特典交換與票券交換共用。
// 負責向後端建立或取得與指定會員的聊天室。

/**
 * 建立或取得與指定會員的聊天室。
 *
 * @param {number|string} targetUserId 刊登者的會員 ID
 * @param {string} token 目前登入會員的 Token
 * @returns {Promise} 後端回傳的聊天室資料
 */
export const getOrCreateExchangeConversation = (
    targetUserId,
    token,
) => {
    if (!token) {
        throw new Error("尚未取得登入 Token，請先登入。");
    }

    if (
        targetUserId === null ||
        targetUserId === undefined ||
        targetUserId === ""
    ) {
        throw new Error("找不到刊登者的會員 ID，無法開啟聊天室。");
    }

    return http.post(
        `/conversations/${targetUserId}`,
        {},
        {
            headers: {
                Authorization: `Bearer ${token}`,
            },
        },
    );
};
