import type { BoardDiffEvent, LogInfoMessage } from "./api/message";
import { extractPlayerIndex, useBoardStore, type CardId } from "./stores/board";
import { findZoneByName } from "./zones";

export interface EventMessage {
    message: string;
    card?: CardId;
}

export function getEventMessage(event: BoardDiffEvent): EventMessage | null {
    const owner = identifyEventOwner(event);
    const name = getOwnerName(owner);

    switch (event.type) {
        case "change_attribute":
        case "change_position":
        case "reveal_card":
        case "spectator_leave":
            return null;
        case "spectator_join":
            return {
                message: `${name} started spectating the game`
            }
        case "change_index": {
            if (((event.newIndex & 0b11110000) >> 4) === 0 && event.newIndex === 0) {
                return {
                    message: `${name} moved a card to the bottom of their deck`,
                    card: event.card
                };
            } else {
                return {
                    message: `${name} moved a card to position ${event.newIndex} in its zone`,
                    card: event.card
                };
            }
        }
        case "destroy_card":
            return {
                message: `${name} destroyed a card`,
                card: event.card
            };
        case "change_zone":
            return {
                message: `${name} moved a card to their ${findZoneByName(event.newZone)!.name}`,
                card: event.card
            };
        case "hide_card":
            return {
                message: `${name} hid a card from ${getPlayerReveals(event.players)}`,
                card: event.card
            };
        case "create_card":
            return {
                message: `${name} created a card`,
                card: event.state.id
            };
        case "change_player_attribute":
            if (event.attribute === "ACTIVE_PLAYER") {
                if (event.newValue === 0) {
                    return {
                        message: `${name} ended their turn`
                    };
                } else {
                    return null;
                }
            }
            return {
                message: `${name} changed their ${event.attribute.toLowerCase()} to ${event.newValue}`
            };
        case "scoop_deck":
            return {
                message: `${name} scooped their deck`
            };
        case "shuffle_deck":
            return {
                message: `${name} shuffled their deck`
            };
    }
}

function identifyEventOwner(event: BoardDiffEvent): string {
    switch (event.type) {
        case "change_attribute":
        case "change_index":
        case "change_position":
        case "destroy_card":
        case "change_zone":
        case "hide_card":
        case "reveal_card":
            return identifyCardOwner(event.card);
        case "create_card":
            return identifyCardOwner(event.state.id);
        case "change_player_attribute":
            return event.player;
        case "scoop_deck":
        case "shuffle_deck":
            return event.playerId;
        case "spectator_join":
            return event.user.id;
        case "spectator_leave":
            return event.user;
    }
}

function identifyCardOwner(cardId: CardId): string {
    const idx = extractPlayerIndex(cardId);
    const board = useBoardStore();
    return Object.values(board.players).find(player => player.index === idx)!.id;
}

function getOwnerName(owner: string) {
    const board = useBoardStore();
    if (owner in board.players) {
        return board.players[owner].name;
    } else {
        return board.spectators[owner]?.name;
    }
}

function getPlayerReveals(players: string[]): string {
    if (players.length === 0) {
        return "everyone";
    }
    const board = useBoardStore();
    return players.map(player => board.players[player].name).join(", ");
}

export function getLogMessage(message: LogInfoMessage, ownerName: string): string {
    switch (message.type) {
        case 'find_card':
            return `${ownerName} is searching their deck for a card`;
        case 'sideboard':
            return `${ownerName} is sideboarding`;
        case 'reveal':
            const board = useBoardStore();
            return `${ownerName} is revealing a card to ${board.players[message.revealTo ?? ""]?.name ?? "everyone"}`;
        case 'scry':
            return `${ownerName} is looking at the top ${message.count} card${message.count === 1 ? "" : "s"} of their deck`;
        case 'roll_die':
            if (message.sides === 2) {
                return `${ownerName} flipped a coin and got ${message.result === 1 ? "heads" : "tails"}`;
            }
            return `${ownerName} rolled a ${message.sides}-sided die and got ${message.result}`;
    }
}

class RollingWindowQueue<T> {
    private readonly contents: T[];
    private currentPos: number = 0;
    private size: number = 0;
    private readonly maxLength: number;


    constructor(maxLength: number) {
        this.maxLength = maxLength;
        this.contents = new Array(maxLength);
    }

    push(item: T) {
        if (this.size < this.maxLength) {
            this.size++;
        }
        this.contents[this.currentPos] = item;
        this.currentPos = (this.currentPos + 1) % this.maxLength;
    }

    get(i: number): T {
        if (i >= this.size) {
            throw new Error("Index out of bounds");
        }
        return this.contents[(this.currentPos + i) % this.maxLength];
    }

    get length(): number {
        return this.size;
    }

    *[Symbol.iterator]() {
        for (let i = 0; i < this.size; i++) {
            yield this.get(i);
        }
    }
}