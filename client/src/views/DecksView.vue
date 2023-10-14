<script setup lang="ts">
import { newDeck } from '@/api/deck';
import { useDecksCache } from '@/cache/decks';
import { useDecksStore } from '@/stores/decks';
import { useRouter } from 'vue-router';


const decksCache = useDecksCache();
const decks = useDecksStore();
const router = useRouter();

const createDeck = async () => {
  const deck = await newDeck();
  decksCache.put(deck.id, deck);
  decks.decks[deck.id] = { name: deck.name, id: deck.id };
  router.push({ name: 'deckbuilder', params: { id: deck.id } });
};

</script>

<template>
  <div id="decks">
    <h1>Decks</h1>
    <button @click="createDeck">New Deck</button>
    <ul>
      <li v-for="deck in decks.decks" :key="deck.id">
        <router-link :to="{ name: 'deckbuilder', params: { id: deck.id } }">
          {{ deck.name }}
        </router-link>
      </li>
    </ul>
  </div>
</template>