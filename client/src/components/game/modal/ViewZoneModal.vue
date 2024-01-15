<script setup lang="ts">
import MoveCardsModal from './MoveCardsModal.vue';
import { computed, ref } from 'vue';
import type { ComponentExposed } from 'vue-component-type-helpers';
import { useBoardStore, type OracleId } from '@/stores/board';
import { zoneFromIndex } from '@/zones';
import { client } from '@/ws';

const props = defineProps<{ zoneId: number }>();

const board = useBoardStore();
const cards = computed(() => {
  const cardIdMap: { [id: number]: OracleId } = {};
  for (const card of board.cards[props.zoneId]) {
    cardIdMap[card.id] = board.cardToOracleId[card.id];
  }
  return cardIdMap;
})

const zoneName = computed(() => zoneFromIndex(props.zoneId)?.name);

const modal = ref<ComponentExposed<typeof MoveCardsModal>>();
defineExpose({ open });

function open() {
  modal.value?.open();
}

function select(ids: string[], zoneId: number, index: number) {
  client.moveCardsToZone(ids.map(Number), zoneId, index);
}

</script>

<template>
  <MoveCardsModal ref="modal" multi-select :title="`Viewing ${zoneName}`" :cards="cards" @select="select" />
</template>