<script setup lang="ts">
import Zone from '@/components/game/Zone.vue';
import FindCardsModal from '@/components/game/modal/FindCardsModal.vue';
import ScryModal from '@/components/game/modal/ScryModal.vue';
import ViewZoneModal from '@/components/game/modal/ViewZoneModal.vue';
import CreateTokenModal from '@/components/game/modal/CreateTokenModal.vue';
import EndGameModal from '@/components/game/modal/EndGameModal.vue';
import SideboardModal from '@/components/game/modal/SideboardModal.vue';
import CreateCardModal from '@/components/game/modal/CreateCardModal.vue';
import RollDieModal from '@/components/game/modal/RollDieModal.vue';
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
import GameOptionButtons from '@/components/game/GameOptionButtons.vue';
import GameLog from '@/components/game/GameLog.vue';


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

function rollDie() {
  rollDieModal.value?.open();
}

const findCardsModal = ref<ComponentExposed<typeof FindCardsModal>>();
const scryModal = ref<ComponentExposed<typeof ScryModal>>();
const viewZoneModal = ref<ComponentExposed<typeof ViewZoneModal>>();
const createTokenModal = ref<ComponentExposed<typeof CreateTokenModal>>();
const endGameModal = ref<ComponentExposed<typeof EndGameModal>>();
const sideboardModal = ref<ComponentExposed<typeof SideboardModal>>();
const createCardModal = ref<ComponentExposed<typeof CreateCardModal>>();
const rollDieModal = ref<ComponentExposed<typeof RollDieModal>>();
const cardPreview = ref<ComponentExposed<typeof CardPreview>>();
const notificationsCache = useNotificationsCache();


onMounted(() => {
  window.addEventListener('keypress', checkHotkey);
  notificationsCache.findCards = () => {
    getVirtualIds().then((message) => {
      board.processOracleInfo({}, message.oracleInfo, []);
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

  board.logs.splice(0, board.logs.length);
});

watchEffect(() => {
  if (games.isLoaded) {
    gameId.value = route.params.id as string;

    const game = games.getGame(gameId.value);
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
    <roll-die-modal ref="rollDieModal" />
    <card-preview ref="cardPreview" />
    <div class="above-deck-elements">
      <GameOptionButtons :isSpectator="isSpectator" @end-game="endGameClicked" @create-token="createToken"
        @create-card="createCard" @untap-all="untapAll" @roll-die="rollDie" />
      <GameLog />
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
  top: 108px;
  height: calc(100vh - 2 * 108px);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
</style>