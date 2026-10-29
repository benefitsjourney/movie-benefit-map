<!-- ========== components/exchange/ExchangeListingSection.vue ========== -->
<!-- 負責公開交換刊登卡片、搜尋、分頁、收藏、詳細視窗及聊天按鈕。 -->
<!-- 透過 props 接收資料，不直接呼叫 API。 -->

<script setup>
import {
    computed,
    ref,
    watch,
    onMounted,
    onBeforeUnmount,
} from "vue";
import { Icon } from "@iconify/vue";
import giftIcon from "@iconify-icons/mdi/gift-outline";
import searchIcon from "@iconify-icons/mdi/magnify";
import compassIcon from "@iconify-icons/mdi/compass-outline";
import accountIcon from "@iconify-icons/mdi/account";
import calendarIcon from "@iconify-icons/mdi/calendar-blank-outline";
import swapIcon from "@iconify-icons/mdi/swap-horizontal";
import heartIcon from "@iconify-icons/mdi/heart";
import heartOutlineIcon from "@iconify-icons/mdi/heart-outline";
import chatIcon from "@iconify-icons/mdi/chat-outline";

import ResultBanner from "@/components/layout/ResultBanner.vue";
import SearchFilter from "@/components/layout/SearchFilter.vue";
import StatePanel from "@/components/layout/StatePanel.vue";
import Pagination from "@/components/ui/Pagination.vue";
import TicketCard from "@/components/ui/TicketCard.vue";
import { useRouter } from "vue-router";

const props = defineProps({
    listings: {
        type: Array,
        default: () => [],
    },
    favorites: {
        type: Array,
        default: () => [],
    },
    isLoggedIn: {
        type: Boolean,
        default: false,
    },
    currentUserId: {
        type: [Number, String],
        default: null,
    },
    loading: {
        type: Boolean,
        default: false,
    },
});

const emit = defineEmits([
    "toggle-favorite",
    "open-chat",
]);
const router = useRouter();

// 進入刊登者的公開會員頁。
function goToMember(item) {
    const memberId =
        item?.usersId ??
        item?.users_id ??
        item?.user?.usersId ??
        item?.users?.usersId ??
        item?.usersBean?.usersId;

    if (
        memberId === null ||
        memberId === undefined ||
        String(memberId).trim() === ""
    ) {
        console.warn("無法開啟會員頁：這筆交換刊登沒有可用的會員 ID。");
        return;
    }

    router.push({
        name: "member-public",
        params: {
            id: String(memberId),
        },
    });
}
// 搜尋及分頁。
const keyword = ref("");
const currentPage = ref(1);
const pageSize = 8;

// 記錄各張交換刊登卡片中，載入失敗的圖片網址。
const failedCardImages = ref({});

// 詳細視窗目前選取的刊登。
const selectedListing = ref(null);

// 詳細視窗目前顯示的圖片索引。
const detailImageIndex = ref(0);

// 記錄詳細視窗中載入失敗的圖片。
const failedDetailImages = ref([]);

// SearchFilter 使用物件型 v-model。
const appliedFilters = computed({
    get: () => ({
        keyword: keyword.value,
    }),
    set: (next) => {
        keyword.value = next?.keyword ?? "";
        currentPage.value = 1;
    },
});

// 搜尋欄位。
const filterFields = [
    {
        key: "keyword",
        label: "關鍵字",
        type: "search",
        placeholder: "輸入標題、刊登者或特典名稱",
        icon: searchIcon,
    },
];

// 取得交換刊登 ID。
function listingId(item) {
    return (
        item?.exchangeListingId ??
        item?.exchange_listing_id ??
        item?.id ??
        ""
    );
}

// 取得圖片狀態的識別鍵。
function imageStateKey(item, index) {
    const id = listingId(item);

    return id !== null && id !== undefined && id !== ""
        ? String(id)
        : `listing-index-${index}`;
}

// 取得會員上傳的持有特典照片，最多使用 5 張。
function memberPhotoCandidates(item) {
    const photos = Array.isArray(item?.useBenefitPhotoUrls)
        ? item.useBenefitPhotoUrls
        : Array.isArray(item?.use_benefit_photo_urls)
            ? item.use_benefit_photo_urls
            : [];

    return [
        ...new Set(
            photos
                .filter((url) => typeof url === "string")
                .map((url) => url.trim())
                .filter(Boolean),
        ),
    ].slice(0, 5);
}

// 取得官方特典圖片網址。
function officialPhotoCandidate(item) {
    const url =
        typeof item?.officialImageUrl === "string"
            ? item.officialImageUrl
            : typeof item?.official_image_url === "string"
                ? item.official_image_url
                : "";

    return url.trim();
}

// 取得海報圖片清單。
// 有會員持有照片：最多顯示 5 張會員照片。
// 沒有會員持有照片：才使用官方特典照片。
function imageCandidates(item) {
    const memberPhotos = memberPhotoCandidates(item);

    if (memberPhotos.length > 0) {
        return memberPhotos;
    }

    const officialPhoto = officialPhotoCandidate(item);

    return officialPhoto ? [officialPhoto] : [];
}

// 取得卡片實際要顯示的圖片。
// 卡片只顯示一張照片。
// 若第一張照片載入失敗，則依序顯示下一張可用照片。
function cardImages(item, index) {
    const key = imageStateKey(item, index);
    const candidates = imageCandidates(item);
    const failedUrls = failedCardImages.value[key] ?? [];

    return candidates
        .filter((url) => !failedUrls.includes(url))
        .slice(0, 1);
}

// 卡片圖片載入失敗時，記錄該網址。
function handleImageError(item, index, url) {
    const key = imageStateKey(item, index);
    const failedUrls = failedCardImages.value[key] ?? [];

    if (!failedUrls.includes(url)) {
        failedCardImages.value = {
            ...failedCardImages.value,
            [key]: [...failedUrls, url],
        };
    }
}

// 取得收藏資料所對應的交換刊登。
function favoriteListing(favorite) {
    return (
        favorite?.exchangeListing ??
        favorite?.exchangeListingBean ??
        favorite?.listing ??
        favorite
    );
}

// 判斷是否已收藏。
function isFavorited(id) {
    return props.favorites.some((favorite) => {
        const listing = favoriteListing(favorite);

        const favoriteId =
            listing?.exchangeListingId ??
            listing?.exchange_listing_id ??
            listing?.id ??
            "";

        return String(favoriteId) === String(id);
    });
}

// 日期只顯示年月日。
function formatDate(value) {
    if (!value) return "未提供";

    return String(value).trim().split(/[T ]/)[0];
}

// 取得刊登者名稱。
function listingMemberName(item) {
    return (
        item?.usersName ||
        item?.userName ||
        item?.username ||
        item?.memberName ||
        ""
    );
}

// 將 HTML 特殊字元轉義，避免 v-html 顯示不安全內容。
function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, (char) => {
        const entities = {
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;",
        };

        return entities[char];
    });
}

// 反白符合目前搜尋關鍵字的文字。
function highlightText(value) {
    const text = String(value ?? "");
    const search = keyword.value.trim();

    if (!search) {
        return escapeHtml(text);
    }

    const escapedKeyword = search.replace(
        /[.*+?^${}()|[\]\\]/g,
        "\\$&",
    );

    const regex = new RegExp(`(${escapedKeyword})`, "giu");

    return text
        .split(regex)
        .map((part, index) => {
            const safeText = escapeHtml(part);

            if (index % 2 === 1) {
                return `<mark class="search-highlight">${safeText}</mark>`;
            }

            return safeText;
        })
        .join("");
}

// 整理可供搜尋的文字。
function searchableText(item) {
    return [
        item?.title,
        item?.usersName,
        item?.userName,
        item?.username,
        item?.memberName,
        item?.benefitName,
    ]
        .filter((value) => value !== null && value !== undefined)
        .join(" ")
        .toLocaleLowerCase();
}

// 關鍵字搜尋。
const filteredListings = computed(() => {
    const search = keyword.value.trim().toLocaleLowerCase();

    if (!search) {
        return props.listings;
    }

    return props.listings.filter((item) =>
        searchableText(item).includes(search),
    );
});

// 搜尋條件改變時回到第一頁。
watch(keyword, () => {
    currentPage.value = 1;
});


/**
 * 公開刊登資料更新時：
 * 1. 不重設目前頁碼，避免背景更新讓使用者跳回第一頁。
 * 2. 不清除圖片載入失敗紀錄，避免每次更新都重試失敗圖片。
 * 3. 詳細視窗同步最新刊登資料；若刊登已不存在，則關閉視窗。
 */

watch(
    () => props.listings,
    (listings) => {
        if (!selectedListing.value) {
            return;
        }

        const selectedId = listingId(selectedListing.value);

        // 沒有有效的刊登 ID，就無法安全配對。
        if (
            selectedId === null ||
            selectedId === undefined ||
            String(selectedId).trim() === ""
        ) {
            closeDetail();
            return;
        }

        const updatedListing = listings.find((item) => {
            const id = listingId(item);

            return (
                id !== null &&
                id !== undefined &&
                String(id).trim() !== "" &&
                String(id).trim() === String(selectedId).trim()
            );
        });

        if (updatedListing) {
            // 刊登仍存在：使用最新資料更新詳細視窗。
            selectedListing.value = updatedListing;
        } else {
            // 刊登已不存在：關閉詳細視窗。
            closeDetail();
        }
    },
);

// 分頁總數至少為 1。
const totalPages = computed(() =>
    Math.max(1, Math.ceil(filteredListings.value.length / pageSize)),
);

// 避免資料減少後停留在不存在的頁數。
watch(totalPages, (total) => {
    if (currentPage.value > total) {
        currentPage.value = total;
    }
});

// 目前頁面顯示的資料。
const paginatedListings = computed(() => {
    const start = (currentPage.value - 1) * pageSize;

    return filteredListings.value.slice(
        start,
        start + pageSize,
    );
});

// 搜尋結果筆數。
const listingCount = computed(
    () => filteredListings.value.length,
);

// 切換頁碼。
function changePage(page) {
    if (page < 1 || page > totalPages.value) return;

    currentPage.value = page;
}

// 收藏事件交由父元件處理。
function toggleFavorite(item) {
    emit("toggle-favorite", item);
}

// 聊天事件交由父元件處理。
function openChat(item) {
    emit("open-chat", item);
}

// 開啟交換刊登詳細內容視窗。
function openDetail(item) {
    if (!item) return;

    selectedListing.value = item;
    detailImageIndex.value = 0;
    failedDetailImages.value = [];
}

// 關閉詳細內容視窗。
function closeDetail() {
    selectedListing.value = null;
    detailImageIndex.value = 0;
    failedDetailImages.value = [];
}

// 詳細視窗中仍可顯示的圖片。
const availableDetailImages = computed(() => {
    if (!selectedListing.value) {
        return [];
    }

    return imageCandidates(selectedListing.value).filter(
        (url) => !failedDetailImages.value.includes(url),
    );
});

// 目前顯示的單張圖片。
const currentDetailImage = computed(
    () => availableDetailImages.value[detailImageIndex.value] ?? "",
);

// 切換至上一張圖片。
function previousDetailImage() {
    const total = availableDetailImages.value.length;

    if (total <= 1) return;

    detailImageIndex.value =
        (detailImageIndex.value - 1 + total) % total;
}

// 切換至下一張圖片。
function nextDetailImage() {
    const total = availableDetailImages.value.length;

    if (total <= 1) return;

    detailImageIndex.value =
        (detailImageIndex.value + 1) % total;
}

// 詳細視窗圖片載入失敗時，排除該圖片。
// 索引保持不變，讓下一張可用圖片接續顯示。
function handleDetailImageError(url) {
    if (!failedDetailImages.value.includes(url)) {
        failedDetailImages.value = [
            ...failedDetailImages.value,
            url,
        ];

        const remaining = availableDetailImages.value.length;

        if (remaining === 0) {
            detailImageIndex.value = 0;
        } else if (detailImageIndex.value >= remaining) {
            detailImageIndex.value = 0;
        }
    }
}

// 詳細視窗中的聊天按鈕。
function openChatFromDetail() {
    if (!selectedListing.value || !props.isLoggedIn) {
        return;
    }

    const item = selectedListing.value;

    closeDetail();
    openChat(item);
}

// 整張卡片的鍵盤操作。
function handleCardKeydown(event, item) {
    if (event.target !== event.currentTarget) return;

    if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        openDetail(item);
    }
}

// Escape 鍵關閉詳細視窗。
function handleEscape(event) {
    if (event.key === "Escape" && selectedListing.value) {
        closeDetail();
    }
}

// 註冊及移除鍵盤事件。
onMounted(() => {
    window.addEventListener("keydown", handleEscape);
});

onBeforeUnmount(() => {
    window.removeEventListener("keydown", handleEscape);
});

// 判斷目前登入會員是否為這筆刊登的刊登者
function isOwnListing(item) {
    const currentUserId = props.currentUserId;
    const targetUserId = item?.usersId;

    if (
        currentUserId === null ||
        currentUserId === undefined ||
        currentUserId === "" ||
        targetUserId === null ||
        targetUserId === undefined ||
        targetUserId === ""
    ) {
        return false;
    }

    return String(currentUserId) === String(targetUserId);
}
</script>

<template>
    <section>
        <!-- 結果統計 -->
        <ResultBanner v-if="!(loading && !listings.length)" :icon="swapIcon" title="特典刊登" :count="listingCount"
            :unit-text="keyword.trim()
                ? '筆符合搜尋條件的刊登'
                : '筆特典'" :empty-text="keyword.trim()
                    ? '找不到符合條件的刊登，換個關鍵字試試吧！'
                    : '目前沒有公開的交換刊登。'" />

        <!-- 搜尋區 -->
        <SearchFilter v-if="!(loading && !listings.length)" v-model="appliedFilters" title="尋找特典"
            :title-icon="compassIcon" submit-label="搜尋特典" :fields="filterFields">
            <!-- 交換刊登卡片 -->
            <div v-if="paginatedListings.length" class="ticket-grid" :class="{ 'is-loading': loading }"
                :aria-busy="loading">
                <div v-for="(item, index) in paginatedListings" :key="listingId(item) || index"
                    class="listing-card-trigger" role="group" tabindex="0"
                    :aria-label="`查看${item.title || '未命名刊登'}的詳細內容`" @click="openDetail(item)"
                    @keydown.self="handleCardKeydown($event, item)">
                    <TicketCard color="green" :appear="index < 6" stamp="特典交換" :title="item.title || '未命名刊登'">
                        <!-- 標題 -->
                        <template #title>
                            <span v-html="highlightText(
                                item.title || '未命名刊登',
                            )" />
                        </template>

                        <!-- 海報區域 -->
                        <template #poster>
                            <div class="poster-wrap">
                                <!-- 有圖片時，顯示所有可用圖片，最多 5 張 -->
                                <div v-if="cardImages(item, index).length" class="poster-image-list" :class="{
                                    'poster-image-list--single':
                                        cardImages(item, index).length === 1,
                                }">
                                    <img v-for="(url, imageIndex) in cardImages(item, index)" :key="url"
                                        class="poster-image" :src="url" :alt="item.benefitName
                                            ? `${item.benefitName}持有照片 ${imageIndex + 1}`
                                            : `交換特典圖片 ${imageIndex + 1}`" loading="lazy"
                                        @error="handleImageError(item, index, url)" />
                                </div>

                                <!-- 沒有可用圖片時，保留原本的禮物圖示 -->
                                <span v-else class="poster-fallback" aria-label="目前沒有可顯示的圖片">
                                    <Icon :icon="giftIcon" aria-hidden="true" />
                                </span>

                                <!-- 收藏按鈕保留原本位置與功能 -->
                                <button type="button" class="favorite-btn" :class="{
                                    'is-on': isFavorited(listingId(item)),
                                }" :aria-pressed="isFavorited(listingId(item))" :aria-label="isFavorited(listingId(item))
                                    ? `取消收藏 ${item.title || '未命名刊登'}`
                                    : `收藏 ${item.title || '未命名刊登'}`" :title="isLoggedIn
                                        ? (
                                            isFavorited(listingId(item))
                                                ? '已收藏（點一下取消）'
                                                : '加入收藏'
                                        )
                                        : '登入後才能收藏'" @click.stop="toggleFavorite(item)" @keydown.stop>
                                    <Icon :icon="isFavorited(listingId(item))
                                        ? heartIcon
                                        : heartOutlineIcon" aria-hidden="true" />
                                </button>
                            </div>
                        </template>

                        <!-- 卡片刊登內容 -->
                        <template #detail>
                            <div class="listing-detail">

                                <!-- 刊登者 -->
                                <div class="listing-publisher" @click.stop @keydown.stop>
                                    <Icon :icon="accountIcon" aria-hidden="true" />

                                    <span class="listing-label">
                                        刊登者
                                    </span>

                                    <button v-if="listingMemberName(item)" type="button" class="member-name-link"
                                        @click.stop="goToMember(item)">
                                        <span v-html="highlightText(listingMemberName(item))"></span>
                                    </button>

                                    <span v-else class="listing-publisher-name">
                                        未提供
                                    </span>
                                </div>

                                <!-- 特典名稱 -->
                                <div class="listing-benefit">
                                    <Icon :icon="giftIcon" aria-hidden="true" />

                                    <span class="listing-label">
                                        特典名稱
                                    </span>

                                    <span class="listing-benefit-name"
                                        v-html="highlightText(item.benefitName || '未提供')"></span>
                                </div>

                                <!-- 刊登說明 -->
                                <p class="listing-description">
                                    {{ item.description || "刊登者尚未提供敘述。" }}
                                </p>

                                <!-- 刊登日期 -->
                                <div class="listing-date">
                                    <Icon :icon="calendarIcon" aria-hidden="true" />

                                    <span class="listing-label">
                                        刊登日期
                                    </span>

                                    <span class="listing-date-value">
                                        {{ formatDate(item.createdAt || item.created_at) }}
                                    </span>
                                </div>

                            </div>
                        </template>

                        <!-- 聯絡刊登者 -->
                        <template #action>
                            <div class="listing-actions" @click.stop @keydown.stop>
                                <button type="button" class="ticket-link chat-link"
                                    :disabled="!isLoggedIn || isOwnListing(item)" :title="!isLoggedIn
                                        ? '登入後才能聯絡刊登者'
                                        : isOwnListing(item)
                                            ? '這是你自己的刊登，無法聯絡自己'
                                            : '聯絡刊登者'" @click.stop="openChat(item)">
                                    <Icon :icon="chatIcon" aria-hidden="true" />
                                    聯絡刊登者
                                </button>
                            </div>
                        </template>
                    </TicketCard>
                </div>
            </div>

            <!-- 搜尋結果為空 -->
            <div v-if="!loading && !filteredListings.length" class="ticket-empty-state" role="status">
                {{
                    keyword.trim()
                        ? "找不到符合條件的刊登，請試試其他關鍵字。"
                        : "目前沒有公開的交換刊登。"
                }}
            </div>

            <!-- 分頁 -->
            <Pagination v-if="totalPages > 1" :page="currentPage" :total-pages="totalPages" label="交換刊登分頁"
                @update:page="changePage" />
        </SearchFilter>

        <!-- 載入狀態 -->
        <StatePanel v-if="loading && !listings.length" variant="loading" message="正在載入交換刊登……" :icon="swapIcon" />

        <!-- 詳細內容視窗 -->
        <Teleport to="body">
            <div v-if="selectedListing" class="detail-modal-overlay" @click.self="closeDetail">
                <section class="detail-modal" role="dialog" aria-modal="true" aria-labelledby="detail-modal-title">
                    <TicketCard appear color="green" class="detail-modal-ticket"
                        :title="selectedListing.title || '未命名刊登'">
                        <!-- 標題及 X 關閉按鈕 -->
                        <template #title>
                            <div class="detail-ticket-heading">
                                <span id="detail-modal-title" class="detail-ticket-title">
                                    {{ selectedListing.title || "未命名刊登" }}
                                </span>

                                <button type="button" class="detail-ticket-close" aria-label="關閉詳細內容" title="關閉"
                                    @click="closeDetail">
                                    <span aria-hidden="true">×</span>
                                </button>
                            </div>
                        </template>

                        <!-- 詳細視窗主要內容 -->
                        <div class="detail-modal-content">
                            <!-- 刊登者 -->
                            <p class="detail-modal-info">
                                <Icon :icon="accountIcon" aria-hidden="true" />
                                <strong class="detail-modal-publisher-label">刊登者</strong>

                                <button v-if="listingMemberName(selectedListing)" type="button" class="member-name-link"
                                    @click.stop="goToMember(selectedListing)">
                                    {{ listingMemberName(selectedListing) }}
                                </button>

                                <span v-else>未提供</span>
                            </p>

                            <!-- 特典名稱 -->
                            <p class="detail-modal-info">
                                <!-- <Icon :icon="giftIcon" aria-hidden="true" /> -->
                                <strong>特典名稱</strong>
                                <span>
                                    {{ selectedListing.benefitName || "未提供" }}
                                </span>
                            </p>

                            <!-- 刊登敘述 -->
                            <section class="detail-modal-section">
                                <h4 class="detail-modal-heading">
                                    <!-- <Icon icon="mdi:text-box-outline" aria-hidden="true" /> -->
                                    <span>刊登敘述</span>
                                </h4>

                                <p class="detail-modal-description">
                                    {{
                                        selectedListing.description ||
                                        "刊登者尚未提供敘述。"
                                    }}
                                </p>
                            </section>

                            <!-- 刊登日期 -->
                            <p class="detail-modal-info">
                                <!-- <Icon :icon="calendarIcon" aria-hidden="true" /> -->
                                <strong>刊登日期</strong>
                                <span>
                                    {{
                                        formatDate(
                                            selectedListing.createdAt ||
                                            selectedListing.created_at,
                                        )
                                    }}
                                </span>
                            </p>

                            <!-- 特典圖片：一次只顯示一張 -->
                            <section class="detail-modal-images">
                                <h4 class="detail-modal-heading">
                                    <!-- <Icon icon="mdi:image-multiple-outline" aria-hidden="true" /> -->
                                    <span>特典圖片</span>
                                </h4>

                                <div v-if="currentDetailImage" class="detail-image-viewer">
                                    <img :key="currentDetailImage" :src="currentDetailImage"
                                        :alt="`特典圖片 ${detailImageIndex + 1}`" class="detail-modal-image"
                                        @error="handleDetailImageError(currentDetailImage)" />

                                    <!-- 只有多張圖片才顯示切換按鈕 -->
                                    <div v-if="availableDetailImages.length > 1" class="detail-image-controls">
                                        <button type="button" class="detail-image-arrow" aria-label="上一張圖片"
                                            title="上一張圖片" @click="previousDetailImage">
                                            <span aria-hidden="true">‹</span>
                                        </button>

                                        <span class="detail-image-count">
                                            {{ detailImageIndex + 1 }}
                                            /
                                            {{ availableDetailImages.length }}
                                        </span>

                                        <button type="button" class="detail-image-arrow" aria-label="下一張圖片"
                                            title="下一張圖片" @click="nextDetailImage">
                                            <span aria-hidden="true">›</span>
                                        </button>
                                    </div>
                                </div>

                                <!-- 沒有可顯示的圖片 -->
                                <div v-else class="detail-modal-no-image">
                                    <Icon :icon="giftIcon" aria-hidden="true" />
                                    <span>目前沒有可顯示的圖片</span>
                                </div>
                            </section>
                        </div>

                        <!-- 詳細視窗底部操作 -->
                        <template #action>
                            <div class="detail-modal-footer">
                                <button type="button" class="ticket-link detail-favorite-button" :class="{
                                    'is-on': isFavorited(
                                        listingId(selectedListing),
                                    ),
                                }" :aria-pressed="isFavorited(
                                    listingId(selectedListing),
                                )" @click="toggleFavorite(selectedListing)">
                                    <Icon :icon="isFavorited(
                                        listingId(selectedListing),
                                    )
                                        ? heartIcon
                                        : heartOutlineIcon" aria-hidden="true" />
                                    {{
                                        isFavorited(listingId(selectedListing))
                                            ? "取消收藏"
                                            : "加入收藏"
                                    }}
                                </button>

                                <button type="button" class="ticket-link is-dark detail-chat-button"
                                    :disabled="!isLoggedIn || isOwnListing(selectedListing)" :title="!isLoggedIn
                                        ? '登入後才能聯絡刊登者'
                                        : isOwnListing(selectedListing)
                                            ? '這是你自己的刊登，無法聯絡自己'
                                            : '聯絡刊登者'" @click="openChatFromDetail">
                                    <Icon :icon="chatIcon" aria-hidden="true" />
                                    聯絡刊登者
                                </button>
                            </div>
                        </template>
                    </TicketCard>
                </section>
            </div>
        </Teleport>
    </section>
</template>

<style scoped>
/* 卡片排列 */

.ticket-grid {
    gap: 1.125rem;
}

@media (min-width: 640px) {
    .ticket-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (min-width: 960px) {
    .ticket-grid {
        grid-template-columns: repeat(3, minmax(0, 1fr));
    }
}

@media (min-width: 1280px) {
    .ticket-grid {
        grid-template-columns: repeat(4, minmax(0, 1fr));
    }
}

/* 整張卡片可點擊 */

.listing-card-trigger {
    min-width: 0;
    cursor: pointer;
    border-radius: 0.75rem;
}

.listing-card-trigger:focus-visible {
    outline: 2px solid #43866b;
    outline-offset: 3px;
}

.listing-card-trigger :deep(.ticket-poster-card) {
    display: flex;
    flex-direction: column;
}

.listing-card-trigger :deep(.ticket-poster-card .ticket-body) {
    order: 1;
    display: flex;
    flex-direction: column;
    flex: 0 0 auto;
}

.listing-card-trigger :deep(.ticket-poster-card .ticket-poster) {
    order: 2;
    flex-shrink: 0;
}

.listing-card-trigger :deep(.ticket-poster-card .ticket-stub) {
    order: 3;
    margin-top: auto;
}

/* 卡片海報區域 */
.poster-wrap {
    position: relative;
    display: grid;
    place-items: center;
    width: 100%;
    height: 100%;
    min-height: 8rem;
}

/* 多張持有照片排列 */
.poster-image-list {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0.25rem;
    width: 100%;
    height: 100%;
    min-height: 8rem;
    overflow: hidden;
}

/* 只有一張圖片時，維持原本的大圖顯示方式 */
.poster-image-list--single {
    grid-template-columns: minmax(0, 1fr);
}

/* 單張圖片 */
.poster-image {
    display: block;
    width: 100%;
    height: 100%;
    min-width: 0;
    min-height: 8rem;
    object-fit: cover;
    border-radius: 0.2rem;
}

/* 多張圖片時，每張圖片都能在網格中顯示 */
.poster-image-list:not(.poster-image-list--single) .poster-image {
    min-height: 3.8rem;
}

.poster-fallback {
    display: grid;
    place-items: center;
    width: 100%;
    height: 100%;
    min-height: 8rem;
}

.poster-fallback :deep(svg) {
    font-size: var(--text-page-title);
}

/* 收藏按鈕 */
.favorite-btn {
    position: absolute;
    top: 0.5rem;
    right: 0.5rem;
    z-index: 1;
    display: grid;
    place-items: center;
    width: 2.6rem;
    height: 2.6rem;
    border: 0;
    border-radius: 50%;
    background: rgb(255 255 255 / 88%);
    color: #a33b6b;
    font-size: 1.45rem;
    cursor: pointer;
    box-shadow: 0 4px 10px rgb(41 43 53 / 18%);
    transition:
        transform var(--ease),
        background-color var(--ease);
}

.favorite-btn:hover {
    background: #fff;
    transform: scale(1.08);
}

.favorite-btn.is-on {
    color: #e0457b;
}

.favorite-btn.is-on :deep(svg) {
    animation: heart-pop 0.35s ease;
}

.favorite-btn:focus-visible {
    outline: 2px solid #e0457b;
    outline-offset: 2px;
}

@media (hover: hover) and (pointer: fine) {
    .favorite-btn:not(.is-on) {
        opacity: 0;
        transition:
            opacity var(--ease),
            transform var(--ease),
            background-color var(--ease);
    }

    .poster-wrap:hover .favorite-btn,
    .favorite-btn:focus-visible {
        opacity: 1;
    }
}

@keyframes heart-pop {
    50% {
        transform: scale(1.3);
    }
}

/* 卡片文字 */
.listing-detail {
    min-width: 0;
}

/* 刊登說明：固定兩行高度，超出部分省略 */
.listing-description {
    display: -webkit-box;
    overflow: hidden;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    white-space: pre-line;
    /* 增加縮排 */
    margin: 0.5rem 0.5rem 0;
    /* 增加左側縮排 */
    padding: 0 1rem;
    box-sizing: border-box;
    color: #3e4450;
    font-size: 0.92rem;
    line-height: 1.8;
    overflow-wrap: anywhere;
    /* 固定保留兩行文字的高度 */
    height: calc(0.92rem * 1.8 * 2);
    min-height: calc(0.92rem * 1.8 * 2);
}


:deep(.ticket-body h2 .search-highlight),
.listing-publisher :deep(.search-highlight),
.listing-benefit :deep(.search-highlight) {
    padding: 0 0.08em;
    border-radius: 0.15em;
    background-color: #ffe082;
    color: #4a3410;
    font-weight: 700;
}

/* 刊登者 */
.listing-publisher {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.4rem;
    margin: 0 0 0.7rem;
    padding-bottom: 0.7rem;
    border-bottom: 1px solid #f1f5f9;
    color: #475569;
    font-size: 1rem;
    line-height: 1.6;
}

.listing-publisher :deep(svg) {
    flex-shrink: 0;
    width: 1rem;
    height: 1rem;
}

/* 共用欄位標籤 */
.listing-label {
    color: #64748b;
    font-weight: 600;
}

/* 沒有提供刊登者名稱 */
.listing-publisher-name {
    min-width: 0;
    color: #334155;
    font-weight: 750;
    overflow-wrap: anywhere;
}

/* 特典名稱：限制單行，超出部分省略 */
.listing-benefit {
    display: flex;
    align-items: center;
    flex-wrap: nowrap;
    gap: 0.4rem;
    min-width: 0;
    margin: 0.5rem 0 0;
    color: #475569;
    font-size: 1rem;
    line-height: 1.6;
}

.listing-benefit :deep(svg) {
    flex-shrink: 0;
    width: 1rem;
    height: 1rem;
}

/* 特典名稱：不可換行，超出部分顯示省略號 */
.listing-benefit-name {
    display: block;
    flex: 1 1 0;
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    color: #334155;
    font-weight: 750;
}

/* 刊登日期 */
.listing-date {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.4rem;
    margin: 0.5rem 0 0;
    color: #475569;
    font-size: 1rem;
    line-height: 1.6;
}

.listing-date :deep(svg) {
    flex-shrink: 0;
    width: 1rem;
    height: 1rem;
}

.listing-date-value {
    min-width: 0;
    color: #334155;
    overflow-wrap: anywhere;
}

/* 聯絡刊登者 */
.listing-actions {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.5rem;
}

.chat-link {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.35rem;
    cursor: pointer;
}

.chat-link:disabled {
    opacity: 0.55;
    cursor: not-allowed;
}

/* 空白搜尋結果 */

.ticket-empty-state {
    padding: 2rem 1rem;
    border: 1px dashed #cbd5e1;
    border-radius: 1rem;
    background: #f8fafc;
    color: #64748b;
    font-size: 0.95rem;
    line-height: 1.8;
    text-align: center;
}

/* 詳細內容視窗 */

:global(.detail-modal-overlay) {
    position: fixed;
    inset: 0;
    z-index: 1000;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 1rem;
    background: rgb(15 23 42 / 62%);
    overflow-y: auto;
}

:global(.detail-modal) {
    width: min(100%, 42rem);
    min-width: 0;
    max-height: min(90vh, 56rem);
    max-height: min(90dvh, 56rem);
    overflow-x: hidden;
    overflow-y: auto;
    overscroll-behavior: contain;
}

.detail-modal-ticket {
    width: 100%;
    min-width: 0;
}

.detail-modal-ticket :deep(.ticket-body h2) {
    width: 100%;
    min-width: 0;
}

/* 標題及 X 按鈕 */

.detail-ticket-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.75rem;
    width: 100%;
    min-width: 0;
}

.detail-ticket-title {
    flex: 1;
    min-width: 0;
    overflow-wrap: anywhere;
}

.detail-ticket-close {
    display: inline-grid;
    flex-shrink: 0;
    place-items: center;
    width: 2.25rem;
    height: 2.25rem;
    padding: 0;
    border: 1.5px solid var(--ticket-ink);
    border-radius: 0.7rem;
    background: #fff;
    color: var(--ticket-ink);
    font-family: inherit;
    font-size: 1.65rem;
    font-weight: 500;
    line-height: 1;
    cursor: pointer;
    transition:
        background-color 0.2s ease,
        color 0.2s ease;
}

.detail-ticket-close:hover {
    background: var(--accent);
    color: var(--c-ink);
}

.detail-ticket-close:focus-visible {
    outline: 2px solid var(--ticket-ink);
    outline-offset: 3px;
}

/* 詳細內容 */

.detail-modal-content {
    min-width: 0;
    max-height: 60vh;
    max-height: 60dvh;
    padding: 0.25rem 0 0.5rem;
    overflow-x: hidden;
    overflow-y: auto;
    overscroll-behavior: contain;
}

.detail-modal-section,
.detail-modal-images {
    min-width: 0;
}


/* 詳細視窗標題與資訊：統一文字樣式 */
.detail-modal-heading,
.detail-modal-info {
    display: flex;
    align-items: center;
    gap: 0.45rem;
    color: #475569;
    font-size: 1rem;
    font-weight: 400;
    line-height: 1.6;
}

/* 刊登敘述、特典圖片：與刊登者文字樣式一致 */
.detail-modal-heading {
    margin: 0 0 0.65rem;
    color: #334155;
    font-weight: 700;
}

.detail-modal-heading :deep(svg),
.detail-modal-info :deep(svg) {
    flex: none;
    font-size: 1.1rem;
}

.detail-modal-info {
    display: flex;
    align-items: flex-start;
    gap: 0.45rem;
    margin: 0.5rem 0 0;
    min-width: 0;
    overflow-wrap: anywhere;
}

/* 詳細視窗的刊登者標籤 */
.detail-modal-content .detail-modal-info>.detail-modal-publisher-label {
    flex-shrink: 0;
    color: #64748b !important;
    font-weight: 600;
}

/* 詳細視窗的欄位標題：不換行 */
.detail-modal-info strong {
    flex-shrink: 0;
    color: #334155;
    font-weight: 600;
    white-space: nowrap;
}

/* 詳細視窗的欄位內容 */
.detail-modal-info span {
    flex: 1 1 0;
    min-width: 0;
    color: #334155;
    overflow-wrap: anywhere;
}

.detail-modal-description {
    margin: 0 0 0 1.55rem;
    padding: 0;
    color: #3e4450;
    font-size: 1rem;
    line-height: 1.8;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
}

/* 單張圖片瀏覽區 */

.detail-modal-images {
    margin: 1rem 0 1.5rem;
    padding-top: 1rem;
    border-top: 1px solid #e5e7eb;
}

.detail-image-viewer,
.detail-modal-no-image {
    margin-left: 1.55rem;
}

.detail-image-viewer {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.75rem;
}

.detail-modal-image {
    display: block;
    width: 100%;
    max-height: 25rem;
    aspect-ratio: 2 / 3;
    border-radius: 0.75rem;
    background: #f1f5f9;
    object-fit: contain;
}

/* 上一張、下一張及圖片數量 */

.detail-image-controls {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 1.25rem;
    width: 100%;
}

.detail-image-arrow {
    display: inline-grid;
    place-items: center;
    width: 2.5rem;
    height: 2.5rem;
    padding: 0;
    border: 1px solid #cbd5e1;
    border-radius: 50%;
    background: #fff;
    color: #334155;
    font-size: 1.8rem;
    line-height: 1;
    cursor: pointer;
}

.detail-image-arrow:hover {
    background: #f1f5f9;
}

.detail-image-arrow:focus-visible {
    outline: 2px solid #43866b;
    outline-offset: 2px;
}

.detail-image-count {
    min-width: 3rem;
    color: #475569;
    font-size: 0.9rem;
    text-align: center;
    font-variant-numeric: tabular-nums;
}

/* 沒有圖片 */

.detail-modal-no-image {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    min-height: 10rem;
    border: 1px dashed #cbd5e1;
    border-radius: 0.75rem;
    background: #f8fafc;
    color: #64748b;
    text-align: center;
}

.detail-modal-no-image :deep(svg) {
    font-size: 2.5rem;
}

/* 詳細視窗底部操作 */

.detail-modal-ticket :deep(.ticket-stub) {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: flex-end;
    gap: 0.65rem;
    padding: 0.85rem 1.1rem;
}

.detail-modal-footer {
    display: flex;
    flex: 1;
    flex-wrap: wrap;
    align-items: center;
    justify-content: flex-end;
    gap: 0.65rem;
    min-width: 0;
}

.detail-modal-ticket :deep(.detail-favorite-button),
.detail-modal-ticket :deep(.detail-chat-button) {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.45rem;
    min-height: 42px;
    padding: 0.6rem 1rem;
    font-family: inherit;
    font-size: 0.88rem;
    font-weight: 750;
    line-height: 1.4;
    text-align: center;
    white-space: normal;
    overflow-wrap: anywhere;
}

.detail-modal-ticket :deep(.detail-favorite-button.is-on) {
    color: #c7376b;
}

.detail-modal-ticket :deep(.detail-chat-button:disabled) {
    cursor: not-allowed;
    opacity: 0.45;
}

/* 避免彈窗內的票券因滑鼠移入而位移 */

.detail-modal-ticket :deep(.ticket-frame),
.detail-modal-ticket :deep(.ticket-frame:hover) {
    transition: none;
}

.detail-modal-ticket :deep(.ticket),
.detail-modal-ticket :deep(.ticket:hover) {
    min-height: 0;
    transform: none;
}

/* 刊登者名稱：預設深色，滑鼠移入變綠色 */
.member-name-link {
    display: inline;
    min-width: 0;
    padding: 0;
    border: 0;
    background: transparent;
    color: #334155;
    font: inherit;
    font-weight: 750;
    text-align: left;
    text-decoration: underline;
    text-underline-offset: 3px;
    overflow-wrap: anywhere;
    cursor: pointer;
    transition: color 0.2s ease;
}

.member-name-link:hover {
    color: #43866b;
}

.member-name-link:focus-visible {
    outline: 2px solid #15803d;
    outline-offset: 3px;
    border-radius: 3px;
}

/* 公開交換刊登卡片標題：限制單行，超出部分顯示省略號 */
.listing-card-trigger :deep(.ticket-body h2) {
    display: block;
    width: 100%;
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
}

/* 手機版 */
@media (max-width: 600px) {
    :global(.detail-modal-overlay) {
        align-items: flex-end;
        padding: 0.75rem;
    }

    :global(.detail-modal) {
        width: 100%;
        max-width: 100%;
        max-height: calc(100dvh - 1.5rem);
    }

    .detail-ticket-heading {
        gap: 0.5rem;
    }

    .detail-ticket-close {
        width: 2rem;
        height: 2rem;
    }

    .detail-modal-ticket :deep(.ticket-body) {
        padding: 0.9rem 0.85rem 1rem;
    }

    .detail-modal-content {
        max-height: 58dvh;
        padding-bottom: 0.5rem;
    }

    .detail-modal-image {
        max-height: 20rem;
    }

    .detail-modal-ticket :deep(.ticket-stub) {
        align-items: stretch;
        justify-content: center;
        padding: 0.8rem;
    }

    .detail-modal-footer {
        flex-direction: column;
        align-items: stretch;
        width: 100%;
    }

    .detail-modal-ticket :deep(.detail-favorite-button),
    .detail-modal-ticket :deep(.detail-chat-button) {
        width: 100%;
        min-height: 44px;
    }
}

/* 尊重使用者減少動畫的系統設定 */
@media (prefers-reduced-motion: reduce) {

    .favorite-btn,
    .favorite-btn:hover {
        transition: none;
        transform: none;
    }

    .favorite-btn.is-on :deep(svg) {
        animation: none;
    }

    .detail-ticket-close {
        transition: none;
    }

    .detail-modal-ticket :deep(.detail-favorite-button),
    .detail-modal-ticket :deep(.detail-chat-button) {
        transition: none;
    }
}
</style>