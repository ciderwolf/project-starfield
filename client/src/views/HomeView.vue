<script setup lang="ts">
import { createGame } from '@/api';
import Modal from '@/components/Modal.vue';
import StyleButton from '@/components/StyleButton.vue';
import GameListingRow from '@/components/GameListingRow.vue';
import { useGameStore } from '@/stores/games';
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import DecksView from './DecksView.vue';
import { useDataStore } from '@/stores/data';

const showCreateGameModal = ref(false);
const gameName = ref('');
const gamePlayers = ref(2);
const games = useGameStore();
const router = useRouter();
const data = useDataStore();
const userName = computed(() => data.userName);

function submitGame() {
  createGame(gameName.value, gamePlayers.value).then((state) => {
    showCreateGameModal.value = false;
    games.processState({ type: 'state', room: 'lobby', roomState: state });
    router.push(`/lobby/${state.id}`);
  });
}

</script>

<template>
  <main>
    <h1>Welcome, {{ userName }}</h1>
    <div class="title">
      <h2>Games</h2>
      <style-button @click="showCreateGameModal = true" small>+ Create Game</style-button>
    </div>
    <Modal :visible="showCreateGameModal" @close="showCreateGameModal = false" title="Create Game">
      <h2>Create Game</h2>
      <label>Name: <input type="text" v-model="gameName"></label>
      <br>
      <br>
      <label>Players: <input type="number" min="2" max="4" v-model.number="gamePlayers"></label>
      <br>
      <br>
      <style-button @click="submitGame">Create Game</style-button>
    </Modal>
    <div v-if="Object.keys(games.games).length > 0">
      <div v-for="game in games.games" :key="game.id">
        <game-listing-row :game="game"></game-listing-row>
      </div>
    </div>
    <div v-else class="empty-container-title">
      <h3>No games right now.</h3>
      <p>Click on '+ Create game' to create one.</p>
    </div>
    <DecksView />
  </main>
</template>


<style>
.title {
  display: flex;
  gap: 20px;
  align-items: center;
  margin-bottom: 1em;
  align-items: center;
}

.empty-container-title {
  margin-top: 1em;
  text-align: center;
  color: #333;
  font-style: italic;
}

main {
  padding: 0 2em;
}
</style>