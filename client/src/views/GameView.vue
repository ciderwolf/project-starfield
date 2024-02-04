<script setup lang="ts">
import Zone from '@/components/game/Zone.vue';
import FindCardsModal from '@/components/game/modal/FindCardsModal.vue';
import ScryModal from '@/components/game/modal/ScryModal.vue';
import ViewZoneModal from '@/components/game/modal/ViewZoneModal.vue';
import CreateTokenModal from '@/components/game/modal/CreateTokenModal.vue';
import EndGameModal from '@/components/game/modal/EndGameModal.vue';
import SideboardModal from '@/components/game/modal/SideboardModal.vue';
import CreateCardModal from '@/components/game/modal/CreateCardModal.vue';
import CardPreview from '@/components/game/CardPreview.vue';
import { OPPONENT_ZONES, ZONES } from '@/zones';
import { ref, onMounted, onUnmounted } from 'vue';
import { client } from '@/ws';
import { endGame } from '@/api/lobby';
import { useRoute, useRouter } from 'vue-router';
import { useBoardStore } from '@/stores/board';
import { getVirtualIds } from '@/api/game';
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
    if (confirm('Are you sure you want to scoop your deck?')) {
      client.scoop();
    }
  }
  else if (e.key === 'c') {
    client.drawCards(1);
  }
  else if (e.key === 'q') {
    endGameClicked();
  }
  else if (e.key === 'f') {
    notificationsCache.findCards();
  }
  else if (e.key === 'm') {
    client.mulligan();
  }
  else if (e.key === 'v') {
    client.shuffle();
  } else if (e.key === 'w') {
    createToken();
  } else if (e.key === 'n') {
    createCard();
  }
  else if (e.key === 'e') {
    endGameClicked();
  }
}

function endGameClicked() {
  endGameModal.value?.open();
}

function untapAll() {
  client.untapAll();
}

function createToken() {
  createTokenModal.value?.open();
}

function createCard() {
  createCardModal.value?.open();
}

const findCardsModal = ref<ComponentExposed<typeof FindCardsModal>>();
const scryModal = ref<ComponentExposed<typeof ScryModal>>();
const viewZoneModal = ref<ComponentExposed<typeof ViewZoneModal>>();
const createTokenModal = ref<ComponentExposed<typeof CreateTokenModal>>();
const endGameModal = ref<ComponentExposed<typeof EndGameModal>>();
const sideboardModal = ref<ComponentExposed<typeof SideboardModal>>();
const createCardModal = ref<ComponentExposed<typeof CreateCardModal>>();
const cardPreview = ref<ComponentExposed<typeof CardPreview>>();
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

  notificationsCache.viewZone = (zoneId: number, readOnly = false) => {
    viewZoneModal.value?.open(zoneId, readOnly);
  }

  notificationsCache.scry = (count: number) => {
    client.scry(count);
    scryModal.value?.open(count);
  }

  notificationsCache.showCardPreview = (id: string, backFace: boolean) => {
    cardPreview.value?.showPreview(id, backFace);
  }

  notificationsCache.hideCardPreview = () => {
    cardPreview.value?.hidePreview();
  }

  notificationsCache.sideboard = () => {
    sideboardModal.value?.open();
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
    <create-token-modal ref="createTokenModal" />
    <end-game-modal ref="endGameModal" />
    <sideboard-modal ref="sideboardModal" />
    <create-card-modal ref="createCardModal" />
    <card-preview ref="cardPreview" />
    <div class="game-options">
      <router-link to="/"><button>Go Home</button></router-link>
      <hr>
      <button @click="endGameClicked">End Game</button>
      <button @click="endGameClicked">New Game</button>
      <hr>
      <button @click="createToken">Create Token</button>
      <button @click=createCard>Create Card</button>
      <hr>
      <button @click="untapAll">Untap All</button>
    </div>
    <zone ref="myZones" v-for="zone in ZONES" :zone="zone"></zone>
    <zone ref="opponentZones" v-for="zone in OPPONENT_ZONES" :zone="zone" />
  </div>
</template>

<style scoped>
.game-options {
  position: fixed;
  right: 0;
  bottom: 108px;
  border: none;
  background-color: #0005;
  font-size: 1rem;
  width: 78px;
  text-align: center;
  border-radius: 5px;
}

.game-options button {
  width: 100%;
  border: none;
  background-color: transparent;
  color: white;
  padding: 5px;
  margin: 0;
  cursor: pointer;
}
</style>