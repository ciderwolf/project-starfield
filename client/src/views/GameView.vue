<script setup lang="ts">
import Zone from '@/components/game/Zone.vue';
import FindCardsModal from '@/components/game/modal/FindCardsModal.vue';
import ViewZoneModal from '@/components/game/modal/ViewZoneModal.vue';
import { OPPONENT_ZONES, ZONES } from '@/zones';
import { ref, onMounted, onUnmounted } from 'vue';
import { client } from '@/ws';
import { endGame } from '@/api/lobby';
import { useRoute, useRouter } from 'vue-router';
import { useBoardStore, type OracleId } from '@/stores/board';
import { getVirtualIds } from '@/api/game';
import { useNotificationsCache } from '@/cache/notifications';
import type { ComponentExposed } from 'vue-component-type-helpers';

const myZones = ref<HTMLElement[]>([]);
const opponentZones = ref<HTMLElement[]>([]);
const route = useRoute();
const router = useRouter();

const deck = ref<{ [key: string]: OracleId } | null>(null);


function checkHotkey(e: KeyboardEvent) {
  if ((e.target as HTMLElement).nodeName === "INPUT") {
    return;
  }

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
    notificationsCache.get('find-cards')!();
  }
  else if (e.key === 'm') {
    client.takeSpecialAction('MULLIGAN');
  }
}

const findCardsModal = ref<ComponentExposed<typeof FindCardsModal>>();
const viewGraveyardModal = ref<ComponentExposed<typeof ViewZoneModal>>();
const viewExileModal = ref<ComponentExposed<typeof ViewZoneModal>>();
const viewFaceDownModal = ref<ComponentExposed<typeof ViewZoneModal>>();
const notificationsCache = useNotificationsCache();

onMounted(() => {
  window.addEventListener('keypress', checkHotkey);
  notificationsCache.set('find-cards', () => {
    getVirtualIds().then((message) => {
      deck.value = message.virtualIds;
      const board = useBoardStore();
      board.processOracleInfo({}, message.oracleInfo);
      findCardsModal.value?.open();
    });
  });

  notificationsCache.set('view-graveyard', () => {
    viewGraveyardModal.value?.open();
  });
  notificationsCache.set('view-exile', () => {
    console.log('view-exile');
    viewExileModal.value?.open();
  });
  notificationsCache.set('view-face-down', () => {
    viewFaceDownModal.value?.open();
  });
});

onUnmounted(() => {
  window.removeEventListener('keypress', checkHotkey);
});

</script>

<template>
  <div id="game">
    <find-cards-modal ref="findCardsModal" :cards="deck ?? {}" />
    <view-zone-modal ref="viewGraveyardModal" :zone-id="ZONES.graveyard.id" />
    <view-zone-modal ref="viewExileModal" :zone-id="ZONES.exile.id" />
    <view-zone-modal ref="viewFaceDownModal" :zone-id="ZONES.faceDown.id" />
    <zone ref="myZones" v-for="zone in ZONES" :zone="zone"></zone>
    <zone ref="opponentZones" v-for="zone in OPPONENT_ZONES" :zone="zone" />
  </div>
</template>

<style scoped></style>