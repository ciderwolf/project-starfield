<script setup lang="ts">
import Modal from '@/components/Modal.vue'
import ContextMenu from '@/components/ContextMenu.vue';
import StyleButton from '@/components/StyleButton.vue';
import CardThumbnail from '@/components/game/modal/CardThumbnail.vue';
import { computed, reactive, ref } from 'vue';
import { getMoveZoneActions } from '@/context-menu';
import { useBoardStore, type OracleId } from '@/stores/board';
import type { OracleCard } from '@/api/message';

const props = defineProps<{ cards: { [id: string]: OracleId }, multiSelect: boolean, title: string, readOnly?: boolean, order?: string[], persist?: boolean }>();

type IdentifiedDeckCard = { uid: string } & OracleCard;

defineExpose({ open, close });
const emit = defineEmits<{
  (event: 'select', ids: string[], zoneId: number, index: number): void
}>()

const board = useBoardStore();

const expandedCards = computed(() => {
  const expanded: IdentifiedDeckCard[] = [];
  const keys = props.order === undefined ? Object.keys(props.cards) : props.order;
  for (const virtualId of keys) {
    const oracleId = props.cards[virtualId];
    const card = board.oracleInfo[oracleId];
    expanded.push({ ...card, uid: virtualId });
  }
  if (props.order !== undefined) {
    return expanded;
  } else {
    return expanded.sort((a, b) => a.name?.localeCompare(b.name));
  }

});
const visible = ref(false);
function open() {
  selected.value.clear();
  cardFilter.value = '';
  visible.value = true;
}
function close() {
  visible.value = false;
}

const selected = ref<Set<string>>(new Set());
const cardFilter = ref('');


const filteredCards = computed(() => {
  return expandedCards.value.filter((card) => {
    return card.name?.toLowerCase().includes(cardFilter.value.toLowerCase());
  });
});

const nonFilteredCards = computed(() => {
  return expandedCards.value.filter((card) => {
    return !card.name?.toLowerCase().includes(cardFilter.value.toLowerCase());
  });
});

function getClassString(card: IdentifiedDeckCard, filter: boolean) {
  const classes = ['card-thumbnail__card'];
  if (selected.value.has(card.uid)) {
    classes.push('selected');
  }
  if (filter) {
    classes.push('filtered');
  }

  return classes.join(' ');
}
function selectCard(e: MouseEvent, id: string) {
  if (props.readOnly) {
    return;
  }

  if (props.multiSelect) {
    if (selected.value.has(id)) {
      selected.value.delete(id);
    }
    else {
      selected.value.add(id);
    }
  } else {
    moveSelected(e);
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
  emit('select', Array.from(selected.value), args[0], targetIndex);
  if (!props.persist) {
    close();
  }
}
</script>

<template>
  <ContextMenu v-if="showMenu" v-click-outside="() => showMenu = false" :menu="menuOptions" :real-pos="contextMenuPos"
    :z-index="2000" />
  <Modal :visible="visible" @close="visible = false" :title="title">
    <h2>{{ title }}</h2>
    <div class="inputs">
      <input type="text" placeholder="Filter cards..." v-model="cardFilter">
      <style-button @click="moveSelected" :disabled="selected.size === 0" v-if="multiSelect && !readOnly" small>Move
        to...</style-button>
    </div>
    <div class="cards">
      <card-thumbnail v-for="card in filteredCards" :class="getClassString(card, true)" :card="card"
        @click="selectCard($event, card.uid)" />
      <card-thumbnail v-for="card in nonFilteredCards" :class="getClassString(card, false)" :card="card"
        @click="selectCard($event, card.uid)" />
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

.inputs {
  display: flex;
  gap: 10px;
}
</style>

<style>
img.card-thumbnail__card {
  width: 10rem;
  margin: 0.5rem;
  border: 3px solid black;
  border-radius: 10px !important;
  cursor: pointer;
  filter: grayscale(50%) brightness(75%);
}

.card-thumbnail__card.filtered {
  filter: none;
}

.card-thumbnail__card.selected {
  border-color: red;
  box-shadow: 0 0 1rem rgb(206, 131, 131);
}
</style>
