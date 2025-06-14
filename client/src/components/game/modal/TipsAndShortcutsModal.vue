<script setup lang="ts">
import { ref } from 'vue';
import Modal from '@/components/Modal.vue';
import { HotkeyAttribute, hotkeys } from '@/hotkeys';

const hotkeyDescriptions = hotkeys.map(h => {
  return {
    key: h.key.toUpperCase(),
    description: h.description,
    isCard: h.attributes?.includes(HotkeyAttribute.CardSpecific),
    isOverrideable: h.attributes?.includes(HotkeyAttribute.ShiftToOverride)
  }
})

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
          <li v-for="hotkey in hotkeyDescriptions">
            <strong>{{ hotkey.key }}</strong>: {{ hotkey.description }}
            <span v-if="hotkey.isOverrideable" class="hint">⇧</span>
            <span v-if="hotkey.isCard" class="hint">
              (Hover)
            </span>
          </li>
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

.hint {
  color: gray;
  font-size: 0.8em
}
</style>