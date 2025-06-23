import { postJson, getJson, deleteJson, putJson } from ".";
import type { Cube } from "./message";

export async function fetchCube(id: string) {
  return await getJson(`cube/${id}`);
}

export async function submitCube(id: string, cards: Cube) {
  return await postJson(`cube/${id}`, cards);
}

export async function importCubeFromCubeCobra(cubeCobraId: string): Promise<Cube> {
  return await postJson(`cube/import/cube-cobra?id=${cubeCobraId}`, {});
}

export async function syncFromCubeCobra(cubeId: string): Promise<Cube> {
  return await putJson(`cube/sync/cube-cobra?id=${cubeId}`);
}

export async function deleteCube(cubeId: string) {
  return await deleteJson(`cube/${cubeId}`);
}

export async function getCubes() {
  return await getJson("cube");
}