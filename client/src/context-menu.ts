import { Pivot, type BoardCard } from "./api/message";
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
  min?: number;
  max?: number;
  effect: (value: number) => void;
}


type ActionEmit = (action: string, ...args: number[]) => void;

export function createContextMenu(zone: number, card: BoardCard, emit: ActionEmit) {
  switch(zone) {
    // case ZONES.hand.id:
    //   return createHandContextMenu(card, emit);
    case ZONES.play.id:
      return createBattlefieldContextMenu(card, emit);
    case ZONES.library.id:
      return createLibraryContextMenu(emit);
    default:
      return { options: [] };
  }
}


function createBattlefieldContextMenu(card: BoardCard, emit: ActionEmit): ContextMenuDefinition {
  return {
    options: [
      {
        type: 'text',
        title: 'Transform',
        effect: () => {
          emit('transform');
        }
      },
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
        type: 'submenu',
        title: 'Move to zone',
        options: getMoveZoneActions(card.zone, emit)
      },
      {
        type: 'seperator'
      }
    ]
  };
}

function createLibraryContextMenu(emit: ActionEmit): ContextMenuDefinition {
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
        type: 'seperator'
      },
      {
        type: 'text',
        title: 'Reveal top card',
        effect: () => {
          emit('reveal-top');
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
              for(let i = 0; i < count; i++) {
                emit('move-zone', ZONES.hand.id);
              }
            }
          },
          {
            type: 'number',
            title: 'Battlefield',
            min: 1,
            effect: (count: number) => {
              count = isNaN(count) ? 1 : count;
              for(let i = 0; i < count; i++) {
                emit('move-zone', ZONES.play.id);
              }
            }
          },
          {
            type: 'number',
            title: 'Graveyard',
            min: 1,
            effect: (count: number) => {
              count = isNaN(count) ? 1 : count;
              for(let i = 0; i < count; i++) {
                emit('move-zone', ZONES.graveyard.id);
              }
            }
          },
          {
            type: 'number',
            title: 'Exile',
            min: 1,
            effect: (count: number) => {
              count = isNaN(count) ? 1 : count;
              for(let i = 0; i < count; i++) {
                emit('move-zone', ZONES.exile.id);
              }
            }
          },
          {
            type: 'number',
            title: 'Face Down',
            min: 1,
            effect: (count: number) => {
              count = isNaN(count) ? 1 : count;
              for(let i = 0; i < count; i++) {
                emit('move-zone', ZONES.faceDown.id);
              }
            }
          }, 
        ]
      }
    ]
  };
}

function getMoveZoneActions(exceptZone: number, emit: ActionEmit): TextOption[] {
  const options: TextOption[] = [
    {
      type: 'text',
      title: 'Deck top',
      effect: () => {
        emit('move-zone', ZONES.library.id, 0);
      }
    },
    {
      type: 'text',
      title: 'Deck bottom',
      effect: () => {
        emit('move-zone', ZONES.library.id, -1);
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