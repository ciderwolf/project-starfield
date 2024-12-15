<script setup lang="ts">
import { useDraftStore } from '@/stores/draft';
import { onMounted, ref } from 'vue';

const draft = useDraftStore();
const arrows = ref<HTMLElement>();
const packQueueMarker = ref<HTMLElement[]>();

onMounted(() => {
  console.log(packQueueMarker.value)
  // drawArrows();
});

function getPlayerIdentifier(id: string) {
  return draft.players.find(player => player.id === id)?.name ?? 'Bot Player';
}

function getPlayerIcon(id: string) {
  return draft.players.some(player => player.id === id) ? 'person' : 'robot_2';
}
type pos = { x: number, y: number };
function drawArrows() {
  const svg = arrows.value!;

  const players = packQueueMarker.value!;
  const positions = Array.from(players).map(player => {
    const rect = player.getBoundingClientRect();
    return {
      x: rect.left - 7, //+ rect.width / 2,
      y: rect.top + rect.height / 2
    };
  });

  for (let i = 0; i < positions.length - 1; i++) {
    const start = positions[i];
    const end = positions[(i + 1) % positions.length];
    drawCurvedArrow(svg, start, end);
  }

  // Draw curved arrow from last to first
  if (positions.length > 1) {
    const start = positions[positions.length - 1];
    const end = positions[0];
    drawCurvedArrowBack(svg, start, end);
  }
}

function drawCurvedArrowBack(svg: HTMLElement, start: pos, end: pos) {
  const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
  const midX = (start.x + end.x) / 2 - 100;
  const midY = (start.y + end.y) / 2; // Adjust the curve height as needed
  const d = `M ${start.x} ${start.y - 5} Q ${midX} ${midY} ${end.x} ${end.y + 5}`;
  path.setAttribute('d', d);
  path.setAttribute('stroke', 'red');
  path.setAttribute('fill', 'transparent');
  path.setAttribute('marker-end', 'url(#arrowhead)');
  svg.appendChild(path);
}

function drawCurvedArrow(svg: HTMLElement, start: pos, end: pos) {
  const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
  const midX = (start.x + end.x) / 2 - 20;
  const midY = (start.y + end.y) / 2; // Adjust the curve height as needed
  const d = `M ${start.x} ${start.y + 5} Q ${midX} ${midY} ${end.x} ${end.y - 5}`;
  path.setAttribute('d', d);
  path.setAttribute('stroke', 'black');
  path.setAttribute('fill', 'transparent');
  path.setAttribute('marker-end', 'url(#arrowhead)');
  svg.appendChild(path);
}

</script>

<template>
  <div class="draft-pack-queues">
    <svg ref="arrows" width="100%" height="100%" style="position: absolute; top: 0; left: 0; z-index: -1;">
      <defs>
        <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="0" refY="3.5" orient="auto">
          <polygon points="0 0, 10 3.5, 0 7" />
        </marker>
      </defs>
    </svg>
    <p ref="packQueueMarker" class="pack-queue-player" v-for="[id, packs] in Object.entries(draft.packQueues)"
      :key="id">
      <span class="material-symbols-rounded">{{ getPlayerIcon(id) }}</span>
      <span>
        {{ getPlayerIdentifier(id) }} ({{ packs }})
      </span>
    </p>
  </div>
</template>

<style scoped>
.draft-pack-queues {
  margin-top: 20px;
}

.pack-queue-player {
  display: flex;
  align-items: center;
  margin: 5px 0;
}

.pack-queue-container {
  position: relative;
}

.pack-queue-player {
  position: relative;
  /* display: inline-block; */
  margin: 10px;
}
</style>