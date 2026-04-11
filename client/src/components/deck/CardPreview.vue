<script setup lang="ts">
import { computed, ref } from 'vue';
import { useCardHover } from '@/composables/useCardHover';

interface Card {
  count?: number;
  name: string;
  image: string;
  backImage: string | null;
  id: string;
}

const props = defineProps<{ card: Card, cursor?: string }>();

const { hoverStyle, mouseOver, mouseMove, mouseLeave: hoverLeave } = useCardHover();

const dfc = computed(() => props.card.backImage !== null);
const frontName = computed(() => props.card.name.split(' // ')[0]);
const backName = computed(() => props.card.name.split(' // ')[1]);
const backFace = ref(false);

function mouseLeave() {
  hoverLeave();
  backFace.value = false;
}
function mouseOverDfc(e: MouseEvent) {
  backFace.value = true;
  mouseOver(e);
}
</script>

<template>
  <div>
    <Teleport to="body">
      <img :style="hoverStyle" class="card-preview" :src="backFace ? card.backImage! : card.image" ref="cardImage" />
    </Teleport>
    <span class="card-line" :style="{ cursor }" @mouseleave="mouseLeave" @mouseover="mouseOver" @mousemove="mouseMove">
      {{ card.count }} {{ frontName }}</span>
    <span v-if="dfc" class="card-line" :style="{ cursor }" @mouseleave="mouseLeave" @mouseover="mouseOverDfc"
      @mousemove="mouseMove">
      // {{ backName }}</span>
  </div>
</template>


<style scoped>
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

.card-line {
  margin-top: 0;
  margin-bottom: 0;
  color: var(--color-text);
  text-decoration: none;
}
</style>