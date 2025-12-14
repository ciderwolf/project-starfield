import type { CardDetailInfo } from "@/stores/search-results";
import { getJson } from ".";
import type { CardDetails, OracleCard } from "./message";

export async function searchCards(setCode: string, query: string): Promise<CardDetailInfo[]> {
    const response = await getJson(`/card/${setCode}/search?q=${encodeURIComponent(query)}`);
    return response as CardDetailInfo[];
}



export async function getCardDetails(id: string): Promise<CardDetailInfo> {
    const response = await getJson(`/card/${id}/parts`);
    return response as CardDetailInfo;
}