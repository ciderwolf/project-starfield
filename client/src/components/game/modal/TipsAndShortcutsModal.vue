<script setup lang="ts">
import { ref } from 'vue';
import Modal from '@/components/Modal.vue';
import StyleButton from '@/components/StyleButton.vue';
import { rollDie } from '@/api/game';

defineExpose({ open });

const visible = ref(false);
const size = ref(10);

function close() {
  visible.value = false;
}

function open() {
  visible.value = true;
  size.value = 10;
}


</script>

<template>
  <Modal title="Tips and Shortcuts" :visible="visible" @close="close">
    <h1>Tips and Shortcuts</h1>
    <div class="modal-content">
      <section>
        <h3>Keyboard Shortcuts</h3>
        <ul>
          <!-- board -->
          <li><strong>X</strong>: Untap all of your cards in play</li>
          <li><strong>C</strong>: Draw a card</li>
          <!-- deck -->
          <li><strong>R</strong>: Scoop your deck</li>
          <li><strong>M</strong>: Mulligan (Scoop your deck and draw seven cards)</li>
          <li><strong>V</strong>: Shuffle your deck</li>
          <li><strong>F</strong>: Search your library for a card</li>
          <!-- create -->
          <li><strong>W</strong>: Create a token</li>
          <li><strong>N</strong>: Create a card from outside the game</li>
          <!-- end -->
          <li><strong>E</strong>: End your turn</li>
          <li><strong>Q</strong>: End the game / start a new game</li>
        </ul>
      </section>
      <section>
        <h3>Other Tips</h3>
        <ul>
          <li>Click and drag on the battlefield to select multiple cards</li>
          <li>Hover over a message in the game log to see which card(s) it corresponds to</li>
          <li>Click the down arrow on the menu bar on the right-hand side to see an expanded list of options</li>
        </ul>
      </section>
    </div>
  </Modal>
</template>

<style scoped>
.modal-content {
  display: flex;
  flex-direction: column;
  /* align-items: center; */
}

.modal-content-row {
  display: flex;
  align-items: center;
  gap: 30px;
}

.dice-count-input {
  width: 50px;
  height: 36px;
  margin: 0 10px;
  padding: 5px;
  font-size: 1.25rem;
  text-align: center;
  border: 1px solid #0005;
  border-radius: 5px;
}
</style>