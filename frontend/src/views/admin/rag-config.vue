<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getRagHealth,
  getRagModels,
  updateRagConfig,
  type RagConfig,
  type RagHealth,
  type RagModelsPayload
} from '@/api/modules/rag'

const loading = ref(false)
const saving = ref(false)
const health = ref<RagHealth | null>(null)
const models = ref<RagModelsPayload | null>(null)
const form = ref<RagConfig>({
  embed: 'bge-small',
  generation: 'qwen3.5',
  rerank: true,
  top_k: 8
})

async function loadData() {
  loading.value = true
  try {
    const [healthRes, modelRes] = await Promise.all([getRagHealth(), getRagModels()])
    health.value = healthRes
    models.value = modelRes
    form.value = { ...modelRes.config }
  } catch (error: any) {
    ElMessage.error(error.message || '知识问答服务不可用，请确认 app.py 已启动')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    const res = await updateRagConfig(form.value)
    form.value = { ...res.config }
    ElMessage.success(res.message || '配置已保存')
    await loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function statusText(status: string) {
  return status === 'ready' ? '已启用' : '预留接口'
}

onMounted(loadData)
</script>

<template>
  <div class="rag-config-page">
    <el-card class="tip-card">
      <h3>知识问答配置</h3>
      <p>
        用于全站 AI 小窗「网页用法助理」的检索问答。切换嵌入模型需要先构建对应索引；
        预留模型只保留接口，不参与实际检索。
      </p>
    </el-card>

    <el-card v-loading="loading" class="config-card">
      <template #header>
        <div class="card-header">
          <span>当前生效配置</span>
          <el-tag :type="health?.ollama ? 'success' : 'danger'" size="small">
            Ollama {{ health?.ollama ? '已连接' : '未连接' }}
          </el-tag>
        </div>
      </template>

      <el-form label-width="120px" class="config-form">
        <el-form-item label="嵌入模型">
          <el-select v-model="form.embed" style="width: 320px">
            <el-option
              v-for="item in models?.embeddings || []"
              :key="item.key"
              :label="`${item.label}（${item.dim} 维）`"
              :value="item.key"
              :disabled="item.status !== 'ready' || !item.ready"
            >
              <span>{{ item.label }}（{{ item.dim }} 维）</span>
              <el-tag
                v-if="item.status !== 'ready' || !item.ready"
                size="small"
                type="info"
                style="margin-left: 8px"
              >
                {{ !item.ready ? item.message : '预留' }}
              </el-tag>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="生成模型">
          <el-select v-model="form.generation" style="width: 320px">
            <el-option
              v-for="item in models?.generations || []"
              :key="item.key"
              :label="item.label"
              :value="item.key"
              :disabled="item.status !== 'ready'"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="开启重排">
          <el-switch v-model="form.rerank" />
        </el-form-item>

        <el-form-item label="送入块数">
          <el-input-number v-model="form.top_k" :min="4" :max="20" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
          <el-button :disabled="loading" @click="loadData">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-loading="loading" class="model-card">
      <template #header>
        <div class="card-header">
          <span>本地模型与索引状态</span>
          <el-tag v-if="health?.index_ready" type="success" size="small">
            当前索引 {{ health.chunks }} 块
          </el-tag>
        </div>
      </template>

      <el-table :data="models?.embeddings || []" size="small">
        <el-table-column prop="key" label="标识" width="120" />
        <el-table-column prop="label" label="模型" min-width="160" />
        <el-table-column prop="provider" label="提供方" width="110" />
        <el-table-column prop="dim" label="维度" width="80" />
        <el-table-column label="注册状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ready' ? 'success' : 'info'" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="索引状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.ready ? 'success' : 'warning'" size="small">
              {{ row.ready ? '可用' : '未就绪' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunks" label="文本块" width="90" />
        <el-table-column prop="note" label="说明" min-width="220" />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.rag-config-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.tip-card h3 {
  margin: 0 0 8px;
  font-size: 16px;
}
.tip-card p {
  margin: 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
.config-form {
  max-width: 620px;
}
</style>
