<script setup lang="ts">
// 推荐结果纯展示面板：骨架 / 卡片 / 空态 / 错误态；不取数、不做策略判断、不弹全局 toast（附录 A.3.2）
import type { RecommendItem } from '@/api/modules/recommend'

const props = withDefaults(
  defineProps<{
    items: RecommendItem[]
    loading: boolean
    error: Error | null
    /** 当前是否为规则策略：决定空态文案与是否显示「切回规则」 */
    isRuleStrategy?: boolean
    topK?: number
  }>(),
  {
    isRuleStrategy: true,
    topK: 6
  }
)

const emit = defineEmits<{
  (e: 'retry'): void
  (e: 'switch-rule'): void
  (e: 'open', poem: RecommendItem): void
}>()

const skeletonCount = Math.min(6, Math.max(3, props.topK))
</script>

<template>
  <div class="rp_root">
    <!-- 加载：3-6 个卡片骨架屏（宣纸底微光） -->
    <template v-if="loading">
      <div v-for="n in skeletonCount" :key="'skeleton-' + n" class="poem_card poem_recommend_card rp_skeleton">
        <div class="rp_skeleton_line rp_title" />
        <div class="rp_skeleton_line rp_meta" />
        <div v-for="m in 2" :key="m" class="rp_skeleton_line rp_content" />
      </div>
    </template>

    <!-- 错误：插画 + 重新加载，不整页报错 -->
    <div v-else-if="error" class="rp_status">
      <div class="rp_status_icon">&#128273;</div>
      <p class="rp_status_text">推荐加载失败，请稍后重试</p>
      <button type="button" class="rp_action_btn" @click="emit('retry')">重新加载</button>
    </div>

    <!-- 空态：规则由服务端热门兜底，仅模型策略出现「试试规则推荐」 -->
    <div v-else-if="items.length === 0" class="rp_status">
      <div class="rp_status_icon">&#127344;</div>
      <p class="rp_status_text">
        {{ isRuleStrategy ? '暂无推荐，多浏览诗词会更精准哦~' : '该策略暂无推荐，试试规则推荐' }}
      </p>
      <button v-if="!isRuleStrategy" type="button" class="rp_action_btn" @click="emit('switch-rule')">
        切回规则推荐
      </button>
    </div>

    <!-- 结果卡片：复用首页诗词卡片结构，点击跳 /poem/:id -->
    <template v-else>
      <div
        v-for="poem in items"
        :key="'recommend-' + poem.id"
        class="poem_card poem_recommend_card"
        @click.stop="$emit('open', poem)"
      >
        <div class="poem_card_body">
          <div class="poem_title">{{ poem.title }}</div>
          <div class="poem_meta">
            <span v-if="poem.dynasty">【{{ poem.dynasty }}】</span>
            <span v-if="poem.author">{{ poem.author }}</span>
          </div>
          <div class="poem_lines">
            <div v-for="(line, li) in poem.content.split('\n')" :key="li" class="poem_line">{{ line }}</div>
          </div>
          <div v-if="poem.recommendReason" class="poem_recommend_reason">
            <span class="recommend_icon">&#128161;</span> {{ poem.recommendReason }}
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.rp_root {
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
  gap: 16px;
  width: 100%;
}

/* 卡片外观与 AncientPoemSelection 的推荐卡片保持一致（scoped 样式需在本组件内声明） */
.poem_card {
  display: flex;
  align-items: stretch;
  background: linear-gradient(135deg, #fffbf5 0%, #fff8ee 50%, #fffbf5 100%);
  border-radius: 14px;
  border: 1px solid rgba(139, 69, 19, 0.15);
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
  position: relative;
  z-index: 1;
  box-shadow: 0 2px 12px rgba(139, 69, 19, 0.08);
  cursor: pointer;
  flex-direction: column;
  animation: rp_fade_in 0.2s ease;
}

.poem_card:hover {
  box-shadow: 0 12px 40px rgba(139, 69, 19, 0.18), 0 4px 12px rgba(0, 0, 0, 0.06);
  transform: translateY(-4px);
  background: linear-gradient(135deg, #fff 0%, #fff9f0 50%, #fff 100%);
  z-index: 10;
}

.poem_card_body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px;
  text-align: center;
  position: relative;
}

.poem_card .poem_title {
  font-size: 20px;
  font-weight: 700;
  color: #8b4513;
  margin-bottom: 8px;
  letter-spacing: 3px;
  text-shadow: 0 1px 2px rgba(139, 69, 19, 0.1);
}

.poem_meta {
  font-size: 13px;
  color: #a0724e;
  margin-bottom: 16px;
  letter-spacing: 2px;
}

.poem_lines {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.poem_line {
  font-size: 16px;
  color: #333;
  letter-spacing: 2px;
  line-height: 2;
  font-family: "Noto Serif SC", "Source Han Serif SC", "STSong", "SimSun", serif;
}

.poem_recommend_reason {
  margin-top: 8px;
  padding: 6px 10px;
  background: linear-gradient(135deg, rgba(138, 90, 43, 0.08), rgba(210, 105, 30, 0.08));
  border-radius: 6px;
  font-size: 12px;
  color: #7a5c37;
}

.recommend_icon {
  margin-right: 4px;
}

/* 骨架屏：宣纸底 1.4s 微光扫过，避免纯灰 */
.rp_skeleton {
  cursor: default;
  align-items: center;
  min-height: 140px;
}

.rp_skeleton:hover {
  transform: none;
  box-shadow: 0 2px 12px rgba(139, 69, 19, 0.08);
}

.rp_skeleton_line {
  border-radius: 6px;
  background: linear-gradient(90deg, #f5efe2 25%, #faf6ec 50%, #f5efe2 75%);
  background-size: 200% 100%;
  animation: rp_shimmer 1.4s ease infinite;
}

.rp_title {
  width: 40%;
  height: 18px;
  margin-bottom: 12px;
}

.rp_meta {
  width: 25%;
  height: 12px;
  margin-bottom: 16px;
}

.rp_content {
  width: 70%;
  height: 14px;
  margin-bottom: 8px;
}

/* 空态 / 错误态 */
.rp_status {
  width: 100%;
  text-align: center;
  padding: 40px 20px;
  color: #999;
  font-size: 14px;
}

.rp_status_icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.rp_status_text {
  margin: 0 0 12px;
}

.rp_action_btn {
  padding: 6px 18px;
  background: #8a5a2b;
  color: #f7efe0;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.rp_action_btn:hover {
  background: #6f4820;
  box-shadow: 0 2px 6px rgba(138, 90, 43, 0.25);
}

@keyframes rp_shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

@keyframes rp_fade_in {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
