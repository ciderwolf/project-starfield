<script setup lang="ts">
import type { ZoneConfig } from '@/zones';
import { onMounted, onUnmounted, reactive, ref, computed } from 'vue';
import { useBoardStore } from '@/stores/board';
import { useZoneStore } from '@/stores/zone';
import ContextMenu from '@/components/ContextMenu.vue';
import CardDispatch from '@/components/game/card/CardDispatch.vue';
import { getZoneContextMenu, type ContextMenuDefinition } from '@/context-menu';
import { useNotificationsCache } from '@/cache/notifications';
import { client } from '@/ws';

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

function doMenuAction(action: string, ...args: any[]) {
  const notifications = useNotificationsCache();
  showMenu.value = false;
  switch (action) {
    case 'view-all-cards':
      notifications.viewZone(props.zone.id, !board.zoneIsMovable(props.zone.id));
      break;
    case 'move-zone':
      client.moveCardsToZone(board.cards[props.zone.id].map(card => card.id), args[0], args[1] ?? -1);
      break;
    default:
      console.error(`Unknown zone action ${action} ${args}`);
  }
}

onMounted(() => {
  if (board.cards[props.zone.id] === undefined) {
    board.cards[props.zone.id] = [];
  }
  window.addEventListener('resize', resetZoneRect);
  resetZoneRect();
});

onUnmounted(() => {
  window.removeEventListener('resize', resetZoneRect);
});

</script>

<template>
  <div class="zone-box" v-if="zone.type !== 'SIDEBOARD'">
    <card-dispatch v-for="card in board.cards[zone.id]" :key="card.id" :zone="zone.type" :card="card"
      :zone-rect="zoneRect" />
    <div class="zone-bounds" ref="zoneBounds" :style="zone.pos">
      <span class="zone-bubble zone-count" v-if="zone.layout === 'stack' && hasCards">
        {{ board.cards[zone.id].length }}</span>
      <span class="zone-bubble zone-options" v-if="zone.layout === 'stack' && zone.type != 'LIBRARY' && hasCards"
        @click="showOptions">≡</span>
    </div>
    <context-menu v-if="showMenu" v-click-outside="() => showMenu = false" :real-pos="menuPos" :menu="menuDefinition" />
  </div>
</template>

<style>
.zone-bounds {
  position: absolute;
  border: 1px solid black;
  background-color: rgba(0, 0, 0, 0.2);
  pointer-events: none;
  box-sizing: border-box;
  pointer-events: none;
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
</style>