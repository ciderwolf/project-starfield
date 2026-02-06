<script setup lang="ts">
import { logout } from '@/api';
import Blade from '@/components/Blade.vue';
import { CLIENT_VERSION } from '@/version';
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const visible = ref(false);
const router = useRouter();

defineExpose({
  showMenu
});

function showMenu() {
  visible.value = true;
}

async function logoutPressed() {
  await logout();
  router.push('/login');
}
</script>

<template>
  <Blade :visible="visible" side="left" @close="visible = false">
    <div class="menu-blade-content">
      <h2>Menu</h2>
      <ul>
        <li>
          <router-link to="/"><i class="material-symbols-rounded">home</i>Home</router-link>
        </li>
        <li>
          <router-link to="/cubes"><i class="material-symbols-rounded">deployed_code</i>Cubes</router-link>
        </li>
        <li>
          <router-link to="/sets"><i class="material-symbols-rounded">category</i>Sets</router-link>
        </li>
      </ul>
      <button class="logout-btn" @click="logoutPressed"><i class="material-symbols-rounded">logout</i> Logout</button>
      <p class="version-info" v-if="CLIENT_VERSION">Client version {{ CLIENT_VERSION }}</p>
    </div>
  </Blade>
</template>

<style scoped>
.menu-blade-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.menu-blade-content h2 {
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.menu-blade-content ul {
  list-style: none;
  padding: 0;
  margin: 0;
  flex: 1;
}

.menu-blade-content li {
  margin: 4px 0;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.menu-blade-content li a {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  text-decoration: none;
  color: black;
  font-weight: 500;
}

.menu-blade-content li:hover {
  background-color: #f5f5f5;
}

.menu-blade-content li:has(a.router-link-exact-active) {
  background-color: hsl(219, 87%, 94%);
}

.menu-blade-content li a.router-link-exact-active {
  color: rgb(78, 128, 220);
}

.menu-blade-content li a.router-link-exact-active i {
  color: hsl(219, 67%, 58%);
}

.menu-blade-content li i {
  font-size: 20px;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-top: auto;
  background: none;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  transition: background-color 0.2s, color 0.2s;
}

.logout-btn:hover {
  background-color: #fee;
  border-color: #f99;
  color: #c33;
}

.logout-btn i {
  font-size: 20px;
}

.version-info {
  margin: 0;
  padding-top: 1em;
  font-size: 0.8em;
  text-align: center;
  color: #666;
}
</style>