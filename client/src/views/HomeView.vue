<script setup lang="ts">
import { createGame, createDraft } from '@/api';
import Modal from '@/components/Modal.vue';
import StyleButton from '@/components/StyleButton.vue';
import GameListingRow from '@/components/GameListingRow.vue';
import { useGameStore } from '@/stores/games';
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import DecksView from './DecksView.vue';
import { useDataStore } from '@/stores/data';
import { Tabs, Tab } from 'vue3-tabs-component';
import { CLIENT_VERSION } from '@/version';
import CubesView from './CubesView.vue';

const showCreateGameModal = ref(false);
const gameName = ref('');
const gamePlayers = ref(2);
const botPlayers = ref(0);
const draftSet = ref('');

const games = useGameStore();
const router = useRouter();
const data = useDataStore();
const userName = computed(() => data.userName);

async function submitGame() {
  const state = await createGame(gameName.value, gamePlayers.value)
  showCreateGameModal.value = false;
  games.processState({ type: 'state', room: 'LOBBY', roomState: state });
  router.push(`/lobby/${state.id}`);
}

async function submitDraft() {
  const state = await createDraft(gameName.value, gamePlayers.value, draftSet.value, botPlayers.value)
  showCreateGameModal.value = false;
  games.processState({ type: 'state', room: 'LOBBY', roomState: state });
  router.push(`/lobby/${state.id}`);
}

function tabChanged(tab: any) {
  if (tab.tab.name === 'Draft') {
    gamePlayers.value = 8;
  }
  else {
    gamePlayers.value = 2;
  }

}

</script>

<template>
  <div id="main">
    <main>
      <h1>Welcome, {{ userName }}</h1>
      <div class="title">
        <h2>Games</h2>
        <style-button @click="showCreateGameModal = true" small>+ Create Game</style-button>
      </div>
      <Modal :visible="showCreateGameModal" @close="showCreateGameModal = false" title="Create Game">
        <Tabs :options="{ useUrlFragment: false }" @changed="tabChanged">
          <Tab name="Game">
            <div class="create-game-form">
              <h2>Create Game</h2>
              <label>Name: <input type="text" v-model="gameName"></label>
              <label>Players: <input type="number" min="2" max="4" v-model.number="gamePlayers"></label>
              <style-button @click="submitGame">Create Game</style-button>
            </div>
          </Tab>
          <Tab name="Draft">
            <div class="create-game-form">
              <h2>Create Draft</h2>
              <label>Name: <input type="text" v-model="gameName"></label>
              <label>Players: <input type="number" min="2" max="8" v-model.number="gamePlayers"></label>
              <label>Bot Players: <input type="number" min="0" max="8" v-model.number="botPlayers"></label>
              <label>Set: <input type="text" v-model="draftSet"></label>
              <style-button @click="submitDraft">Create Draft</style-button>
            </div>
          </Tab>
        </Tabs>

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
      <CubesView />

    </main>
    <p class="version-info" v-if="CLIENT_VERSION">Client version {{ CLIENT_VERSION }}</p>
  </div>
</template>


<style>
@import url("@/assets/tabs.css");

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

#main {
  padding: 0 2em;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.create-game-form {
  display: flex;
  flex-direction: column;
  gap: 1em;
}

.version-info {
  padding: 1em;
  font-size: 0.8em;
  text-align: center;
  color: #666;
}
</style>