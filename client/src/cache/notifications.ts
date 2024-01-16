import { createLocalCache } from ".";

interface ModalHooks {
  findCards: () => void;
  viewZone: (zoneId: number) => void;
  scry: (count: number) => void;
}

export const useNotificationsCache = createLocalCache<ModalHooks>(() => { 
  return {
    findCards: () => {},
    viewZone: () => {},
    scry: () => {},
  };
});
