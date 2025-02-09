import { reactive } from "vue";

export function resetReactive(obj: Record<string, any>) {
  const baseReactive = reactive({});
  for (const key in obj) {
    if (!baseReactive.hasOwnProperty(key)) {
      delete obj[key];
    }
  }
}