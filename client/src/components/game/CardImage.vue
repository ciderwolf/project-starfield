<script setup lang="ts">
import type { BoardCard } from '@/api/message';
import { pivotToAngle, useBoardStore } from '@/stores/board';
import { computed, ref } from 'vue';

interface Position {
  x: number;
  y: number;
}

const props = defineProps<{ card: BoardCard, imagePos: Position, boardPos: Position, moving: boolean }>();

const emits = defineEmits<{
  (event: 'dblclick'): void
  (event: 'mousedown', e: MouseEvent): void
  (event: 'mouseup', e: MouseEvent): void
}>();

const image = ref<HTMLImageElement>();

defineExpose<{ getBounds: () => DOMRect }>({ getBounds: () => image.value!.getBoundingClientRect() });

const board = useBoardStore();

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

const positionInfo = computed(() => ({
  left: `${props.imagePos.x}px`,
  top: `${props.imagePos.y}px`,
  zIndex: props.moving ? 1 : 0,
  transform: `rotate(${pivotToAngle(props.card.pivot)})`,
}));

const ghostPositionInfo = computed(() => ({
  left: `${props.boardPos.x}px`,
  top: `${props.boardPos.y}px`,
  transform: `rotate(${pivotToAngle(props.card.pivot)})`,
}));

</script>

<template>
  <img class="board-card" :style="positionInfo" draggable="false" @dblclick="$emit('dblclick')"
    @mousedown="$emit('mousedown', $event)" @mouseup="$emit('mouseup', $event)" :src="imageUrl" ref="image">
  <img v-if="moving && (boardPos.x != imagePos.x || boardPos.y != imagePos.y)" class="board-card board-card-ghost"
    :style="ghostPositionInfo" draggable="false" :src="imageUrl">
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