import { getJson, postJson } from ".";
import type { Deck, DeckListing } from "./message";

export async function getDecks(): Promise<DeckListing[]> {
  return getJson('deck');
}

export async function fetchDeck(id: string): Promise<Deck> {
  return getJson(`deck/${id}`);
}

export async function newDeck(): Promise<Deck> {
  return postJson('deck/new', {});
}

export async function submitDeck(id: string, name: string, maindeck: string[], sideboard: string[]): Promise<Deck> {
  return postJson(`deck/${id}`, { name, maindeck, sideboard });
}