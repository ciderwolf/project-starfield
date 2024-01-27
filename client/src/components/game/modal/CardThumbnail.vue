<script setup lang="ts">
import type { OracleCard } from '@/api/message';
import { computed, ref } from 'vue';


const props = defineProps<{ card?: OracleCard, class?: string }>();

const imageUrl = computed(() => {
  if (!props.card?.id) return '/back.png';
  return `https://api.scryfall.com/cards/${props.card.id}?format=image&version=small`;
});

const previewUrl = computed(() => {
  if (!props.card?.id) return '/back.png';
  return `https://api.scryfall.com/cards/${props.card.id}?format=image&version=normal`;
});

defineEmits<{
  (event: 'click', e: MouseEvent): void
}>()

const left = ref(0);
const top = ref(0);
const display = ref("none");
const cardImage = ref<HTMLInputElement>();

const hoverStyle = computed(() => ({
  display: display.value,
  left: `${left.value}px`,
  top: `${top.value}px`
}))

function normalizePreviewY(e: MouseEvent) {
  const height = (cardImage.value?.height ?? 0) + 20;
  let newY = e.pageY;
  if (newY + height > window.innerHeight + window.scrollY) {
    newY = window.innerHeight + window.scrollY - height;
  }
  return newY;
}

function normalizePreviewX(e: MouseEvent) {
  const width = (cardImage.value?.width ?? 0) + 20;
  let newX = e.pageX;
  if (newX + width > window.innerWidth + window.scrollX) {
    newX = window.innerWidth + window.scrollX - width;
  }
  return newX;
}
function mouseLeave() {
  display.value = 'none';
}
function mouseOver(e: MouseEvent) {
  display.value = 'inline';
  // get the x coordiate of the mouse
  left.value = normalizePreviewX(e);
  top.value = normalizePreviewY(e);
}
function mouseMove(e: MouseEvent) {
  left.value = normalizePreviewX(e);
  top.value = normalizePreviewY(e);
}

</script>

<template>
  <Teleport to="body">
    <img ref="cardImage" class="card-preview" :style="hoverStyle" :src="previewUrl" />
  </Teleport>

  <img :src="imageUrl" class="card-thumbnail" @mouseleave="mouseLeave" @mouseover="mouseOver" @mousemove="mouseMove"
    :class="class" @click="$emit('click', $event)" />
</template>

<style scoped>
.card-thumbnail {
  width: 100px;
  height: 140px;
  object-fit: contain;
  border-radius: 5px;
}

.card-preview {
  display: none;
  background-size: 224px auto;
  height: 310px;
  position: absolute;
  z-index: 200000;
  border: 3px solid white;
  border-radius: 5px;
  pointer-events: none;
}
</style>