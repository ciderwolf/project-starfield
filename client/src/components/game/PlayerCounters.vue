<script setup lang="ts">
import { type PlayerAttributes, useBoardStore } from '@/stores/board';
import { client } from '@/ws';
import { ScreenPosition } from '@/zones';
import { type StyleValue, computed, ref, watchEffect } from 'vue';

const props = defineProps<{ player: PlayerAttributes }>();
const board = useBoardStore();

const lifeValue = ref(props.player.life);
const poisonValue = ref(props.player.poison);

watchEffect(() => {
  lifeValue.value = props.player.life;
  poisonValue.value = props.player.poison;
});

const positionInfo = computed<StyleValue>(() => {
  const position = board.getScreenPositionFromPlayerIndex(props.player.index);
  if (position == ScreenPosition.PRIMARY) {
    return {
      right: '0',
      top: 'calc(100vh - 108px)',
      'flex-direction': 'column-reverse',
    };
  } else {
    return {
      bottom: 'calc(100vh - 108px)',
      right: '0',
      'flex-direction': 'column',
    };
  }
});

const disabled = computed(() => {
  return board.playerIsMovable(props.player.index) === false;
});

const updateTimerHandle = ref<number | null>(null);

function updateValue(type: 'LIFE' | 'POISON', event: Event) {
  const target = event.target as HTMLInputElement;
  const value = parseInt(target.value);
  if (isNaN(value)) {
    return;
  }
  if (type === 'LIFE') {
    lifeValue.value = value;
  } else {
    poisonValue.value = value;
  }

  if (updateTimerHandle.value !== null) {
    clearTimeout(updateTimerHandle.value);
  }
  updateTimerHandle.value = setTimeout(() => {
    client.changePlayerAttribute(type, value);
  }, 500);
}


</script>

<template>
  <div class="player-counters" :style="positionInfo">
    <div class="player-counter">
      <div class="player-counter-label">Life</div>
      <input class="player-counter-value" type="number" :value="lifeValue" :disabled="disabled"
        @input="updateValue('LIFE', $event)">
    </div>
    <div class="player-counter">
      <div class="player-counter-label">Poison</div>
      <input class="player-counter-value" type="number" :value="poisonValue" :disabled="disabled"
        @input="updateValue('POISON', $event)">
    </div>
  </div>
</template>

<style scoped>
.player-counters {
  position: absolute;
  display: flex;
  justify-content: space-between;
  min-width: max-content;
}

.player-counter {
  min-width: max-content;
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: space-between;
  width: 78px;
}

.player-counter-label {
  text-transform: uppercase;
  font-size: 0.8em;
}

.player-counter-value {
  width: 78px;
  background-color: white;
  border: 1px solid #000;
  border-left: none;
  border-right: none;
  border-radius: 0;
  box-sizing: border-box;
}

.player-counter-value:disabled {
  -webkit-text-fill-color: black;
  opacity: 1;
}
</style>