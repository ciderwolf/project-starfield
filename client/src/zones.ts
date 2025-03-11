import type { Zone } from "./api/message";

const Z_WIDTH = '78px';
const Z_HEIGHT = '108px';
const GAME_LOG_WIDTH = '250px';

type ZoneFlex = 'free' | 'stack' | 'hstack';

export interface ZoneConfig {
  pos: {
    left?: string;
    right?: string;
    top?: string;
    bottom?: string
    width: string;
    height: string;
  },
  layout: ZoneFlex,
  type: Zone,
  name: string;
  id: number,
  reveal: [boolean, boolean]
}

export const OPPONENT_ZONES: { [name: string]: ZoneConfig } = {
  library: {
    pos: {
      right: `calc(${GAME_LOG_WIDTH} - ${Z_WIDTH})`,
      top: '0',
      width: `${Z_WIDTH}`,
      height: `${Z_HEIGHT}`,
    },
    layout: 'stack',
    type: 'LIBRARY',
    name: 'Library',
    id: -1,
    reveal: [false, false]
  },
  hand: {
    pos: {
      left: `${Z_WIDTH}`,
      top: '0px',
      width: `calc(100% - ${Z_WIDTH} - ${GAME_LOG_WIDTH})`,
      height: `${Z_HEIGHT}`,
    },
    layout: 'hstack',
    type: 'HAND',
    name: 'Hand',
    id: -2,
    reveal: [true, false]
  },
  play: {
    pos: {
      left: `${Z_WIDTH}`,
      top: `${Z_HEIGHT}`,
      width: `calc(100% - ${GAME_LOG_WIDTH} - ${Z_WIDTH})`,
      height: `calc(50vh - ${Z_HEIGHT})`,
    },
    layout: 'free',
    type: 'BATTLEFIELD',
    name: 'Battlefield',
    id: -3,
    reveal: [true, true]
  },
  graveyard: {
    pos: {
      left: '0',
      top: '0px',
      width: `${Z_WIDTH}`,
      height: `${Z_HEIGHT}`,
    },
    layout: 'stack',
    type: 'GRAVEYARD',
    name: 'Graveyard',
    id: -4,
    reveal: [true, true]
  },
  exile: {
    pos: {
      left: '0',
      top: `${Z_HEIGHT}`,
      width: `${Z_WIDTH}`,
      height: `${Z_HEIGHT}`,
    },
    layout: 'stack',
    type: 'EXILE',
    name: 'Exile',
    id: -5,
    reveal: [true, true]
  },
  faceDown: {
    pos: {
      left: '0',
      top: `calc(2 * ${Z_HEIGHT})`,
      width: `${Z_WIDTH}`,
      height: `${Z_HEIGHT}`,
    },
    layout: 'stack',
    type: 'FACE_DOWN',
    name: 'Face Down',
    id: -6,
    reveal: [false, false]
  },
  sideboard: {
    pos: {
      width: '0',
      height: '0'
    },
    layout: 'stack',
    type: 'SIDEBOARD',
    name: 'Sideboard',
    id: -7,
    reveal: [true, false]
  }
};

export const ZONES: { [name: string]: ZoneConfig } = {
  library: {
    pos: {
      right: `calc(${GAME_LOG_WIDTH} - ${Z_WIDTH})`,
      bottom: '0',
      width: `${Z_WIDTH}`,
      height: `${Z_HEIGHT}`,
    },
    layout: 'stack',
    type: 'LIBRARY',
    name: 'Library',
    id: 0,
    reveal: [false, false]
  },
  hand: {
    pos: {
      left: `${Z_WIDTH}`,
      bottom: '0px',
      width: `calc(100% - ${Z_WIDTH} - ${GAME_LOG_WIDTH})`,
      height: `${Z_HEIGHT}`,
    },
    layout: 'hstack',
    type: 'HAND',
    name: 'Hand',
    id: 1,
    reveal: [true, false]
  },
  play: {
    pos: {
      left: `${Z_WIDTH}`,
      top: '50vh',
      width: `calc(100% - ${GAME_LOG_WIDTH} - ${Z_WIDTH})`,
      height: `calc(50% - ${Z_HEIGHT})`,
    },
    layout: 'free',
    type: 'BATTLEFIELD',
    name: 'Battlefield',
    id: 2,
    reveal: [true, true]
  },
  graveyard: {
    pos: {
      left: '0',
      bottom: '0px',
      width: `${Z_WIDTH}`,
      height: `${Z_HEIGHT}`,
    },
    layout: 'stack',
    type: 'GRAVEYARD',
    name: 'Graveyard',
    id: 3,
    reveal: [true, true]
  },
  exile: {
    pos: {
      left: '0',
      bottom: `${Z_HEIGHT}`,
      width: `${Z_WIDTH}`,
      height: `${Z_HEIGHT}`,
    },
    layout: 'stack',
    type: 'EXILE',
    name: 'Exile',
    id: 4,
    reveal: [true, true]
  },
  faceDown: {
    pos: {
      left: '0',
      bottom: `calc(2 * ${Z_HEIGHT})`,
      width: `${Z_WIDTH}`,
      height: `${Z_HEIGHT}`,
    },
    layout: 'stack',
    type: 'FACE_DOWN',
    name: 'Face Down',
    id: 5,
    reveal: [false, false]
  },
  sideboard: {
    pos: {
      width: '0',
      height: '0'
    },
    layout: 'stack',
    type: 'SIDEBOARD',
    name: 'Sideboard',
    id: 6,
    reveal: [true, false]
  }
};

export enum ScreenPosition {
  PRIMARY, SECONDARY
}

export function zoneFromIndex(index: number): ZoneConfig | undefined {
  const zone = Object.values(ZONES).find((zone) => zone.id === index);
  if (zone === undefined) {
    const normalizedIndex = index % 10;
    const negativeIndex = -(normalizedIndex + 1);
    const oppZone = Object.values(OPPONENT_ZONES).find((zone) => zone.id === negativeIndex);
    return { ...oppZone, id: index } as ZoneConfig;
  }
  return zone;
}

export function findZoneByName(name: string, pos: ScreenPosition, playerIndex: number): ZoneConfig | undefined {
  return getZones(pos, playerIndex).find((zone) => zone.type === name);
}

export function zoneNameToId(zone: Zone, pos: ScreenPosition, playerIndex: number): number {
  const primaryZone = Object.values(ZONES).find((z) => z.type === zone)!.id;
  return getZoneIdFromScreenPositionAndPlayerIndex(primaryZone, pos, playerIndex);
}


export function getZones(pos: ScreenPosition, playerIndex: number): ZoneConfig[] {
  return Object.values(getZonesMap(pos, playerIndex));
}

export function getZonesMap(pos: ScreenPosition, playerIndex: number | undefined): { [key: string]: ZoneConfig } {
  if (playerIndex === undefined) {
    return {};
  }

  if (pos === ScreenPosition.PRIMARY) {
    return ZONES;
  }
  else {
    const zones: { [key: string]: ZoneConfig } = {};
    for (const [key, value] of Object.entries(OPPONENT_ZONES)) {
      const zoneId = (playerIndex + 2) * 10 + -(value.id + 1);
      zones[key] = { ...value, id: zoneId };
    }
    return zones;
  }
}

export function isOpponentZone(zoneId: number, type: Zone): boolean {
  const zone = zoneFromIndex(zoneId);
  return zone !== undefined && zone.type === type && zone.id > 10;
}

export function getZoneIdFromScreenPositionAndPlayerIndex(zoneIndex: number, pos: ScreenPosition, playerIndex: number): number {
  if (pos === ScreenPosition.PRIMARY) {
    return zoneIndex;
  }
  else {
    return (playerIndex + 2) * 10 + zoneIndex;
  }
}

export function zoneName(zone: Zone): string {
  return Object.values(ZONES).find((z) => z.type === zone)!.name;
}