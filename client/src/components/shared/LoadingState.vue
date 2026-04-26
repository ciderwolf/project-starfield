<script setup lang="ts">
import LoadingSpinner from './LoadingSpinner.vue';

withDefaults(defineProps<{ message?: string, fullPage?: boolean }>(), {
    message: 'Loading...',
    fullPage: false,
});
</script>

<template>
    <div class="loading-state" :class="{ 'loading-state--full-page': fullPage }">
        <h3 class="loading-state-title" v-if="!fullPage">
            <LoadingSpinner /> {{ message }}
        </h3>
        <h1 class="loading-state-title" v-else>
            <LoadingSpinner /> {{ message }}
        </h1>
    </div>
</template>

<style scoped>
.loading-state-title {
    display: flex;
    align-items: center;
    justify-content: center;
}

.loading-state--full-page .loading-state-title {
    flex-direction: row;
    margin-top: 25%;
}
</style>
