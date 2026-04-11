<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

const props = defineProps<{ visible: boolean, title?: string, modalContentStyles?: string }>()
const minimized = ref(false);

const emit = defineEmits(['close']);

function minimize() {
  minimized.value = true;
}

function maximize() {
  minimized.value = false;
}

function keyPressed(event: KeyboardEvent) {
  if (event.key === 'Escape' && props.visible) {
    maximize();
    emit('close')
  }
}

onMounted(() => {
  window.addEventListener('keydown', keyPressed);
})

onUnmounted(() => {
  window.removeEventListener('keydown', keyPressed);
})
</script>

<template>
  <div class="modal" v-if="visible" :class="{ minimized }">
    <div class="modal-minimized" v-if="minimized">
      <h3>{{ title ?? 'Minimized Window' }}</h3>
      <span class="modal-control" @click="maximize">&plus;</span>
    </div>
    <div class="modal-background" v-if="!minimized"></div>
    <div class="modal-content" v-if="!minimized" :style="modalContentStyles">
      <div class="modal-controls">
        <span class="modal-control" @click="minimize">&ndash;</span>
        <span class="modal-control" @click="emit('close')">&times;</span>
      </div>
      <slot></slot>
    </div>
  </div>
</template>

<style scoped>
.modal {
  z-index: var(--z-modal);
  position: absolute;
}

.modal-background {
  background-color: var(--overlay-heavy);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.modal.minimized {
  position: fixed;
  bottom: 0;
  right: 0;
  width: max-content;
  border-radius: 20px 20px 0 0;
  box-shadow: var(--shadow-lg);
  cursor: pointer;
  background: var(--color-white);
}

.modal.minimized .modal-minimized {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-xxl);
  padding: 0 var(--space-xl);
}

.modal-controls {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 1rem;
  position: absolute;
  top: 0;
  right: 0;
}

.modal-control {
  padding: 0 0.5rem;
  top: 1rem;
  right: 1rem;
  font-size: 2rem;
  cursor: pointer;
}

.modal-control:hover {
  background-color: var(--color-gray-400);
}

.modal-content {
  background-color: var(--color-white);
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  padding: 2rem;
  border-radius: 0.5rem;
  box-shadow: var(--shadow-lg);
  max-height: 80%;
  overflow-y: scroll;
  width: max-content;
  max-width: 90%;
  min-width: 50%;
  box-sizing: border-box;
}
</style>