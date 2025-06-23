<script setup lang="ts">
import CardPreview from './CardPreview.vue';
import type { DeckCard as Card, Deck as DeckData, DeckCard } from '@/api/message';
import { computed, ref, watchEffect } from 'vue';

const props = defineProps({
  deckData: {
    type: Object as () => DeckData,
    required: true,
  },
  includeSideboard: {
    type: Boolean,
    default: true,
  },
});

const deck = computed(() => groupDeckByType(props.deckData));

type Deck = { [type: string]: DeckCard[] };
interface CardColumn {
  title: string;
  count: number;
  cards: Card[];
}

function groupDeckByType(deck: DeckData): Deck {
  const result = deck.maindeck.reduce((acc, item) => {
    const type = item.type || 'Unknown';
    if (type in acc) {
      acc[type].push(item);
    } else {
      acc[type] = [item];
    }
    return acc;
  }, { Unknown: [] } as Deck);

  result.Sideboard = deck.sideboard;
  return result;
}

function createColumn(typename: string, cardlist: Card[]): CardColumn {
  const size = cardlist.reduce((acc, item) => acc + item.count, 0);
  return {
    title: typename,
    count: size,
    cards: cardlist,
  };
}

function makeColumns(decklist: Deck): [CardColumn[], CardColumn[]] {
  const rowOnes: any[] = []; const rowTwos: any[] = []; let rowOneTotal = 0; let rowTwoTotal = 0;
  const rows = Object.keys(decklist).reverse()
    .map((key) => ({ count: decklist[key].length, name: key }));

  rows.sort((a, b) => b.count - a.count);

  while (rows.length > 0) {
    const current = rows[0];
    if (rowOneTotal <= rowTwoTotal) {
      rowOnes.push(current);
      rowOneTotal += current.count + 4;
    } else {
      rowTwos.push(current);
      rowTwoTotal += current.count + 4;
    }
    rows.splice(0, 1);
  }
  return [
    rowOnes.map((item) => createColumn(item.name, decklist[item.name])),
    rowTwos.map((item) => createColumn(item.name, decklist[item.name])),
  ];
}

function makeDecklist(deck: { [type: string]: Card[] }): [CardColumn[], CardColumn[], CardColumn, CardColumn] {
  const sideboard = deck.Sideboard;
  const unknowns = deck.Unknown;
  delete deck.Sideboard;
  delete deck.Unknown;

  const [one, two] = makeColumns(deck);
  return [one, two, createColumn('Sideboard', sideboard), createColumn('Unknown', unknowns)];
}
// onMounted(displayDeck);



function displayDeck() {
  const [one, two, sb, uk] = makeDecklist(deck.value);
  rowOne.value = one;
  rowTwo.value = two;
  sideboard.value = sb;
  unknowns.value = uk;
}

const rowOne = ref<CardColumn[]>([]),
  rowTwo = ref<CardColumn[]>([]),
  sideboard = ref<CardColumn>({} as CardColumn),
  unknowns = ref<CardColumn>({} as CardColumn);

watchEffect(() => {
  props.deckData;
  displayDeck();
});
</script>

<template>
  <div class="deck-preview">
    <div class="row">
      <div class="type" v-for="section of rowOne" :key="section.title">
        <h3>{{ section.title }} ({{ section.count }})</h3>
        <card-preview v-for="card of section.cards" :key="card.name" :card="card" />
      </div>
    </div>
    <div class="row">
      <div class="type" v-for="section of rowTwo" :key="section.title">
        <h3>{{ section.title }} ({{ section.count }})</h3>
        <card-preview v-for="card of section.cards" :key="card.name" :card="card" />
      </div>
    </div>
    <div class="row" v-if="includeSideboard !== false">
      <div class="type">
        <h3>{{ sideboard.title }} ({{ sideboard.count }})</h3>
        <card-preview v-for="card of sideboard.cards" :key="card.name" :card="card" />
      </div>
      <div class="type" v-if="unknowns.count > 0">
        <h3>{{ unknowns.title }} ({{ unknowns.count }})</h3>
        <p v-for="card of unknowns.cards" :key="card.name">{{ card.count }} {{ card.name }}</p>
      </div>
    </div>
  </div>
</template>


<style>
.deck-preview {
  display: flex;
  flex-grow: 1;
  flex-direction: row;
  justify-content: space-around;
}

.row {
  min-width: 33%;
}

.type {
  padding: 20px;
}

.type p {
  margin-top: 0;
  margin-bottom: 0;
}
</style>