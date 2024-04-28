<script setup lang="ts">
import { Highlight } from '@/api/message';
import { useBoardStore, type CardId } from '@/stores/board';
import { computed } from 'vue';

const board = useBoardStore();

type CondensedMessage = {
  message: string;
  count: number;
  cards: CardId[];
}

const messages = computed(() => {
  const condensedMessages: CondensedMessage[] = [];
  board.logs.forEach((log) => {
    const lastMessage = condensedMessages[condensedMessages.length - 1];
    if (lastMessage && lastMessage.message === log.message) {
      lastMessage.count++;
      if (log.card) {
        lastMessage.cards.push(log.card);
      }
    } else {
      condensedMessages.push({ message: log.message, cards: log.card ? [log.card] : [], count: 1 });
    }
  });
  return condensedMessages.reverse();
});

function hoverMessage(message: CondensedMessage) {
  for (const card of message.cards) {
    board.highlightCard(card, Highlight.LOG);
  }
}

function clearHover(message: CondensedMessage) {
  for (const card of message.cards) {
    board.clearHighlight(card);
  }
}
</script>

<template>
  <div class="game-log">
    <div class="log-message" v-for="msg in messages" @mouseenter="hoverMessage(msg)" @mouseleave="clearHover(msg)">
      {{ msg.message }}
      <span class="log-repeat" v-if="msg.count > 1">(x{{ msg.count }})</span>
    </div>
  </div>
</template>

<style scoped>
.game-log {
  display: flex;
  flex-direction: column-reverse;
  max-width: 250px;
  overflow-y: scroll;
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