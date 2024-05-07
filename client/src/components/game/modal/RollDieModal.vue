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

async function rollDieSubmit(size: number) {
  const result = await rollDie(size);
  console.log(result);
  close();
}

</script>

<template>
  <Modal title="Roll a Die" :visible="visible" @close="close">
    <h1>Roll a Die</h1>
    <div class="modal-content">
      <h3 class="modal-subheading">Choose one of these dice</h3>
      <section class="modal-content-row">
        <style-button class="icon-button" @click="rollDieSubmit(2)">Flip a Coin</style-button>
        <style-button class="icon-button" @click="rollDieSubmit(6)">Roll a D6</style-button>
        <style-button class="icon-button" @click="rollDieSubmit(20)">Roll a D20</style-button>
      </section>
      <h3 class="modal-subheading">Or, enter a number of sides</h3>
      <section class="modal-content-row">
        <input class="dice-count-input" type="number" v-model="size" />
        <StyleButton class="icon-button" @click="rollDieSubmit(size)">
          Roll a D{{ size }}
        </StyleButton>
      </section>
    </div>
  </Modal>
</template>

<style scoped>
.modal-content {
  display: flex;
  flex-direction: column;
  align-items: center;
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