import { useGameStore } from "@/stores/games";
import type { ClientMessage, WebSocketMessage } from "./message";
import { useDataStore } from "@/stores/data";
import router from '@/router';
import { useBoardStore } from "@/stores/board";

export class WebSocketConnection {
  private ws: WebSocket;

  constructor() {
    this.ws = new WebSocket('ws://127.0.0.1:8080/ws');
    this.ws.onmessage = this.onMessage;
    this.ws.onclose = this.attemptReconnect;
  }

  onMessage(event: MessageEvent) {
    const message: WebSocketMessage = JSON.parse(event.data);
    const gamesStore = useGameStore();
    const boardStore = useBoardStore();
    const login = useDataStore();
    switch (message.type) {
      case 'location':
        // login.goTo(message.location, message.id);
        console.log('location', message.location);
        break;
      case 'identity':
        console.log('identity', message.username, message.id);
        login.login(message.username, message.id);
        break;
      case 'listing':
        console.log('listing', message.listing);
        gamesStore.processListing(message.listing);
        break;
      case 'delete_listing':
        console.log('delete_listing', message.id);
        gamesStore.processDeleteListing(message.id);
        break;
      case 'state':
        console.log('state', message.room, message.roomState);
        gamesStore.processState(message);
        if (message.room === 'game') {
          boardStore.setBoardState(message.roomState.players);
        }
        // const gameStore = useGameStore();
        // gameStore.processState(message.roomState);
        break;
      case 'oracle_cards':
        console.log('oracle_cards', message);
        boardStore.processOracleInfo(message.oracleInfo);
        break;
      case 'board_update':
        console.log('board_update', message);
        boardStore.processBoardUpdate(message.events);
        break;
      default:
        const _exhaustiveCheck: never = message;
        console.error(_exhaustiveCheck);
    }
  }

  private attemptReconnect(e: CloseEvent) {
    console.log(e);
  }

  send(message: ClientMessage) {
    this.ws.send(JSON.stringify(message));
  }
}