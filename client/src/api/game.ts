import type { OracleId } from "@/stores/board";
import { deleteJson, deleteJsonRaw, getJson, postJson } from ".";
import type { OracleCard } from "./message";

type OracleIdMap = { [key: string]: OracleId };
type VirtualIdsMessage = {
  virtualIds: OracleIdMap,
  oracleInfo: { [oracleId: OracleId]: OracleCard }
}
type SideboardingVirtualIdsMessage = {
  main: OracleIdMap,
  side: OracleIdMap,
  oracleInfo: { [oracleId: OracleId]: OracleCard }
}

export function getVirtualIds(): Promise<VirtualIdsMessage> {
  return getJson('game/virtual-ids/library');
}

export function getVirtualScryIds(count: number): Promise<VirtualIdsMessage> {
  return getJson(`game/virtual-ids/scry?count=${count}`);
}

export function getVirtualSideboardingIds(): Promise<SideboardingVirtualIdsMessage> {
  return getJson('game/virtual-ids/sideboarding');
}

export function rollDie(sides: number): Promise<number> {
  return getJson(`game/roll-die?sides=${sides}`);
}

export function beginSpectating(gameId: string): Promise<void> {
  return postJson(`game/${gameId}/spectate`, {});
}

export async function stopSpectating(gameId: string): Promise<boolean> {
  const result = await deleteJsonRaw(`game/${gameId}/spectate`);
  return result.success;
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