<script setup lang="ts">
import type { BoardCard } from '@/api/message';
import ContextMenu from '@/components/shared/ContextMenu.vue';
import type { ActionEmit, ContextMenuDefinition } from '@/context-menu';
import { reactive, ref } from 'vue';

const props = defineProps<{
  card: BoardCard;
  contextMenu: ContextMenuDefinition;
  debounceClick?: boolean
}>();

defineExpose({
  showContextMenu,
  closeContextMenu
});

const showMenu = ref(false);
const menuPos = reactive({ x: 0, y: 0 });
let showMenuTimer: number;

function showContextMenu(e: MouseEvent) {
  if (e.detail > 1 || showMenu.value) {
    return;
  }

  menuPos.x = e.clientX + 5;
  menuPos.y = e.clientY + 5;

  if (props.debounceClick && e.button == 0) {
    showMenuTimer = setTimeout(() => {
      showMenu.value = true;
    }, 200);
  } else {
    showMenuTimer = setTimeout(() => {
      showMenu.value = true;
    }, 200);
  }
}

function closeContextMenu() {
  window.clearTimeout(showMenuTimer);
  showMenu.value = false;
}

const clickOutsideMenu = {
  handler: () => { closeContextMenu(); },
  events: ['click', 'dblclick', 'contextmenu'],
  detectIFrame: false,
}

</script>

<template>
  <ContextMenu v-if="showMenu" v-click-outside="clickOutsideMenu" :real-pos="menuPos" :menu="props.contextMenu"
    @select="closeContextMenu" />
</template>