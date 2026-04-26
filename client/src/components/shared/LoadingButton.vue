<script setup lang="ts">
import StyleButton from './StyleButton.vue';
import LoadingSpinner from './LoadingSpinner.vue';
import { ref } from 'vue';

const props = defineProps<{ onClick: (e: MouseEvent) => Promise<void> }>();

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
  <style-button @click="buttonClicked" :disabled="state === 'loading'">
    <loading-spinner v-if="state === 'loading'" color="white" />
    <slot></slot>
  </style-button>
</template>