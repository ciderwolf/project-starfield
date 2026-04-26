<script setup lang="ts">
import { newDeck, deleteDeck } from '@/api/deck';
import type { DeckListing } from '@/api/message';
import { useDecksCache } from '@/cache/decks';
import LoadingState from '@/components/LoadingState.vue';
import EmptyState from '@/components/EmptyState.vue';
import LoadingButton from '@/components/LoadingButton.vue';
import ItemCard from '@/components/home/ItemCard.vue';
import ItemCardGrid from '@/components/home/ItemCardGrid.vue';
import { useDecksStore } from '@/stores/decks';
import { useRouter } from 'vue-router';


const decksCache = useDecksCache();
const decks = useDecksStore();
const router = useRouter();

const createDeck = async () => {
  const deck = await newDeck();
  decksCache.put(deck.id, deck);
  decks.decks[deck.id] = { name: deck.name, id: deck.id, thumbnailImage: "", ownerId: deck.ownerId };
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

function deckThumbnailUrl(deck: DeckListing) {
  if (deck.thumbnailImage) {
    return deck.thumbnailImage;
  }
  else {
    return 'https://api.scryfall.com/cards/ec8e4142-7c46-4d2f-aaa6-6410f323d9f0?format=image&version=art_crop';
  }
}

</script>

<template>
  <div id="decks">
    <div class="title">
      <h2>Decks</h2>
      <loading-button :on-click="createDeck" small>+ New Deck</loading-button>
    </div>
    <div v-if="!decks.isLoaded">
      <LoadingState message="Loading decks..." />
    </div>
    <ItemCardGrid v-else-if="Object.keys(decks.decks).length > 0">
      <ItemCard v-for="deck in decks.decks" :key="deck.id" :title="deck.name" :image="deckThumbnailUrl(deck)"
        :to="{ name: 'deckbuilder', params: { id: deck.id } }">
        <template #actions>
          <LoadingButton class="delete-button" type="danger" :on-click="(e) => deleteDeckClicked(e, deck.id)">
            Delete deck
          </LoadingButton>
        </template>
      </ItemCard>
    </ItemCardGrid>
    <EmptyState v-else title="You have no decks." subtitle="Click on '+ New Deck' to create one." />
  </div>
</template>

<style scoped>
.title {
  display: flex;
  gap: var(--space-xl);
  align-items: center;
}

.delete-button {
  width: 100%;
}
</style>