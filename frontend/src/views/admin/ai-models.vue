<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
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
  getAllModuleConfigs,
  updateModuleConfig,
  type AiModelConfig,
  type AiModuleConfig
} from '@/api/modules/admin-ai-model'

const activeTab = ref('models')
const models = ref<AiModelConfig[]>([])
const moduleConfigs = ref<AiModuleConfig[]>([])
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
  nvidia: 'NVIDIA NIM',
  qwen: '千问',
  mimo: '小米MiMo',
  openrouter: 'OpenRouter'
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
  if (type === 'vision') return 'warning' as const
  if (type === 'both') return 'success' as const
  return 'primary' as const
}

function getModelName(modelId: number | null) {
  if (!modelId) return '未配置'
  const model = models.value.find(m => m.id === modelId)
  return model ? model.displayName : '未知模型'
}

function getAvailableModels(requireVision: number) {
  if (requireVision === 1) {
    return models.value.filter(m => m.isEnabled === 1 && (m.modelType === 'vision' || m.modelType === 'both'))
  }
  return models.value.filter(m => m.isEnabled === 1)
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

async function loadModuleConfigs() {
  try {
    const res = await getAllModuleConfigs()
    if (res.data && res.data.code === 200) {
      moduleConfigs.value = res.data.data || []
    }
  } catch (e) {
    console.error('加载模块配置失败', e)
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

async function handleModuleModelChange(moduleCode: string, modelId: number | null) {
  try {
    await updateModuleConfig(moduleCode, { modelId })
    ElMessage.success('模块配置已更新')
    await loadModuleConfigs()
  } catch (e: any) {
    ElMessage.error(e.message || '更新失败')
  }
}

onMounted(() => {
  loadModels()
  loadProviders()
  loadModuleConfigs()
})
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">AI 模型管理</h2>

    <el-tabs v-model="activeTab" class="ai-tabs">
      <el-tab-pane label="模型管理" name="models">
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
      </el-tab-pane>

      <el-tab-pane label="模块配置" name="modules">
        <el-card class="table-card">
          <template #header>
            <div class="card-header">
              <span>AI功能模块模型配置</span>
              <el-tooltip content="为每个AI功能模块指定使用的模型，识图类功能只能选择支持视觉的模型" placement="top">
                <el-tag type="info">配置说明</el-tag>
              </el-tooltip>
            </div>
          </template>

          <el-table :data="moduleConfigs" stripe style="width: 100%">
            <el-table-column prop="moduleName" label="模块名称" width="150" />
            <el-table-column prop="moduleCode" label="模块编码" width="120" />
            <el-table-column prop="description" label="模块描述" min-width="200" />
            <el-table-column label="需要视觉" width="100">
              <template #default="{ row }">
                <el-tag :type="row.requireVision === 1 ? 'warning' : 'info'" size="small">
                  {{ row.requireVision === 1 ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="当前模型" width="150">
              <template #default="{ row }">
                <el-tag :type="row.modelId ? 'success' : 'danger'" size="small">
                  {{ getModelName(row.modelId) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="配置模型" width="250">
              <template #default="{ row }">
                <el-select
                  :model-value="row.modelId"
                  placeholder="选择模型"
                  clearable
                  style="width: 100%"
                  @change="(val: number | null) => handleModuleModelChange(row.moduleCode, val)"
                >
                  <el-option
                    v-for="model in getAvailableModels(row.requireVision)"
                    :key="model.id"
                    :label="`${model.displayName} (${model.modelId})`"
                    :value="model.id!"
                  />
                </el-select>
                <div v-if="row.requireVision === 1" class="vision-tip">
                  仅显示支持视觉的模型
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" :title="editingModel ? '编辑模型' : '新增模型'" width="650px">
      <el-form :model="form" label-width="90px" :rules="rules" ref="formRef">
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

.ai-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 16px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-card {
  border-radius: 8px;
}

.vision-tip {
  font-size: 12px;
  color: #e6a23c;
  margin-top: 4px;
}

:deep(.el-dialog) {
  .el-form-item__content {
    flex: 1;
    min-width: 0;
  }
  .el-input,
  .el-textarea,
  .el-select,
  .el-input-number {
    width: 100% !important;
  }
}
</style>
