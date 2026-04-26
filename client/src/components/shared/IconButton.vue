<script setup lang="ts">
withDefaults(
  defineProps<{ icon: string; size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | 'xxl' }>(),
  {
    size: 'sm',
  },
);
</script>

<template>
  <button class="icon-button material-symbols-rounded" :class="`size-${size}`">
    {{ icon }}
  </button>
</template>

<style scoped>
.icon-button {
  background: none;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  padding: 0.25em;
}

.icon-button:hover {
  background: var(--color-gray-300);
}

.icon-button.size-xs {
  font-size: var(--font-size-xs);
}

.icon-button.size-sm {
  font-size: var(--font-size-sm);
}

.icon-button.size-md {
  font-size: var(--font-size-md);
}

.icon-button.size-lg {
  font-size: var(--font-size-lg);
}

.icon-button.size-xl {
  font-size: var(--font-size-xl);
}

.icon-button.size-xxl {
  font-size: var(--font-size-xxl);
}
</style>