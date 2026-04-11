export interface CardWithManaCost {
    manaCost: string;
}

export interface CardWithType extends CardWithManaCost {
    type: string;
}

export function cardSymbolGroups(card: CardWithManaCost): string[][] {
    const cost = card.manaCost;
    if (!cost) return [];

    const parts = cost.split(' // ');
    const result: string[][] = [];
    for (const part of parts) {
        const matches = part.matchAll(/\{([^\}]+)\}/g);
        const partSymbols = Array.from(matches).map(match => match[1].replace('/', '').toLowerCase());
        if (partSymbols.length > 0) {
            result.push(partSymbols);
        }
    }
    return result;
}

export function cardSymbols(card: CardWithManaCost): string[] {
    return cardSymbolGroups(card).flatMap(symbol => symbol);
}

export function cardColors(card: CardWithManaCost): string {
    const symbols = cardSymbols(card);
    const colors = new Set<string>();
    for (const symbol of symbols) {
        if (symbol.includes('w')) colors.add('w');
        if (symbol.includes('u')) colors.add('u');
        if (symbol.includes('b')) colors.add('b');
        if (symbol.includes('r')) colors.add('r');
        if (symbol.includes('g')) colors.add('g');
    }
    return colors.size > 0 ? Array.from(colors).join('') : 'c';
}

export function cardFrameColor(card: CardWithType): string {
    const colors = cardColors(card);
    if (colors.length === 1 && colors[0] == 'c') {
        if (card.type === 'Artifact') {
            return 'artifact';
        } else if (card.type == 'Land') {
            return 'land';
        } else {
            return 'colorless';
        }
    }
    else if (colors.length === 1) {
        return colors[0];
    }
    else if (colors.length > 1) {
        return 'multicolor';
    }
    else {
        return 'colorless';
    }
}
