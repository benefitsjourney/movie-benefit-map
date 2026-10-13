<script setup>
import { Icon } from "@iconify/vue";

defineProps({
  kicker: { type: String, required: true },
  lead: { type: String, default: "" },
  mark: { type: String, required: true },
  tail: { type: String, default: "" },
  description: { type: String, required: true },
  iconLeft: { type: [Object, String], required: true },
  iconRight: { type: [Object, String], required: true },
  iconTop: { type: [Object, String], required: true },
});
</script>

<template>
  <header class="hero">
    <div class="hero-copy">
      <p class="hero-kicker">{{ kicker }}</p>

      <h1>
        {{ lead }}<span class="title-mark">{{ mark }}</span
        >{{ tail }}
      </h1>

      <p class="hero-description">{{ description }}</p>
    </div>

    <div class="hero-art" aria-hidden="true">
      <span class="hero-path"></span>

      <Icon :icon="iconLeft" class="art-icon art-left" />
      <Icon :icon="iconRight" class="art-icon art-right" />
      <Icon :icon="iconTop" class="art-icon art-top" />

      <span class="art-star">✦</span>
    </div>
  </header>
</template>

<style scoped>
/* 主容器 */
.hero {
  position: relative;
  display: grid;
  align-items: center;
  min-height: 180px;
  margin-bottom: 1rem;
  padding: 1.5rem;
  overflow: hidden;
  border: 1.5px solid var(--c-border);
  border-radius: 22px;
  background: var(--c-card);
  box-shadow: var(--shadow-soft);
}

.hero::after {
  position: absolute;
  top: -70px;
  right: -55px;
  width: 190px;
  height: 190px;
  border-radius: 50%;
  background: rgb(255 200 87 / 35%);
  content: "";
}

/* 文字區域 */
.hero-copy {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-self: start;
}

.hero-kicker {
  display: inline-block;
  align-self: flex-start;
  margin: 0 0 1rem;
  padding: 0.2rem 0.8rem;
  border-radius: 999px;
  background: #fff1cc;
  color: #865500;
  font-family: var(--font-latin);
  font-size: var(--text-control, 1rem);
  font-weight: 700;
  letter-spacing: 0.08em;
}

.hero h1 {
  margin: 0.25rem 0 0.25rem;
  font-size: var(--text-page-title, 2.1875rem);
  line-height: 1.3;
  font-weight: 900;
}

.hero-description {
  margin: 0;
  color: var(--c-muted);
  font-size: var(--text-body, 1.0625rem);
}

.title-mark {
  background: linear-gradient(#ffe08a, #ffe08a) left 85% / 100% 0.38em no-repeat;
}

/* 裝飾區域 */
.hero-art {
  display: none;
}

.art-icon,
.art-star {
  position: absolute;
}

.art-icon {
  color: var(--icon-color);
  font-size: var(--icon-size);
  filter: drop-shadow(0 6px 0 rgb(41 43 53 / 9%));
  animation: float 4.5s ease-in-out var(--delay, 0s) infinite;
}

/* 桌面版 */
@media (min-width: 768px) {
  .hero {
    grid-template-columns: 1.2fr 0.8fr;
    min-height: 215px;
  }

  .hero-art {
    position: relative;
    display: block;
    height: 160px;
  }

  .hero-path {
    position: absolute;
    top: 45px;
    left: 0;
    width: 100%;
    height: 90px;
    border-top: 3px dashed var(--c-brand);
    border-radius: 50%;
    transform: rotate(-16deg);
  }

  .art-left {
    top: 28px;
    left: 10%;
    --icon-color: var(--c-brand);
    --icon-size: 5rem;
    transform: rotate(-8deg);
  }

  .art-right {
    right: 8%;
    bottom: 0;
    --icon-color: var(--c-sun);
    --icon-size: 4.7rem;
    --delay: 0.6s;
  }

  .art-top {
    top: 0;
    right: 20%;
    --icon-color: var(--c-sky);
    --icon-size: 3.5rem;
    --delay: 0.3s;
  }

  .art-star {
    top: 0;
    left: 35%;
    color: var(--c-brand);
    font-size: 1.8rem;
    animation: twinkle 3s ease-in-out infinite;
  }
}

/* 小螢幕保留標題層級，同時避免破版 */
@media (max-width: 519px) {
  .hero h1 {
    font-size: 2rem;
  }
}

/* 動畫 */
@keyframes float {
  50% {
    translate: 0 -7px;
  }
}

@keyframes twinkle {
  0%,
  100% {
    opacity: 0.35;
  }
  50% {
    opacity: 1;
  }
}

/* 無障礙：減少動畫 */
@media (prefers-reduced-motion: reduce) {
  .art-icon,
  .art-star {
    animation: none;
  }
}
</style>
