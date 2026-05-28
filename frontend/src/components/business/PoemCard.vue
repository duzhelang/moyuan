<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { Poem } from '@/types/model'

const props = defineProps<{
  poem: Poem
  showAuthor?: boolean
  showImage?: boolean
}>()

const router = useRouter()

const goToDetail = () => {
  router.push(`/poem/${props.poem.id}`)
}
</script>

<template>
  <div class="poem-card" @click="goToDetail">
    <div class="poem-image" v-if="showImage">
      <img src="/images/h6.jpg" :alt="poem.title" />
    </div>
    <div class="poem-info">
      <h3 class="poem-title">{{ poem.title }}</h3>
      <p class="poem-author" v-if="showAuthor">
        <span v-if="poem.dynastyName">{{ poem.dynastyName }}：</span>
        <span v-if="poem.poetName">{{ poem.poetName }}</span>
      </p>
      <p class="poem-content">{{ poem.content }}</p>
      <div class="poem-meta">
        <span class="meta-item">
          <el-icon><View /></el-icon>
          {{ poem.viewCount }}
        </span>
        <span class="meta-item">
          <el-icon><Star /></el-icon>
          {{ poem.likeCount }}
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.poem-card {
  @include card;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  gap: $spacing-lg;
}

.poem-image {
  width: 200px;
  height: 150px;
  flex-shrink: 0;
  border-radius: $border-radius-sm;
  overflow: hidden;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform $transition-slow;
  }
}

.poem-card:hover .poem-image img {
  transform: scale(1.1);
}

.poem-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.poem-title {
  font-size: $font-size-xl;
  color: $text-color;
  margin-bottom: $spacing-xs;
}

.poem-author {
  font-size: $font-size-sm;
  color: $text-color-secondary;
  margin-bottom: $spacing-sm;
}

.poem-content {
  font-size: $font-size-base;
  color: $text-color-secondary;
  line-height: $line-height-loose;
  @include text-clamp(2);
  flex: 1;
}

.poem-meta {
  display: flex;
  gap: $spacing-md;
  margin-top: $spacing-sm;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $text-color-light;
}
</style>