<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import DeckPreview from '@/components/deck/DeckPreview.vue';
import DeckStatsViz from '@/components/deck/DeckStatsViz.vue';
import LoadingButton from '@/components/LoadingButton.vue';
import LoadingState from '@/components/LoadingState.vue';
import IconButton from '@/components/IconButton.vue';
import { useRoute, useRouter } from 'vue-router';
import { useDecksCache } from '@/cache/decks';
import type { Deck, DeckCard } from '@/api/message';
import { useDecksStore } from '@/stores/decks';
import { useAlertsStore } from '@/stores/alerts';
import DeckCardEditor from '@/components/deck/DeckCardEditor.vue';
import ButtonGroup from '@/components/ButtonGroup.vue';
import BackButton from '@/components/BackButton.vue';


const maindeck = ref('');
const sideboard = ref('');
const deck = ref<Deck | null>(null);
const unknownCardWarningAlertId = ref('');

const route = useRoute();
const deckId = route.params.id as string;
const decks = useDecksCache();
const router = useRouter();
const alertsStore = useAlertsStore();

const viewMode = computed({
  get: () => (route.query.display as 'columns' | 'list') || 'list',
  set: (value: 'columns' | 'list') => {
    router.replace({ query: { ...route.query, display: value } });
  }
});

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
    deck.value = decklist;
    updateDeckText();
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
  store.decks[deck.value!.id] = {
    name: deck.value!.name,
    id: deck.value!.id,
    thumbnailImage: deck.value!.thumbnailImage,
    ownerId: deck.value!.ownerId
  };

  updateDeckText();

  const deckHasUnknownCards = deck.value.maindeck.some(card => card.image === '')
    || deck.value.sideboard.some(card => card.image === '');

  if (deckHasUnknownCards) {
    const alertId = alertsStore.addAlert('Your deck contains some unknown cards', 'Those cards have not been saved.', 'error');
    unknownCardWarningAlertId.value = alertId;
  }
}

async function submitDeckAndCloseClicked() {
  await submitDeckClicked();
  exitPage();
}

function exitPage() {
  if (window.history.state?.back) {
    router.back();
  } else {
    router.push({ name: 'home' });
  }
}

function moveToMaindeck(cardId: string) {
  if (deck.value) {
    moveCardBetweenZones(cardId, deck.value.sideboard, deck.value.maindeck);
  }
}

function moveToSideboard(cardId: string) {
  if (deck.value) {
    moveCardBetweenZones(cardId, deck.value.maindeck, deck.value.sideboard);
  }
}

function moveCardBetweenZones(cardId: string, source: DeckCard[], destination: DeckCard[]) {
  const cardIndex = source.findIndex(card => card.id === cardId);
  if (cardIndex === -1) return;

  const sourceCard = source[cardIndex];
  const destinationCard = destination.find(card => card.id === cardId);
  if (destinationCard) {
    destinationCard.count += 1;
  } else {
    destination.push({ ...sourceCard, count: 1 });
  }

  if (sourceCard.count > 1) {
    sourceCard.count -= 1;
  } else {
    source.splice(cardIndex, 1);
  }

  updateDeckText();
}

function updateDeckText() {
  if (!deck.value) return;

  maindeck.value = deck.value.maindeck.map(createCardLine).join('\n');
  sideboard.value = deck.value.sideboard.map(createCardLine).join('\n');
}

</script>

<template>
  <div id="decklist" v-if="deck">
    <div class="title" v-if="deck !== null">
      <div style="display: flex; align-items: center; gap: 10px;">
        <BackButton />
        <h1>Decklist ({{ mainCount }}/{{ sideCount }}) <span id="decklist-title-separator">&mdash;</span></h1>
      </div>
      <input type=text v-model="deck.name" id="deck-name-input">
      <loading-button :on-click="submitDeckAndCloseClicked">Save deck and close</loading-button>
      <button-group v-model="viewMode" class="deckbuilder-mode-selector">
        <template #list>
          <i class="material-symbols-rounded">view_list</i>
        </template>
        <template #columns>
          <i class="material-symbols-rounded">view_column</i>
        </template>
      </button-group>
    </div>
    <div id="decklist-deckbuilder">
      <div id="deck-inputs">
        <textarea v-model="maindeck" placeholder="Maindeck (60 cards)"></textarea>
        <textarea v-model="sideboard" id="sideboard-input" placeholder="Sideboard (15 cards)"></textarea>
        <div class="submit-controls">
          <loading-button :on-click="submitDeckClicked">Save deck</loading-button>
        </div>
      </div>
      <DeckCardEditor sortBy="manaValue" v-if="viewMode == 'columns'" :maindeck="deck.maindeck"
        :sideboard="deck.sideboard" direction="row" @move-to-maindeck="moveToMaindeck"
        @move-to-sideboard="moveToSideboard" />
      <DeckPreview v-else :deckData="deck" :includeSideboard="true" />
    </div>
    <hr>
    <DeckStatsViz :cards="deck.maindeck" :width="800" :height="600" />
  </div>
  <div v-else>
    <LoadingState full-page />
  </div>
</template>

<style scoped>
#decklist {
  margin-left: 10px;
  margin-right: 10px;
}

.title {
  display: flex;
  flex-direction: row;
  align-items: center;
}

#deck-name-input {
  font-size: 1.75em;
}

.deckbuilder-mode-selector {
  margin-left: auto;
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