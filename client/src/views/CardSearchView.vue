<script setup lang="ts">
import { type OracleCard } from '@/api/message';
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useSearchResultsStore } from '@/stores/search-results';
import Modal from '@/components/Modal.vue';
import { useDecksCache } from '@/cache/decks';
import { useDecksStore } from '@/stores/decks';
import LoadingButton from '@/components/LoadingButton.vue';
import { addCardsToDeck, newDeck } from '@/api/deck';
import { useAlertsStore } from '@/stores/alerts';
import StyleButton from '@/components/StyleButton.vue';


const decksCache = useDecksCache();
const decksStore = useDecksStore();
const alertsStore = useAlertsStore();
const deckName = ref('');

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

async function createDeck() {
  const deck = await newDeck(deckName.value.trim());
  decksCache.put(deck.id, deck);
  decksStore.decks[deck.id] = { name: deck.name, id: deck.id, thumbnailImage: "" };
  showDeckModal.value = false;
  searchStore.selectedDeckId = deck.id;
};

const showDeckModal = ref(false);
function openDeckModal() {
  deckName.value = '';
  showDeckModal.value = true;
}

function closeDeckModal() {
  showDeckModal.value = false;
  cardsToAddToMaindeck.value = [];
  cardsToAddToSideboard.value = [];
}

const hoveredCardId = ref<string | null>(null);
function hoverCard(id: string) {
  hoveredCardId.value = id;
}

function unhoverCard(id: string) {
  hoveredCardId.value = null;
}

const cardsToAddToSideboard = ref<string[]>([]);
const cardsToAddToMaindeck = ref<string[]>([]);

// Save state management
type SaveStatus = 'idle' | 'pending' | 'saving' | 'saved' | 'error';
const saveStatus = ref<SaveStatus>('idle');
const isSaving = ref(false);
let autosaveTimerHandle: number | null = null;
const AUTOSAVE_DELAY = 1500;

const unsavedChanges = computed(() => cardsToAddToMaindeck.value.length > 0 || cardsToAddToSideboard.value.length > 0);
const pendingCardCount = computed(() => cardsToAddToMaindeck.value.length + cardsToAddToSideboard.value.length);

function scheduleAutosave() {
  if (autosaveTimerHandle !== null) {
    clearTimeout(autosaveTimerHandle);
  }

  autosaveTimerHandle = setTimeout(() => {
    commitCardsToDeck();
  }, AUTOSAVE_DELAY);
}

function keyPress(e: KeyboardEvent) {
  if (hoveredCardId.value && searchStore.selectedDeckId !== null) {
    if (e.key === 'a') {
      cardsToAddToMaindeck.value.push(hoveredCardId.value);
      saveStatus.value = 'pending';
      scheduleAutosave();
    }
    else if (e.key === 's') {
      cardsToAddToSideboard.value.push(hoveredCardId.value);
      saveStatus.value = 'pending';
      scheduleAutosave();
    }
  }
}

async function commitCardsToDeck() {
  if (isSaving.value || !unsavedChanges.value || searchStore.selectedDeckId === null) {
    return;
  }

  // Capture current state to avoid race conditions
  const deckId = searchStore.selectedDeckId;
  const maindeckCards = [...cardsToAddToMaindeck.value];
  const sideboardCards = [...cardsToAddToSideboard.value];

  // Clear the arrays immediately to capture any new additions separately
  cardsToAddToMaindeck.value = [];
  cardsToAddToSideboard.value = [];

  isSaving.value = true;
  saveStatus.value = 'saving';
  console.log(saveStatus.value);

  try {
    const updatedDeck = await addCardsToDeck(deckId, maindeckCards, sideboardCards);
    decksCache.put(updatedDeck.id, updatedDeck);
    saveStatus.value = 'saved';
    console.log(saveStatus.value);

    // Reset to idle after showing "saved" briefly
    setTimeout(() => {
      if (saveStatus.value === 'saved') {
        saveStatus.value = 'idle';
      }
    }, 2000);

  } catch (error) {
    // Save failed - restore the cards to pending arrays
    cardsToAddToMaindeck.value.unshift(...maindeckCards);
    cardsToAddToSideboard.value.unshift(...sideboardCards);

    saveStatus.value = 'error';
    alertsStore.addAlert(
      'Save Failed',
      `Failed to autosave changes. Will retry next time.`,
      'error',
      3000
    );
  } finally {
    isSaving.value = false;
  }
}

// Watch for deck changes and prompt user
let previousDeckId = searchStore.selectedDeckId;
watch(() => searchStore.selectedDeckId, async (newDeckId, oldDeckId) => {
  if (oldDeckId && unsavedChanges.value) {
    const shouldSave = confirm(`You have ${pendingCardCount.value} unsaved card(s). Save before switching decks?`);
    if (shouldSave) {
      // Temporarily switch back to save to the correct deck
      const tempDeckId = searchStore.selectedDeckId;
      searchStore.selectedDeckId = oldDeckId;
      await commitCardsToDeck();
      searchStore.selectedDeckId = tempDeckId;
    } else {
      // Discard changes
      cardsToAddToMaindeck.value = [];
      cardsToAddToSideboard.value = [];
      saveStatus.value = 'idle';
    }
  }
  previousDeckId = newDeckId;
});

function handleBeforeUnload(e: BeforeUnloadEvent) {
  if (unsavedChanges.value) {
    e.preventDefault();
    e.returnValue = '';
  }
}

onMounted(() => {
  window.addEventListener('keydown', keyPress);
  window.addEventListener('beforeunload', handleBeforeUnload);
});

onUnmounted(() => {
  window.removeEventListener('keydown', keyPress);
  window.removeEventListener('beforeunload', handleBeforeUnload);

  if (unsavedChanges.value) {
    commitCardsToDeck();
  }

  if (autosaveTimerHandle !== null) {
    clearTimeout(autosaveTimerHandle);
  }
});

</script>

<template>
  <div id="card-search">
    <Modal :visible="showDeckModal" title="Choose Deck" @close="closeDeckModal">
      <h1>Choose Deck</h1>
      <i>While your cursor is over a card, you can press 'a' to add it to your deck or 's' to add it to your
        sideboard.</i>
      <div style="display: flex; justify-content: space-around;">
        <div style="display: flex; flex-direction: column; gap: 10px;">
          <h3>Select an existing deck</h3>
          <select v-model="searchStore.selectedDeckId">
            <option disabled :value="null">No deck selected</option>
            <option v-for="deck in decksStore.decks" :key="deck.id" :value="deck.id">{{ deck.name }}</option>
          </select>
          <router-link v-if="searchStore.selectedDeckId" :to="`/deckbuilder/${searchStore.selectedDeckId}`">
            <StyleButton small style="width: 100%;">Open in Deckbuilder</StyleButton>
          </router-link>
        </div>
        <div>
          <h3>Or, create a new deck</h3>
          <div style="display: flex; flex-direction: column; gap: 10px; align-items: center;">
            <input type="text" placeholder="Deck name" v-model="deckName" />
            <loading-button :onClick="createDeck" small style="width: 100%">Create Deck</loading-button>
          </div>
        </div>
      </div>
    </Modal>
    <div class="title-bar">
      <form id="search-bar" @submit.prevent="submitSearch">
        <span class="material-symbols-rounded">search</span>
        <input type="text" placeholder="Search for cards..." v-model="query" />
        <loading-spinner v-if="loadingResults" />
      </form>
      <router-link class="icon-button" :to="`/cards/${setCode}/advanced-search`" title="Advanced Search">
        <span class="material-symbols-rounded">tune</span>
      </router-link>
      <button class="icon-button" title="Choose deck" @click="openDeckModal">
        <span class="material-symbols-rounded">stack</span> {{ decksStore.decks[searchStore.selectedDeckId!]?.name ||
          'No Deck Selected' }}
      </button>

      <div class="save-status-container">
        <div v-if="pendingCardCount > 0" class="pending-badge" :title="`${pendingCardCount} card(s) pending save`">
          {{ pendingCardCount }}
        </div>
        <div v-if="saveStatus !== 'pending'" class="save-status-icon" :class="saveStatus">
          <loading-spinner v-if="saveStatus === 'saving'" />
          <span v-else-if="saveStatus === 'saved'" class="material-symbols-rounded success-icon" title="Cards saved">
            check_circle
          </span>
          <span v-else-if="saveStatus === 'error'" class="material-symbols-rounded error-icon" title="Save failed">
            error
          </span>
        </div>
      </div>
    </div>
    <div v-if="searchResults.length > 0" class="search-results-container">
      <router-link v-for="card in searchResults" :to="`/cards/${setCode}/${card.id}`" :key="card.id" class="card-link"
        @mouseenter="hoverCard(card.id)" @mouseleave="unhoverCard(card.id)">
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
  align-items: center;
  margin-bottom: 40px;
  justify-content: center;
  gap: 15px;
}

#search-bar {
  display: flex;
  align-items: center;
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

.material-symbols-rounded {
  color: black;
}

.icon-button {
  background: none;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px;
}

.icon-button:hover {
  background: #ddd;
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

.save-status-container {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 10px;
  border-radius: 4px;
  min-width: 90px;
}

.pending-badge {
  background: #ff9800;
  color: white;
  font-weight: bold;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  min-width: 20px;
  text-align: center;
}

.save-status-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  min-height: 24px;
}

.success-icon {
  color: #4caf50;
}

.error-icon {
  color: #f44336;
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

  .deck-button {
    margin: 0 5px;
    font-size: 20pt;
  }

  .save-status-container {
    order: -1;
    width: 100%;
    justify-content: center;
    margin-bottom: 10px;
  }

  .title-bar {
    flex-wrap: wrap;
  }
}
</style>