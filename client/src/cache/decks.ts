import { createAsyncCache } from ".";
import { fetchDeck, submitDeck } from "@/api/deck";

type DeckUpdate = {
  name: string;
  main: string[];
  side: string[];
}

export const useDecksCache = createAsyncCache(() => {
  return {
    load: async (id: string) => {
      return await fetchDeck(id);
    },
    write: (id: string, value: DeckUpdate) => {
      return submitDeck(id, value.name, value.main, value.side);
    }
  }
});