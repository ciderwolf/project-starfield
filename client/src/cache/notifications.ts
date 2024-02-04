import type { OracleId } from "@/stores/board";
import { createLocalCache } from ".";

interface ModalHooks {
  findCards: () => void;
  viewZone: (zoneId: number, readOnly?: boolean) => void;
  scry: (count: number) => void;
  showCardPreview: (oracleId: OracleId, backFace: boolean) => void;
  hideCardPreview: () => void;
  sideboard(): void;
}

export const useNotificationsCache = createLocalCache<ModalHooks>(() => { 
  return {
    findCards: () => {},
    viewZone: () => {},
    scry: () => {},
    showCardPreview: () => {},
    hideCardPreview: () => {},
    sideboard: () => {},
  };
});
