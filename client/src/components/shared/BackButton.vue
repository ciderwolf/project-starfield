<script setup lang="ts">
import { useRouter } from 'vue-router';
import IconButton from './IconButton.vue';

const router = useRouter();
function exitPage() {
  if (window.history.state?.back) {
    router.back();
  } else {
    router.push({ name: 'home' });
  }
}

</script>

<template>
  <IconButton @click="exitPage" icon="arrow_back" size="xl" />
</template>
