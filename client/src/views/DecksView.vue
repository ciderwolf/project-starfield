<script setup lang="ts">
import { newDeck, deleteDeck } from '@/api/deck';
import { useDecksCache } from '@/cache/decks';
import StyleButton from '@/components/StyleButton.vue';
import { useDecksStore } from '@/stores/decks';
import { useRouter } from 'vue-router';


const decksCache = useDecksCache();
const decks = useDecksStore();
const router = useRouter();

const createDeck = async () => {
  const deck = await newDeck();
  decksCache.put(deck.id, deck);
  decks.decks[deck.id] = { name: deck.name, id: deck.id, thumbnailId: "" };
  router.push({ name: 'deckbuilder', params: { id: deck.id } });
};

async function deleteDeckClicked(e: MouseEvent, id: string) {
  e.preventDefault();
  const deleted = await deleteDeck(id);
  if (deleted) {
    decksCache.remove(id);
    delete decks.decks[id];
  }
}

</script>

<template>
  <div id="decks">
    <div class="title">
      <h2>Decks</h2>
      <style-button @click="createDeck" small>+ New Deck</style-button>
    </div>
    <div class="deck-cards" v-if="Object.keys(decks.decks).length > 0">
      <div v-for="deck in decks.decks" :key="deck.id">
        <router-link :to="{ name: 'deckbuilder', params: { id: deck.id } }" class="deck-card">
          <img :alt="`${deck.name} Thumnail`" class="deck-card-thumnail"
            :src="`https://api.scryfall.com/cards/${deck.thumbnailId || 'ec8e4142-7c46-4d2f-aaa6-6410f323d9f0'}?format=image&version=art_crop`" />
          <h3 class="deck-card-title">{{ deck.name }}</h3>
          <style-button @click="deleteDeckClicked($event, deck.id)" type="danger" class="delete-deck-button">Delete
            deck</style-button>
        </router-link>
      </div>
    </div>
    <div v-else class="empty-container-title">
      <h3>You have no decks.</h3>
      <p>Click on '+ New Deck' to create one.</p>
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

.deck-cards {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  background-color: #eee;
  border: 1px solid #ddd;
  border-radius: 5px;
  max-width: 80%;
  width: fit-content;
  margin: 0 auto;
  padding: 1em 5em;
  margin-bottom: 200px;
}

.deck-card {
  display: flex;
  flex-direction: column;
  text-decoration: none;
  text-align: center;
  color: #000;
  margin: 0.5em;
  box-shadow: 0 0 5px 0 rgba(0, 0, 0, 0.5);
  width: 250px;

}

.deck-card:hover {
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.5);
}

.delete-deck-button {
  display: none;
  margin: 0 0.5em 0.5em 0.5em;
}

.deck-card:hover .delete-deck-button {
  display: block;
}

.deck-card-thumbnail {
  width: 200px;
  height: 200px;
  object-fit: cover;
}

.deck-card-title {
  margin: 0.75em;
}
</style>