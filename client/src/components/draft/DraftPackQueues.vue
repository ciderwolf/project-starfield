<script setup lang="ts">
import { useDraftStore } from '@/stores/draft';

const draft = useDraftStore();

function getPlayerIdentifier(id: string) {
  return draft.players.find(player => player.id === id)?.name ?? 'Bot Player';
}

function getPlayerIcon(id: string) {
  return draft.players.some(player => player.id === id) ? 'person' : 'robot_2';
}

</script>

<template>
  <div class="draft-pack-queues">
    <p class="pack-queue-player" v-for="[id, packs] in Object.entries(draft.packQueues)" :key="id">
      <span class="material-symbols-rounded">{{ getPlayerIcon(id) }}</span>
      <span>
        {{ getPlayerIdentifier(id) }} ({{ packs }})
      </span>
    </p>
  </div>
</template>

<style scoped>
.draft-pack-queues {
  margin-top: 20px;
}

.pack-queue-player {
  display: flex;
  align-items: center;
  margin: 5px 0;
}

.pack-queue-container {
  position: relative;
}

.pack-queue-player {
  position: relative;
  /* display: inline-block; */
  margin: 10px;
}
</style>