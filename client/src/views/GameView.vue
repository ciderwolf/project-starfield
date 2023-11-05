<script setup lang="ts">
import Zone from '@/components/game/Zone.vue';
import { OPPONENT_ZONES, ZONES } from '@/zones';
import { ref, onMounted, onUnmounted } from 'vue';
import { client } from '@/ws';
import { endGame } from '@/api/lobby';
import { useRoute, useRouter } from 'vue-router';

const myZones = ref<HTMLElement[]>([]);
const opponentZones = ref<HTMLElement[]>([]);
const route = useRoute();
const router = useRouter();

function checkHotkey(e: KeyboardEvent) {
  if (e.key === '1') {
    client.takeSpecialAction('SCOOP');
  }
  else if (e.key === 'c') {
    client.drawCards(1);
  }
  else if (e.key === '2') {
    endGame(route.params.id as string).then(() => {
      router.push('/');
    });
  }
}

onMounted(() => {
  window.addEventListener('keypress', checkHotkey);
});

onUnmounted(() => {
  window.removeEventListener('keypress', checkHotkey);
});

</script>

<template>
  <div id="game">
    <zone ref="myZones" v-for="zone in ZONES" :zone="zone"></zone>
    <zone ref="opponentZones" v-for="zone in OPPONENT_ZONES" :zone="zone" />
  </div>
</template>

<style scoped></style>