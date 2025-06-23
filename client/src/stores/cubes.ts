import { reactive, ref } from 'vue'
import { defineStore } from 'pinia'
import { type DeckListing } from '@/api/message';
import { fetchCube, getCubes } from '@/api/cube';

export const useCubesStore = defineStore('cubes', () => {

  const cubes = reactive<{ [id: string]: DeckListing }>({});
  const isLoaded = ref(false);

  async function getCube(id: string) {
    const cached = cubes[id];
    if (cached) {
      return cached;
    }
    const cube = await fetchCube(id);
    return cube;
  }

  async function reloadCubes() {
    isLoaded.value = false;
    const cubesList = await getCubes();
    for (const cube of cubesList) {
      cubes[cube.id] = cube;
    }
    isLoaded.value = true;
  }

  return { cubes, isLoaded, getCube, reloadCubes }
})
