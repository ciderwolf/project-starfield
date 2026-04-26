<script setup lang="ts">
defineProps<{ title: string; subtitle?: string }>();
</script>

<template>
    <div class="empty-state">
        <h3>{{ title }}</h3>
        <p v-if="subtitle">{{ subtitle }}</p>
    </div>
</template>

<style scoped>
.empty-state {
    margin-top: 1em;
    text-align: center;
    color: var(--color-gray-900);
    font-style: italic;
}
</style>
