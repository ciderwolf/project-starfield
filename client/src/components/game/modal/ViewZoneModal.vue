<script setup lang="ts">
import MoveCardsModal from './MoveCardsModal.vue';
import { computed, ref } from 'vue';
import type { ComponentExposed } from 'vue-component-type-helpers';
import { useBoardStore, UserType, type OracleId } from '@/stores/board';
import { zoneFromIndex } from '@/zones';
import { client } from '@/ws';

const zoneId = ref(0);
const userType = ref<UserType>(UserType.PLAYER);
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

function open(zoneIdValue: number) {
  zoneId.value = zoneIdValue;
  userType.value = board.zoneUserType(zoneIdValue);
  modal.value?.open();
}

function select(ids: string[], zoneId: number, index: number) {
  client.moveCardsToZone(ids.map(Number), zoneId, index);
}

function copyFaceDown(id: string) {
  client.createCardWithAttributes(Number(id), {
    FLIPPED: 1,
  })
}

function playFaceDown(id: string) {
  client.playWithAttributes(Number(id), 0, 0, {
    FLIPPED: 1,
  })
}

</script>

<template>
  <MoveCardsModal ref="modal" :user-type="userType" :title="`Viewing ${zoneName}`" :cards="cards" :order="order"
    @select="select" @copy-face-down="copyFaceDown" @play-face-down="playFaceDown" />
</template>