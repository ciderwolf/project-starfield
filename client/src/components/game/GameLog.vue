<script setup lang="ts">
import { useBoardStore } from '@/stores/board';
import { computed } from 'vue';

const board = useBoardStore();

type CondensedMessage = {
  message: string;
  count: number;
}

const messages = computed(() => {
  const condensedMessages: CondensedMessage[] = [];
  board.logs.forEach((log) => {
    const lastMessage = condensedMessages[condensedMessages.length - 1];
    if (lastMessage && lastMessage.message === log) {
      lastMessage.count++;
    } else {
      condensedMessages.push({ message: log, count: 1 });
    }
  });
  return condensedMessages.reverse();
});
</script>

<template>
  <div class="game-log">
    <div class="log-message" v-for="msg in messages">{{ msg.message }} <span class="log-repeat"
        v-if="msg.count > 1">x({{ msg.count }})</span></div>
  </div>
</template>

<style scoped>
.game-log {
  display: flex;
  flex-direction: column-reverse;
  max-width: 250px;
  overflow: scroll;
}

.log-message {
  background-color: #eee;
  color: black;
  padding: 5px;
  margin: 2px;
  border-radius: 5px;
  font-size: 0.8rem;
}

.log-message:hover {
  background-color: #ddd;
  cursor: default;
}

.log-repeat {
  font-size: 0.6rem;
  color: #3f65b0;
}
</style>