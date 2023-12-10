<script setup lang="ts">
import { useBoardStore } from '@/stores/board';
import { useZoneStore } from '@/stores/zone';
import { onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { type ComponentExposed } from 'vue-component-type-helpers';
import { ScreenPosition, ZONES } from '@/zones';
import type { BoardCard } from '@/api/message';
import CardImage from './CardImage.vue';

interface Position {
  x: number;
  y: number;
}

const props = defineProps<{ card: BoardCard, parentBounds?: DOMRect }>();
defineExpose<{ recomputePosition: () => void }>({ recomputePosition: updatePositionFromVirtualCoords });
const emit = defineEmits<{
  (event: 'move', x: number, y: number): void
  (event: 'move-zone', zoneId: number, x: number, y: number): void
  (event: 'dblclick'): void,
  (event: 'contextmenu', e: MouseEvent): void,
}>()

const board = useBoardStore();
const zones = useZoneStore();
const image = ref<ComponentExposed<typeof CardImage>>();

const boardPos = reactive<Position>({ x: 0, y: 0 });
const imagePos = reactive<Position>({ x: 0, y: 0 });
const offsetPos = reactive<Position>({ x: 0, y: 0 });
const moving = ref(false);
let hasMoved = false;

function clampToBounds(rect: DOMRect, parentRect: DOMRect) {
  imagePos.x = Math.max(parentRect.left, Math.min(imagePos.x, parentRect.left + parentRect.width - rect.width));
  imagePos.y = Math.max(parentRect.top, Math.min(imagePos.y, parentRect.top + parentRect.height - rect.height));

  boardPos.x = imagePos.x;
  boardPos.y = imagePos.y;
}

function stopMoving(): Position {
  // get the position of the element relative to the board
  const rect = image.value!.getBounds();
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
      emit('contextmenu', e);
      return;
    }
    hasMoved = false;

    const otherZone = zones.overlappingZone(e.clientX, e.clientY);

    if (otherZone !== null && otherZone.id !== props.card.zone) {
      // move this card to that zone
      const rect = image.value!.getBounds();
      const newPos = zones.pointInZone(otherZone.id, imagePos.x + rect.width / 2, imagePos.y + rect.height / 2);
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

  const rect = image.value!.getBounds();
  imagePos.x = parentRect.left + parentRect.width * props.card.x - rect.width / 2;
  imagePos.y = parentRect.top + parentRect.height * visualYPos - rect.height / 2;

  clampToBounds(rect, parentRect);
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
});

onUnmounted(() => {
  document.removeEventListener('mousemove', onMouseMove);
});

</script>

<template>
  <CardImage :card="card" :moving="moving" :board-pos="boardPos" :image-pos="imagePos" ref="image"
    @dblclick="$emit('dblclick')" @mousedown="onMouseDown" @mouseup="onMouseUp" />
</template>

<style scoped>
.dot {
  position: absolute;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: blue;
  pointer-events: none;
  z-index: 100;
}
</style>