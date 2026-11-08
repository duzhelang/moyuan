import { ref, watch } from 'vue'

const DEMO_KEY = 'moyuan_admin_demo'

const readDemo = (): boolean => {
  try {
    return localStorage.getItem(DEMO_KEY) === '1'
  } catch {
    return false
  }
}

/** 模块级单例：控制台、用户分析等页面共享同一个演示开关 */
const demoMode = ref(readDemo())

watch(demoMode, (value) => {
  try {
    localStorage.setItem(DEMO_KEY, value ? '1' : '0')
  } catch {
    // 隐私模式下不可写，仅本次会话生效
  }
})

export const useAdminDemo = () => {
  const toggleDemo = () => {
    demoMode.value = !demoMode.value
  }

  const setDemo = (value: boolean) => {
    demoMode.value = value
  }

  return { demoMode, toggleDemo, setDemo }
}
