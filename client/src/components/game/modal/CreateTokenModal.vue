<script setup lang="ts">
import Modal from '@/components/Modal.vue'
import StyleButton from '@/components/StyleButton.vue';
import LoadingButton from '@/components/LoadingButton.vue';
import CardThumbnail from '@/components/game/modal/CardThumbnail.vue';
import { ref } from 'vue';
import { searchForTokens } from '@/api/game';
import { type OracleCard } from '@/api/message';
import { client } from '@/ws';

defineExpose({ open });

const visible = ref(false);
const nameInput = ref("");
const typeInput = ref("");
const colorsInput = ref("");
const ptInput = ref("");
const textInput = ref("");

const tokens = ref<OracleCard[]>([]);

async function searchClicked(e: MouseEvent) {
  e.preventDefault();
  const name = nameInput.value.trim() || undefined;
  const type = typeInput.value.trim() || undefined;
  const colors = colorsInput.value.trim() || undefined;
  const text = textInput.value.trim() || undefined;
  const pt = ptInput.value.trim() || undefined;

  const result = await searchForTokens(name, type, colors, text, pt);
  tokens.value = result;
}

function open() {
  visible.value = true;
}

function createToken(token: OracleCard) {
  client.createToken(token.id);
  visible.value = false;
}

</script>

<template>
  <Modal :visible="visible" title="Create a Token" @close="visible = false">

    <div class="search-form">
      <form class="form-inputs">
        <h2>Create a Token</h2>
        <em>Search by any or all attributes for a token to create</em>
        <div class="input-row">
          <label for="name">Name: </label>
          <input type="text" id="name" v-model="nameInput" placeholder="e.g. Knight">
        </div>
        <div class="input-row">
          <label for="type">Type: </label>
          <input type="text" id="type" v-model="typeInput" placeholder="e.g. 'Artifact - Clue' or 'Goblin'">
        </div>
        <div class="input-row">
          <label for="colors">Colors: </label>
          <input type="text" id="colors" v-model="colorsInput" placeholder="e.g. 'C' or 'UW'">
        </div>
        <div class="input-row">
          <label for="pt">Power/Toughness: </label>
          <input type="text" id="pt" v-model="ptInput" placeholder="e.g. 2/2">
        </div>
        <div class="input-row">
          <label for="text">Text: </label>
          <input type="text" id="text" v-model="textInput" placeholder="e.g. 'Flying'">
        </div>
        <loading-button @click="searchClicked" class="search-button">Search</loading-button>
      </form>
      <div class="search-results">
        <div v-if="tokens.length > 0" v-for="token in tokens" :key="token.id" class="search-result">
          <CardThumbnail :card="token" />
          <StyleButton @click="createToken(token)" small>Create</StyleButton>
        </div>
        <h3 class="no-results-title" v-else><em>No tokens found for those search parameters.</em></h3>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.search-form {
  display: flex;
  gap: 1rem;
}

.form-inputs {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.input-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;

}

.input-row label {
  margin-right: 0.5rem;
  min-width: 140px;
  display: inline-block;
}

.search-button {
  margin: 0 auto;
}

.search-results {
  display: flex;
  flex-wrap: wrap;
  overflow: scroll;
  max-height: 400px;
  background-color: #eee;
  border: 1px solid #ddd;
  border-radius: 15px;
  justify-content: center;
  width: 40vw;
}

.search-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  margin: 1rem;
}

.no-results-title {
  margin: auto auto;
  max-width: 70%;
  text-align: center;
}
</style>