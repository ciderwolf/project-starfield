import type { OracleId } from "@/stores/board";
import { getJson } from ".";
import type { OracleCard } from "./message";

type VirtualIdsMessage = {
  virtualIds: {[key: string]: OracleId},
  oracleInfo: { [oracleId: OracleId]: OracleCard }
}

export function getVirtualIds(): Promise<VirtualIdsMessage> {
  return getJson('game/virtual-ids/library');
}

export function getVirtualScryIds(count: number): Promise<VirtualIdsMessage> {
  return getJson(`game/virtual-ids/scry?count=${count}`);
}

export function searchForTokens(name?: string, type?: string, colors?: string, text?: string, pt?: string): Promise<OracleCard[]> {
  const params = { name, type, colors, text, pt };
  
  const searchParams = new URLSearchParams();
  for(const [key, value] of Object.entries(params)) {
    if(value) {
      searchParams.append(key, value);
    }
  }

  return getJson(`game/search/tokens?${searchParams.toString()}`);
}

export function searchForCards(name: string): Promise<OracleCard[]> {
  return getJson(`game/search/cards?name=${name}`);
}