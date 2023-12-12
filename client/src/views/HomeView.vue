<script setup lang="ts">
import { createGame, joinGame, login } from '@/api';
import Modal from '@/components/Modal.vue';
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

function joinGameClicked(id: string) {
  joinGame(id).then((state) => {
    if (state) {
      games.processState({ type: 'state', room: 'lobby', roomState: state });
      router.push(`/lobby/${id}`);
    }
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
