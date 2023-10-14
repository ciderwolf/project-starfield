<script setup lang="ts">
import type { Card } from '@/api/message';
import BattlefieldZone from '@/components/game/BattlefieldZone.vue';
import Zone from '@/components/game/Zone.vue';
import { OPPONENT_ZONES, ZONES } from '@/zones';
import { ref } from 'vue';

const myZones = ref<HTMLElement[]>([]);
const opponentZones = ref<HTMLElement[]>([]);

</script>

<template>
  <div id="game">
    <zone ref="myZones" v-for="zone in ZONES" :zone="zone"></zone>
    <zone :cards="[]" v-for="zone in OPPONENT_ZONES" :zone="zone" ref="opponentZones" />
  </div>
</template>

<style scoped></style>