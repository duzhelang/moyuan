<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAiChat } from '@/composables/useAiChat'
import type { AiSkill } from '@/configs/ai-chat/skills'
import RoleSidebar from '@/components/ai-chat/RoleSidebar.vue'
import ChatStream from '@/components/ai-chat/ChatStream.vue'
import SkillChips from '@/components/ai-chat/SkillChips.vue'
import ChatInput from '@/components/ai-chat/ChatInput.vue'

const route = useRoute()
const router = useRouter()

const {
  personas,
  roleId,
  persona,
  skills,
  skillId,
  activeSkill,
  messages,
  input,
  loading,
  feihuaKeyword,
  switchRole,
  newSession,
  toggleSkill,
  clearSkill,
  send
} = useAiChat()

/** 空态推荐能力：按角色默认能力取，取不到则跳过 */
const recommendedSkills = computed<AiSkill[]>(() =>
  persona.value.defaultSkillIds
    .map((id) => skills.value.find((s) => s.id === id))
    .filter((s): s is AiSkill => !!s)
)

/** 输入框占位：选中能力时用能力的提示语；飞花令随对局阶段切换 */
const placeholder = computed(() => {
  const skill = activeSkill.value
  if (skill?.id === 'feihua') {
    return feihuaKeyword.value
      ? `输入含「${feihuaKeyword.value}」的诗句`
      : '输入一个汉字作为令字，如：花'
  }
  return skill?.placeholder ?? '输入你的问题，Enter 发送 / Shift+Enter 换行'
})

const handlePickSkill = (id: string) => {
  toggleSkill(id)
}

/** 飞花令 / 接龙在对话内轻量对局，完整计时闯关可跳独立游戏页 */
const gamePath = computed(() => {
  if (activeSkill.value?.id === 'feihua') return '/game/feihua'
  if (activeSkill.value?.id === 'chain') return '/game/chain'
  return ''
})

const gameLabel = computed(() => {
  if (activeSkill.value?.id === 'feihua') return '飞花令完整对局'
  if (activeSkill.value?.id === 'chain') return '接龙完整对局'
  return ''
})

const openGame = () => {
  if (gamePath.value) router.push(gamePath.value)
}

// 支持首页跳转携带 ?role=xxx&skill=xxx（预留能力）
onMounted(() => {
  const qRole = typeof route.query.role === 'string' ? route.query.role : ''
  if (qRole && personas.some((p) => p.id === qRole)) switchRole(qRole)
  const qSkill = typeof route.query.skill === 'string' ? route.query.skill : ''
  if (qSkill && skills.value.some((s) => s.id === qSkill)) skillId.value = qSkill
})
</script>

<template>
  <div class="ai-chat-page">
    <div class="ai-chat-header">
      <div class="ai-chat-header-title">
        <span class="ai-chat-page-title">诗宝阁</span>
        <span class="ai-chat-header-divider"></span>
        <span class="ai-chat-header-icon">{{ persona.icon }}</span>
        <span class="ai-chat-header-name">{{ persona.name }}</span>
        <span class="ai-chat-header-sub">{{ persona.subtitle }}</span>
      </div>
      <button class="ai-chat-new-btn" @click="newSession">＋ 新建会话</button>
    </div>

    <div class="ai-chat-body">
      <RoleSidebar :personas="personas" :active-id="roleId" @select="switchRole" />

      <div class="ai-chat-main ink-bg ink-bg--4">
        <ChatStream
          :messages="messages"
          :loading="loading"
          :persona="persona"
          :recommended-skills="recommendedSkills"
          @pick-skill="handlePickSkill"
        />

        <div class="ai-chat-footer">
          <SkillChips :skills="skills" :active-id="skillId" @select="toggleSkill" />
          <ChatInput
            v-model="input"
            :loading="loading"
            :placeholder="placeholder"
            :active-skill="activeSkill"
            :game-path="gamePath"
            :game-label="gameLabel"
            @send="send"
            @clear-skill="clearSkill"
            @open-game="openGame"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.ai-chat-page {
  /* 顶部导航为 fixed 定位（55px），需留出同等高度，避免页头被遮挡、按钮无法点击 */
  margin-top: 55px;
  height: calc(100vh - 55px);
  min-height: 520px;
  display: flex;
  flex-direction: column;
  background: #f2e6d3;
  border-top: 1px solid #d4c5a9;
}

.ai-chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 20px;
  border-bottom: 1px solid #d4c5a9;
  /* 改用宣纸渐变色，避免与固定导航的山水底图在同一视口内重复堆叠 */
  background: linear-gradient(180deg, #fdf8f0 0%, #f3e7d3 100%);
}

.ai-chat-header-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  min-width: 0;
}

.ai-chat-page-title {
  flex-shrink: 0;
  font-family: 'KaiTi', 'STKaiti', 'Noto Serif SC', serif;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #6b4c2a;
}

.ai-chat-header-divider {
  flex-shrink: 0;
  align-self: center;
  width: 1px;
  height: 18px;
  background: rgba(107, 76, 42, 0.3);
}

.ai-chat-header-icon {
  font-size: 22px;
  line-height: 1;
  align-self: center;
}

.ai-chat-header-name {
  font-family: 'KaiTi', 'STKaiti', 'Noto Serif SC', serif;
  font-size: 20px;
  font-weight: 700;
  color: #8b5a2b;
}

.ai-chat-header-sub {
  font-size: 13px;
  color: #9a8367;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ai-chat-new-btn {
  flex-shrink: 0;
  padding: 7px 16px;
  border: 1px solid rgba(139, 69, 19, 0.45);
  border-radius: 999px;
  background: rgba(139, 69, 19, 0.08);
  color: #7a4a20;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.ai-chat-new-btn:hover {
  background: #8b4513;
  border-color: #8b4513;
  color: #fff;
}

.ai-chat-body {
  flex: 1;
  min-height: 0;
  display: flex;
}

.ai-chat-main {
  position: relative;
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

/* 右侧配景：取松山图中的远山云雾，只在右缘渐显，与左侧竹影形成「竹 + 远岫」搭配 */
.ai-chat-main::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: url('/img/bg_ink_5.jpg') center top / cover no-repeat;
  opacity: 0.5;
  -webkit-mask-image: linear-gradient(to left, #000 0%, rgba(0, 0, 0, 0.35) 32%, transparent 58%);
  mask-image: linear-gradient(to left, #000 0%, rgba(0, 0, 0, 0.35) 32%, transparent 58%);
}

/* 对话流与输入区置于底纹之上 */
.ai-chat-main > * {
  position: relative;
  z-index: 1;
}

.ai-chat-footer {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 20px 16px;
  border-top: 1px solid #d4c5a9;
  background: rgba(255, 252, 247, 0.85);
}

@media (max-width: 768px) {
  .ai-chat-page {
    height: auto;
    min-height: calc(100vh - 55px);
  }

  .ai-chat-body {
    flex-direction: column;
  }

  .ai-chat-main {
    min-height: 60vh;
  }

  .ai-chat-header-sub {
    display: none;
  }

  .ai-chat-footer {
    padding: 10px 14px 14px;
  }
}
</style>
