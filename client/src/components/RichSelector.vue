<script setup lang="ts" generic="T">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import LoadingSpinner from '@/components/LoadingSpinner.vue';

const props = withDefaults(defineProps<{
    items: T[];
    selectionMode?: 'single' | 'multiple';
    loading?: boolean;
    placeholder?: string;
    getKey?: (item: T) => string | number;
    getLabel?: (item: T) => string;
    filter?: (item: T, query: string) => boolean;
}>(), {
    selectionMode: 'single',
    loading: false,
    placeholder: 'Search...',
    getLabel: (item: any) => String(item?.name ?? item),
    getKey: (item: any) => item?.id ?? String(item),
});

const emit = defineEmits<{
    (e: 'select', item: T): void;
}>();

const selectedKeys = defineModel<Array<string | number>>('selectedKeys', { default: () => [] });

defineExpose({
    clearSearch
})

const isOpen = ref(false);
const searchQuery = ref('');
const searchInputRef = ref<HTMLInputElement | null>(null);
const toggleRef = ref<HTMLElement | null>(null);
const dropdownRef = ref<HTMLElement | null>(null);
const dropdownPosition = ref({ top: 0, left: 0, width: 0 });

const filteredItems = computed(() => {
    if (!searchQuery.value) return props.items;
    const query = searchQuery.value.toLowerCase();
    if (props.filter) {
        return props.items.filter(item => props.filter!(item, query));
    }
    return props.items.filter(item => props.getLabel(item).toLowerCase().includes(query));
});

function updateDropdownPosition() {
    if (!toggleRef.value) return;
    const rect = toggleRef.value.getBoundingClientRect();
    dropdownPosition.value = {
        top: rect.bottom,
        left: rect.left,
        width: rect.width,
    };
}

function openDropdown() {
    isOpen.value = true;
    setTimeout(updateDropdownPosition, 0);
}

function toggleDropdown() {
    isOpen.value = !isOpen.value;
    if (isOpen.value) setTimeout(updateDropdownPosition, 0);
}

function isSelected(item: T) {
    return selectedKeys.value.includes(props.getKey(item));
}

function selectItem(item: T) {
    const key = props.getKey(item);
    if (props.selectionMode === 'multiple') {
        selectedKeys.value = selectedKeys.value.includes(key)
            ? selectedKeys.value.filter(k => k !== key)
            : [...selectedKeys.value, key];
    } else {
        selectedKeys.value = [key];
        searchQuery.value = props.getLabel(item);
        isOpen.value = false;
    }
    emit('select', item);
}

function clearSearch() {
    searchQuery.value = '';
    searchInputRef.value?.focus();
}

function closeOnOutsideClick(e: MouseEvent) {
    if (dropdownRef.value?.contains(e.target as Node)) return;
    isOpen.value = false;
}

function handleResize() {
    if (isOpen.value) updateDropdownPosition();
}

onMounted(() => {
    window.addEventListener('resize', handleResize);
    window.addEventListener('scroll', handleResize);
});

onUnmounted(() => {
    window.removeEventListener('resize', handleResize);
    window.removeEventListener('scroll', handleResize);
});
</script>

<template>
    <div class="rich-selector" v-click-outside="closeOnOutsideClick">
        <div class="dropdown">
            <button ref="toggleRef" @click="toggleDropdown" class="dropdown-toggle" :disabled="loading">
                <LoadingSpinner v-if="loading" />
                <template v-else>
                    <div class="search-container">
                        <input ref="searchInputRef" type="text" v-model="searchQuery" :placeholder="placeholder"
                            class="search-input" @focus="openDropdown" @click.stop />
                        <button v-if="searchQuery" class="clear-search-btn" @click.stop="clearSearch"
                            title="Clear search">
                            ✕
                        </button>
                    </div>
                </template>
            </button>

            <Teleport to="body">
                <div v-if="isOpen" ref="dropdownRef" class="dropdown-menu" :style="{
                    top: `${dropdownPosition.top}px`,
                    left: `${dropdownPosition.left}px`,
                    width: `${dropdownPosition.width}px`,
                }">
                    <div v-if="filteredItems.length === 0" class="no-results">
                        <slot name="empty" :query="searchQuery">No matching items found</slot>
                    </div>
                    <div v-for="item in filteredItems" :key="getKey(item)" class="dropdown-item"
                        :class="{ active: isSelected(item) }" @click="selectItem(item)">
                        <input v-if="props.selectionMode === 'multiple'" type="checkbox" class="dropdown-check"
                            :checked="isSelected(item)" @click.stop @change="selectItem(item)" />
                        <slot name="item" :item="item" :active="isSelected(item)">
                            {{ getLabel(item) }}
                        </slot>
                    </div>
                </div>
            </Teleport>
        </div>
    </div>
</template>

<style scoped>
.rich-selector {
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
    background: var(--color-white);
    border: none;
    border-radius: var(--radius-md);
    cursor: pointer;
    text-align: left;
}

.dropdown-toggle:disabled {
    cursor: not-allowed;
    opacity: 0.7;
}

.dropdown-menu {
    position: fixed;
    max-height: 300px;
    overflow-y: auto;
    background: var(--color-white);
    border: 1px solid var(--color-gray-400);
    border-radius: var(--radius-md);
    z-index: var(--z-modal);
    margin-top: 2px;
    padding-top: 0;
    box-shadow: var(--shadow-dropdown);
}

.search-container {
    position: relative;
    width: 100%;
}

.search-input {
    width: 100%;
    padding-right: 30px;
    box-sizing: border-box;
    font-size: var(--font-size-fixed-md);
}

.clear-search-btn {
    position: absolute;
    right: 16px;
    top: 50%;
    transform: translateY(-50%);
    background: none;
    border: none;
    color: var(--color-gray-600);
    cursor: pointer;
    font-size: var(--font-size-fixed-sm);
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
}

.clear-search-btn:hover {
    color: var(--color-danger);
}

.dropdown-item {
    display: flex;
    align-items: center;
    padding: var(--space-md);
    cursor: pointer;
}

.dropdown-item:hover {
    background: var(--color-gray-100);
}

.dropdown-item.active {
    background: var(--color-gray-300);
}

.dropdown-check {
    margin-right: var(--space-sm);
    cursor: pointer;
}

.no-results {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-md);
    padding: var(--space-md);
    color: var(--color-gray-700);
    font-style: italic;
}
</style>
