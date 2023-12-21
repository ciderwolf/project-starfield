<script setup lang="ts">
import type { BoardCard, Zone } from '@/api/message';
import { useBoardStore } from '@/stores/board';
import BattlefieldCard from '@/components/game/card/BattlefieldCard.vue';
import HandCard from '@/components/game/card/HandCard.vue';
import PileCard from '@/components/game/card/PileCard.vue';
import CardImage from '@/components/game/card/CardImage.vue';
import { computed } from 'vue';

const props = defineProps<{ zoneRect?: DOMRect, card: BoardCard, zone: Zone }>();

const board = useBoardStore();
const isMe = computed(() => board.cardIsMovable(props.card.id));


</script>

<template>
  <template v-if="isMe">
    <battlefield-card v-if="zone === 'BATTLEFIELD'" :zone-bounds="zoneRect" :card="card" />
    <hand-card v-else-if="zone === 'HAND'" :zone-bounds="zoneRect" :card="card" />
    <pile-card v-else :zone-bounds="zoneRect" :card="card" />
  </template>
  <template v-else>
    <card-image :card="card" :moving="false" :image-pos="{ x: 0, y: 0 }" :zone-rect="zoneRect" />
  </template>
</template>