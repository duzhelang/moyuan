<script setup lang="ts">
import type { AiPersona } from '@/configs/ai-chat/personas'

defineProps<{
  personas: AiPersona[]
  activeId: string
}>()

const emit = defineEmits<{
  select: [id: string]
}>()

const roleStyle = (p: AiPersona) => ({ '--role-color': p.color })
</script>

<template>
  <aside class="role-sidebar">
    <div class="role-sidebar-title">角色</div>
    <ul class="role-list">
      <li
        v-for="p in personas"
        :key="p.id"
        class="role-item"
        :class="{ active: p.id === activeId }"
        :style="roleStyle(p)"
        @click="emit('select', p.id)"
      >
        <span class="role-icon">{{ p.icon }}</span>
        <span class="role-meta">
          <span class="role-name">{{ p.name }}</span>
          <span class="role-subtitle">{{ p.subtitle }}</span>
        </span>
      </li>
    </ul>
  </aside>
</template>

<style scoped lang="scss">
.role-sidebar {
  width: 210px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #d4c5a9;
  background: rgba(255, 252, 247, 0.72);
  overflow-y: auto;
}

.role-sidebar-title {
  padding: 14px 18px 8px;
  font-size: 13px;
  letter-spacing: 2px;
  color: #8b6914;
  font-weight: 600;
}

.role-list {
  list-style: none;
  margin: 0;
  padding: 0 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.role-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.role-item:hover {
  background: rgba(139, 69, 19, 0.06);
}

.role-item.active {
  background: rgba(139, 69, 19, 0.08);
  border-color: var(--role-color, #8b4513);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.6);
}

.role-icon {
  font-size: 20px;
  line-height: 1;
  flex-shrink: 0;
}

.role-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.role-name {
  font-size: 14px;
  font-weight: 600;
  color: #4a3520;
  white-space: nowrap;
}

.role-item.active .role-name {
  color: var(--role-color, #8b4513);
}

.role-subtitle {
  font-size: 12px;
  color: #8a7a63;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 768px) {
  .role-sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #d4c5a9;
  }

  .role-list {
    flex-direction: row;
    overflow-x: auto;
    padding-bottom: 8px;
  }

  .role-item {
    flex-shrink: 0;
  }

  .role-subtitle {
    display: none;
  }
}
</style>
