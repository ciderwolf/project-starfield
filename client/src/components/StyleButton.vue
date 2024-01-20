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
  background-color: #fff;
  border: 1px solid #999;
  border-radius: 3px;
  color: #666;
  cursor: pointer;
  font-size: 1.25em;
  padding: 0.5em 1em;
}

.button.small {
  font-size: 1em;
  padding: 0.25em 0.5em;
}

.button:hover {
  background-color: #eee;
}

.button:active {
  background-color: #ddd;
}

.button:disabled {
  opacity: 0.8;
  cursor: not-allowed;
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