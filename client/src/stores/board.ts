import { reactive } from 'vue'
import { defineStore } from 'pinia'
import { type ZoneConfig, zoneFromIndex } from '@/zones';
import type { Card } from '@/api/message';
import { v4 as uuidv4 } from 'uuid';

export type BoardCard = Card & {
  x: number;
  y: number;
  pivot: 'TAPPED' | 'UNTAPPED' | 'UPSIDE_DOWN' | 'LEFT_TAPPED';
  counter: number;
  transformed: boolean;
  zone: number;
  key: string;
};

export const useBoardStore = defineStore('board', () => {
  
  const zoneBounds: { [id: number]: DOMRect } = reactive({});
  const cards: { [zoneId: number]: BoardCard[] } = reactive({
    0: [
      {
        name: 'Lightning Bolt',
        image: 'f29ba16f-c8fb-42fe-aabf-87089cb214a7',
        id: 'f29ba16f-c8fb-42fe-aabf-87089cb214a7',
        x: 0,
        y: 0,
        pivot: 'UNTAPPED',
        counter: 0,
        transformed: false,
        zone: 0,
        key: uuidv4()
      },
      {
        name: 'Path to Exile',
        image: '061df0a2-1967-4ddd-84e3-3ecf3af98f6b',
        id: '061df0a2-1967-4ddd-84e3-3ecf3af98f6b',
        x: 0,
        y: 0,
        pivot: 'UNTAPPED',
        counter: 0,
        transformed: false,
        zone: 0,
        key: uuidv4()
      },
      {
        name: 'Serum Visions',
        image: '4bc61952-88ba-447a-835a-f1e9643fcd0d',
        id: '4bc61952-88ba-447a-835a-f1e9643fcd0d',
        x: 0,
        y: 0,
        pivot: 'UNTAPPED',
        counter: 0,
        transformed: false,
        zone: 0,
        key: uuidv4()
      },
    ]
  });

  function overlappingZone(x: number, y: number): ZoneConfig | null {
    for (const [id, bounds] of Object.entries(zoneBounds)) {
      if (bounds.left < x && x < bounds.right && bounds.top < y && y < bounds.bottom) {
        return zoneFromIndex(Number(id)) ?? null;
      }
    }
    return null;
  }

  function pointInZone(zoneId: number, x: number, y: number): {x: number, y: number} {
    const zone = zoneBounds[zoneId];
    return {
      x: (x - zone.left) / zone.width,
      y: (y - zone.top) / zone.height,
    };
  }

  function updateZoneBounds(id: number, bounds: DOMRect) {
    zoneBounds[id] = bounds;
  }
  
  function moveCard(zoneId: number, cardId: number, x: number, y: number) {
    cards[zoneId][cardId].x = x;
    cards[zoneId][cardId].y = y;
  }

  function moveCardZone(currentZoneId: number, cardId: number, newZoneId: number, x: number, y: number) {
    const card = cards[currentZoneId].splice(cardId, 1)[0];
    cards[newZoneId].push(card);
    card.x = x;
    card.y = y;
    card.zone = newZoneId;
    card.key = uuidv4();
  }

  return { cards, moveCard, moveCardZone, zoneBounds, updateZoneBounds, overlappingZone, pointInZone }
})

