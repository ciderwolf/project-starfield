import { zoneFromIndex, type ZoneConfig, ZONES, getZonesMap, ScreenPosition } from "@/zones";
import { defineStore } from "pinia";
import { computed, reactive, ref } from "vue";
import { useBoardStore, type PlayerAttributes } from "./board";

export const useZoneStore = defineStore('zone', () => {
  const zoneBounds: { [id: number]: DOMRect } = reactive({});

  const primaryPlayer = ref<PlayerAttributes | null>(null);
  const secondaryPlayer = ref<PlayerAttributes | null>(null);
  const availablePlayers = computed(() => [primaryPlayer.value, secondaryPlayer.value].filter((p) => p !== null) as PlayerAttributes[]);

  const opponentHand = computed(() => opponentZones.value?.hand?.id);
  const opponentZones = computed(() => getZonesMap(ScreenPosition.SECONDARY, secondaryPlayer.value?.index));

  function overlappingZone(x: number, y: number): ZoneConfig | null {
    for (const [id, bounds] of Object.entries(zoneBounds)) {
      if (bounds && bounds.left < x && x < bounds.right && bounds.top < y && y < bounds.bottom) {
        // count the opponent's battlefield as the same as ours
        if (Number(id) === opponentZones.value.play.id) {
          return ZONES.play;
        }
        return zoneFromIndex(Number(id)) ?? null;
      }
    }
    return null;
  }

  function pointInZone(zoneId: number, x: number, y: number): { x: number, y: number } {
    const zone = zoneBounds[zoneId];
    return {
      x: (x - zone.left) / zone.width,
      y: (y - zone.top) / zone.height,
    };
  }

  function updateZoneBounds(id: number, bounds: DOMRect) {
    zoneBounds[id] = bounds;
    const board = useBoardStore();
    if (id === ZONES.hand.id || id === opponentHand.value) {
      board.updateHandPos(id, bounds);
    }
  }

  function playFieldExtents(): { left: number, top: number, bottom: number, right: number } {
    const play = zoneBounds[ZONES.play.id];
    const opponentPlay = zoneBounds[opponentZones.value.play.id];
    return {
      left: Math.min(play.left, opponentPlay?.left),
      top: Math.min(play.top, opponentPlay?.top),
      bottom: Math.max(play.bottom, opponentPlay?.bottom),
      right: Math.max(play.right, opponentPlay?.right),
    };
  }

  function resetState() {
    primaryPlayer.value = null;
    secondaryPlayer.value = null;
  }



  return { zoneBounds, primaryPlayer, secondaryPlayer, availablePlayers, opponentZones, opponentHand, overlappingZone, pointInZone, updateZoneBounds, playFieldExtents, resetState };
})