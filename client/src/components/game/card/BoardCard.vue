<script setup lang="ts">
import { useZoneStore } from '@/stores/zone';
import { onMounted, onUnmounted, reactive, ref } from 'vue';
import { type ComponentExposed } from 'vue-component-type-helpers';
import { type BoardCard } from '@/api/message';
import CardImage from './CardImage.vue';
import { useNotificationsCache } from '@/cache/notifications';
import { nonRotatedRect, useBoardStore } from '@/stores/board';

interface Position {
  x: number;
  y: number;
}

const props = defineProps<{ card: BoardCard, parentBounds?: DOMRect }>();
const image = ref<ComponentExposed<typeof CardImage>>();

defineExpose<{ recomputePosition: () => void }>({ recomputePosition: () => image.value!.recomputePosition() });
const emit = defineEmits<{
  (event: 'move', x: number, y: number): void
  (event: 'move-zone', zoneId: number, x: number, y: number): void
  (event: 'dblclick'): void,
  (event: 'contextmenu', e: MouseEvent): void,
}>()

const zones = useZoneStore();
const notifications = useNotificationsCache();
const board = useBoardStore();

const imagePos = reactive<Position>({ x: 0, y: 0 });
const offsetPos = reactive<Position>({ x: 0, y: 0 });
const moving = ref(false);
let hasMoved = false;

function stopMoving(): Position {
  // get the position of the element relative to the board
  const rect = image.value!.getBounds();
  const parentRect = props.parentBounds!;

  const cardRect = nonRotatedRect(rect, props.card.pivot);
  const centerX = (imagePos.x + cardRect.width / 2 - parentRect.left) / parentRect.width;
  const centerY = (imagePos.y + cardRect.height / 2 - parentRect.top) / parentRect.height;

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

    if (otherZone !== null && otherZone.id !== props.card.zone && otherZone.id >= 0) {
      // move this card to that zone
      const rect = image.value!.getBounds();
      const newPos = zones.pointInZone(otherZone.id, imagePos.x + rect.width / 2, imagePos.y + rect.height / 2);

      // if the card is being moved to hand, track the relative position on client
      if (otherZone.type == 'HAND') {
        board.moveCard(props.card.zone, props.card.id, newPos.x, newPos.y);
      }

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
  if (moving.value) {
    return;
  }

  // note the position in the element that was originally clicked
  const img = image.value!.cardPosition();
  imagePos.x = img.x;
  imagePos.y = img.y;

  offsetPos.x = e.clientX - imagePos.x;
  offsetPos.y = e.clientY - imagePos.y;
  moving.value = true;
}


function onMouseMove(e: MouseEvent) {
  if (moving.value) {
    notifications.hideCardPreview();
    hasMoved = true;
    imagePos.x = e.clientX - offsetPos.x;
    imagePos.y = e.clientY - offsetPos.y;
  }
}

onMounted(() => {
  document.addEventListener('mousemove', onMouseMove);
});

onUnmounted(() => {
  document.removeEventListener('mousemove', onMouseMove);
});

</script>

<template>
  <card-image :card="card" :moving="moving" :image-pos="imagePos" :zone-rect="parentBounds" ref="image"
    @dblclick="$emit('dblclick')" @mousedown="onMouseDown" @mouseup="onMouseUp" />
</template>
