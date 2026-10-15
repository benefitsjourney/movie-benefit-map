<script>
export const THEMES = {
  yellow: {
    "--nav-bg": "#1e2b45",
    "--nav-brand": "#ff7049",
    "--nav-accent": "#ffc857",
    "--nav-ink": "#292b35",
  },
  purple: {
    "--nav-bg": "#1E2B45",
    "--nav-brand": "#FF709E",
    "--nav-accent": "#C99BFF",
    "--nav-ink": "#292B35",
  },
};
</script>

<script setup>
import { computed, ref } from "vue";
import { useRoute } from "vue-router";
import { Icon } from "@iconify/vue";
import filmIcon from "@iconify-icons/mdi/filmstrip";
import menuIcon from "@iconify-icons/mdi/menu";
import closeIcon from "@iconify-icons/mdi/close";

const props = defineProps({
  links: {
    type: Array,
    required: true,
  },
  brandTo: {
    type: String,
    default: "/map",
  },
  brandName: {
    type: String,
    default: "特典地圖",
  },
  navLabel: {
    type: String,
    default: "主要導覽",
  },
  switchTo: {
    type: String,
    required: true,
  },
  switchLabel: {
    type: String,
    required: true,
  },
  switchIcon: {
    type: Object,
    required: true,
  },
  theme: {
    type: String,
    default: "yellow",
    validator: (value) => Object.hasOwn(THEMES, value),
  },
});

const themeStyle = computed(() => THEMES[props.theme] ?? THEMES.yellow);

const route = useRoute();
const menuOpen = ref(false);

const isActive = (path) =>
  route.path === path || route.path.startsWith(`${path}/`);

function closeMenu() {
  menuOpen.value = false;
}
</script>

<template>
  <nav class="site-nav" :aria-label="navLabel" :style="themeStyle">
    <div class="nav-deco" aria-hidden="true">
      <span class="nav-star star-left">✦</span>
      <span class="nav-star star-right">✦</span>
    </div>

    <div class="nav-inner">
      <RouterLink :to="brandTo" class="brand" @click="closeMenu">
        <span class="brand-mark">
          <Icon :icon="filmIcon" aria-hidden="true" />
          <span class="brand-star" aria-hidden="true">★</span>
        </span>
        <span class="brand-name">{{ brandName }}</span>
      </RouterLink>

      <div id="main-nav-links" class="nav-links" :class="{ 'is-open': menuOpen }">
        <RouterLink v-for="link in links" :key="link.to" :to="link.to" class="nav-pill"
          :class="{ active: isActive(link.to) }" :aria-current="isActive(link.to) ? 'page' : undefined"
          @click="closeMenu">
          <Icon :icon="link.icon" aria-hidden="true" />
          {{ link.label }}
        </RouterLink>
      </div>

      <div class="nav-actions">
        <RouterLink :to="switchTo" class="nav-switch" @click="closeMenu">
          <Icon :icon="switchIcon" aria-hidden="true" />
          {{ switchLabel }}
        </RouterLink>

        <button class="nav-toggle" type="button" :aria-expanded="menuOpen" aria-controls="main-nav-links"
          :aria-label="menuOpen ? '關閉選單' : '開啟選單'" @click="menuOpen = !menuOpen">
          <Icon :icon="menuOpen ? closeIcon : menuIcon" aria-hidden="true" />
        </button>
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
  background: var(--nav-bg);
  box-shadow: 0 8px 18px color-mix(in srgb, var(--nav-bg) 16%, transparent);
}

.site-nav::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 10px;
  background-color: var(--nav-brand);
  background-image: radial-gradient(circle at 12px 5px, var(--nav-bg) 4px, transparent 4.5px);
  background-repeat: repeat;
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
  color: var(--nav-accent);
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
  background: var(--nav-chip-bg, var(--nav-accent));
  color: var(--nav-chip-ink, var(--nav-ink));
  box-shadow: 0 5px 0 color-mix(in srgb, var(--nav-brand) 45%, transparent);
  font-size: 1.5rem;
  transition:
    transform var(--ease),
    background-color var(--ease);
}

.brand:hover .brand-mark {
  background: var(--nav-chip-hover, var(--nav-chip-bg, var(--nav-accent)));
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
  background: var(--nav-brand);
  color: #fff;
  font-size: 0.7rem;
}

.brand-name {
  font-size: var(--text-card-title, 1.375rem);
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
  font-size: var(--text-control, 1rem);
  font-weight: 700;
  white-space: nowrap;
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
  background: var(--nav-accent);
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
  color: var(--nav-accent);
}

.nav-pill:hover::after,
.nav-pill.active::after {
  transform: scaleX(1);
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin-left: auto;
}

.nav-switch {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.9rem;
  border-radius: 999px;
  background: var(--nav-chip-bg, var(--nav-accent));
  color: var(--nav-chip-ink, var(--nav-ink));
  font-size: var(--text-control, 1rem);
  font-weight: 800;
  white-space: nowrap;
  text-decoration: none;
  box-shadow: 0 4px 0 color-mix(in srgb, var(--nav-brand) 42%, transparent);
  transition:
    transform var(--ease),
    color var(--ease),
    background-color var(--ease);
}

.nav-switch svg {
  width: 1.15rem;
  height: 1.15rem;
}

.nav-switch:hover {
  background: var(--nav-chip-hover, var(--nav-chip-bg, var(--nav-accent)));
  color: var(--nav-chip-ink, var(--nav-ink));
  transform: translateY(-1px);
}

.nav-toggle {
  display: none;
  padding: 0.35rem 0.65rem;
  border: 1px solid rgb(255 255 255 / 45%);
  border-radius: 10px;
  background: transparent;
  color: #fff;
  font-size: 1.5rem;
}

.brand:focus-visible,
.nav-pill:focus-visible,
.nav-switch:focus-visible,
.nav-toggle:focus-visible {
  outline: 2px solid var(--nav-accent);
  outline-offset: 3px;
}

@media (max-width: 991.98px) {
  .nav-deco {
    display: none;
  }

  .nav-inner {
    gap: 0.45rem;
    padding-right: 0.75rem;
    padding-left: 0.75rem;
  }

  .brand {
    order: 1;
    gap: 0.45rem;
    min-width: 0;
  }

  .brand-mark {
    width: 2.25rem;
    height: 2.25rem;
    font-size: 1.2rem;
  }

  .brand-name {
    font-size: 1.15rem;
  }

  .nav-actions {
    order: 2;
    flex-shrink: 0;
  }

  .nav-switch {
    padding: 0.32rem 0.7rem;
    font-size: 0.92rem;
  }

  .nav-toggle {
    display: grid;
    place-items: center;
  }

  .nav-links {
    display: none;
    order: 3;
    width: 100%;
    padding: 0.5rem 0;
  }

  .nav-links.is-open {
    display: flex;
    flex-wrap: wrap;
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
