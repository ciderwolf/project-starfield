<script setup lang="ts">
import type { BoardCard as BoardCardData } from '@/api/message';
import BoardCard from './BoardCard.vue';
import ContextMenu from '@/components/ContextMenu.vue';
import { reactive, ref, watch } from 'vue';
import { createBattlefieldContextMenu, type ContextMenuDefinition } from '@/context-menu';
import { ZONES } from '@/zones';
import { Pivot, useBoardStore, type CardId } from '@/stores/board';
import { client } from '@/ws';

const props = defineProps<{ zoneBounds?: DOMRect, card: BoardCardData }>()
const zone = ZONES.play.id;
const board = useBoardStore();

function applyAction(action: (cardId: CardId) => void) {
  if (!board.selectedCards.some(card => card.id === props.card.id)) {
    action(props.card.id);
  } else {
    for (const cardId of board.selectedCards) {
      action(cardId.id);
    }
  }
}

function tap() {
  // cancel the context menu
  window.clearTimeout(showMenuTimer);

  const action = (cardId: CardId) => {
    const currentPivot = board.cards[zone].find(c => c.id === cardId)!.pivot;
    if (currentPivot === Pivot.UNTAPPED) {
      client.changeCardAttribute(cardId, 'PIVOT', Pivot.TAPPED);
    } else {
      client.changeCardAttribute(cardId, 'PIVOT', Pivot.UNTAPPED);
    }
  }

  applyAction(action);
}

function transform() {
  const action = (cardId: CardId) => {
    const card = board.cards[zone].find(c => c.id === cardId)!;
    client.changeCardAttribute(cardId, 'TRANSFORMED', card.transformed ? 0 : 1);
  }
  applyAction(action);
}

function flip() {
  const action = (cardId: CardId) => {
    const card = board.cards[zone].find(c => c.id === cardId)!;
    client.changeCardAttribute(cardId, 'FLIPPED', card.flipped ? 0 : 1);
  }
  applyAction(action);
}

function addCounter(counter: number) {
  const action = (cardId: CardId) => {
    client.changeCardAttribute(cardId, 'COUNTER', counter);
  }
  applyAction(action);
}


function moveCard(x: number, y: number) {
  const deltaX = x - props.card.x;
  const deltaY = y - props.card.y;
  const action = (cardId: CardId) => {

    const card = board.cards[zone].find(c => c.id === cardId)!;
    client.moveCard(zone, cardId, card.x + deltaX, card.y + deltaY);
  }
  applyAction(action);
}

function moveZone(zoneId: number, x: number, y: number) {
  const action = (cardId: CardId) => {
    client.moveCardToZone(cardId, zoneId, x, y);
  }
  applyAction(action);
}


const showMenu = ref(false);
const menuPos = reactive({ x: 0, y: 0 });
let showMenuTimer: number = 0;
function showContextMenu(e: MouseEvent) {
  if (e.detail > 1 || showMenu.value) {
    return;
  }

  menuPos.x = e.clientX + 5;
  menuPos.y = e.clientY + 5;

  menuDefinition.value = createBattlefieldContextMenu(props.card, doMenuAction);

  showMenuTimer = window.setTimeout(() => {
    showMenu.value = true;
  }, 250);
}
function doMenuAction(name: string, ...args: any[]) {
  showMenu.value = false;

  switch (name) {
    case 'tap':
      tap();
      break;
    case 'transform':
      transform();
      break;
    case 'flip':
      flip();
      break;
    case 'move-zone':
      if (args[1] !== undefined) {
        applyAction(cardId => client.moveCardToZoneWithIndex(cardId, args[0], args[1]))
      } else {
        moveZone(args[0], 0, 0);
      }
      break;
    case 'add-counter':
      addCounter(args[0]);
      break;
    case 'reveal-to':
      applyAction(cardId => client.revealCard(cardId, args[0]));
      break;
    case 'unreveal-to':
      applyAction(cardId => client.unrevealCard(cardId, args[0]));
      break;
    case 'copy':
      applyAction(cardId => client.cloneCard(cardId));
      break;
    case 'create-token':
      for (const tokenId of args[0]) {
        client.createToken(tokenId);
      }
      break;
    default:
      console.error('Unknown action for battlefield card', name, args);
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
  <BoardCard :parent-bounds="zoneBounds" :card="card" @move="moveCard" @move-zone="moveZone" @dblclick="tap"
    @contextmenu="showContextMenu" />
  <ContextMenu v-if="showMenu" v-click-outside="() => showMenu = false" :real-pos="menuPos" :menu="menuDefinition" />
</template>