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
  font-size: var(--font-size-sm);
  background: none;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm);
}

.icon-button:hover {
  background: var(--color-gray-300);
}
</style>