<script setup lang="ts">
import { type PlayerAttributes, useBoardStore } from '@/stores/board';
import { client } from '@/ws';
import { ScreenPosition, ZONES } from '@/zones';
import { computed, ref, type StyleValue } from 'vue';
import ContextMenu from '@/components/shared/ContextMenu.vue';
import { createPlayerContextMenu } from '@/context-menu';
import { useNotificationsCache } from '@/cache/notifications';

const props = defineProps<{ player: PlayerAttributes }>();
const board = useBoardStore();

const playerInfoPositionInfo = computed<StyleValue>(() => {
  const position = board.getScreenPositionFromPlayerIndex(props.player.index);
  if (position == ScreenPosition.PRIMARY) {
    return {
      right: '0',
      bottom: '0',
      backgroundColor: board.currentPlayer === props.player.index ? 'var(--color-accent-highlight)' : 'var(--color-white)',
      flexDirection: 'column-reverse',
    };
  } else {
    return {
      right: '0',
      top: '0',
      backgroundColor: board.currentPlayer === props.player.index ? 'var(--color-accent-highlight)' : 'var(--color-white)',
      flexDirection: 'column',
    };
  }
});

const arrow = computed(() => {
  const position = board.getScreenPositionFromPlayerIndex(props.player.index);
  if (position == ScreenPosition.PRIMARY) {
    return 'arrow_upward';
  } else {
    return 'arrow_downward';
  }
});

const isMe = computed(() => {
  return board.playerIsMovable(props.player.index);
});

const isMyTurn = computed(() => {
  return board.currentPlayer === props.player.index;
});

function endTurn() {
  if (isMe.value) {
    client.endTurn();
  }
}

function showPlayerMenu(e: MouseEvent) {
  if (isMe.value) {
    showMenu.value = true;
    contextMenuPos.value = { x: e.clientX, y: e.clientY };
  }
}

const showMenu = ref(false);
const contextMenuPos = ref({ x: 0, y: 0 });
const menuOptions = createPlayerContextMenu(props.player, doMenuAction);

function doMenuAction(action: string) {
  showMenu.value = false;
  const notifications = useNotificationsCache();
  switch (action) {
    case 'scoop':
      client.scoop();
      break;
    case 'show-sideboard':
      notifications.viewZone(ZONES.sideboard.id);
      break;
    case 'scoop-sideboard':
      notifications.sideboard();
      break;
  }
}
</script>

<template>
  <ContextMenu v-if="showMenu" v-click-outside="() => showMenu = false" :menu="menuOptions" :real-pos="contextMenuPos"
    :z-index="2000" />
  <div class="player-info" :style="playerInfoPositionInfo">
    <span class="player-name" :title="player.name">{{ player.name }}</span>
    <img src="@/assets/account-circle.svg" :class="{ [`my-profile`]: isMe }" @click="showPlayerMenu">
    <span v-if="isMyTurn && isMe" @click="endTurn()" class="end-turn-indicator end-turn-button">
      <span class="material-symbols-rounded ">arrow_upward</span>
      End Turn
    </span>
    <span v-else-if="isMyTurn" class="end-turn-indicator">
      <span class="material-symbols-rounded">{{ arrow }}</span>
      <i>Waiting</i>
    </span>
    <span v-else style="color: var(--color-text-muted); cursor: not-allowed;"
      class="end-turn-indicator end-turn-button">
      <span class="material-symbols-rounded">{{ arrow }}</span>
      <i>End Turn</i>
    </span>
  </div>
</template>

<style scoped>
.player-info {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  position: absolute;
  padding: var(--space-xxs);
  height: 108px;
  width: 94px;
  font-size: var(--font-size-xs);

  background-color: var(--color-white);
  border: 1px solid var(--color-black);
  border-radius: 0;
  box-sizing: border-box;
}

.player-info .player-name {
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
  max-width: 90px;
  width: min-content;
  display: block;
}

.player-info .my-profile {
  cursor: pointer;
}

.player-info .end-turn-indicator {
  display: flex;
  align-items: center;
  gap: var(--space-xxs);
  padding-right: var(--space-xs);
}

.player-info .end-turn-button {
  cursor: pointer;
  transition: background-color var(--transition-normal);
  border-radius: var(--radius-sm);
}

.player-info .end-turn-button:hover {
  background-color: #eee8;

}
</style>