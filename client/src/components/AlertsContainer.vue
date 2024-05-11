<script setup lang="ts">
import { useAlertsStore } from '@/stores/alerts';

const alerts = useAlertsStore();

function closeAlert(id: string) {
  alerts.removeAlert(id);
}
</script>

<template>
  <div id="alerts" v-if="Object.keys(alerts.alerts).length > 0">
    <div v-for="alert in alerts.alerts" :key="alert.id">
      <div class="alert" :class="`alert-${alert.type}`" role="alert">
        <span @click="closeAlert(alert.id)" class="material-symbols-rounded close-alert">close</span>
        <b>{{ alert.title }}</b>
        {{ alert.message }}
      </div>
    </div>
  </div>
</template>

<style scoped>
#alerts {
  position: fixed;
  top: 0;
  right: 10px;
  z-index: 1000;
  padding: 20px;
  width: 300px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.alert {
  width: 100%;
  padding: 10px;
  border-radius: 5px;
  margin-bottom: 10px;
}

.close-alert {
  float: right;
  cursor: pointer;
  font-size: 20px;
}

.close-alert:hover {
  color: red;
}

.alert-error {
  background-color: #f8d7da;
  border-color: #f5c6cb;
  color: #721c24;
}

.alert-success {
  background-color: #d4edda;
  border-color: #c3e6cb;
  color: #155724;
}

.alert-info {
  background-color: #d1ecf1;
  border-color: #bee5eb;
  color: #0c5460;
}


.alert-warning {
  background-color: #fff3cd;
  border-color: #ffeeba;
  color: #856404;
}
</style>
