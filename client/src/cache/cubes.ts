import type { Cube } from "@/api/message";
import { createAsyncCache } from ".";
import { fetchCube, submitCube } from "@/api/cube";


export const useCubesCache = createAsyncCache(() => {
  return {
    load: async (id: string) => {
      return await fetchCube(id);
    },
    write: (id: string, value: Cube) => {
      return submitCube(id, value);
    }
  }
});