<script setup lang="ts">
import { useBoardStore, type OracleId } from '@/stores/board';
import { computed, onMounted, onUnmounted, ref } from 'vue';

defineExpose({ showPreview, hidePreview });

const visible = ref(false);
const cardId = ref<OracleId | null>(null);
const backFace = ref(false);

const board = useBoardStore();
const rotated = ref(false);
const showOppositeFace = ref(false);
const hasOtherFace = computed(() => {
  if (cardId.value == null) {
    return false;
  }
  return board.oracleInfo[cardId.value]?.backImage;
})

function showPreview(id: OracleId, backFaceValue: boolean) {
  visible.value = true;
  cardId.value = id;
  backFace.value = backFaceValue;
}

function hidePreview() {
  visible.value = false;
}

function getCardUrl() {
  if (cardId.value === null) {
    return '';
  }
  const card = board.oracleInfo[cardId.value];

  if (backFace.value != showOppositeFace.value && card.backImage) {
    return card.backImage;
  } else {
    return card.image;
  }

}

function keyPressed(e: KeyboardEvent) {
  if (e.key === 'Alt') {
    rotated.value = true;
  } else if (e.key === 'Shift') {
    showOppositeFace.value = true;
  }
}

function keyReleased(e: KeyboardEvent) {
  if (e.key === 'Alt') {
    rotated.value = false;
  } else if (e.key === 'Shift') {
    showOppositeFace.value = false;
  }
}

onMounted(() => {
  window.addEventListener('keydown', keyPressed);
  window.addEventListener('keyup', keyReleased);
})

onUnmounted(() => {
  window.removeEventListener('keydown', keyPressed);
  window.removeEventListener('keyup', keyReleased);
})

</script>

<template>
  <div class="card-preview" v-if="visible">
    <img class="card-preview-image" :class="{ rotated }" :src="getCardUrl()">
    <span class="preview-help-text" v-if="hasOtherFace">Hold shift to view other face</span>
    <span class="preview-help-text">Hold Alt to rotate</span>
  </div>
</template>

<style scoped>
.card-preview {
  position: fixed;
  top: 25%;
  right: 0;
  z-index: 100;
  pointer-events: none;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.card-preview-image {
  height: 50%;
  border-radius: 10px;
}

.card-preview-image.rotated {
  transform: rotate(90deg) translate(50%, 50%);
  transform-origin: 100% 50%;
}

.preview-help-text {
  margin: none;
  padding: 0 10px;
  color: white;
  background: #0008;

}
</style>