import { useGameStore } from "@/stores/games";
import type { ClientMessage, WebSocketMessage } from "./message";
import { useDataStore } from "@/stores/data";
import { useBoardStore } from "@/stores/board";
import { useAlertsStore } from "@/stores/alerts";
import { useDecksStore } from "@/stores/decks";
import { useDraftStore } from "@/stores/draft";

function websocketUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/ws`;
}

export class WebSocketConnection {

  private ws!: WebSocket;

  constructor() {
    this.reconnect();
  }

  onMessage(event: MessageEvent) {
    const message: WebSocketMessage = JSON.parse(event.data);
    const gamesStore = useGameStore();
    const boardStore = useBoardStore();
    const draftStore = useDraftStore();
    const login = useDataStore();
    switch (message.type) {
      case 'location':
        // login.goTo(message.location, message.id);
        console.log('location', message.location);
        break;
      case 'identity':
        console.log('identity', message.username, message.id);
        const decks = useDecksStore();
        decks.reloadDecks();
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
        if (message.room === 'GAME') {
          boardStore.setBoardState(message.roomState.players, message.roomState.currentPlayer, message.roomState.oracleInfo);
        }
        else if (message.room === 'DRAFT') {
          draftStore.setDraftState(message.roomState);
        }
        // const gameStore = useGameStore();
        // gameStore.processState(message.roomState);
        break;
      case 'oracle_cards':
        console.log('oracle_cards', message);
        boardStore.processOracleInfo(message.cards, message.oracleInfo, message.cardsToHide);
        break;
      case 'log':
        console.log('log', message);
        boardStore.processLog(message.message, message.owner);
        break;
      case 'board_update':
        console.log('board_update', message);
        boardStore.processBoardUpdate(message.events);
        break;
      case 'draft_event':
        console.log('draft_event', message);
        message.events.forEach(event => draftStore.processEvent(event));
        // draftStore.processEvent(message.event);
        break;
      default:
        const _exhaustiveCheck: never = message;
        console.error(_exhaustiveCheck);
    }
  }

  private attemptReconnect(e: CloseEvent) {
    console.log(e);
    if (e.reason === "Closed due to inactivity") {
      const alerts = useAlertsStore();
      alerts.addAlert('Connection lost', 'You were disconnected due to inactivity. Reload the page to reconnect', 'warning');
    }
  }

  reconnect() {
    this.ws = new WebSocket(websocketUrl());
    this.ws.onmessage = this.onMessage;
    this.ws.onclose = this.attemptReconnect;
  }

  send(message: ClientMessage) {
    this.ws.send(JSON.stringify(message));
  }
}