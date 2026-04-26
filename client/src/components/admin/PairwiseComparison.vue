<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import type { OracleCard } from '@/api/message';
import CardThumbnail from '@/components/game/modal/CardThumbnail.vue';
import Modal from '@/components/shared/Modal.vue';
import StarRating from '@/components/admin/StarRating.vue';
import StyleButton from '@/components/shared/StyleButton.vue';
import HelpPopover from './HelpPopover.vue';

const props = defineProps<{
  cards: OracleCard[];
}>();

const emit = defineEmits<{
  (event: 'close'): void;
}>();

// v-model for the existing rating record (0.5-5 stars in 0.5 increments, 0 = unrated)
const ratings = defineModel<Record<string, number>>('ratings', { required: true });

const MIN_STARS = 0.5;
const MAX_STARS = 5;
const DEFAULT_STARS = 3;
// K controls how much each comparison moves a rating, in star units. With K = 1
// an evenly-matched comparison moves the winner up by ~0.5 stars and the loser
// down by ~0.5 stars; lopsided matchups move less.
const K = 1.0;
// Scale of the logistic in star units: a 1-star gap means the higher-rated card
// is the ~73% favorite, so an upset moves ratings notably more than a confirmation.
const SCALE = 1.0;

function clamp(v: number) {
  return Math.max(MIN_STARS, Math.min(MAX_STARS, v));
}
function roundToHalf(v: number) {
  return Math.round(v * 2) / 2;
}

// Continuous internal rating in star units. Preserves fractional progress
// between half-star steps so repeated nudges accumulate. Written back to the
// model rounded to the nearest 0.5.
const internal = ref<Record<string, number>>({});
const comparisonsDone = ref(0);
const recentPairs = ref<string[]>([]);
const RECENT_PAIR_WINDOW = 12;

const currentPair = ref<[OracleCard, OracleCard] | null>(null);

onMounted(() => {
  initialize();
  pickNextPair();
});


function initialize() {
  const next: Record<string, number> = {};
  for (const card of props.cards) {
    const existing = internal.value[card.id];
    const seeded = ratings.value[card.id];
    next[card.id] = existing ?? (seeded && seeded > 0 ? seeded : DEFAULT_STARS);
  }
  internal.value = next;
  comparisonsDone.value = 0;
  recentPairs.value = [];
}

function pairKey(a: string, b: string): string {
  return a < b ? `${a}|${b}` : `${b}|${a}`;
}

function pickNextPair() {
  const cards = props.cards;
  if (cards.length < 2) {
    currentPair.value = null;
    return;
  }

  // Prefer pairs whose current ratings are close together — those comparisons
  // are the most informative.
  const recentSet = new Set(recentPairs.value);
  let best: [OracleCard, OracleCard] | null = null;
  let bestGap = Infinity;

  const attempts = 30;
  for (let i = 0; i < attempts; i++) {
    const a = cards[Math.floor(Math.random() * cards.length)];
    const b = cards[Math.floor(Math.random() * cards.length)];
    if (a.id === b.id) continue;
    const key = pairKey(a.id, b.id);
    if (recentSet.has(key)) continue;
    const gap = Math.abs(
      (internal.value[a.id] ?? DEFAULT_STARS) - (internal.value[b.id] ?? DEFAULT_STARS),
    );
    if (gap < bestGap) {
      best = [a, b];
      bestGap = gap;
      if (gap < 0.25) break;
    }
  }

  if (!best) {
    const a = cards[Math.floor(Math.random() * cards.length)];
    let b = cards[Math.floor(Math.random() * cards.length)];
    while (b.id === a.id) {
      b = cards[Math.floor(Math.random() * cards.length)];
    }
    best = [a, b];
  }

  currentPair.value = best;
}

function expectedScore(self: number, opponent: number): number {
  return 1 / (1 + Math.pow(10, (opponent - self) / SCALE));
}

function applyResult(aId: string, bId: string, scoreA: number) {
  const ra = internal.value[aId] ?? DEFAULT_STARS;
  const rb = internal.value[bId] ?? DEFAULT_STARS;
  const expA = expectedScore(ra, rb);
  internal.value[aId] = clamp(ra + K * (scoreA - expA));
  internal.value[bId] = clamp(rb - K * (scoreA - expA));
}

function writeBack() {
  const next: Record<string, number> = { ...ratings.value };
  for (const card of props.cards) {
    const v = internal.value[card.id];
    if (v !== undefined) {
      next[card.id] = roundToHalf(v);
    }
  }
  ratings.value = next;
}

function recordPair(aId: string, bId: string) {
  recentPairs.value.push(pairKey(aId, bId));
  if (recentPairs.value.length > RECENT_PAIR_WINDOW) {
    recentPairs.value.shift();
  }
}

function chooseWinner(winnerId: string) {
  if (!currentPair.value) return;
  const [a, b] = currentPair.value;
  applyResult(a.id, b.id, winnerId === a.id ? 1 : 0);
  recordPair(a.id, b.id);
  comparisonsDone.value++;
  writeBack();
  pickNextPair();
}

function callItEqual() {
  if (!currentPair.value) return;
  const [a, b] = currentPair.value;
  applyResult(a.id, b.id, 0.5);
  recordPair(a.id, b.id);
  comparisonsDone.value++;
  writeBack();
  pickNextPair();
}

function skip() {
  if (!currentPair.value) return;
  const [a, b] = currentPair.value;
  recordPair(a.id, b.id);
  pickNextPair();
}

const cardA = computed(() => currentPair.value?.[0] ?? null);
const cardB = computed(() => currentPair.value?.[1] ?? null);
</script>

<template>
  <div class="pairwise">
    <div style="display: flex; flex-direction: column;" v-if="cardA && cardB">
      <div class="pair">
        <div class="choice">
          <button class="card-button" @click="chooseWinner(cardA.id)">
            <img class="card-image" :src="cardA.image">
          </button>
          <StarRating :model-value="ratings[cardA.id] ?? 0" readonly />
        </div>
        <div class="middle">
          <span class="vs">vs</span>
        </div>
        <div class="choice">
          <button class="card-button" @click="chooseWinner(cardB.id)">
            <img class="card-image" :src="cardB.image">
          </button>
          <StarRating :model-value="ratings[cardB.id] ?? 0" readonly />
        </div>
      </div>
      <div class="pair">
        <StyleButton @click="callItEqual">About equal</StyleButton>
        <StyleButton @click="skip">Skip</StyleButton>
      </div>
    </div>
    <p v-else class="hint">Not enough cards to compare.</p>
  </div>
</template>

<style scoped>
.pairwise {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md);
}

.hint {
  color: var(--color-gray-600);
  text-align: center;
  margin: 0;
}

.pair {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-lg);
  flex-wrap: wrap;
}

.choice {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
}

.card-image {
  width: 200px;
  height: auto;
}

.card-button {
  border: 2px solid transparent;
  border-radius: var(--radius-lg);
  padding: 4px;
  background: none;
  cursor: pointer;
}

.card-button:hover {
  border-color: var(--color-primary);
}

.middle {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
  min-width: 120px;
}

.vs {
  font-size: 1.25rem;
  font-weight: bold;
  color: var(--color-gray-600);
}
</style>
