import type { CardId } from "@/stores/board";

export type WebSocketMessage = LocationMessage | IdentityMessage | ListingUpdateMessage | DeleteListingMessage | RoomStateMessage | BoardUpdateMessage | OracleInfoMessage;

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
  oracleId: string;
  image: string;
}

export type SpecialAction = 'MULLIGAN' | 'SCOOP' | 'SHUFFLE';
export type CardAttribute = 'PIVOT' | 'COUNTER' | 'TRANSFORMED';
export type PlayerAttribute = 'LIFE' | 'POISON';


export type BoardUpdateMessage = {
  type: 'board_update';
  events: BoardDiffEvent[];
}

export type OracleInfoMessage = {
  type: 'oracle_info';
  oracleInfo: { [cardId: CardId]: string };
}

export type BoardDiffEvent = ChangeZoneEvent | ChangeIndexEvent | ChangePositionEvent | ChangeAttributeEvent | ChangePlayerAttribute | ScoopDeck | ShuffleDeck;

export type ChangeZoneEvent = {
  type: 'change_zone';
  cardId: CardId;
  newZone: Zone;
  oldCardId: CardId;
}

export type ChangeIndexEvent = {
  type: 'change_index';
  cardId: CardId;
  newIndex: number;
}

export type ChangePositionEvent = {
  type: 'change_position';
  cardId: CardId;
  x: number;
  y: number;
}

export type ChangeAttributeEvent = {
  type: 'change_attribute';
  cardId: CardId;
  attribute: CardAttribute;
  value: number;
}

export type ChangePlayerAttribute = {
  attribute: PlayerAttribute;
  newValue: number;
  playerId: string;
  type: 'change_player_attribute';
}

export type ScoopDeck = {
  newIds: CardId[];
  type: 'scoop_deck';
}

export type ShuffleDeck = {
  newIds: CardId[];
  type: 'shuffle_deck';
}
