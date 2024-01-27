<script setup lang="ts">
import Modal from '@/components/Modal.vue'
import StyleButton from '@/components/StyleButton.vue';
import CardThumbnail from '@/components/game/modal/CardThumbnail.vue';
import { ref } from 'vue';
import { searchForCards } from '@/api/game';
import { type OracleCard } from '@/api/message';
import { client } from '@/ws';

defineExpose({ open });

const visible = ref(false);
const nameInput = ref("");
const searched = ref("");

const cards = ref<OracleCard[]>([]);

async function searchClicked() {
  const name = nameInput.value.trim();
  if (name.length === 0) {
    return;
  }
  const result = await searchForCards(name);
  cards.value = result;
  searched.value = name;
}

function open() {
  visible.value = true;
}

function createCard(token: OracleCard) {
  client.createCard(token.id);
  visible.value = false;
}

</script>

<template>
  <Modal :visible="visible" title="Create a Token" @close="visible = false">

    <div class="search-form">
      <div class="form-inputs">
        <h2>Create a Card</h2>
        <div class="input-row">
          <label for="name">Name:</label>
          <input type="text" id="name" v-model="nameInput" placeholder="e.g. Brisela">
          <StyleButton @click="searchClicked" class="search-button" small>Search</StyleButton>
        </div>

      </div>
      <div class="search-results">
        <div v-if="cards.length > 0" v-for="token in cards" :key="token.id" class="search-result">
          <CardThumbnail :card="token" />
          <StyleButton @click="createCard(token)" small>Create</StyleButton>
        </div>
        <h3 class="no-results-title" v-else-if="!searched"><em>Search for cards by name to see them here.</em></h3>
        <h3 class="no-results-title" v-else><em>No cards found that matched '{{ searched }}'.</em></h3>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.search-form {
  display: flex;
  flex-direction: column;
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
  gap: 10px;
  align-items: center;
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
  width: 100%;
}

.search-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  margin: 1rem;
}

.no-results-title {
  margin: 2rem auto;
  max-width: 70%;
  text-align: center;
}
</style>