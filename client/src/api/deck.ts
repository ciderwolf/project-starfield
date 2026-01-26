import { deleteJson, getJson, getJsonRaw, postJson } from ".";
import type { Deck, DeckListing } from "./message";

export async function getDecks(): Promise<DeckListing[]> {
  const response = await getJsonRaw<DeckListing[]>('deck');
  if (response.success) {
    return response.content;
  } else {
    return [];
  }
}

export async function fetchDeck(id: string): Promise<Deck> {
  return getJson(`deck/${id}`);
}

export async function newDeck(name?: string): Promise<Deck> {
  let path = 'deck/new';
  if (name) {
    path += `?name=${encodeURIComponent(name)}`;
  }
  return postJson(path, {});
}

export async function submitDeck(id: string, name: string, maindeck: string[], sideboard: string[]): Promise<Deck> {
  return postJson(`deck/${id}`, { name, maindeck, sideboard });
}

export async function deleteDeck(id: string): Promise<boolean> {
  return deleteJson(`deck/${id}`);
}

export async function addCardsToDeck(deckId: string, cardsToAddToMain: string[], cardsToAddToSide: string[]): Promise<Deck> {
  return postJson(`deck/${deckId}/add-cards`, { maindeck: cardsToAddToMain, sideboard: cardsToAddToSide });
}