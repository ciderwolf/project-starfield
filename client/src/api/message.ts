import type { CardId } from "@/stores/board";

export type WebSocketMessage = LocationMessage | IdentityMessage | ListingUpdateMessage | DeleteListingMessage | RoomStateMessage | BoardUpdateMessage | OracleCardsMessage | GameLogMessage;

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
  players: PlayerListing[],
  inProgress: boolean,
}

export interface PlayerListing {
  id: string;
  name: string;
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
  users: PlayerListing[];
  decks: (string | null)[];
}

export type GameState = {
  id: string;
  name: string;
  players: PlayerState[];
}

export type Zone = 'BATTLEFIELD' | 'HAND' | 'LIBRARY' | 'GRAVEYARD' | 'EXILE' | 'FACE_DOWN' | 'SIDEBOARD';

export type PlayerState = {
  id: string;
  name: string;
  board: Partial<{ [key in Zone]: BoardCard[] }>;
  cardToOracleId: { [cardId: CardId]: string };
  oracleInfo: { [oracleId: string]: OracleCard };
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
  index: number;
  flipped: boolean;
  zone: number;
  id: CardId;
  visibility: string[];
};

export type Deck = {
  maindeck: DeckCard[];
  sideboard: DeckCard[];
  name: string;
  id: string;
  thumbnailImage: string;
}

export type DeckListing = {
  name: string;
  id: string;
  thumbnailImage: string;
}

export type ConflictResolutionStrategy = 'NoConflict' | 'Best' | 'Default' | 'Pinned';
export type DeckCard = {
  name: string;
  type: string;
  count: number;
  id: string;
  image: string;
  backImage: string | null;
  source: string;
  conflictResolutionStrategy: ConflictResolutionStrategy;
};

export type Card = {
  name: string;
  oracleId: string;
  image: string;
  backImage: string | null;
}

export type OracleCard = {
  name: string;
  id: string;
  image: string;
  backImage: string | null;
}

export type CardAttributeMap = Partial<Record<CardAttribute, number>>;

export type SpecialAction = 'MULLIGAN' | 'SCOOP' | 'SHUFFLE' | 'UNTAP_ALL';
export type CardAttribute = 'PIVOT' | 'COUNTER' | 'TRANSFORMED' | 'FLIPPED';
export type PlayerAttribute = 'LIFE' | 'POISON';


export type BoardUpdateMessage = {
  type: 'board_update';
  events: BoardDiffEvent[];
}

export type OracleCardsMessage = {
  type: 'oracle_cards';
  cards: { [cardId: CardId]: string };
  oracleInfo: { [oracleId: string]: OracleCard };
  cardsToHide: CardId[];
}

export type AccountableAction = "FIND_CARD" | "SIDEBOARD" | "SCRY" | "REVEAL";
export type GameLogMessage = {
  type: "log";
  owner: string;
  message: LogInfoMessage;
}

export type LogInfoMessage = SideboardLogMessage | FindCardLogMessage | RollDieLogMessage | ScryLogMessage | RevealLogMessage;

export type SideboardLogMessage = {
  type: "sideboard";
}
export type FindCardLogMessage = {
  type: "find_card";
}
export type RollDieLogMessage = {
  type: "roll_die";
  sides: number;
  result: number;
}
export type ScryLogMessage = {
  type: "scry";
  count: number;
}
export type RevealLogMessage = {
  type: "reveal";
  card: CardId;
  revealTo: string | null;
}

export type BoardDiffEvent = ChangeZoneEvent | ChangeIndexEvent | ChangePositionEvent | ChangeAttributeEvent | ChangePlayerAttribute | ScoopDeck | ShuffleDeck | RevealCard | HideCard | CreateCard | DestroyCard;

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
  player: string;
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

export type HideCard = {
  players: string[];
  card: CardId;
  type: 'hide_card';
}

export type CreateCard = {
  state: BoardCard;
  type: 'create_card';
}

export type DestroyCard = {
  card: CardId;
  type: 'destroy_card';
}

export type ClientMessage = ChangeCardAttributeMessage 
  | ChangeCardIndexMessage 
  | ChangeCardPositionMessage 
  | ChangeCardZoneMessage 
  | ChangeCardZonesMessage
  | MoveCardVirtualMessage
  | ChangePlayerAttributeMessage 
  | DrawCardMessage 
  | PlayCardMessage 
  | SpecialActionMessage 
  | RevealCardMessage
  | ScryMessage
  | CreateTokenMessage
  | CreateCardMessage
  | CloneCardMessage
  | SideboardMessage;

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

type ChangeCardZonesMessage = {
  cards: number[];
  index: number;
  zone: Zone;
  type: "change_zones";
}

type ChangePlayerAttributeMessage = {
  attribute: PlayerAttribute;
  newValue: number;
  type: "change_player_attribute";
}

type DrawCardMessage = {
  count: number;
  to: Zone;
  fromBottom: boolean;
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
  reveal: boolean;
  type: "reveal";
}

type ScryMessage = {
  count: number;
  type: "scry";
}

type CreateTokenMessage = {
  id: string;
  type: "create_token";
}

type CreateCardMessage = {
  id: string;
  type: "create_card";
}

type CloneCardMessage = {
  id: CardId;
  type: "clone_card";
}

type SideboardMessage = {
  main: string[];
  side: string[];
  type: "sideboard";
}

type MoveCardVirtualMessage = {
  ids: string[];
  zone: Zone;
  index: number;
  type: "move_virtual";
}