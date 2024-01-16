<script setup lang="ts">
import { client } from '@/ws';
import type { OracleId } from '@/stores/board';
import MoveCardsModal from './MoveCardsModal.vue';
import { ref } from 'vue';
import { type ComponentExposed } from 'vue-component-type-helpers';

defineExpose({ open });

const cards = ref<{ [id: string]: OracleId }>({});

const modal = ref<ComponentExposed<typeof MoveCardsModal>>();

function open(cardsValue: { [id: string]: OracleId }) {
  cards.value = cardsValue;
  modal.value?.open();
}

function select(ids: string[], zone: number, targetIndex: number) {
  client.moveCardsVirtual(ids, zone, targetIndex);
}
</script>

<template>
  <MoveCardsModal ref="modal" multi-select title="Searching your library" :cards="cards" @select="select" />
</template>
