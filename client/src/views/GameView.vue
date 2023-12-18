<script setup lang="ts">
import Zone from '@/components/game/Zone.vue';
import { OPPONENT_ZONES, ZONES } from '@/zones';
import { ref, onMounted, onUnmounted } from 'vue';
import { client } from '@/ws';
import { endGame } from '@/api/lobby';
import { useRoute, useRouter } from 'vue-router';
import FindCardsModal from '@/components/game/FindCardsModal.vue';
import { useDecksCache } from '@/cache/decks';
import { useDecksStore } from '@/stores/decks';
import { useBoardStore, type OracleId } from '@/stores/board';
import { getVirtualIds } from '@/api/game';

const myZones = ref<HTMLElement[]>([]);
const opponentZones = ref<HTMLElement[]>([]);
const route = useRoute();
const router = useRouter();

const decks = useDecksCache();
const decksStore = useDecksStore();

const deck = ref<{ [key: string]: OracleId } | null>(null);


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
  else if (e.key === '3') {
    getVirtualIds().then((message) => {
      deck.value = message.virtualIds;
      const board = useBoardStore();
      board.processOracleInfo({}, message.oracleInfo);
      findCardsModal.value?.open();
    });
  }
  else if (e.key === 'm') {
    client.takeSpecialAction('MULLIGAN');
  }
}

const findCardsModal = ref();

onMounted(() => {
  window.addEventListener('keypress', checkHotkey);
});

onUnmounted(() => {
  window.removeEventListener('keypress', checkHotkey);
});

</script>

<template>
  <div id="game">
    <find-cards-modal ref="findCardsModal" :cards="deck ?? {}" />
    <zone ref="myZones" v-for="zone in ZONES" :zone="zone"></zone>
    <zone ref="opponentZones" v-for="zone in OPPONENT_ZONES" :zone="zone" />
  </div>
</template>

<style scoped></style>