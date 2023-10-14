<script setup lang="ts">
import BoardCard from '@/components/game/BoardCard.vue';

const card = {
  name: 'Lightning Bolt',
  image: 'f29ba16f-c8fb-42fe-aabf-87089cb214a7',
  id: 'f29ba16f-c8fb-42fe-aabf-87089cb214a7'
}


</script>

<template>
  <div class="battlefield-zone">
    <board-card :card="card" @move="console.log"></board-card>
    <board-card :card="card"></board-card>
  </div>
</template>

<style scoped>
.battlefield-zone {
  position: absolute;
  width: 90%;
  height: 90%;
  /* background-color: pink; */
  /* z-index: -101; */
}
</style>
