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
  gap: var(--space-md);
  width: fit-content;
  font-size: var(--font-size-xxl);
  font-weight: var(--font-weight-light);
}
</style>

<style>
body {
  font-family: var(--font-family);
  margin: 0;
}

input,
select {
  border: 1px solid var(--color-border-input);
  background: var(--color-white);
  outline: none;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  padding: 0.25em 0.5em;
}

input:focus,
select:focus {
  border-color: var(--color-primary);
}

a {
  text-decoration: none;
}
</style>