<script setup lang="ts">
import { computed, ref } from 'vue';

interface Card {
    count: number;
    name: string;
    type: string;
    image: string;
}

defineProps<{ card: Card }>();

const display = ref("none");
const left = ref(0);
const top = ref(0);
const cardImage = ref<HTMLInputElement | null>(null);
const face = ref("front");

const style = computed(() => ({
    display: display.value,
    left: `${left.value}px`,
    top: `${top.value}px`
}))

function normalizePreviewY(e: MouseEvent) {
    const height = (cardImage.value?.height ?? 0) + 20;
    let newY = e.pageY;
    if (newY + height > window.innerHeight + window.scrollY) {
        newY = window.innerHeight + window.scrollY - height;
    }
    return newY;
}

function normalizePreviewX(e: MouseEvent) {
    const width = (cardImage.value?.width ?? 0) + 20;
    let newX = e.pageX;
    if (newX + width > window.innerWidth + window.scrollX) {
        newX = window.innerWidth + window.scrollX - width;
    }
    return newX;
}
function mouseLeave() {
    display.value = 'none';
}
function mouseOver(e: MouseEvent) {
    display.value = 'inline';
    left.value = normalizePreviewX(e);
    top.value = normalizePreviewY(e);
}
function mouseMove(e: MouseEvent) {
    left.value = normalizePreviewX(e);
    top.value = normalizePreviewY(e);
}
</script>

<template>
    <div>
        <img :style="style" class="card-preview"
            :src="`https://api.scryfall.com/cards/${card.image}?format=image&version=normal&face=${face}`"
            ref="cardImage" />
        <a class="card-line" :href="`https://scryfall.com/card/${card.image}`" target="_blank" rel="noreferrer"
            @mouseleave="mouseLeave" @mouseover="mouseOver" @mousemove="mouseMove">
            {{ card.count }} {{ card.name }}</a>
    </div>
</template>

  
<style scoped>
.card-preview {
    display: none;
    background-size: 224px auto;
    height: 310px;
    position: absolute;
    z-index: 2;
    border: 3px solid white;
    border-radius: 5px;
    pointer-events: none;
}

.card-line {
    margin-top: 0;
    margin-bottom: 0;
    color: black;
    text-decoration: none;
}
</style>
  