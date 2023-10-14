<script setup lang="ts">
import { useBoardStore, type BoardCard } from '@/stores/board';
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { ZONES } from '@/zones';

interface Position {
  x: number;
  y: number;
}

const props = defineProps<{ card: BoardCard, parentBounds?: DOMRect }>();

const emit = defineEmits<{
  (event: 'move', x: number, y: number): void
  (event: 'move-zone', zoneId: number, x: number, y: number): void
}>()

const board = useBoardStore();
const image = ref<HTMLImageElement | null>(null);

const boardPos = reactive<Position>({ x: 0, y: 0 });
const imagePos = reactive<Position>({ x: 0, y: 0 });
const offsetPos = reactive<Position>({ x: 0, y: 0 });
const moving = ref(false);
const tapped = ref(false);

const positionInfo = computed(() => ({
  left: `${imagePos.x}px`,
  top: `${imagePos.y}px`,
  zIndex: moving.value ? 1 : 0,
  transform: `rotate(${tapped.value ? '90deg' : '0deg'})`,
}));

const ghostPositionInfo = computed(() => ({
  left: `${boardPos.x}px`,
  top: `${boardPos.y}px`,
}));

function clampToBounds(rect: DOMRect, parentRect: DOMRect) {
  if (props.card.zone === ZONES.hand.id) {
    const handCards = board.cards[ZONES.hand.id].length;
    const index = board.cards[ZONES.hand.id].findIndex(c => c.key === props.card.key);
    const totalHandWidth = rect.width * handCards;
    const cardXPos = parentRect.left + rect.width * index + (parentRect.width - totalHandWidth) / 2;
    imagePos.x = cardXPos;
    imagePos.y = parentRect.top;
  }
  else {
    imagePos.x = Math.max(parentRect.left, Math.min(imagePos.x, parentRect.left + parentRect.width - rect.width));
    imagePos.y = Math.max(parentRect.top, Math.min(imagePos.y, parentRect.top + parentRect.height - rect.height));
  }

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

    const otherZone = board.overlappingZone(e.clientX, e.clientY);

    if (otherZone !== null && otherZone.id !== props.card.zone) {
      // move this card to that zone
      const newPos = board.pointInZone(otherZone.id, e.clientX, e.clientY);
      emit('move-zone', otherZone.id, newPos.x, newPos.y);
    }
    else {
      const centerPos = stopMoving();
      // emit the position as a percentage of the board
      emit('move', centerPos.x, centerPos.y);
    }
  }
}

function onMouseDown(e: MouseEvent) {
  // note the position in the element that was originally clicked
  const rect = image.value!.getBoundingClientRect();
  offsetPos.x = e.clientX - rect.left;
  offsetPos.y = e.clientY - rect.top;

  moving.value = true;
}


function onMouseMove(e: MouseEvent) {
  if (moving.value) {
    imagePos.x = e.clientX - offsetPos.x;
    imagePos.y = e.clientY - offsetPos.y;
  }
}

function updatePositionFromScaleCoords() {
  const rect = image.value!.getBoundingClientRect();
  const parentRect = props.parentBounds!;
  imagePos.x = parentRect.left + parentRect.width * props.card.x - rect.width / 2;
  imagePos.y = parentRect.top + parentRect.height * props.card.y - rect.height / 2;

  clampToBounds(rect, parentRect);
}

function tap() {
  tapped.value = !tapped.value;
}

onMounted(() => {
  document.addEventListener('mousemove', onMouseMove);
  if (props.parentBounds) {
    updatePositionFromScaleCoords();
  }

  if (props.card.zone === ZONES.hand.id) {
    watch([() => board.cards[ZONES.hand.id].length], () => {
      updatePositionFromScaleCoords();
    })
  }
})

watch([() => props.parentBounds], () => {
  if (props.parentBounds) {
    updatePositionFromScaleCoords();
  }
})

onUnmounted(() => {
  document.removeEventListener('mousemove', onMouseMove);
})
</script>

<template>
  <img class="board-card" :style="positionInfo" draggable="false" @dblclick="tap" @mousedown="onMouseDown"
    @mouseup="onMouseUp" :src="`https://api.scryfall.com/cards/${card.image}?format=image&version=small`" ref="image">
  <img v-if="moving && (boardPos.x != imagePos.x || boardPos.y != imagePos.y)" class="board-card board-card-ghost"
    :style="ghostPositionInfo" draggable="false"
    :src="`https://api.scryfall.com/cards/${card.image}?format=image&version=small`">
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