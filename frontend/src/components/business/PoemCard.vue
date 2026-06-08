<script setup lang="ts">
import { computed } from 'vue'
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

const formattedContent = computed(() => {
  if (!props.poem.content) return []
  const content = props.poem.content
  const lines = content.split(/[。！？；\n]+/).filter(line => line.trim())
  return lines.slice(0, 4)
})
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
      <div class="poem-verses">
        <p v-for="(line, index) in formattedContent" :key="index" class="verse-line">
          {{ line.trim() }}
        </p>
      </div>
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
    <div class="card-decoration"></div>
  </div>
</template>

<style scoped lang="scss">
.poem-card {
  @include card;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  gap: $spacing-lg;
  position: relative;
  border-left: 3px solid $primary-color;

  &:hover {
    border-left-color: darken($primary-color, 10%);
  }
}

.poem-image {
  width: 180px;
  height: 140px;
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
  top: $spacing-xs;
  right: $spacing-xs;
  background: linear-gradient(135deg, #67C23A, #85ce61);
  color: white;
  padding: 2px 6px;
  border-radius: $border-radius-sm;
  font-size: 10px;
  display: flex;
  align-items: center;
  gap: 2px;
  font-weight: bold;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  z-index: 2;
}

.poem-card:hover .poem-image img {
  transform: scale(1.08);
}

.poem-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding-right: $spacing-xl;
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
  font-family: $font-family-title;
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

.poem-verses {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: $spacing-xs;
  min-height: 60px;
}

.verse-line {
  font-size: $font-size-base;
  color: $text-color-secondary;
  line-height: 1.6;
  margin: 0;
  font-family: $font-family-base;
  white-space: normal;
  word-break: break-all;
}

.poem-meta {
  display: flex;
  gap: $spacing-md;
  margin-top: $spacing-sm;
  padding-top: $spacing-sm;
  border-top: 1px dashed $border-color-light;
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

.card-decoration {
  position: absolute;
  top: 0;
  right: 0;
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, transparent 50%, rgba($primary-color, 0.05) 50%);
  pointer-events: none;
}
</style>