import Rectangle from './rect';
// import Card from './card';

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
  type: ZoneFlex,
  name: string,
  id: number,
  reveal: [boolean, boolean]
}

export const OPPONENT_ZONES: { [name: string] : ZoneConfig } = {
  play: {
    pos: {
      left: `${Z_WIDTH}`,
      top: `${Z_HEIGHT}`,
      width: `calc(100% - 2 * ${Z_WIDTH})`,
      height: `calc(50vh - ${Z_HEIGHT})`,
    },
    type: 'free',
    name: 'play', 
    id: -1, 
    reveal: [true, true]
  },
  hand: {
    pos: {
      left: `${Z_WIDTH}`,
      top: '0px',
      width: `calc(100% - 2 * ${Z_WIDTH})`,
      height: `${Z_HEIGHT}`,
    },
    type: 'hstack',
    name: 'hand', 
    id: -2, 
    reveal: [true, false]
  },
  graveyard: {
    pos: {
      left: '0',
      top: '0px',
      width: `${Z_WIDTH}`,
      height: `${Z_HEIGHT}`,
    },
    type: 'stack',
    name: 'graveyard', 
    id: -3, 
    reveal: [true, true]
  },
  exile: {
    pos: {
      left: '0',
      top: `${Z_HEIGHT}`,
      width: `${Z_WIDTH}`,
      height: `${Z_HEIGHT}`,
    },
    type: 'stack',
    name: 'exile', 
    id: -4, 
    reveal: [true, true]
  },
  faceDown: {
    pos: {
      left: '0',
      top: `calc(2 * ${Z_HEIGHT})`,
      width: `${Z_WIDTH}`,
      height: `${Z_HEIGHT}`,
    },
    type: 'stack',
    name: 'Face Down', 
    id: -5, 
    reveal: [false, false]
  },
  library: {
    pos: {
      right: '0',
      top: '0',
      width: `${Z_WIDTH}`,
      height: `${Z_HEIGHT}`,
    },
    type: 'stack',
    name: 'library', 
    id: -6, 
    reveal: [false, false]
  },
};

export const ZONES: { [name: string] : ZoneConfig } = {
  play: {
    pos: {
      left: `${Z_WIDTH}`,
      top: '50vh',
      width: `calc(100% - 2 * ${Z_WIDTH})`,
      height: `calc(50% - ${Z_HEIGHT})`,
    },
    type: 'free',
    name: 'play', 
    id: 0, 
    reveal: [true, true]
  },
  hand: {
    pos: {
      left: `${Z_WIDTH}`,
      bottom: '0px',
      width: `calc(100% - 2 * ${Z_WIDTH})`,
      height: `${Z_HEIGHT}`,
    },
    type: 'hstack',
    name: 'hand', 
    id: 1, 
    reveal: [true, false]
  },
  graveyard: {
    pos: {
      left: '0',
      bottom: '0px',
      width: `${Z_WIDTH}`,
      height: `${Z_HEIGHT}`,
    },
    type: 'stack',
    name: 'graveyard', 
    id: 2, 
    reveal: [true, true]
  },
  exile: {
    pos: {
      left: '0',
      bottom: `${Z_HEIGHT}`,
      width: `${Z_WIDTH}`,
      height: `${Z_HEIGHT}`,
    },
    type: 'stack',
    name: 'exile', 
    id: 3, 
    reveal: [true, true]
  },
  faceDown: {
    pos: {
      left: '0',
      bottom: `calc(2 * ${Z_HEIGHT})`,
      width: `${Z_WIDTH}`,
      height: `${Z_HEIGHT}`,
    },
    type: 'stack',
    name: 'Face Down', 
    id: 4, 
    reveal: [false, false]
  },
  library: {
    pos: {
      right: '0',
      bottom: '0',
      width: `${Z_WIDTH}`,
      height: `${Z_HEIGHT}`,
    },
    type: 'stack',
    name: 'library', 
    id: 5, 
    reveal: [false, false]
  },
};

// export function zoneAtPoint(x: number, y: number): Zone | undefined {
//   return Object.values(ZONES).find((zone) => zone.getBounds().containsPoint(x, y));
// }

export function zoneFromIndex(index: number): ZoneConfig | undefined {
  return Object.values(ZONES).find((zone) => zone.id === index);
}

export function opponentZone(zone: ZoneConfig): ZoneConfig {
  return Object.values(OPPONENT_ZONES).find((oZone) => oZone.name === zone.name)!;
}
