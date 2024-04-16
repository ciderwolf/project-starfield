<script setup lang="ts">
import { ref } from 'vue';

defineProps<{ isSpectator: boolean }>();

const emit = defineEmits<{
  endGame: []
  createToken: []
  createCard: []
  untapAll: []
  rollDie: []
}>();

const expanded = ref(false);
</script>


<template>
  <div class="game-options" v-if="isSpectator">
    <div class="button-group">
      <router-link to="/">
        <button class="icon-button expanded">
          <span class="material-symbols-rounded">home</span>
          <span class="icon-button-text">Go Home</span>
        </button>
      </router-link>
    </div>
  </div>
  <div class="game-options" :class="{ expanded }" v-else>
    <div class="button-group">
      <router-link to="/">
        <button class="icon-button" :class="{ expanded }" title="Go Home">
          <span class="material-symbols-rounded">home</span>
          <span class="icon-button-text">Go Home</span>
        </button>
      </router-link>
    </div>
    <div class="button-group">
      <button class="icon-button" :class="{ expanded }" @click="emit('endGame')" title="End Game">
        <span class="material-symbols-rounded">exit_to_app</span>
        <span class="icon-button-text">End Game</span>
      </button>
      <button class="icon-button" :class="{ expanded }" @click="emit('endGame')" title="New Game">
        <span class="material-symbols-rounded">restart_alt</span>
        <span class="icon-button-text">New Game</span>
      </button>
    </div>
    <div class="button-group">
      <button class="icon-button" :class="{ expanded }" @click="emit('createToken')" title="Create Token">
        <span class="material-symbols-rounded">texture_add</span>
        <span class="icon-button-text">Create Token</span>
      </button>
      <button class="icon-button" :class="{ expanded }" @click="emit('createCard')" title="Create Card">
        <span class="material-symbols-rounded">add_box</span>
        <span class="icon-button-text">Create Card</span>
      </button>
      <button class="icon-button" :class="{ expanded }" @click="emit('rollDie')" title="Roll a Die">
        <span class="material-symbols-rounded">casino</span>
        <span class="icon-button-text">Roll a Die</span>
      </button>
    </div>
    <div class="button-group">
      <button class="icon-button" :class="{ expanded }" @click="emit('untapAll')" title="Untap All">
        <span class="material-symbols-rounded">rotate_90_degrees_ccw</span>
        <span class="icon-button-text">Untap All</span>
      </button>
    </div>
    <button class="icon-button" :class="{ expanded }" @click="expanded = !expanded" title="Show Details">
      <span v-if="expanded" class="material-symbols-rounded">expand_less</span>
      <span v-else class="material-symbols-rounded">expand_more</span>
      <span class="icon-button-text">Show Less</span>
    </button>
  </div>

</template>

<style scoped>
.game-options {
  border: none;
  background-color: #0005;
  font-size: 1rem;
  text-align: center;
  /* border-radius: 5px; */
  display: flex;
  width: 250px;
  justify-content: center;
}

.game-options.expanded {
  flex-direction: column;
}

.game-options button:hover {
  background-color: #0008;
}

.button-group {
  display: flex;
  border: none;
}

.button-group:not(:last-child) {
  border-right: 1px solid #fff5;
}

.game-options.expanded .button-group {
  flex-direction: column;
  border-bottom: 1px solid #fff5;
  border-right: none;
}

.icon-button {
  display: flex;
  align-items: center;
  padding: 5px;
  gap: 5px;
  border: none;
  background-color: transparent;
  color: white;
  padding: 5px;
  margin: 0;
  cursor: pointer;
  border-radius: 5px;
}

.icon-button.expanded {
  width: 100%;
}

.icon-button-text {
  display: none;
  font-size: 1em;
}

.icon-button.expanded .icon-button-text {
  display: block;
}
</style>