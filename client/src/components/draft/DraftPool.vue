<script setup lang="ts">
import { useDraftStore } from '@/stores/draft';
import CardPreview from '../deck/CardPreview.vue';
import { computed, ref, onMounted, onUnmounted } from 'vue';
import StyleButton from '../StyleButton.vue';
import { endGame } from '@/api/lobby';
import { useRouter } from 'vue-router';
import { draftClient } from '@/ws';
import type { DraftCard } from '@/api/message';

type DraftDisplayCard = DraftCard & {
  count: number;
};

interface GroupedCards {
  label: string;
  cards: DraftDisplayCard[];
}

type SortingMode = 'name' | 'mv' | 'type';

const draft = useDraftStore();
const router = useRouter();
const sortBy = ref<SortingMode>('name');

const maindeck = computed(() => {
  const grouped = draft.pool
    .filter(card => !card.sideboard)
    .map(card => ({ ...card.card, count: card.count }))
    .reduce((acc, card) => {
      let key;
      if (sortBy.value === 'mv') {
        key = card.manaValue;
      } else if (sortBy.value === 'type') {
        key = card.type;
      } else {
        key = card.name;
      }
      if (!acc[key]) acc[key] = [];
      acc[key].push(card);
      return acc;
    }, {} as Record<string, DraftDisplayCard[]>);

  // Sort the keys
  const sortedKeys = Object.keys(grouped).sort((a, b) => {
    if (sortBy.value === 'mv') {
      return parseInt(a) - parseInt(b);
    } else if (sortBy.value === 'type') {
      return a.localeCompare(b);
    } else {
      return a.localeCompare(b);
    }
  });

  return sortedKeys.map(key => ({ label: key, cards: grouped[key] }));
});

const sideboard = computed(() => draft.pool.filter(card => card.sideboard).map(card => ({ ...card.card, count: card.count })));

const maindeckCount = computed(() => maindeck.value.reduce((acc, card) => accumulateCardCount(acc, card), 0));
const sideboardCount = computed(() => sideboard.value.reduce((acc, card) => acc + card.count, 0));

function accumulateCardCount(acc: number, card: DraftDisplayCard | GroupedCards): number {
  if ('cards' in card) {
    return acc + card.cards.reduce((sum, c) => sum + c.count, 0);
  }
  return acc + card.count;
}

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

const leftPanelWidth = ref(300);
let isResizing = false;

function startResizing(event: MouseEvent) {
  isResizing = true;
  document.body.style.userSelect = 'none';
}

function resizePanel(event: MouseEvent) {
  if (isResizing) {
    const screenWidth = window.innerWidth;
    leftPanelWidth.value = Math.max(200, screenWidth - event.clientX);
  }
}

function stopResizing() {
  isResizing = false;
  document.body.style.userSelect = '';
}

onMounted(() => {
  document.addEventListener('mousemove', resizePanel);
  document.addEventListener('mouseup', stopResizing);
});

onUnmounted(() => {
  document.removeEventListener('mousemove', resizePanel);
  document.removeEventListener('mouseup', stopResizing);
});
</script>

<template>
  <div class="draft-pool-container">
    <div class="draft-pool" :style="{ width: leftPanelWidth + 'px' }">
      <div class="resize-handle" @mousedown="startResizing"></div>
      <div>
        <h3>Picks ({{ maindeckCount }})</h3>
        <select v-model="sortBy">
          <option value="name">Name</option>
          <option value="mv">Mana Value</option>
          <option value="type">Type</option>
        </select>
      </div>
      <div v-if="sortBy === 'name'" class="grouped-cards">
        <CardPreview :card="card.cards[0]" v-for="card in maindeck" :key="card.cards[0].name"
          @click="moveToSideboard(card.cards[0].id)" />
      </div>
      <div v-else>
        <div v-for="group in maindeck" :key="group.label" class="grouped-cards">
          <h4>{{ group.label }}</h4>
          <div style="margin-bottom: 15px;">
            <CardPreview :card="card" v-for="card in group.cards" :key="card.name" @click="moveToSideboard(card.id)" />
          </div>
        </div>
      </div>
      <h3>Sideboard ({{ sideboardCount }})</h3>
      <div class="grouped-cards" v-if="sideboard.length > 0">
        <CardPreview :card="card" v-for="card in sideboard" :key="card.name" @click="moveToMaindeck(card.id)" />
      </div>
    </div>
    <div class="leave-draft-control">
      <style-button v-if="!draft.isEnded" @click="leaveDraft" small type="danger">End Draft</style-button>
      <style-button @click="goHome" small>Go Home</style-button>
    </div>
  </div>
</template>

<style scoped>
.draft-pool-container {
  display: flex;
  height: 100vh;
}

.draft-pool {
  min-width: 200px;
  max-width: 600px;
  padding: 10px;
  border: 1px solid #ddd;
  background: #eee;
  overflow-y: scroll;
  box-sizing: border-box;
  position: relative;
}

.resize-handle {
  position: absolute;
  top: 0;
  left: 0;
  /* Changed from right to left */
  width: 5px;
  height: 100%;
  cursor: ew-resize;
  background-color: #ccc;
}

.leave-draft-control {
  display: flex;
  gap: 5px;
  position: absolute;
  bottom: 20px;
  right: 20px;
}

.grouped-cards {
  margin-bottom: 15px;
  position: relative;
  background-color: #f9f9f9;
  border: 1px solid #ddd;
  padding: 10px;
  border-radius: 5px;
}

.grouped-cards h4 {
  margin: 10px;
  font-size: 0.8em;
  color: gray;
  position: absolute;
  bottom: 0;
  right: 0;
}
</style>