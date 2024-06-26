<script setup lang="ts">
import MoveCardsModal from './MoveCardsModal.vue';
import { computed, ref } from 'vue';
import type { ComponentExposed } from 'vue-component-type-helpers';
import { useBoardStore, type OracleId } from '@/stores/board';
import { zoneFromIndex } from '@/zones';
import { client } from '@/ws';

const zoneId = ref(0);
const readOnly = ref(false);
const zoneName = computed(() => zoneFromIndex(zoneId.value)?.name);

const board = useBoardStore();
const cards = computed(() => {
  const cardIdMap: { [id: number]: OracleId } = {};
  if (!board.cards[zoneId.value]) {
    return cardIdMap;
  }

  for (const card of board.cards[zoneId.value]) {
    cardIdMap[card.id] = board.cardToOracleId[card.id];
  }
  return cardIdMap;
});

const order = computed(() => {
  if (!board.cards[zoneId.value]) {
    return [];
  }
  return board.cards[zoneId.value]
    .map(card => card.id.toString())
    .reverse();
});

const modal = ref<ComponentExposed<typeof MoveCardsModal>>();
defineExpose({ open });

function open(zoneIdValue: number, readOnlyValue = false) {
  zoneId.value = zoneIdValue;
  readOnly.value = readOnlyValue;
  modal.value?.open();
}

function select(ids: string[], zoneId: number, index: number) {
  client.moveCardsToZone(ids.map(Number), zoneId, index);
}

</script>

<template>
  <MoveCardsModal ref="modal" multi-select :title="`Viewing ${zoneName}`" :cards="cards" :order="order"
    :read-only="readOnly" @select="select" />
</template>