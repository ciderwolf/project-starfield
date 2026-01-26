<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import StyleButton from '@/components/StyleButton.vue';

const route = useRoute();
const router = useRouter();
const setCode = route.params.set as string;

// Text filter type
type TextFilter = {
  value: string;
  mode: 'include' | 'exclude';
};

type ComparisonOperator = '=' | '<' | '>' | '<=' | '>=' | '!=';

// Text search filters - consolidated into a map for easier management
const textFilters = ref({
  name: [{ value: '', mode: 'include' as const }] as TextFilter[],
  oracle: [{ value: '', mode: 'include' as const }] as TextFilter[],
  type: [{ value: '', mode: 'include' as const }] as TextFilter[],
  flavor: [{ value: '', mode: 'include' as const }] as TextFilter[],
  cardType: [{ value: '', mode: 'include' as const }] as TextFilter[],
});

// Helper functions for dynamic text inputs
const handleTextInput = (filters: TextFilter[], index: number) => {
  if (index === filters.length - 1 && filters[index].value.trim()) {
    filters.push({ value: '', mode: 'include' });
  }
};

const removeTextFilter = (filters: TextFilter[], index: number) => {
  if (filters.length > 1) {
    filters.splice(index, 1);
  } else {
    filters[0] = { value: '', mode: 'include' };
  }
};

// Color filters
const colorIdentity = ref({
  white: false,
  blue: false,
  black: false,
  red: false,
  green: false,
  colorless: false
});

const colorMode = ref<'exactly' | 'including' | 'at-most'>('including');

const numericFilters = ref({
  cmc: { value: '', operator: '=' as ComparisonOperator },
  power: { value: '', operator: '=' as ComparisonOperator },
  toughness: { value: '', operator: '=' as ComparisonOperator },
  loyalty: { value: '', operator: '=' as ComparisonOperator },
  defense: { value: '', operator: '=' as ComparisonOperator },
});

const manaSymbols = ref('');

// Rarity
const rarityFilters = ref({
  common: false,
  uncommon: false,
  rare: false,
  mythic: false
});

// Configuration data for rendering
const textFilterConfig = [
  { key: 'name', label: 'Card Name', placeholder: 'e.g., Lightning Bolt' },
  { key: 'oracle', label: 'Oracle Text', placeholder: 'e.g., draw a card' },
  { key: 'type', label: 'Type Line', placeholder: 'e.g., Dragon' },
  { key: 'flavor', label: 'Flavor Text', placeholder: 'e.g., magic' },
  { key: 'cardType', label: 'Card Types', placeholder: 'e.g., Legendary Creature, Instant, Artifact' },
] as const;

const colorConfig = [
  { key: 'white', symbol: 'W', label: 'White' },
  { key: 'blue', symbol: 'U', label: 'Blue' },
  { key: 'black', symbol: 'B', label: 'Black' },
  { key: 'red', symbol: 'R', label: 'Red' },
  { key: 'green', symbol: 'G', label: 'Green' },
  { key: 'colorless', symbol: 'C', label: 'Colorless' },
] as const;

const numericFilterConfig = [
  { key: 'power', label: 'Power' },
  { key: 'toughness', label: 'Toughness' },
  { key: 'loyalty', label: 'Loyalty' },
  { key: 'defense', label: 'Defense' },
] as const;

const operatorOptions = [
  { value: '=', label: '=' },
  { value: '<', label: '<' },
  { value: '>', label: '>' },
  { value: '<=', label: '≤' },
  { value: '>=', label: '≥' },
  { value: '!=', label: '≠' },
] as const;

const rarityConfig = [
  { key: 'common', label: 'Common' },
  { key: 'uncommon', label: 'Uncommon' },
  { key: 'rare', label: 'Rare' },
  { key: 'mythic', label: 'Mythic' },
] as const;

// Helper to add text filters to query
const addTextFiltersToQuery = (filters: TextFilter[], keyword: string, parts: string[]) => {
  filters.forEach(filter => {
    if (filter.value.trim()) {
      const prefix = filter.mode === 'exclude' ? '-' : '';
      parts.push(`${prefix}${keyword}:"${filter.value.trim()}"`);
    }
  });
};

// Build query string from filters
const buildQuery = () => {
  const parts: string[] = [];

  // Text filters
  addTextFiltersToQuery(textFilters.value.name, 'name', parts);
  addTextFiltersToQuery(textFilters.value.oracle, 'oracle', parts);
  addTextFiltersToQuery(textFilters.value.type, 'type', parts);
  addTextFiltersToQuery(textFilters.value.flavor, 'flavor', parts);
  addTextFiltersToQuery(textFilters.value.cardType, 'type', parts);

  // Color identity
  const selectedColors = Object.entries(colorIdentity.value)
    .filter(([_, selected]) => selected)
    .map(([color]) => colorConfig.find(c => c.key === color)?.symbol)
    .join('');

  if (selectedColors.length > 0) {
    if (colorIdentity.value.colorless && selectedColors === 'C') {
      parts.push('color:c');
    } else {
      const colors = selectedColors.replace('C', '');
      if (colors) {
        const modeMap = {
          'exactly': '=',
          'including': ':',
          'at-most': '<='
        };
        parts.push(`color${modeMap[colorMode.value]}${colors}`);
      }
    }
  }

  // Numeric filters
  Object.entries(numericFilters.value).forEach(([key, filter]) => {
    if (filter.value.trim()) {
      parts.push(`${key}${filter.operator}${filter.value.trim()}`);
    }
  });

  // Mana symbols
  if (manaSymbols.value.trim()) {
    parts.push(`mana:${manaSymbols.value.trim()}`);
  }

  // Rarity
  const selectedRarities = Object.entries(rarityFilters.value)
    .filter(([_, selected]) => selected)
    .map(([rarity]) => rarity[0]);

  if (selectedRarities.length > 0) {
    if (selectedRarities.length === 1) {
      parts.push(`rarity:${selectedRarities[0]}`);
    } else {
      parts.push(`(${selectedRarities.map(r => `rarity:${r}`).join(' OR ')})`);
    }
  }

  return parts.join(' ');
};

const searchQuery = computed(() => buildQuery());
const performSearch = () => {
  const query = searchQuery.value;
  if (query.trim()) {
    router.push({
      name: 'card-search',
      params: { set: setCode },
      query: { q: query }
    });
  }
};

const resetFilters = () => {
  // Reset text filters
  Object.keys(textFilters.value).forEach(key => {
    textFilters.value[key as keyof typeof textFilters.value] = [{ value: '', mode: 'include' }];
  });

  // Reset color filters
  Object.keys(colorIdentity.value).forEach(key => {
    colorIdentity.value[key as keyof typeof colorIdentity.value] = false;
  });
  colorMode.value = 'including';

  // Reset numeric filters
  Object.keys(numericFilters.value).forEach(key => {
    numericFilters.value[key as keyof typeof numericFilters.value] = { value: '', operator: '=' };
  });

  manaSymbols.value = '';

  // Reset rarity filters
  Object.keys(rarityFilters.value).forEach(key => {
    rarityFilters.value[key as keyof typeof rarityFilters.value] = false;
  });
};
</script>

<template>
  <div class="advanced-search-view">
    <div class="header">
      <h1>Advanced Search</h1>
      <div class="header-actions">
        <StyleButton @click="resetFilters" variant="secondary">Reset</StyleButton>
        <StyleButton @click="performSearch" variant="primary" :disabled="!searchQuery">Search</StyleButton>
      </div>
    </div>

    <div class="search-sections">
      <!-- Text Filters Section -->
      <section class="search-section">
        <h2>Text Filters</h2>
        <div class="filter-group">
          <div v-for="config in textFilterConfig" :key="config.key" class="operator-input">
            <span class="filter-label">{{ config.label }}</span>
            <div v-for="(filter, index) in textFilters[config.key]" :key="index" class="input-with-operator">
              <select v-model="filter.mode">
                <option value="include">Include</option>
                <option value="exclude">Exclude</option>
              </select>
              <input v-model="filter.value" type="text" :placeholder="config.placeholder"
                @input="handleTextInput(textFilters[config.key], index)" @keyup.enter="performSearch" />
              <button v-if="textFilters[config.key].length > 1 || filter.value"
                @click="removeTextFilter(textFilters[config.key], index)" class="remove-btn" type="button">×</button>
            </div>
          </div>
        </div>
      </section>

      <!-- Colors Section -->
      <section class="search-section">
        <h2>Colors</h2>
        <div class="filter-group">
          <div class="color-mode">
            <label>
              <input type="radio" v-model="colorMode" value="including" />
              <span>Including these colors</span>
            </label>
            <label>
              <input type="radio" v-model="colorMode" value="exactly" />
              <span>Exactly these colors</span>
            </label>
            <label>
              <input type="radio" v-model="colorMode" value="at-most" />
              <span>At most these colors</span>
            </label>
          </div>

          <div class="color-checkboxes">
            <label v-for="color in colorConfig" :key="color.key" class="color-checkbox">
              <input type="checkbox" v-model="colorIdentity[color.key]" />
              <abbr :class="`card-symbol card-symbol-${color.symbol}`">{{ color.symbol }}</abbr>
              <span>{{ color.label }}</span>
            </label>
          </div>
        </div>
      </section>

      <!-- Mana Cost Section -->
      <section class="search-section">
        <h2>Mana Cost</h2>
        <div class="filter-group">
          <label class="operator-input">
            <span class="filter-label">Mana Value (CMC)</span>
            <div class="input-with-operator">
              <select v-model="numericFilters.cmc.operator">
                <option v-for="op in operatorOptions" :key="op.value" :value="op.value">{{ op.label }}</option>
              </select>
              <input v-model="numericFilters.cmc.value" type="text" placeholder="e.g., 3"
                @keyup.enter="performSearch" />
            </div>
          </label>

          <label>
            <span class="filter-label">Mana Symbols</span>
            <input v-model="manaSymbols" type="text" placeholder="e.g., {2}{R}{R}" @keyup.enter="performSearch" />
          </label>
        </div>
      </section>

      <!-- Stats Section -->
      <section class="search-section">
        <h2>Stats</h2>
        <div class="filter-group stats-group">
          <label v-for="stat in numericFilterConfig" :key="stat.key" class="operator-input">
            <span class="filter-label">{{ stat.label }}</span>
            <div class="input-with-operator">
              <select v-model="numericFilters[stat.key].operator">
                <option v-for="op in operatorOptions" :key="op.value" :value="op.value">{{ op.label }}</option>
              </select>
              <input v-model="numericFilters[stat.key].value" type="text" placeholder="e.g., 2"
                @keyup.enter="performSearch" />
            </div>
          </label>
        </div>
      </section>

      <!-- Rarity Section -->
      <section class="search-section">
        <h2>Rarity</h2>
        <div class="filter-group">
          <div class="checkbox-grid">
            <label v-for="rarity in rarityConfig" :key="rarity.key">
              <input type="checkbox" v-model="rarityFilters[rarity.key]" />
              <span>{{ rarity.label }}</span>
            </label>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style>
@import url('@/assets/symbols.css');
</style>

<style scoped>
.advanced-search-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 2rem;
  padding-bottom: 6rem;
}

.header {
  position: sticky;
  top: 0;
  background-color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 2rem;
  padding-top: 2rem;
}

.header h1 {
  margin: 0;
  font-size: 2rem;
}

.header-actions {
  display: flex;
  gap: 1rem;
}

.search-sections {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.search-section {
  background-color: #eee;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 1.5rem;
}

.search-section h2 {
  margin: 0 0 1rem 0;
  font-size: 1.5rem;
}

.search-section h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.1rem;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.filter-group label {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.filter-label {
  font-weight: 600;
  margin-bottom: 0.5rem;
  display: block;
}

.remove-btn {
  border: none;
  background: none;
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
  transition: all 0.2s;
}

.remove-btn:hover {
  color: red;
}

input[type="text"],
select {
  padding: 0.5rem;
}

.color-mode {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.color-mode label {
  flex-direction: row;
  align-items: center;
  gap: 0.5rem;
}

.color-checkboxes {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 0.75rem;
}

.color-checkbox {
  flex-direction: row !important;
  align-items: center;
  gap: 0.5rem !important;
  padding: 0.5rem;
  border-radius: 4px;
  cursor: pointer;
}

.color-checkbox .card-symbol {
  margin: 0;
  width: 20px;
  height: 20px;
}

.checkbox-grid {
  display: flex;
  gap: 0.75rem;
}

.checkbox-grid label {
  flex-direction: row !important;
  align-items: center;
  gap: 0.5rem !important;
}

.operator-input {
  gap: 0.5rem;
  display: flex;
  flex-direction: column;
}

.operator-input .input-with-operator {
  display: flex;
  gap: 0.5rem;
}

.operator-input select {
  width: 80px;
  min-width: max-content;
  flex-shrink: 0;
}

.operator-input input {
  flex: 1;
}

.stats-group {
  display: flex;
}

input[type="checkbox"],
input[type="radio"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

@media (max-width: 768px) {
  .advanced-search-view {
    padding: 1rem;
  }

  .header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }

  .footer-actions {
    flex-direction: column;
  }
}
</style>