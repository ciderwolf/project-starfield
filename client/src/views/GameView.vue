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
import PlayerCounters from '@/components/game/PlayerCounters.vue';
import { OPPONENT_ZONES, ZONES } from '@/zones';
import { ref, onMounted, onUnmounted, watchEffect, computed } from 'vue';
import { client } from '@/ws';
import { useBoardStore } from '@/stores/board';
import { beginSpectating, getVirtualIds, stopSpectating } from '@/api/game';
import { useNotificationsCache } from '@/cache/notifications';
import type { ComponentExposed } from 'vue-component-type-helpers';
import { useGameStore } from '@/stores/games';
import { useDataStore } from '@/stores/data';
import { useRouter, useRoute } from 'vue-router';


const myZones = ref<HTMLElement[]>([]);
const opponentZones = ref<HTMLElement[]>([]);
const router = useRouter();
const route = useRoute();
const board = useBoardStore();
const data = useDataStore();
const games = useGameStore();
const gameId = ref('');
const isSpectator = computed(() => {
  if (games.isLoaded === false) {
    return true;
  }
  const game = games.getGame(gameId.value);
  return game?.players.find(p => p.id === data.userId) === undefined;
});


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
  } else if (e.key === 'x') {
    untapAll();
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

watchEffect(() => {
  console.log('watchEffect', games.isLoaded);
  if (games.isLoaded) {
    gameId.value = route.params.id as string;

    const game = games.getGame(gameId.value);
    console.log('watchEffect', game, gameId, route.params.id, games.games);
    if (game === undefined) {
      router.push('/');
      return;
    }
    handleSpectatorJoin();
  }
});

function handleSpectatorJoin() {
  if (isSpectator.value) {
    beginSpectating(gameId.value);
  }
}

function handleSpectatorLeave() {
  if (isSpectator.value) {
    stopSpectating(gameId.value);
  }
}

onUnmounted(() => {
  handleSpectatorLeave();
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
    <div class="above-deck-elements">
      <div class="game-options" v-if="isSpectator">
        <router-link to="/"><button>Go Home</button></router-link>
      </div>
      <div class="game-options" v-else>
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
    </div>
    <div class="player-counters">
      <player-counters v-for="player in board.players" :player="player" />
    </div>
    <zone ref="myZones" v-for="zone in ZONES" :zone="zone"></zone>
    <zone ref="opponentZones" v-for="zone in OPPONENT_ZONES" :zone="zone" />
  </div>
</template>

<style scoped>
.above-deck-elements {
  position: fixed;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
}

.game-options {
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
  border-radius: 5px;
}

.game-options button:hover {
  background-color: #0008;
}
</style>