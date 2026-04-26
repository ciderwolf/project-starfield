<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  visible: boolean
  side?: 'left' | 'right'
  width?: string
}>(), {
  side: 'right',
  width: '400px'
})

const emit = defineEmits<{
  (e: 'close'): void
}>()

const close = () => {
  emit('close')
}

const bladeStyle = computed(() => ({
  width: props.width,
  [props.side]: 0
}))
</script>

<template>
  <Teleport to="body">
    <Transition :name="`slide-${side}`">
      <div v-if="visible" class="blade-overlay">
        <div class="blade" :class="[`blade-${side}`]" :style="bladeStyle" v-click-outside="close">
          <button class="blade-close" @click="close" aria-label="Close">
            <span class="material-symbols-rounded">close</span>
          </button>
          <div class="blade-content">
            <slot />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.blade-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: var(--z-modal);
  pointer-events: none;
}

.blade {
  position: absolute;
  top: 0;
  height: 100%;
  background-color: var(--color-white);
  box-shadow: var(--shadow-xl);
  display: flex;
  flex-direction: column;
  pointer-events: all;
}

.blade-left {
  border-right: 1px solid var(--color-gray-400);
}

.blade-right {
  border-left: 1px solid var(--color-gray-400);
}

.blade-close {
  position: absolute;
  top: 12px;
  background: none;
  border: none;
  cursor: pointer;
  padding: var(--space-xs);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  color: var(--color-gray-700);
  transition: background-color var(--transition-normal), color var(--transition-normal);
}

.blade-close:hover {
  background-color: var(--color-gray-150);
  color: var(--color-gray-900);
}

.blade-left .blade-close {
  right: 12px;
}

.blade-right .blade-close {
  left: 12px;
}

.blade-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  padding-top: 48px;
}

/* Slide from right transitions */
.slide-right-enter-active,
.slide-right-leave-active {
  transition: transform var(--transition-slow);
}

.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(100%);
}

/* Slide from left transitions */
.slide-left-enter-active,
.slide-left-leave-active {
  transition: transform var(--transition-slow);
}

.slide-left-enter-from,
.slide-left-leave-to {
  transform: translateX(-100%);
}
</style>
