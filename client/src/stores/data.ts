import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const useDataStore = defineStore('data', () => {

  const userId = ref<string | null>(null);
  const userName = ref<string | null>(null);
  const loggedIn = computed(() => userId !== null);

  function login(name: string, id: string) {
    userId.value = id;
    userName.value = name;
  }

  return { userId, userName, loggedIn, login }
})

function getRoute(name: string) {
  if (name === "HOME") {
    return "";
  }
  else {
    return name.toLowerCase();
  }
}