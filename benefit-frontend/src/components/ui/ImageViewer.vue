<script setup>
/**
 * 看大圖（可左右切換）。
 * 用法：
 *   <ImageViewer v-model="open" v-model:index="index" :images="['網址1', '網址2']" title="標題" />
 * 只有一張圖時不會顯示箭頭和頁數。
 */
import { Icon } from "@iconify/vue";
import chevronLeftIcon from "@iconify-icons/mdi/chevron-left";
import chevronRightIcon from "@iconify-icons/mdi/chevron-right";
import AppModal from "@/components/ui/AppModal.vue";

const open = defineModel({ type: Boolean, default: false });
const index = defineModel("index", { type: Number, default: 0 });

const props = defineProps({
  images: { type: Array, default: () => [] },
  title: { type: String, default: "" },
});

// step 是 -1（上一張）或 1（下一張），到底會繞回另一頭
function move(step) {
  const total = props.images.length;
  if (total) index.value = (index.value + step + total) % total;
}
</script>

<template>
  <AppModal v-model="open" :title="title" width="48rem">
    <div class="viewer">
      <img v-if="images[index]" :src="images[index]" :alt="`${title} 大圖`" referrerpolicy="no-referrer" />
      <template v-if="images.length > 1">
        <button type="button" class="viewer-nav prev" aria-label="上一張" @click="move(-1)">
          <Icon :icon="chevronLeftIcon" aria-hidden="true" />
        </button>
        <button type="button" class="viewer-nav next" aria-label="下一張" @click="move(1)">
          <Icon :icon="chevronRightIcon" aria-hidden="true" />
        </button>
        <span class="viewer-count">{{ index + 1 }} / {{ images.length }}</span>
      </template>
    </div>
  </AppModal>
</template>

<style scoped>
.viewer {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 16rem;
  overflow: hidden;
  border-radius: 14px;
  background: #1e2b45;
}

/* contain：完整顯示，不裁切，直式橫式都看得到全部 */
.viewer img {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

.viewer-nav {
  position: absolute;
  top: 50%;
  translate: 0 -50%;
  display: grid;
  place-items: center;
  width: 2.75rem;
  height: 2.75rem;
  border: 0;
  border-radius: 50%;
  background: rgb(255 255 255 / 85%);
  color: #292b35;
  font-size: 1.6rem;
  cursor: pointer;
}

.viewer-nav.prev {
  left: 0.6rem;
}

.viewer-nav.next {
  right: 0.6rem;
}

.viewer-count {
  position: absolute;
  bottom: 0.6rem;
  left: 50%;
  translate: -50% 0;
  padding: 0.1rem 0.7rem;
  border-radius: 999px;
  background: rgb(0 0 0 / 55%);
  color: #fff;
  font-size: var(--text-min);
  font-weight: 800;
}
</style>
