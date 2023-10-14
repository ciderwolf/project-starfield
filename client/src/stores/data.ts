import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { useRouter } from 'vue-router'

export const useDataStore = defineStore('data', () => {
  
  const router = useRouter();
  function goTo(a: string, b: string | null) {
    let route = "/" + getRoute(a);
    if (b !== null) {
      route += "/" + b;
    }

    router.push(route);

  }

  const userId = ref<string | null>(null);
  const userName = ref<string | null>(null);

  function login(name: string, id: string) {
    userId.value = id;
    userName.value = name;
  }

  return { userId, userName, login, goTo }
})

function getRoute(name: string) {
  if (name === "HOME") {
    return "";
  }
  else {
    return name.toLowerCase();
  }
}