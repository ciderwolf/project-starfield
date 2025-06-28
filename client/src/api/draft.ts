import { getJson, postJson } from ".";

export interface DraftSet {
    id: string;
    name: string;
    image: string;
    setType: string;
}

export function getSets(): Promise<DraftSet[]> {
    return getJson("draft/sets");
}

export function downloadSet(code: string): Promise<DraftSet> {
    return postJson(`draft/sets/${code}`, {});
}