import type { CardId, OracleId } from "@/stores/board";
import { createLocalCache } from ".";

interface ModalHooks {
  findCards: () => void;
  viewZone: (zoneId: number) => void;
  scry: (count: number) => void;
  hoverCardEnter: (cardId: CardId) => void;
  hoverCardLeave: () => void;
  showCardPreview: (oracleId: OracleId, backFace: boolean) => void;
  hideCardPreview: () => void;
  sideboard(): void;
  createToken(): void;
  createCard(): void;
  endGame(): void;
  rollDie(): void;
}

export const useNotificationsCache = createLocalCache<ModalHooks>(() => {
  return {
    findCards: () => { },
    viewZone: () => { },
    scry: () => { },
    hoverCardEnter: () => { },
    hoverCardLeave: () => { },
    showCardPreview: () => { },
    hideCardPreview: () => { },
    sideboard: () => { },
    createToken: () => { },
    createCard: () => { },
    endGame: () => { },
    rollDie: () => { },
  };
});
