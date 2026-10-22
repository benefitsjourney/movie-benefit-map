<!-- ========== components/exchange/TicketFavoriteSection.vue ========== -->
<!-- 這個元件只處理會員的票券收藏清單。 -->
<script setup>
defineProps({
    favorites: {
        type: Array,
        default: () => [],
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

const emit = defineEmits(["remove-favorite"]);

function favoriteListing(favorite) {
    return (
        favorite?.ticketListing ??
        favorite?.ticketListingBean ??
        favorite?.listing ??
        favorite
    );
}

function listingId(favorite) {
    const item = favoriteListing(favorite);

    return (
        item?.ticketListingId ??
        item?.ticket_listing_id ??
        item?.id ??
        ""
    );
}

function formatDate(value) {
    if (!value) return "未提供";

    const text = String(value);

    return text.includes("T")
        ? text.split("T")[0]
        : text;
}
</script>

<template>
    <div v-if="!favorites.length" class="empty-state">
        目前沒有票券收藏。
    </div>

    <article v-for="favorite in favorites" :key="listingId(favorite)" class="favorite-row">
        <div class="favorite-main">
            <h4>
                {{ favoriteListing(favorite)?.title || "票券刊登" }}
            </h4>

            <p>
                {{ favoriteListing(favorite)?.description || "沒有補充敘述" }}
            </p>

            <p class="small-meta">
                {{ cinemaName(favoriteListing(favorite)) }}・
                {{ movieName(favoriteListing(favorite)) }}・
                {{ formatDate(
                    favoriteListing(favorite)?.ticketShowtime ||
                    favoriteListing(favorite)?.ticket_showtime
                ) }}
            </p>
        </div>

        <button class="btn btn-sm btn-outline-danger" type="button"
            @click="emit('remove-favorite', favoriteListing(favorite))">
            取消收藏
        </button>
    </article>
</template>

<style scoped>
.favorite-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    padding: 1rem 0;
    border-bottom: 1px solid #e2e8f0;
}

.favorite-row:last-child {
    border-bottom: 0;
}

.favorite-main {
    min-width: 0;
}

.favorite-row h4 {
    margin: 0 0 .3rem;
    font-size: 1.05rem;
    font-weight: 750;
    overflow-wrap: anywhere;
}

.favorite-row p {
    margin: 0 0 .5rem;
    color: #64748b;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
}

.small-meta {
    font-size: .85rem;
}

.empty-state {
    padding: 2rem 1rem;
    text-align: center;
    border: 1px dashed #cbd5e1;
    border-radius: .9rem;
    color: #64748b;
    background: #f8fafc;
}

@media (max-width: 600px) {
    .favorite-row {
        align-items: flex-start;
        flex-direction: column;
    }
}
</style>