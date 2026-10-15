<script>
import { nextTick, onMounted, onUnmounted, reactive, ref, watch } from "vue";
import { Icon } from "@iconify/vue";
import syncIcon from "@iconify-icons/mdi/sync";
import loadingIcon from "@iconify-icons/mdi/loading";
import checkIcon from "@iconify-icons/mdi/check-circle-outline";
import alertIcon from "@iconify-icons/mdi/alert-circle-outline";
import infoIcon from "@iconify-icons/mdi/information-outline";
import warningIcon from "@iconify-icons/mdi/alert-outline";
import TicketCard from "@/components/ui/TicketCard.vue";

const VARIANTS = {
  confirm: {
    color: "purple",
    icon: syncIcon,
    dismissible: true,
    focus: "cancel",
  },
  loading: {
    color: "blue",
    icon: loadingIcon,
    dismissible: false,
    focus: "dialog",
  },
  success: {
    color: "green",
    icon: checkIcon,
    dismissible: true,
    focus: "confirm",
  },
  error: {
    color: "red",
    icon: alertIcon,
    dismissible: true,
    focus: "confirm",
  },
  info: {
    color: "yellow",
    icon: infoIcon,
    dismissible: true,
    focus: "confirm",
  },
  warning: {
    color: "orange",
    icon: warningIcon,
    dismissible: true,
    focus: "confirm",
  },
};

const state = reactive({
  open: false,
  token: 0,
  variant: "confirm",
  title: "",
  text: "",
  confirmText: "確定",
  cancelText: "取消",
});

let resolver = null;
let previousOverflow = "";
let previousFocus = null;

function variantMeta(variant = state.variant) {
  return VARIANTS[variant] ?? VARIANTS.confirm;
}

function settle(result) {
  const resolve = resolver;
  resolver = null;
  state.open = false;
  resolve?.(result);
}

export function openAppAlert(options = {}) {
  const variant = VARIANTS[options.variant] ? options.variant : "confirm";
  const previous = resolver;
  resolver = null;
  state.variant = variant;
  state.title = options.title ?? "";
  state.text = options.text ?? "";
  state.confirmText = options.confirmText ?? "確定";
  state.cancelText =
    variant === "loading" ? "" : (options.cancelText ?? "取消");
  state.token += 1;
  state.open = true;

  return new Promise((resolve) => {
    resolver = resolve;
    previous?.({ isConfirmed: false, isDismissed: true });
  });
}

export function updateAppAlert(patch = {}) {
  if (!state.open) return;
  if (patch.title != null) state.title = patch.title;
  if (patch.text != null) state.text = patch.text;
}

export function closeAppAlert() {
  if (!state.open && !resolver) return;
  settle({ isConfirmed: false, isDismissed: true });
}

export default {
  name: "Alert",
  components: { TicketCard, Icon },
  setup() {
    const dialogRef = ref(null);
    const confirmRef = ref(null);
    const cancelRef = ref(null);

    function onConfirm() {
      settle({ isConfirmed: true, isDismissed: false });
    }

    function onDismiss() {
      if (!variantMeta().dismissible) return;
      settle({ isConfirmed: false, isDismissed: true });
    }

    function onKeydown(event) {
      if (!state.open) return;

      if (event.key === "Escape") {
        if (!variantMeta().dismissible) return;
        event.preventDefault();
        onDismiss();
        return;
      }

      if (event.key !== "Tab") return;
      const nodes = [cancelRef.value, confirmRef.value].filter(Boolean);
      if (!nodes.length) return;
      const first = nodes[0];
      const last = nodes[nodes.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    function focusPreferred() {
      const preferred =
        variantMeta().focus === "cancel"
          ? cancelRef.value
          : variantMeta().focus === "confirm"
            ? confirmRef.value
            : dialogRef.value;
      (preferred || confirmRef.value || dialogRef.value)?.focus();
    }

    watch(
      () => state.open,
      (open) => {
        if (open) {
          previousOverflow = document.body.style.overflow;
          previousFocus = document.activeElement;
          document.body.style.overflow = "hidden";
          return;
        }
        document.body.style.overflow = previousOverflow;
        if (previousFocus instanceof HTMLElement) previousFocus.focus();
        previousFocus = null;
      },
    );

    watch(
      () => state.token,
      async (token) => {
        if (!token || !state.open) return;
        await nextTick();
        focusPreferred();
      },
    );

    onMounted(() => {
      window.addEventListener("keydown", onKeydown);
    });

    onUnmounted(() => {
      window.removeEventListener("keydown", onKeydown);
      if (state.open) document.body.style.overflow = previousOverflow;
    });

    return {
      state,
      variantMeta,
      dialogRef,
      confirmRef,
      cancelRef,
      onConfirm,
      onDismiss,
    };
  },
};
</script>

<template>
  <Teleport to="body">
    <div
      v-if="state.open"
      class="alert-backdrop"
      @click.self="onDismiss"
    >
      <div
        ref="dialogRef"
        :key="state.token"
        class="alert-dialog"
        :class="{ 'is-loading': state.variant === 'loading' }"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="app-alert-title"
        aria-describedby="app-alert-text"
        :aria-busy="state.variant === 'loading' ? 'true' : 'false'"
        tabindex="-1"
      >
        <TicketCard
          appear
          :color="variantMeta().color"
          :title="state.title"
          :detail="state.text"
        >
          <template #title>
            <span id="app-alert-title">{{ state.title }}</span>
          </template>
          <template #detail>
            <span id="app-alert-text">{{ state.text }}</span>
          </template>
          <div class="alert-hero" aria-hidden="true">
            <span class="alert-badge">
              <Icon
                :icon="variantMeta().icon"
                :class="{ 'is-spin': state.variant === 'loading' }"
              />
            </span>
          </div>
          <template v-if="state.variant !== 'loading'" #action>
            <button
              v-if="state.cancelText"
              ref="cancelRef"
              type="button"
              class="ticket-link"
              @click="onDismiss"
            >
              {{ state.cancelText }}
            </button>
            <button
              ref="confirmRef"
              type="button"
              class="ticket-link is-dark"
              @click="onConfirm"
            >
              {{ state.confirmText }}
            </button>
          </template>
        </TicketCard>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.alert-backdrop {
  position: fixed;
  z-index: 1200;
  display: grid;
  align-items: safe center;
  justify-items: center;
  padding: 1.25rem;
  overflow: auto;
  background: color-mix(in srgb, var(--c-ink) 46%, transparent);
  backdrop-filter: blur(3px);
  inset: 0;
}

.alert-dialog {
  width: min(32rem, 100%);
  outline: none;
}

.alert-dialog:focus-visible {
  outline: none;
}

.alert-dialog :deep(.ticket),
.alert-dialog :deep(.ticket:hover) {
  --stub-size: 3.85rem;
  min-height: 0;
}

.alert-dialog.is-loading :deep(.ticket) {
  --stub-size: 0px;
}

.alert-hero {
  order: -1;
  align-self: flex-start;
}

.alert-badge {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border: 1px solid color-mix(in srgb, var(--ticket-ink) 18%, white);
  border-radius: 12px;
  background: var(--ticket-pale);
  color: var(--ticket-ink);
  font-size: var(--text-section-title);
}

.alert-badge :deep(svg) {
  display: block;
  width: 1em;
  height: 1em;
}

.alert-dialog :deep(.ticket-body) {
  align-items: flex-start;
  padding: 1.25rem 1.35rem 1.15rem;
  text-align: left;
}

.alert-dialog :deep(.ticket-body h2) {
  margin-top: 0.85rem;
  font-size: var(--text-feature-title);
  line-height: 1.4;
}

.alert-dialog :deep(.ticket-detail) {
  margin-top: 0.55rem;
  font-size: var(--text-body);
  line-height: var(--leading-body);
}

.alert-dialog :deep(.ticket-stub) {
  justify-content: flex-end;
  min-height: 0;
  gap: 0.6rem;
  padding: 0.85rem 1.15rem;
}

.alert-dialog.is-loading :deep(.ticket-stub) {
  display: none;
}

.alert-dialog .ticket-link {
  justify-content: center;
  min-width: 6rem;
  min-height: 2.75rem;
  padding: 0.5rem 1.05rem;
  font-size: var(--text-control);
}

.alert-dialog :deep(.ticket-film) {
  display: none;
}

.is-spin {
  animation: alert-spin 0.8s linear infinite;
}

@keyframes alert-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
