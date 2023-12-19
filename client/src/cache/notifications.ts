import { createLocalCache } from ".";

type Callback = () => void;
export const useNotificationsCache = createLocalCache(() => new Map<string, Callback>());