import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { getGames } from '@/api';
import type { GameListing, GameState, LobbyState, RoomStateMessage } from '@/api/message';

type GameMap = { [id: string]: GameListing }

export const useGameStore = defineStore('games', () => {

  getGames().then((gameInfo) => {
    for(const game of gameInfo) {
      games.value[game.id] = game;
    }
  });

  function processListing(listing: GameListing) {
    games.value[listing.id] = listing;
  }

  function processDeleteListing(id: string) {
    delete games.value[id];
  }


  const gameState = ref<GameState | null>(null);
  const lobbyState = ref<LobbyState | null>(null);
  function processState(message: RoomStateMessage) {
    switch (message.room) {
      case 'lobby':
        lobbyState.value = message.roomState;
        break;
      case 'game':
        gameState.value = message.roomState;
        break;
      default:
        const _exhaustiveCheck: never = message;
        console.error(_exhaustiveCheck);
    }
  }

  function getGame(id: string) {
    return games.value[id];
  }

  const games = ref<GameMap>({});
  return { games, processListing, processDeleteListing, processState, getGame, gameState, lobbyState };
});
