<script setup lang="ts">
import { type ZoneConfig } from '@/zones';
import { client } from '@/ws';
import BoardCard from '@/components/game/BoardCard.vue';
import { onMounted, onUnmounted, ref } from 'vue';
import { useBoardStore, type CardId, Pivot } from '@/stores/board';
import { useZoneStore } from '@/stores/zone';

const props = defineProps<{ zone: ZoneConfig }>();

const zoneBounds = ref<HTMLElement | null>(null);
const zoneRect = ref<DOMRect | undefined>(undefined);
const board = useBoardStore();
const zones = useZoneStore();

function resetZoneRect() {
  zoneRect.value = zoneBounds.value?.getBoundingClientRect()!;
  zones.updateZoneBounds(props.zone.id, zoneRect.value!);
}

onMounted(() => {
  if (board.cards[props.zone.id] === undefined) {
    board.cards[props.zone.id] = [];
  }
  window.addEventListener('resize', resetZoneRect);
  resetZoneRect();
});

onUnmounted(() => {
  window.removeEventListener('resize', resetZoneRect);
});

function moveCardPos(cardId: CardId, x: number, y: number) {
  client.moveCard(props.zone.id, cardId, x, y);
  board.moveCard(props.zone.id, cardId, x, y);
}

function moveCardZone(cardId: CardId, zoneId: number, x: number, y: number) {
  client.moveCardToZone(props.zone.id, cardId, zoneId, x, y);
  board.moveCardZone(props.zone.id, cardId, zoneId, x, y);
}

function tap(cardId: CardId) {
  const currentPivot = board.cards[props.zone.id].find(c => c.id === cardId)!.pivot;
  if (currentPivot === Pivot.UNTAPPED) {
    client.changeCardAttribute(props.zone.id, cardId, 'PIVOT', Pivot.TAPPED);
  } else {
    client.changeCardAttribute(props.zone.id, cardId, 'PIVOT', Pivot.UNTAPPED);
  }
}

</script>

<template>
  <div class="zone-box">
    <board-card v-for="card, index in board.cards[zone.id]" :parent-bounds="zoneRect" :card="card" :key="card.id"
      @move="(x, y) => moveCardPos(card.id, x, y)" @move-zone="(z, x, y) => moveCardZone(card.id, z, x, y)"
      @tap="tap(card.id)" />
    <div class="zone-bounds" ref="zoneBounds" :style="zone.pos"></div>
  </div>
</template>

<style>
.zone-bounds {
  position: absolute;
  border: 1px solid black;
  background-color: rgba(0, 0, 0, 0.2);
  pointer-events: none;
  box-sizing: border-box;
  z-index: -1;
  pointer-events: none;
}
</style>