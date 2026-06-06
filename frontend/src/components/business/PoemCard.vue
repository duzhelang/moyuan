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
      <div class="original-badge" v-if="poem.isOriginal">
        <el-icon><EditPen /></el-icon>
        原创
      </div>
    </div>
    <div class="poem-info">
      <div class="poem-title-row">
        <h3 class="poem-title">{{ poem.title }}</h3>
        <div class="poem-tags">
          <span class="tag original-tag" v-if="poem.isOriginal">原创</span>
        </div>
      </div>
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
        <span class="meta-item rating-meta" v-if="poem.averageScore">
          <el-icon><Trophy /></el-icon>
          {{ poem.averageScore.toFixed(1) }}
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
  position: relative;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform $transition-slow;
  }
}

.original-badge {
  position: absolute;
  top: $spacing-sm;
  right: $spacing-sm;
  background: linear-gradient(135deg, #67C23A, #85ce61);
  color: white;
  padding: $spacing-xs $spacing-sm;
  border-radius: $border-radius-sm;
  font-size: $font-size-xs;
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-weight: bold;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.poem-card:hover .poem-image img {
  transform: scale(1.1);
}

.poem-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.poem-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: $spacing-xs;
  gap: $spacing-sm;
}

.poem-title {
  font-size: $font-size-xl;
  color: $text-color;
  flex: 1;
  min-width: 0;
  @include text-ellipsis;
}

.poem-tags {
  display: flex;
  gap: $spacing-xs;
  flex-shrink: 0;
}

.tag {
  padding: $spacing-xs $spacing-sm;
  border-radius: $border-radius-sm;
  font-size: $font-size-xs;
  font-weight: 500;
  white-space: nowrap;
}

.original-tag {
  background-color: rgba($success-color, 0.1);
  color: $success-color;
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

.rating-meta {
  color: $warning-color;
  font-weight: 600;
}
</style>