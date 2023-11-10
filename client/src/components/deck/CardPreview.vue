<script setup lang="ts">
import { computed, mergeProps, ref } from 'vue';

interface Card {
    count: number;
    name: string;
    type: string;
    image: string;
    backImage: string | null;
    id: string;
}

const props = defineProps<{ card: Card }>();

const display = ref("none");
const left = ref(0);
const top = ref(0);
const cardImage = ref<HTMLInputElement | null>(null);

const dfc = computed(() => props.card.backImage !== null);
const frontName = computed(() => props.card.name.split(' // ')[0]);
const backName = computed(() => props.card.name.split(' // ')[1]);
const backFace = ref(false);

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
    backFace.value = false
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
function mouseOverDfc(e: MouseEvent) {
    backFace.value = true;
    mouseOver(e);
}
</script>

<template>
    <div>
        <img :style="style" class="card-preview" :src="backFace ? card.backImage! : card.image" ref="cardImage" />
        <a class="card-line" :href="`https://scryfall.com/card/${card.id}`" target="_blank" rel="noreferrer"
            @mouseleave="mouseLeave" @mouseover="mouseOver" @mousemove="mouseMove">
            {{ card.count }} {{ frontName }}</a>
        <a v-if="dfc" class="card-line" :href="`https://scryfall.com/card/${card.id}?back`" target="_blank" rel="noreferrer"
            @mouseleave="mouseLeave" @mouseover="mouseOverDfc" @mousemove="mouseMove"> // {{ backName }}</a>
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
  