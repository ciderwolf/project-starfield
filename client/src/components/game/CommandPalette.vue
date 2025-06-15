<script setup lang="ts">

import { rollDie } from '@/api/game';
import { endGame } from '@/api/lobby';
import { useNotificationsCache } from '@/cache/notifications';
import { client } from '@/ws';
import { ZONES } from '@/zones';
import { ref, computed, onMounted, onUnmounted, type ComputedRef, watch } from 'vue';
import { useRoute } from 'vue-router';

interface Command {
  name?: string;
  pattern?: RegExp;
  aliases?: string[];
  description: string;
  action: (args?: string[]) => void;
}
const notifications = useNotificationsCache();
const inputBox = ref<HTMLInputElement | null>(null);
const visible = ref(false);
const route = useRoute();

defineExpose({ open });

function open() {
  visible.value = true;
  setTimeout(() => {
    if (inputBox.value) {
      inputBox.value.focus();
    }
  }, 0);
}

const commands: Command[] = [
  {
    name: 'Find Cards',
    aliases: ['Search Library', 'Search your Library'],
    description: 'Find cards in your deck',
    action: () => {
      notifications.findCards();
    }
  },
  {
    name: 'Scry',
    pattern: /^scry (\d+)$/,
    description: 'Look at the top card(s) of your library',
    action: (args?: string[]) => {
      let count: number;
      console.log(args);
      if (args && args.length > 0) {
        count = parseInt(args[0], 10);
      } else {
        // get user input
        const input = prompt('Enter the number of cards to scry:');
        if (input) {
          count = parseInt(input, 10);
        } else {
          console.error('Invalid input');
          return;
        }
      }
      notifications.scry(count);
    }
  },
  {
    name: 'Mulligan',
    description: 'Scoop your deck and draw seven cards',
    action: () => {
      client.mulligan();
    },
  },
  {
    name: 'End Game',
    description: 'End the current game and leave',
    action: () => {
      const gameId = route.params.id as string;
      endGame(gameId);
    },
  },
  {
    name: 'Roll Die',
    aliases: ['Flip coin', 'flip a coin', 'roll dice'],
    pattern: /^roll (?:a )?d(\d+)$/,
    description: 'Roll a die',
    action: (args) => {
      let sides = NaN;
      if (args && args.length > 0) {
        sides = parseInt(args[0], 10);
      }
      if (isNaN(sides)) {
        notifications.rollDie();
      } else {
        rollDie(sides);
      }
    },
  },
  {
    name: 'Create Card',
    description: 'Create a new card from outside the game',
    action: () => {
      notifications.createCard();
    },
  },
  {
    name: 'Create Token',
    description: 'Create a token',
    action: () => {
      notifications.createToken();
    },
  },
  {
    name: 'View Sideboard',
    description: 'View your sideboard',
    action: () => {
      notifications.viewZone(ZONES.sideboard.id);
    },
  },
  {
    name: 'Sideboard',
    description: 'Move cards between your sideboard and deck',
    action: () => {
      notifications.sideboard();
    },
  },
  {
    name: 'Draw Cards',
    pattern: /^draw (\d+)$/,
    description: 'Draw cards from your library',
    action: (ctx) => {
      let count = 1;
      if (ctx && ctx.length > 0) {
        count = parseInt(ctx[0], 10);
      }
      client.drawCards(count);
    },
  },
  {
    name: 'Mill Cards',
    pattern: /^mill (\d+)$/,
    description: 'Mill cards from the top your library',
    action: (ctx) => {
      let count = 1;
      if (ctx && ctx.length > 0) {
        count = parseInt(ctx[0], 10);
      }
      client.drawCards(count, 'GRAVEYARD');
    },
  },
  {
    name: 'Shuffle',
    description: 'Shuffle your library',
    action: () => {
      client.shuffle();
    },
  },
  {
    name: "Untap all",
    description: "Untap all cards in play",
    action: () => {
      client.untapAll();
    },
  }
];

const emit = defineEmits(['close']);
const searchQuery = ref('');
const selectedIndex = ref(-1);

const filteredCommands = computed(() => {
  if (!searchQuery.value) {
    return [];
  }
  const matchValue = searchQuery.value.trim().toLowerCase();

  return commands.filter(command =>
    command.name?.toLowerCase().includes(matchValue) ||
    command.description.toLowerCase().includes(matchValue) ||
    (command.pattern && command.pattern.test(matchValue)) ||
    (command.aliases && command.aliases.some(alias => alias.toLowerCase().includes(matchValue)))
  );
});

watch(filteredCommands, (filtered, previous) => {
  let selectedId: string | undefined = undefined;
  if (previous && selectedIndex.value >= 0) {
    selectedId = previous[selectedIndex.value].name;
  }

  if (selectedId === undefined) {
    selectedIndex.value = -1;
  } else {
    const index = filtered.findIndex(command => command.name === selectedId);
    if (index !== -1) {
      selectedIndex.value = index;
    } else {
      selectedIndex.value = -1;
    }
  }
});

const closePalette = () => {
  if (visible.value === false) {
    return;
  }
  emit('close');
  console.log('Command palette closed');
  visible.value = false;
};

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    closePalette();
  }
  if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
    event.preventDefault();
    let offset: number;
    if (event.key === 'ArrowUp') {
      offset = filteredCommands.value.length - 1;
    } else {
      offset = 1;
    }
    selectedIndex.value = (selectedIndex.value + offset) % filteredCommands.value.length;
  }
  if (event.key === 'Enter') {
    const index = selectedIndex.value === -1 ? 0 : selectedIndex.value;
    const selectedCommand = filteredCommands.value[index];
    if (selectedCommand) {
      handleCommandClick(selectedCommand);
    }
  }
};

onMounted(() => {
  window.addEventListener('keydown', handleKeydown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown);
});

function handleCommandClick(command: Command) {
  console.log(`Command clicked: ${command.name}`);
  if (command.pattern) {
    const match = searchQuery.value.match(command.pattern);
    if (match) {
      const args = match.slice(1);
      command.action(args);
    } else {
      command.action();
    }
  } else {
    command.action();
  }
  closePalette();
}

</script>

<template>
  <div class="command-palette" v-if="visible">
    <div class="command-palette-content" v-click-outside="closePalette">
      <div class="search-bar">
        <span class="material-symbols-rounded">search</span>
        <input ref="inputBox" type="text" placeholder="Search commands or game actions..." v-model="searchQuery" />
      </div>
      <ul v-if="filteredCommands.length">
        <li v-for="command, index in filteredCommands" :key="command.name" @click="handleCommandClick(command)"
          :class="{ selected: selectedIndex === index }" :data-id="index">
          <span>{{ command.name }}</span>
          <span class="description">{{ command.description }}</span>

        </li>
      </ul>
      <span v-else-if="searchQuery">
        <p>No commands found</p>
      </span>
      <span v-else>
        <p>Search for commands to see them here</p>
      </span>
    </div>
  </div>
</template>

<style scoped>
.description {
  font-size: 0.8em;
  color: #999;
  margin-left: 8px;
  font-style: italic;
}

li.selected .description {
  color: #d0d0d0;
}

.command-palette {
  position: absolute;
  height: 100vh;
  width: 100vw;
  z-index: 100;
  pointer-events: none;
}

.command-palette-content {
  pointer-events: all;
  background-color: #fff8;
  backdrop-filter: blur(10px);
  border: 1px solid #ccc;
  border-radius: 4px;
  min-width: 500px;
  max-width: 40%;
  margin: 0 auto;
  margin-top: 25vh;

}


.command-palette-content .search-bar {
  display: flex;
  width: 100%;
  padding: 8px;
  border-bottom: 1px solid #ccc;
  border-radius: 0;
  box-sizing: border-box;
}

.search-bar input {
  flex: 1;
  border: none;
  outline: none;
  padding-left: 8px;

  background-color: transparent;
}

.command-palette-content ul {
  list-style: none;
  padding: 0;
}

.command-palette-content li {
  padding: 8px;
  cursor: pointer;
}

.command-palette-content li:hover {
  background-color: #f0f0f0;
}

.command-palette-content li.selected {
  background-color: #007bff;
  color: #fff;
}

.command-palette-content li.selected:hover {
  background-color: #0056b3;
}

p {
  color: #999;
  text-align: center;
  margin: 0;
  font-style: italic;
  padding: 8px;
}
</style>