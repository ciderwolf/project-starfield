<script setup lang="ts">
import { useDraftStore, type PoolCard } from '@/stores/draft';
import CardPreview from '../deck/CardPreview.vue';
import { computed } from 'vue';
import StyleButton from '../StyleButton.vue';
import { endGame } from '@/api/lobby';
import { useRouter } from 'vue-router';

const draft = useDraftStore();
const router = useRouter();

const maindeck = computed(() => draft.pool.filter(card => !card.sideboard));
const sideboard = computed(() => draft.pool.filter(card => card.sideboard));

const maindeckCount = computed(() => maindeck.value.reduce((acc, card) => acc + card.count, 0));
const sideboardCount = computed(() => sideboard.value.reduce((acc, card) => acc + card.count, 0));

function moveToSideboard(card: PoolCard) {
  draft.setZone(card, !card.sideboard);
}

function moveToMaindeck(card: PoolCard) {
  draft.setZone(card, !card.sideboard);
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
      <CardPreview :card="card" v-for="card in maindeck" :key="card.name" @click="moveToSideboard(card)" />
      <h3>Sideboard ({{ sideboardCount }})</h3>
      <CardPreview :card="card" v-for="card in sideboard" :key="card.name" @click="moveToMaindeck(card)" />
    </div>
    <div class="leave-draft-control">
      <style-button @click="leaveDraft" small type="danger">End Draft</style-button>
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