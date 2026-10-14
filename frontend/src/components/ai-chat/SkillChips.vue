<script setup lang="ts">
import type { AiSkill } from '@/configs/ai-chat/skills'

defineProps<{
  skills: AiSkill[]
  activeId: string | null
}>()

const emit = defineEmits<{
  select: [id: string]
}>()
</script>

<template>
  <div class="skill-chips">
    <span class="skill-chips-label">能力</span>
    <template v-if="skills.length">
      <button
        v-for="s in skills"
        :key="s.id"
        class="skill-chip"
        :class="{ active: s.id === activeId }"
        :title="s.desc"
        @click="emit('select', s.id)"
      >
        <span class="skill-chip-icon">{{ s.icon }}</span>{{ s.name }}
      </button>
    </template>
    <span v-else class="skill-chips-empty">当前角色无需特定能力，直接提问即可</span>
  </div>
</template>

<style scoped lang="scss">
.skill-chips {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 2px 0;
}

.skill-chips-label {
  font-size: 12px;
  color: #8b6914;
  font-weight: 600;
  letter-spacing: 1px;
  margin-right: 2px;
}

.skill-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border: 1px solid #d4c5a9;
  border-radius: 999px;
  background: #fffcf7;
  color: #6b4513;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.skill-chip:hover {
  border-color: #b34a31;
  color: #b34a31;
  background: #fdf6ee;
}

.skill-chip.active {
  border-color: #b34a31;
  background: linear-gradient(135deg, #a0522d, #b34a31);
  color: #fff;
}

.skill-chip-icon {
  font-size: 14px;
  line-height: 1;
}

.skill-chips-empty {
  font-size: 12px;
  color: #a09585;
}
</style>
