<script setup lang="ts">
import { ZONES, type ZoneConfig } from '@/zones';
import { onMounted, onUnmounted, reactive, ref, computed, watch } from 'vue';
import { useBoardStore } from '@/stores/board';
import { useZoneStore } from '@/stores/zone';
import ContextMenu from '@/components/ContextMenu.vue';
import CardDispatch from '@/components/game/card/CardDispatch.vue';
import { getZoneContextMenu, type ContextMenuDefinition } from '@/context-menu';
import { useNotificationsCache } from '@/cache/notifications';
import { client } from '@/ws';
import { Highlight } from '@/api/message';

const props = defineProps<{ zone: ZoneConfig }>();

const zoneBounds = ref<HTMLElement | null>(null);
const zoneRect = ref<DOMRect | undefined>(undefined);
const board = useBoardStore();
const zones = useZoneStore();
const isMe = computed(() => board.zoneIsMovable(props.zone.id));
const hasCards = computed(() => board.cards[props.zone.id]?.length > 0);

function resetZoneRect() {
  zoneRect.value = zoneBounds.value?.getBoundingClientRect();
  zones.updateZoneBounds(props.zone.id, zoneRect.value!);
}

const showMenu = ref(false);
const menuPos = reactive({ x: 0, y: 0 });
const menuDefinition = ref<ContextMenuDefinition>({ options: [] });
function showOptions(e: MouseEvent) {
  menuDefinition.value = getZoneContextMenu(props.zone.id, isMe.value, doMenuAction);

  showMenu.value = true;
  menuPos.x = e.clientX;
  menuPos.y = e.clientY;
}

function viewCards() {
  doMenuAction('view-all-cards');
}

function doMenuAction(action: string, ...args: any[]) {
  const notifications = useNotificationsCache();
  showMenu.value = false;
  switch (action) {
    case 'view-all-cards':
      notifications.viewZone(props.zone.id);
      break;
    case 'move-zone':
      client.moveCardsToZone(board.cards[props.zone.id].map(card => card.id), args[0], args[1] ?? -1);
      break;
    case 'scoop':
      client.scoop();
      break;
    case 'shuffle':
      client.shuffle();
      break;
    case 'find-card':
      notifications.findCards();
      break;
    case 'scry':
      notifications.scry(args[0]);
      break;
    case 'show-sideboard':
      notifications.viewZone(ZONES.sideboard.id);
      break;
    default:
      console.error(`Unknown zone action ${action} ${args}`);
  }
}


const dragCoords = reactive({ x: 0, y: 0, width: 0, height: 0 });
const isDragging = ref(false);
function startDrag(e: MouseEvent) {
  if (isMe.value && props.zone.type === 'BATTLEFIELD') {
    // clear all highlights
    for (const card of board.cards[props.zone.id]) {
      card.highlight = Highlight.NONE;
    }

    isDragging.value = true;
    dragCoords.x = e.clientX;
    dragCoords.y = e.clientY;
    dragCoords.width = 0;
    dragCoords.height = 0;
  }
}

function drag(e: MouseEvent) {
  if (isDragging.value) {
    dragCoords.width = e.clientX - dragCoords.x;
    dragCoords.height = e.clientY - dragCoords.y;
  }
}

function endDrag() {
  if (isDragging.value) {
    isDragging.value = false;

    // calculate drag rect in virtual coords
    const rect = zoneRect.value!;
    const top = Math.min(dragCoords.y, dragCoords.y + dragCoords.height);
    const left = Math.min(dragCoords.x, dragCoords.x + dragCoords.width);
    const width = Math.abs(dragCoords.width);
    const height = Math.abs(dragCoords.height);

    // convert to virtual coords
    const x = (left - rect.left) / rect.width;
    const y = (top - rect.top) / rect.height;
    const w = width / rect.width;
    const h = height / rect.height;

    // find all cards inside the rect
    const cards = board.cards[props.zone.id];
    const selected = cards.filter(card => {
      return card.x >= x && card.x <= x + w && card.y >= y && card.y <= y + h;
    });
    selected.forEach(card => {
      card.highlight = Highlight.SELECTED;
    })
  }
}

const dragRectStyles = computed(() => {
  const top = Math.min(dragCoords.y, dragCoords.y + dragCoords.height);
  const left = Math.min(dragCoords.x, dragCoords.x + dragCoords.width);

  return {
    left: `${left}px`,
    top: `${top}px`,
    width: `${Math.abs(dragCoords.width)}px`,
    height: `${Math.abs(dragCoords.height)}px`
  };
});

onMounted(() => {
  if (board.cards[props.zone.id] === undefined) {
    board.cards[props.zone.id] = [];
  }
  window.addEventListener('resize', resetZoneRect);
  window.addEventListener('mousemove', drag);
  window.addEventListener('mouseup', endDrag);
  resetZoneRect();
});

watch(() => props.zone.id, () => {
  resetZoneRect();
});

onUnmounted(() => {
  window.removeEventListener('resize', resetZoneRect);
  window.removeEventListener('mousemove', drag);
  window.removeEventListener('mouseup', endDrag);
});



</script>

<template>
  <div class="zone-box" v-if="zone.type !== 'SIDEBOARD'">
    <card-dispatch v-for="card in board.cards[zone.id]" :key="card.id" :zone="zone.type" :card="card"
      :zone-rect="zoneRect" />
    <div class="zone-bounds" ref="zoneBounds" :style="zone.pos" @mousedown="startDrag">
      <span class="zone-bubble zone-count" v-if="zone.layout === 'stack' && hasCards">
        {{ board.cards[zone.id].length }}</span>
      <span class="zone-bubble zone-options" v-if="zone.layout === 'stack' && hasCards" @click="showOptions" @dblclick="viewCards">≡</span>
    </div>
    <context-menu v-if="showMenu" v-click-outside="() => showMenu = false" :real-pos="menuPos" :menu="menuDefinition" />
  </div>
  <div v-if="isDragging" class="drag-rect" :style="dragRectStyles" />
</template>

<style>
.zone-bounds {
  position: absolute;
  border: 1px solid black;
  background-color: rgba(0, 0, 0, 0.2);
  /* pointer-events: none; */
  box-sizing: border-box;
}

.zone-bubble {
  position: absolute;
  font-size: 0.8rem;
  background-color: rgba(0, 0, 0, 0.4);
  border-radius: 0.2rem;
  margin: 0.1rem;
  z-index: 2;
  color: white;
}

.zone-count {
  bottom: 0;
  right: 0;
  padding: 0.1rem 0.2rem;
}

.zone-options {
  top: 0;
  right: 0;
  font-size: 1em;
  background-color: rgba(0, 0, 0, 0.4);
  padding: 0 0.3rem 0.2rem 0.3rem;
  pointer-events: all;
  cursor: pointer;
}

.zone-options:hover {
  background-color: rgba(0, 0, 0, 0.6);
}

.drag-rect {
  position: absolute;
  border: 1px solid black;
  background-color: rgba(0, 0, 0, 0.2);
  pointer-events: none;
  box-sizing: border-box;
}
</style>