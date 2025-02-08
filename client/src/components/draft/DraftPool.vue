<script setup lang="ts">
import { useDraftStore, type PoolCard } from '@/stores/draft';
import CardPreview from '../deck/CardPreview.vue';
import { computed } from 'vue';
import StyleButton from '../StyleButton.vue';
import { endGame } from '@/api/lobby';
import { useRouter } from 'vue-router';
import { draftClient } from '@/ws';

const draft = useDraftStore();
const router = useRouter();

const maindeck = computed(() => draft.pool.filter(card => !card.sideboard).map(card => ({ ...card.card, count: card.count })));
const sideboard = computed(() => draft.pool.filter(card => card.sideboard).map(card => ({ ...card.card, count: card.count })));

const maindeckCount = computed(() => maindeck.value.reduce((acc, card) => acc + card.count, 0));
const sideboardCount = computed(() => sideboard.value.reduce((acc, card) => acc + card.count, 0));

function moveToSideboard(cardId: string) {
  // draft.setZone(card, !card.sideboard);
  draftClient.moveCard(cardId, true);
}

function moveToMaindeck(cardId: string) {
  // draft.setZone(card, !card.sideboard);
  draftClient.moveCard(cardId, false);
}

function goHome() {
  router.push('/');
}

function leaveDraft() {
  endGame(draft.draftId!);
}
</script>

<template>
  <div>
    <div class="draft-pool">
      <h3>Picks ({{ maindeckCount }})</h3>
      <CardPreview :card="card" v-for="card in maindeck" :key="card.name" @click="moveToSideboard(card.id)" />
      <h3>Sideboard ({{ sideboardCount }})</h3>
      <CardPreview :card="card" v-for="card in sideboard" :key="card.name" @click="moveToMaindeck(card.id)" />
    </div>
    <div class="leave-draft-control">
      <style-button v-if="!draft.isEnded" @click="leaveDraft" small type="danger">End Draft</style-button>
      <style-button @click="goHome" small>Go Home</style-button>
    </div>
  </div>
</template>

<style scoped>
.draft-pool {
  min-width: 300px;
  padding: 10px;
  border: 1px solid #ddd;
  background: #eee;
  height: 100vh;
  overflow-y: scroll;
  box-sizing: border-box;
}

.leave-draft-control {
  display: flex;
  gap: 5px;
  position: absolute;
  bottom: 20px;
  right: 20px;
}
</style>