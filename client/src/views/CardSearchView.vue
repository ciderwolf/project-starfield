<script setup lang="ts">
import { type OracleCard } from '@/api/message';
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useSearchResultsStore } from '@/stores/search-results';

const route = useRoute();
const router = useRouter();
const setCode = route.params.set as string;
const query = ref(route.query.q as string || '');
const searchResults = ref<OracleCard[]>([]);
const loadingResults = ref(true);
const searchStore = useSearchResultsStore();
const flips = reactive<Record<string, boolean>>({});


onMounted(() => {
  submitSearch();
})

async function submitSearch() {
  loadingResults.value = true;
  searchResults.value = await searchStore.searchForCards(setCode, query.value);
  router.replace({ query: { q: query.value } });
  loadingResults.value = false;
}

function toggleFlip(e: MouseEvent, cardId: string) {
  e.preventDefault();
  flips[cardId] = !flips[cardId];
}

</script>

<template>
  <div id="card-search">
    <div class="title-bar">
      <form id="search-bar" @submit.prevent="submitSearch">
        <span class="material-symbols-rounded">search</span>
        <input type="text" placeholder="Search for cards..." v-model="query" />
        <LoadingSpinner v-if="loadingResults" />
      </form>
    </div>
    <div v-if="searchResults.length > 0" class="search-results-container">
      <router-link v-for="card in searchResults" :to="`/cards/${setCode}/${card.id}`" :key="card.id" class="card-link">
        <img :src="flips[card.id] && card.backImage ? card.backImage : card.image" :alt="card.name" :key="card.id"
          class="card-image"></img>
        <span class="flip-card-button material-symbols-rounded" :class="{ flipped: flips[card.id] }"
          @click="toggleFlip($event, card.id)" v-if="card.backImage">
          360
        </span>
      </router-link>
    </div>
    <div v-else-if="!loadingResults">
      <h4>No cards found.</h4>
    </div>
  </div>
</template>

<style scoped>
#card-search {
  padding-top: 20px;
}

.title-bar {
  display: flex;
  flex-direction: row;
  align-items: baseline;
  margin-bottom: 20px;
  justify-content: center;
}

#search-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding-left: 5px;
  border: 1px solid #ccc;
  max-width: 500px;
  min-width: 300px;
  width: 50%;
}

#search-bar input {
  flex: 1;
  padding: 10px;
  font-size: 16px;
  border: none;
  outline: none;
}

.search-results-container {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  width: 80%;
  margin: auto;
}

.card-link {
  position: relative;
  min-width: 150px;
  width: 24%;
  margin: max(0.5%, 5px);
}

.card-link img {
  width: 100%;
}


.flip-card-button {
  cursor: pointer;
  user-select: none;
  font-size: 24pt;
  color: #555;
  background: #eeea;
  border: 2px solid #555;
  border-radius: 50%;
  padding: 5px;
  position: absolute;
  top: 12.5%;
  left: 50%;
  transform: translateX(-50%) rotate(180deg);
  transition: 0.3s color, background;
}

.flip-card-button.flipped {
  transform: translateX(-50%);
  color: #eeea;
  background: #555a;
  border: 2px solid #eeea;
}

.flip-card-button:hover {
  background: #eee;
}

.flip-card-button.flipped:hover {
  background: #555;
}

h4 {
  color: #333;
  font-style: italic;
  text-align: center;
  margin-top: 5%;
}

@media screen and (max-width: 600px) {
  .search-results-container {
    width: 100%;
    gap: 8px;
  }

  .card-link {
    width: calc(50% - 8px);
    margin: 0;
    min-width: unset;
  }

  #search-bar {
    width: 90%;
    min-width: unset;
  }
}
</style>