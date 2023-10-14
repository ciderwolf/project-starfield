<script setup lang="ts">
import { ZONES, type ZoneConfig } from '@/zones';
import BoardCard from '@/components/game/BoardCard.vue';
import { onMounted, onUnmounted, ref } from 'vue';
import { useBoardStore } from '@/stores/board';

const props = defineProps<{ zone: ZoneConfig }>();

const zoneBounds = ref<HTMLElement | null>(null);
const zoneRect = ref<DOMRect | undefined>(undefined);
const board = useBoardStore();

function resetZoneRect() {
  zoneRect.value = zoneBounds.value?.getBoundingClientRect()!;
  board.updateZoneBounds(props.zone.id, zoneRect.value!);
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

function moveCardPos(index: number, x: number, y: number) {
  board.moveCard(props.zone.id, index, x, y);
}

function moveCardZone(index: number, zoneId: number, x: number, y: number) {
  board.moveCardZone(props.zone.id, index, zoneId, x, y);
}

</script>

<template>
  <div class="zone-box">
    <board-card v-for="card, index in board.cards[zone.id]" :parent-bounds="zoneRect" :card="card" :key="card.key"
      @move="(x, y) => moveCardPos(index, x, y)" @move-zone="(z, x, y) => moveCardZone(index, z, x, y)"></board-card>
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