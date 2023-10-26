import { reactive } from 'vue'
import { defineStore } from 'pinia'
import { v4 as uuidv4 } from 'uuid';
import { ZONES } from '@/zones';
import type { BoardDiffEvent, Card, ChangeAttributeEvent, ChangeIndexEvent, ChangePlayerAttribute, ChangePositionEvent, ChangeZoneEvent, ScoopDeck, ShuffleDeck } from '@/api/message';


export type Pivot = 'TAPPED' | 'UNTAPPED' | 'UPSIDE_DOWN' | 'LEFT_TAPPED';
export function pivotToAngle(pivot: Pivot) {
  switch (pivot) {
    case 'LEFT_TAPPED':
      return '270deg';
    case 'TAPPED':
      return '90deg';
    case 'UNTAPPED':
      return '0deg';
    case 'UPSIDE_DOWN':
      return '180deg';
    default:
      const _exhaustiveCheck: never = pivot;
      console.error(_exhaustiveCheck);
  }
}
function indexToPivot(index: number): Pivot {
  switch (index) {
    case 0:
      return 'UNTAPPED';
    case 1:
      return 'TAPPED';
    case 2:
      return 'UPSIDE_DOWN';
    case 3:
      return 'LEFT_TAPPED';
    default:
      throw new Error(`Invalid index ${index}. Unable to convert to pivot.`);
  }
}

export type CardId = string;
export type OracleId = string;
export type BoardCard = Card & {
  x: number;
  y: number;
  pivot: Pivot;
  counter: number;
  transformed: boolean;
  zone: number;
  id: CardId;
};

interface PlayerAttributes {
  life: number;
  poison: number;
}

interface HandOrderInfo {
  index: number;
  pos: number;
}

type HandOrder = { [id: CardId]: HandOrderInfo };

function extractZone(cardId: CardId): number {
  return Number(cardId.split('-')[0]);
}

const TestCards: BoardCard[] = [
  {
    name: 'Lightning Bolt',
    image: 'f29ba16f-c8fb-42fe-aabf-87089cb214a7',
    oracleId: 'f29ba16f-c8fb-42fe-aabf-87089cb214a7',
    x: 0,
    y: 0,
    pivot: 'UNTAPPED',
    counter: 0,
    transformed: false,
    zone: 0,
    id: uuidv4()
  },
  {
    name: 'Path to Exile',
    image: '061df0a2-1967-4ddd-84e3-3ecf3af98f6b',
    oracleId: '061df0a2-1967-4ddd-84e3-3ecf3af98f6b',
    x: 0,
    y: 0,
    pivot: 'UNTAPPED',
    counter: 0,
    transformed: false,
    zone: 0,
    id: uuidv4()
  },
  {
    name: 'Serum Visions',
    image: '4bc61952-88ba-447a-835a-f1e9643fcd0d',
    oracleId: '4bc61952-88ba-447a-835a-f1e9643fcd0d',
    x: 0,
    y: 0,
    pivot: 'UNTAPPED',
    counter: 0,
    transformed: false,
    zone: 0,
    id: uuidv4()
  },
]

export const useBoardStore = defineStore('board', () => {
  
  const cards: { [zoneId: number]: BoardCard[] } = reactive({ [ZONES.battlefield.id]: TestCards });
  const oracleInfo = reactive<{ [cardId: CardId]: OracleId }>({});
  const players = reactive<{ [playerId: string]: PlayerAttributes }>({});
  
  function moveCard(zoneId: number, cardId: CardId, x: number, y: number) {
    const cardIndex = findCardIndex(cardId);
    cards[zoneId][cardIndex].x = x;
    cards[zoneId][cardIndex].y = y;
    if (zoneId === ZONES.hand.id) {
      // recalculateHandOrder(cards[ZONES.hand.id], zoneBounds[zoneId]);
    } 

  }

  function moveCardZone(currentZoneId: number, cardId: CardId, newZoneId: number, x: number, y: number) {
    const cardIndex = findCardIndex(cardId);
    const card = cards[currentZoneId].splice(cardIndex, 1)[0];
    cards[newZoneId].push(card);
    card.x = x;
    card.y = y;
    card.zone = newZoneId;
    card.id = uuidv4();

    if (newZoneId === ZONES.hand.id || currentZoneId === ZONES.hand.id) {
      // recalculateHandOrder(cards[ZONES.hand.id], zoneBounds[ZONES.hand.id]);
    }
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
      default:
        const _exhaustiveCheck: never = event;
        console.error(_exhaustiveCheck);
    }
  }

  function processChangeZone(event: ChangeZoneEvent) {
    const oldZoneId = extractZone(event.oldCardId);
    const newZoneId = extractZone(event.cardId);
    const card = cards[oldZoneId].splice(findCardIndex(event.oldCardId), 1)[0];
    cards[newZoneId].push(card);
    card.id = event.cardId;
    card.zone = newZoneId;
    if (event.oldCardId in oracleInfo) {
      oracleInfo[card.id] = oracleInfo[event.oldCardId];
      delete oracleInfo[event.oldCardId];
    }
  }

  function processChangeIndex(event: ChangeIndexEvent) {
    const zoneId = extractZone(event.cardId);
    const cardIndex = findCardIndex(event.cardId);
    const card = cards[zoneId].splice(cardIndex, 1)[0];
    cards[zoneId].splice(event.newIndex, 0, card);
  }

  function processChangePosition(event: ChangePositionEvent) {
    const zoneId = extractZone(event.cardId);
    const cardIndex = findCardIndex(event.cardId);
    const card = cards[zoneId][cardIndex];
    card.x = event.x;
    card.y = event.y;
  }

  function processChangeAttribute(event: ChangeAttributeEvent) {
    const zoneId = extractZone(event.cardId);
    const cardIndex = findCardIndex(event.cardId);
    const card = cards[zoneId][cardIndex];
    switch(event.attribute) {
      case 'PIVOT':
        card.pivot = indexToPivot(event.value);
        break;
      case 'COUNTER':
        card.counter = event.value;
        break;
      case 'TRANSFORMED':
        card.transformed = event.value === 1;
        break;
      default:
        const _exhaustiveCheck: never = event.attribute;
        console.error(_exhaustiveCheck);
    }
  }

  function processChangePlayerAttribute(event: ChangePlayerAttribute) {
    const player = players[event.playerId];
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
    for(const [zone, cardList] of Object.entries(cards)) {
      cards[Number(zone)] = [];
      deck.push(...cardList);
    }

    for(let i = 0; i < deck.length; i++) {
      const card = deck[i];
      if (card.id in oracleInfo) {
        delete oracleInfo[card.id];
      }
      resetCard(card);
      card.id = event.newIds[i];
      card.zone = ZONES.library.id;
    }

    cards[ZONES.library.id] = deck;
  }

  function processShuffleDeck(event: ShuffleDeck) {
    const deck = cards[ZONES.library.id];
    for(let i = 0; i < deck.length; i++) {
      const card = deck[i];
      if (card.id in oracleInfo) {
        delete oracleInfo[card.id];
      }
      resetCard(card);
      card.id = event.newIds[i];
    }
  }

  function resetCard(card: BoardCard) {
    card.pivot = 'UNTAPPED';
    card.counter = 0;
    card.transformed = false;
    card.x = 0;
    card.y = 0;
    card.zone = ZONES.library.id;

  }

  function processOracleInfo(newOracleInfo: { [cardId: CardId]: string }) {
    Object.assign(oracleInfo, newOracleInfo);
  }

  function findCardIndex(cardId: CardId): number {
    const zone = extractZone(cardId);
    const cardIndex = cards[zone].findIndex(card => card.id === cardId);
    return cardIndex;
  }

  return { processBoardUpdate, processOracleInfo, cards, moveCard, moveCardZone }
});

function recalculateHandOrder(handCards: BoardCard[], handBounds: DOMRect) {
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
}``