<script setup lang="ts">
import { RouterView } from 'vue-router'
import { WebSocketConnection } from '@/api/websocket'
import { WebSocketGameClient } from './api/game-client';

const ws = new WebSocketConnection();
const client = new WebSocketGameClient(ws);
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