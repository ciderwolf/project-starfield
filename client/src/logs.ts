import type { AccountableAction, BoardDiffEvent } from "./api/message";
import { extractPlayerIndex, useBoardStore, type CardId } from "./stores/board";
import { findZoneByName } from "./zones";

export function getEventMessage(event: BoardDiffEvent): string | null {
    const owner = identifyEventOwner(event);
    const board = useBoardStore();
    const name = board.players[owner].name;

    switch (event.type) {
        case "change_attribute":
        case "change_position":
        case "reveal_card":
            return null;
        case "change_index": {
            if (((event.newIndex & 0b11110000) >> 4) === 0 && event.newIndex === 0) {
                return `${name} moved a card to the bottom of their deck`;
            } else {
                return `${name} moved ${event.card} to index ${event.newIndex}`;
            }
        }
        case "destroy_card":
            return `${name} destroyed a card`;
        case "change_zone":
            return `${name} moved a card to their ${findZoneByName(event.newZone)!.name}`;
        case "hide_card":
            return `${name} hid a card from ${getPlayerReveals(event.players)}`;
        case "create_card":
            return `${name} created a card`;
        case "change_player_attribute":
            return `${name} changed their ${event.attribute.toLowerCase()} to ${event.newValue}`;
        case "scoop_deck":
            return `${name} scooped their deck`;
        case "shuffle_deck":
            return `${name} shuffled their deck`;
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
    }
}

function identifyCardOwner(cardId: CardId): string {
    const idx = extractPlayerIndex(cardId);
    const board = useBoardStore();
    return Object.values(board.players).find(player => player.index === idx)!.id;
}

function getPlayerReveals(players: string[]): string {
    if (players.length === 0) {
        return "everyone";
    }
    const board = useBoardStore();
    return players.map(player => board.players[player].name).join(", ");
}

export function getAccountabilityMessage(action: AccountableAction, ownerName: string, payload: number, player: string | null): string {
    switch (action) {
        case 'FIND_CARD':
            return `${ownerName} is searching their deck for a card`;
        case 'REVEAL':
            const board = useBoardStore();
            return `${ownerName} is revealing a card to ${board.players[player??""]?.name ?? "everyone"}`;
        case 'SCRY':
            return `${ownerName} is looking at the top ${payload} card${payload === 1 ? "" : "s"} of their deck`;
        case 'SIDEBOARD':
            return `${ownerName} is sideboarding`;
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
        for(let i = 0; i < this.size; i++) {
            yield this.get(i);
        }
    }
}