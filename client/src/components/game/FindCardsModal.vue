<script setup lang="ts">
import Modal from '@/components/Modal.vue'
import { computed, reactive, ref } from 'vue';
import { getMoveZoneActions } from '@/context-menu';
import ContextMenu from '../ContextMenu.vue';
import { client } from '@/ws';
import { useBoardStore, type OracleId } from '@/stores/board';
import type { OracleCard } from '@/api/message';

const props = defineProps<{ cards: { [id: string]: OracleId } }>();

type IdentifiedDeckCard = { uid: string } & OracleCard;

defineExpose({ open });

const board = useBoardStore();

const expandedCards = computed(() => {
  const expanded: IdentifiedDeckCard[] = [];
  for (const [virtualId, oracleId] of Object.entries(props.cards)) {
    const card = board.oracleInfo[oracleId];
    expanded.push({ ...card, uid: virtualId });
  }
  return expanded.sort((a, b) => a.name.localeCompare(b.name));
});
const visible = ref(false);
function open() {
  visible.value = true;
}

const selected = ref<Set<string>>(new Set());
const cardFilter = ref('');


const filteredCards = computed(() => {
  return expandedCards.value.filter((card) => {
    return card.name.toLowerCase().includes(cardFilter.value.toLowerCase());
  });
});

const nonFilteredCards = computed(() => {
  return expandedCards.value.filter((card) => {
    return !card.name.toLowerCase().includes(cardFilter.value.toLowerCase());
  });
});

function selectCard(id: string) {
  if (selected.value.has(id)) {
    selected.value.delete(id);
  }
  else {
    selected.value.add(id);
  }
}

const contextMenuPos = reactive({ x: 0, y: 0 });
const showMenu = ref(false);
const menuOptions = { options: getMoveZoneActions(0, doMenuAction) };
function moveSelected(e: MouseEvent) {
  contextMenuPos.x = e.clientX;
  contextMenuPos.y = e.clientY;

  showMenu.value = true;
}

function doMenuAction(_: string, ...args: any[]) {
  showMenu.value = false;
  const targetIndex = args[1] ?? -1;
  client.moveCardsVirtual(Array.from(selected.value), args[0], targetIndex);
  visible.value = false;
}
</script>

<template>
  <ContextMenu v-if="showMenu" v-click-outside="() => showMenu = false" :menu="menuOptions" :real-pos="contextMenuPos"
    :z-index="2000" />
  <Modal :visible="visible" @close="visible = false">
    <h2>Find Card</h2>
    <input type="text" placeholder="Filter cards..." v-model="cardFilter">
    <button @click="moveSelected" :disabled="selected.size === 0">Move to...</button>

    <div class="cards">
      <img v-for="card in filteredCards" :src="`https://api.scryfall.com/cards/${card.id}?format=image`"
        @click="selectCard(card.uid)" class="card" :class="{ selected: selected.has(card.uid), filtered: true }" />
      <img v-for="card in nonFilteredCards" :src="`https://api.scryfall.com/cards/${card.id}?format=image`"
        @click="selectCard(card.uid)" class="card" :class="{ selected: selected.has(card.uid), filtered: false }" />
    </div>
  </Modal>
</template>

<style scoped>
.cards {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  overflow: scroll;
}

.card {
  width: 10rem;
  margin: 0.5rem;
  border: 3px solid black;
  border-radius: 15px;
  cursor: pointer;
  filter: grayscale(50%) brightness(75%);
}

.card.filtered {
  filter: none;
}

.selected {
  border-color: red;
  box-shadow: 0 0 1rem rgb(206, 131, 131);
}
</style>