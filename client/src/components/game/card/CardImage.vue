<script setup lang="ts">
import { Highlight, type BoardCard } from '@/api/message';
import { useNotificationsCache } from '@/cache/notifications';
import { nonRotatedRect, pivotToAngle, useBoardStore } from '@/stores/board';
import { useDataStore } from '@/stores/data';
import { useZoneStore } from '@/stores/zone';
import { ScreenPosition, ZONES, isOpponentZone, zoneFromIndex } from '@/zones';
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
const animate = ref(false);

defineExpose<{ getBounds: () => DOMRect, recomputePosition: () => void, cardPosition: () => Position }>({
  getBounds: () => image.value!.getBoundingClientRect(),
  recomputePosition: () => updateScreenPosFromVirtualCoords(),
  cardPosition: () => boardPos,
});

const board = useBoardStore();
const zones = useZoneStore();
const notifications = useNotificationsCache();

const imageUrl = computed(() => {
  if (board.cardToOracleId[props.card.id] && !props.card.flipped) {
    const id = board.cardToOracleId[props.card.id];
    const oracleCard = board.oracleInfo[id];
    if (props.card.transformed) {
      return oracleCard.backImage || './back.png';
    } else {
      return oracleCard.image;
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
      backgroundImage: `url(${imageUrl.value})`,
    };
  } else {
    return {
      left: `${boardPos.x}px`,
      top: `${boardPos.y}px`,
      transform: `rotate(${pivotToAngle(props.card.pivot)})`,
      backgroundImage: `url(${imageUrl.value})`,
      transition: animate.value ? '0.3s' : 'none',
      outline: props.card.highlight === Highlight.SELECTED
        ? '3px solid rgb(78, 128, 220)'
        : props.card.highlight === Highlight.LOG && (board.cardIsMovable(props.card.id) || zoneFromIndex(props.card.zone)?.type !== 'HAND')
          ? '3px solid gold'
          : 'none',
    };
  }
});

const ghostPositionInfo = computed(() => ({
  left: `${boardPos.x}px`,
  top: `${boardPos.y}px`,
  transform: `rotate(${pivotToAngle(props.card.pivot)})`,
}));

const shouldShowRevealedIndicator = computed(() => {
  const data = useDataStore();
  const zone = zoneFromIndex(props.card.zone)!;
  const [_, revealedToOpponents] = zone.reveal;

  return !revealedToOpponents
    && board.cardIsMovable(props.card.id)
    && props.card.visibility.some(player => player !== data.userId && player in board.players);
});


function updateScreenPosFromVirtualCoords() {
  if (props.zoneRect === undefined || image === undefined) {
    return { x: 0, y: 0 };
  }
  const parentRect = props.zoneRect!;

  const screenPos = board.getScreenPositionFromCard(props.card.id);
  const visualYPos = screenPos === ScreenPosition.PRIMARY ? props.card.y : 1 - props.card.y;

  const rect = image.value!.getBoundingClientRect();
  const cardRect = nonRotatedRect(rect, props.card.pivot);
  const x = parentRect.left + parentRect.width * props.card.x - cardRect.width / 2;
  const y = parentRect.top + parentRect.height * visualYPos - cardRect.height / 2;

  const clamped = clampToBounds(x, y, cardRect, parentRect);
  boardPos.x = clamped.x;
  boardPos.y = clamped.y;
}

function clampToBounds(x: number, y: number, rect: DOMRect, parentRect: DOMRect) {
  const { top, bottom } = zones.playFieldExtents();
  const minY = props.card.zone === ZONES.play.id ? top : parentRect.top;
  const maxY = isOpponentZone(props.card.zone, 'BATTLEFIELD') ? bottom : parentRect.top + parentRect.height;
  x = Math.max(parentRect.left, Math.min(x, parentRect.left + parentRect.width - rect.width));
  y = Math.max(minY, Math.min(y, maxY - rect.height));
  return { x, y };
}

let showPreviewTimerHandle: number | undefined;
function mouseEnter() {
  const id = board.cardToOracleId[props.card.id];
  if (id && showPreviewTimerHandle === undefined) {
    showPreviewTimerHandle = window.setTimeout(() => {
      notifications.showCardPreview(id, props.card.transformed);
    }, 250);
  }
}

function mouseLeave() {
  if (showPreviewTimerHandle) {
    window.clearTimeout(showPreviewTimerHandle);
    showPreviewTimerHandle = undefined;
  }
  notifications.hideCardPreview();
}

onMounted(() => {
  if (props.zoneRect) {
    updateScreenPosFromVirtualCoords();
  }
  setTimeout(() => {
    animate.value = zoneFromIndex(props.card.zone)?.type === 'BATTLEFIELD';
  }, 100);
});

watch([() => props.zoneRect, () => props.card.x, () => props.card.y], () => {
  if (props.zoneRect) {
    updateScreenPosFromVirtualCoords();
  }
});

</script>

<template>
  <figure class="board-card" :style="positionInfo" draggable="false" @dblclick="$emit('dblclick')"
    @mousedown="$emit('mousedown', $event)" @mouseup="$emit('mouseup', $event)" @mouseenter="mouseEnter"
    @mouseleave="mouseLeave" ref="image">
    <span v-if="card.counter > 0" class="board-card-counter">{{ card.counter }}</span>
    <span v-if="shouldShowRevealedIndicator" class="board-card-revealed material-symbols-rounded">visibility</span>
  </figure>
  <img v-if="showGhost" class="board-card board-card-ghost" :style="ghostPositionInfo" draggable="false"
    :src="imageUrl">
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
  box-sizing: border-box;
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

.board-card-revealed {
  position: absolute;
  top: 25%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 1.5em;
  color: white;
  background-color: rgba(0, 0, 0, 0.5);
  padding: 2px;
  border-radius: 50%;
}
</style>