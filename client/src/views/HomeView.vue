<script setup lang="ts">
import { createGame } from '@/api';
import Modal from '@/components/Modal.vue';
import StyleButton from '@/components/StyleButton.vue';
import GameListingRow from '@/components/GameListingRow.vue';
import { useGameStore } from '@/stores/games';
import { ref } from 'vue';
import { useRouter } from 'vue-router';

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

</script>

<template>
  <main>
    <h1>Home</h1>
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
