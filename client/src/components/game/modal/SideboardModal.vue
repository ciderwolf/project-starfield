<script setup lang="ts">
import { getVirtualSideboardingIds } from '@/api/game';
import type { OracleCard } from '@/api/message';
import Modal from '@/components/Modal.vue'
import StyleButton from '@/components/StyleButton.vue';
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import CardPreview from '@/components/deck/CardPreview.vue';
import { client } from '@/ws';
import { ref } from 'vue';

defineExpose({ open });

type IdentifiedDeckCard = {
  id: string;
  name: string;
  image: string;
  backImage: string | null;
  virtualId: string;
}

const visible = ref(false);
const sideboardCards = ref<IdentifiedDeckCard[]>([]);
const maindeckCards = ref<IdentifiedDeckCard[]>([]);

function mapCard(card: OracleCard, virtualId: string): IdentifiedDeckCard {
  return {
    id: card.id,
    name: card.name,
    image: `https://api.scryfall.com/cards/${card.id}?format=image`,
    backImage: card.hasBackFace ? `https://api.scryfall.com/cards/${card.id}?format=image&face=back` : null,
    virtualId
  };
}

async function open() {
  visible.value = true;
  const { main, side, oracleInfo } = await getVirtualSideboardingIds();
  maindeckCards.value = Object.entries(main)
    .map(([virtualId, oracleId]) => mapCard(oracleInfo[oracleId], virtualId))
    .sort((a, b) => a.name.localeCompare(b.name));
  sideboardCards.value = Object.entries(side)
    .map(([virtualId, oracleId]) => mapCard(oracleInfo[oracleId], virtualId));
}

function moveToSideboard(card: IdentifiedDeckCard) {
  sideboardCards.value.push(card);
  maindeckCards.value = maindeckCards.value.filter(c => c.virtualId !== card.virtualId);
}

function moveToMaindeck(card: IdentifiedDeckCard) {
  maindeckCards.value.push(card);
  sideboardCards.value = sideboardCards.value.filter(c => c.virtualId !== card.virtualId);
}

async function submitSideboardChoices() {
  const maindeck = maindeckCards.value.map(c => c.virtualId);
  const sideboard = sideboardCards.value.map(c => c.virtualId);

  client.sideboard(maindeck, sideboard);

  visible.value = false;
}

</script>

<template>
  <Modal :visible="visible" title="Sideboard" @close="visible = false">
    <h2>Sideboard</h2>
    <div class="deck-chooser" v-if="maindeckCards.length > 0 || sideboardCards.length > 0">
      <div class="deck-column">
        <h3>Maindeck ({{ maindeckCards.length }})</h3>
        <div class="card" v-for="card of maindeckCards" :key="card.virtualId">
          <style-button small @click="moveToSideboard(card)">Side ></style-button>
          <card-preview :card="card"></card-preview>
        </div>
      </div>
      <div class="deck-column">
        <h3>Sideboard ({{ sideboardCards.length }})</h3>
        <div class="card" v-for="card of sideboardCards" :key="card.virtualId">
          <style-button small @click="moveToMaindeck(card)">&lt; Main</style-button>
          <card-preview :card="card"></card-preview>
        </div>
      </div>
    </div>
    <div class="loading-screen" v-else><loading-spinner /> Loading...</div>
    <style-button @click="submitSideboardChoices">Confirm Deck</style-button>
  </Modal>
</template>

<style scoped>
.deck-chooser {
  display: flex;
  justify-content: space-around;
}

.deck-column {
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #eee;
  border: 1px solid #ddd;
  border-radius: 5px;
  padding: 10px;
}

.card {
  display: flex;
  align-items: center;
  gap: 10px;
  overflow: hidden;
  white-space: nowrap;
  width: 300px;
}

.loading-screen {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}
</style>