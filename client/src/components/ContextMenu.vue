<script setup lang="ts">
import type { ContextMenuDefinition, ContextMenuOption } from '@/context-menu';
import { computed, reactive, ref, onMounted } from 'vue';

interface Position {
  x: number;
  y: number;
}

const props = defineProps<{ realPos?: Position, relativePos?: Position, parentPos?: Position, menu: ContextMenuDefinition, zIndex?: number }>();
const emit = defineEmits<{
  (event: 'select', option: string, argument: number): void
}>();

const submenuVisibilities = reactive<{ [index: number]: { x: number, y: number, persistant: boolean } }>({});

const displayX = computed(() => {
  const screenX = props.realPos?.x ?? (props.relativePos!.x + props.parentPos!.x);
  const width = (menu.value?.offsetWidth ?? 0);
  const clampedX = clampToWindow(screenX, width, window.innerWidth + window.scrollX - 20);
  if (props.realPos !== undefined) {
    return clampedX;
  } else {

    if (screenX + width > window.innerWidth + window.scrollX - 20) {
      return - width;
    }

    return clampedX - props.parentPos!.x;
  }
});

const displayY = computed(() => {
  const screenY = props.realPos?.y ?? (props.relativePos!.y + props.parentPos!.y);
  const clampedY = clampToWindow(screenY, menu.value?.offsetHeight ?? 0, window.innerHeight + window.scrollY - 20);
  if (props.realPos !== undefined) {
    return clampedY;
  } else {
    return clampedY - props.parentPos!.y;
  }
});

function clampToWindow(elementPos: number, elementSize: number, windowSize: number) {
  if (elementPos + elementSize > windowSize) {
    return windowSize - elementSize;
  } else {
    return elementPos;
  }
}

const pos = computed(() => {
  const style: any = {
    left: `${displayX.value}px`,
    top: `${displayY.value}px`,
  }
  if (props.zIndex !== undefined) {
    style['z-index'] = props.zIndex;
  }
  return style;
});


function showOption(index: number) {
  return submenuVisibilities[index] !== undefined;
}

function yesShow(index: number, persistant = false) {
  if (!optionElements.value[index]) {
    return;
  }
  const bounds = optionElements.value[index].getBoundingClientRect();
  submenuVisibilities[index] = {
    x: bounds.width,
    y: bounds.top,
    persistant
  }
}

function noShow(index: number) {
  if (submenuVisibilities[index] && submenuVisibilities[index].persistant === false) {
    delete submenuVisibilities[index];
  }
}

const optionElements = ref<HTMLElement[]>([]);
const menu = ref<HTMLElement | null>(null);
const numberInputs = ref<number[]>([]);

onMounted(() => {
  for (let i = 0; i < props.menu.options.length; i++) {
    const option = props.menu.options[i]
    if (option.type === 'number') {
      numberInputs.value[i] = option.default ?? 1;
    }
  }
});

function cancelSubClick(e: MouseEvent) {
  e.stopPropagation();
  e.preventDefault();
}

function optionClicked(option: ContextMenuOption) {
  if (option.type == 'text' && !option.disabled) {
    option.effect();
  }
}
</script>

<template>
  <div class="card-context-menu" :style="pos" ref="menu">
    <div class="card-context-menu-options">
      <div v-for="option, index in props.menu.options" ref="optionElements">
        <template v-if="option.type == 'text'">
          <div class="card-context-menu-option" @click="optionClicked(option)">
            <div class="card-context-menu-text" :class="{ ['option-disabled']: option.disabled }">{{ option.title }}
            </div>
          </div>
        </template>
        <template v-if="option.type == 'number'">
          <div class="card-context-menu-option card-context-menu-number-option"
            @click="option.effect(numberInputs[index])">
            <div class="card-context-menu-text">{{ option.title }}</div>
            <input class="card-context-menu-number-input" type="number" :min="option.min" :max="option.max"
              v-model.number="numberInputs[index]" :placeholder="`${option.default}`" @click="cancelSubClick" />
          </div>
        </template>
        <template v-if="option.type == 'seperator'">
          <div class="card-context-menu-seperator"></div>
        </template>
        <template v-if="option.type == 'submenu'">
          <div class="card-context-menu-submenu" @mouseenter="yesShow(index)" @mouseleave="noShow(index)"
            @click="yesShow(index, true)">
            <div class="card-context-menu-submenu-title">
              {{ option.title }} ›
            </div>
            <ContextMenu :menu="option" :relative-pos="{ x: submenuVisibilities[index].x, y: 0 }"
              :parent-pos="{ x: displayX, y: submenuVisibilities[index].y }" v-if="showOption(index)">
            </ContextMenu>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card-context-menu {
  position: absolute;
  background-color: #fff;
  border-radius: 3px;
  box-shadow: 0 0 5px 0 rgba(0, 0, 0, 0.5);
  padding: 5px;
  z-index: 100;
  box-sizing: content-box;
  min-width: max-content;
}

.card-context-menu-title {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 5px;
}

.card-context-menu-options {
  display: flex;
  flex-direction: column;
}

.card-context-menu-option {
  display: flex;
  flex-direction: column;
  cursor: pointer;
}

.card-context-menu-submenu:hover,
.card-context-menu-option:hover {
  background: #eee;
}

.card-context-menu-seperator {
  height: 1px;
  background-color: #ccc;
  margin: 5px 0;
}

.card-context-menu-submenu {
  display: flex;
  flex-direction: column;
  margin-bottom: 5px;
  position: relative;
}

.card-context-menu-submenu-title {
  font-size: 12px;
  font-weight: bold;
  margin-bottom: 5px;
}

.card-context-menu-submenu-seperator {
  height: 1px;
  background-color: #ccc;
}

.card-context-menu-submenu-text {
  font-size: 12px;
  margin: 5px 0;
}

.card-context-menu-text {
  font-size: 12px;
  margin: 5px 0;
}

.card-context-menu-text.option-disabled {
  color: gray;
  cursor: not-allowed;
}

.card-context-menu-number-option {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.card-context-menu-number-input {
  font-size: 12px;
  margin: 5px 0;
  width: 50px;
  text-align: right;
}
</style>