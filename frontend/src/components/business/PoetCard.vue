<script setup lang="ts">
import { useRouter } from 'vue-router'

interface PoetData {
  id: number
  poetId?: number | null
  name: string
  dynasty?: string
  description: string
  biography: string
  imageUrl: string
}

const props = defineProps<{
  poet: PoetData
}>()

const router = useRouter()

const handleClick = () => {
  if (props.poet.poetId) {
    router.push(`/poet/${props.poet.poetId}`)
  }
}
</script>

<template>
  <div class="poet-card" @click="handleClick">
    <img :src="poet.imageUrl" :alt="poet.name" class="poet-img">
    <div class="poet-info">
      <span class="poet-name">{{ poet.name }}</span>
      <p class="poet-desc">{{ poet.description }}</p>
    </div>
  </div>
</template>

<style scoped>
.poet-card {
  width: 250px;
  height: 310px;
  cursor: pointer;
  margin: 0 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background-color: rgba(227, 196, 165, 0.5);
  border: 1px solid rgba(227, 196, 165, 0.8);
  border-radius: 10px;
}

.poet-img {
  width: 250px;
  height: 240px;
  display: block;
  object-fit: cover;
  flex-shrink: 0;
}

.poet-info {
  flex: 1;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 0;
}

.poet-name {
  display: block;
  text-align: left;
  font-size: 16px;
  font-weight: 500;
}

.poet-desc {
  margin: 0;
  font-size: 14px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}
</style>