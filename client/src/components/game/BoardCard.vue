<script setup lang="ts">
import { useBoardStore, pivotToAngle } from '@/stores/board';
import { useZoneStore } from '@/stores/zone';
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { ScreenPosition, ZONES } from '@/zones';
import type { BoardCard } from '@/api/message';
import ContextMenu from '@/components/ContextMenu.vue';
import { createContextMenu, type ContextMenuDefinition } from '@/context-menu';

interface Position {
  x: number;
  y: number;
}

const props = defineProps<{ card: BoardCard, parentBounds?: DOMRect }>();

const emit = defineEmits<{
  (event: 'move', x: number, y: number): void
  (event: 'move-zone', zoneId: number, x: number, y: number): void
  (event: 'tap'): void,
  (event: 'transform'): void,
  (event: 'flip'): void,
}>()

const board = useBoardStore();
const zones = useZoneStore();
const image = ref<HTMLImageElement | null>(null);

const imageUrl = computed(() => {
  if (board.oracleInfo[props.card.id] && !props.card.flipped) {
    const id = board.oracleInfo[props.card.id]
    if (props.card.transformed) {
      return `https://api.scryfall.com/cards/${id}?format=image&version=small&face=back`;
    } else {
      return `https://api.scryfall.com/cards/${id}?format=image&version=small`;
    }
  } else {
    return '/back.png';
  }
});

const boardPos = reactive<Position>({ x: 0, y: 0 });
const imagePos = reactive<Position>({ x: 0, y: 0 });
const offsetPos = reactive<Position>({ x: 0, y: 0 });
const moving = ref(false);
let hasMoved = false;
const isMe = computed(() => board.cardIsMovable(props.card.id));

const positionInfo = computed(() => ({
  left: `${imagePos.x}px`,
  top: `${imagePos.y}px`,
  zIndex: moving.value ? 1 : 0,
  transform: `rotate(${pivotToAngle(props.card.pivot)})`,
}));

const ghostPositionInfo = computed(() => ({
  left: `${boardPos.x}px`,
  top: `${boardPos.y}px`,
  transform: `rotate(${pivotToAngle(props.card.pivot)})`,
}));

function clampToBounds(rect: DOMRect, parentRect: DOMRect) {
  imagePos.x = Math.max(parentRect.left, Math.min(imagePos.x, parentRect.left + parentRect.width - rect.width));
  imagePos.y = Math.max(parentRect.top, Math.min(imagePos.y, parentRect.top + parentRect.height - rect.height));

  boardPos.x = imagePos.x;
  boardPos.y = imagePos.y;
}

function stopMoving(): Position {
  // get the position of the element relative to the board
  const rect = image.value!.getBoundingClientRect();
  const parentRect = props.parentBounds!;

  // clamp the image position inside the parent element
  clampToBounds(rect, parentRect);

  // get the center of the element
  const centerX = (imagePos.x + rect.width / 2 - parentRect.left) / parentRect.width;
  const centerY = (imagePos.y + rect.height / 2 - parentRect.top) / parentRect.height;

  return { x: centerX, y: centerY };
}

function onMouseUp(e: MouseEvent) {
  if (moving.value) {
    moving.value = false;
    // if the card hasn't actually moved, skip
    if (!hasMoved) {
      showContextMenu(e);
      return;
    }
    hasMoved = false;

    const otherZone = zones.overlappingZone(e.clientX, e.clientY);

    if (otherZone !== null && otherZone.id !== props.card.zone) {
      // move this card to that zone
      const rect = image.value!.getBoundingClientRect();
      const newPos = zones.pointInZone(otherZone.id, imagePos.x + rect.width / 2, imagePos.y + rect.height / 2);
      emit('move-zone', otherZone.id, newPos.x, newPos.y);
    }
    else {
      const centerPos = stopMoving();
      // emit the position as a percentage of the board
      emit('move', centerPos.x, centerPos.y);
      // cards in hand should always snap back to their defined position
      if (props.card.zone === ZONES.hand.id) {
        updatePositionFromVirtualCoords();
      }
    }
  }
}

function onMouseDown(e: MouseEvent) {
  // note the position in the element that was originally clicked
  offsetPos.x = e.clientX - imagePos.x;
  offsetPos.y = e.clientY - imagePos.y;
  moving.value = true;
}


function onMouseMove(e: MouseEvent) {
  if (moving.value) {
    hasMoved = true;
    imagePos.x = e.clientX - offsetPos.x;
    imagePos.y = e.clientY - offsetPos.y;
  }
}

function updatePositionFromVirtualCoords() {
  const parentRect = props.parentBounds!;

  const screenPos = board.getScreenPositionFromCard(props.card.id);
  const visualYPos = screenPos === ScreenPosition.PRIMARY ? props.card.y : 1 - props.card.y;

  const rect = image.value!.getBoundingClientRect();
  imagePos.x = parentRect.left + parentRect.width * props.card.x - rect.width / 2;
  imagePos.y = parentRect.top + parentRect.height * visualYPos - rect.height / 2;

  clampToBounds(rect, parentRect);
  showMenu.value = false;
}

function tap() {
  if (props.card.zone !== ZONES.play.id) {
    return;
  }
  // cancel the context menu
  window.clearTimeout(showMenuTimer);
  emit('tap');
}

const showMenu = ref(false);
const menuPos = reactive<Position>({ x: 0, y: 0 });
let showMenuTimer: number = 0;
function showContextMenu(e: MouseEvent) {
  if (hasMoved || e.detail > 1 || showMenu.value) {
    return;
  }
  moving.value = false;

  menuPos.x = e.clientX + 5;
  menuPos.y = e.clientY + 5;

  menuDefinition.value = createContextMenu(props.card.zone, props.card, doMenuAction);

  showMenuTimer = window.setTimeout(() => {
    showMenu.value = true;
  }, 250);
}

onMounted(() => {
  document.addEventListener('mousemove', onMouseMove);
  if (props.parentBounds) {
    updatePositionFromVirtualCoords();
  }

  if (props.card.zone === ZONES.hand.id) {
    watch([() => board.cards[ZONES.hand.id].length], () => {
      updatePositionFromVirtualCoords();
    })
  }
})

watch([() => props.parentBounds, () => props.card.x, () => props.card.y], () => {
  if (props.parentBounds) {
    updatePositionFromVirtualCoords();
  }
})

onUnmounted(() => {
  document.removeEventListener('mousemove', onMouseMove);
})


const menu = ref();

function doMenuAction(name: string, ...args: number[]) {
  showMenu.value = false;

  switch (name) {
    case 'tap':
      emit(name);
      break;
    case 'transform':
      emit(name);
      break;
    case 'flip':
      emit(name);
      break;
    case 'move-zone':
      emit(name, args[0], 0, 0);
      break;
  }
}

const menuDefinition = ref<ContextMenuDefinition>({ options: [] });


</script>

<template>
  <img v-if="isMe" class="board-card" :style="positionInfo" draggable="false" @dblclick="tap" @mousedown="onMouseDown"
    @mouseup="onMouseUp" :src="imageUrl" ref="image">
  <img v-else class="board-card" :style="positionInfo" draggable="false" :src="imageUrl" ref="image">
  <img v-if="moving && (boardPos.x != imagePos.x || boardPos.y != imagePos.y)" class="board-card board-card-ghost"
    :style="ghostPositionInfo" draggable="false" :src="imageUrl">

  <ContextMenu v-if="showMenu" v-click-outside="() => showMenu = false" :real-pos="menuPos" :menu="menuDefinition"
    ref="menu" />
</template>

<style scoped>
.board-card {
  position: absolute;
  user-select: none;
  width: 78px;
  height: 108px;
  pointer-events: all;
  border-radius: 3px;
}

.board-card-ghost {
  pointer-events: none;
  opacity: 0.5;
}
</style>