<script setup lang="ts">
import { ref } from 'vue';
import LoadingButton from '@/components/LoadingButton.vue';
import { createAccount, login } from '@/api';
import { ws } from '@/ws';
import { useRouter } from 'vue-router';

const username = ref('');
const password = ref('');
const repeatPassword = ref('');
const errorMessage = ref('');
const router = useRouter();

async function createAccountClicked(e: Event) {
  e.preventDefault();
  if (password.value !== repeatPassword.value) {
    errorMessage.value = 'Passwords do not match';
    return;
  }
  const createAccountResult = await createAccount(username.value, password.value)
  if (createAccountResult.success) {
    ws.reconnect();
    router.push('/');
  } else {
    errorMessage.value = createAccountResult.message;
  }
}


</script>

<template>
  <div class="create-account">
    <form class="create-account-form" @submit="createAccountClicked">
      <h1>Create Account</h1>
      <span v-if="errorMessage" class="error-message">{{ errorMessage }}</span>
      <label for="username">Username</label>
      <input type="text" id="username" v-model="username">
      <label for="password">Password</label>
      <input type="password" id="password" v-model="password">
      <label for="repeat-password">Repeat Password</label>
      <input type="password" id="repeat-password" v-model="repeatPassword">

      <loading-button :on-click="createAccountClicked">Create Account</loading-button>
    </form>
  </div>
</template>

<style scoped>
h1 {
  text-align: center;
}


.create-account-form {
  display: flex;
  flex-direction: column;
  gap: 1em;
  max-width: 300px;
  margin: 0 auto;
}

.error-message {
  color: orangered;
  text-align: center;
}
</style>