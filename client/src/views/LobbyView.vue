<script setup lang="ts">
import DeckPreview from '@/components/deck/DeckPreview.vue';
import StyleButton from '@/components/StyleButton.vue';
import { submitDeckChoice, leaveGame, kickPlayer, startGame } from '@/api/lobby';
import { useDecksStore } from '@/stores/decks';
import { useGameStore } from '@/stores/games';
import { computed, ref, watchEffect } from 'vue';
import { useDecksCache } from '@/cache/decks';
import { type Deck } from '@/api/message';
import { useDataStore } from '@/stores/data';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter()
const gameId = route.params.id as string;
const games = useGameStore();
const game = computed(() => games.lobbyState);
const decks = useDecksStore();


const currentUser = useDataStore().userId;
const isOwner = computed(() => game.value?.users[0].id == currentUser);
const deckChoice = ref('none');
const deck = ref<Deck | null>(null)

watchEffect(() => {
  if (game.value && currentUser) {
    const currentDeckId = game.value.users.findIndex(u => u.id === currentUser);
    if (deckChoice.value === 'none') {
      deckChoice.value = game.value.decks[currentDeckId] ?? 'none';
    }
  }
});

watchEffect(() => {
  if (games.isLoaded && !games.games[gameId]) {
    router.push('/');
  }
})

watchEffect(() => {
  if (deckChoice.value !== 'none') {
    previewDeck();
  }
});

watchEffect(() => {
  if (games.games[gameId]?.inProgress) {
    router.push(`/game/${gameId}`);
  }
});

async function submitDeckChoiceId() {
  await submitDeckChoice(deckChoice.value);
}

async function previewDeck() {
  const deckCache = useDecksCache();
  deck.value = await deckCache.get(deckChoice.value);
}

async function leaveGameClicked() {
  await leaveGame(gameId);
  router.push('/');
}

async function kickPlayerClicked(player: string) {
  await kickPlayer(gameId, player);
}

async function startGameClicked() {
  await startGame(gameId);
  router.push(`/game/${gameId}`);
}

</script>

<template>
  <div id="lobby">
    <div class="title">
      <h1>Lobby <span v-if="game">&mdash; {{ game.name }}</span></h1>
      <style-button v-if="isOwner" @click="leaveGameClicked" type="danger">Cancel game</style-button>
      <style-button v-else @click="leaveGameClicked" type="danger">Leave game</style-button>
      <style-button v-if="isOwner && game && game.decks.every(d => d !== null)" @click="startGameClicked">Start
        game</style-button>
    </div>
    <h2>Players</h2>
    <div v-if="game" class="player-status-cards">
      <div class="player-status-card" v-for="player, i in game.users " :key="player.id">
        <b>{{ player.name }}</b>
        <p>({{ game.decks[i] !== null ? 'Ready' : 'Waiting' }})</p>
        <style-button v-if="isOwner && player.id != currentUser" @click="kickPlayerClicked(player.id)" small
          type="danger">Kick
          Player</style-button>
        <style-button v-if="player.id === currentUser" @click="submitDeckChoiceId" small
          :disabled="deckChoice === 'none'">Confirm deck</style-button>
      </div>
    </div>
    <div class="deck-select-section">
      <h3>Select a deck</h3>
      <select class="deck-select" v-model="deckChoice">
        <option value="none" disabled>Select a deck...</option>
        <option v-for="deck in decks.decks" :key="deck.id" :value="deck.id">{{ deck.name }}</option>
      </select>
    </div>
    <deck-preview v-if="deck" :deckData="deck" />
  </div>
</template>

<style scoped>
.title {
  display: flex;
  align-items: center;
  gap: 50px;
}

.player-status-cards {
  display: flex;
  align-items: center;
  gap: 20px;
  margin: 0 auto;
  width: fit-content;

}

.player-status-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  margin: 0 auto;
  width: 250px;
  height: 150px;
  box-shadow: 0 0 5px 0 rgba(0, 0, 0, 0.5);
  padding: 20px;
}

.deck-select-section {
  display: flex;
  align-items: center;
  gap: 20px;
  margin: 0 auto;
  width: fit-content;
}
</style>