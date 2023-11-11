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
  if (props.zone.type == 'BATTLEFIELD') {
    client.moveCard(props.zone.id, cardId, x, y);
  }
  else if (props.zone.type === 'HAND') {
    board.moveCard(props.zone.id, cardId, x, y);
  }
}

function moveCardZone(cardId: CardId, zoneId: number, x: number, y: number) {
  client.moveCardToZone(props.zone.id, cardId, zoneId, x, y);
}

function tap(cardId: CardId) {
  const currentPivot = board.cards[props.zone.id].find(c => c.id === cardId)!.pivot;
  if (currentPivot === Pivot.UNTAPPED) {
    client.changeCardAttribute(props.zone.id, cardId, 'PIVOT', Pivot.TAPPED);
  } else {
    client.changeCardAttribute(props.zone.id, cardId, 'PIVOT', Pivot.UNTAPPED);
  }
}

function transform(cardId: CardId) {
  const card = board.cards[props.zone.id].find(c => c.id === cardId)!;
  client.changeCardAttribute(props.zone.id, cardId, 'TRANSFORMED', card.transformed ? 0 : 1);
}

function flip(cardId: CardId) {
  const card = board.cards[props.zone.id].find(c => c.id === cardId)!;
  client.changeCardAttribute(props.zone.id, cardId, 'FLIPPED', card.flipped ? 0 : 1);
}

</script>

<template>
  <div class="zone-box">
    <board-card v-for="card in board.cards[zone.id]" :parent-bounds="zoneRect" :card="card" :key="card.id"
      @move="(x, y) => moveCardPos(card.id, x, y)" @move-zone="(z, x, y) => moveCardZone(card.id, z, x, y)"
      @tap="tap(card.id)" @transform="transform(card.id)" @flip="flip(card.id)" />
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