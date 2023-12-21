<script setup lang="ts">
import type { ZoneConfig } from '@/zones';
import { onMounted, onUnmounted, ref } from 'vue';
import { useBoardStore } from '@/stores/board';
import { useZoneStore } from '@/stores/zone';
import CardDispatch from '@/components/game/card/CardDispatch.vue';

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

</script>

<template>
  <div class="zone-box">
    <card-dispatch v-for="card in board.cards[zone.id]" :key="card.id" :zone="zone.type" :card="card"
      :zone-rect="zoneRect" />
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