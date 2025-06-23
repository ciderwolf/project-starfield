<script setup lang="ts">
import { onMounted, ref } from 'vue';
import DeckPreview from '@/components/deck/DeckPreview.vue';
import LoadingButton from '@/components/LoadingButton.vue';
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import { useRoute, useRouter } from 'vue-router';
import type { Cube } from '@/api/message';
import { useCubesCache } from '@/cache/cubes';
import { syncFromCubeCobra } from '@/api/cube';
import { useCubesStore } from '@/stores/cubes';

const cube = ref<Cube | null>(null);

const route = useRoute();
const cubeId = route.params.id as string;
const cubes = useCubesCache();
const cubeStore = useCubesStore();

onMounted(async () => {
  const cubeList = await cubes.get(cubeId);
  if (cubeList) {
    cube.value = cubeList;
  }
});

async function syncCubeClicked() {
  const cubeResponse = await syncFromCubeCobra(cubeId);
  cubes.put(cubeResponse.id, cubeResponse);
  cube.value = cubeResponse;
  cubeStore.cubes[cubeResponse.id] = {
    id: cubeResponse.id,
    name: cubeResponse.name,
    thumbnailImage: cubeResponse.thumbnailImage,
  };
}

</script>

<template>
  <div id="decklist" v-if="cube">
    <div class="title" v-if="cube !== null">
      <router-link :to="{ name: 'home' }">
        <span class="material-symbols-rounded" id="home-button">home</span>
      </router-link>
      <h1>{{ cube.name }}</h1>
      <loading-button :on-click="syncCubeClicked">Re-sync from Cube Cobra</loading-button>
    </div>
    <deck-preview :include-sideboard="false" :deckData="{ maindeck: cube.cards, sideboard: [], ...cube }" />
  </div>
  <div v-else>
    <h1 class="loading-title"><loading-spinner /> Loading...</h1>
  </div>
</template>

<style scoped>
#decklist {
  margin-left: 10px;
}

#home-button {
  color: black;
  cursor: pointer;
  font-size: 2em;
  padding: 10px;
}

#home-button:hover {
  background-color: rgba(0, 0, 0, 0.1);
}

.loading-title {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  margin-top: 25%;
}

.title {
  display: flex;
  flex-direction: row;
  align-items: center;
}

#deck-name-input {
  font-size: 1.75em;
}

#decklist-deckbuilder {
  display: flex;
  flex-direction: row;
}

#deck-inputs {
  display: flex;
  flex-direction: column;
  max-width: min-content;
}

#decklist textarea {
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  font-size: 1em;
  min-width: 300px;
  min-height: 400px;
  margin: 5px 0;
}

#decklist #sideboard-input {
  min-height: 150px;
}

.submit-controls {
  display: flex;
  flex-direction: row;
  justify-content: center;
  width: 100%;
}

.submit-controls button {
  margin: 5px;
  width: 100%;
}
</style>