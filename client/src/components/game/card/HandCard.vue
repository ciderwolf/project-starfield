<script setup lang="ts">
import { computed, ref } from 'vue';
import { type ComponentExposed } from 'vue-component-type-helpers';
import type { BoardCard as BoardCardData } from '@/api/message';
import BoardCard from '@/components/game/card/BoardCard.vue';
import { createHandContextMenu } from '@/context-menu';
import { useBoardStore } from '@/stores/board';
import { client } from '@/ws';


const props = defineProps<{ zoneBounds?: DOMRect, card: BoardCardData }>()

const boardCard = ref<ComponentExposed<typeof BoardCard>>();

function moveCard(x: number, y: number) {
  const board = useBoardStore();
  board.moveCard(props.card.zone, props.card.id, x, y);
  boardCard.value?.recomputePosition();
}

function moveZone(zoneId: number, x: number, y: number) {
  client.moveCardToZone(props.card.id, zoneId, x, y);
}

const menuDefintion = computed(() => createHandContextMenu(props.card, doMenuAction));
function doMenuAction(name: string, ...args: any[]) {
  switch (name) {
    case 'transform':
      props.card.transformed = !props.card.transformed;
      break;
    case 'reveal':
      client.revealCard(props.card.id);
      break;
    case 'reveal-to':
      client.revealCard(props.card.id, args[0]);
      break;
    case 'unreveal-to':
      client.unrevealCard(props.card.id, args[0]);
      break;
    case 'play':
      client.playWithAttributes(props.card.id, 0, 0, []);
      break;
    case 'play-face-down':
      client.playWithAttributes(props.card.id, 0, 0, [{ type: 'flipped', flipped: true }]);
      break;
    case 'move-zone':
      if (args[1] !== undefined) {
        client.moveCardToZoneWithIndex(props.card.id, args[0], args[1])
      } else {
        moveZone(args[0], 0, 0);
      }
      break;
    default:
      console.error('Unknown menu action for hand card', name, args);
  }
}

</script>

<template>
  <BoardCard ref="boardCard" :parent-bounds="zoneBounds" :card="card" :context-menu="menuDefintion" @move="moveCard"
    @move-zone="moveZone" />
</template>