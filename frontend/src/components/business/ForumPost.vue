<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { ForumPost as ForumPostType } from '@/types/model'

const props = defineProps<{
  post: ForumPostType
}>()

const router = useRouter()

const goToDetail = () => {
  router.push(`/forum/${props.post.id}`)
}
</script>

<template>
  <div class="forum-post" @click="goToDetail">
    <div class="post-header">
      <el-avatar :src="post.avatar" :size="48">
        {{ post.username?.charAt(0)?.toUpperCase() }}
      </el-avatar>
      <div class="post-meta">
        <h3 class="post-title">{{ post.title }}</h3>
        <div class="post-info">
          <span class="post-author">{{ post.username }}</span>
          <span class="post-time">{{ post.createdAt }}</span>
        </div>
      </div>
    </div>
    
    <div class="post-content">
      <p>{{ post.content }}</p>
    </div>
    
    <div class="post-footer">
      <span class="meta-item">
        <el-icon><View /></el-icon>
        {{ post.viewCount }}
      </span>
      <span class="meta-item">
        <el-icon><Star /></el-icon>
        {{ post.likeCount }}
      </span>
      <span class="meta-item">
        <el-icon><ChatDotRound /></el-icon>
        {{ post.commentCount }}
      </span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.forum-post {
  @include card;
  cursor: pointer;
}

.post-header {
  display: flex;
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.post-meta {
  flex: 1;
}

.post-title {
  font-size: $font-size-xl;
  color: $text-color;
  margin-bottom: $spacing-xs;
}

.post-info {
  display: flex;
  gap: $spacing-md;
}

.post-author {
  font-size: $font-size-sm;
  color: $primary-color;
  font-weight: 600;
}

.post-time {
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.post-content {
  margin-bottom: $spacing-md;
  
  p {
    font-size: $font-size-base;
    color: $text-color-secondary;
    line-height: $line-height-loose;
    @include text-clamp(3);
  }
}

.post-footer {
  display: flex;
  gap: $spacing-lg;
  padding-top: $spacing-md;
  border-top: 1px solid $border-color;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-sm;
  color: $text-color-secondary;
}
</style>