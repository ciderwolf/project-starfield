import { reactive, ref } from 'vue'
import { defineStore } from 'pinia'
import { type DeckListing } from '@/api/message';
import { fetchDeck, getDecks } from '@/api/deck';

export const useDecksStore = defineStore('decks', () => {

  const decks = reactive<{ [id: string]: DeckListing }>({});

  async function getDeck(id: string) {
    const cached = decks[id];
    if (cached) {
      return cached;
    }
    const deck = await fetchDeck(id);
    return deck;
  }

  async function reloadDecks() {
    const decksList = await getDecks()
    for (const deck of decksList) {
      decks[deck.id] = deck;
    }
  }
  
  return { decks, getDeck, reloadDecks }
})
