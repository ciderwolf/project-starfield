<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router';

withDefaults(defineProps<{
  title: string
  image: string
  imageAlt?: string
  to: string | RouteLocationRaw
  imageFit?: 'fill' | 'contain'
}>(), {
  imageFit: 'fill'
});
</script>

<template>
  <router-link :to="to" class="item-card">
    <img :src="image" :alt="imageAlt ?? `${title} Thumbnail`" class="item-card-image" :class="`fit-${imageFit}`" />
    <h3 class="item-card-title">{{ title }}</h3>
    <div v-if="$slots.actions" class="item-card-actions">
      <slot name="actions" />
    </div>
  </router-link>
</template>

<style scoped>
.item-card {
  display: flex;
  flex-direction: column;
  text-decoration: none;
  text-align: center;
  color: #000;
  margin: 0.5em;
  box-shadow: 0 0 5px 0 rgba(0, 0, 0, 0.5);
  width: 250px;
  background: white;
  height: min-content;
}

.item-card:hover {
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.5);
}

.item-card-image {
  height: 182.5px;
}

.item-card-image.fit-fill {
  object-fit: fill;
}

.item-card-image.fit-contain {
  object-fit: contain;
  padding: 1em;
}

.item-card-title {
  margin: 0.75em;
}

.item-card-actions {
  display: none;
  margin: 0 0.5em 0.5em 0.5em;
}

.item-card:hover .item-card-actions {
  display: block;
}
</style>
