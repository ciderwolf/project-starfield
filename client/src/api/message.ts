import type { CardId } from "@/stores/board";

export type WebSocketMessage = LocationMessage | IdentityMessage | ListingUpdateMessage | DeleteListingMessage | RoomStateMessage | BoardUpdateMessage | OracleCardsMessage;

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
  id: string;
  name: string;
  users: string[];
  decks: (string | null)[];
}

export type GameState = {
  name: string;
  players: PlayerState[];
}

export type Zone = 'BATTLEFIELD' | 'HAND' | 'LIBRARY' | 'GRAVEYARD' | 'EXILE' | 'FACE_DOWN' | 'SIDEBOARD';

export type PlayerState = {
  id: string;
  name: string;
  board: Partial<{ [key in Zone]: BoardCard[] }>;
  oracleInfo: { [cardId: CardId]: string };
  life: number;
  poison: number;
}

export enum Pivot {
  UNTAPPED,
  TAPPED,
  LEFT_TAPPED,
  UPSIDE_DOWN,
}

export type BoardCard = Card & {
  x: number;
  y: number;
  pivot: Pivot;
  counter: number;
  transformed: boolean;
  flipped: boolean;
  zone: number;
  id: CardId;
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
export type CardAttributeMap = Partial<Record<CardAttribute, number>>;

export type SpecialAction = 'MULLIGAN' | 'SCOOP' | 'SHUFFLE';
export type CardAttribute = 'PIVOT' | 'COUNTER' | 'TRANSFORMED' | 'FLIPPED';
export type PlayerAttribute = 'LIFE' | 'POISON';


export type BoardUpdateMessage = {
  type: 'board_update';
  events: BoardDiffEvent[];
}

export type OracleCardsMessage = {
  type: 'oracle_cards';
  cards: { [cardId: CardId]: string };
}

export type BoardDiffEvent = ChangeZoneEvent | ChangeIndexEvent | ChangePositionEvent | ChangeAttributeEvent | ChangePlayerAttribute | ScoopDeck | ShuffleDeck | RevealCard;

export type ChangeZoneEvent = {
  type: 'change_zone';
  card: CardId;
  newZone: Zone;
  oldCardId: CardId;
}

export type ChangeIndexEvent = {
  type: 'change_index';
  card: CardId;
  newIndex: number;
}

export type ChangePositionEvent = {
  type: 'change_position';
  card: CardId;
  x: number;
  y: number;
}

export type ChangeAttributeEvent = {
  type: 'change_attribute';
  card: CardId;
  attribute: CardAttribute;
  newValue: number;
}

export type ChangePlayerAttribute = {
  attribute: PlayerAttribute;
  newValue: number;
  playerId: string;
  type: 'change_player_attribute';
}

export type ScoopDeck = {
  playerId: string;
  newIds: CardId[];
  type: 'scoop_deck';
}

export type ShuffleDeck = {
  playerId: string;
  newIds: CardId[];
  type: 'shuffle_deck';
}

export type RevealCard = {
  players: string[];
  card: CardId;
  type: 'reveal_card';
}

export type ClientMessage = ChangeCardAttributeMessage 
  | ChangeCardIndexMessage 
  | ChangeCardPositionMessage 
  | ChangeCardZoneMessage 
  | ChangePlayerAttributeMessage 
  | DrawCardMessage 
  | PlayCardMessage 
  | SpecialActionMessage 
  | RevealCardMessage;

type ChangeCardAttributeMessage = {
  attribute: CardAttribute;
  card: number;
  newValue: number;
  type: "change_card_attribute";
}

type ChangeCardIndexMessage = {
  card: number;
  index: number;
  type: "change_index";
}

type ChangeCardPositionMessage = {
  card: number;
  x: number;
  y: number;
  type: "change_position";
}

type ChangeCardZoneMessage = {
  card: number;
  index: number;
  zone: Zone;
  type: "change_zone";
}

type ChangePlayerAttributeMessage = {
  attribute: PlayerAttribute;
  newValue: number;
  type: "change_player_attribute";
}

type DrawCardMessage = {
  count: number;
  to: Zone;
  type: "draw_card";
}

type PlayCardMessage = {
  card: number;
  x: number;
  y: number;
  attributes: CardAttributeMap;
  type: "play_card";
}

type SpecialActionMessage = {
  action: SpecialAction;
  type: "special_action";
}

type RevealCardMessage = {
  card: CardId;
  revealTo: string | null;
  type: "reveal";
}

