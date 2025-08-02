<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import DeckPreview from '@/components/deck/DeckPreview.vue';
import DeckStatsViz from '@/components/deck/DeckStatsViz.vue';
import LoadingButton from '@/components/LoadingButton.vue';
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import { useRoute, useRouter } from 'vue-router';
import { useDecksCache } from '@/cache/decks';
import type { Deck, DeckCard } from '@/api/message';
import { useDecksStore } from '@/stores/decks';
import { useAlertsStore } from '@/stores/alerts';


const maindeck = ref('');
const sideboard = ref('');
const deck = ref<Deck | null>(null);
const unknownCardWarningAlertId = ref('');

const route = useRoute();
const deckId = route.params.id as string;
const decks = useDecksCache();
const router = useRouter();
const alertsStore = useAlertsStore();

const mainCount = computed(() => {
  return deck.value?.maindeck.reduce((sum, card) => sum + card.count, 0) || 0;
});

const sideCount = computed(() => {
  return deck.value?.sideboard.reduce((sum, card) => sum + card.count, 0) || 0;
});

function createCardLine(card: DeckCard) {
  const line = `${card.count} ${card.name}`;
  if (card.type === '' && card.source !== '') {
    return `${line} (${card.source.toUpperCase()})`;
  }

  switch (card.conflictResolutionStrategy) {
    case 'Pinned':
    case 'Best':
      return `${line} (${card.source.toUpperCase()})`;
    case 'Default':
    case 'NoConflict':
      return line;
    default:
      const _exhaustiveCheck: never = card.conflictResolutionStrategy;
      return _exhaustiveCheck;
  }
}

onMounted(async () => {
  const decklist = await decks.get(deckId);
  if (decklist) {
    maindeck.value = decklist.maindeck.map(createCardLine).join('\n');
    sideboard.value = decklist.sideboard.map(createCardLine).join('\n');

    deck.value = decklist;
  }
});

onUnmounted(() => {
  if (unknownCardWarningAlertId.value) {
    alertsStore.removeAlert(unknownCardWarningAlertId.value);
  }
});

const submitDeckClicked = async () => {

  if (unknownCardWarningAlertId.value) {
    alertsStore.removeAlert(unknownCardWarningAlertId.value);
  }

  const main = maindeck.value.split('\n')
    .map(line => line.trim())
    .filter(line => line.length > 0);
  const side = sideboard.value.split('\n')
    .map(line => line.trim())
    .filter(line => line.length > 0);

  deck.value = await decks.set(deck.value!.id, { name: deck.value!.name, main, side });

  const store = useDecksStore();
  store.decks[deck.value!.id] = { name: deck.value!.name, id: deck.value!.id, thumbnailImage: deck.value!.thumbnailImage };

  const decklist = deck.value;
  maindeck.value = decklist.maindeck.map(createCardLine).join('\n');
  sideboard.value = decklist.sideboard.map(createCardLine).join('\n');

  const deckHasUnknownCards = deck.value.maindeck.some(card => card.image === '')
    || deck.value.sideboard.some(card => card.image === '');

  if (deckHasUnknownCards) {
    const alertId = alertsStore.addAlert('Your deck contains some unknown cards', 'Those cards have not been saved.', 'error');
    unknownCardWarningAlertId.value = alertId;
  }
}

async function submitDeckAndCloseClicked() {
  await submitDeckClicked();
  router.push('/');
}

</script>

<template>
  <div id="decklist" v-if="deck">
    <div class="title" v-if="deck !== null">
      <div style="display: flex; align-items: center; gap: 10px;">
        <router-link :to="{ name: 'home' }">
          <span class="material-symbols-rounded" id="home-button">home</span>
        </router-link>
        <h1>Decklist ({{ mainCount }}/{{ sideCount }}) <span id="decklist-title-separator">&mdash;</span></h1>
      </div>
      <input type=text v-model="deck.name" id="deck-name-input">
      <loading-button :on-click="submitDeckAndCloseClicked">Save deck and close</loading-button>
    </div>
    <div id="decklist-deckbuilder">
      <div id="deck-inputs">
        <textarea v-model="maindeck" placeholder="Maindeck (60 cards)"></textarea>
        <textarea v-model="sideboard" id="sideboard-input" placeholder="Sideboard (15 cards)"></textarea>
        <div class="submit-controls">
          <loading-button :on-click="submitDeckClicked">Save deck</loading-button>
        </div>
      </div>
      <deck-preview :deckData="deck" />
    </div>
    <hr>
    <DeckStatsViz :cards="deck.maindeck" :width="800" :height="600" />
  </div>
  <div v-else>
    <h1 class="loading-title"><loading-spinner></loading-spinner> Loading...</h1>
  </div>
</template>

<style scoped>
#decklist {
  margin-left: 10px;
  margin-right: 10px;
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
  box-sizing: border-box;
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

/* mobile styles */
@media (max-width: 850px) {

  .title {
    flex-direction: column;
    align-items: center;
    gap: 10px;
  }

  #decklist-title-separator {
    display: none;
  }

  #deck-name-input {
    width: 90%;
  }

  #decklist-deckbuilder {
    flex-direction: column;
  }

  #deck-inputs {
    max-width: 100%;
  }

  #decklist textarea {
    min-width: 100%;
  }
}
</style>