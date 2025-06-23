<script setup lang="ts">
import StyleButton from '@/components/StyleButton.vue';
import { joinGame } from '@/api';
import type { GameListing } from '@/api/message';
import { useDataStore } from '@/stores/data';
import { useGameStore } from '@/stores/games';
import { useRouter } from 'vue-router';

defineProps<{ game: GameListing }>();
const games = useGameStore();
const router = useRouter();

const data = useDataStore();

function myGame(players: { id: string }[]) {
  return players.some(p => p.id === data.userId);
}

function joinGameClicked(id: string) {
  joinGame(id).then((state) => {
    if (state) {
      games.processState({ type: 'state', room: 'LOBBY', roomState: state });
      router.push(`/lobby/${id}`);
    }
  });
}

function rejoinGameClicked(game: GameListing) {
  if (game.inProgress) {
    if (game.type === 'DRAFT') {
      router.push(`/draft/${game.id}`);
    } else {
      router.push(`/game/${game.id}`);
    }
  } else {
    router.push(`/lobby/${game.id}`);
  }
}

function spectateGameClicked(id: string) {
  router.push(`/game/${id}`);
}

</script>

<template>
  <div class="game-listing">
    <b>{{ game.name }}</b>

    <span class="game-players" :class="{ 'in-progress': game.inProgress }">
      <span class="game-player" v-for="player in game.players" :key="player.id">
        {{ player.name }}
      </span>
    </span>

    <style-button v-if="myGame(game.players)" @click="rejoinGameClicked(game)" small>Rejoin</style-button>
    <style-button v-else-if="game.type === 'GAME'" @click="spectateGameClicked(game.id)" small>Spectate</style-button>
    <style-button v-else @click="joinGameClicked(game.id)" small :disabled="game.inProgress">Join</style-button>
  </div>
</template>

<style scoped>
.game-listing {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem;
  border: 1px solid #ddd;
  background: #eee;
  border-radius: 0.5rem;
  margin: 0.5rem auto;
  gap: 10px;
  width: 50%;
  min-width: min-content;
}

.game-listing .game-players {
  display: flex;
}

.game-listing .game-players.in-progress {
  color: gray;
}

.game-listing .game-player {
  font-weight: 500;
}

.game-listing .game-player::after {
  margin: 0 0.5rem;
  content: '•';
}

.game-players.in-progress .game-player:last-child::after {
  content: '';
}
</style>