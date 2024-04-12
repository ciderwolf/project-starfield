import { reactive } from 'vue'
import { defineStore } from 'pinia'
import { ScreenPosition, ZONES, OPPONENT_ZONES, findZoneByName, getZones, zoneNameToId } from '@/zones';
import type { BoardDiffEvent, BoardCard, ChangeAttributeEvent, ChangeIndexEvent, ChangePlayerAttribute, ChangePositionEvent, ChangeZoneEvent, PlayerState, ScoopDeck, ShuffleDeck, Zone, OracleCard, CreateCard, DestroyCard } from '@/api/message';
import { useZoneStore } from './zone';
import { useDataStore } from './data';


export enum Pivot {
  UNTAPPED,
  TAPPED,
  LEFT_TAPPED,
  UPSIDE_DOWN,
}

export function nonRotatedRect(rect: DOMRect, pivot: Pivot) {
  if (pivot === Pivot.LEFT_TAPPED || pivot === Pivot.TAPPED) {
    return new DOMRect(rect.x, rect.y, rect.height, rect.width);
  } else {
    return rect;
  }
}

export function pivotToAngle(pivot: Pivot) {
  switch (pivot) {
    case Pivot.LEFT_TAPPED:
      return '270deg';
    case Pivot.TAPPED:
      return '90deg';
    case Pivot.UNTAPPED:
      return '0deg';
    case Pivot.UPSIDE_DOWN:
      return '180deg';
    default:
      const _exhaustiveCheck: never = pivot;
      console.error(_exhaustiveCheck);
  }
}
function indexToPivot(index: number): Pivot {
  return index as Pivot;
}

export type CardId = number;
export type OracleId = string;

export interface PlayerAttributes {
  life: number;
  poison: number;
  id: string;
  name: string;
  index: number;
}

interface HandOrderInfo {
  index: number;
  pos: number;
}

type HandOrder = { [id: CardId]: HandOrderInfo };

function extractPlayerIndex(cardId: CardId): number {
  // extract the first four bits and return the corresponding player index
  return cardId & 0b00001111;
}

export const useBoardStore = defineStore('board', () => {
  
  const cards: { [zoneId: number]: BoardCard[] } = reactive({  });
  const cardToOracleId = reactive<{ [cardId: CardId]: OracleId }>({});
  const oracleInfo = reactive<{ [oracleId: OracleId]: OracleCard }>({});
  const players = reactive<{ [playerId: string]: PlayerAttributes }>({});
  const zones = useZoneStore();
  
  function moveCard(zoneId: number, cardId: CardId, x: number, y: number) {
    const cardIndex = findCardIndex(cardId);
    cards[zoneId][cardIndex].x = x;
    cards[zoneId][cardIndex].y = y;

    const handId = getZoneId(cardId, 'HAND');
    if (zoneId === handId) {
      recalculateHandOrder(cards[handId], zones.zoneBounds[handId]);
    }
  }

  function setBoardState(playerStates: PlayerState[]) {
    let index = 0;
    for(const state of playerStates) {
      players[state.id] = {
        life: state.life,
        poison: state.poison,
        id: state.id,
        name: state.name,
        index
      }
      index += 1;
    }

    for(const state of playerStates) {
      const pos = getScreenPosition(players[state.id].index);
      for(const [zoneName, cardList] of Object.entries(state.board)) {
        const zone = findZoneByName(zoneName, pos);
        // reserialize Zone name as id number
        if (cardList.length > 0) {
          cardList.forEach(card => card.zone = zoneNameToId(card.zone as unknown as Zone, pos))  
        }
        if (zone) {
          cards[zone.id] = cardList;
        }
      }

      // overwrite oracleInfo from message
      for(const [cardId, oracleId] of Object.entries(state.cardToOracleId)) {
        cardToOracleId[Number(cardId)] = oracleId;
      }

      // overwrite oracleInfo from message
      for(const [oracleId, card] of Object.entries(state.oracleInfo)) {
        oracleInfo[oracleId] = card;
      }
    }
    
    recalculateHandOrder(cards[ZONES.hand.id], zones.zoneBounds[ZONES.hand.id]);
    recalculateHandOrder(cards[OPPONENT_ZONES.hand.id], zones.zoneBounds[OPPONENT_ZONES.hand.id]);
  }

  function processBoardUpdate(events: BoardDiffEvent[]) {
    for(const event of events) {
      processBoardEvent(event);
    }
  }

  function processBoardEvent(event: BoardDiffEvent) {
    switch(event.type) {
      case 'change_zone':
        processChangeZone(event);
        break;
      case 'change_index':
        processChangeIndex(event);
        break;
      case 'change_position':
        processChangePosition(event);
        break;
      case 'change_attribute':
        processChangeAttribute(event);
        break;
      case 'change_player_attribute':
        processChangePlayerAttribute(event);
        break;
      case 'scoop_deck':
        processScoopDeck(event);
        break;
      case 'shuffle_deck':
        processShuffleDeck(event);
        break;
      case 'reveal_card':
      case 'hide_card':
        break;
      case 'create_card':
        processCreateCard(event);
        break;
      case 'destroy_card':
        processDestroyCard(event);
        break;
      default:
        const _exhaustiveCheck: never = event;
        console.error(_exhaustiveCheck);
    }
  }

  function processChangeZone(event: ChangeZoneEvent) {
    const oldZoneId = extractZone(event.oldCardId);
    const newZoneId = extractZone(event.card);
    const card = cards[oldZoneId].splice(findCardIndex(event.oldCardId), 1)[0];
    cards[newZoneId].push(card);
    resetCardAttributes(card);
    card.id = event.card;
    card.zone = newZoneId;
    if (event.oldCardId in cardToOracleId) {
      cardToOracleId[card.id] = cardToOracleId[event.oldCardId];
      delete cardToOracleId[event.oldCardId];
    }
    
    const handId = getZoneId(event.card, 'HAND');
    if (newZoneId === handId || oldZoneId === handId) {
      card.x = 1;
      recalculateHandOrder(cards[handId], zones.zoneBounds[handId]);
    }
  }

  function processChangeIndex(event: ChangeIndexEvent) {
    const zoneId = extractZone(event.card);
    const cardIndex = findCardIndex(event.card);
    const card = cards[zoneId].splice(cardIndex, 1)[0];
    cards[zoneId].splice(event.newIndex, 0, card);
  }

  function processChangePosition(event: ChangePositionEvent) {
    const zoneId = extractZone(event.card);
    const cardIndex = findCardIndex(event.card);
    const card = cards[zoneId][cardIndex];
    card.x = event.x;
    card.y = event.y;
  }

  function processChangeAttribute(event: ChangeAttributeEvent) {
    const zoneId = extractZone(event.card);
    const cardIndex = findCardIndex(event.card);
    const card = cards[zoneId][cardIndex];
    switch(event.attribute) {
      case 'PIVOT':
        card.pivot = indexToPivot(event.newValue);
        break;
      case 'COUNTER':
        card.counter = event.newValue;
        break;
      case 'TRANSFORMED':
        card.transformed = event.newValue === 1;
        break;
      case 'FLIPPED':
        card.flipped = event.newValue === 1;
        break;
      default:
        const _exhaustiveCheck: never = event.attribute;
        console.error(_exhaustiveCheck);
    }
  }

  function processChangePlayerAttribute(event: ChangePlayerAttribute) {
    const player = players[event.player];
    switch(event.attribute) {
      case 'LIFE':
        player.life = event.newValue;
        break;
      case 'POISON':
        player.poison = event.newValue;
        break;
      default:
        const _exhaustiveCheck: never = event.attribute;
        console.error(_exhaustiveCheck);
    }
  }

  function processScoopDeck(event: ScoopDeck) {
    const deck: BoardCard[] = [];
    const player = players[event.playerId];
    const pos = getScreenPosition(player.index);
    const zones = getZones(pos);
    const libraryId = zones.find(z => z.type === 'LIBRARY')!.id;
    for(const zone of zones) {
      if (zone.type === 'SIDEBOARD') {
        continue;
      }
      const zoneId = zone.id;
      const cardList = cards[zoneId];
      cards[zoneId] = [];
      deck.push(...cardList);
    }

    for(let i = 0; i < deck.length; i++) {
      const card = deck[i];
      if (card.id in cardToOracleId) {
        delete cardToOracleId[card.id];
      }
      resetCard(card);
      card.id = event.newIds[i];
      card.zone = libraryId;
    }

    cards[libraryId] = deck;
  }

  function processShuffleDeck(event: ShuffleDeck) {
    const player = players[event.playerId];
    const libraryId = getZones(getScreenPosition(player.index))
      .find(z => z.type === 'LIBRARY')!.id;
    const deck = cards[libraryId];
    for(let i = 0; i < deck.length; i++) {
      const card = deck[i];
      if (card.id in cardToOracleId) {
        delete cardToOracleId[card.id];
      }
      resetCard(card);
      card.id = event.newIds[i];
    }
  }

  function processCreateCard(event: CreateCard) {
    const card = event.state;
    card.zone = getZoneId(card.id, card.zone as unknown as Zone);
    const cardList = cards[card.zone];
    cardList.splice(card.index, 0, card);
  }

  function processDestroyCard(event: DestroyCard) {
    const card = event.card;
    const zoneId = extractZone(card);
    const cardList = cards[zoneId];
    const index = cardList.findIndex(c => c.id === card);
    cardList.splice(index, 1);
  }

  function resetCard(card: BoardCard) {
    resetCardAttributes(card);
    card.x = 0;
    card.y = 0;
    card.zone = getZoneId(card.id, 'LIBRARY');
  }

  function resetCardAttributes(card: BoardCard) {
    card.pivot = Pivot.UNTAPPED;
    card.counter = 0;
    card.transformed = false;
    card.flipped = false;
  }

  function processOracleInfo(newCards: { [cardId: CardId]: string }, newOracleInfo: { [oracleId: OracleId]: OracleCard }, cardsToHide: CardId[]) {
    Object.assign(cardToOracleId, newCards);
    Object.assign(oracleInfo, newOracleInfo);

    for (const cardId of cardsToHide) {
      delete cardToOracleId[cardId];
    }
  }

  function cardIsMovable(card: CardId): boolean {
    const player = extractPlayerIndex(card);
    return playerIsMovable(player);
  }

  function playerIsMovable(playerIndex: number) {
    const data = useDataStore();
    const myAttributes = players[data.userId!];
    if (myAttributes && myAttributes.index === playerIndex) {
      return true;
    } else {
      return false;
    }
  }

  function zoneIsMovable(zoneId: number): boolean {
    const pos = zoneId < 0 ? ScreenPosition.SECONDARY : ScreenPosition.PRIMARY;
    const data = useDataStore();
    return pos == ScreenPosition.PRIMARY && data.userId! in players;
  }

  function updateHandPos(id: number, bounds: DOMRect) {
    const handCards = cards[id];
    if (!handCards) return;
    recalculateHandOrder(handCards, bounds);
  }

  function findCardIndex(cardId: CardId): number {
    const zone = extractZone(cardId);
    const cardIndex = cards[zone].findIndex(card => card.id === cardId);
    return cardIndex;
  }

  function extractZone(cardId: CardId): number {
    // extract the second four bits and return the corresponding zone id
    const playerIndex = extractPlayerIndex(cardId);
    const pos = getScreenPosition(playerIndex);
    const zoneIndex = (cardId & 0b11110000) >> 4;
    if (pos === ScreenPosition.PRIMARY) {
      return zoneIndex;
    } else if (pos === ScreenPosition.SECONDARY) {
      return -(zoneIndex + 1);
    } else {
      const _exhaustiveCheck: never = pos;
      throw new Error(_exhaustiveCheck);
    }
  }

  function getScreenPosition(playerIndex: number): ScreenPosition {
    const data = useDataStore();
    const myAttributes = players[data.userId!];
    if (myAttributes) {
      if (myAttributes.index === 0) {
        return playerIndex === 0 ? ScreenPosition.PRIMARY : ScreenPosition.SECONDARY;
      } else {
        return playerIndex === 0 ? ScreenPosition.SECONDARY : ScreenPosition.PRIMARY;
      }
    } else {
      return playerIndex as ScreenPosition;
    }
  }

  function getScreenPositionFromCard(cardId: CardId): ScreenPosition {
    const playerIndex = extractPlayerIndex(cardId);
    return getScreenPosition(playerIndex);
  }

  function getZoneId(cardId: CardId, zone: Zone): number {
    const playerIndex = extractPlayerIndex(cardId);
    const pos = getScreenPosition(playerIndex);
    return zoneNameToId(zone, pos);
  }

  return { setBoardState, processBoardUpdate, processOracleInfo, cardToOracleId, oracleInfo, updateHandPos, cards, moveCard, cardIsMovable, zoneIsMovable, playerIsMovable, getScreenPositionFromCard, getScreenPositionFromPlayerIndex: getScreenPosition, players }
});

function recalculateHandOrder(handCards: BoardCard[], handBounds: DOMRect) {
  if (!handBounds) return;
  // assign indices based on pos order, and then recalculate pos based on index
  const handOrder: HandOrder = {};
  for (const card of handCards) {
    handOrder[card.id] = {
      index: 0,
      pos: card.x,
    };
  }

  const cardWidth = 78;
  const cards = Object.values(handOrder).sort((a, b) => a.pos - b.pos);
  const totalWidth = cards.length * cardWidth;

  const virtualCardWidth = cardWidth / handBounds.width;
  const virtualOffset = (handBounds.width - totalWidth) / (2 * handBounds.width) + 0.5 * virtualCardWidth;;

  for (let i = 0; i < cards.length; i++) {
    cards[i].index = i;
    cards[i].pos = virtualOffset + i * virtualCardWidth;
  }

  for(const card of handCards) {
    card.x = handOrder[card.id].pos;
  }
}