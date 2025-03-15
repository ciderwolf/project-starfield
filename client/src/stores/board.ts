import { computed, reactive, ref } from 'vue'
import { defineStore } from 'pinia'
import { ScreenPosition, ZONES, getZoneIdFromScreenPositionAndPlayerIndex, getZones, isAnyOpponentZone, zoneNameToId } from '@/zones';
import { type BoardDiffEvent, type BoardCard, type ChangeAttributeEvent, type ChangeIndexEvent, type ChangePlayerAttribute, type ChangePositionEvent, type ChangeZoneEvent, type PlayerState, type ScoopDeck, type ShuffleDeck, type Zone, type OracleCard, type CreateCard, type DestroyCard, type LogInfoMessage, type RevealCard, type HideCard, Highlight, type SpectatorJoin, type SpectatorLeave, type UserState } from '@/api/message';
import { useZoneStore } from './zone';
import { useDataStore } from './data';
import { getLogMessage, getEventMessage, type EventMessage } from '@/logs';
import { resetReactive } from '.';

export enum UserType {
  PLAYER,
  OPPONENT,
  SPECTATOR,
}

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

export function extractPlayerIndex(cardId: CardId): number {
  // extract the first four bits and return the corresponding player index
  return cardId & 0b00001111;
}


export const useBoardStore = defineStore('board', () => {

  const cards: { [zoneId: number]: BoardCard[] } = reactive({});
  const cardToOracleId = reactive<{ [cardId: CardId]: OracleId }>({});
  const oracleInfo = reactive<{ [oracleId: OracleId]: OracleCard }>({});
  const players = reactive<{ [playerId: string]: PlayerAttributes }>({});
  const spectators = reactive<{ [userId: string]: UserState }>({});
  const currentPlayer = ref(0);
  const logs = ref<EventMessage[]>([]);
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

  function resetState() {
    for (const zone in cards) {
      cards[zone] = [];
    }
    resetReactive(cardToOracleId);
    resetReactive(oracleInfo);
    resetReactive(players);
    resetReactive(spectators);
    currentPlayer.value = 0;
    logs.value = [];
    zones.resetState();
  }

  function setBoardState(playerStates: PlayerState[], currentPlayerIdx: number, gameOracleInfo: { [oracleId: OracleId]: OracleCard }) {
    resetState();

    let index = 0;
    currentPlayer.value = currentPlayerIdx;
    for (const state of playerStates) {
      players[state.id] = {
        life: state.life,
        poison: state.poison,
        id: state.id,
        name: state.name,
        index
      }
      index += 1;
    }

    for (const state of playerStates) {
      const index = players[state.id].index;
      const pos = getScreenPosition(index);
      if (pos == ScreenPosition.PRIMARY) {
        zones.primaryPlayer = players[state.id];
      } else if (pos == ScreenPosition.SECONDARY && zones.secondaryPlayer == null) {
        zones.secondaryPlayer = players[state.id];
      }
      for (const [zoneName, cardList] of Object.entries(state.board)) {
        const zoneId = zoneNameToId(zoneName as unknown as Zone, pos, index)
        // reserialize Zone name as id number
        if (cardList.length > 0) {
          cardList.forEach(card => card.zone = zoneId)
        }
        cards[zoneId] = cardList;
      }

      // overwrite oracleInfo from message
      for (const [cardId, oracleId] of Object.entries(state.cardToOracleId)) {
        cardToOracleId[Number(cardId)] = oracleId;
      }
    }
    // overwrite oracleInfo from message
    for (const [oracleId, card] of Object.entries(gameOracleInfo)) {
      oracleInfo[oracleId] = card;
    }

    recalculateHandOrder(cards[ZONES.hand.id], zones.zoneBounds[ZONES.hand.id]);
    recalculateHandOrder(cards[zones.opponentHand], zones.zoneBounds[zones.opponentHand]);
  }

  function processBoardUpdate(events: BoardDiffEvent[]) {
    // sort events so that reveal and hide events are last
    events.sort((a, b) => {
      if (a.type === 'reveal_card' || a.type === 'hide_card') {
        return 1;
      } else if (b.type === 'reveal_card' || b.type === 'hide_card') {
        return -1;
      } else {
        return 0;
      }
    });

    for (const event of events) {
      processBoardEvent(event);
      const logMessage = getEventMessage(event);
      if (logMessage) {
        logs.value.push(logMessage);
      }
    }
  }

  function processBoardEvent(event: BoardDiffEvent) {
    switch (event.type) {
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
        processRevealCard(event);
        break;
      case 'hide_card':
        processHideCard(event);
        break;
      case 'create_card':
        processCreateCard(event);
        break;
      case 'destroy_card':
        processDestroyCard(event);
        break;
      case 'spectator_join':
        processSpectatorJoin(event);
        break;
      case 'spectator_leave':
        processSpectatorLeave(event);
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
      recalculateHandOrder(cards[handId], zones.zoneBounds[handId]);
    } else {
      // reset a card's position when it moves zones, except if it was moved to hand.
      card.x = 0;
      card.y = 0;
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

    if (zoneId === getZoneId(event.card, 'HAND')) {
      recalculateHandOrder(cards[zoneId], zones.zoneBounds[zoneId]);
    }
  }

  function processChangeAttribute(event: ChangeAttributeEvent) {
    const zoneId = extractZone(event.card);
    const cardIndex = findCardIndex(event.card);
    const card = cards[zoneId][cardIndex];
    switch (event.attribute) {
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
    switch (event.attribute) {
      case 'LIFE':
        player.life = event.newValue;
        break;
      case 'POISON':
        player.poison = event.newValue;
        break;
      case 'ACTIVE_PLAYER':
        if (event.newValue === 1) {
          currentPlayer.value = player.index;
        }
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
    const zones = getZones(pos, player.index);
    const libraryId = zones.find(z => z.type === 'LIBRARY')!.id;
    for (const zone of zones) {
      if (zone.type === 'SIDEBOARD') {
        continue;
      }
      const zoneId = zone.id;
      const cardList = cards[zoneId];
      cards[zoneId] = [];
      deck.push(...cardList);
    }

    for (let i = 0; i < deck.length; i++) {
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
    const libraryId = getZones(getScreenPosition(player.index), player.index)
      .find(z => z.type === 'LIBRARY')!.id;
    const deck = cards[libraryId];
    for (let i = 0; i < deck.length; i++) {
      const card = deck[i];
      if (card.id in cardToOracleId) {
        delete cardToOracleId[card.id];
      }
      resetCard(card);
      card.id = event.newIds[i];
    }
  }

  function processRevealCard(event: RevealCard) {
    const zone = extractZone(event.card);
    const cardIndex = findCardIndex(event.card);
    const card = cards[zone][cardIndex];

    const revealTo = event.players.length === 0
      ? Object.keys(players)
      : event.players;

    for (const player of revealTo) {
      if (!card.visibility.includes(player)) {
        card.visibility.push(player);
      }
    }
  }

  function processHideCard(event: HideCard) {
    const zone = extractZone(event.card);
    const cardIndex = findCardIndex(event.card);
    const card = cards[zone][cardIndex];

    const revealTo = event.players.length === 0
      ? Object.keys(players)
      : event.players;

    for (const player of revealTo) {
      const index = card.visibility.indexOf(player);
      if (index >= 0) {
        card.visibility.splice(index, 1);
      }
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
    card.visibility = [];
    card.zone = getZoneId(card.id, 'LIBRARY');
  }

  function resetCardAttributes(card: BoardCard) {
    card.pivot = Pivot.UNTAPPED;
    card.counter = 0;
    card.transformed = false;
    card.flipped = false;

    card.highlight = Highlight.NONE;
  }

  function processSpectatorJoin(message: SpectatorJoin) {
    spectators[message.user.id] = message.user;
  }

  function processSpectatorLeave(message: SpectatorLeave) {
    delete spectators[message.user];
  }

  function processOracleInfo(newCards: { [cardId: CardId]: string }, newOracleInfo: { [oracleId: OracleId]: OracleCard }, cardsToHide: CardId[]) {
    Object.assign(cardToOracleId, newCards);
    Object.assign(oracleInfo, newOracleInfo);

    for (const cardId of cardsToHide) {
      delete cardToOracleId[cardId];
    }
  }

  function processLog(message: LogInfoMessage, owner: string) {
    const name = players[owner].name;
    logs.value.push({ message: getLogMessage(message, name), owner })
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
    const pos = isAnyOpponentZone(zoneId) ? ScreenPosition.SECONDARY : ScreenPosition.PRIMARY;
    const data = useDataStore();
    return pos == ScreenPosition.PRIMARY && data.userId! in players;
  }

  function zoneUserType(zoneId: number): UserType {
    const pos = isAnyOpponentZone(zoneId) ? ScreenPosition.SECONDARY : ScreenPosition.PRIMARY;
    const data = useDataStore();
    if (pos == ScreenPosition.PRIMARY && data.userId! in players) {
      return UserType.PLAYER;
    } else if (pos == ScreenPosition.SECONDARY && data.userId! in players) {
      return UserType.OPPONENT;
    } else {
      return UserType.SPECTATOR;
    }
  }

  function updateHandPos(id: number, bounds: DOMRect) {
    const handCards = cards[id];
    if (!handCards) return;
    recalculateHandOrder(handCards, bounds);
  }

  function highlightCard(cardId: CardId, highlight: Highlight) {
    const zoneId = extractZone(cardId);
    const cardIndex = findCardIndex(cardId);
    const card = cards[zoneId][cardIndex];
    if (card) {
      card.highlight = highlight;
    }
  }

  function clearHighlight(cardId: CardId) {
    const zoneId = extractZone(cardId);
    const cardIndex = findCardIndex(cardId);
    const card = cards[zoneId][cardIndex];
    if (card) {
      card.highlight = Highlight.NONE;
    }
  }

  const selectedCards = computed(() => {
    const selected = cards[ZONES.play.id].filter(card => card.highlight === Highlight.SELECTED);
    return selected;
  });

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
    return getZoneIdFromScreenPositionAndPlayerIndex(zoneIndex, pos, playerIndex);
  }

  function getScreenPosition(playerIndex: number): ScreenPosition {
    const data = useDataStore();
    const myAttributes = players[data.userId!];
    if (myAttributes) {
      return playerIndex === myAttributes.index ? ScreenPosition.PRIMARY : ScreenPosition.SECONDARY;
    } else {
      return playerIndex === 0 ? ScreenPosition.PRIMARY : ScreenPosition.SECONDARY;
    }
  }

  function getScreenPositionFromCard(cardId: CardId): ScreenPosition {
    const playerIndex = extractPlayerIndex(cardId);
    return getScreenPosition(playerIndex);
  }

  function getZoneId(cardId: CardId, zone: Zone): number {
    const playerIndex = extractPlayerIndex(cardId);
    const pos = getScreenPosition(playerIndex);
    return zoneNameToId(zone, pos, playerIndex);
  }

  return { setBoardState, processBoardUpdate, processOracleInfo, processLog, cardToOracleId, oracleInfo, updateHandPos, cards, moveCard, cardIsMovable, zoneIsMovable, playerIsMovable, getScreenPositionFromCard, getScreenPositionFromPlayerIndex: getScreenPosition, zoneUserType, players, spectators, logs, highlightCard, clearHighlight, selectedCards, currentPlayer }
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

  for (const card of handCards) {
    card.x = handOrder[card.id].pos;
  }
}