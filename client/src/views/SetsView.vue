<script setup lang="ts">
import LoadingState from '@/components/shared/LoadingState.vue';
import EmptyState from '@/components/shared/EmptyState.vue';
import IconButton from '@/components/shared/IconButton.vue';
import MenuNavigationBlade from '@/components/home/MenuNavigationBlade.vue';
import ItemCard from '@/components/home/ItemCard.vue';
import ItemCardGrid from '@/components/home/ItemCardGrid.vue';
import type { ComponentExposed } from 'vue-component-type-helpers';
import { ref, onMounted } from 'vue';
import { getCustomSets } from '@/api/card';
import StyleButton from '@/components/shared/StyleButton.vue';

interface SetInfo {
  name: string;
  code: string;
  icon: string;
}

const sets = ref<SetInfo[]>([]);
const isLoading = ref(true);

onMounted(async () => {
  try {
    sets.value = await getCustomSets();
  } finally {
    isLoading.value = false;
  }
});

const menuNavigationBlade = ref<ComponentExposed<typeof MenuNavigationBlade>>();
function showMenuBlade() {
  menuNavigationBlade.value?.showMenu();
}
</script>

<template>
  <div id="sets">
    <MenuNavigationBlade ref="menuNavigationBlade" />
    <div class="title">
      <h1 class="title-text">
        <IconButton @click="showMenuBlade" icon="menu" />
        Sets
      </h1>
      <router-link to="/sets/all/cards">
        <StyleButton small>Search All Cards</StyleButton>
      </router-link>
    </div>
    <div v-if="isLoading">
      <LoadingState message="Loading sets..." />
    </div>
    <ItemCardGrid v-else-if="sets.length > 0">
      <ItemCard v-for="set in sets" :key="set.code" :title="set.name" :image="set.icon" :image-alt="`${set.name} Icon`"
        :to="`/sets/${set.code.toLowerCase()}/cards`" image-fit="contain" />
    </ItemCardGrid>
    <EmptyState v-else title="No sets available." />
  </div>
</template>

<style scoped>
#sets {
  padding: 0 2em;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.title {
  display: flex;
  gap: var(--space-xl);
  align-items: center;
}

.title-text {
  display: flex;
  gap: var(--space-lg);
  align-items: center;
}
</style>
