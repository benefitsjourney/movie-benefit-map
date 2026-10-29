// ========== src/composables/useExchangeData.js ==========
// 管理資料狀態、API 資料載入、資料解析、影城／電影名稱及錯誤訊息。
import { ref } from "vue";

import {
    getExchangeListings,
    getMyExchangeListings,
    getMyExchangeFavorites,
    getTicketListings,
    getMyTicketListings,
    getMyTicketFavorites,
    getCinemas,
    getMovies,
    getMyUseBenefits,
} from "@/api/exchangeApi.js";

export function useExchangeData({ token, onAuthExpired }) {
    // ========== 各類資料 ==========
    const exchangeListings = ref([]);
    const ticketListings = ref([]);
    const myExchangeListings = ref([]);
    const myTicketListings = ref([]);
    const myExchangeFavorites = ref([]);
    const myTicketFavorites = ref([]);
    const cinemas = ref([]);
    const movies = ref([]);
    const useBenefits = ref([]);

    const loading = ref(false);
    const errorMessage = ref("");

    // ========== 刊登狀態選項 ==========
    const statusOptions = [
        { value: "ACTIVE", label: "刊登中" },
        { value: "CLOSED", label: "已結束" },
        { value: "DELETED", label: "已刪除" },
    ];

    // ========== 通用資料處理 ==========
    function unwrapList(response) {
        const data = response?.data ?? response;

        if (Array.isArray(data)) return data;
        if (Array.isArray(data?.content)) return data.content;
        if (Array.isArray(data?.list)) return data.list;
        if (Array.isArray(data?.data)) return data.data;
        if (Array.isArray(data?.items)) return data.items;

        return [];
    }

    function firstValue(object, keys, fallback = "") {
        for (const key of keys) {
            const value = object?.[key];

            if (
                value !== undefined &&
                value !== null &&
                value !== ""
            ) {
                return value;
            }
        }

        return fallback;
    }

    function listingId(item, kind) {
        if (kind === "exchange") {
            return firstValue(item, [
                "exchangeListingId",
                "exchange_listing_id",
                "id",
            ]);
        }

        return firstValue(item, [
            "ticketListingId",
            "ticket_listing_id",
            "id",
        ]);
    }

    function getUseBenefitId(item) {
        return firstValue(item, [
            "useBenefitId",
            "use_benefit_id",
            "useBenefit_id",
            "id",
        ]);
    }

    function useBenefitTitle(item) {
        return firstValue(
            item,
            ["title", "benefitName", "name", "benefitTitle"],
            `持有特典 #${getUseBenefitId(item)}`,
        );
    }

    function useBenefitImage(item) {
        // 1. 優先使用會員持有特典的第一張照片。
        const heldPhotoUrl = firstValue(item?.photos?.[0], [
            "photoUrl",
            "photo_url",
        ]);

        if (heldPhotoUrl) return heldPhotoUrl;

        // 2. 沒有會員持有照片時，使用官方特典圖片。
        const officialImageUrl = firstValue(item, [
            "officialImageUrl",
            "official_image_url",
        ]);

        if (officialImageUrl) return officialImageUrl;

        // 3. 保留原本的圖片欄位作為最後備援。
        return firstValue(item, [
            "imageUrl",
            "image_url",
            "benefitImageUrl",
            "benefit_image_url",
            "image",
        ]);
    }

    function statusLabel(status) {
        const matchedStatus = statusOptions.find(
            (item) => item.value === status,
        );

        if (matchedStatus) return matchedStatus.label;

        // 舊資料若仍回傳已移除的 HIDDEN，顯示為「已結束」。
        if (status === "HIDDEN") return "已結束";

        return status || "未知";
    }

    function formatDate(value) {
        if (!value) return "未提供";

        const text = String(value);

        return text.includes("T")
            ? text.split("T")[0]
            : text;
    }

    function toDateInputValue(value) {
        if (!value) return "";

        return String(value).slice(0, 10);
    }

    function toLocalDateTime(value) {
        const date = String(value ?? "").trim();

        const match = date.match(
            /^(\d{4})-(\d{2})-(\d{2})$/,
        );

        if (!match) {
            throw new Error(
                "放映日期格式錯誤，請重新選擇日期。",
            );
        }

        const year = Number(match[1]);
        const month = Number(match[2]);
        const day = Number(match[3]);

        if (month < 1 || month > 12) {
            throw new Error(
                "放映月份無效，請重新選擇日期。",
            );
        }

        const isLeapYear =
            year % 400 === 0 ||
            (year % 4 === 0 && year % 100 !== 0);

        const daysInMonth = [
            31,
            isLeapYear ? 29 : 28,
            31,
            30,
            31,
            30,
            31,
            31,
            30,
            31,
            30,
            31,
        ];

        if (
            day < 1 ||
            day > daysInMonth[month - 1]
        ) {
            throw new Error(
                "放映日期不存在，請重新選擇日期。",
            );
        }

        return `${date} 00:00:00`;
    }

    // ========== 影城、電影資料 ==========
    function cinemaId(item) {
        return firstValue(item, [
            "cinemaId",
            "cinema_id",
            "id",
        ]);
    }

    function movieId(item) {
        return firstValue(item, [
            "movieId",
            "movie_id",
            "id",
        ]);
    }

    function cinemaOptionName(item) {
        return firstValue(
            item,
            ["cinemaName", "name", "cinema_name", "title"],
            `影城 #${cinemaId(item)}`,
        );
    }

    function movieOptionName(item) {
        return firstValue(
            item,
            ["movieName", "name", "movie_name", "title"],
            `電影 #${movieId(item)}`,
        );
    }

    function cinemaName(item) {
        const nested = item?.cinemaBean ?? item?.cinema;

        const directName = firstValue(
            item,
            ["cinemaName", "cinema_name"],
            firstValue(nested, ["cinemaName", "name"], ""),
        );

        if (directName) return directName;

        const id = firstValue(
            item,
            ["cinemaId", "cinema_id"],
            firstValue(
                nested,
                ["cinemaId", "cinema_id", "id"],
                "",
            ),
        );

        const match = cinemas.value.find(
            (cinema) =>
                String(cinemaId(cinema)) === String(id),
        );

        if (match) return cinemaOptionName(match);

        return id
            ? `影城編號 ${id}`
            : "影城資料未提供";
    }

    function movieName(item) {
        const nested = item?.movieBean ?? item?.movie;

        const directName = firstValue(
            item,
            ["movieName", "movie_name"],
            firstValue(nested, ["movieName", "name"], ""),
        );

        if (directName) return directName;

        const id = firstValue(
            item,
            ["movieId", "movie_id"],
            firstValue(
                nested,
                ["movieId", "movie_id", "id"],
                "",
            ),
        );

        const match = movies.value.find(
            (movie) =>
                String(movieId(movie)) === String(id),
        );

        if (match) return movieOptionName(match);

        return id
            ? `電影編號 ${id}`
            : "電影資料未提供";
    }

    // ========== 收藏資料處理 ==========
    function favoriteListing(favorite, kind) {
        if (kind === "exchange") {
            return (
                favorite?.exchangeListing ??
                favorite?.exchangeListingBean ??
                favorite?.listing ??
                favorite
            );
        }

        return (
            favorite?.ticketListing ??
            favorite?.ticketListingBean ??
            favorite?.listing ??
            favorite
        );
    }

    function isFavorited(id, kind) {
        const favorites =
            kind === "exchange"
                ? myExchangeFavorites.value
                : myTicketFavorites.value;

        return favorites.some((favorite) => {
            const listing = favoriteListing(favorite, kind);

            return (
                String(listingId(listing, kind)) ===
                String(id)
            );
        });
    }

    // ========== 錯誤訊息 ==========
    function errorText(error) {
        const status = error?.response?.status;

        if (status === 401) {
            return "登入憑證未通過驗證或已過期，請確認登入狀態。";
        }

        if (status === 403) {
            return "目前帳號沒有執行此操作的權限。";
        }

        if (status === 404) {
            return "找不到 API 路徑，請確認後端 Controller 與 Axios proxy 設定。";
        }

        return (
            error?.response?.data?.message ||
            error?.response?.data?.detail ||
            error?.message ||
            "發生未知錯誤。"
        );
    }

    // ========== 登入失效判斷 ==========
    function isUnauthorized(error) {
        return error?.response?.status === 401;
    }

    // ========== 載入公開刊登 ==========
    async function loadPublicListings() {
        loading.value = true;
        errorMessage.value = "";

        try {
            const [
                exchangeResponse,
                ticketResponse,
            ] = await Promise.all([
                getExchangeListings(),
                getTicketListings(),
            ]);

            exchangeListings.value =
                unwrapList(exchangeResponse);

            ticketListings.value =
                unwrapList(ticketResponse);
        } catch (error) {
            if (isUnauthorized(error) && token.value) {
                await onAuthExpired?.(error);
                return false;
            }

            errorMessage.value =
                `公開刊登載入失敗：${errorText(error)}`;

            return false;
        } finally {
            loading.value = false;
        }

        return true;
    }

    // ========== 載入會員資料 ==========
    async function loadMemberData() {
        if (!token.value) return false;

        loading.value = true;
        errorMessage.value = "";

        try {
            const results = await Promise.allSettled([
                getMyExchangeListings(token.value),
                getMyTicketListings(token.value),
                getMyExchangeFavorites(token.value),
                getMyTicketFavorites(token.value),
                getMyUseBenefits(token.value),
            ]);

            // 先統一檢查是否有 401。
            // 避免同一批請求同時失敗時，產生多項重複錯誤。
            const unauthorizedResult = results.find(
                (result) =>
                    result.status === "rejected" &&
                    isUnauthorized(result.reason),
            );

            if (unauthorizedResult) {
                await onAuthExpired?.(
                    unauthorizedResult.reason,
                );
                return false;
            }

            const setters = [
                (response) => {
                    myExchangeListings.value =
                        unwrapList(response);
                },
                (response) => {
                    myTicketListings.value =
                        unwrapList(response);
                },
                (response) => {
                    myExchangeFavorites.value =
                        unwrapList(response).filter((favorite) => {
                            return (
                                String(favorite?.status ?? "")
                                    .trim()
                                    .toUpperCase() === "ACTIVE"
                            );
                        });
                },
                (response) => {
                    myTicketFavorites.value =
                        unwrapList(response).filter((favorite) => {
                            return (
                                String(favorite?.status ?? "")
                                    .trim()
                                    .toUpperCase() === "ACTIVE"
                            );
                        });
                },
                (response) => {
                    useBenefits.value =
                        unwrapList(response);
                },
            ];

            const labels = [
                "我的交換刊登",
                "我的票券刊登",
                "我的交換收藏",
                "我的票券收藏",
                "我的持有特典",
            ];

            const failures = [];

            results.forEach((result, index) => {
                if (result.status === "fulfilled") {
                    setters[index](result.value);
                } else {
                    failures.push(
                        `${labels[index]}：${errorText(result.reason)}`,
                    );
                }
            });

            if (failures.length > 0) {
                errorMessage.value =
                    "部分會員資料無法載入：\n" +
                    failures.join("\n") +
                    "\n請確認失敗 API 的路徑、登入權限與回傳格式。";
            }

            return failures.length === 0;
        } finally {
            loading.value = false;
        }
    }

    // ========== 載入影城、電影選項 ==========
    async function loadSelectionOptions() {
        const results = await Promise.allSettled([
            getCinemas(),
            getMovies(),
        ]);

        if (results[0].status === "fulfilled") {
            cinemas.value = unwrapList(results[0].value);
        }

        if (results[1].status === "fulfilled") {
            movies.value = unwrapList(results[1].value);
        }
    }

    return {
        exchangeListings,
        ticketListings,
        myExchangeListings,
        myTicketListings,
        myExchangeFavorites,
        myTicketFavorites,
        cinemas,
        movies,
        useBenefits,
        loading,
        errorMessage,
        statusOptions,

        unwrapList,
        firstValue,
        listingId,
        getUseBenefitId,
        useBenefitTitle,
        useBenefitImage,
        statusLabel,
        formatDate,
        toDateInputValue,
        toLocalDateTime,

        cinemaId,
        movieId,
        cinemaOptionName,
        movieOptionName,
        cinemaName,
        movieName,

        favoriteListing,
        isFavorited,
        errorText,

        loadPublicListings,
        loadMemberData,
        loadSelectionOptions,
    };
}