<script setup lang="ts">
import { computed, ref } from 'vue';
import type { OracleCard } from '@/api/message';
import { useCardHover } from '@/composables/useCardHover';

const props = defineProps<{ card?: OracleCard, class?: string }>();

const imageUrl = computed(() => {
  if (!props.card?.id) return '/back.png';
  return props.card.image;
});

const previewUrl = computed(() => {
  if (!props.card?.id) return '/back.png';
  return props.card.image;
});

const emit = defineEmits<{
  (event: 'click', e: MouseEvent): void
  (event: 'mouseover', e: MouseEvent): void
  (event: 'mouseleave', e: MouseEvent): void
}>()

const cardImage = ref<HTMLImageElement>();
const { hoverStyle, mouseOver: hoverOver, mouseMove, mouseLeave: hoverLeave } = useCardHover(cardImage);

function mouseLeave(e: MouseEvent) {
  hoverLeave();
  emit('mouseleave', e);
}

function mouseOver(e: MouseEvent) {
  hoverOver(e);
  emit('mouseover', e);
}

</script>

<template>
  <Teleport to="body">
    <img ref="cardImage" class="card-preview" :style="hoverStyle" :src="previewUrl" />
  </Teleport>

  <img :src="imageUrl" class="card-thumbnail" @mouseleave="mouseLeave" @mouseover="mouseOver" @mousemove="mouseMove"
    :class="$props.class" @click="$emit('click', $event)" />
</template>

<style scoped>
.card-thumbnail {
  width: 100px;
  height: 140px;
  object-fit: contain;
  border-radius: var(--radius-md);
}

.card-preview {
  display: none;
  background-size: 224px auto;
  height: 310px;
  position: absolute;
  z-index: var(--z-card-preview);
  border: 3px solid var(--color-white);
  border-radius: var(--radius-md);
  pointer-events: none;
}
</style>