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
  padding-bottom: var(--space-lg);
  border-bottom: 1px solid var(--color-gray-200);
}

.menu-blade-content ul {
  list-style: none;
  padding: 0;
  margin: 0;
  flex: 1;
}

.menu-blade-content li {
  margin: var(--space-xs) 0;
  border-radius: var(--radius-lg);
  transition: background-color var(--transition-normal);
}

.menu-blade-content li a {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  padding: var(--space-lg) 16px;
  text-decoration: none;
  color: var(--color-text);
  font-weight: var(--font-weight-medium);
}

.menu-blade-content li:hover {
  background-color: var(--color-gray-100);
}

.menu-blade-content li:has(a.router-link-exact-active) {
  background-color: var(--color-primary-light);
}

.menu-blade-content li a.router-link-exact-active {
  color: var(--color-primary);
}

.menu-blade-content li a.router-link-exact-active i {
  color: var(--color-primary-text);
}

.menu-blade-content li i {
  font-size: var(--font-size-fixed-lg);
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  padding: var(--space-lg) 16px;
  margin-top: auto;
  background: none;
  border: 1px solid var(--color-gray-300);
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-size: var(--font-size-fixed-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-gray-700);
  transition: background-color var(--transition-normal), color var(--transition-normal);
}

.logout-btn:hover {
  background-color: #fee;
  border-color: #f99;
  color: #c33;
}

.logout-btn i {
  font-size: var(--font-size-fixed-lg);
}

.version-info {
  margin: 0;
  padding-top: 1em;
  font-size: var(--font-size-xs);
  text-align: center;
  color: var(--color-gray-700);
}
</style>