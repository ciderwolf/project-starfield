import { Pivot, type BoardCard } from "./api/message";
import { useBoardStore } from "./stores/board";
import { useDataStore } from "./stores/data";
import { ZONES, zoneFromIndex } from "./zones";

export interface ContextMenuDefinition {
  options: ContextMenuOption[];
}

type ContextMenuOption = ContextMenuSeperator | SubMenu | TextOption | NumberInputOption;

type ContextMenuSeperator = {
  type: 'seperator'
}

type SubMenu = {
  type: 'submenu';
  title: string;
  options: ContextMenuOption[];
}

type TextOption = {
  type: 'text';
  title: string;
  effect: () => void;
}

type NumberInputOption = {
  type: 'number';
  title: string;
  default?: number;
  min?: number;
  max?: number;
  effect: (value: number) => void;
}


type ActionEmit = (action: string, ...args: any[]) => void;

export function createContextMenu(zone: number, card: BoardCard, emit: ActionEmit) {
  switch(zone) {
    case ZONES.hand.id:
      return createHandContextMenu(card, emit);
    case ZONES.play.id:
      return createBattlefieldContextMenu(card, emit);
    case ZONES.library.id:
      return createLibraryContextMenu(card, emit);
    default:
      return { options: [] };
  }
}


export function createBattlefieldContextMenu(card: BoardCard, emit: ActionEmit): ContextMenuDefinition {
  const options: ContextMenuOption[] = [];
  if (card.flipped) {
    options.push({
      type: 'submenu',
      title: 'Reveal to...',
      options: getRevealToPlayersSubmenu(emit)
    });
  }

  const board = useBoardStore();
  if (board.oracleInfo[board.cardToOracleId[card.id]].backImage) {
    options.push({
      type: 'text',
      title: 'Transform',
      effect: () => {
        emit('transform');
      }
    });
  }

  options.push(
    {
      type: 'text',
      title: card.flipped ? 'Turn face up' : 'Turn face down',
      effect: () => {
        emit('flip');
      }
    },
    {
      type: 'text',
      title: card.pivot === Pivot.TAPPED ? 'Untap' : 'Tap',
      effect: () => {
        emit('tap');
      }
    },
    {
      type: 'text',
      title: 'Copy card',
      effect: () => {
        emit('copy');
      }
    },
    {
      type: 'seperator'
    },
    {
      type: 'submenu',
      title: 'Move to zone',
      options: getMoveZoneActions(card.zone, emit)
    },
    {
      type: 'seperator'
    },
    {
      type: 'number',
      title: card.counter === 0 ? 'Add counter' : 'Set counter',
      default: card.counter,
      min: 0,
      effect: (count: number) => {
        count = isNaN(count) ? 1 : count;
        emit('add-counter', count);
      }
    }
  );
  return { options };
}

export function createLibraryContextMenu(card: BoardCard, emit: ActionEmit): ContextMenuDefinition {
  return {
    options: [
      {
        type: 'text',
        title: 'Shuffle',
        effect: () => {
          emit('shuffle');
        }
      },
      {
        type: 'text',
        title: 'Scoop deck',
        effect: () => {
          emit('scoop');
        }
      },
      {
        type: 'text',
        title: 'Show Sideboard',
        effect: () => {
          emit('show-sideboard');
        }
      },
      { 
        type: 'seperator'
      },
      {
        type: 'text',
        title: 'Reveal top card',
        effect: () => {
          emit('reveal');
        }
      },
      {
        type: 'submenu',
        title: 'Reveal top card to...',
        options: getRevealToPlayersSubmenu(emit)
      },
      {
        type: 'number',
        title: 'Scry',
        min: 1,
        effect: (count: number) => {
          count = isNaN(count) ? 1 : count;
          emit('scry', count);
        }
      },
      {
        type: 'text',
        title: 'Find card',
        effect: () => {
          emit('find-card');
        }
      },
      {
        type: 'submenu',
        title: 'Draw cards...',
        options: [
          {
            type: 'number',
            title: 'Hand',
            min: 1,
            effect: (count: number) => {
              count = isNaN(count) ? 1 : count;
              emit('move-zone-n', ZONES.hand.type, count);
            }
          },
          {
            type: 'number',
            title: 'Battlefield',
            min: 1,
            effect: (count: number) => {
              count = isNaN(count) ? 1 : count;
              emit('move-zone-n', ZONES.play.type, count);
            }
          },
          {
            type: 'number',
            title: 'Graveyard',
            min: 1,
            effect: (count: number) => {
              count = isNaN(count) ? 1 : count;
              emit('move-zone-n', ZONES.graveyard.type, count);
            }
          },
          {
            type: 'number',
            title: 'Exile',
            min: 1,
            effect: (count: number) => {
              count = isNaN(count) ? 1 : count;
              emit('move-zone-n', ZONES.exile.type, count);
            }
          },
          {
            type: 'number',
            title: 'Face Down',
            min: 1,
            effect: (count: number) => {
              count = isNaN(count) ? 1 : count;
              emit('move-zone-n', ZONES.faceDown.type, count);
            }
          }, 
        ]
      }
    ]
  };
}

export function createHandContextMenu(card: BoardCard, emit: ActionEmit): ContextMenuDefinition {
  const options: ContextMenuOption[] = [
    {
      type: 'text',
      title: 'Reveal',
      effect: () => {
        emit('reveal');
      }
    },
    {
      type: 'submenu',
      title: 'Reveal to...',
      options: getRevealToPlayersSubmenu(emit)
    },
  ];
  const board = useBoardStore();
  if (board.oracleInfo[board.cardToOracleId[card.id]].backImage) {
    options.push({
      type: 'text',
      title: card.transformed ? 'See front face' : 'See back face',
      effect: () => {
        emit('transform');
      }
    })
  }
  options.push(
    {
      type: 'seperator'
    },
    {
      type: 'text',
      title: 'Play',
      effect: () => {
        emit('play');
      }
    },
    {
      type: 'text',
      title: 'Play face down',
      effect: () => {
        emit('play-face-down');
      }
    },
    {
      type: 'submenu',
      title: 'Move to zone...',
      options: getMoveZoneActions(card.zone, emit)
    }
  );
  return { options };
}

export function getMoveZoneActions(exceptZone: number, emit: ActionEmit): TextOption[] {
  const options: TextOption[] = [
    {
      type: 'text',
      title: 'Deck top',
      effect: () => {
        emit('move-zone', ZONES.library.id, -1);
      }
    },
    {
      type: 'text',
      title: 'Deck bottom',
      effect: () => {
        emit('move-zone', ZONES.library.id, 0);
      }
    },
    {
      type: 'text',
      title: 'Hand',
      effect: () => {
        emit('move-zone', ZONES.hand.id);
      }
    },
    {
      type: 'text',
      title: 'Battlefield',
      effect: () => {
        emit('move-zone', ZONES.play.id);
      }
    },
    {
      type: 'text',
      title: 'Graveyard',
      effect: () => {
        emit('move-zone', ZONES.graveyard.id);
      }
    },
    {
      type: 'text',
      title: 'Exile',
      effect: () => {
        emit('move-zone', ZONES.exile.id);
      }
    },
    {
      type: 'text',
      title: 'Face Down',
      effect: () => {
        emit('move-zone', ZONES.faceDown.id);
      }
    }, 
  ];
  return options
    .filter(option => option.title !== zoneFromIndex(exceptZone)?.name);
}

export function getZoneContextMenu(zoneId: number, isInteractive: boolean, emit: ActionEmit): ContextMenuDefinition {
  const options: ContextMenuOption[] = [
    {
      type: 'text',
      title: 'View all cards',
      effect: () => emit('view-all-cards')
    }
  ];
  if (isInteractive) {
    options.push({
      type: 'submenu',
      title: 'Move all to zone...',
      options: getMoveZoneActions(zoneId, emit)
    });
  }
  return { options };
}

function getRevealToPlayersSubmenu(emit: ActionEmit) {
  const board = useBoardStore();
  const data = useDataStore();
  
  const options: ContextMenuOption[] = Object.values(board.players)
    .filter(player => player.id !== data.userId)
    .map(player => {
      return {
        type: 'text',
        title: player.name,
        effect: () => {
          emit('reveal-to', player.id);
        }
      }
    });

  options.push(
    {
    'type': 'seperator'
    }, 
    {
      type: 'text',
      title: 'All',
      effect: () => {
        emit('reveal-to', undefined);
      }
    }
  );
  
  return options;
}