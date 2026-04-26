<script setup lang="ts">
withDefaults(defineProps<{ color?: string }>(), { color: "#333" });
</script>

<template>
  <span class="button-spinner">
    <svg viewBox="0 0 50 50" class="button-spinner-inner">
      <circle cx="25" cy="25" r="20" fill="none" :stroke="color" stroke-width="5" class="path"></circle>
    </svg>
  </span>
</template>

<style>
.button-spinner {
  height: 1em;
  width: 1em;
  margin: -3px 8px -3px 0;

  display: inline-block;
  vertical-align: middle;
}

.button-spinner-inner {
  animation: rotate 2s linear infinite;
}

.button-spinner-inner .path {
  stroke-linecap: round;
  animation: dash 1.5s ease-in-out infinite;
}

@keyframes rotate {
  100% {
    transform: rotate(360deg);
  }
}

@keyframes dash {
  0% {
    stroke-dasharray: 1, 150;
    stroke-dashoffset: 0;
  }

  50% {
    stroke-dasharray: 90, 150;
    stroke-dashoffset: -35;
  }

  100% {
    stroke-dasharray: 90, 150;
    stroke-dashoffset: -124;
  }
}
</style>