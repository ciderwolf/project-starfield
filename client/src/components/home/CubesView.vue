<script setup lang="ts">
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import LoadingButton from '@/components/LoadingButton.vue';
import StyleButton from '@/components/StyleButton.vue';
import Modal from '@/components/Modal.vue';
import { useCubesCache } from '@/cache/cubes';
import { useCubesStore } from '@/stores/cubes';
import { deleteCube, importCubeFromCubeCobra } from '@/api/cube';
import { ref } from 'vue';


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

</script>

<template>
  <div id="cubes">
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
      <h2>Cubes</h2>
      <style-button @click="showCreateCubeModal" small>+ New cube</style-button>
    </div>
    <div class="empty-container-title" v-if="!cubes.isLoaded">
      <h3 class="loading-cubes-title">
        <LoadingSpinner /> Loading cubes...
      </h3>
    </div>
    <div class="cube-cards" v-else-if="Object.keys(cubes.cubes).length > 0">
      <div v-for="cube in cubes.cubes" :key="cube.id">
        <router-link :to="{ name: 'cube', params: { id: cube.id } }" class="cube-card">
          <img :alt="`${cube.name} Thumbnail`" class="cube-card-thumbnail" :src="cube.thumbnailImage" />
          <h3 class="cube-card-title">{{ cube.name }}</h3>
          <loading-button type="danger" class="delete-cube-button" :on-click="(e) => deleteCubeClicked(e, cube.id)">
            Delete cube</loading-button>
        </router-link>
      </div>
    </div>
    <div v-else class="empty-container-title">
      <h3>You have no cubes.</h3>
      <p>Click on '+ New Cube' to create one.</p>
    </div>
  </div>
</template>

<style scoped>
.title {
  display: flex;
  gap: 20px;
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

.cube-cards {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  background-color: #eee;
  border: 1px solid #ddd;
  border-radius: 5px;
  max-width: 80%;
  width: fit-content;
  margin: 0 auto;
  padding: 1em 10%;
  min-width: 250px;
  margin-bottom: 200px;
}

.cube-card {
  display: flex;
  flex-direction: column;
  text-decoration: none;
  text-align: center;
  color: #000;
  margin: 0.5em;
  box-shadow: 0 0 5px 0 rgba(0, 0, 0, 0.5);
  width: 250px;
  background: white;
}

.cube-card:hover {
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.5);
}

.delete-cube-button {
  display: none;
  margin: 0 0.5em 0.5em 0.5em;
}

.cube-card:hover .delete-cube-button {
  display: block;
}

.cube-card-thumbnail {
  height: 182.5px;
  object-fit: fill;
}

.cube-card-title {
  margin: 0.75em;
}
</style>