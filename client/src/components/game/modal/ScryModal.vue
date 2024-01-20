<script setup lang="ts">
import MoveCardsModal from './MoveCardsModal.vue';
import { computed, ref } from 'vue';
import type { ComponentExposed } from 'vue-component-type-helpers';
import { useBoardStore, type OracleId } from '@/stores/board';
import { ZONES } from '@/zones';
import { client } from '@/ws';

const count = ref(0);

const board = useBoardStore();
const cards = computed(() => {
  const cardIdMap: { [id: number]: OracleId } = {};
  const library = board.cards[ZONES.library.id];
  for (let i = count.value; i > 0; i--) {
    const card = library[library.length - i];
    cardIdMap[card.id] = board.cardToOracleId[card.id];
  }
  return cardIdMap;
})

const modal = ref<ComponentExposed<typeof MoveCardsModal>>();
defineExpose({ open });

function open(countValue: number) {
  count.value = countValue;
  modal.value?.open();
}

function select(ids: string[], zoneId: number, index: number) {
  client.moveCardsToZone(ids.map(Number), zoneId, index);
}
const title = computed(() => {
  const cardCount = Object.keys(cards.value).length;
  if (cardCount === 1) {
    return `Top card of your library`;
  } else {
    return `Top ${cardCount} cards of your library`;
  }
})

</script>

<template>
  <MoveCardsModal ref="modal" multi-select :title="title" :cards="cards" @select="select" />
</template>