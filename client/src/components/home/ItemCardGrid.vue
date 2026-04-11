<script setup lang="ts">
</script>

<template>
  <div class="item-card-grid">
    <slot />
  </div>
</template>

<style scoped>
.item-card-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  background-color: var(--color-gray-200);
  border: 1px solid var(--color-gray-300);
  border-radius: var(--radius-md);
  max-width: 80%;
  width: fit-content;
  margin: 0 auto;
  padding: 1em 10%;
  min-width: 250px;
  margin-bottom: 200px;
}
</style>
