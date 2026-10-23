<script setup>
/**
 * 追蹤電影按鈕（電影頁、行事曆視窗共用）。追蹤後上映日會出現在「我的日曆」。
 * movie 要有 movieId、movieName
 *
 * 兩種樣子：
 * 1. 一般按鈕（行事曆視窗）：
 *    <MovieFollowButton :movie="movie" @change="重新載入" />
 * 2. 海報角落鈴鐺（電影卡片）：把海報包起來，鈴鐺會疊在右上角
 *    <MovieFollowButton :movie="movie" corner>
 *      <img ... />
 *    </MovieFollowButton>
 */
import { onMounted } from "vue";
import { Icon } from "@iconify/vue";
import bellOutlineIcon from "@iconify-icons/mdi/bell-plus-outline";
import bellRingIcon from "@iconify-icons/mdi/bell-ring";
import { useBenefitReminders } from "@/composables/useBenefitReminders";

const props = defineProps({
  movie: { type: Object, required: true },
  corner: { type: Boolean, default: false },
});

const emit = defineEmits(["change"]);

const { loadReminders, isMovieFollowing, isMovieBusy, toggleMovieFollow } = useBenefitReminders();

// 每次出現都重新同步一次（很多顆同時出現時只會查一次），避免在追蹤清單頁取消後這裡還顯示已追蹤
onMounted(() => {
  loadReminders(true);
});

async function onClick() {
  const changed = await toggleMovieFollow(props.movie);
  if (changed) emit("change", isMovieFollowing(props.movie.movieId));
}
</script>

<template>
  <!-- 海報角落鈴鐺 -->
  <div v-if="corner" class="poster-follow">
    <slot />
    <button type="button" class="corner-btn" :class="{ 'is-on': isMovieFollowing(movie.movieId) }"
      :disabled="isMovieBusy(movie.movieId)" :aria-pressed="isMovieFollowing(movie.movieId)"
      :aria-label="isMovieFollowing(movie.movieId) ? `取消追蹤 ${movie.movieName}` : `追蹤 ${movie.movieName}`"
      :title="isMovieFollowing(movie.movieId) ? '已追蹤（點一下取消）' : '追蹤上映日'" @click="onClick">
      <Icon :icon="isMovieFollowing(movie.movieId) ? bellRingIcon : bellOutlineIcon" aria-hidden="true" />
    </button>
  </div>

  <!-- 一般按鈕（樣子跟特典的追蹤按鈕一樣） -->
  <button v-else type="button" class="follow-btn" :class="{ 'is-on': isMovieFollowing(movie.movieId) }"
    :disabled="isMovieBusy(movie.movieId)" :aria-pressed="isMovieFollowing(movie.movieId)" @click="onClick">
    <Icon :icon="isMovieFollowing(movie.movieId) ? bellRingIcon : bellOutlineIcon" aria-hidden="true" />
    {{ isMovieFollowing(movie.movieId) ? "已追蹤" : "追蹤" }}
  </button>
</template>

<style scoped>
/* ── 海報外框：讓鈴鐺可以疊在右上角 ── */
.poster-follow {
  position: relative;
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
}

.poster-follow :slotted(img) {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.poster-follow :slotted(.poster-fallback) {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
}

/* ── 右上角鈴鐺（大小、位置跟特典卡片的愛心一樣） ── */
.corner-btn {
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
  color: #1e2b45;
  font-size: 1.4rem;
  cursor: pointer;
  box-shadow: 0 4px 10px rgb(41 43 53 / 18%);
  transition:
    transform var(--ease),
    background-color var(--ease);
}

.corner-btn:hover:not(:disabled) {
  background: #fff;
  transform: scale(1.08);
}

/* 已追蹤：深藍底黃鈴鐺（跟一般追蹤按鈕的「已追蹤」同色） */
.corner-btn.is-on {
  background: #1e2b45;
  color: #ffc857;
}

.corner-btn.is-on:hover:not(:disabled) {
  background: #2c3d5f;
}

.corner-btn:disabled {
  cursor: wait;
  opacity: 0.7;
}

.corner-btn:focus-visible {
  outline: 2px solid #1e2b45;
  outline-offset: 2px;
}

/* 電腦（有滑鼠）：沒追蹤的鈴鐺平常隱藏，滑鼠移到海報上才出現；
   已追蹤的一直顯示。手機沒有滑鼠，鈴鐺一直顯示（跟特典的愛心一樣） */
@media (hover: hover) and (pointer: fine) {
  .corner-btn:not(.is-on) {
    opacity: 0;
    transition:
      opacity var(--ease),
      transform var(--ease),
      background-color var(--ease);
  }

  .poster-follow:hover .corner-btn,
  .corner-btn:focus-visible {
    opacity: 1;
  }
}

/* ── 一般按鈕（照 BenefitFollowButton） ── */
.follow-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  min-height: 2.75rem;
  padding: 0.4rem 1.1rem;
  border: 1.5px solid #1e2b45;
  border-radius: 999px;
  background: #fff;
  color: #1e2b45;
  font-size: var(--text-control);
  font-weight: 800;
  cursor: pointer;
  transition:
    background-color var(--ease),
    color var(--ease);
}

.follow-btn :deep(svg) {
  font-size: 1.3rem;
}

.follow-btn:hover:not(:disabled) {
  background: #eef2f9;
}

.follow-btn.is-on {
  background: #1e2b45;
  color: #ffc857;
}

.follow-btn.is-on:hover:not(:disabled) {
  background: #2c3d5f;
}

.follow-btn:disabled {
  opacity: 0.6;
  cursor: wait;
}

.follow-btn:focus-visible {
  outline: 2px solid #1e2b45;
  outline-offset: 3px;
}

/* 追蹤的那一下，鈴鐺搖一下 */
.follow-btn.is-on :deep(svg),
.corner-btn.is-on :deep(svg) {
  animation: bell-ring 0.6s ease;
  transform-origin: 50% 10%;
}

@keyframes bell-ring {
  20% {
    transform: rotate(18deg);
  }

  40% {
    transform: rotate(-14deg);
  }

  60% {
    transform: rotate(9deg);
  }

  80% {
    transform: rotate(-5deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .follow-btn.is-on :deep(svg),
  .corner-btn.is-on :deep(svg) {
    animation: none;
  }
}
</style>
