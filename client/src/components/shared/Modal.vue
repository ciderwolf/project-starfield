<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'

const props = defineProps<{ visible: boolean, title?: string, modalContentStyles?: string }>()
const minimized = ref(false);

const emit = defineEmits(['close']);

watch(() => props.visible, (visible) => {
  if (visible && !minimized.value) {
    document.body.style.overflow = 'hidden';
  } else {
    document.body.style.overflow = '';
  }
});

watch(minimized, (isMinimized) => {
  if (isMinimized || !props.visible) {
    document.body.style.overflow = '';
  } else {
    document.body.style.overflow = 'hidden';
  }
});

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
  document.body.style.overflow = '';
})
</script>

<template>
  <div class="modal" v-if="visible" :class="{ minimized }">
    <div class="modal-minimized" @click="maximize" v-if="minimized">
      <h3>{{ title }}</h3>
      <span class="material-symbols-rounded modal-control">add</span>
    </div>
    <div class="modal-background" v-if="!minimized"></div>
    <div class="modal-content" v-if="!minimized" :style="modalContentStyles">
      <div class="modal-controls">
        <span class="material-symbols-rounded modal-control" @click="minimize" v-if="title">minimize</span>
        <span class="material-symbols-rounded modal-control" @click="emit('close')">close</span>
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
  border-radius: 0.5rem 0.5rem 0 0;
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
  margin: 0.5rem;
}

.modal-control {
  font-size: 24px;
  cursor: pointer;
  background: none;
  border: none;
  padding: var(--space-xs);
  border-radius: var(--radius-md);
  color: var(--color-gray-700);
  transition: background-color var(--transition-normal), color var(--transition-normal);
}

.modal-control:hover {
  background-color: var(--color-gray-100);
  color: var(--color-black);
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