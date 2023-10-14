export type WebSocketMessage = LocationMessage | IdentityMessage | ListingUpdateMessage | DeleteListingMessage | RoomStateMessage;

type LocationMessage = {
  type: 'location';
  location: 'HOME' | 'LOBBY' | 'GAME' | 'DECK_BUILDER' | 'LOGIN';
  id: string | null;
};

type IdentityMessage = {
  type: 'identity';
  id: string;
  username: string;
}

type ListingUpdateMessage = {
  type: 'listing';
  listing: GameListing;
}

type DeleteListingMessage = {
  type: 'delete_listing';
  id: string;
}

export interface GameListing {
  id: string,
  name: string,
  players: string[],
  inProgress: boolean,
}

export type RoomStateMessage = GameStateMessage | LobbyStateMessage;

type GameStateMessage = {
  type: 'state';
  room: 'game';
  roomState: GameState;
}

type LobbyStateMessage = {
  type: 'state';
  room: 'lobby';
  roomState: LobbyState;
}

export type LobbyState = {
  name: string;
  users: string[];
  decks: (string | null)[];
}

export type GameState = {
  name: string;
  players: PlayerState[];
}

type Zone = 'BATTLEFIELD' | 'HAND' | 'LIBRARY' | 'GRAVEYARD' | 'EXILE' | 'FACE_DOWN';

type PlayerState = {
  id: string;
  name: string;
  board: Partial<{ [key in Zone]: BoardCard[] }>;
  life: number;
  poison: number;
}

type BoardCard = {
  id: string;
  x: number;
  y: number;
  attributes: { [attr: string]: number };
  card: Card;
};

export type Deck = {
  maindeck: DeckCard[];
  sideboard: DeckCard[];
  name: string;
  id: string;
}

export type DeckListing = {
  name: string;
  id: string;
}

export type DeckCard = Card & {
  type: string;
  count: number;
}


export type Card = {
  name: string;
  id: string;
  image: string;
}