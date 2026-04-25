<script setup lang="ts">
const colors = ['W', 'U', 'B', 'R', 'G'];
const colorNames = {
  W: 'White',
  U: 'Blue',
  B: 'Black',
  R: 'Red',
  G: 'Green',
} as Record<string, string>;
const model = defineModel<string[]>({ required: true });

function toggle(color: string, checked: boolean) {
  const set = new Set(model.value);
  checked ? set.add(color) : set.delete(color);
  model.value = colors.filter(c => set.has(c));
}
</script>

<template>
  <div class="color-combination-chooser">
    <div v-for="color in colors" :key="color" class="color-option">
      <label>
        <input type="checkbox" :checked="model.includes(color)"
          @change="toggle(color, ($event.target as HTMLInputElement).checked)" />
        <abbr :class="`card-symbol card-symbol-${color}`">{{ color }}</abbr>
        <span>{{ colorNames[color] }}</span>
      </label>
    </div>
  </div>
</template>


<style>
@import url('@/assets/symbols.css');
</style>

<style scoped>
.color-combination-chooser {
  display: flex;
  flex-direction: row;
  gap: var(--space-sm);
  justify-content: space-evenly;
  width: 100%;
}
</style>
