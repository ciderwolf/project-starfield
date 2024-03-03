<script setup lang="ts">
import { ref } from 'vue';
import LoadingButton from '@/components/LoadingButton.vue';
import { login } from '@/api';
import { ws } from '@/ws';
import { useRouter } from 'vue-router';

const username = ref('');
const password = ref('');
const errorMessage = ref('');
const router = useRouter();

async function loginClicked(e: Event) {
  e.preventDefault();
  const loginResult = await login(username.value, password.value)
  if (loginResult.success) {
    ws.reconnect();
    router.push('/');
  } else {
    errorMessage.value = loginResult.message;
  }
}

</script>

<template>
  <div id="login">
    <form class="login-form" @submit="loginClicked">
      <h1>Login</h1>
      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
      <label for="username">Username</label>
      <input type="text" id="username" v-model="username">
      <label for="password">Password</label>
      <input type="password" id="password" v-model="password">
      <loading-button :on-click="loginClicked">Log In</loading-button>
      <span class="create-account-text">or, <router-link to="/create-account">create an account</router-link>.</span>
    </form>
  </div>
</template>

<style scoped>
.login-form {
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

.create-account-text {
  text-align: center;
  color: #666;
}

.create-account-text a {
  color: #666;
  text-decoration: underline;
}

.create-account-text a:hover {
  color: #333;

}

h1 {
  text-align: center;
}
</style>