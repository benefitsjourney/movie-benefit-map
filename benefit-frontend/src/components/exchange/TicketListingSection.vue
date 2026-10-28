<!-- src/components/exchange/TicketListingSection.vue -->
<!-- 負責公開票券刊登、搜尋、日期篩選、分頁、收藏、詳細視窗及聯絡刊登者。 -->

<script setup>
import { computed, ref, watch, onMounted, onBeforeUnmount } from "vue";
import { Icon } from "@iconify/vue";
import ticketIcon from "@iconify-icons/mdi/ticket";
import searchIcon from "@iconify-icons/mdi/magnify";
import compassIcon from "@iconify-icons/mdi/compass-outline";
import calendarIcon from "@iconify-icons/mdi/calendar";
import cinemaIcon from "@iconify-icons/mdi/filmstrip";
import filmIcon from "@iconify-icons/mdi/movie-open";
import accountIcon from "@iconify-icons/mdi/account";
import closeIcon from "@iconify-icons/mdi/close";
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
    cinemaName: {
        type: Function,
        required: true,
    },
    movieName: {
        type: Function,
        required: true,
    },
});

const emit = defineEmits(["toggle-favorite", "open-chat"]);

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
        console.warn("無法開啟會員頁：這筆票券刊登沒有可用的會員 ID。");
        return;
    }

    router.push({
        name: "member-public",
        params: {
            id: String(memberId),
        },
    });
}

const keyword = ref("");

// 日期輸入值：使用者目前選擇的日期，尚未必然套用。
const startDate = ref("");
const endDate = ref("");

// 已套用的日期：只有搜尋時才更新。
const searchStartDate = ref("");
const searchEndDate = ref("");

const currentPage = ref(1);
const pageSize = 8;
const selectedListing = ref(null);

// 起始日期最早只能選擇明天。
function getTomorrowDate() {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);

    const year = tomorrow.getFullYear();
    const month = String(tomorrow.getMonth() + 1).padStart(2, "0");
    const day = String(tomorrow.getDate()).padStart(2, "0");

    return `${year}-${month}-${day}`;
}

const tomorrowDate = getTomorrowDate();
const searchFilters = computed({
    get: () => ({
        keyword: keyword.value,

        // 隱藏的條件旗標，讓 SearchFilter 知道日期條件已套用。
        // 只要已套用起始日期或結束日期，就設為 true。
        dateCondition: Boolean(
            searchStartDate.value || searchEndDate.value,
        ),
    }),

    set: (next) => {
        // 只更新真正的關鍵字，不把日期條件當成關鍵字。
        keyword.value = next?.keyword ?? "";
    },
});

const filterFields = [
    {
        key: "keyword",
        label: "關鍵字",
        type: "search",
        placeholder: "輸入標題、刊登者、電影或影城名稱",
        icon: searchIcon,
    },
    {
        // 這是提供 SearchFilter 判斷清除按鈕的內部旗標。
        // 會在 CSS 隱藏，不會新增可見的輸入欄位。
        key: "dateCondition",
        label: "日期條件",
        type: "checkbox",
    },
];

function listingId(item) {
    return (
        item?.ticketListingId ??
        item?.ticket_listing_id ??
        item?.id ??
        ""
    );
}

function listingUserName(item) {
    const name =
        item?.usersName ??
        item?.userName ??
        item?.username ??
        item?.memberName;

    return name !== null && name !== undefined && String(name).trim()
        ? String(name).trim()
        : "未提供";
}

function isOwnListing(item) {
    const userId = props.currentUserId;
    const listingUserId = item?.usersId;

    if (
        userId === null ||
        userId === undefined ||
        userId === "" ||
        listingUserId === null ||
        listingUserId === undefined ||
        listingUserId === ""
    ) {
        return false;
    }

    return String(userId) === String(listingUserId);
}

function favoriteListing(favorite) {
    return (
        favorite?.ticketListing ??
        favorite?.ticketListingBean ??
        favorite?.listing ??
        favorite
    );
}

function isFavorited(id) {
    return props.favorites.some(
        (favorite) =>
            String(listingId(favoriteListing(favorite))) === String(id),
    );
}

function formatDate(value) {
    return value ? String(value).slice(0, 10) : "未提供";
}

function showDateValue(item) {
    return String(
        item?.ticketShowtime ?? item?.ticket_showtime ?? "",
    ).slice(0, 10);
}

function searchableText(item) {
    return [
        item?.title,
        listingUserName(item),
        props.cinemaName(item),
        props.movieName(item),
    ]
        .filter((value) => value !== null && value !== undefined)
        .join(" ")
        .toLocaleLowerCase();
}
// 將文字拆成一般文字與符合搜尋關鍵字的文字。
// 不使用 v-html，避免將資料直接當成 HTML 插入頁面。
function highlightSegments(value) {
    const text = String(value ?? "");
    const search = keyword.value.trim();

    // 沒有搜尋關鍵字時，整段文字都不反白。
    if (!search) {
        return [
            {
                text,
                matched: false,
            },
        ];
    }

    const lowerText = text.toLocaleLowerCase();
    const lowerSearch = search.toLocaleLowerCase();

    const segments = [];
    let currentIndex = 0;

    while (currentIndex < text.length) {
        const matchIndex = lowerText.indexOf(
            lowerSearch,
            currentIndex,
        );

        // 剩餘文字沒有符合關鍵字。
        if (matchIndex === -1) {
            segments.push({
                text: text.slice(currentIndex),
                matched: false,
            });

            break;
        }

        // 加入符合關鍵字前面的普通文字。
        if (matchIndex > currentIndex) {
            segments.push({
                text: text.slice(currentIndex, matchIndex),
                matched: false,
            });
        }

        // 加入符合搜尋關鍵字的文字。
        segments.push({
            text: text.slice(
                matchIndex,
                matchIndex + search.length,
            ),
            matched: true,
        });

        currentIndex = matchIndex + search.length;
    }

    return segments;
}
const filteredListings = computed(() => {
    const search = keyword.value.trim().toLocaleLowerCase();

    return props.listings.filter((item) => {
        // 關鍵字條件
        const matchesKeyword =
            !search || searchableText(item).includes(search);

        // 票券的放映日期
        const ticketDate = showDateValue(item);

        let matchesDate = true;

        if (searchStartDate.value && searchEndDate.value) {
            // 有起始日期與結束日期：搜尋包含起訖日的日期區間
            matchesDate =
                ticketDate >= searchStartDate.value &&
                ticketDate <= searchEndDate.value;
        } else if (searchStartDate.value) {
            // 只有起始日期：只搜尋當日
            matchesDate = ticketDate === searchStartDate.value;
        } else if (searchEndDate.value) {
            // 只有結束日期：搜尋結束日期當日及之前
            matchesDate = ticketDate <= searchEndDate.value;
        }

        // 關鍵字與日期必須同時符合
        return matchesKeyword && matchesDate;
    });
});

watch(
    [
        keyword,
        searchStartDate,
        searchEndDate,
        () => props.listings,
    ],
    () => {
        currentPage.value = 1;
    },
);

const listingCount = computed(() => filteredListings.value.length);

const totalPages = computed(() =>
    Math.max(1, Math.ceil(listingCount.value / pageSize)),
);

watch(totalPages, (total) => {
    if (currentPage.value > total) {
        currentPage.value = total;
    }
});

const paginatedListings = computed(() => {
    const start = (currentPage.value - 1) * pageSize;
    return filteredListings.value.slice(start, start + pageSize);
});

function changePage(page) {
    if (page < 1 || page > totalPages.value) return;
    currentPage.value = page;
}

// 按下「搜尋票券」時，才套用目前選擇的日期。
function applyDateSearch() {
    searchStartDate.value = startDate.value;
    searchEndDate.value = endDate.value;
}

// 使用 SearchFilter 原有的「清除條件」按鈕，清空所有搜尋條件。
function handleSearchFilterClick(event) {
    const target = event.target;

    if (!(target instanceof Element)) {
        return;
    }

    const button = target.closest("button");

    if (button?.textContent.trim() !== "清除條件") {
        return;
    }

    // 清除真正的關鍵字。
    keyword.value = "";

    // 清除日期輸入欄位。
    startDate.value = "";
    endDate.value = "";

    // 清除已套用的日期條件。
    searchStartDate.value = "";
    searchEndDate.value = "";

    // 回到第一頁。
    currentPage.value = 1;
}

function openDetail(item) {
    selectedListing.value = item;
}

function handleCardClick(event, item) {
    // 收藏、刊登者與聯絡操作不開啟詳細視窗。
    if (
        event.target.closest(
            ".favorite-btn, .ticket-publisher, .ticket-link",
        )
    ) {
        return;
    }

    openDetail(item);
}

function closeDetail() {
    selectedListing.value = null;
}

function toggleFavorite(item) {
    emit("toggle-favorite", item);
}

function openChat(item) {
    emit("open-chat", item);
}

function handleKeydown(event) {
    if (event.key === "Escape" && selectedListing.value) {
        closeDetail();
    }
}

onMounted(() => {
    window.addEventListener("keydown", handleKeydown);
});

onBeforeUnmount(() => {
    window.removeEventListener("keydown", handleKeydown);
});
</script>

<template>
    <section class="ticket-listing-section">
        <ResultBanner :icon="ticketIcon" title="票券刊登" :count="listingCount" unit-text="筆票券" :empty-text="keyword.trim() || searchStartDate || searchEndDate
            ? '找不到符合搜尋條件的票券刊登。'
            : '目前沒有公開的票券刊登。'
            " />

        <div @submit.capture="applyDateSearch" @click.capture="handleSearchFilterClick">
            <SearchFilter v-model="searchFilters" title="尋找票券" :title-icon="compassIcon" submit-label="搜尋票券"
                :fields="filterFields">

                <!-- 修改處：日期區間改為同一行排列 -->
                <div class="ticket-filter-row">
                    <div class="ticket-date-range">
                        <div class="ticket-date-field">
                            <label for="ticket-start-date" class="form-label">
                                起始日期
                            </label>

                            <input id="ticket-start-date" v-model="startDate" type="date" class="form-control"
                                :min="tomorrowDate" />

                            <small class="ticket-date-hint">
                                僅查詢明天起的放映日期，不包含今天。
                            </small>
                        </div>

                        <span class="ticket-date-separator" aria-hidden="true">～</span>

                        <div class="ticket-date-field">

                            <label for="ticket-end-date" class="form-label">
                                結束日期
                            </label>
                            <input id="ticket-end-date" v-model="endDate" type="date" class="form-control"
                                :min="tomorrowDate" />

                        </div>
                    </div>
                </div>

                <StatePanel v-if="loading && !listings.length" variant="loading" message="正在載入票券刊登……"
                    :icon="ticketIcon" />

                <template v-else>
                    <div v-if="!paginatedListings.length" class="ticket-empty-state">
                        {{
                            keyword.trim() || searchStartDate || searchEndDate
                                ? "找不到符合搜尋條件的票券刊登。"
                                : "目前沒有公開的票券刊登。"
                        }}
                    </div>

                    <div v-else class="ticket-grid" :class="{ 'is-loading': loading }" :aria-busy="loading">

                        <TicketCard v-for="(item, index) in paginatedListings" :key="listingId(item)" color="blue"
                            :appear="index < 6" class="ticket-listing-card" role="button" tabindex="0"
                            :aria-label="`查看票券刊登：${item.title || '未命名刊登'}`" @click="handleCardClick($event, item)"
                            @keydown.enter.self.prevent="openDetail(item)"
                            @keydown.space.self.prevent="openDetail(item)">
                            <!-- 1. 標題：必須放在 TicketCard 內 -->
                            <template #title>
                                <template v-for="(segment, segmentIndex) in highlightSegments(
                                    item.title || '未命名刊登',
                                )" :key="segmentIndex">
                                    <mark v-if="segment.matched" class="search-highlight">{{ segment.text }}</mark>
                                    <span v-else>{{ segment.text }}</span>
                                </template>
                            </template>

                            <template #stamp>
                                <div class="ticket-stamp-row">
                                    <span class="ticket-stamp-text">
                                        票券交換
                                    </span>

                                    <button type="button" class="favorite-btn" :class="{
                                        'is-on': isFavorited(listingId(item)),
                                    }" :aria-label="isFavorited(listingId(item))
                                        ? '取消收藏'
                                        : '加入收藏'
                                        " :aria-pressed="isFavorited(listingId(item))" :title="isLoggedIn
                                            ? isFavorited(listingId(item))
                                                ? '取消收藏'
                                                : '加入收藏'
                                            : '登入後才能收藏'
                                            " @click.stop="toggleFavorite(item)">
                                        <Icon :icon="isFavorited(listingId(item))
                                            ? heartIcon
                                            : heartOutlineIcon
                                            " aria-hidden="true" />
                                    </button>
                                </div>
                            </template>
                            <!-- 票券卡片 -->
                            <div class="ticket-detail-content">

                                <div class="ticket-publisher" @click.stop>
                                    <Icon :icon="accountIcon" aria-hidden="true" />

                                    <span class="ticket-publisher-label">
                                        刊登者
                                    </span>

                                    <button v-if="listingUserName(item) !== '未提供'" type="button"
                                        class="member-name-link" @click.stop="goToMember(item)">
                                        <template v-for="(segment, segmentIndex) in highlightSegments(
                                            listingUserName(item),
                                        )" :key="segmentIndex">
                                            <mark v-if="segment.matched" class="search-highlight">
                                                {{ segment.text }}
                                            </mark>

                                            <span v-else>
                                                {{ segment.text }}
                                            </span>
                                        </template>
                                    </button>

                                    <span v-else class="ticket-publisher-name">
                                        未提供
                                    </span>
                                </div>

                                <p class="ticket-description">
                                    {{
                                        item.description ||
                                        "刊登者尚未提供敘述。"
                                    }}
                                </p>

                                <dl class="ticket-details">
                                    <div class="ticket-detail-item">
                                        <dt>
                                            <Icon :icon="cinemaIcon" aria-hidden="true" />
                                            <span>影城</span>
                                        </dt>
                                        <dd>
                                            <template v-for="(segment, segmentIndex) in highlightSegments(
                                                cinemaName(item),
                                            )" :key="segmentIndex">
                                                <mark v-if="segment.matched" class="search-highlight">
                                                    {{ segment.text }}
                                                </mark>

                                                <span v-else>
                                                    {{ segment.text }}
                                                </span>
                                            </template>
                                        </dd>
                                    </div>

                                    <div class="ticket-detail-item">
                                        <dt>
                                            <Icon :icon="filmIcon" aria-hidden="true" />
                                            <span>電影</span>
                                        </dt>
                                        <dd>
                                            <template v-for="(segment, segmentIndex) in highlightSegments(
                                                movieName(item),
                                            )" :key="segmentIndex">
                                                <mark v-if="segment.matched" class="search-highlight">
                                                    {{ segment.text }}
                                                </mark>

                                                <span v-else>
                                                    {{ segment.text }}
                                                </span>
                                            </template>
                                        </dd>
                                    </div>

                                    <div class="ticket-detail-item">
                                        <dt>
                                            <Icon :icon="calendarIcon" aria-hidden="true" />
                                            <span>放映日期</span>
                                        </dt>
                                        <dd>
                                            {{
                                                formatDate(
                                                    item.ticketShowtime ||
                                                    item.ticket_showtime,
                                                )
                                            }}
                                        </dd>
                                    </div>
                                </dl>

                                <p class="ticket-view-hint">
                                    點擊卡片查看詳細資訊
                                </p>
                            </div>

                            <template #action>
                                <button type="button" class="ticket-link" :disabled="!isLoggedIn || isOwnListing(item)"
                                    :title="!isLoggedIn
                                        ? '登入後才能聯絡刊登者'
                                        : isOwnListing(item)
                                            ? '這是你自己的刊登，無法聯絡自己'
                                            : '聯絡刊登者'
                                        " @click.stop="openChat(item)">
                                    <Icon :icon="chatIcon" aria-hidden="true" />
                                    <span>聯絡刊登者</span>
                                </button>
                            </template>
                        </TicketCard>
                    </div>

                    <Pagination v-if="totalPages > 1" :page="currentPage" :total-pages="totalPages" label="票券刊登分頁"
                        color="purple" @update:page="changePage" />
                </template>
            </SearchFilter>
        </div>

        <!-- 票券詳細資訊視窗 -->
        <Teleport to="body">
            <Transition name="ticket-detail-modal">
                <div v-if="selectedListing" class="detail-modal-overlay" @click.self="closeDetail">
                    <section class="detail-modal" role="dialog" aria-modal="true"
                        :aria-label="selectedListing.title || '票券刊登詳細資訊'">
                        <TicketCard appear color="blue" class="detail-modal-ticket">
                            <template #title>
                                <div class="detail-ticket-heading">
                                    <Icon :icon="ticketIcon" aria-hidden="true" />

                                    <h2>
                                        {{
                                            selectedListing.title ||
                                            "未命名刊登"
                                        }}
                                    </h2>

                                    <button type="button" class="detail-ticket-close" aria-label="關閉詳細資訊" title="關閉"
                                        @click.stop="closeDetail">
                                        <Icon :icon="closeIcon" aria-hidden="true" />
                                    </button>
                                </div>
                            </template>

                            <div class="detail-modal-content">

                                <div class="detail-modal-publisher">
                                    <Icon :icon="accountIcon" aria-hidden="true" />

                                    <span class="detail-modal-label">
                                        刊登者
                                    </span>

                                    <button v-if="listingUserName(selectedListing) !== '未提供'" type="button"
                                        class="member-name-link" @click.stop="goToMember(selectedListing)">
                                        {{ listingUserName(selectedListing) }}
                                    </button>

                                    <span v-else class="detail-modal-value">
                                        未提供
                                    </span>
                                </div>

                                <section class="detail-modal-section">
                                    <h3>刊登敘述</h3>

                                    <p class="detail-modal-description">
                                        {{
                                            selectedListing.description ||
                                            "刊登者尚未提供敘述。"
                                        }}
                                    </p>
                                </section>

                                <section class="detail-modal-section">
                                    <h3>票券資訊</h3>

                                    <dl class="detail-modal-details">
                                        <div class="detail-modal-detail-item">
                                            <dt>
                                                <Icon :icon="cinemaIcon" aria-hidden="true" />
                                                <span>影城</span>
                                            </dt>

                                            <dd>
                                                {{ cinemaName(selectedListing) }}
                                            </dd>
                                        </div>

                                        <div class="detail-modal-detail-item">
                                            <dt>
                                                <Icon :icon="filmIcon" aria-hidden="true" />
                                                <span>電影</span>
                                            </dt>

                                            <dd>
                                                {{ movieName(selectedListing) }}
                                            </dd>
                                        </div>

                                        <div class="detail-modal-detail-item">
                                            <dt>
                                                <Icon :icon="calendarIcon" aria-hidden="true" />
                                                <span>放映日期</span>
                                            </dt>

                                            <dd>
                                                {{
                                                    formatDate(
                                                        selectedListing.ticketShowtime ||
                                                        selectedListing.ticket_showtime,
                                                    )
                                                }}
                                            </dd>
                                        </div>
                                    </dl>
                                </section>
                            </div>

                            <template #action>
                                <div class="detail-modal-footer">
                                    <button type="button" class="ticket-link detail-modal-action" :class="{
                                        'is-favorited': isFavorited(
                                            listingId(selectedListing),
                                        ),
                                    }" @click.stop="toggleFavorite(selectedListing)">
                                        <Icon :icon="isFavorited(listingId(selectedListing))
                                            ? heartIcon
                                            : heartOutlineIcon
                                            " aria-hidden="true" />

                                        <span>
                                            {{
                                                isFavorited(listingId(selectedListing))
                                                    ? "取消收藏"
                                                    : "加入收藏"
                                            }}
                                        </span>
                                    </button>

                                    <button type="button" class="ticket-link is-dark detail-modal-action" :disabled="!isLoggedIn ||
                                        isOwnListing(selectedListing)
                                        " :title="!isLoggedIn
                                            ? '登入後才能聯絡刊登者'
                                            : isOwnListing(selectedListing)
                                                ? '這是你自己的刊登，無法聯絡自己'
                                                : '聯絡刊登者'
                                            " @click.stop="openChat(selectedListing)">
                                        <Icon :icon="chatIcon" aria-hidden="true" />
                                        <span>聯絡刊登者</span>
                                    </button>
                                </div>
                            </template>
                        </TicketCard>
                    </section>
                </div>
            </Transition>
        </Teleport>
    </section>
</template>

<style scoped>
/* 隱藏提供 SearchFilter 判斷日期條件的內部 checkbox。
   此元件的 checkbox 只作為清除按鈕顯示判斷，不提供使用者操作。 */
.ticket-listing-section :deep(.search-filter-grid > .field.is-check) {
    display: none;
}

/* 修改處：日期區間固定同一行，兩個日期框左右排列 */
.ticket-filter-row {
    display: flex;
    align-items: flex-end;
    flex-wrap: wrap;
    gap: 0.85rem;
    margin-bottom: 1.25rem;
}

.ticket-date-range {
    display: flex;
    flex: 1 1 400px;
    align-items: flex-start;
    gap: 0.75rem;
    min-width: 0;
}

.ticket-date-field {
    display: flex;
    flex: 1 1 0;
    flex-direction: column;
    align-self: flex-start;
    min-width: 0;
}

.ticket-date-field .form-label {
    display: block;
    min-height: 1.5rem;
    margin-bottom: 0.5rem;
}

.ticket-date-field .form-control {
    width: 100%;
    min-width: 0;
    min-height: 42px;
}

.ticket-date-hint {
    display: block;
    margin-top: 0.4rem;
    color: #64748b;
    font-size: 0.78rem;
    line-height: 1.5;
}

.ticket-date-separator {
    flex-shrink: 0;
    align-self: flex-start;
    margin-top: 2.8rem;
    color: #64748b;
    font-weight: 700;
}

.ticket-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 1.125rem;
}

.ticket-listing-card {
    cursor: pointer;
}

/* 公開票券刊登卡片標題：限制一行，超出部分顯示省略號 */
.ticket-listing-card :deep(.ticket-body h2) {
    display: block;
    width: 100%;
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
}

.ticket-listing-card:focus-visible {
    outline: 3px solid #6689d8;
    outline-offset: 4px;
    border-radius: 0.75rem;
}

.ticket-detail-content {
    min-width: 0;
    padding-top: 0.75rem;
}

.ticket-grid :deep(.ticket-stamp) {
    display: block;
    width: 100%;
    padding: 0;
    border: 0;
    border-radius: 0;
    transform: none;
}

.ticket-stamp-row {
    display: flex;
    align-items: center;
    width: 100%;
    gap: 0.65rem;
}

.ticket-stamp-row .favorite-btn {
    margin-left: auto;
}

.ticket-stamp-text {
    display: inline-block;
    padding: 0.1rem 0.5rem;
    border: 2px solid var(--ticket-ink, #1e2b45);
    border-radius: 7px;
    color: var(--ticket-ink, #1e2b45);
    font-size: var(--text-control, 1.0625rem);
    font-weight: 800;
    line-height: 1.4;
    transform: rotate(-5deg);
}

.ticket-publisher {
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

.ticket-publisher :deep(svg) {
    flex-shrink: 0;
    width: 1rem;
    height: 1rem;
}

.ticket-publisher-label {
    color: #64748b;
    font-weight: 600;
}

.ticket-publisher-name {
    min-width: 0;
    color: #334155;
    font-weight: 750;
    overflow-wrap: anywhere;
}

.favorite-btn {
    display: inline-grid;
    place-items: center;
    flex-shrink: 0;
    width: 2.6rem;
    height: 2.6rem;
    padding: 0;
    border: 0;
    border-radius: 50%;
    background: rgb(255 255 255 / 88%);
    color: #a33b6b;
    font-size: 1.45rem;
    cursor: pointer;
    box-shadow: 0 4px 10px rgb(41 43 53 / 18%);
    transition: opacity var(--ease), transform var(--ease), background-color var(--ease);
}

.favorite-btn :deep(svg) {
    width: 1.45rem;
    height: 1.45rem;
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
    opacity: 1;
    outline: 2px solid #e0457b;
    outline-offset: 2px;
}

@media (hover: hover) and (pointer: fine) {
    .favorite-btn:not(.is-on) {
        opacity: 0;
    }

    .ticket-grid :deep(.ticket-frame:hover) .favorite-btn:not(.is-on),
    .favorite-btn:focus-visible {
        opacity: 1;
    }
}

@media (hover: none),
(pointer: coarse) {
    .favorite-btn:not(.is-on) {
        opacity: 1;
    }
}

@keyframes heart-pop {
    50% {
        transform: scale(1.3);
    }
}

.ticket-description {
    display: -webkit-box;
    overflow: hidden;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    white-space: pre-line;
    margin: 0 0 0.75rem;
    padding-left: 1.5rem;
    color: #475569;
    font-size: 0.92rem;
    line-height: 1.8;
    overflow-wrap: anywhere;
    /* 固定保留兩行文字的高度 */
    min-height: calc(0.92rem * 1.8 * 2);
}

.ticket-details {
    display: grid;
    gap: 0.65rem;
    margin: 1.1rem 0 0;
    padding-top: 1rem;
    border-top: 1px solid #f1f5f9;
}

.ticket-detail-item {
    display: grid;
    grid-template-columns: minmax(5rem, auto) minmax(0, 1fr);
    align-items: start;
    gap: 0.6rem;
}

.ticket-detail-item dt {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    margin: 0;
    color: #64748b;
    font-size: 0.95rem;
    font-weight: 600;
}

.ticket-detail-item dt :deep(svg) {
    flex-shrink: 0;
    width: 1rem;
    height: 1rem;
}

.ticket-detail-item dd {
    min-width: 0;
    margin: 0;
    color: #334155;
    font-size: 1rem;
    font-weight: 700;
    line-height: 1.6;
    overflow-wrap: anywhere;
    padding-left: 0.5rem;
}

.ticket-view-hint {
    margin: 0.8rem 0 0;
    color: #64748b;
    font-size: 0.78rem;
    text-align: right;
}

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

.ticket-link {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 0.45rem;
    max-width: 100%;
    cursor: pointer;
}

.ticket-link:disabled {
    opacity: 0.55;
    cursor: not-allowed;
}

.ticket-link :deep(svg) {
    flex-shrink: 0;
    width: 1.1rem;
    height: 1.1rem;
}

/* ========== 票券詳細內容視窗，比照 TicketListingFormModal.vue 的票券卡片外觀  ========== */
.detail-modal-overlay {
    position: fixed;
    z-index: 2000;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 1rem;
    overflow-y: auto;
    background: rgb(15 23 42 / 62%);
    backdrop-filter: blur(4px);
    overscroll-behavior: contain;
}

.detail-modal {
    width: min(100%, 42rem);
    max-height: min(90vh, 56rem);
    max-height: min(90dvh, 56rem);
    min-width: 0;
    min-height: 0;
    overflow-x: hidden;
    overflow-y: auto;
    overscroll-behavior: contain;
}

.detail-modal-ticket {
    width: 100%;
    min-width: 0;
}

/* 固定票券位置，避免滑鼠移入時卡片位移 */
.detail-modal-ticket :deep(.ticket-frame),
.detail-modal-ticket :deep(.ticket-frame:hover) {
    filter: drop-shadow(0 8px 12px color-mix(in srgb, var(--c-ink) 20%, transparent));
    transition: none;
}

.detail-modal-ticket :deep(.ticket),
.detail-modal-ticket :deep(.ticket:hover) {
    min-height: 0;
    transform: none;
}

/* 標題列 */
.detail-modal-ticket :deep(.ticket-body h2) {
    width: 100%;
    min-width: 0;
}

.detail-ticket-heading {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    width: 100%;
    min-width: 0;
}

.detail-ticket-heading> :deep(svg) {
    flex-shrink: 0;
    width: 1.4rem;
    height: 1.4rem;
}

.detail-ticket-heading h2 {
    flex: 1;
    min-width: 0;
    margin: 0;
    font-size: 1.15rem;
    font-weight: 800;
    line-height: 1.5;
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
    cursor: pointer;
    transition:
        background-color 0.2s ease,
        color 0.2s ease,
        opacity 0.2s ease;
}

.detail-ticket-close :deep(svg) {
    width: 1.25rem;
    height: 1.25rem;
}

.detail-ticket-close:hover {
    background: var(--accent);
    color: var(--c-ink);
}

.detail-ticket-close:focus-visible {
    outline: 2px solid var(--ticket-ink);
    outline-offset: 3px;
}

/* 內容區 */
.detail-modal-content {
    max-height: min(60vh, 60dvh);
    min-height: 0;
    overflow-y: auto;
    padding: 0.25rem 0;
    overscroll-behavior: contain;
    -webkit-overflow-scrolling: touch;
}

.detail-modal-publisher {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.5rem;
    margin-bottom: 1.1rem;
    padding: 0.85rem 0;
    border-top: 1px solid #e2e8f0;
    border-bottom: 1px solid #e2e8f0;
    color: #334155;
}

.detail-modal-publisher> :deep(svg) {
    flex-shrink: 0;
    width: 1.15rem;
    height: 1.15rem;
    color: #64748b;
}

.detail-modal-label {
    color: #64748b;
    font-size: 0.92rem;
    font-weight: 650;
}

.detail-modal-value {
    min-width: 0;
    font-weight: 800;
    overflow-wrap: anywhere;
}

.detail-modal-section+.detail-modal-section {
    margin-top: 1.35rem;
    padding-top: 1.2rem;
    border-top: 1px solid #e2e8f0;
}


.detail-modal-section h3 {
    margin: 0 0 0.75rem;
    color: #1e293b;
    font-size: 1rem;
    font-weight: 800;
}

/* 刊登敘述、票券資訊：整個內容區向右縮排約一個中文字 */
.detail-modal-section>.detail-modal-description,
.detail-modal-section>.detail-modal-details {
    padding-left: 1em;
}

.detail-modal-description {
    margin: 0;
    color: #475569;
    font-size: 0.95rem;
    line-height: 1.9;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
}


/* 影城、電影、放映日期 */
.detail-modal-details {
    display: grid;
    gap: 0.9rem;
    margin: 0;
}

.detail-modal-detail-item {
    display: grid;
    grid-template-columns: minmax(6rem, 0.35fr) minmax(0, 1fr);
    align-items: start;
    gap: 0.8rem;
}

.detail-modal-detail-item dt {
    display: inline-flex;
    align-items: center;
    gap: 0.45rem;
    margin: 0;
    color: #64748b;
    font-size: 0.9rem;
    font-weight: 650;
}

.detail-modal-detail-item dt :deep(svg) {
    flex-shrink: 0;
    width: 1.1rem;
    height: 1.1rem;
}

.detail-modal-detail-item dd {
    min-width: 0;
    margin: 0;
    color: #334155;
    font-size: 0.95rem;
    font-weight: 750;
    line-height: 1.7;
    overflow-wrap: anywhere;
}

/* 底部操作區：沿用表單 TicketCard 的 ticket-stub 排版 */
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

/* 收藏與聯絡按鈕 */
.detail-modal-ticket :deep(.detail-modal-action) {
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
    cursor: pointer;
    transition:
        background-color 0.2s ease,
        color 0.2s ease;
}

.detail-modal-ticket :deep(.detail-modal-action svg) {
    flex-shrink: 0;
    width: 1.1rem;
    height: 1.1rem;
}

/* 收藏按鈕 */
.detail-modal-ticket :deep(.detail-modal-action:not(.is-dark)) {
    border: 1px solid #ded8d2;
    background: #fff;
    color: #334155;
}

.detail-modal-ticket :deep(.detail-modal-action.is-favorited) {
    border-color: #f2b8cf;
    color: #e0457b;
}

.detail-modal-ticket :deep(.detail-modal-action:not(.is-dark):hover) {
    background: #f5f2ef;
}

/* 聯絡刊登者按鈕 */
.detail-modal-ticket :deep(.detail-modal-action.is-dark) {
    border-color: var(--ticket-ink);
    background: var(--ticket-ink);
    color: #fff;
}

.detail-modal-ticket :deep(.detail-modal-action.is-dark:hover:not(:disabled)),
.detail-modal-ticket :deep(.detail-modal-action.is-dark:focus-visible) {
    border-color: var(--ticket-ink);
    background: color-mix(in srgb, var(--ticket-ink) 78%, white);
    color: #fff;
}

/* 停用狀態與鍵盤焦點 */
.detail-modal-ticket :deep(.detail-modal-action:disabled) {
    cursor: not-allowed;
    opacity: 0.45;
}

.detail-modal-ticket :deep(.detail-modal-action:focus-visible) {
    outline: 2px solid var(--ticket-ink);
    outline-offset: 3px;
}

/* 彈窗動畫 */
.ticket-detail-modal-enter-active,
.ticket-detail-modal-leave-active {
    transition: opacity 0.2s ease;
}

.ticket-detail-modal-enter-active .detail-modal,
.ticket-detail-modal-leave-active .detail-modal {
    transition:
        opacity 0.2s ease,
        transform 0.2s ease;
}

.ticket-detail-modal-enter-from,
.ticket-detail-modal-leave-to {
    opacity: 0;
}

.ticket-detail-modal-enter-from .detail-modal,
.ticket-detail-modal-leave-to .detail-modal {
    opacity: 0;
    transform: translateY(12px) scale(0.98);
}

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
}

.member-name-link:hover {
    color: #6689d8;
}

.member-name-link:focus-visible {
    outline: 2px solid #6689d8;
    outline-offset: 3px;
    border-radius: 3px;
}

/* 搜尋結果關鍵字反白 */
.ticket-listing-section .search-highlight {
    display: inline;
    padding: 0.05em 0.12em;
    border-radius: 0.2em;
    background-color: #fff1a8;
    color: #713f12;
    font-weight: 800;
    box-decoration-break: clone;
    -webkit-box-decoration-break: clone;
}

/* 平板 */
@media (max-width: 991px) {
    .ticket-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 1rem;
    }
}

/* 手機 */
@media (max-width: 639px) {
    .ticket-grid {
        grid-template-columns: minmax(0, 1fr);
        gap: 1rem;
    }

    .detail-modal-overlay {
        align-items: flex-end;
        padding: 0.75rem;
    }

    .detail-modal {
        width: 100%;
        max-width: 100%;
        max-height: calc(100dvh - 1.5rem);
    }

    .detail-modal-content {
        max-height: 58dvh;
        padding-bottom: 0.5rem;
    }

    .detail-ticket-heading {
        gap: 0.5rem;
    }

    .detail-ticket-close {
        width: 2rem;
        height: 2rem;
    }

    .detail-modal-detail-item {
        grid-template-columns: 5.2rem minmax(0, 1fr);
        gap: 0.5rem;
    }

    .detail-modal-ticket :deep(.ticket-body) {
        padding: 0.9rem 0.85rem 1rem;
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

    .detail-modal-ticket :deep(.detail-modal-action) {
        width: 100%;
        min-height: 44px;
    }
}

/* 尊重使用者的減少動態效果設定 */
@media (prefers-reduced-motion: reduce) {

    .detail-ticket-close,
    .ticket-detail-modal-enter-active,
    .ticket-detail-modal-leave-active,
    .ticket-detail-modal-enter-active .detail-modal,
    .ticket-detail-modal-leave-active .detail-modal {
        transition: none;
    }
}
</style>