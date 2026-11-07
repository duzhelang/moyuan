import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import router from './router'
import App from './App.vue'
import '@/assets/styles/original.css'
import '@/assets/styles/global.scss'
import '@/assets/styles/admin-themes.scss'
import '@fontsource/noto-serif-sc/400.css'
import '@fontsource/noto-serif-sc/600.css'
import '@fontsource/noto-serif-sc/700.css'
import { permission, permissionOr, permissionAnd } from './directives/permission'
import { initAdminTheme } from '@/composables/useAdminTheme'

initAdminTheme()

const app = createApp(App)
const pinia = createPinia()

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.directive('permission', permission)
app.directive('permission-or', permissionOr)
app.directive('permission-and', permissionAnd)

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')