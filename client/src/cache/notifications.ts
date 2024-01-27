import { createLocalCache } from ".";

interface ModalHooks {
  findCards: () => void;
  viewZone: (zoneId: number, readOnly?: boolean) => void;
  scry: (count: number) => void;
}

export const useNotificationsCache = createLocalCache<ModalHooks>(() => { 
  return {
    findCards: () => {},
    viewZone: () => {},
    scry: () => {},
  };
});
