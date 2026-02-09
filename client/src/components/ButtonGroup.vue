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
  border: 1px solid rgb(78, 128, 220);
  border-radius: 3px;
  overflow: hidden;
}

.button-group-item {
  background-color: #fff;
  border: none;
  border-right: 1px solid rgb(78, 128, 220);
  color: black;
  cursor: pointer;
  font-size: 1.25em;
  padding: 0.5em 1em;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.1s ease;
}

.button-group-item:last-child {
  border-right: none;
}

.button-group-item:hover {
  background-color: #eee;
}

.button-group-item:active {
  background-color: #ddd;
}

.button-group-item.active {
  background-color: rgb(78, 128, 220);
  color: #fff;
}

.button-group-item.active:hover {
  background-color: #3f65b0;
}

.button-group-item.active:active {
  background-color: #375595;
}

.button-group.small .button-group-item {
  font-size: 1em;
  padding: 0.25em 0.5em;
}
</style>
