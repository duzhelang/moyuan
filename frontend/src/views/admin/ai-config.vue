<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getAllModels,
  getAllModuleConfigs,
  updateModuleConfig,
  type AiModelConfig,
  type AiModuleConfig
} from '@/api/modules/admin-ai-model'

// 模块编码 → 前台应用位置说明（展示用，便于在后台直观看出「AI 在哪些功能生效」）
const moduleUsageMap: Record<string, string> = {
  chat: '诗阁对话 · 首页AI工具（藏头诗等） · 诗词详情AI填补 · 押韵辅助',
  poet_chat: '诗人详情页对话 · 诗人详情AI填补',
  poetry_chat: '诗词列表 译文/注释/赏析/背景 · 外部诗词详情弹窗',
  write_poem: '首页 看图写诗',
  analyze: '首页 诗词鉴赏分析',
  rating: '诗词详情 AI 评分',
  couplet: '首页 对对联',
  ocr: '首页 古籍识文（OCR）',
  feihua_gen_ancient: '飞花令 · 古制严格候选生成',
  feihua_gen_normal: '飞花令 · 标准版候选生成',
  feihua_gen_super: '飞花令 · 超级候选生成',
  feihua_verify: '飞花令 · 诗句真伪兜底核验',
  feihua_topic: '飞花令 · 主题语义匹配',
  chain_generate: '诗词接龙 · 接句生成'
}

const models = ref<AiModelConfig[]>([])
const moduleConfigs = ref<AiModuleConfig[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = ref<AiModuleConfig>({
  moduleCode: '',
  moduleName: '',
  modelId: null,
  requireVision: 0,
  description: '',
  promptTemplate: '',
  maxLength: 200,
  responseStyle: 'concise',
  firstResponseLength: 100,
  enableMarkdown: 0
})

const formRules: FormRules = {
  maxLength: [
    { required: true, message: '请输入最大回答长度', trigger: 'blur' },
    { type: 'number', min: 50, max: 1000, message: '范围50-1000字', trigger: 'blur' }
  ],
  firstResponseLength: [
    { required: true, message: '请输入首次回答长度', trigger: 'blur' },
    { type: 'number', min: 30, max: 500, message: '范围30-500字', trigger: 'blur' }
  ],
  responseStyle: [{ required: true, message: '请选择回答风格', trigger: 'change' }]
}

// 兜底模型：未接入模块的 AI 调用（如漏传 moduleCode）会回退到该模型
const defaultModel = computed(() => models.value.find(m => m.isDefault === 1))

function getModuleUsage(moduleCode: string) {
  return moduleUsageMap[moduleCode] || '—'
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
  try {
    const res = await getAllModels()
    models.value = res.data || []
  } catch (e) {
    console.error('加载模型列表失败', e)
  }
}

async function loadModuleConfigs() {
  loading.value = true
  try {
    const res = await getAllModuleConfigs()
    moduleConfigs.value = res.data || []
  } catch (e) {
    console.error('加载模块配置失败', e)
  } finally {
    loading.value = false
  }
}

async function handleModuleModelChange(moduleCode: string, modelId: number | null) {
  try {
    await updateModuleConfig(moduleCode, { modelId })
    ElMessage.success('模块模型已更新')
    await loadModuleConfigs()
  } catch (e: any) {
    ElMessage.error(e.message || '更新失败')
  }
}

function openDialog(row: AiModuleConfig) {
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleSave() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await updateModuleConfig(form.value.moduleCode, form.value)
    ElMessage.success('配置已保存')
    dialogVisible.value = false
    await loadModuleConfigs()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadModels()
  loadModuleConfigs()
})
</script>

<template>
  <div class="ai-config-page">
    <el-card class="tip-card">
      <div class="tip-content">
        <div class="tip-text">
          <h3>AI 功能配置</h3>
          <p>为各 AI 功能模块指定使用的模型与提示词约束。下方「应用位置」说明该模块在前台的生效功能点。</p>
          <p class="tip-warn">
            兜底模型：当前为
            <strong>{{ defaultModel ? defaultModel.displayName : '未设置' }}</strong>
            ，未接入模块的 AI 调用会回退到此模型，建议设为额度充足、稳定的模型。
          </p>
        </div>
      </div>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <span>功能模块列表</span>
      </template>

      <el-table :data="moduleConfigs" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="moduleName" label="模块名称" width="150" />
        <el-table-column prop="moduleCode" label="模块编码" width="150" />
        <el-table-column label="应用位置" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="usage-text">{{ getModuleUsage(row.moduleCode) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="当前模型" width="140">
          <template #default="{ row }">
            <el-tag :type="row.modelId ? 'success' : 'danger'" size="small">
              {{ getModelName(row.modelId) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="配置模型" width="200">
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
                :label="model.displayName"
                :value="model.id!"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openDialog(row)">配置</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="模块 AI 配置" width="700px">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="120px">
        <el-form-item label="模块名称">
          <el-input v-model="form.moduleName" disabled />
        </el-form-item>
        <el-form-item label="模块编码">
          <el-input v-model="form.moduleCode" disabled />
        </el-form-item>
        <el-form-item label="应用位置">
          <span class="usage-text">{{ getModuleUsage(form.moduleCode) }}</span>
        </el-form-item>
        <el-form-item label="角色设定">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="AI角色描述" />
        </el-form-item>
        <el-form-item label="提示词模板">
          <el-input
            v-model="form.promptTemplate"
            type="textarea"
            :rows="4"
            placeholder="支持变量：{poetName}诗人名称, {poemTitle}诗词标题, {poemContent}诗词内容, {maxLength}最大长度, {styleHint}风格提示"
          />
          <div class="form-tip">可用变量：{poetName} - 诗人名称, {poemTitle} - 诗词标题, {poemContent} - 诗词内容, {maxLength} - 最大回答长度, {styleHint} - 风格提示</div>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="最大回答长度" prop="maxLength">
              <el-input-number v-model="form.maxLength" :min="50" :max="1000" :step="50" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="首次回答长度" prop="firstResponseLength">
              <el-input-number v-model="form.firstResponseLength" :min="30" :max="500" :step="10" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="回答风格" prop="responseStyle">
              <el-select v-model="form.responseStyle" style="width: 100%">
                <el-option label="简洁" value="concise" />
                <el-option label="均衡" value="balanced" />
                <el-option label="详细" value="detailed" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="允许Markdown">
              <el-switch v-model="form.enableMarkdown" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.ai-config-page {
  .tip-card {
    margin-bottom: 16px;

    .tip-text {
      h3 {
        margin: 0 0 8px;
        font-size: 16px;
        color: #5a3d1e;
      }

      p {
        margin: 4px 0;
        font-size: 13px;
        color: #7a6a55;
      }

      .tip-warn {
        color: #b88230;

        strong {
          color: #8a5a1e;
        }
      }
    }
  }

  .usage-text {
    font-size: 13px;
    color: #6b5b45;
  }

  .form-tip {
    margin-top: 6px;
    font-size: 12px;
    color: #999;
  }
}
</style>
