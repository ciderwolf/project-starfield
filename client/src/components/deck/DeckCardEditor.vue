<script setup lang="ts">
import type { CountDisplayCard, DisplayCard } from '@/api/message';
import { computed, ref, onMounted } from 'vue';
import CardPreview from './CardPreview.vue';

const props = defineProps<{ maindeck: CountDisplayCard[], sideboard: CountDisplayCard[], includeSideboard?: boolean, direction: 'column' | 'row', sortBy: string }>();

const emit = defineEmits<{
  (e: 'moveToMaindeck', cardId: string): void;
  (e: 'moveToSideboard', cardId: string): void;
}>()

// Scroll shadow state
const deckGroupsRef = ref<HTMLElement | null>(null);
const showLeftShadow = ref(false);
const showRightShadow = ref(false);

function updateScrollShadows() {
  const el = deckGroupsRef.value;
  if (!el || props.direction === 'column') return;

  const { scrollLeft, scrollWidth, clientWidth } = el;
  showLeftShadow.value = scrollLeft > 0;
  showRightShadow.value = scrollLeft + clientWidth < scrollWidth - 1;
}

onMounted(() => {
  updateScrollShadows();
});

interface GroupedCards {
  label: string;
  cards: CountDisplayCard[];
}

const maindeck = computed(() => {
  const grouped = props.maindeck
    .reduce((acc, card) => {
      let key;
      if (props.sortBy === 'manaValue') {
        key = card.manaValue;
      } else if (props.sortBy === 'type') {
        key = card.type;
      } else if (props.sortBy === 'color') {
        key = cardColors(card);
      } else {
        key = card.name;
      }
      if (!acc[key]) acc[key] = [];
      acc[key].push(card);
      return acc;
    }, {} as Record<string, CountDisplayCard[]>);

  // Sort the keys
  const sortedKeys = Object.keys(grouped).sort((a, b) => {
    if (props.sortBy === 'manaValue') {
      return parseInt(a) - parseInt(b);
    } else if (props.sortBy === 'type') {
      return a.localeCompare(b);
    } else {
      return a.localeCompare(b);
    }
  });

  return sortedKeys.map(key => ({ label: key, cards: grouped[key] }));
});

const sideboard = computed(() => props.sideboard || []);

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
  emit('moveToSideboard', cardId);
}
function moveToMaindeck(cardId: string) {
  emit('moveToMaindeck', cardId);
}
</script>

<template>

  <div class="deck-card-editor" :class="`direction-${direction}`">
    <div v-if="sortBy === 'name'" class="grouped-cards">
      <div class="card-line-container" :class="cardFrameColor(card.cards[0])" v-for="card in maindeck"
        :key="card.cards[0].name">
        <CardPreview :card="card.cards[0]" @click="moveToSideboard(card.cards[0].id)" />
        <span>
          <abbr :class="`card-symbol card-symbol-${symbol.toUpperCase()}`" v-for="symbol in cardSymbols(card.cards[0])">
            {{ symbol }}
          </abbr>
        </span>
      </div>
    </div>
    <div v-else class="deck-groups-wrapper"
      :class="{ 'show-left-shadow': showLeftShadow, 'show-right-shadow': showRightShadow, 'direction-column': props.direction === 'column', 'direction-row': props.direction === 'row' }">
      <div class="deck-groups" ref="deckGroupsRef" @scroll="updateScrollShadows" :class="`direction-${direction}`">
        <div v-for="group in maindeck" :key="group.label" class="grouped-cards">
          <h4>{{ group.label }} ({{ cardCount(group) }})</h4>
          <div class="card-line-container" :class="cardFrameColor(card)" v-for="card in group.cards" :key="card.name">
            <CardPreview :card="card" cursor="pointer" @click="moveToSideboard(card.id)" />
            <span class="card-line-mana">
              <span class="card-symbol-group" v-for="group in cardSymbolGroups(card)" :key="group.join('-')">
                <abbr :class="`card-symbol card-symbol-${symbol.toUpperCase()}`" v-for="symbol in group">
                  {{ symbol }}
                </abbr>
              </span>
            </span>
          </div>
        </div>
      </div>
    </div>
    <hr>
    <div class="grouped-cards" v-if="sideboard.length > 0">
      <h4>Sideboard ({{ sideboard.length }})</h4>
      <div class="card-line-container" :class="cardFrameColor(card)" v-for="card in sideboard" :key="card.name">
        <CardPreview :card="card" cursor="pointer" @click="moveToMaindeck(card.id)" />
        <span class="card-line-mana">
          <span class="card-symbol-group" v-for="group in cardSymbolGroups(card)" :key="group.join('-')">
            <abbr :class="`card-symbol card-symbol-${symbol.toUpperCase()}`" v-for="symbol in group">
              {{ symbol }}
            </abbr>
          </span>
        </span>
      </div>
    </div>
  </div>

</template>

<style scoped>
@import url('@/assets/symbols.css');

.deck-card-editor {
  display: flex;
  gap: var(--space-md);
  padding: 0 var(--space-md);
  overflow: scroll;
}

.direction-row {
  flex-direction: row;
}

.direction-column {
  flex-direction: column;
}

.deck-groups-wrapper {
  position: relative;
  flex: 1;
  min-width: 0;
}

.deck-groups-wrapper::before,
.deck-groups-wrapper::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  width: 30px;
  pointer-events: none;
  opacity: 0;
  transition: opacity var(--transition-normal);
  z-index: var(--z-base);
}

.deck-groups-wrapper::before {
  left: 0;
  background: linear-gradient(to right, rgba(0, 0, 0, 0.15), transparent);
}

.deck-groups-wrapper::after {
  right: 0;
  background: linear-gradient(to left, rgba(0, 0, 0, 0.15), transparent);
}

.deck-groups-wrapper.show-left-shadow::before {
  opacity: 1;
}

.deck-groups-wrapper.show-right-shadow::after {
  opacity: 1;
}

.deck-groups {
  display: flex;
  gap: var(--space-xl);
  overflow: scroll;
  height: 100%;
}

.grouped-cards {
  position: relative;
  background-color: var(--color-gray-50);
  border: 1px solid var(--color-gray-300);
  padding: var(--space-md);
  border-radius: var(--radius-md);
}

.grouped-cards h4 {
  margin: 0;
  padding: 0;
  text-align: right;
  font-size: 0.7em;
  color: var(--color-text-muted);
  text-transform: uppercase;
}

.card-line-container {
  display: flex;
  align-items: center;
  padding: var(--space-sm);
  margin: var(--space-sm) 0;
  border: 1px solid var(--color-black);
  border-radius: var(--radius-md);
  justify-content: space-between;
}

.direction-row .card-line-container {
  width: 250px;
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

.card-line-mana {
  display: flex;
  margin-left: 10px;
}

.card-symbol-group {
  display: flex;
}

.card-symbol-group:not(:last-child)::after {
  content: " // ";
  margin-right: var(--space-sm);
  padding: 0;
  color: var(--color-gray-650);
}

hr {
  border: none;
  border-top: 1px solid var(--color-gray-300);
  margin: 10px 0;
}
</style>