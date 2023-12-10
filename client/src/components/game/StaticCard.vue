<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import type { ComponentExposed } from 'vue-component-type-helpers';
import CardImage from '@/components/game/CardImage.vue';
import { ScreenPosition } from '@/zones';
import type { BoardCard, Zone } from '@/api/message';
import { useBoardStore } from '@/stores/board';


const props = defineProps<{ card: BoardCard, zone: Zone, zoneRect?: DOMRect }>();
const board = useBoardStore();

const image = ref<ComponentExposed<typeof CardImage>>();
const imagePos = reactive<{ x: number, y: number }>({ x: 0, y: 0 });

function updateScreenPosFromVirtualCoords() {
  if (props.zoneRect === undefined || image === undefined) {
    return { x: 0, y: 0 };
  }
  const parentRect = props.zoneRect!;

  const screenPos = board.getScreenPositionFromCard(props.card.id);
  const visualYPos = screenPos === ScreenPosition.PRIMARY ? props.card.y : 1 - props.card.y;

  const rect = image.value!.getBounds();
  const x = parentRect.left + parentRect.width * props.card.x - rect.width / 2;
  const y = parentRect.top + parentRect.height * visualYPos - rect.height / 2;

  const clamped = clampToBounds(x, y, rect, parentRect);
  imagePos.x = clamped.x;
  imagePos.y = clamped.y;
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

  // if (props.card.zone === ZONES.hand.id) {
  //   watch([() => board.cards[ZONES.hand.id].length], () => {
  //     updateScreenPosFromVirtualCoords();
  //   })
  // }
})

watch([() => props.zoneRect, () => props.card.x, () => props.card.y], () => {
  if (props.zoneRect) {
    updateScreenPosFromVirtualCoords();
  }
});

</script>

<template>
  <card-image ref="image" :card="card" :moving="false" :board-pos="imagePos" :image-pos="imagePos" />
</template>