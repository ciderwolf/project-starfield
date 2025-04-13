<script setup lang="ts">
import type { DraftCard } from '@/api/message';
import DraftCardElement from '@/components/draft/DraftCard.vue';
import DraftPackQueues from '@/components/draft/DraftPackQueues.vue';
import DraftPool from '@/components/draft/DraftPool.vue';
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import { useDecksStore } from '@/stores/decks';
import { useDraftStore } from '@/stores/draft';
import { useGameStore } from '@/stores/games';
import { draftClient } from '@/ws';
import { watchEffect } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const draft = useDraftStore();
const route = useRoute();
const router = useRouter();
const games = useGameStore();
const decks = useDecksStore();

watchEffect(() => {
  const gameId = route.params.id as string;
  if (games.isLoaded && !games.games[gameId] && !draft.isEnded) {
    router.push('/');
  }
})


watchEffect(() => {
  if (draft.isEnded) {
    decks.reloadDecks();
  }
})

function pickCard(card: DraftCard) {
  draftClient.pickCard(card.id);
  draft.pickCard(card);
}

</script>

<template>
  <div id="draft" v-if="!draft.isEnded">
    <div class="draft-main">
      <h3>Pack {{ draft.packNumber }} Pick {{ draft.pickNumber + 1 }}</h3>
      <div class="draft-pack-cards" v-if="draft.currentPack.length > 0">
        <DraftCardElement :card="card" v-for="card in draft.currentPack" :key="card.id" @pick="pickCard(card)" />
      </div>
      <div class="draft-message" v-else>
        <span>
          <LoadingSpinner /> <b>Waiting for other players to pick...</b>
        </span>
        <DraftPackQueues />
      </div>
    </div>
    <DraftPool />
  </div>
  <div id="draft" v-else>
    <div class="draft-message">
      <h2>Draft Ended</h2>
      <p>
        Your draft deck has automatically been saved.
        <router-link :to="`/deckbuilder/${draft.deckId}`">Go to deckbuilder.</router-link>
      </p>
    </div>
    <DraftPool />
  </div>

</template>

<style scoped>
#draft {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
}

.draft-message {
  width: 100%;
  margin: auto 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.draft-main {
  max-height: 100vh;
  min-height: 100vh;
  overflow: scroll;
  width: 100%;
}

.draft-pack-cards {
  display: flex;
  flex-wrap: wrap;
}
</style>