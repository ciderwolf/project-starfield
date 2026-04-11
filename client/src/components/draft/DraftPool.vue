<script setup lang="ts">
import { useDraftStore } from '@/stores/draft';
import CardPreview from '../deck/CardPreview.vue';
import { computed, ref, onMounted, onUnmounted } from 'vue';
import StyleButton from '../StyleButton.vue';
import { endGame } from '@/api/lobby';
import { useRouter } from 'vue-router';
import { draftClient } from '@/ws';
import type { CountDisplayCard, DisplayCard } from '@/api/message';
import Modal from '../Modal.vue';
import DeckStatsViz from '../deck/DeckStatsViz.vue';
import DeckCardEditor from '../deck/DeckCardEditor.vue';

interface GroupedCards {
  label: string;
  cards: CountDisplayCard[];
}

type SortingMode = 'name' | 'manaValue' | 'type' | 'color';

const draft = useDraftStore();
const router = useRouter();
const sortBy = ref<SortingMode>('manaValue');
const showPoolDetails = ref(false);

// const maindeck = computed(() => {
//   const grouped = draft.pool
//     .filter(card => !card.sideboard)
//     .map(card => ({ ...card.card, count: card.count }))
//     .reduce((acc, card) => {
//       let key;
//       if (sortBy.value === 'mv') {
//         key = card.manaValue;
//       } else if (sortBy.value === 'type') {
//         key = card.type;
//       } else if (sortBy.value === 'color') {
//         key = cardColors(card);
//       } else {
//         key = card.name;
//       }
//       if (!acc[key]) acc[key] = [];
//       acc[key].push(card);
//       return acc;
//     }, {} as Record<string, CountDisplayCard[]>);

//   // Sort the keys
//   const sortedKeys = Object.keys(grouped).sort((a, b) => {
//     if (sortBy.value === 'mv') {
//       return parseInt(a) - parseInt(b);
//     } else if (sortBy.value === 'type') {
//       return a.localeCompare(b);
//     } else {
//       return a.localeCompare(b);
//     }
//   });

//   return sortedKeys.map(key => ({ label: key, cards: grouped[key] }));
// });

const maindeck = computed(() => draft.pool.filter(card => !card.sideboard).map(card => ({ ...card.card, count: card.count })));
const sideboard = computed(() => draft.pool.filter(card => card.sideboard).map(card => ({ ...card.card, count: card.count })));

const maindeckCount = computed(() => maindeck.value.reduce((acc, card) => acc + cardCount(card), 0));
const sideboardCount = computed(() => sideboard.value.reduce((acc, card) => acc + card.count, 0));

function cardCount(card: CountDisplayCard | GroupedCards): number {
  if ('cards' in card) {
    return card.cards.reduce((sum, c) => sum + c.count, 0);
  }
  return card.count;
}

function cardFrameColor(card: DisplayCard): string {
  const colors = cardColors(card);
  if (colors.length === 1 && colors[0] == 'c') {
    if (card.type === 'Artifact') {
      return 'artifact';
    } else if (card.type == 'Land') {
      return 'land';
    } else {
      return 'colorless';
    }
  }
  else if (colors.length === 1) {
    return colors[0];
  }
  else if (colors.length > 1) {
    return 'multicolor';
  }
  else {
    return 'colorless';
  }
}

function cardSymbolGroups(card: DisplayCard): string[][] {
  const cost = card.manaCost;
  if (!cost) return [];

  const parts = cost.split(' // ');
  const result: string[][] = [];
  for (const part of parts) {
    const matches = part.matchAll(/\{([^\}]+)\}/g);
    const partSymbols = Array.from(matches).map(match => match[1].replace('/', '').toLowerCase());
    if (partSymbols.length > 0) {
      result.push(partSymbols);
    }
  }
  return result;
}

function cardSymbols(card: DisplayCard): string[] {
  return cardSymbolGroups(card).flatMap(symbol => symbol);
}

function cardColors(card: DisplayCard): string {
  const symbols = cardSymbols(card);
  const colors = new Set<string>();
  for (const symbol of symbols) {
    if (symbol.includes('w')) colors.add('w');
    if (symbol.includes('u')) colors.add('u');
    if (symbol.includes('b')) colors.add('b');
    if (symbol.includes('r')) colors.add('r');
    if (symbol.includes('g')) colors.add('g');
  }
  return colors.size > 0 ? Array.from(colors).join('') : 'c';
}

function moveToSideboard(cardId: string) {
  draftClient.moveCard(cardId, true);
}

function moveToMaindeck(cardId: string) {
  draftClient.moveCard(cardId, false);
}

function goHome() {
  router.push('/');
}

function leaveDraft() {
  if (confirm('Are you sure you want to end the draft for everyone?')) {
    endGame(draft.draftId!);
  }
}

const deckCards = computed(() =>
  draft.pool
    .filter(card => !card.sideboard)
    .map(card => ({
      ...card.card,
      count: card.count
    })) as CountDisplayCard[]
)

const leftPanelWidth = ref(300);
let isResizing = false;

function startResizing() {
  isResizing = true;
  document.body.style.userSelect = 'none';
}

function resizePanel(e: MouseEvent) {
  if (isResizing) {
    const screenWidth = window.innerWidth;
    leftPanelWidth.value = Math.max(250, screenWidth - e.clientX + 5);
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
    <Modal title="Pool Details" :visible="showPoolDetails" @close="showPoolDetails = false"
      modal-content-styles="width: 90%; height: 90%;">
      <DeckStatsViz :cards="deckCards" :width="600" :height="450" />
    </Modal>
    <div class="draft-pool" :style="{ width: leftPanelWidth + 'px' }">
      <div class="resize-handle" @mousedown="startResizing"></div>
      <div class="draft-pool-content">
        <div class="leave-draft-control">
          <style-button class="pool-details-button" @click="showPoolDetails = true" small title="Show pool details">
            <span class="material-symbols-rounded">bar_chart</span>
          </style-button>
          <style-button @click="goHome" small title="Go home"><span
              class="material-symbols-rounded">home</span></style-button>
          <style-button v-if="!draft.isEnded" @click="leaveDraft" small type="danger" title="End draft and leave"><span
              class="material-symbols-rounded">cancel</span></style-button>
        </div>
        <div class="draft-pool-header">
          <h3>Picks ({{ maindeckCount + sideboardCount }})</h3>
          <select v-model="sortBy">
            <option value="name">Name</option>
            <option value="manaValue">Mana Value</option>
            <option value="type">Type</option>
            <option value="color">Color</option>
          </select>
        </div>
        <DeckCardEditor :maindeck="maindeck" :sideboard="sideboard" direction="column" @moveToMaindeck="moveToMaindeck"
          @moveToSideboard="moveToSideboard" :sortBy="sortBy"></DeckCardEditor>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('@/assets/symbols.css');

.draft-pool-container {
  display: flex;
  height: 100vh;
}

.draft-pool-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-md);
  background-color: var(--color-gray-150);
}

.draft-pool {
  min-width: 260px;
  max-width: 600px;
  height: 100vh;
  position: relative;
  border: 1px solid var(--color-gray-300);
  background: var(--color-gray-200);
  box-sizing: border-box;
  display: flex;
}

.resize-handle {
  position: absolute;
  top: 0;
  left: 0;
  width: 5px;
  height: 100%;
  cursor: ew-resize;
  background-color: var(--color-gray-400);
  box-sizing: border-box;
}

.draft-pool-content {
  overflow-y: scroll;
  width: 100%;
  padding: var(--space-md);
}


.leave-draft-control {
  display: flex;
  gap: var(--space-sm);
  justify-content: right;
}
</style>