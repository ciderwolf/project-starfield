<script setup lang="ts">
import { endGame } from '@/api/lobby';
import { useNotificationsCache } from '@/cache/notifications';
import Modal from '@/components/Modal.vue';
import StyleButton from '@/components/StyleButton.vue';
import { useGameStore } from '@/stores/games';
import { client } from '@/ws';
import { ref } from 'vue';
import { useRouter } from 'vue-router';

defineExpose({ open });

const visible = ref(false);
const router = useRouter();

function open() {
  visible.value = true;
}


function keepDeckClicked() {
  visible.value = false;
  client.scoop();
}

function openSideboardClicked() {
  visible.value = false;
  const notifications = useNotificationsCache();
  notifications.sideboard();
}

function endGameClicked() {
  const gameId = useGameStore().gameState!.id
  endGame(gameId).then(() => {
    visible.value = false;
    router.push('/');
  });
}

</script>

<template>
  <Modal :visible="visible" title="End Game" @close="visible = false">
    <h2>End Game</h2>
    <div>
      <h3 class="option-title">End game and play another...</h3>
      <div class="buttons">
        <StyleButton @click="keepDeckClicked">Keep existing deck</StyleButton>
        <StyleButton @click="openSideboardClicked">Open sideboard</StyleButton>
      </div>
    </div>
    <h3 class="option-title">or</h3>
    <StyleButton class="end-game-button" type="danger" @click="endGameClicked">End game and leave</StyleButton>
  </Modal>
</template>

<style scoped>
.option-title {
  margin-bottom: 0.5rem;
  text-align: center;
}

.buttons {
  display: flex;
  justify-content: space-between;
  margin-top: 1rem;
  gap: 20px;
}

.buttons button {
  flex: 1;
}

.end-game-button {
  width: 100%;
}
</style>