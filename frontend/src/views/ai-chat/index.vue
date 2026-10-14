<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAiChat } from '@/composables/useAiChat'
import type { AiSkill } from '@/configs/ai-chat/skills'
import RoleSidebar from '@/components/ai-chat/RoleSidebar.vue'
import ChatStream from '@/components/ai-chat/ChatStream.vue'
import SkillChips from '@/components/ai-chat/SkillChips.vue'
import ChatInput from '@/components/ai-chat/ChatInput.vue'

const route = useRoute()

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

/** 输入框占位：选中能力时用能力的提示语 */
const placeholder = computed(
  () => activeSkill.value?.placeholder ?? '输入你的问题，Enter 发送 / Shift+Enter 换行'
)

const handlePickSkill = (id: string) => {
  toggleSkill(id)
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
        <span class="ai-chat-header-icon">{{ persona.icon }}</span>
        <span class="ai-chat-header-name">{{ persona.name }}</span>
        <span class="ai-chat-header-sub">{{ persona.subtitle }}</span>
      </div>
      <button class="ai-chat-new-btn" @click="newSession">＋ 新建会话</button>
    </div>

    <div class="ai-chat-body">
      <RoleSidebar :personas="personas" :active-id="roleId" @select="switchRole" />

      <div class="ai-chat-main">
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
            @send="send"
            @clear-skill="clearSkill"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.ai-chat-page {
  height: calc(100vh - 76px);
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
  background: url('/img/dt_0_0.jpg');
  background-size: cover;
  background-position: 0 -320px;
}

.ai-chat-header-title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  min-width: 0;
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
  color: #ffe8c4;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.45);
}

.ai-chat-header-sub {
  font-size: 13px;
  color: rgba(255, 236, 210, 0.75);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ai-chat-new-btn {
  flex-shrink: 0;
  padding: 7px 16px;
  border: 1px solid rgba(255, 232, 196, 0.7);
  border-radius: 999px;
  background: rgba(139, 69, 19, 0.35);
  color: #ffe8c4;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.ai-chat-new-btn:hover {
  background: rgba(179, 74, 49, 0.6);
  color: #fff;
}

.ai-chat-body {
  flex: 1;
  min-height: 0;
  display: flex;
}

.ai-chat-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
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
    min-height: calc(100vh - 76px);
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
