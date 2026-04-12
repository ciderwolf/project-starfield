<script setup lang="ts">
import type { BoardCard as BoardCardData } from '@/api/message';
import BoardCard from '@/components/game/card/BoardCard.vue';
import { createHandContextMenu, createLibraryContextMenu, type ActionEmit } from '@/context-menu';
import { client } from '@/ws';
import { ZONES } from '@/zones';
import { useNotificationsCache } from '@/cache/notifications';
import { computed } from 'vue';


const props = defineProps<{ zoneBounds?: DOMRect, card: BoardCardData }>()

const notifications = useNotificationsCache();

function moveZone(zoneId: number, x: number, y: number) {
  client.moveCardToZone(props.card.id, zoneId, x, y);
}

const menuDefinition = computed(() => {
  if (props.card.zone === ZONES.library.id) {
    return createLibraryContextMenu(props.card, doMenuAction);
  } else {
    return createHandContextMenu(props.card, doMenuAction);
  }
});
function doMenuAction(name: string, ...args: any[]) {
  switch (name) {
    case 'move-zone':
      moveZone(args[0], 0, 0);
      break;
    case 'move-zone-n':
      client.drawCards(args[1], args[0], args[2]);
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
    case 'play-face-down':
      client.playWithAttributes(props.card.id, 0, 0, [{ type: 'flipped', flipped: true }]);
      break;
    case 'scoop':
      client.scoop();
      break;
    case 'shuffle':
      client.shuffle();
      break;
    case 'find-card':
      notifications.findCards();
      break;
    case 'scry':
      notifications.scry(args[0]);
      break;
    case 'show-sideboard':
      notifications.viewZone(ZONES.sideboard.id);
      break;
    default:
      console.error('Unknown action for pile card', name, args);
  }
}
</script>

<template>
  <BoardCard :parent-bounds="zoneBounds" :card="card" :context-menu="menuDefinition" @move-zone="moveZone" />
</template>