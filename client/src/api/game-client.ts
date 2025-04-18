import { ZONES, zoneFromIndex } from "@/zones";
import type { WebSocketConnection } from "./websocket";
import { type CardId, type OracleId } from "@/stores/board";
import type { PlayerAttribute, SpecialAction, CardAttribute, CardAttributeMap, Zone } from "./message";
import { getJson } from ".";

export abstract class GameClient {
  abstract drawCards(count: number, to?: Zone): void;

  abstract takeSpecialAction(action: SpecialAction): void;
  mulligan() {
    this.takeSpecialAction('MULLIGAN');
  }
  shuffle() {
    this.takeSpecialAction('SHUFFLE');
  }
  scoop() {
    this.takeSpecialAction('SCOOP');
  }
  untapAll() {
    this.takeSpecialAction('UNTAP_ALL');
  }
  endTurn() {
    this.takeSpecialAction('END_TURN');
  }

  abstract changeCardAttribute(cardId: CardId, attribute: CardAttribute, value: number): void;
  abstract changePlayerAttribute(attribute: PlayerAttribute, newValue: number): void;
  abstract moveCardToZone(cardId: CardId, newZoneId: number, x: number, y: number): void;
  abstract moveCardsToZone(cardId: CardId[], newZoneId: number, index: number): void;
  abstract moveCardsVirtual(ids: string[], zoneId: number, index: number): void;
  abstract moveCardToZoneWithIndex(cardId: CardId, newZoneId: number, index: number): void;
  abstract playWithAttributes(cardId: CardId, x: number, y: number, attributes: Record<CardAttribute, number>): void;
  abstract moveCard(zoneId: number, cardId: CardId, x: number, y: number): void;
  abstract revealCard(cardId: CardId, playerId?: string): void;
  abstract scry(count: number): void;
  abstract createToken(id: OracleId): void;
  abstract createCard(id: OracleId): void;
  abstract cloneCard(id: CardId): void;
  abstract sideboard(main: string[], side: string[]): void;
}

export class WebSocketGameClient extends GameClient {

  constructor(private ws: WebSocketConnection) {
    super();
  }

  drawCards(count: number, to?: Zone, fromBottom?: boolean): void {
    this.ws.send({
      type: 'draw_card',
      to: to ?? 'HAND',
      fromBottom: fromBottom ?? false,
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

  moveCardsToZone(cardIds: number[], newZoneId: number, index: number): void {
    this.ws.send({
      type: 'change_zones',
      cards: cardIds,
      zone: zoneFromIndex(newZoneId)!.type,
      index,
    });
  }

  moveCardToZoneWithIndex(cardId: CardId, newZoneId: number, index: number): void {
    this.ws.send({
      type: 'change_zone',
      card: cardId,
      zone: zoneFromIndex(newZoneId)!.type,
      index,
    });
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

  moveCardsVirtual(ids: string[], zoneId: number, index: number): void {
    this.ws.send({
      type: 'move_virtual',
      ids,
      zone: zoneFromIndex(zoneId)!.type,
      index,
    });
  }

  revealCard(cardId: number, playerId?: string | undefined): void {
    this.ws.send({
      type: 'reveal',
      card: cardId,
      revealTo: playerId ?? null,
      reveal: true,
    });
  }

  unrevealCard(cardId: number, playerId?: string | undefined): void {
    this.ws.send({
      type: 'reveal',
      card: cardId,
      revealTo: playerId ?? null,
      reveal: false,
    });
  }

  scry(count: number): void {
    this.ws.send({
      type: 'scry',
      count,
    });
  }

  createToken(id: OracleId): void {
    this.ws.send({
      type: 'create_token',
      id,
    });
  }

  createCard(id: OracleId): void {
    this.ws.send({
      type: 'create_card',
      id,
    });
  }

  cloneCard(id: CardId): void {
    this.ws.send({
      type: 'clone_card',
      id,
      attributes: {},
    });
  }

  cloneCardWithAttributes(id: CardId, attributes: CardAttributeMap): void {
    this.ws.send({
      type: 'clone_card',
      id,
      attributes,
    });
  }

  sideboard(main: string[], side: string[]): void {
    this.ws.send({
      type: 'sideboard',
      main,
      side,
    });
  }

  getVirtualIds(): Promise<{ [key: string]: string }> {
    return getJson('/game/virtual-ids');
  }
}