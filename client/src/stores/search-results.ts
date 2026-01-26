import { searchCards, getCardDetails as fetchCardDetails } from "@/api/card";
import type { CardDetails, OracleCard } from "@/api/message";
import { defineStore } from "pinia";
import { ref } from "vue";

export type CardDetailInfo = {
    oracleCard: OracleCard,
    parts: CardDetails[],
}

export const useSearchResultsStore = defineStore('searchResults', () => {
    const queryCache = ref<Map<string, string[]>>(new Map());
    const cardCache = ref<Map<string, CardDetailInfo>>(new Map());
    const selectedDeckId = ref<string | null>(null);

    async function searchForCards(setCode: string, query: string): Promise<OracleCard[]> {
        if (queryCache.value.has(query)) {
            const cardIds = queryCache.value.get(query)!;
            return cardIds.map(id => {
                const cached = cardCache.value.get(id);
                if (!cached) {
                    throw new Error(`Card ID ${id} not found in cache`);
                }
                return cached.oracleCard;
            });
        }
        else {
            const results = await searchCards(setCode, query);
            if (results === null) {
                return [];
            }
            queryCache.value.set(query, results.map(cd => cd.oracleCard.id));
            results.forEach(cd => {
                cardCache.value.set(cd.oracleCard.id, cd);
            });
            return results.map(cd => cd.oracleCard);
        }
    }

    async function getCardDetails(id: string): Promise<CardDetailInfo> {
        if (cardCache.value.has(id)) {
            return cardCache.value.get(id)!;
        }

        // Not found in cache, fetch from API
        const result = await fetchCardDetails(id);
        cardCache.value.set(id, result);
        return result;
    }

    return { searchForCards, getCardDetails, selectedDeckId };
})