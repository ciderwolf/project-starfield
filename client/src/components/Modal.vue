<script setup lang="ts">

const props = defineProps<{ visible: boolean }>()

const emit = defineEmits(['close']);
</script>

<template>
  <div class="modal" v-if="visible">
    <div class="modal-background"></div>
    <span class="close-button" @click="emit('close')">&times;</span>
    <div class="modal-content">
      <slot></slot>
    </div>
  </div>
</template>

<style scoped>
.modal-background {
  background-color: rgba(0, 0, 0, 0.5);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.close-button {
  position: absolute;
  top: 1rem;
  right: 1rem;
  font-size: 2rem;
  cursor: pointer;
}

.modal-content {
  background-color: white;
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  padding: 2rem;
  border-radius: 0.5rem;
  box-shadow: 0 0 1rem rgba(0, 0, 0, 0.5);
}
</style>