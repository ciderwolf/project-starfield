<script setup lang="ts">
import { useSlots } from 'vue';

const props = withDefaults(defineProps<{
  modelValue: string;
  small?: boolean;
}>(), {
  small: false,
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

const slots = useSlots();

function select(value: string) {
  emit('update:modelValue', value);
}
</script>

<template>
  <div class="button-group" :class="{ small }">
    <button v-for="(_, name) in slots" :key="name" class="button-group-item" :class="{ active: modelValue === name }"
      @click="select(String(name))">
      <slot :name="name" :active="modelValue === name"></slot>
    </button>
  </div>
</template>

<style scoped>
.button-group {
  display: inline-flex;
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.button-group-item {
  background-color: var(--color-white);
  border: none;
  border-right: 1px solid var(--color-primary);
  color: var(--color-text);
  cursor: pointer;
  font-size: var(--font-size-md);
  padding: 0.5em 1em;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color var(--transition-fast);
}

.button-group-item:last-child {
  border-right: none;
}

.button-group-item:hover {
  background-color: var(--color-gray-200);
}

.button-group-item:active {
  background-color: var(--color-gray-300);
}

.button-group-item.active {
  background-color: var(--color-primary);
  color: var(--color-white);
}

.button-group-item.active:hover {
  background-color: var(--color-primary-hover);
}

.button-group-item.active:active {
  background-color: var(--color-primary-active);
}

.button-group.small .button-group-item {
  font-size: var(--font-size-sm);
  padding: 0.25em 0.5em;
}
</style>
