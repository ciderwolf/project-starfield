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
}