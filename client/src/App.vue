<script setup lang="ts">
import { RouterView } from 'vue-router'
import { ws } from './ws'
console.log(ws);
</script>

<template>
  <RouterView />
</template>

<style>
body {
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
  margin: 0;
}

input,
select {
  border: 1px solid rgb(123, 123, 123);
  background: white;
  outline: none;
  border-radius: 3px;
  font-size: 1em;
  padding: 0.25em 0.5em;
}

input:focus,
select:focus {
  border-color: rgb(78, 128, 220);
}
</style>