<script setup lang="ts">
import { RouterView } from 'vue-router'
import { WebSocketConnection } from '@/api/websocket'

// const ws = new WebSocketConnection();
</script>

<template>
  <RouterView />
</template>

<style>
body {
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  margin: 0;
}
</style>