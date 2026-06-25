<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createRepairOrder } from '@/api/modules/repair'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  title: '',
  description: '',
  category: '',
  priority: 2,
  images: ''
})

const categoryOptions = [
  { value: 'system', label: '系统故障' },
  { value: 'function', label: '功能异常' },
  { value: 'ui', label: '界面问题' },
  { value: 'data', label: '数据问题' },
  { value: 'other', label: '其他' }
]

const priorityOptions = [
  { value: 1, label: '低' },
  { value: 2, label: '中' },
  { value: 3, label: '高' },
  { value: 4, label: '紧急' }
]

const rules: FormRules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入问题描述', trigger: 'blur' },
    { min: 10, max: 2000, message: '描述长度在 10 到 2000 个字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择问题类别', trigger: 'change' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await createRepairOrder({
          title: form.title,
          description: form.description,
          category: form.category,
          priority: form.priority,
          images: form.images || undefined
        })
        ElMessage.success('报修提交成功')
        router.push('/repair')
      } catch (error) {
        console.error('提交报修失败', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleCancel = () => {
  router.push('/repair')
}

const goToRepairList = () => {
  router.push('/repair')
}

const goToContact = () => {
  router.push('/contact')
}

const goBack = () => {
  router.go(-1)
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    userStore.logout()
    router.push('/')
    ElMessage.success('已退出登录')
  } catch {}
}
</script>

<template>
  <div class="create-container">
    <div class="create-header">
      <div class="header-left">
        <el-button @click="goBack">
          <el-icon><Back /></el-icon>
          <span>上一页</span>
        </el-button>
        <h2 class="create-title">提交报修</h2>
      </div>
      <div class="header-right">
        <el-button @click="goToRepairList">
          <el-icon><List /></el-icon>
          <span>报修列表</span>
        </el-button>
        <el-button @click="goToContact">
          <el-icon><ChatDotRound /></el-icon>
          <span>联系团队</span>
        </el-button>
        <el-button type="danger" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          <span>退出</span>
        </el-button>
      </div>
    </div>

    <el-card class="form-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="top"
      >
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入问题标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="问题描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请详细描述您遇到的问题"
            :rows="6"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="问题类别" prop="category">
              <el-select v-model="form.category" placeholder="请选择问题类别" style="width: 100%">
                <el-option
                  v-for="item in categoryOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级">
              <el-radio-group v-model="form.priority">
                <el-radio-button
                  v-for="item in priorityOptions"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="相关图片（可选）">
          <el-input
            v-model="form.images"
            placeholder="请输入图片URL，多个用逗号分隔"
          />
          <div class="form-tip">支持粘贴图片链接，多个链接用逗号分隔</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交报修</el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.create-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.create-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.create-title {
  font-size: 24px;
  color: #333;
  margin: 0;
}

.form-card {
  border-radius: 8px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>