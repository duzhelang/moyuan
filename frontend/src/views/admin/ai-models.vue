<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getAllModels,
  createModel,
  updateModel,
  deleteModel as deleteModelApi,
  toggleModel,
  setDefaultModel,
  getProviders,
  testConnection,
  type AiModelConfig
} from '@/api/modules/admin-ai-model'

const models = ref<AiModelConfig[]>([])
const providers = ref<{ value: string; label: string }[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingModel = ref<AiModelConfig | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const defaultForm: AiModelConfig = {
  name: '',
  displayName: '',
  provider: '',
  modelType: 'text',
  apiUrl: '',
  apiKey: '',
  modelId: '',
  visionModelId: '',
  maxTokens: 2048,
  sortOrder: 0
}

const form = ref<AiModelConfig>({ ...defaultForm })

const rules: FormRules = {
  name: [{ required: true, message: '请输入模型标识', trigger: 'blur' }],
  displayName: [{ required: true, message: '请输入显示名称', trigger: 'blur' }],
  provider: [{ required: true, message: '请选择提供商', trigger: 'change' }],
  modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
  apiUrl: [{ required: true, message: '请输入API地址', trigger: 'blur' }],
  apiKey: [{ required: true, message: '请输入API密钥', trigger: 'blur' }],
  modelId: [{ required: true, message: '请输入模型ID', trigger: 'blur' }]
}

const providerLabelMap: Record<string, string> = {
  zhipu: '智谱AI',
  deepseek: 'DeepSeek',
  kimi: 'Kimi',
  nvidia: 'NVIDIA NIM'
}

const typeLabelMap: Record<string, string> = {
  text: '文本',
  vision: '视觉',
  both: '多模态'
}

function getProviderLabel(provider: string) {
  return providerLabelMap[provider] || provider
}

function getTypeLabel(type: string) {
  return typeLabelMap[type] || type
}

const getTypeTag = (type: string) => {
  if (type === 'vision') return 'warning'
  if (type === 'both') return 'success'
  return ''
}

async function loadModels() {
  loading.value = true
  try {
    const res = await getAllModels()
    if (res.data && res.data.code === 200) {
      models.value = res.data.data || []
    }
  } catch (e) {
    console.error('加载模型列表失败', e)
  } finally {
    loading.value = false
  }
}

async function loadProviders() {
  try {
    const res = await getProviders()
    if (res.data && res.data.code === 200) {
      providers.value = res.data.data || []
    }
  } catch (e) {
    console.error('加载提供商列表失败', e)
  }
}

function openDialog(model?: AiModelConfig) {
  if (model) {
    editingModel.value = model
    form.value = { ...model }
  } else {
    editingModel.value = null
    form.value = { ...defaultForm }
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    if (editingModel.value?.id) {
      await updateModel(editingModel.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createModel(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadModels()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(model: AiModelConfig) {
  try {
    await ElMessageBox.confirm(`确定删除模型 "${model.displayName}" 吗？`, '确认删除', { type: 'warning' })
    await deleteModelApi(model.id!)
    ElMessage.success('删除成功')
    await loadModels()
  } catch (error) {
    if (error !== 'cancel') console.error('删除失败', error)
  }
}

async function handleToggle(model: AiModelConfig) {
  try {
    await toggleModel(model.id!)
    ElMessage.success(model.isEnabled ? '已启用' : '已禁用')
  } catch (e: any) {
    model.isEnabled = model.isEnabled ? 0 : 1
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleSetDefault(model: AiModelConfig) {
  try {
    await setDefaultModel(model.id!)
    ElMessage.success('已设为默认模型')
    await loadModels()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleTest(model: AiModelConfig) {
  const loadingInstance = ElMessage({ message: '正在测试连接...', type: 'info', duration: 0 })
  try {
    const res = await testConnection(model.id!)
    loadingInstance.close()
    if (res.data && res.data.code === 200) {
      ElMessage.success('连接测试成功：' + res.data.data)
    } else {
      ElMessage.warning('连接测试返回异常')
    }
  } catch (e: any) {
    loadingInstance.close()
    ElMessage.error('连接测试失败：' + (e.message || '未知错误'))
  }
}

onMounted(() => {
  loadModels()
  loadProviders()
})
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">AI 模型管理</h2>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>模型列表</span>
          <el-button type="primary" @click="openDialog()">
            <el-icon><Plus /></el-icon>
            新增模型
          </el-button>
        </div>
      </template>

      <el-table :data="models" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="displayName" label="模型名称" min-width="120" />
        <el-table-column prop="provider" label="提供商" width="100">
          <template #default="{ row }">
            <el-tag>{{ getProviderLabel(row.provider) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="modelType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.modelType)">
              {{ getTypeLabel(row.modelType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="modelId" label="模型ID" min-width="150" />
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-switch v-model="row.isEnabled" :active-value="1" :inactive-value="0" @change="handleToggle(row)" />
            <el-tag v-if="row.isDefault" type="success" size="small" style="margin-left: 8px">默认</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="success" @click="handleTest(row)">测试</el-button>
            <el-button size="small" type="warning" @click="handleSetDefault(row)" :disabled="row.isDefault === 1">默认</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingModel ? '编辑模型' : '新增模型'" width="600px">
      <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">
        <el-form-item label="提供商" prop="provider">
          <el-select v-model="form.provider" placeholder="选择提供商" style="width: 100%">
            <el-option v-for="p in providers" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型标识" prop="name">
          <el-input v-model="form.name" placeholder="如：zhipu-main" />
        </el-form-item>
        <el-form-item label="显示名称" prop="displayName">
          <el-input v-model="form.displayName" placeholder="如：智谱AI" />
        </el-form-item>
        <el-form-item label="模型类型" prop="modelType">
          <el-select v-model="form.modelType" style="width: 100%">
            <el-option label="文本模型" value="text" />
            <el-option label="视觉模型" value="vision" />
            <el-option label="多模态" value="both" />
          </el-select>
        </el-form-item>
        <el-form-item label="API地址" prop="apiUrl">
          <el-input v-model="form.apiUrl" placeholder="https://open.bigmodel.cn/api/paas/v4" />
        </el-form-item>
        <el-form-item label="API密钥" prop="apiKey">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="API Key" />
        </el-form-item>
        <el-form-item label="模型ID" prop="modelId">
          <el-input v-model="form.modelId" placeholder="glm-4-flash" />
        </el-form-item>
        <el-form-item label="视觉模型ID" v-if="form.modelType === 'both'">
          <el-input v-model="form.visionModelId" placeholder="glm-4v-flash" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="最大Token数">
              <el-input-number v-model="form.maxTokens" :min="256" :max="128000" :step="256" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序顺序">
              <el-input-number v-model="form.sortOrder" :min="0" :max="999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.page-title {
  font-size: 24px;
  margin-bottom: 20px;
  color: #333;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-card {
  border-radius: 8px;
}
</style>
