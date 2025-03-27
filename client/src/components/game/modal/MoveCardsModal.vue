<script setup lang="ts">
import Modal from '@/components/Modal.vue'
import ContextMenu from '@/components/ContextMenu.vue';
import StyleButton from '@/components/StyleButton.vue';
import CardThumbnail from '@/components/game/modal/CardThumbnail.vue';
import { computed, onMounted, onUnmounted, reactive, ref, type ComputedRef } from 'vue';
import { getMoveZoneActions, type ContextMenuDefinition, type ContextMenuOption } from '@/context-menu';
import { useBoardStore, UserType, type OracleId } from '@/stores/board';
import type { OracleCard } from '@/api/message';

const props = defineProps<{ cards: { [id: string]: OracleId }, title: string, userType: UserType, order?: string[], persist?: boolean, virtual?: boolean }>();

type IdentifiedDeckCard = { uid: string } & OracleCard;

defineExpose({ open, close });
const emit = defineEmits<{
  (event: 'select', ids: string[], zoneId: number, index: number): void
  (event: 'copy-face-down', id: string): void
  (event: 'play-face-down', id: string): void
}>()
const board = useBoardStore();
const multiSelect = computed(() => props.userType === UserType.PLAYER);
const readOnly = computed(() => props.userType === UserType.SPECTATOR);

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
  singleSelected.value = null;
  cardFilter.value = '';
  visible.value = true;
}
function close() {
  visible.value = false;
}

const selected = ref<Set<string>>(new Set());
const singleSelected = ref<string | null>(null);
const cardFilter = ref('');


const filteredCards = computed(() => {
  return expandedCards.value.filter((card) => {
    return (card.name ?? "").toLowerCase().includes(cardFilter.value.toLowerCase());
  });
});

const nonFilteredCards = computed(() => {
  return expandedCards.value.filter((card) => {
    return !(card.name ?? "").toLowerCase().includes(cardFilter.value.toLowerCase());
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
  if (readOnly.value) {
    return;
  }

  if (multiSelect.value && isShiftPressed.value) {
    singleSelected.value = null;
    if (selected.value.has(id)) {
      selected.value.delete(id);
    }
    else {
      selected.value.add(id);
    }
  } else {
    selected.value.clear();
    moveSelected(e);
    singleSelected.value = id;
  }
}

const contextMenuPos = reactive({ x: 0, y: 0 });
const showMenu = ref(false);
const menuOptionValues: ComputedRef<ContextMenuOption[]> = computed(() => {
  if (props.virtual) {
    return getMoveZoneActions(0, doMenuAction);
  }
  if (multiSelect.value === true && selected.value.size === 0) {
    return [
      ...getMoveZoneActions(0, doMenuAction),
      {
        type: 'seperator'
      },
      {
        title: 'Play Face Down',
        effect: () => doMenuAction('play-face-down'),
        type: 'text',
      }
    ]
  }
  else if (multiSelect.value === true) {
    return getMoveZoneActions(0, doMenuAction);
  }
  else {
    return [
      {
        title: 'Copy Face Down',
        effect: () => doMenuAction('copy-face-down'),
        type: 'text',
      }
    ];
  }
})
const menuOptions: ComputedRef<ContextMenuDefinition> = computed(() => ({ options: menuOptionValues.value }));
function moveSelected(e: MouseEvent) {
  contextMenuPos.x = e.clientX;
  contextMenuPos.y = e.clientY;

  showMenu.value = true;
}

function getSelectedCards() {
  if (singleSelected.value) {
    return [singleSelected.value];
  } else if (selected.value.size > 0) {
    return Array.from(selected.value);
  } else {
    return [];
  }
}

function doMenuAction(action: string, ...args: any[]) {
  showMenu.value = false;
  switch (action) {
    case 'copy-face-down':
      emit('copy-face-down', getSelectedCards()[0]);
      return;
    case 'play-face-down':
      emit('play-face-down', getSelectedCards()[0]);
      return;
    case 'move-zone':
      const targetIndex = args[1] ?? -1;
      emit('select', getSelectedCards(), args[0], targetIndex);
      break;
    default:
      console.error('Unknown action', args);
      return;
  }

  singleSelected.value = null;
  selected.value.clear();
  if (!props.persist) {
    close();
  }
}

const isShiftPressed = ref(false);
function keyPressed(e: KeyboardEvent) {
  if (e.key === 'Shift') {
    isShiftPressed.value = true;
  }
}

function keyReleased(e: KeyboardEvent) {
  if (e.key === 'Shift') {
    isShiftPressed.value = false;
  }
}

onMounted(() => {
  window.addEventListener('keydown', keyPressed);
  window.addEventListener('keyup', keyReleased);
});

onUnmounted(() => {
  window.removeEventListener('keydown', keyPressed);
  window.removeEventListener('keyup', keyReleased);
});

</script>

<template>
  <ContextMenu v-if="showMenu" v-click-outside="() => showMenu = false" :menu="menuOptions" :real-pos="contextMenuPos"
    :z-index="2000000" />
  <Modal :visible="visible" @close="visible = false" :title="title">
    <h2>{{ title }}</h2>
    <div class="inputs">
      <input type="text" placeholder="Filter cards..." v-model="cardFilter">
      <style-button @click="moveSelected" :disabled="selected.size === 0" v-if="multiSelect && !readOnly" small>Move
        to...</style-button>
      <i v-if="multiSelect">Hold Shift to select multiple cards</i>
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
  overflow-y: scroll;
}

.inputs {
  display: flex;
  gap: 10px;
  align-items: center;
}

.inputs i {
  color: gray;
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
  box-shadow: 0 0 0.3rem rgb(206, 131, 131);
}
</style>
