import type { CardDetailInfo } from "@/stores/search-results";
import { getJson, getJsonRaw } from ".";
import { useAlertsStore } from "@/stores/alerts";

type SearchResponse = {
    results: CardDetailInfo[];
    warnings: string[];
}

const searchAlertIds: string[] = [];

export async function searchCards(setCode: string, query: string): Promise<CardDetailInfo[] | null> {
    const alerts = useAlertsStore();
    for (const alertId of searchAlertIds) {
        alerts.removeAlert(alertId);
    }

    const response = await getJsonRaw<SearchResponse>(`/card/${setCode}/search?q=${encodeURIComponent(query)}`);
    if (response.success) {
        if (response.content.warnings) {
            response.content.warnings.forEach(warning => {
                const alertId = alerts.addAlert('Part of your search query was ignored', warning, 'warning');
                searchAlertIds.push(alertId);
            });
        }
        return response.content.results;
    }
    else {
        const alertId = alerts.addAlert('Search syntax error', response.message, 'error');
        searchAlertIds.push(alertId);
        return null;
    }
}



export async function getCardDetails(id: string): Promise<CardDetailInfo> {
    const response = await getJson(`/card/${id}/parts`);
    return response as CardDetailInfo;
}