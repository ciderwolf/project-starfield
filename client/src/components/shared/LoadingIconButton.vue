<script setup lang="ts">
import StyleButton from './StyleButton.vue';
import LoadingSpinner from './LoadingSpinner.vue';
import { ref } from 'vue';
import IconButton from './IconButton.vue';

const props = defineProps<{ onClick: (e: MouseEvent) => Promise<void>, icon: string, size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | 'xxl' }>();

const state = ref('idle');

function buttonClicked(e: MouseEvent) {
  state.value = 'loading';
  props.onClick(e).then(() => {
    state.value = 'idle';
  }).catch(() => {
    state.value = 'error';
  });
}

</script>

<template>
  <div class="loading-icon-button">
    <loading-spinner v-if="state === 'loading'" color="black" />
    <icon-button v-else @click="buttonClicked" :icon="icon" :size="size" />
  </div>
</template>

<style scoped>
.loading-icon-button {
  height: 34px;
}
</style>