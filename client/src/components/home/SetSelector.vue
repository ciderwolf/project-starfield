<script setup lang="ts">
import { downloadSet, getSets } from '@/api/draft';
import { ref, onMounted, computed, onUnmounted } from 'vue';
import type { DraftSet } from '@/api/draft';
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import LoadingButton from '../LoadingButton.vue';
import { setConstantValue } from 'typescript';

const sets = ref<DraftSet[]>([]);
const loading = ref(true);
const selectedSet = ref<DraftSet | null>(null);
const isDropdownOpen = ref(false);
const searchQuery = ref('');
const searchInputRef = ref<HTMLInputElement | null>(null);
const dropdownRef = ref<HTMLElement | null>(null);
const dropdownPosition = ref({ top: 0, left: 0, width: 0 });

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

function updateDropdownPosition() {
  if (!dropdownRef.value) return;

  const button = document.querySelector('.dropdown-toggle') as HTMLElement;
  if (button) {
    const rect = button.getBoundingClientRect();
    dropdownPosition.value = {
      top: rect.bottom,
      left: rect.left,
      width: rect.width
    };
  }
};

function toggleDropdown() {
  isDropdownOpen.value = !isDropdownOpen.value;
  if (isDropdownOpen.value) {
    // Wait for the next tick to ensure the dropdown is in the DOM
    setTimeout(() => {
      updateDropdownPosition();
    }, 0);
  }
};

function selectSet(set: DraftSet) {
  selectedSet.value = set;
  emit('set-selected', set);
  searchQuery.value = set.name;
  isDropdownOpen.value = false;
};

const filteredSets = computed(() => {
  if (!searchQuery.value) return sets.value;
  const query = searchQuery.value.toLowerCase();
  return sets.value.filter(set =>
    set.name.toLowerCase().includes(query) ||
    set.setType.toLowerCase().includes(query)
  );
});

function openDropdown() {
  isDropdownOpen.value = true;
  setTimeout(() => {
    updateDropdownPosition();
  }, 0);
};

function clearSearch() {
  searchQuery.value = '';
  if (searchInputRef.value) {
    searchInputRef.value.focus();
  }
};

function closeDropdownOnOutsideClick(e: MouseEvent) {
  // don't close if the click on .dropdown-menu
  if (dropdownRef.value && dropdownRef.value.contains(e.target as Node)) {
    return;
  }
  isDropdownOpen.value = false;
};

// Update dropdown position on window resize
function handleResize() {
  if (isDropdownOpen.value) {
    updateDropdownPosition();
  }
};

onMounted(() => {
  window.addEventListener('resize', handleResize);
  window.addEventListener('scroll', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  window.removeEventListener('scroll', handleResize);
});

async function downloadSetFromScryfall(): Promise<void> {
  if (!searchQuery.value) return;

  const result = await downloadSet(searchQuery.value);
  if (!sets.value.some(s => s.id === result.id)) {
    sets.value.push(result);
  }
  selectSet(result);
}
</script>

<template>
  <div class="set-selector" v-click-outside="closeDropdownOnOutsideClick">
    <div class="dropdown">
      <button @click="toggleDropdown" class="dropdown-toggle" :disabled="loading">
        <loading-spinner v-if="loading" />
        <template v-else>
          <div class="search-container">
            <input ref="searchInputRef" type="text" v-model="searchQuery" placeholder="Search sets..."
              class="search-input" @focus="openDropdown" @click.stop />
            <button v-if="searchQuery" class="clear-search-btn" @click.stop="clearSearch" title="Clear search">
              ✕
            </button>
          </div>
        </template>
      </button>

      <Teleport to="body">
        <div v-if="isDropdownOpen" ref="dropdownRef" class="dropdown-menu" :style="{
          top: `${dropdownPosition.top}px`,
          left: `${dropdownPosition.left}px`,
          width: `${dropdownPosition.width}px`
        }">
          <div v-if="filteredSets.length === 0" class="no-results">
            No matching sets found
            <LoadingButton :on-click="downloadSetFromScryfall" small>Import set with code '{{ searchQuery }}' from
              Scryfall
            </LoadingButton>
          </div>
          <div v-for="set in filteredSets" :key="set.id" class="dropdown-item" @click="selectSet(set)"
            :class="{ 'active': selectedSet?.id === set.id }">
            <img v-if="set.image" :src="set.image" class="set-icon" />
            <span v-else class="material-symbols-rounded set-icon">deployed_code</span>
            <span>{{ set.name }}</span>
          </div>
        </div>
      </Teleport>
    </div>
  </div>
</template>

<style scoped>
.set-selector {
  position: relative;
  width: 100%;
}

.dropdown {
  position: relative;
}

.dropdown-toggle {
  display: flex;
  align-items: center;
  width: 100%;
  background: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  text-align: left;
}

.dropdown-toggle:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.dropdown-arrow {
  margin-left: auto;
}

.dropdown-menu {
  position: fixed;
  max-height: 300px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid #ccc;
  border-radius: 4px;
  z-index: 9999;
  margin-top: 2px;
  padding-top: 0;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.search-container {
  position: relative;
  width: 100%;
}

.search-input {
  width: 100%;
  padding-right: 30px;
  box-sizing: border-box;
  font-size: 16px;
}

.clear-search-btn {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  font-size: 14px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
}

.clear-search-btn:hover {
  color: red;
}

.dropdown-item {
  display: flex;
  align-items: center;
  padding: 10px;
  cursor: pointer;
}

.dropdown-item:hover {
  background: #f5f5f5;
}

.dropdown-item.active {
  background: #e0e0e0;
}

.set-icon {
  width: 20px;
  height: 20px;
  margin-right: 8px;
  object-fit: contain;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 8px;
}

.no-results {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 10px;
  color: #666;
  font-style: italic;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(360deg);
  }
}
</style>