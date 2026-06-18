<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'

interface Props {
  visible: boolean
  message?: string
  countdown?: number
}

const props = withDefaults(defineProps<Props>(), {
  message: '此功能需要登录后才能使用',
  countdown: 3
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'cancel'): void
}>()

const router = useRouter()
const remainingSeconds = ref(props.countdown)
let timer: number | null = null

const startCountdown = () => {
  remainingSeconds.value = props.countdown
  timer = window.setInterval(() => {
    remainingSeconds.value--
    if (remainingSeconds.value <= 0) {
      stopCountdown()
      goToLogin()
    }
  }, 1000)
}

const stopCountdown = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

const goToLogin = () => {
  stopCountdown()
  emit('update:visible', false)
  router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
}

const handleCancel = () => {
  stopCountdown()
  emit('update:visible', false)
  emit('cancel')
}

watch(() => props.visible, (newVal) => {
  if (newVal) {
    startCountdown()
  } else {
    stopCountdown()
  }
})

onMounted(() => {
  if (props.visible) {
    startCountdown()
  }
})

onUnmounted(() => {
  stopCountdown()
})
</script>

<template>
  <Teleport to="body">
    <Transition name="login-prompt">
      <div v-if="visible" class="login-prompt-overlay" @click.self="handleCancel">
        <div class="login-prompt-dialog">
          <div class="login-prompt-icon">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 15V17M6 7V6C6 4.34315 7.34315 3 9 3H15C16.6569 3 18 4.34315 18 6V7M6 7H18C19.1046 7 20 7.89543 20 9V19C20 20.1046 19.1046 21 18 21H6C4.89543 21 4 20.1046 4 19V9C4 7.89543 4.89543 7 6 7Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="login-prompt-content">
            <h3 class="login-prompt-title">需要登录</h3>
            <p class="login-prompt-message">{{ message }}</p>
            <p class="login-prompt-countdown">
              {{ remainingSeconds }} 秒后自动跳转到登录页
            </p>
          </div>
          <div class="login-prompt-actions">
            <button class="login-prompt-btn login-prompt-btn--cancel" @click="handleCancel">
              取消
            </button>
            <button class="login-prompt-btn login-prompt-btn--confirm" @click="goToLogin">
              立即登录
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.login-prompt-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(4px);
}

.login-prompt-dialog {
  background: white;
  border-radius: 16px;
  padding: 32px;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  text-align: center;
  animation: dialogEnter 0.3s ease-out;
}

@keyframes dialogEnter {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.login-prompt-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #8B4513, #D2691E);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.login-prompt-icon svg {
  width: 32px;
  height: 32px;
}

.login-prompt-content {
  margin-bottom: 24px;
}

.login-prompt-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px;
}

.login-prompt-message {
  font-size: 14px;
  color: #666;
  margin: 0 0 16px;
  line-height: 1.5;
}

.login-prompt-countdown {
  font-size: 13px;
  color: #8B4513;
  margin: 0;
  font-weight: 500;
}

.login-prompt-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.login-prompt-btn {
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  min-width: 100px;
}

.login-prompt-btn--cancel {
  background: #f5f5f5;
  color: #666;
}

.login-prompt-btn--cancel:hover {
  background: #e8e8e8;
  color: #333;
}

.login-prompt-btn--confirm {
  background: linear-gradient(135deg, #8B4513, #D2691E);
  color: white;
}

.login-prompt-btn--confirm:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(139, 69, 19, 0.3);
}

/* 过渡动画 */
.login-prompt-enter-active {
  transition: opacity 0.3s ease;
}

.login-prompt-leave-active {
  transition: opacity 0.2s ease;
}

.login-prompt-enter-from,
.login-prompt-leave-to {
  opacity: 0;
}
</style>
