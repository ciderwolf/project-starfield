<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref } from 'vue';

const props = withDefaults(defineProps<{
  text?: string;
  openDelay?: number;
  closeDelay?: number;
  maxWidth?: number;
}>(), {
  text: '',
  openDelay: 150,
  closeDelay: 120,
  maxWidth: 300,
});

let uid = 0;
const popoverId = `help-popover-${++uid}`;

const isOpen = ref(false);
const triggerRef = ref<HTMLElement | null>(null);
const popoverRef = ref<HTMLElement | null>(null);

const position = ref({ top: 0, left: 0, placement: 'bottom' as 'top' | 'bottom' });

let openTimer: number | undefined;
let closeTimer: number | undefined;

function clearTimers() {
  if (openTimer) {
    window.clearTimeout(openTimer);
    openTimer = undefined;
  }
  if (closeTimer) {
    window.clearTimeout(closeTimer);
    closeTimer = undefined;
  }
}

async function updatePosition() {
  await nextTick();
  if (!triggerRef.value || !popoverRef.value) return;

  const triggerRect = triggerRef.value.getBoundingClientRect();
  const popRect = popoverRef.value.getBoundingClientRect();

  const gap = 8;
  const margin = 8;

  const spaceBelow = window.innerHeight - triggerRect.bottom;
  const placement: 'top' | 'bottom' = spaceBelow < popRect.height + gap + margin && triggerRect.top > popRect.height + gap + margin
    ? 'top'
    : 'bottom';

  const top = placement === 'bottom'
    ? triggerRect.bottom + gap
    : triggerRect.top - popRect.height - gap;

  let left = triggerRect.left + triggerRect.width / 2 - popRect.width / 2;
  left = Math.max(margin, Math.min(left, window.innerWidth - popRect.width - margin));

  position.value = { top, left, placement };
}

function open() {
  clearTimers();
  if (isOpen.value) return;
  openTimer = window.setTimeout(async () => {
    isOpen.value = true;
    await updatePosition();
  }, props.openDelay);
}

function close() {
  clearTimers();
  if (!isOpen.value) return;
  closeTimer = window.setTimeout(() => {
    isOpen.value = false;
  }, props.closeDelay);
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && isOpen.value) {
    clearTimers();
    isOpen.value = false;
    triggerRef.value?.blur();
  }
}

function onScrollOrResize() {
  if (isOpen.value) updatePosition();
}

window.addEventListener('scroll', onScrollOrResize, true);
window.addEventListener('resize', onScrollOrResize);

onBeforeUnmount(() => {
  clearTimers();
  window.removeEventListener('scroll', onScrollOrResize, true);
  window.removeEventListener('resize', onScrollOrResize);
});

const popoverStyle = computed(() => ({
  top: `${position.value.top}px`,
  left: `${position.value.left}px`,
  maxWidth: `${props.maxWidth}px`,
}));
</script>

<template>
  <button ref="triggerRef" type="button" class="help-button" :aria-describedby="isOpen ? popoverId : undefined"
    :aria-expanded="isOpen" aria-label="Help" @mouseenter="open" @mouseleave="close" @focus="open" @blur="close"
    @keydown="onKeydown">
    <span class="material-symbols-rounded">help</span>
  </button>

  <Teleport to="body">
    <Transition name="help-fade">
      <div v-if="isOpen" :id="popoverId" ref="popoverRef" role="tooltip" class="help-popover"
        :class="`placement-${position.placement}`" :style="popoverStyle" @mouseenter="open" @mouseleave="close">
        <slot>{{ text }}</slot>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.help-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  padding: 0;
  margin-left: var(--space-xxs);
  cursor: pointer;
  color: var(--color-gray-500);
  border-radius: var(--radius-round, 50%);
  line-height: 1;
  transition: color 0.1s ease;
}

.help-button:hover,
.help-button:focus-visible {
  color: var(--color-primary, var(--color-gray-700));
  outline: none;
}

.help-button .material-symbols-rounded {
  font-size: 1.25em;
}

.help-popover {
  position: fixed;
  background: var(--color-white);
  color: var(--color-gray-800, inherit);
  border: 1px solid var(--color-gray-300);
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-md);
  font-size: 14px;
  line-height: 1.4;
  box-shadow: var(--shadow-dropdown, 0 4px 12px rgba(0, 0, 0, 0.12));
  z-index: var(--z-tooltip, 2000);
}

.help-fade-enter-active,
.help-fade-leave-active {
  transition: opacity 0.12s ease, transform 0.12s ease;
}

.help-fade-enter-from,
.help-fade-leave-to {
  opacity: 0;
}

.help-fade-enter-from.placement-bottom,
.help-fade-leave-to.placement-bottom {
  transform: translateY(-4px);
}

.help-fade-enter-from.placement-top,
.help-fade-leave-to.placement-top {
  transform: translateY(4px);
}
</style>
