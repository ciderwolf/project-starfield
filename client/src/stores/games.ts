import { ref, reactive } from 'vue'
import { defineStore } from 'pinia'
import { getGames } from '@/api';
import type { DraftState, GameListing, GameState, LobbyState, RoomStateMessage } from '@/api/message';
import { useRouter } from 'vue-router';

type GameMap = { [id: string]: GameListing }

export const useGameStore = defineStore('games', () => {
  const router = useRouter();
  const isLoaded = ref(false);
  getGames().then((gameInfo) => {
    for (const game of gameInfo) {
      games[game.id] = game;
    }
    isLoaded.value = true;
  });

  function processListing(listing: GameListing) {
    games[listing.id] = listing;
  }

  function processDeleteListing(id: string) {
    if (gameState.value?.id === id || lobbyState.value?.id === id) {
      gameState.value = null;
      lobbyState.value = null;
      if (draftState.value?.id !== id) {
        // drafts shouldn't automatically kick users out to the home page
        router.push('/');
      }
    }
    delete games[id];
  }


  const gameState = ref<GameState | null>(null);
  const lobbyState = ref<LobbyState | null>(null);
  const draftState = ref<DraftState | null>(null);
  function processState(message: RoomStateMessage) {
    switch (message.room) {
      case 'LOBBY':
        lobbyState.value = message.roomState;
        break;
      case 'GAME':
        gameState.value = message.roomState;
        break;
      case 'DRAFT':
        draftState.value = message.roomState;
        break
      default:
        const _exhaustiveCheck: never = message;
        console.error(_exhaustiveCheck);
    }
  }

  function getGame(id: string) {
    return games[id];
  }

  const games = reactive<GameMap>({});
  return { games, processListing, processDeleteListing, processState, getGame, gameState, lobbyState, isLoaded };
});
