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
          <span class="post-time">{{ post.createTime }}</span>
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
  border-radius: $border-radius-lg;
  padding: $spacing-lg $spacing-xl;
  border: 1px solid transparent;
  transition: all $transition-base;

  &:hover {
    transform: translateY(-2px);
    box-shadow: $box-shadow-lg;
    border-color: $border-color-light;

    .post-title {
      color: $primary-color;
    }
  }
}

.post-header {
  display: flex;
  gap: $spacing-md;
  margin-bottom: $spacing-md;

  .el-avatar {
    flex-shrink: 0;
    border: 2px solid $background-color;
    box-shadow: $box-shadow;
  }
}

.post-meta {
  flex: 1;
  min-width: 0;
}

.post-title {
  font-size: $font-size-xl;
  color: $text-color;
  margin-bottom: $spacing-xs;
  font-weight: 600;
  transition: color $transition-fast;
  @include text-ellipsis;
}

.post-info {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.post-author {
  font-size: $font-size-sm;
  color: $primary-color;
  font-weight: 600;
}

.post-time {
  font-size: $font-size-sm;
  color: $text-color-light;

  &::before {
    content: '·';
    margin-right: $spacing-sm;
    color: $border-color;
  }
}

.post-content {
  margin-bottom: $spacing-md;
  padding: 0 $spacing-xs;
  
  p {
    font-size: $font-size-base;
    color: $text-color-secondary;
    line-height: $line-height-loose;
    @include text-clamp(2);
  }
}

.post-footer {
  display: flex;
  gap: $spacing-xl;
  padding-top: $spacing-md;
  border-top: 1px solid $border-color-light;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: $font-size-sm;
  color: $text-color-light;
  transition: color $transition-fast;

  .el-icon {
    font-size: 16px;
  }

  &:hover {
    color: $primary-color;
  }
}

@include responsive(md) {
  .forum-post {
    padding: $spacing-md;
  }

  .post-footer {
    gap: $spacing-md;
    flex-wrap: wrap;
  }
}

@include responsive(sm) {
  .post-header {
    gap: $spacing-sm;
  }

  .post-title {
    font-size: $font-size-lg;
  }
}
</style>