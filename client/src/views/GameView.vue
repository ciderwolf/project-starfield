<script setup lang="ts">
import Zone from '@/components/game/Zone.vue';
import FindCardsModal from '@/components/game/modal/FindCardsModal.vue';
import ScryModal from '@/components/game/modal/ScryModal.vue';
import ViewZoneModal from '@/components/game/modal/ViewZoneModal.vue';
import { OPPONENT_ZONES, ZONES } from '@/zones';
import { ref, onMounted, onUnmounted } from 'vue';
import { client } from '@/ws';
import { endGame } from '@/api/lobby';
import { useRoute, useRouter } from 'vue-router';
import { useBoardStore, type OracleId } from '@/stores/board';
import { getVirtualIds, getVirtualScryIds } from '@/api/game';
import { useNotificationsCache } from '@/cache/notifications';
import type { ComponentExposed } from 'vue-component-type-helpers';

const myZones = ref<HTMLElement[]>([]);
const opponentZones = ref<HTMLElement[]>([]);
const route = useRoute();
const router = useRouter();

function checkHotkey(e: KeyboardEvent) {
  if ((e.target as HTMLElement).nodeName === "INPUT") {
    return;
  }
  console.log(e);
  if (e.key === 'r') {
    client.takeSpecialAction('SCOOP');
  }
  else if (e.key === 'c') {
    client.drawCards(1);
  }
  else if (e.key === 'q') {
    endGame(route.params.id as string).then(() => {
      router.push('/');
    });
  }
  else if (e.key === 'f') {
    notificationsCache.findCards();
  }
  else if (e.key === 'm') {
    client.takeSpecialAction('MULLIGAN');
  }
  else if (e.key === 'v') {
    client.takeSpecialAction('SHUFFLE');
  }
}

const findCardsModal = ref<ComponentExposed<typeof FindCardsModal>>();
const scryModal = ref<ComponentExposed<typeof ScryModal>>();
const viewZoneModal = ref<ComponentExposed<typeof ViewZoneModal>>();
const notificationsCache = useNotificationsCache();


onMounted(() => {
  window.addEventListener('keypress', checkHotkey);
  notificationsCache.findCards = () => {
    getVirtualIds().then((message) => {
      const board = useBoardStore();
      board.processOracleInfo({}, message.oracleInfo);
      findCardsModal.value?.open(message.virtualIds);
    });
  };

  notificationsCache.viewZone = (zoneId: number) => {
    viewZoneModal.value?.open(zoneId);
  }

  notificationsCache.scry = (count: number) => {
    client.scry(count);
    scryModal.value?.open(count);
  }
});

onUnmounted(() => {
  window.removeEventListener('keypress', checkHotkey);
});

</script>

<template>
  <div id="game">
    <find-cards-modal ref="findCardsModal" />
    <scry-modal ref="scryModal" />
    <view-zone-modal ref="viewZoneModal" />
    <zone ref="myZones" v-for="zone in ZONES" :zone="zone"></zone>
    <zone ref="opponentZones" v-for="zone in OPPONENT_ZONES" :zone="zone" />
  </div>
</template>

<style scoped></style>