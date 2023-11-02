<script setup lang="ts">
import { createGame, joinGame, login } from '@/api';
import Modal from '@/components/Modal.vue';
import { useGameStore } from '@/stores/games';
import { useDataStore } from '@/stores/data';
import { ref } from 'vue';
import { getDecks, newDeck, submitDeck } from '@/api/deck';

const showCreateGameModal = ref(false);
const gameName = ref('');
const games = useGameStore();

function submitGame() {
  createGame(gameName.value).then((state) => {
    showCreateGameModal.value = false;
    games.processState({ type: 'state', room: 'lobby', roomState: state });
  });
}

function joinGameClicked(id: string) {
  joinGame(id).then((state) => {
    if (state) {
      games.processState({ type: 'state', room: 'lobby', roomState: state });
    }
  });
}

async function loginClicked() {
  const userInfo = await login('myname');
  const data = useDataStore();
  data.login(userInfo.username, userInfo.id);
  // todo: reconnect ws
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
    <button @click="loginClicked">Login</button>
    <router-link to="/decks"><button>Decks</button></router-link>
    <h2>Games</h2>
    <Modal :visible="showCreateGameModal" @close="showCreateGameModal = false">
      <h2>Create Game</h2>
      <label>Name: <input type="text" v-model="gameName"></label>
      <button type="submit" @click="submitGame">Create Game</button>
    </Modal>
    <button @click="showCreateGameModal = true">Create Game</button>
    <ul>
      <li v-for="game in games.games" :key="game.id">
        <router-link v-if="game.inProgress" :to="`/game/${game.id}`">{{ game.name }}</router-link>
        <div v-else>
          <router-link :to="`/lobby/${game.id}`">{{ game.name }}</router-link>
          <button @click="joinGameClicked(game.id)">Join</button>
        </div>
      </li>
    </ul>
  </main>
</template>
