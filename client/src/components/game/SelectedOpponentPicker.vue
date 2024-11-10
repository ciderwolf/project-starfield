<script setup lang="ts">
import { useBoardStore } from '@/stores/board';
import { useZoneStore } from '@/stores/zone';
import { computed, reactive, watch } from 'vue';

const board = useBoardStore();
const zones = useZoneStore();

const opponentNotifications = reactive<{ [id: string]: number }>({});
let previousLogIndex = 0;

watch([board.logs], () => {
  for (let i = previousLogIndex; i < board.logs.length; i++) {
    const log = board.logs[i];
    if (opponentNotifications[log.owner] === undefined) {
      opponentNotifications[log.owner] = 0;
    }
    if (log.owner !== zones.primaryPlayer?.id && log.owner !== zones.secondaryPlayer?.id) {
      opponentNotifications[log.owner]++;
    }
  }
  previousLogIndex = board.logs.length;
});

const otherPlayers = computed(() => {
  return Object.keys(board.players).filter(p => p !== zones.primaryPlayer?.id && p !== zones.secondaryPlayer?.id);
});

function selectPlayer(player: string) {
  zones.secondaryPlayer = board.players[player];
  opponentNotifications[player] = 0;
}

</script>

<template>
  <div class="opponents-picker">
    <div v-for="player in otherPlayers" class="opponent-button"
      :class="{ current: board.currentPlayer === board.players[player].index }" :title="board.players[player].name"
      @click="selectPlayer(player)">
      <span class="material-symbols-rounded" v-if="!opponentNotifications[player]">account_circle</span>
      <span class="notification-badge" v-else>{{ opponentNotifications[player] }}</span>
      <span class="player-name">{{ board.players[player].name }}</span>
    </div>
  </div>
</template>

<style scoped>
.opponents-picker {
  border: none;
  font-size: 1rem;
  text-align: center;
  display: flex;
  width: 250px;
  justify-content: space-between;
  position: relative;
}

.opponent-button {
  display: flex;
  align-items: center;
  padding: 5px;
  gap: 5px;
  border: none;
  background: white;
  color: black;
  padding: 5px;
  margin: 0;
  cursor: pointer;
  border-radius: 5px;
  min-width: 0;
  flex: 1;
}

.opponent-button.current {
  background: lightblue;
}

.notification-badge {
  background-color: red;
  color: white;
  border-radius: 50%;
  padding: 2px;
  font-size: 0.8em;
  margin-left: 5px;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.player-name {
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}
</style>