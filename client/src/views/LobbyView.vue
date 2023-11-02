<script setup lang="ts">
import DeckPreview from '@/components/deck/DeckPreview.vue';
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
const isOwner = computed(() => game.value?.users[0] == currentUser);
const deckChoice = ref('none');
watchEffect(() => {
  if (game.value && currentUser) {
    deckChoice.value = game.value.decks[game.value.users.indexOf(currentUser)] ?? 'none';
  }
});

async function submitDeckChoiceId() {
  await submitDeckChoice(deckChoice.value);
}

const deck = ref<Deck | null>(null)
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
    <h1>Lobby <span v-if="game">&mdash; {{ game.name }}</span></h1>
    <button v-if="isOwner" @click="leaveGameClicked">Cancel game</button>
    <button v-else @click="leaveGameClicked">Leave game</button>
    <button v-if="isOwner && game && game.decks.every(d => d !== null)" @click="startGameClicked">Start game</button>
    <p>Players:</p>
    <div v-if="game">
      <div class="player-status" v-for="player, i in game.users" :key="player">
        <p>{{ player }}</p>
        <p>({{ game.decks[i] !== null ? 'Ready' : 'Waiting' }})</p>
        <button v-if="isOwner && player != currentUser" @click="kickPlayerClicked(player)">Kick Player</button>
      </div>
    </div>
    <h3>Select a deck</h3>
    <select v-model="deckChoice" @change="previewDeck">
      <option value="none" disabled>Select a deck...</option>
      <option v-for="deck in decks.decks" :key="deck.id" :value="deck.id">{{ deck.name }}</option>
    </select>
    <button @click="submitDeckChoiceId">Confirm deck</button>
    <deck-preview v-if="deck" :deckData="deck" />
  </div>
</template>