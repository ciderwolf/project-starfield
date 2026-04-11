<script setup lang="ts">
import { type CardDetails, type OracleCard } from '@/api/message';
import StyleButton from '@/components/StyleButton.vue';
import { useSearchResultsStore } from '@/stores/search-results';
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { cardSymbols } from '@/utils/card-utils';

const route = useRoute();

const cardId = route.params.id as string;
const parts = ref<CardDetails[]>([]);
const card = ref<OracleCard>();
const imageUrl = ref<string>('');
const cardFlipped = ref(false);
const cardSearchStore = useSearchResultsStore();

onMounted(async () => {
  const response = await cardSearchStore.getCardDetails(cardId);
  card.value = response.oracleCard;
  parts.value = response.parts;
  imageUrl.value = card.value.image;
})



function flipCard() {
  if (cardFlipped.value) {
    imageUrl.value = card.value!.image;
    cardFlipped.value = false;
  } else {
    if (card.value?.backImage) {
      imageUrl.value = card.value.backImage;
      cardFlipped.value = true;
    }
  }
}

</script>

<template>
  <div id="card-details" v-if="card">
    <h1>{{ parts[0].name }}</h1>
    <div class="card-details-container">
      <div class="card-image-container">
        <img :src="imageUrl" :alt="card.name"></img>
        <StyleButton small class="flip-card-button" @click="flipCard" :class="{ flipped: cardFlipped }"
          v-if="card.backImage">
          <span class="material-symbols-rounded">360</span>
          Flip card
        </StyleButton>
      </div>
      <div>
        <div v-for="part in parts" class="card-part-details">
          <div class="card-title-bar card-text">
            <p class="title"><b>{{ part.name }}</b></p>
            <p><abbr v-for="symbol in cardSymbols(part)" :key="symbol"
                :class="`card-symbol card-symbol-${symbol.toUpperCase()}`">{{ symbol }}</abbr></p>
          </div>
          <p class="card-text">{{ part.typeLine }}</p>
          <p class="card-text" v-html="part.html"></p>
          <p class="card-text" v-if="part.flavorText"><i>{{ part.flavorText }}</i></p>
          <p class="card-text card-pt" v-if="part.power || part.toughness">{{ part.power }}/{{ part.toughness }}</p>
          <p class="card-text card-pt" v-if="part.loyalty">{{ part.loyalty }}</p>
          <p class="card-text card-pt" v-if="part.defense">{{ part.defense }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
@import url('@/assets/symbols.css');
</style>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=EB+Garamond:ital,wght@0,400..800;1,400..800&display=swap');

#card-details {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.card-details-container {
  display: flex;
  flex-direction: row;
  margin-top: var(--space-xl);
  flex-wrap: wrap;
  justify-content: center;
}

img {
  width: auto;
  max-height: 523px;

}

.card-image-container {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-lg);
}

.flip-card-button {
  width: max-content;
  display: flex;
  gap: var(--space-lg);
}

.flip-card-button span {
  transform: rotate(180deg);
}

.flip-card-button.flipped span {
  transform: rotate(0deg);
}

.card-part-details {
  margin-bottom: 30px;
  width: 500px;
}

.card-title-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-text {
  font-family: "EB Garamond", serif;
  font-optical-sizing: auto;
  font-weight: var(--font-weight-normal);
  font-style: normal;
  font-size: 14pt;
  border-bottom: 1px solid var(--color-gray-300);
  margin: 0;
  padding: var(--space-md);
}

.card-pt {
  font-weight: 600;
  text-align: right;
}

@media screen and (max-width: 600px) {
  img {
    width: 80%;
    margin: auto;
  }

  .card-part-details {
    width: 100%;
  }

}
</style>