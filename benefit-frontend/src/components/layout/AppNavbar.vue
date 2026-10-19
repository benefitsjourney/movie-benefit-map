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
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { Icon } from "@iconify/vue";
import filmIcon from "@iconify-icons/mdi/filmstrip";
import menuIcon from "@iconify-icons/mdi/menu";
import closeIcon from "@iconify-icons/mdi/close";
import chevronIcon from "@iconify-icons/mdi/chevron-down";

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
const openDropdown = ref(null);

const isActive = (path) =>
  route.path === path || route.path.startsWith(`${path}/`);

function dropdownId(label) {
  return `nav-dropdown-${label}`;
}

function isGroupActive(link) {
  return link.children?.some((child) => isActive(child.to)) ?? false;
}

function canHoverNav() {
  return window.matchMedia("(hover: hover) and (min-width: 992px)").matches;
}

function onGroupEnter(label) {
  if (canHoverNav()) openDropdown.value = label;
}

function onGroupLeave(label) {
  if (canHoverNav() && openDropdown.value === label) openDropdown.value = null;
}

function onGroupToggle(label) {
  if (canHoverNav() && openDropdown.value === label) return;
  openDropdown.value = openDropdown.value === label ? null : label;
}

function onGroupFocusOut(event, label) {
  if (event.currentTarget.contains(event.relatedTarget)) return;
  if (openDropdown.value === label) openDropdown.value = null;
}

function closeMenu() {
  menuOpen.value = false;
  openDropdown.value = null;
}

function onDocumentPointerDown(event) {
  if (event.target instanceof Element && event.target.closest(".nav-dropdown")) return;
  openDropdown.value = null;
}

onMounted(() => {
  document.addEventListener("pointerdown", onDocumentPointerDown);
});

onBeforeUnmount(() => {
  document.removeEventListener("pointerdown", onDocumentPointerDown);
});
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
        <template v-for="link in links" :key="link.to || link.label">
          <div v-if="link.children?.length" class="nav-dropdown" :class="{ 'is-open': openDropdown === link.label }"
            @mouseenter="onGroupEnter(link.label)" @mouseleave="onGroupLeave(link.label)"
            @focusout="onGroupFocusOut($event, link.label)" @keydown.esc="openDropdown = null">
            <button type="button" class="nav-pill nav-dropdown-toggle" :class="{ active: isGroupActive(link) }"
              :aria-expanded="openDropdown === link.label" aria-haspopup="true" :aria-controls="dropdownId(link.label)"
              @click="onGroupToggle(link.label)">
              <Icon :icon="link.icon" aria-hidden="true" />
              {{ link.label }}
              <Icon :icon="chevronIcon" class="nav-caret" aria-hidden="true" />
            </button>
            <div :id="dropdownId(link.label)" class="nav-dropdown-menu" :aria-label="link.label">
              <RouterLink v-for="child in link.children" :key="child.to" :to="child.to" class="nav-dropdown-item"
                :class="{ active: isActive(child.to) }" :aria-current="isActive(child.to) ? 'page' : undefined"
                @click="closeMenu">
                <Icon :icon="child.icon" aria-hidden="true" />
                {{ child.label }}
              </RouterLink>
            </div>
          </div>
          <RouterLink v-else :to="link.to" class="nav-pill" :class="{ active: isActive(link.to) }"
            :aria-current="isActive(link.to) ? 'page' : undefined" @click="closeMenu">
            <Icon :icon="link.icon" aria-hidden="true" />
            {{ link.label }}
          </RouterLink>
        </template>
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
  font-size: var(--text-section-title);
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
  top: -0.42rem;
  right: -0.48rem;
  display: grid;
  place-items: center;
  width: 1.35rem;
  height: 1.35rem;
  border-radius: 50%;
  background: var(--nav-brand);
  color: #fff;
  font-size: var(--text-min);
  line-height: 1;
}

.brand-name {
  font-size: var(--text-feature-title);
  line-height: 1.3;
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
  font-size: var(--text-control);
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

button.nav-pill {
  border: 0;
  background: transparent;
  cursor: pointer;
  font-family: inherit;
  line-height: inherit;
}

.nav-pill .nav-caret {
  width: 1rem;
  height: 1rem;
  transition: transform var(--ease);
}

.nav-dropdown.is-open .nav-caret {
  transform: rotate(180deg);
}

.nav-dropdown {
  position: relative;
}

.nav-dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 5;
  display: none;
  flex-direction: column;
  gap: 0.15rem;
  min-width: 11.5rem;
  padding: 0.45rem;
  border-radius: 0.9rem;
  background: #fff9f1;
  box-shadow:
    0 6px 0 color-mix(in srgb, var(--nav-brand) 38%, transparent),
    0 14px 24px rgb(41 43 53 / 16%);
}

.nav-dropdown.is-open .nav-dropdown-menu {
  display: flex;
}

.nav-dropdown-item {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.55rem 0.8rem;
  border-radius: 0.65rem;
  color: var(--nav-ink);
  font-size: var(--text-control);
  font-weight: 800;
  white-space: nowrap;
  text-decoration: none;
}

.nav-dropdown-item svg {
  width: 1.2rem;
  height: 1.2rem;
}

.nav-dropdown-item:hover,
.nav-dropdown-item.active {
  background: var(--nav-accent);
  color: var(--nav-ink);
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
  font-size: var(--text-control);
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
  font-size: var(--text-section-title);
}

.brand:focus-visible,
.nav-pill:focus-visible,
.nav-dropdown-item:focus-visible,
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
    font-size: var(--text-card-title);
  }

  .brand-name {
    font-size: var(--text-card-title);
  }

  .nav-actions {
    order: 2;
    flex-shrink: 0;
  }

  .nav-switch {
    padding: 0.4rem 0.75rem;
    font-size: var(--text-control);
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

  .nav-dropdown {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
  }

  .nav-dropdown-menu {
    position: static;
    min-width: 0;
    margin: 0 0.35rem 0.25rem;
    box-shadow: none;
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
