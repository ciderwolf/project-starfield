<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import type { BoardCard as BoardCardData } from '@/api/message';
import BoardCard from '@/components/game/card/BoardCard.vue';
import ContextMenu from '@/components/ContextMenu.vue';
import { createHandContextMenu, createLibraryContextMenu, type ContextMenuDefinition } from '@/context-menu';
import { client } from '@/ws';
import { ZONES } from '@/zones';
import { useNotificationsCache } from '@/cache/notifications';


const props = defineProps<{ zoneBounds?: DOMRect, card: BoardCardData }>()

const notifications = useNotificationsCache();

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

  if (props.card.zone === ZONES.library.id) {
    menuDefinition.value = createLibraryContextMenu(props.card, doMenuAction);
  } else {
    menuDefinition.value = createHandContextMenu(props.card, doMenuAction);
  }

  showMenu.value = true;
}
function doMenuAction(name: string, ...args: any[]) {
  showMenu.value = false;

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


watch([() => props.zoneBounds, () => props.card.x, () => props.card.y], () => {
  if (props.zoneBounds) {
    showMenu.value = false;
  }
});

const menuDefinition = ref<ContextMenuDefinition>({ options: [] });

</script>

<template>
  <BoardCard :parent-bounds="zoneBounds" :card="card" @move-zone="moveZone" @contextmenu="showContextMenu" />
  <ContextMenu v-if="showMenu" v-click-outside="() => showMenu = false" :real-pos="menuPos" :menu="menuDefinition" />
</template>