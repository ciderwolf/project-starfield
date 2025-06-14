import { useNotificationsCache } from "./cache/notifications";
import type { CardId } from "./stores/board";
import { client } from "./ws";
import { ZONES } from "./zones";

enum HotkeyModifier {
  Ctrl = 'Control',
  Alt = 'Alt',
  Shift = 'Shift'
}

export enum HotkeyAttribute {
  CardSpecific,
  ShiftToOverride
}

type HotkeyDefinition = {
  key: string;
  description: string;
  attributes?: HotkeyAttribute[];
  action: (context: HotkeyContext) => void;
};

type HotkeyContext = {
  modifiers: HotkeyModifier[];
  number: number | null;
  card: CardId | null;
}

export const hotkeys: HotkeyDefinition[] = [
  {
    key: 'x',
    description: 'Untap all',
    action: () => {
      client.untapAll();
    },
  },
  {
    key: 'r',
    description: 'Scoop',
    action: () => {
      if (confirm('Are you sure you want to scoop your deck?')) {
        client.scoop();
      }
    }
  },
  {
    key: 'v',
    description: 'Shuffle',
    action: () => {
      client.shuffle();
    }
  },
  {
    key: 'c',
    description: 'Draw',
    action: (ctx) => {
      client.drawCards(ctx.number || 1);
    }
  },
  {
    key: 'm',
    description: 'Mulligan',
    attributes: [HotkeyAttribute.ShiftToOverride],
    action: (ctx) => {
      if (ctx.modifiers.includes(HotkeyModifier.Shift) || confirm('Are you sure you want to scoop your deck and mulligan?')) {
        client.mulligan();
      }
    }
  },
  {
    key: 'f',
    description: 'Find cards',
    action: () => {
      const notification = useNotificationsCache();
      notification.findCards();
    }
  },
  {
    key: 'q',
    description: 'End game',
    action: () => {
      const notification = useNotificationsCache();
      notification.endGame();
    }
  },
  {
    key: 'w',
    description: 'Create token',
    action: () => {
      const notification = useNotificationsCache();
      notification.createToken();
    }
  },
  {
    key: 'n',
    description: 'Create card',
    action: () => {
      const notification = useNotificationsCache();
      notification.createCard();
    }
  },
  {
    key: 'e',
    description: 'End turn',
    action: () => {
      client.endTurn();
    }
  },
  {
    key: 'd',
    description: 'Destroy a card',
    attributes: [HotkeyAttribute.CardSpecific],
    action: (ctx) => {
      moveCardToZone(ctx.card, ZONES.graveyard.id);
    }
  },
  {
    key: 's',
    description: 'Move a card to exile',
    attributes: [HotkeyAttribute.CardSpecific],
    action: (ctx) => {
      moveCardToZone(ctx.card, ZONES.exile.id);
    }
  },
];

let currentDigit: number | null = null;
let digitTimeoutHandle: number | null = null;
export function handleHotkey(e: KeyboardEvent) {
  if ((e.target as HTMLElement).nodeName === "INPUT") {
    return;
  }
  const key = e.key;
  if (key.match(/^\d$/)) {
    if (currentDigit === null) {
      currentDigit = parseInt(key);
    } else {
      const cardIndex = currentDigit * 10 + parseInt(key);
      currentDigit = cardIndex;
    }
    if (digitTimeoutHandle) {
      clearTimeout(digitTimeoutHandle);
    }
    digitTimeoutHandle = window.setTimeout(() => {
      currentDigit = null;
      digitTimeoutHandle = null;
    }, 1000);
    e.preventDefault();
    return;
  }
  const modifiers: HotkeyModifier[] = [];
  if (e.ctrlKey) {
    modifiers.push(HotkeyModifier.Ctrl);
  }
  if (e.altKey) {
    modifiers.push(HotkeyModifier.Alt);
  }
  if (e.shiftKey) {
    modifiers.push(HotkeyModifier.Shift);
  }
  const context: HotkeyContext = {
    modifiers,
    number: currentDigit,
    card: cardId,
  };
  const hotkey = hotkeys.find(h => h.key === key.toLowerCase());
  if (hotkey) {
    e.preventDefault();
    hotkey.action(context);
  }
}


let cardId: CardId | null = null;
export function mountHotkeys() {
  window.addEventListener('keypress', handleHotkey);
  const cache = useNotificationsCache()
  cache.hoverCardEnter = (hoveredCardId: CardId) => {
    cardId = hoveredCardId;
  }
  cache.hoverCardLeave = () => {
    cardId = null;
  }
};

function moveCardToZone(cardId: CardId | null, zoneId: number) {
  if (cardId) {
    if (cardId) {
      client.moveCardToZone(cardId, zoneId, 0, 0);
      const notification = useNotificationsCache();
      notification.hideCardPreview();
      notification.hoverCardLeave();
    }
  }
}