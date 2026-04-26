<script setup lang="ts">
import { getDraftStrategy, syncFromCubeCobra, postDraftStrategy, deleteCube } from '@/api/cube';
import { type Cube, type DraftStrategy } from '@/api/message';
import { useCubesCache } from '@/cache/cubes';
import ColorCombinationChooser from '@/components/admin/ColorCombinationChooser.vue';
import HelpPopover from '@/components/admin/HelpPopover.vue';
import StarRating from '@/components/admin/StarRating.vue';
import CardPreview from '@/components/deck/CardPreview.vue';
import EmptyState from '@/components/EmptyState.vue';
import CardThumbnail from '@/components/game/modal/CardThumbnail.vue';
import IconButton from '@/components/IconButton.vue';
import LoadingButton from '@/components/LoadingButton.vue';
import LoadingIconButton from '@/components/LoadingIconButton.vue';
import LoadingState from '@/components/LoadingState.vue';
import PairwiseComparison from '@/components/admin/PairwiseComparison.vue';
import RichSelector from '@/components/RichSelector.vue';
import { useCubesStore } from '@/stores/cubes';
import { Tabs, Tab } from 'vue3-tabs-component';
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import BackButton from '@/components/BackButton.vue';

const route = useRoute();
const router = useRouter();
const cubeId = route.params.id as string;
const cubes = useCubesCache();
const cubeStore = useCubesStore();


const cube = ref<Cube | null>(null);
const draftStrategy = ref<DraftStrategy | null>(null);

onMounted(async () => {
  const cubeList = await cubes.get(cubeId);
  if (cubeList) {
    cube.value = cubeList;
    for (const card of cube.value!.cards) {
      cardRatings.value[card.id] = 0;
    }

    const strategy = await getDraftStrategy(cubeId);
    if (strategy !== null) {
      draftStrategy.value = strategy;
      cardRatings.value = strategy.cards.map((card) => ({ id: card.id, rating: card.synergy_score })).reduce((acc, { id, rating }) => {
        acc[id] = rating;
        return acc;
      }, {} as Record<string, number>);
      combinations.value = strategy.color_combinations;
      fixingIds.value = strategy.mana_fixers;
    }
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
    ownerId: cubeResponse.ownerId,
  };
}

async function saveDraftStrategy() {
  if (!cube.value || !draftStrategy.value) return;

  draftStrategy.value.color_combinations = combinations.value;
  draftStrategy.value.mana_fixers = fixingIds.value;
  draftStrategy.value.cards = draftStrategy.value.cards.map(card => ({
    ...card,
    synergy_score: cardRatings.value[card.id] || 0,
  }));
  await postDraftStrategy(cubeId, draftStrategy.value);
}

async function deleteCubeClicked() {
  if (!cube.value) return;
  if (!window.confirm(`Delete cube "${cube.value.name}"? This cannot be undone.`)) return;
  const deleted = await deleteCube(cubeId);
  if (deleted) {
    cubes.remove(cubeId);
    delete cubeStore.cubes[cubeId];
    router.push({ name: 'cubes' });
  }
}

const combinations = ref<string[][]>([]);
const fixingIds = ref<string[]>([]);

const fixings = computed(() => {
  if (!cube.value) return [];
  const fixingSet = new Set(fixingIds.value);
  return cube.value.cards.filter(c => fixingSet.has(c.id));
});
function removeFixing(cardId: string) {
  const index = fixingIds.value.findIndex(id => id === cardId);
  if (index !== -1) {
    fixingIds.value.splice(index, 1);
  }
}

function addFixing(cardId: string) {
  if (!fixingIds.value.includes(cardId)) {
    fixingIds.value.push(cardId);
  }
}

const cardRatings = ref<Record<string, number>>({});
const ratingCardFilter = ref('');
const filteredRatingCards = computed(() => {
  if (!cube.value) return [];
  const filter = ratingCardFilter.value.toLowerCase();
  return cube.value.cards.filter(card => card.name.toLowerCase().includes(filter));
});
</script>

<template>
  <div id="cube-admin" v-if="cube">
    <div style="display: flex; align-items: center;">
      <BackButton />
      <h1>{{ cube.name }}</h1>
    </div>
    <h2 class="section-title">Cube info</h2>
    <section class="panel">
      <div class="cube-header">
        <p>Source: CubeCobra</p>
        <a :href="`https://cubecobra.com/cube/list/${cube.remoteId}`" target="_blank" rel="noopener noreferrer"
          class="link">View
          online
          <span class="material-symbols-rounded">open_in_new</span></a>
        <LoadingButton :on-click="syncCubeClicked" small>Sync latest cards</LoadingButton>
        <LoadingButton type="danger" :on-click="deleteCubeClicked" small>Delete cube</LoadingButton>
      </div>
    </section>
    <h2 class="section-title">Set up draft strategy
      <LoadingIconButton :on-click="saveDraftStrategy" icon="save" size="sm" v-if="draftStrategy" />
    </h2>
    <section class="panel">
      <h3 class="section-title">
        Color combinations
        <IconButton icon="add" @click="combinations.push([])" size="xs" />
        <HelpPopover text="Which color combinations are supported in the draft format? The draft AI will aim for these
      combinations when making picks." />
      </h3>
      <div v-for="(_, index) in combinations" :key="index" class="archetype-color card" v-if="combinations.length > 0">
        <ColorCombinationChooser v-model="combinations[index]" />
        <IconButton icon="close" @click="combinations.splice(index, 1)" />
      </div>
      <EmptyState v-else title=""
        subtitle="No color combinations defined. Click the + button to add a color combination." />
    </section>
    <section class="panel">
      <h3 class="section-title">Fixing cards
        <HelpPopover text="Which cards cards help with mana fixing? This includes lands and non-land fixing like mana rocks and mana
      filtering abilities." />
      </h3>
      <div class="fixing-cards">
        <RichSelector selection-mode="multiple" v-model:selected-keys="fixingIds" :items="cube.cards">
          <template #item="{ item }">
            <CardPreview :card="{ ...item, count: undefined }" />
          </template>
        </RichSelector>
        <div v-if="fixingIds.length > 0" class="fixing-selections">
          <div v-for="card in fixings" :key="card.id" class="fixing-selection">
            <CardThumbnail :card="card" />
            <IconButton icon="close" @click="removeFixing(card.id)" class="card-thumbnail-button" />
          </div>
        </div>
        <EmptyState v-if="fixingIds.length === 0" title=""
          subtitle="No fixing cards selected. Select cards from the dropdown to designate them as fixing cards." />
      </div>
    </section>
    <section class="panel">
      <Tabs :options="{ useUrlFragment: false }">
        <Tab name="Card ratings">
          <h3 class="section-title">Card ratings
            <HelpPopover
              text="How good is each card on a 5-star scale? The AI will use this information to weight its picks." />
          </h3>
          <input class="search-input" type="text" placeholder="Search for cards..." v-model="ratingCardFilter">
          <div class="card-ratings">
            <div v-for="card in filteredRatingCards" :key="card.id" class="card-rating">
              <CardThumbnail :card="{ ...card, tokens: null }" />
              <IconButton icon="add" @click="addFixing(card.id)" title="Mark card as mana-fixing"
                class="card-thumbnail-button add-fixing-button" />
              <StarRating v-model="cardRatings[card.id]" />
            </div>
          </div>
        </Tab>
        <Tab name="Head to head">
          <h3 class="section-title">Head to head
            <HelpPopover text="Pick the stronger card in this draft format. Each comparison nudges the
        star ratings up or down by about half a star." />
          </h3>
          <PairwiseComparison :cards="cube.cards" v-model:ratings="cardRatings" />
        </Tab>
      </Tabs>
    </section>
  </div>
  <LoadingState v-else message="Loading cube..." full-page />
</template>

<style scoped>
#cube-admin {
  width: 80%;
  margin: 0 auto;
  padding: var(--space-xl) var(--space-md);
}

.panel {
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: 0.75rem;
  padding: var(--space-lg) var(--space-xl);
  margin: 0 0 var(--space-xl);
  box-shadow: var(--shadow-md);
}

.panel .section-title {
  margin-top: 0;
  padding-bottom: var(--space-sm);
  border-bottom: 1px solid var(--color-border);
  margin-bottom: var(--space-md);
}

.cube-header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  justify-content: space-between;
}

.link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--color-primary-text);
}

.link:hover {
  text-decoration: underline;
}

.section-title {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.archetype-color {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: var(--space-sm);
}

.card {
  align-items: center;
  padding: 0.5rem;
  border: 1px solid var(--color-gray-300);
  background: var(--color-gray-200);
  border-radius: 0.5rem;
  margin: 0.5rem auto;
  gap: var(--space-md);
  width: 80%;
}

.archetype-color .color-combination-chooser {
  flex-grow: 1;
}

.fixing-selections {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
  justify-content: center;
}

.fixing-selection {
  position: relative;
  display: inline-block;
}

.card-thumbnail-button {
  position: absolute;
  top: 25%;
  right: 50%;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 50%;
  transform: translate(50%, 0);
  color: red;
}

.card-ratings {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 20px;
}

.search-input {
  margin-bottom: var(--space-md);
  padding: var(--space-sm);
  width: 100%;
  border-radius: var(--radius-md);
}

.card-rating {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.add-fixing-button {
  display: none;
  color: green;
}

.card-rating:hover .add-fixing-button {
  display: block;
}
</style>