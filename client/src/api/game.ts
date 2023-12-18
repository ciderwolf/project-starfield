import type { OracleId } from "@/stores/board";
import { getJson } from ".";
import type { OracleCard } from "./message";

type VirtualIdsMessage = {
  virtualIds: {[key: string]: OracleId},
  oracleInfo: { [oracleId: OracleId]: OracleCard }
}

export function getVirtualIds(): Promise<VirtualIdsMessage> {
  return getJson('/game/virtual-ids');
}