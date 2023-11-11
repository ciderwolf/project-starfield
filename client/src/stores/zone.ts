import { zoneFromIndex, type ZoneConfig, ZONES, OPPONENT_ZONES } from "@/zones";
import { defineStore } from "pinia";
import { reactive } from "vue";
import { useBoardStore } from "./board";

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
    const board = useBoardStore();
    if (id === ZONES.hand.id || id === OPPONENT_ZONES.hand.id) {
      board.updateHandPos(id, bounds);
    }
  }

  return { zoneBounds, overlappingZone, pointInZone, updateZoneBounds };
})