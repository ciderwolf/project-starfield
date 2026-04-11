<script setup lang="ts">
import type { DraftCard } from '@/api/message';
import { computed, onMounted, onUnmounted, ref } from 'vue';


const props = defineProps<{
  card: DraftCard;
}>()

const emit = defineEmits<{
  pick: []
}>();

const showBackImage = ref(false);
const image = computed(() => {
  if (showBackImage.value) {
    return props.card.backImage;
  }
  return props.card.image;
});

function keyPressed(e: KeyboardEvent) {
  if (e.key === 'Shift' && props.card.backImage !== null) {
    showBackImage.value = true;
  }
}

function keyReleased(e: KeyboardEvent) {
  if (e.key === 'Shift') {
    showBackImage.value = false;
  }
}

onMounted(() => {
  window.addEventListener('keydown', keyPressed);
  window.addEventListener('keyup', keyReleased);
});

onUnmounted(() => {
  window.removeEventListener('keydown', keyPressed);
  window.removeEventListener('keyup', keyReleased);
});


</script>

<template>
  <div class="card" :style="{ backgroundImage: `url(${image})` }" @dblclick="emit('pick')">
    <div v-if="card.foil" class="foil-card"></div>
    <span class="dfc-hint" v-if="card.backImage">Hold Shift to view other face</span>
  </div>
</template>


<style scoped>
.card {
  background-size: cover;
  background-position: center;
  height: 350px;
  width: 250px;
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  margin: var(--space-md);
  cursor: default;
  position: relative;
}

.foil-card {
  height: 100%;
  width: 100%;
  border-radius: inherit;
  background: linear-gradient(45deg, #f003, #ff03, #0f03, #0ff3, #00f3, #f0f3, #f003);
  clip-path: polygon(0 0, 100% 0, 100% 100%, 0 100%);
}

.dfc-hint {
  position: absolute;
  bottom: 0px;
  background-color: var(--overlay-heavy);
  color: var(--color-white);
  padding: var(--space-sm);
  border-radius: var(--radius-md);
  font-size: var(--font-size-xs);
}
</style>