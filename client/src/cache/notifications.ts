import type { OracleId } from "@/stores/board";
import { createLocalCache } from ".";

interface ModalHooks {
  findCards: () => void;
  viewZone: (zoneId: number, readOnly?: boolean) => void;
  scry: (count: number) => void;
  showCardPreview: (oracleId: OracleId, backFace: boolean) => void;
  hideCardPreview: () => void;

}

export const useNotificationsCache = createLocalCache<ModalHooks>(() => { 
  return {
    findCards: () => {},
    viewZone: () => {},
    scry: () => {},
    showCardPreview: () => {},
    hideCardPreview: () => {},
  };
});
