import { ZONES, zoneFromIndex } from "@/zones";
import type { WebSocketConnection } from "./websocket";
import { type CardId } from "@/stores/board";
import type { PlayerAttribute, SpecialAction, CardAttribute, CardAttributeMap, Zone } from "./message";

export abstract class GameClient {
  abstract drawCards(count: number, to?: Zone): void;

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

  abstract changeCardAttribute(cardId: CardId, attribute: CardAttribute, value: number): void;
  abstract changePlayerAttribute(attribute: PlayerAttribute, newValue: number): void;
  abstract moveCardToZone(cardId: CardId, newZoneId: number, x: number, y: number): void;
  abstract playWithAttributes(cardId: CardId, x: number, y: number, attributes: Record<CardAttribute, number>): void;
  abstract moveCard(zoneId: number, cardId: CardId, x: number, y: number): void;
  abstract revealCard(cardId: CardId, playerId?: string): void;
}

export class WebSocketGameClient extends GameClient {

  constructor(private ws: WebSocketConnection) {
    super();
  }

  drawCards(count: number, to?: Zone): void {
    this.ws.send({
      type: 'draw_card',
      to: to ?? 'HAND',
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

  changeCardAttribute(cardId: CardId, attribute: CardAttribute, newValue: number): void {
    this.ws.send({
      type: 'change_card_attribute',
      card: cardId,
      attribute,
      newValue
    });
  }

  moveCardToZone(cardId: CardId, newZoneId: number, x: number, y: number): void {
    if (newZoneId === ZONES.play.id) {
      this.ws.send({
        type: 'play_card',
        card: cardId,
        x,
        y,
        attributes: {}
      });
    }
    else {
      this.ws.send({
        type: 'change_zone',
        card: cardId,
        zone: zoneFromIndex(newZoneId)!.type,
        index: -1,
      });
    }
  }

  playWithAttributes(cardId: number, x: number, y: number, attributes: CardAttributeMap): void {
    this.ws.send({
      type: 'play_card',
      card: cardId,
      x,
      y,
      attributes
    });
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

  revealCard(cardId: number, playerId?: string | undefined): void {
    this.ws.send({
      type: 'reveal',
      card: cardId,
      revealTo: playerId ?? null,
    });
  }
}