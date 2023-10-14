<script setup lang="ts">
import { onMounted, ref } from 'vue';
import DeckPreview from '@/components/deck/DeckPreview.vue';
import { useRoute } from 'vue-router';
import { useDecksCache } from '@/cache/decks';
import type { Deck } from '@/api/message';
import { useDecksStore } from '@/stores/decks';

const maindeck = ref('');
const sideboard = ref('');
const deck = ref<Deck | null>(null);

const route = useRoute();
const deckId = route.params.id as string;
const decks = useDecksCache();

onMounted(async () => {
  const decklist = await decks.get(deckId);
  if (decklist) {
    maindeck.value = decklist.maindeck.map(card => `${card.count} ${card.name}`).join('\n');
    sideboard.value = decklist.sideboard.map(card => `${card.count} ${card.name}`).join('\n');

    deck.value = decklist;
  }
});

const submitDeckClicked = async () => {
  const main = maindeck.value.split('\n')
    .map(line => line.trim())
    .filter(line => line.length > 0);
  const side = sideboard.value.split('\n')
    .map(line => line.trim())
    .filter(line => line.length > 0);

  const store = useDecksStore();
  store.decks[deck.value!.id] = { name: deck.value!.name, id: deck.value!.id };
  deck.value = await decks.set(deck.value!.id, { name: deck.value!.name, main, side });

  const decklist = deck.value;
  maindeck.value = decklist.maindeck.map(card => `${card.count} ${card.name}`).join('\n');
  sideboard.value = decklist.sideboard.map(card => `${card.count} ${card.name}`).join('\n');
}

</script>

<template>
  <div id="decklist">
    <h1>Decklist <span v-if="deck !== null">&mdash; <input type=text v-model="deck.name" id="deck-name-input"></span></h1>
    <div id="decklist-deckbuilder">
      <div id="deck-inputs">
        <textarea v-model="maindeck" placeholder="Maindeck (60 cards)"></textarea>
        <textarea v-model="sideboard" id="sideboard-input" placeholder="Sideboard (15 cards)"></textarea>
        <div class="submit-controls">
          <button @click="submitDeckClicked">Save deck</button>
        </div>
      </div>
      <deck-preview v-if="deck" :deckData="deck" />
      <h3 v-else>No deck yet</h3>
    </div>
  </div>
</template>

<style scoped>
#deck-name-input {
  font-size: 1em;
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
  font-family: sans-serif;
  min-width: 300px;
  min-height: 400px;
  margin: 5px;
}

#decklist #sideboard-input {
  min-height: 150px;
}
</style>