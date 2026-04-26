<script setup lang="ts">
import { computed, ref } from 'vue';

const props = withDefaults(defineProps<{
  max?: number;
  readonly?: boolean;
}>(), {
  max: 5,
  readonly: false,
});

const rating = defineModel<number>({ default: 0 });
const hover = ref(0);

// Round to the nearest half-star bucket (e.g. 1.5, 2, 3.5).
function roundToHalf(value: number) {
  return Math.round(value * 2) / 2;
}

const displayValue = computed(() => {
  const raw = hover.value || rating.value || 0;
  return roundToHalf(raw);
});

function valueFor(i: number, e: MouseEvent): number {
  const target = e.currentTarget as HTMLElement;
  const rect = target.getBoundingClientRect();
  const isLeftHalf = e.clientX - rect.left < rect.width / 2;
  return isLeftHalf ? i - 0.5 : i;
}

function setRating(i: number, e: MouseEvent) {
  if (props.readonly) return;
  const value = valueFor(i, e);
  rating.value = rating.value === value ? 0 : value;
}

function onMove(i: number, e: MouseEvent) {
  if (props.readonly) return;
  hover.value = valueFor(i, e);
}

function onLeave() {
  hover.value = 0;
}

function starState(i: number): 'full' | 'half' | 'empty' {
  const v = displayValue.value;
  if (v >= i) return 'full';
  if (v >= i - 0.5) return 'half';
  return 'empty';
}
</script>

<template>
  <div class="star-rating" :class="{ readonly }" @mouseleave="onLeave" role="radiogroup"
    :aria-label="`Rating out of ${max}`">
    <button v-for="i in max" :key="i" type="button" class="star"
      :class="{ filled: starState(i) === 'full', half: starState(i) === 'half' }" :disabled="readonly"
      :aria-label="`${i} star${i === 1 ? '' : 's'}`" :aria-checked="rating === i" role="radio"
      @click="(e) => setRating(i, e)" @mousemove="(e) => onMove(i, e)">
      <span class="material-symbols-rounded">{{ starState(i) === 'half' ? 'star_half' : 'star' }}</span>
    </button>
  </div>
</template>

<style scoped>
.star-rating {
  display: inline-flex;
}

.star {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  color: var(--color-gray-500);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: -2px;
}

.star.filled,
.star.half {
  color: var(--color-accent, gold);
}

.star:disabled {
  cursor: default;
}

.star-rating.readonly .star {
  cursor: default;
}

.star .material-symbols-rounded {
  font-size: 24px;
  font-variation-settings: 'FILL' 0;
}

.star.filled .material-symbols-rounded,
.star.half .material-symbols-rounded {
  font-variation-settings: 'FILL' 1;
}
</style>
