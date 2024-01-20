<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(defineProps<{ type?: string, disabled?: boolean }>(), {
  type: 'default',
  disabled: false
});

</script>

<template>
  <button class="button" draggable="false" :class="type">
    <slot></slot>
  </button>
</template>

<style scoped>
.button {
  background-color: #fff;
  border: 1px solid #999;
  border-radius: 3px;
  color: #666;
  cursor: pointer;
  font-size: 1.25em;
  margin: 0 0.5em;
  padding: 0.5em 1em;
}

.button:hover {
  background-color: #eee;
}

.button:active {
  background-color: #ddd;
}

.button.default {
  background-color: rgb(78, 128, 220);
  border-color: rgb(78, 128, 220);
  color: #fff;
}

.button.default:hover {
  background-color: #3f65b0;
}

.button.default:active {
  background-color: #375595;
}

.button.danger {
  background-color: #f00;
  border-color: #f00;
  color: #fff;
}

.button.danger:hover {
  background-color: #f55;
}

.button.danger:active {
  background-color: #f77;
}
</style>