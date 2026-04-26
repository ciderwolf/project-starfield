<script setup lang="ts">
import LoadingState from '@/components/shared/LoadingState.vue';
import EmptyState from '@/components/shared/EmptyState.vue';
import LoadingButton from '@/components/shared/LoadingButton.vue';
import StyleButton from '@/components/shared/StyleButton.vue';
import Modal from '@/components/shared/Modal.vue';
import ItemCard from '@/components/home/ItemCard.vue';
import ItemCardGrid from '@/components/home/ItemCardGrid.vue';
import { useCubesCache } from '@/cache/cubes';
import { useCubesStore } from '@/stores/cubes';
import { useDataStore } from '@/stores/data';
import { importCubeFromCubeCobra } from '@/api/cube';
import { computed, ref } from 'vue';
import IconButton from '@/components/shared/IconButton.vue';
import MenuNavigationBlade from '@/components/home/MenuNavigationBlade.vue';
import type { ComponentExposed } from 'vue-component-type-helpers';


const cubesCache = useCubesCache();
const cubes = useCubesStore();
const data = useDataStore();

const isCreatingCube = ref(false);
const cubeCobraId = ref('')

const myCubes = computed(() =>
  Object.values(cubes.cubes).filter(c => c.ownerId === data.userId),
);
const publicCubes = computed(() =>
  Object.values(cubes.cubes).filter(c => c.ownerId !== data.userId),
);

async function importCube() {
  const cube = await importCubeFromCubeCobra(cubeCobraId.value);
  cubesCache.put(cube.id, cube);
  cubes.cubes[cube.id] = {
    id: cube.id,
    name: cube.name,
    thumbnailImage: cube.thumbnailImage,
    ownerId: cube.ownerId,
  };

  isCreatingCube.value = false;
}


function showCreateCubeModal(e: MouseEvent) {
  isCreatingCube.value = true;
}

function closeModal() {
  isCreatingCube.value = false;
}

const menuNavigationBlade = ref<ComponentExposed<typeof MenuNavigationBlade>>();
function showMenuBlade() {
  menuNavigationBlade.value?.showMenu();
}
</script>

<template>
  <div id="cubes">
    <MenuNavigationBlade ref="menuNavigationBlade" />
    <Modal :visible="isCreatingCube" @close="closeModal">
      <div style="display: flex; flex-direction: column; gap: 1em;">
        <h2>Import From Cube Cobra</h2>
        <label>
          Cube ID: <input v-model="cubeCobraId" placeholder="Enter Cube Cobra ID" />
        </label>
        <LoadingButton :on-click="importCube">Import Cube</LoadingButton>
      </div>
    </Modal>
    <div class="title">
      <h1 class="title-text">
        <IconButton @click="showMenuBlade" icon="menu" />
        Cubes
      </h1>
      <style-button @click="showCreateCubeModal" small>+ New cube</style-button>
    </div>
    <div v-if="!cubes.isLoaded">
      <LoadingState message="Loading cubes..." />
    </div>
    <template v-else>
      <section>
        <h2>My cubes</h2>
        <ItemCardGrid v-if="myCubes.length > 0">
          <ItemCard v-for="cube in myCubes" :key="cube.id" :title="cube.name" :image="cube.thumbnailImage"
            :to="{ name: 'cube', params: { id: cube.id } }">
          </ItemCard>
        </ItemCardGrid>
        <EmptyState v-else title="You have no cubes." subtitle="Click on '+ New Cube' to create one." />
      </section>
      <section>
        <h2>Public cubes</h2>
        <ItemCardGrid v-if="publicCubes.length > 0">
          <ItemCard v-for="cube in publicCubes" :key="cube.id" :title="cube.name" :image="cube.thumbnailImage"
            :to="{ name: 'cube', params: { id: cube.id } }">
          </ItemCard>
        </ItemCardGrid>
        <EmptyState v-else title="No public cubes available." subtitle="" />
      </section>
    </template>
  </div>
</template>

<style scoped>
#cubes {
  padding: 0 2em;
  display: flex;
  flex-direction: column;
}

.title {
  display: flex;
  gap: var(--space-xl);
  align-items: center;
}

.title-text {
  display: flex;
  gap: var(--space-lg);
  align-items: center;
}

.delete-button {
  width: 100%;
}
</style>