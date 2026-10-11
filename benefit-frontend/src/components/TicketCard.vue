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
const safeHref = computed(() => {
  const url = typeof props.href === "string" ? props.href.trim() : "";
  return /^https?:\/\//i.test(url) ? url : "";
});
</script>

<template>
  <article
    class="ticket"
    :class="[`tone-${tone}`, { 'ticket-appear': appear }]"
  >
    <div class="ticket-reel" aria-hidden="true"></div>
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

    <div class="ticket-stub">
      <slot name="action">
        <a
          v-if="safeHref && linkText"
          :href="safeHref"
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
</template>

<style scoped>
.ticket {
  --accent: #ffc857;
  --ticket-pale: #fff6df;
  --ticket-ink: #865500;
  --ticket-border: 1.5px;
  --dash: 2px;
  --stub-size: 4.5rem;
  --notch-r: 0.5625rem;
  --notch-y: calc(
    100% - var(--ticket-border) - var(--stub-size) + (var(--dash) / 2)
  );
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 235px;
  border: var(--ticket-border) solid rgb(41 43 53 / 9%);
  border-radius: 20px;
  background: #fff;
  box-shadow: var(--shadow-soft);
  transition:
    transform var(--ease),
    box-shadow var(--ease);
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
  -webkit-mask-size: 51% 100%, 51% 100%;
  -webkit-mask-position: left center, right center;
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
  mask-size: 51% 100%, 51% 100%;
  mask-position: left center, right center;
  mask-repeat: no-repeat;
}

.ticket:hover {
  transform: translateY(-5px);
  box-shadow: 0 16px 28px rgb(41 43 53 / 12%);
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
  --accent: #ffc857;
  --ticket-pale: #fff6df;
  --ticket-ink: #865500;
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

.ticket-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  padding: 1rem 1.1rem;
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
  font-weight: 800;
  transform: rotate(-5deg);
}

.ticket-body h2 {
  margin: 0.9rem 0 0;
  font-size: 1.28rem;
  font-weight: 900;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.ticket-detail {
  display: flex;
  gap: 0.4rem;
  margin: 0.7rem 0 0;
  color: #3e4450;
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

.ticket-link,
:slotted(.ticket-link) {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.7rem;
  border: 1.5px solid var(--ticket-ink);
  border-radius: 999px;
  background: #fff;
  color: var(--ticket-ink);
  font-weight: 700;
  text-decoration: none;
}

.ticket-link:hover,
.ticket-link:focus-visible,
:slotted(.ticket-link:hover),
:slotted(.ticket-link:focus-visible) {
  background: var(--accent);
  color: var(--c-ink);
}

.ticket-link:focus-visible,
:slotted(.ticket-link:focus-visible) {
  outline: 2px solid var(--c-brand);
  outline-offset: 2px;
}

.ticket-empty {
  color: var(--c-muted);
  font-weight: 700;
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
