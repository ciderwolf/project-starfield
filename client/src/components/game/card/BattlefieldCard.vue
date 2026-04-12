<script setup lang="ts">
import { Pivot, type BoardCard as BoardCardData } from '@/api/message';
import BoardCard from './BoardCard.vue';
import { createBattlefieldContextMenu } from '@/context-menu';
import { ZONES } from '@/zones';
import { useBoardStore, type CardId } from '@/stores/board';
import { client } from '@/ws';
import { computed } from 'vue';

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
  const action = (cardId: CardId) => {
    const currentPivot = board.cards[zone].find(c => c.id === cardId)!.pivot;
    if (currentPivot === Pivot.UNTAPPED) {
      client.changeCardAttribute(cardId, { type: 'pivot', pivot: Pivot.TAPPED });
    } else {
      client.changeCardAttribute(cardId, { type: 'pivot', pivot: Pivot.UNTAPPED });
    }
  }

  applyAction(action);
}

function transform() {
  const action = (cardId: CardId) => {
    const card = board.cards[zone].find(c => c.id === cardId)!;
    client.changeCardAttribute(cardId, { type: 'transformed', transformed: !card.transformed });
  }
  applyAction(action);
}

function flip() {
  const action = (cardId: CardId) => {
    const card = board.cards[zone].find(c => c.id === cardId)!;
    client.changeCardAttribute(cardId, { type: 'flipped', flipped: !card.flipped });
  }
  applyAction(action);
}

function addCounter(counter: number) {
  const action = (cardId: CardId) => {
    client.changeCardAttribute(cardId, { type: 'counter', counter });
  }
  applyAction(action);
}
function addNote() {
  const note = prompt("Enter a note for this card:", props.card.note);
  if (note === null) {
    return;
  }

  client.changeCardAttribute(props.card.id, { type: 'note', note });
}

function moveCard(x: number, y: number) {
  const deltaX = x - props.card.x;
  const deltaY = y - props.card.y;
  const action = (cardId: CardId) => {

    const card = board.cards[zone].find(c => c.id === cardId)!;
    client.moveCard(zone, cardId, card.x + deltaX, card.y + deltaY);
    board.moveCard(zone, cardId, card.x + deltaX, card.y + deltaY);
  }
  applyAction(action);
}

function moveZone(zoneId: number, x: number, y: number) {
  const action = (cardId: CardId) => {
    client.moveCardToZone(cardId, zoneId, x, y);
  }
  applyAction(action);
}

const menuDefintion = computed(() => createBattlefieldContextMenu(props.card, doMenuAction));
function doMenuAction(name: string, ...args: any[]) {
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
    case 'add-note':
      addNote();
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

</script>

<template>
  <BoardCard :parent-bounds="zoneBounds" :card="card" :context-menu="menuDefintion" @move="moveCard"
    @move-zone="moveZone" @dblclick="tap" />
</template>