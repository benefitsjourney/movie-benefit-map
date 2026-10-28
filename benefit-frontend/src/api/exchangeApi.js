//  ========== api/exchangeApi.js ========== 
import http from "@/plugins/axios.js";

// 交易專區 API，使用專案原本的 Axios instance：@/plugins/axios.js，不在這裡重新建立 Axios，也不另外修改 baseURL。
function authConfig(token) {
    if (!token) {
        throw new Error("尚未取得登入 Token，請先登入。");
    }

    return {
        headers: {
            Authorization: `Bearer ${token}`,
        },
    };
}

// ========== 交換刊登 Exchange Listing ==========
// 查詢公開交換刊登
export const getExchangeListings = (params = {}) =>
    http.get("/exchange-listings", { params });

// 查詢單筆交換刊登
export const getExchangeListingById = (id) =>
    http.get(`/exchange-listings/${id}`);

// 查詢自己的交換刊登
export const getMyExchangeListings = (token) =>
    http.get("/exchange-listings/my", authConfig(token));

// 新增交換刊登
export const createExchangeListing = (data, token) =>
    http.post("/exchange-listings", data, authConfig(token));

// 修改交換刊登
export const updateExchangeListing = (id, data, token) =>
    http.put(`/exchange-listings/${id}`, data, authConfig(token));

// 修改交換刊登狀態
export const updateExchangeListingStatus = (id, status, token) =>
    http.patch(
        `/exchange-listings/${id}/status`,
        { status },
        authConfig(token),
    );

// ========== 交換收藏 Exchange Favorite ==========
// 查詢自己的交換收藏
export const getMyExchangeFavorites = (token) =>
    http.get("/exchange-favorites/my", authConfig(token));

// 收藏交換刊登
export const addExchangeFavorite = (listingId, token) =>
    http.post(
        `/exchange-favorites/${listingId}`,
        {},
        authConfig(token),
    );

// 取消交換收藏
export const removeExchangeFavorite = (listingId, token) =>
    http.delete(
        `/exchange-favorites/${listingId}`,
        authConfig(token),
    );

// ========== 票券刊登 Ticket Listing ==========
// 查詢公開票券刊登
export const getTicketListings = (params = {}) =>
    http.get("/ticket-listings", { params });

// 查詢單筆票券刊登
export const getTicketListingById = (id) =>
    http.get(`/ticket-listings/${id}`);

// 查詢自己的票券刊登
export const getMyTicketListings = (token) =>
    http.get("/ticket-listings/my", authConfig(token));

// 新增票券刊登
export const createTicketListing = (data, token) =>
    http.post("/ticket-listings", data, authConfig(token));

// 修改票券刊登
export const updateTicketListing = (id, data, token) =>
    http.put(`/ticket-listings/${id}`, data, authConfig(token));

// 修改票券刊登狀態
export const updateTicketListingStatus = (id, status, token) =>
    http.patch(
        `/ticket-listings/${id}/status`,
        { status },
        authConfig(token),
    );

// ========== 票券收藏 Ticket Favorite ==========
// 查詢自己的票券收藏
export const getMyTicketFavorites = (token) =>
    http.get("/ticket-favorites/my", authConfig(token));

// 收藏票券刊登
export const addTicketFavorite = (listingId, token) =>
    http.post(
        `/ticket-favorites/${listingId}`,
        {},
        authConfig(token),
    );

// 取消票券收藏
export const removeTicketFavorite = (listingId, token) =>
    http.delete(
        `/ticket-favorites/${listingId}`,
        authConfig(token),
    );

// ========== 其他共用選項 API ==========
// 注意：以下三個路徑仍須與負責該功能的組員確認。本次先保留原本前端使用的路徑，不代表已確認後端一定有這些 API。
export const getCinemas = () =>
    http.get("/cinemas/explore");

export const getMovies = () =>
    http.get("/movies/explore");

export const getMyUseBenefits = (token) =>
    http.get("/use-benefits/me", authConfig(token));