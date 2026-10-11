<script setup>
import { ref } from "vue";
import { useRoute } from "vue-router";
import { Icon } from "@iconify/vue";
import filmIcon from "@iconify-icons/mdi/filmstrip";
import movieIcon from "@iconify-icons/mdi/movie-open-outline";
import mapIcon from "@iconify-icons/mdi/map-outline";
import menuIcon from "@iconify-icons/mdi/menu";
import closeIcon from "@iconify-icons/mdi/close";

const route = useRoute();
const menuOpen = ref(false);
const links = [
  { to: "/map", label: "地圖", icon: mapIcon },
  { to: "/cinemas", label: "影城", icon: movieIcon },
];

const isActive = (path) =>
  route.path === path || route.path.startsWith(`${path}/`);
</script>

<template>
  <nav class="site-nav" aria-label="主要導覽">
    <div class="nav-deco" aria-hidden="true">
      <span class="nav-star star-left">✦</span>
      <span class="nav-star star-right">✦</span>
    </div>

    <div class="nav-inner">
      <RouterLink to="/map" class="brand" @click="menuOpen = false">
        <span class="brand-mark">
          <Icon :icon="filmIcon" aria-hidden="true" />
          <span class="brand-star" aria-hidden="true">★</span>
        </span>
        <span class="brand-name">特典地圖</span>
      </RouterLink>

      <button
        class="nav-toggle"
        type="button"
        :aria-expanded="menuOpen"
        aria-controls="main-nav-links"
        :aria-label="menuOpen ? '關閉選單' : '開啟選單'"
        @click="menuOpen = !menuOpen"
      >
        <Icon :icon="menuOpen ? closeIcon : menuIcon" aria-hidden="true" />
      </button>

      <div
        id="main-nav-links"
        class="nav-links"
        :class="{ 'is-open': menuOpen }"
      >
        <RouterLink
          v-for="link in links"
          :key="link.to"
          :to="link.to"
          class="nav-pill"
          :class="{ active: isActive(link.to) }"
          :aria-current="isActive(link.to) ? 'page' : undefined"
          @click="menuOpen = false"
        >
          <Icon :icon="link.icon" aria-hidden="true" />
          {{ link.label }}
        </RouterLink>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.site-nav {
  position: sticky;
  top: 0;
  z-index: 30;
  padding: 0.8rem 0 1rem;
  background: var(--c-navy);
  box-shadow: 0 8px 18px rgb(30 43 69 / 16%);
}

.site-nav::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 10px;
  background: var(--c-brand);
  background-image: radial-gradient(
    circle at 12px 5px,
    var(--c-navy) 4px,
    transparent 4.5px
  );
  background-size: 24px 10px;
  content: "";
}

.nav-inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-start;
  gap: 0.7rem;
  width: 100%;
  padding-right: 1.25rem;
  padding-left: 1.25rem;
}

.nav-deco {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.nav-star {
  position: absolute;
  top: 12px;
  color: var(--c-sun);
  animation: twinkle 3s ease-in-out infinite;
}

.star-left {
  left: 33%;
}
.star-right {
  right: 25%;
  animation-delay: 1s;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 0.7rem;
  color: #fff9f1;
  text-decoration: none;
}

.brand:hover {
  color: #fff9f1;
}

.brand-mark {
  position: relative;
  display: grid;
  place-items: center;
  width: 2.7rem;
  height: 2.7rem;
  border-radius: 0.9rem;
  background: var(--c-sun);
  color: var(--c-ink);
  box-shadow: 0 5px 0 rgb(255 112 73 / 45%);
  font-size: 1.5rem;
  transition: transform var(--ease);
}

.brand:hover .brand-mark {
  transform: rotate(-7deg);
}

.brand-star {
  position: absolute;
  top: -0.35rem;
  right: -0.4rem;
  display: grid;
  place-items: center;
  width: 1.15rem;
  height: 1.15rem;
  border-radius: 50%;
  background: var(--c-brand);
  color: #fff;
  font-size: 0.7rem;
}

.brand-name {
  font-size: 1.3rem;
  font-weight: 900;
}

.nav-links {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 0.4rem;
}

.nav-pill {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.5rem 1rem 0.65rem;
  color: #fff9f1;
  font-size: 1rem;
  font-weight: 700;
  text-decoration: none;
  transition: color var(--ease);
}

.nav-pill::after {
  position: absolute;
  right: 1rem;
  bottom: 0.28rem;
  left: 1rem;
  height: 3px;
  border-radius: 999px;
  background: var(--c-sun);
  content: "";
  transform: scaleX(0);
  transform-origin: center;
  transition: transform var(--ease);
}

.nav-pill svg {
  width: 1.2rem;
  height: 1.2rem;
}

.nav-pill:hover,
.nav-pill.active {
  color: var(--c-sun);
}

.nav-pill:hover::after,
.nav-pill.active::after {
  transform: scaleX(1);
}

.nav-toggle {
  display: none;
  margin-left: auto;
  padding: 0.35rem 0.65rem;
  border: 1px solid rgb(255 255 255 / 45%);
  border-radius: 10px;
  background: transparent;
  color: #fff;
  font-size: 1.5rem;
}

.brand:focus-visible,
.nav-pill:focus-visible,
.nav-toggle:focus-visible {
  outline: 2px solid var(--c-sun);
  outline-offset: 3px;
}

@media (max-width: 991.98px) {
  .nav-deco {
    display: none;
  }
  .nav-toggle {
    display: grid;
    place-items: center;
  }
  .nav-links {
    display: none;
    width: 100%;
    padding: 0.5rem 0;
  }
  .nav-links.is-open {
    display: flex;
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
</style>
