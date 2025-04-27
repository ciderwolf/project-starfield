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

type SortingMode = 'name' | 'mv' | 'type' | 'color';

const draft = useDraftStore();
const router = useRouter();
const sortBy = ref<SortingMode>('mv');

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
      } else if (sortBy.value === 'color') {
        key = cardColors(card);
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

const maindeckCount = computed(() => maindeck.value.reduce((acc, card) => acc + cardCount(card), 0));
const sideboardCount = computed(() => sideboard.value.reduce((acc, card) => acc + card.count, 0));

function cardCount(card: DraftDisplayCard | GroupedCards): number {
  if ('cards' in card) {
    return card.cards.reduce((sum, c) => sum + c.count, 0);
  }
  return card.count;
}

function cardFrameColor(card: DraftCard): string {
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

function cardSymbols(card: DraftCard): string[] {
  const cost = card.manaCost;
  if (!cost) return [];
  const matches = cost.matchAll(/\{([^\}]+)\}/g);
  return Array.from(matches).map(match => match[1].toLowerCase());
}

function cardColors(card: DraftCard): string {
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
  endGame(draft.draftId!);
}

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
    <div class="draft-pool" :style="{ width: leftPanelWidth + 'px' }">
      <div class="resize-handle" @mousedown="startResizing"></div>
      <div class="draft-pool-content">
        <div class="draft-pool-header">
          <h3>Picks ({{ maindeckCount }})</h3>
          <select v-model="sortBy">
            <option value="name">Name</option>
            <option value="mv">Mana Value</option>
            <option value="type">Type</option>
            <option value="color">Color</option>
          </select>
        </div>
        <div v-if="sortBy === 'name'" class="grouped-cards">
          <div class="card-line-container" :class="cardFrameColor(card.cards[0])" v-for="card in maindeck"
            :key="card.cards[0].name">
            <CardPreview :card="card.cards[0]" @click="moveToSideboard(card.cards[0].id)" />
            <span>
              <abbr :class="`card-symbol card-symbol-${symbol.toUpperCase()}`"
                v-for="symbol in cardSymbols(card.cards[0])">{{ symbol
                }}</abbr>
            </span>
          </div>
        </div>
        <div v-else>
          <div v-for="group in maindeck" :key="group.label" class="grouped-cards">
            <h4>{{ group.label }} ({{ cardCount(group) }})</h4>
            <div style="margin-top: 15px;">
              <div class="card-line-container" :class="cardFrameColor(card)" v-for="card in group.cards"
                :key="card.name">
                <CardPreview :card="card" cursor="pointer" @click="moveToSideboard(card.id)" />
                <span>
                  <abbr :class="`card-symbol card-symbol-${symbol.toUpperCase()}`"
                    v-for="symbol in cardSymbols(card)">{{ symbol
                    }}</abbr>
                </span>
              </div>

            </div>
          </div>
        </div>
        <h3>Sideboard ({{ sideboardCount }})</h3>
        <div class="grouped-cards" v-if="sideboard.length > 0">
          <CardPreview :card="card" cursor="pointer" v-for="card in sideboard" :key="card.name"
            @click="moveToMaindeck(card.id)" />
        </div>
        <div class="leave-draft-control">
          <style-button v-if="!draft.isEnded" @click="leaveDraft" small type="danger">End Draft</style-button>
          <style-button @click="goHome" small>Go Home</style-button>
        </div>
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
  padding: 10px;
  background-color: #f0f0f0;
  border-bottom: 1px solid #ddd;
}

.draft-pool {
  min-width: 260px;
  max-width: 600px;
  height: 100vh;
  position: relative;
  border: 1px solid #ddd;
  background: #eee;
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
  background-color: #ccc;
  box-sizing: border-box;
}

.draft-pool-content {
  overflow-y: scroll;
  width: 100%;
  padding: 10px;
}


.leave-draft-control {
  display: flex;
  gap: 5px;
  margin: 20px 0;
}

.grouped-cards {
  margin-top: 15px;
  position: relative;
  background-color: #f9f9f9;
  border: 1px solid #ddd;
  padding: 10px;
  border-radius: 5px;
}

.grouped-cards h4 {
  margin: 10px;
  font-size: 0.7em;
  color: gray;
  position: absolute;
  top: 0;
  right: 0;
  text-transform: uppercase;
}

.card-line-container {
  display: flex;
  align-items: center;
  padding: 5px;
  margin: 5px 0;
  border: 1px solid #000;
  border-radius: 5px;
  justify-content: space-between;
}

.card-line-container.b {
  background-color: rgb(199, 189, 183);
  border-color: #49423c;
}

.card-line-container.g {
  background-color: rgb(212, 223, 203);
  border-color: #487d4e;
}

.card-line-container.w {
  background-color: rgb(236, 232, 221);
  border-color: #e4e2dd;
}

.card-line-container.u {
  background-color: rgb(198, 211, 227);
  border-color: #4377b9;
}

.card-line-container.r {
  background-color: rgb(234, 193, 161);
  border-color: #e04422;
}

.card-line-container.multicolor {
  background-color: rgb(237, 229, 160);
  border-color: #e5d46e;
}

.card-line-container.land {
  background-color: rgb(227, 220, 210);
  border-color: #b09886;

}

.card-line-container.artifact {
  background-color: rgb(212, 215, 215);
  border-color: #788581;
}

.card-line-container.colorless {
  background-color: rgb(212, 215, 215);
  border-color: #788581;
}
</style>