<script setup lang="ts">

withDefaults(defineProps<{ type?: string, disabled?: boolean, small?: boolean }>(), {
  type: 'default',
  disabled: false,
  small: false,
});

</script>

<template>
  <button class="button" :class="type + (small ? ' small' : '')" :disabled="disabled" draggable="false">
    <slot></slot>
  </button>
</template>

<style scoped>
.button {
  background-color: var(--color-white);
  border: 1px solid var(--color-gray-600);
  border-radius: var(--radius-sm);
  color: var(--color-gray-700);
  cursor: pointer;
  font-size: var(--font-size-md);
  padding: 0.5em 1em;
  display: flex;
  align-items: center;
  justify-content: center;
}

.button.small {
  font-size: var(--font-size-sm);
  padding: 0.25em 0.5em;
}

.button:hover {
  background-color: var(--color-gray-200);
}

.button:active {
  background-color: var(--color-gray-300);
}

.button:disabled {
  opacity: 0.8;
  cursor: not-allowed;
}

.button.default {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-white);
}

.button.default:hover {
  background-color: var(--color-primary-hover);
}

.button.default:active {
  background-color: var(--color-primary-active);
}

.button.danger {
  background-color: var(--color-danger);
  border-color: var(--color-danger);
  color: var(--color-white);
}

.button.danger:hover {
  background-color: var(--color-danger-hover);
}

.button.danger:active {
  background-color: var(--color-danger-active);
}
</style>