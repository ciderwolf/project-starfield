import type { Zone } from "./api/message";

const Z_WIDTH = '78px';
const Z_HEIGHT = '108px';

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

export const OPPONENT_ZONES: { [name: string] : ZoneConfig } = {
  library: {
    pos: {
      right: '0',
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
      width: `calc(100% - 2 * ${Z_WIDTH})`,
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
      width: `calc(100% - 2 * ${Z_WIDTH})`,
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

export const ZONES: { [name: string] : ZoneConfig } = {
  library: {
    pos: {
      right: '0',
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
      width: `calc(100% - 2 * ${Z_WIDTH})`,
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
      width: `calc(100% - 2 * ${Z_WIDTH})`,
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
    return Object.values(OPPONENT_ZONES).find((zone) => zone.id === index);
  }
  return zone;
}

export function opponentZone(zone: ZoneConfig): ZoneConfig {
  return Object.values(OPPONENT_ZONES).find((oZone) => oZone.type === zone.type)!;
}

export function findZoneByName(name: string, pos = ScreenPosition.PRIMARY): ZoneConfig | undefined {
  return getZones(pos).find((zone) => zone.type === name);
}


export function zoneNameToId(zone: Zone, pos = ScreenPosition.PRIMARY): number {
  return getZones(pos).find((z) => z.type === zone)!.id;
}


export function getZones(pos = ScreenPosition.PRIMARY): ZoneConfig[] {
  if (pos === ScreenPosition.SECONDARY) {
    return Object.values(OPPONENT_ZONES);
  }
  else {
    return Object.values(ZONES);
  }
}