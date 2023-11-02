import { ZONES, zoneFromIndex } from "@/zones";
import type { WebSocketConnection } from "./websocket";
import { useBoardStore, type CardId } from "@/stores/board";
import type { PlayerAttribute, SpecialAction, CardAttribute } from "./message";


export abstract class GameClient {
  abstract drawCards(count: number): void;

  abstract takeSpecialAction(action: SpecialAction): void;
  muligan() {
    this.takeSpecialAction('MULLIGAN');
  }
  shuffle() {
    this.takeSpecialAction('SHUFFLE');
  }
  scoop() {
    this.takeSpecialAction('SCOOP');
  }

  abstract changeCardAttribute(zoneId: number, cardId: CardId, attribute: CardAttribute, value: number): void;
  abstract changePlayerAttribute(attribute: PlayerAttribute, newValue: number): void;
  abstract moveCardToZone(zoneId: number, cardId: CardId, newZoneId: number, x: number, y: number): void;
  abstract moveCard(zoneId: number, cardId: CardId, x: number, y: number): void;
}

export class WebSocketGameClient extends GameClient {

  // private board: ReturnType<typeof useBoardStore>;

  constructor(private ws: WebSocketConnection) {
    super();
    // this.board = useBoardStore();
  }

  drawCards(count: number): void {
    this.ws.send({
      type: 'draw_card',
      count
    });
  }
  takeSpecialAction(action: SpecialAction): void {
    this.ws.send({
      type: 'special_action',
      action
    });
  }
  changePlayerAttribute(attribute: PlayerAttribute, newValue: number): void {
    this.ws.send({
      type: 'change_player_attribute',
      attribute,
      newValue
    });
  }

  changeCardAttribute(zoneId: number, cardId: CardId, attribute: CardAttribute, newValue: number): void {
    this.ws.send({
      type: 'change_card_attribute',
      card: cardId,
      attribute,
      newValue
    });
  }

  moveCardToZone(zoneId: number, cardId: CardId, newZoneId: number, x: number, y: number): void {
    if (newZoneId === ZONES.play.id) {
      this.ws.send({
        type: 'play_card',
        card: cardId,
        x,
        y,
      });
    }
    else {
      this.ws.send({
        type: 'change_zone',
        card: cardId,
        zone: zoneFromIndex(newZoneId)!.name,
        index: -1,
      });
    }
  }
  moveCard(zoneId: number, cardId: CardId, x: number, y: number): void {
    if (zoneId === ZONES.play.id) {
      this.ws.send({
        type: 'change_position',
        card: cardId,
        x,
        y,
      });
    }
    else {
      this.ws.send({
        type: 'change_index',
        card: cardId,
        index: -1,
      });
    }
  }
}