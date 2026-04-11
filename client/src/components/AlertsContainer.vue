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
  z-index: var(--z-modal);
  padding: var(--space-xl);
  width: 300px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-md);
}

.alert {
  width: 100%;
  padding: var(--space-md);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-md);
}

.close-alert {
  float: right;
  cursor: pointer;
  font-size: var(--font-size-fixed-lg);
}

.close-alert:hover {
  color: var(--color-danger);
}

.alert-error {
  background-color: var(--color-alert-danger-bg);
  border-color: var(--color-alert-danger-border);
  color: var(--color-alert-danger-text);
}

.alert-success {
  background-color: var(--color-alert-success-bg);
  border-color: var(--color-alert-success-border);
  color: var(--color-alert-success-text);
}

.alert-info {
  background-color: var(--color-alert-info-bg);
  border-color: var(--color-alert-info-border);
  color: var(--color-alert-info-text);
}


.alert-warning {
  background-color: var(--color-alert-warning-bg);
  border-color: var(--color-alert-warning-border);
  color: var(--color-alert-warning-text);
}
</style>
