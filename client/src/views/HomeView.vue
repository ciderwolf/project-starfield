<script setup lang="ts">
import { createGame, joinGame, login } from '@/api';
import Modal from '@/components/Modal.vue';
import StyleButton from '@/components/StyleButton.vue';
import GameListingRow from '@/components/GameListingRow.vue';
import { useGameStore } from '@/stores/games';
import { useDataStore } from '@/stores/data';
import { ref } from 'vue';
import { getDecks, newDeck, submitDeck } from '@/api/deck';
import { useRouter } from 'vue-router';
import { ws } from '@/ws';

const showCreateGameModal = ref(false);
const gameName = ref('');
const games = useGameStore();
const router = useRouter();

function submitGame() {
  createGame(gameName.value).then((state) => {
    showCreateGameModal.value = false;
    games.processState({ type: 'state', room: 'lobby', roomState: state });
    router.push(`/lobby/${state.id}`);
  });
}


async function loginClicked() {
  const userInfo = await login('myname');
  const data = useDataStore();
  data.login(userInfo.username, userInfo.id);
  ws.reconnect();
  const listings = await getDecks();
  if (!listings.some(d => d.name === 'TestDeck')) {
    const created = await newDeck();
    await submitDeck(created.id, 'TestDeck', ['Serum Visions', 'Lightning Bolt', 'Path to Exile'], []);
  }
}

</script>

<template>
  <main>
    <h1>Home</h1>
    <style-button @click="loginClicked">Login</style-button>
    <router-link to="/decks">
      <style-button>Decks</style-button>
    </router-link>
    <h2>Games</h2>
    <Modal :visible="showCreateGameModal" @close="showCreateGameModal = false" title="Create Game">
      <h2>Create Game</h2>
      <label>Name: <input type="text" v-model="gameName"></label>
      <br>
      <br>
      <style-button @click="submitGame">Create Game</style-button>
    </Modal>
    <style-button @click="showCreateGameModal = true">Create Game</style-button>
    <div>
      <div v-for="game in games.games" :key="game.id">
        <game-listing-row :game="game"></game-listing-row>
      </div>
    </div>
  </main>
</template>
