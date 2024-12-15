<script setup lang="ts">
import type { DraftCard } from '@/api/message';


const props = defineProps<{
  card: DraftCard;
}>()

const emit = defineEmits<{
  pick: []
}>();

</script>

<template>
  <div class="card" :style="{ backgroundImage: `url(${card.image})` }" @dblclick="emit('pick')">
    <div v-if="card.foil" class="foil-card"></div>
  </div>

</template>


<style scoped>
.card {
  background-size: cover;
  background-position: center;
  height: 350px;
  width: 250px;
  border-radius: 10px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  margin: 10px;
  cursor: default;
}

.foil-card {
  height: 100%;
  width: 100%;
  border-radius: inherit;

  background: linear-gradient(45deg, #f003, #ff03, #0f03, #0ff3, #00f3, #f0f3, #f003);

  clip-path: polygon(0 0, 100% 0, 100% 100%, 0 100%);
}
</style>