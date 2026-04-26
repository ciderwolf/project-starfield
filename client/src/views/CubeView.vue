<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import DeckPreview from '@/components/deck/DeckPreview.vue';
import LoadingState from '@/components/LoadingState.vue';
import IconButton from '@/components/IconButton.vue';
import { useRoute, useRouter } from 'vue-router';
import type { Cube } from '@/api/message';
import { useCubesCache } from '@/cache/cubes';
import { useDataStore } from '@/stores/data';
import BackButton from '@/components/BackButton.vue';

const cube = ref<Cube | null>(null);

const route = useRoute();

const cubeId = route.params.id as string;
const cubes = useCubesCache();
const dataStore = useDataStore();

const isMine = computed(() => cube.value?.ownerId === dataStore.userId);

onMounted(async () => {
  const cubeList = await cubes.get(cubeId);
  if (cubeList) {
    cube.value = cubeList;
  }
});


</script>

<template>
  <div id="decklist" v-if="cube">
    <div class="title" v-if="cube !== null">
      <BackButton />
      <h1>{{ cube.name }}</h1>
      <router-link :to="`/cubes/${cube.id}/admin`" v-if="isMine">
        <IconButton icon="settings" size="xl" />
      </router-link>
    </div>
    <deck-preview :include-sideboard="false" :deckData="{ maindeck: cube.cards, sideboard: [], ...cube }" />
  </div>
  <div v-else>
    <LoadingState full-page />
  </div>
</template>

<style scoped>
#decklist {
  margin-left: 10px;
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
  font-family: var(--font-family);
  font-size: var(--font-size-sm);
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