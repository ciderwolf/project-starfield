<script setup lang="ts">
import { RouterView, useRouter } from 'vue-router'
import { ws } from './ws'
import { authenticate } from './api';
import { ref } from 'vue';
import LoadingSpinner from './components/LoadingSpinner.vue';
import AlertsContainer from './components/AlertsContainer.vue';
console.log(ws);

const router = useRouter();
const loaded = ref(false)
authenticate().then((isLoggedIn) => {
  if (!isLoggedIn) {
    router.push('/login');
  }
  loaded.value = true;
});

</script>

<template>
  <RouterView v-if="loaded" />
  <div v-else class="loading-view"><loading-spinner /> Loading...</div>
  <AlertsContainer />
</template>

<style scoped>
.loading-view {
  margin: 0 auto;
  margin-top: 25%;
  display: flex;
  align-items: center;
  gap: 10px;
  width: fit-content;
  font-size: 2.5em;
  font-weight: 300;
}
</style>

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

a {
  text-decoration: none;
}
</style>