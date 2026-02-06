<script setup lang="ts">
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import LoadingButton from '@/components/LoadingButton.vue';
import StyleButton from '@/components/StyleButton.vue';
import Modal from '@/components/Modal.vue';
import ItemCard from '@/components/home/ItemCard.vue';
import ItemCardGrid from '@/components/home/ItemCardGrid.vue';
import { useCubesCache } from '@/cache/cubes';
import { useCubesStore } from '@/stores/cubes';
import { deleteCube, importCubeFromCubeCobra } from '@/api/cube';
import { ref } from 'vue';
import IconButton from '@/components/IconButton.vue';
import MenuNavigationBlade from '@/components/home/MenuNavigationBlade.vue';
import type { ComponentExposed } from 'vue-component-type-helpers';


const cubesCache = useCubesCache();
const cubes = useCubesStore();

const isCreatingCube = ref(false);
const cubeCobraId = ref('')

async function deleteCubeClicked(e: MouseEvent, id: string) {
  e.preventDefault();
  const deleted = await deleteCube(id);
  if (deleted) {
    cubesCache.remove(id);
    delete cubes.cubes[id];
  }
}

async function importCube() {
  const cube = await importCubeFromCubeCobra(cubeCobraId.value);
  cubesCache.put(cube.id, cube);
  cubes.cubes[cube.id] = {
    id: cube.id,
    name: cube.name,
    thumbnailImage: cube.thumbnailImage,
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
    <div class="empty-container-title" v-if="!cubes.isLoaded">
      <h3 class="loading-cubes-title">
        <LoadingSpinner /> Loading cubes...
      </h3>
    </div>
    <ItemCardGrid v-else-if="Object.keys(cubes.cubes).length > 0">
      <ItemCard v-for="cube in cubes.cubes" :key="cube.id" :title="cube.name" :image="cube.thumbnailImage"
        :to="{ name: 'cube', params: { id: cube.id } }">
        <template #actions>
          <LoadingButton class="delete-button" type="danger" :on-click="(e) => deleteCubeClicked(e, cube.id)">
            Delete cube
          </LoadingButton>
        </template>
      </ItemCard>
    </ItemCardGrid>
    <div v-else class="empty-container-title">
      <h3>You have no cubes.</h3>
      <p>Click on '+ New Cube' to create one.</p>
    </div>
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
  gap: 20px;
  align-items: center;
}

.title-text {
  display: flex;
  gap: 12px;
  align-items: center;
}

.empty-container-title {
  margin-top: 1em;
  text-align: center;
  color: #333;
  font-style: italic;
}

.loading-cubes-title {
  display: flex;
  align-items: center;
  justify-content: center;
}

.delete-button {
  width: 100%;
}
</style>