import { reactive } from 'vue'
import { defineStore } from 'pinia'
import { v4 as uuid } from 'uuid';


type AlertType = 'success' | 'error' | 'warning' | 'info';
interface Alert {
  title: string;
  message: string;
  type: AlertType;
  id: string;
}

export const useAlertsStore = defineStore('alerts', () => {

  const alerts = reactive<{ [id: string]: Alert }>({});

  function addAlert(title: string, message: string, type: AlertType, expires?: number) {
    const id = uuid();
    alerts[id] = { title, message, type, id };
    if (expires) {
      setTimeout(() => {
        removeAlert(id);
      }, expires);
    }
  }

  function removeAlert(id: string) {
    delete alerts[id];
  }
  return { addAlert, alerts }
});