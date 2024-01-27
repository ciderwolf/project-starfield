<script setup lang="ts">
import type { OracleId } from '@/stores/board';
import { ref } from 'vue';

defineExpose({ showPreview, hidePreview });

const visible = ref(false);
const cardId = ref<OracleId | null>(null);
const backFace = ref(false);

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
  if (backFace.value) {
    return `https://api.scryfall.com/cards/${cardId.value}?format=image&version=large&face=back`;
  } else {
    return `https://api.scryfall.com/cards/${cardId.value}?format=image&version=large`;
  }

}

</script>

<template>
  <img v-if="visible" class="card-preview" :src="getCardUrl()">
</template>

<style scoped>
.card-preview {
  position: fixed;
  top: 25%;
  right: 0;
  height: 50%;
  z-index: 100;
  pointer-events: none;
  border-radius: 10px;
}
</style>