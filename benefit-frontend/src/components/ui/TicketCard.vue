<script>
export const TICKET_COLORS = [
  "red",
  "orange",
  "yellow",
  "green",
  "blue",
  "purple",
  "pink",
];
</script>

<script setup>
import { computed, useSlots } from "vue";
import { Icon } from "@iconify/vue";
import externalIcon from "@iconify-icons/mdi/open-in-new";
import filmIcon from "@iconify-icons/mdi/filmstrip";

const props = defineProps({
  color: {
    type: String,
    default: "yellow",
  },
  appear: {
    type: Boolean,
    default: false,
  },
  stamp: {
    type: String,
    default: "",
  },
  title: {
    type: String,
    default: "",
  },
  detail: {
    type: String,
    default: "",
  },
  href: {
    type: String,
    default: "",
  },
  linkText: {
    type: String,
    default: "",
  },
  emptyText: {
    type: String,
    default: "",
  },
});

const slots = useSlots();

const tone = computed(() =>
  TICKET_COLORS.includes(props.color) ? props.color : "yellow",
);
const hasStamp = computed(() => Boolean(props.stamp) || Boolean(slots.stamp));
const hasTitle = computed(() => Boolean(props.title) || Boolean(slots.title));
const hasDetail = computed(
  () => Boolean(props.detail) || Boolean(slots.detail),
);
const hasPoster = computed(() => Boolean(slots.poster));
const linkHref = computed(() => props.href.trim());
const hasAction = computed(
  () =>
    Boolean(slots.action) ||
    Boolean(linkHref.value && props.linkText) ||
    Boolean(props.emptyText),
);
</script>

<template>
  <div class="ticket-frame">
    <article
      class="ticket"
      :class="[
        `tone-${tone}`,
        {
          'ticket-appear': appear,
          'ticket-poster-card': hasPoster,
        },
      ]"
    >
      <div class="ticket-reel" aria-hidden="true"></div>
      <div v-if="hasPoster" class="ticket-poster">
        <slot name="poster" />
      </div>
      <div class="ticket-body">
        <div v-if="hasStamp" class="ticket-head">
          <span class="ticket-stamp">
            <slot name="stamp">{{ stamp }}</slot>
          </span>
        </div>

        <h2 v-if="hasTitle">
          <slot name="title">{{ title }}</slot>
        </h2>
        <p v-if="hasDetail" class="ticket-detail">
          <slot name="detail">{{ detail }}</slot>
        </p>
        <slot />
      </div>

      <div class="ticket-stub" :class="{ 'ticket-stub-mark': !hasAction }">
        <slot name="action">
          <a
            v-if="linkHref && linkText"
            :href="linkHref"
            target="_blank"
            rel="noopener noreferrer"
            class="ticket-link"
          >
            {{ linkText }}
            <Icon :icon="externalIcon" aria-hidden="true" />
          </a>
          <span v-else-if="emptyText" class="ticket-empty">{{ emptyText }}</span>
        </slot>
        <span class="ticket-film">
          <slot name="mark">
            <Icon :icon="filmIcon" aria-hidden="true" />
          </slot>
        </span>
      </div>
    </article>
  </div>
</template>

<!-- 票券列表共用的基礎網格；各頁面自行指定響應式欄數。 -->
<style>
.ticket-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: stretch;
  gap: 1.1rem;
  scroll-margin-top: 5.5rem;
  transition: opacity var(--ease);
}

.ticket-grid.is-loading {
  opacity: 0.48;
  pointer-events: none;
}
</style>

<style scoped>
.ticket-frame {
  display: flex;
  min-width: 0;
  filter: drop-shadow(
    0 6px 8px color-mix(in srgb, var(--c-ink) 18%, transparent)
  );
  transition: filter var(--ease);
}

.ticket-frame:hover {
  filter: drop-shadow(
    0 14px 16px color-mix(in srgb, var(--c-ink) 24%, transparent)
  );
}

.ticket {
  --accent: #ffe08a;
  --ticket-pale: #fffbeb;
  --ticket-ink: #8a6400;
  --ticket-border: 1.5px;
  --dash: 2px;
  --stub-size: 4.5rem;
  --notch-r: 0.5625rem;
  --notch-y: calc(
    100% - var(--ticket-border) - var(--stub-size) + (var(--dash) / 2)
  );
  display: flex;
  flex: 1;
  flex-direction: column;
  width: 100%;
  min-width: 0;
  min-height: 235px;
  border: var(--ticket-border) solid
    color-mix(in srgb, var(--ticket-ink) 34%, white);
  border-radius: 20px;
  background: #fff;
  transition: transform var(--ease);
  -webkit-mask-image:
    radial-gradient(
      circle at 0 var(--notch-y),
      transparent var(--notch-r),
      #000 calc(var(--notch-r) + 0.5px)
    ),
    radial-gradient(
      circle at 100% var(--notch-y),
      transparent var(--notch-r),
      #000 calc(var(--notch-r) + 0.5px)
    );
  -webkit-mask-size:
    51% 100%,
    51% 100%;
  -webkit-mask-position:
    left center,
    right center;
  -webkit-mask-repeat: no-repeat;
  mask-image:
    radial-gradient(
      circle at 0 var(--notch-y),
      transparent var(--notch-r),
      #000 calc(var(--notch-r) + 0.5px)
    ),
    radial-gradient(
      circle at 100% var(--notch-y),
      transparent var(--notch-r),
      #000 calc(var(--notch-r) + 0.5px)
    );
  mask-size:
    51% 100%,
    51% 100%;
  mask-position:
    left center,
    right center;
  mask-repeat: no-repeat;
}

.ticket:hover {
  transform: translateY(-5px);
}

.ticket-appear {
  animation: rise 0.45s ease backwards;
}

.tone-red {
  --accent: #f1999e;
  --ticket-pale: #fff0f1;
  --ticket-ink: #a33b42;
}

.tone-orange {
  --accent: #ffb07a;
  --ticket-pale: #fff4ea;
  --ticket-ink: #b85a12;
}

.tone-yellow {
  --accent: #ffe08a;
  --ticket-pale: #fffbeb;
  --ticket-ink: #8a6400;
}

.tone-green {
  --accent: #8fd9c4;
  --ticket-pale: #e9f8f4;
  --ticket-ink: #0f6e5c;
}

.tone-blue {
  --accent: #a9c4f0;
  --ticket-pale: #eef4fd;
  --ticket-ink: #2157a8;
}

.tone-purple {
  --accent: #c5b3f0;
  --ticket-pale: #f5f1fd;
  --ticket-ink: #5c3d9e;
}

.tone-pink {
  --accent: #f5b4d4;
  --ticket-pale: #fff0f6;
  --ticket-ink: #a33b6b;
}

.ticket-reel {
  height: 14px;
  flex-shrink: 0;
  border-radius: 18px 18px 0 0;
  background: var(--accent);
  background-image: radial-gradient(
    circle at 10px 7px,
    white 3px,
    transparent 3.5px
  );
  background-size: 20px 14px;
}

.ticket-poster {
  display: grid;
  place-items: center;
  margin: 0.65rem 0.65rem 0;
  overflow: hidden;
  border-radius: 12px;
  aspect-ratio: 2 / 3;
  background: var(--ticket-pale);
}

.ticket-poster :slotted(img) {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  transition: transform 0.35s ease;
}

.ticket:hover .ticket-poster :slotted(img) {
  transform: scale(1.045);
}

.ticket-poster :slotted(.poster-fallback) {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  color: var(--ticket-ink);
  background:
    radial-gradient(circle at 50% 42%, rgb(255 255 255 / 72%), transparent 58%),
    var(--ticket-pale);
}

.ticket-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  padding: 1rem 1.1rem;
  font-size: var(--text-body, 1.0625rem);
}

.ticket-film {
  margin-left: auto;
  color: var(--ticket-ink);
  font-size: 1.4rem;
  line-height: 1;
}

.ticket-film svg,
.ticket-film :slotted(svg) {
  display: block;
}

.ticket-head {
  display: flex;
  align-items: center;
}

.ticket-stamp {
  padding: 0.1rem 0.5rem;
  border: 2px solid var(--ticket-ink);
  border-radius: 7px;
  color: var(--ticket-ink);
  font-size: var(--text-control, 1rem);
  font-weight: 800;
  transform: rotate(-5deg);
}

.ticket-body h2 {
  margin: 0;
  font-size: var(--text-card-title, 1.375rem);
  font-weight: 900;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.ticket-head + h2 {
  margin-top: 0.9rem;
}

.ticket-detail {
  display: flex;
  gap: 0.4rem;
  margin: 0.7rem 0 0;
  color: #3e4450;
  font-size: var(--text-body, 1.0625rem);
  overflow-wrap: anywhere;
}

.ticket-detail :slotted(svg) {
  flex-shrink: 0;
  margin-top: 0.2rem;
  color: var(--ticket-ink);
  font-size: 1.2rem;
}

.ticket-stub {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  flex-shrink: 0;
  min-height: var(--stub-size);
  margin-top: auto;
  padding: 0.75rem 1.1rem;
  border-top: 2px dashed var(--accent);
  border-radius: 0 0 18px 18px;
  background: var(--ticket-pale);
}

.ticket-stub-mark {
  justify-content: center;
}

.ticket-stub-mark .ticket-film {
  margin-left: 0;
}

.ticket-link,
:slotted(.ticket-link),
:slotted(.ticket-chip) {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.7rem;
  border: 1.5px solid var(--ticket-ink);
  border-radius: 999px;
  background: #fff;
  color: var(--ticket-ink);
  font: inherit;
  font-size: var(--text-control, 1rem);
  font-weight: 700;
  text-decoration: none;
  cursor: pointer;
}

:slotted(.ticket-chip) {
  cursor: default;
}

.ticket-link:hover:not(:disabled),
.ticket-link:focus-visible,
:slotted(.ticket-link:hover:not(:disabled)),
:slotted(.ticket-link:focus-visible) {
  background: var(--accent);
  color: var(--c-ink);
}

.ticket-link:disabled,
:slotted(.ticket-link:disabled) {
  cursor: not-allowed;
  opacity: 0.45;
}

.ticket-link:focus-visible,
:slotted(.ticket-link:focus-visible) {
  outline: 2px solid var(--ticket-ink);
  outline-offset: 2px;
}

.ticket-empty,
:slotted(.ticket-empty) {
  color: var(--c-muted);
  font-size: var(--text-control, 1rem);
  font-weight: 700;
}

.ticket-poster-card {
  --stub-size: 2.625rem;
  min-height: 0;
  border-radius: 16px;
}

.ticket-poster-card .ticket-body {
  padding: 0.7rem 0.75rem 0.35rem;
}

.ticket-poster-card .ticket-body h2 {
  font-size: 1rem;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
}

.ticket-poster-card .ticket-detail {
  gap: 0.3rem;
  margin-top: 0.3rem;
  font-size: 0.875rem;
  line-height: 1.35;
}

.ticket-poster-card .ticket-detail :slotted(svg) {
  margin-top: 0.05rem;
  font-size: 1rem;
}

.ticket-poster-card .ticket-stub {
  gap: 0.45rem;
  padding: 0.35rem 0.75rem;
}

.ticket-poster-card .ticket-film {
  font-size: 1.05rem;
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
