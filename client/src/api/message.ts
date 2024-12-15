import type { CardId } from "@/stores/board";

export type WebSocketMessage = LocationMessage | IdentityMessage | ListingUpdateMessage | DeleteListingMessage | RoomStateMessage | BoardUpdateMessage | OracleCardsMessage | GameLogMessage | DraftEventMessage;
export type Location = 'HOME' | 'LOBBY' | 'DRAFT' | 'GAME' | 'DECK_BUILDER' | 'LOGIN';


export type DraftEvent =
  | { type: 'receive_pack', pack: DraftCard[], pickNumber: number, packNumber: number }
  | { type: 'pack_queue', packs: { [id: string]: number } }
  | { type: 'end_draft', deckId: string };

export type DraftEventMessage = {
  type: 'draft_event';
  events: DraftEvent[];
}

type LocationMessage = {
  type: 'location';
  location: Location;
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
  type: 'GAME' | 'DRAFT' | 'LOBBY',
  name: string,
  players: PlayerListing[],
  inProgress: boolean,
}

export interface PlayerListing {
  id: string;
  name: string;
}

export type RoomStateMessage = GameStateMessage | LobbyStateMessage | DraftStateMessage;

type GameStateMessage = {
  type: 'state';
  room: 'GAME';
  roomState: GameState;
}

type LobbyStateMessage = {
  type: 'state';
  room: 'LOBBY';
  roomState: LobbyState;
}

type DraftStateMessage = {
  type: 'state';
  room: 'DRAFT';
  roomState: DraftState;
}

export type LobbyState = GameLobbyState | DraftLobbyState;

export type GameLobbyState = {
  type: 'game_lobby';
  id: string;
  name: string;
  users: PlayerListing[];
  decks: (string | null)[];
}

export type DraftLobbyState = {
  type: 'draft_lobby';
  id: string;
  name: string;
  users: PlayerListing[];
  set: string;
  bots: number;
}

export type GameState = {
  id: string;
  name: string;
  currentPlayer: number;
  oracleInfo: { [oracleId: string]: OracleCard };
  players: PlayerState[];
  spectators: UserState[];
}

export type UserState = {
  name: string;
  id: string;
}

export type DraftState = {
  id: string;
  name: string;
  players: PlayerListing[];
  pack: DraftCard[];
  picks: DraftCard[];
  packQueues: { [id: string]: number };
  set: string;
  pickNumber: number;
  packNumber: number;
}

export type Zone = 'BATTLEFIELD' | 'HAND' | 'LIBRARY' | 'GRAVEYARD' | 'EXILE' | 'FACE_DOWN' | 'SIDEBOARD';

export type PlayerState = {
  id: string;
  name: string;
  board: Partial<{ [key in Zone]: BoardCard[] }>;
  cardToOracleId: { [cardId: CardId]: string };
  life: number;
  poison: number;
}

export enum Pivot {
  UNTAPPED,
  TAPPED,
  LEFT_TAPPED,
  UPSIDE_DOWN,
}
export enum Highlight {
  NONE, LOG, SELECTED
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

  highlight?: Highlight;
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
  tokens: string[] | null;
}

export type CardAttributeMap = Partial<Record<CardAttribute, number>>;

export type SpecialAction = 'MULLIGAN' | 'SCOOP' | 'SHUFFLE' | 'UNTAP_ALL' | 'END_TURN';
export type CardAttribute = 'PIVOT' | 'COUNTER' | 'TRANSFORMED' | 'FLIPPED';
export type PlayerAttribute = 'LIFE' | 'POISON' | 'ACTIVE_PLAYER';


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

export type BoardDiffEvent = ChangeZoneEvent | ChangeIndexEvent | ChangePositionEvent | ChangeAttributeEvent | ChangePlayerAttribute | ScoopDeck | ShuffleDeck | RevealCard | HideCard | CreateCard | DestroyCard | SpectatorJoin | SpectatorLeave;

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

export type SpectatorJoin = {
  user: UserState;
  type: "spectator_join";
};

export type SpectatorLeave = {
  user: string;
  type: "spectator_leave";
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
  | SideboardMessage
  | EndTurnMessage
  | { type: 'pick', card: string };

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

type EndTurnMessage = {
  type: "end_turn";
}

type MoveCardVirtualMessage = {
  ids: string[];
  zone: Zone;
  index: number;
  type: "move_virtual";
}

export type DraftCard = Card & { foil: boolean; id: string };