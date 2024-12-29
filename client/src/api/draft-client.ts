import type { WebSocketConnection } from "./websocket";

export class WebSocketDraftClient {
  constructor(private ws: WebSocketConnection) {
  }

  pickCard(card: string): void {
    this.ws.send({
      type: 'pick',
      card
    });
  }

  moveCard(card: string, sideboard: boolean): void {
    this.ws.send({
      type: 'move_zone',
      card,
      sideboard
    });
  }
}