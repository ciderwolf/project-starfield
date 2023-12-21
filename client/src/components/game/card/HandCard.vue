<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import type { BoardCard as BoardCardData } from '@/api/message';
import BoardCard from '@/components/game/card/BoardCard.vue';
import ContextMenu from '@/components/ContextMenu.vue';
import { createHandContextMenu, type ContextMenuDefinition } from '@/context-menu';
import { useBoardStore } from '@/stores/board';
import { type ComponentExposed } from 'vue-component-type-helpers';
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


const showMenu = ref(false);
const menuPos = reactive({ x: 0, y: 0 });
function showContextMenu(e: MouseEvent) {
  if (e.detail > 1 || showMenu.value) {
    return;
  }

  menuPos.x = e.clientX + 5;
  menuPos.y = e.clientY + 5;

  menuDefinition.value = createHandContextMenu(props.card, doMenuAction);

  showMenu.value = true;
}
function doMenuAction(name: string, ...args: any[]) {
  showMenu.value = false;

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
    case 'play':
      client.playWithAttributes(props.card.id, 0, 0, {});
      break;
    case 'play-face-down':
      client.playWithAttributes(props.card.id, 0, 0, { FLIPPED: 1 });
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


watch([() => props.zoneBounds, () => props.card.x, () => props.card.y], () => {
  if (props.zoneBounds) {
    showMenu.value = false;
  }
});

const menuDefinition = ref<ContextMenuDefinition>({ options: [] });

</script>

<template>
  <BoardCard ref="boardCard" :parent-bounds="zoneBounds" :card="card" @move="moveCard" @move-zone="moveZone"
    @contextmenu="showContextMenu" />
  <ContextMenu v-if="showMenu" v-click-outside="() => showMenu = false" :real-pos="menuPos" :menu="menuDefinition" />
</template>