<script setup lang="ts">
import { downloadSet, getSets } from '@/api/draft';
import { ref, onMounted } from 'vue';
import type { DraftSet } from '@/api/draft';
import LoadingButton from '../LoadingButton.vue';
import RichSelector from '../RichSelector.vue';

const sets = ref<DraftSet[]>([]);
const loading = ref(true);
const selectedSet = ref<DraftSet | null>(null);

const emit = defineEmits<{
  (e: 'set-selected', set: DraftSet): void;
}>();

onMounted(async () => {
  try {
    sets.value = await getSets();
    if (sets.value.length > 0) {
      selectedSet.value = sets.value[0];
    }
  } catch (error) {
    console.error('Failed to load draft sets:', error);
  } finally {
    loading.value = false;
  }
});

function onSelect(set: DraftSet) {
  selectedSet.value = set;
  emit('set-selected', set);
}

function filterSet(set: DraftSet, query: string) {
  return set.name.toLowerCase().includes(query) || set.setType.toLowerCase().includes(query);
}

async function downloadSetFromScryfall(query: string): Promise<void> {
  if (!query) return;

  const result = await downloadSet(query);
  if (!sets.value.some(s => s.id === result.id)) {
    sets.value.push(result);
  }
  onSelect(result);
}
</script>

<template>
  <RichSelector :items="sets" :loading="loading" placeholder="Search sets..." :filter="filterSet" @select="onSelect">
    <template #item="{ item }">
      <img v-if="item.image" :src="item.image" class="set-icon" />
      <span v-else class="material-symbols-rounded set-icon">deployed_code</span>
      <span>{{ item.name }}</span>
    </template>
    <template #empty="{ query }">
      No matching sets found
      <LoadingButton :on-click="() => downloadSetFromScryfall(query)" small>
        Import set with code '{{ query }}' from Scryfall
      </LoadingButton>
    </template>
  </RichSelector>
</template>

<style scoped>
.set-icon {
  width: 20px;
  height: 20px;
  margin-right: 8px;
  object-fit: contain;
}
</style>
