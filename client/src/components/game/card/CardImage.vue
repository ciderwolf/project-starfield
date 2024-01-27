<script setup lang="ts">
import type { BoardCard } from '@/api/message';
import { pivotToAngle, useBoardStore } from '@/stores/board';
import { ScreenPosition } from '@/zones';
import { computed, onMounted, reactive, ref, watch } from 'vue';

interface Position {
  x: number;
  y: number;
}

const props = defineProps<{
  card: BoardCard,
  imagePos: Position,
  zoneRect?: DOMRect,
  moving: boolean
}>();

const emits = defineEmits<{
  (event: 'dblclick'): void
  (event: 'mousedown', e: MouseEvent): void
  (event: 'mouseup', e: MouseEvent): void
}>();

const image = ref<HTMLImageElement>();
const boardPos = reactive<Position>({ x: 0, y: 0 });

defineExpose<{ getBounds: () => DOMRect, recomputePosition: () => void, cardPosition: () => Position }>({
  getBounds: () => image.value!.getBoundingClientRect(),
  recomputePosition: () => updateScreenPosFromVirtualCoords(),
  cardPosition: () => boardPos,
});

const board = useBoardStore();

const imageUrl = computed(() => {
  if (board.cardToOracleId[props.card.id] && !props.card.flipped) {
    const id = board.cardToOracleId[props.card.id]
    if (props.card.transformed) {
      return `https://api.scryfall.com/cards/${id}?format=image&version=small&face=back`;
    } else {
      return `https://api.scryfall.com/cards/${id}?format=image&version=small`;
    }
  } else {
    return '/back.png';
  }
});

const showGhost = computed(() => props.moving && (props.imagePos.x != boardPos.x || props.imagePos.y != boardPos.y))

const positionInfo = computed(() => {
  if (showGhost.value) {
    return {
      left: `${props.imagePos.x}px`,
      top: `${props.imagePos.y}px`,
      zIndex: props.moving ? 3 : 1,
      transform: `rotate(${pivotToAngle(props.card.pivot)})`,
      backgroundImage: `url(${imageUrl.value})`
    };
  } else {
    return {
      left: `${boardPos.x}px`,
      top: `${boardPos.y}px`,
      transform: `rotate(${pivotToAngle(props.card.pivot)})`,
      backgroundImage: `url(${imageUrl.value})`
    };
  }
});

const ghostPositionInfo = computed(() => ({
  left: `${boardPos.x}px`,
  top: `${boardPos.y}px`,
  transform: `rotate(${pivotToAngle(props.card.pivot)})`,
}));


function updateScreenPosFromVirtualCoords() {
  if (props.zoneRect === undefined || image === undefined) {
    return { x: 0, y: 0 };
  }
  const parentRect = props.zoneRect!;

  const screenPos = board.getScreenPositionFromCard(props.card.id);
  const visualYPos = screenPos === ScreenPosition.PRIMARY ? props.card.y : 1 - props.card.y;

  const rect = image.value!.getBoundingClientRect();
  const x = parentRect.left + parentRect.width * props.card.x - rect.width / 2;
  const y = parentRect.top + parentRect.height * visualYPos - rect.height / 2;

  const clamped = clampToBounds(x, y, rect, parentRect);
  boardPos.x = clamped.x;
  boardPos.y = clamped.y;
}

function clampToBounds(x: number, y: number, rect: DOMRect, parentRect: DOMRect) {
  x = Math.max(parentRect.left, Math.min(x, parentRect.left + parentRect.width - rect.width));
  y = Math.max(parentRect.top, Math.min(y, parentRect.top + parentRect.height - rect.height));
  return { x, y };
}

onMounted(() => {
  if (props.zoneRect) {
    updateScreenPosFromVirtualCoords();
  }
});

watch([() => props.zoneRect, () => props.card.x, () => props.card.y], () => {
  if (props.zoneRect) {
    updateScreenPosFromVirtualCoords();
  }
});

</script>

<template>
  <figure class="board-card" :style="positionInfo" draggable="false" @dblclick="$emit('dblclick')"
    @mousedown="$emit('mousedown', $event)" @mouseup="$emit('mouseup', $event)" ref="image" :data-counter="card.counter">
    <span v-if="card.counter > 0" class="board-card-counter">{{ card.counter }}</span>
  </figure>
  <img v-if="showGhost" class="board-card board-card-ghost" :style="ghostPositionInfo" draggable="false" :src="imageUrl">
</template>

<style scoped>
.board-card {
  position: absolute;
  user-select: none;
  width: 78px;
  height: 108px;
  background-size: contain;
  margin: 0;
  pointer-events: all;
  border-radius: 3px;
  z-index: 1;
}

.board-card-ghost {
  pointer-events: none;
  opacity: 0.5;
}

.board-card-counter {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  color: white;
  padding: 2px;
  border-radius: 3px;
  font-size: 0.8em;
  font-weight: bold;
  text-align: center;
  padding: 0;
}
</style>