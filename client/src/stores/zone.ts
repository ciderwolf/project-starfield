import { zoneFromIndex, type ZoneConfig, ZONES } from "@/zones";
import { defineStore } from "pinia";
import { reactive } from "vue";

export const useZoneStore = defineStore('zone', () => {
  const zoneBounds: { [id: number]: DOMRect } = reactive({});

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
    if (id === ZONES.hand.id) {
      // recalculateHandOrder(cards[ZONES.hand.id], bounds);
    }
  }

  return { zoneBounds, overlappingZone, pointInZone, updateZoneBounds };
})