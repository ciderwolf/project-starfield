<script setup lang="ts">
defineProps<{ icon: string }>();
</script>

<template>
  <button class="icon-button material-symbols-rounded">
    {{ icon }}
  </button>
</template>

<style scoped>
.icon-button {
  font-size: 1em;
  background: none;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px;
}

.icon-button:hover {
  background: #ddd;
}
</style>