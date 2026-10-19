<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminSystemKnowledgeList,
  createAdminSystemKnowledge,
  updateAdminSystemKnowledge,
  updateAdminSystemKnowledgeShow,
  deleteAdminSystemKnowledge,
} from '@/api/modules/systemKnowledge'
import type { SystemKnowledgeItem } from '@/api/modules/systemKnowledge'

const loading = ref(false)
const dataList = ref<SystemKnowledgeItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const categoryFilter = ref('')
const keyword = ref('')

// 分类取值与中文标签映射
const categoryOptions = [
  { label: '玩法', value: 'play' },
  { label: '页面', value: 'page' },
  { label: '功能', value: 'feature' },
  { label: '账号', value: 'account' },
]

const categoryMap: Record<string, string> = {
  play: '玩法',
  page: '页面',
  feature: '功能',
  account: '账号',
}

const categoryLabel = (value?: string) => (value ? categoryMap[value] || value : '')

// FAQ 为 JSON 数组文本，元素形如 {"q":"问题","a":"答案","aliases":["别名"]}
const faqPlaceholder = 'JSON 数组文本，元素格式：{"q":"问题","a":"答案","aliases":["别名"]}\n示例：[{"q":"怎么玩","a":"每回合提交一句含令字的古诗","aliases":["玩法"]}]'

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminSystemKnowledgeList({
      pageNum: page.value,
      pageSize: size.value,
      category: categoryFilter.value || undefined,
      keyword: keyword.value || undefined,
    })
    dataList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchData()
}

const handleReset = () => {
  categoryFilter.value = ''
  keyword.value = ''
  page.value = 1
  fetchData()
}

// 新建/编辑弹窗
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)

interface KnowledgeForm {
  id: number | null
  code: string
  title: string
  category: string
  keywords: string
  aliases: string
  route: string
  sortOrder: number
  isShow: number
  body: string
  steps: string
  faq: string
}

const emptyForm = (): KnowledgeForm => ({
  id: null,
  code: '',
  title: '',
  category: '',
  keywords: '',
  aliases: '',
  route: '',
  sortOrder: 0,
  isShow: 1,
  body: '',
  steps: '',
  faq: '',
})

const form = ref<KnowledgeForm>(emptyForm())

const openCreate = () => {
  form.value = emptyForm()
  isEdit.value = false
  dialogVisible.value = true
}

const openEdit = (row: SystemKnowledgeItem) => {
  form.value = {
    id: row.id,
    code: row.code,
    title: row.title,
    category: row.category,
    keywords: row.keywords || '',
    aliases: row.aliases || '',
    route: row.route || '',
    sortOrder: row.sortOrder ?? 0,
    isShow: row.isShow ?? 1,
    body: row.body || '',
    steps: row.steps || '',
    faq: row.faq || '',
  }
  isEdit.value = true
  dialogVisible.value = true
}

const buildPayload = () => ({
  code: form.value.code,
  title: form.value.title,
  category: form.value.category,
  keywords: form.value.keywords,
  aliases: form.value.aliases,
  route: form.value.route,
  sortOrder: form.value.sortOrder,
  isShow: form.value.isShow,
  body: form.value.body,
  steps: form.value.steps,
  faq: form.value.faq,
})

const submitForm = async () => {
  if (!form.value.code || !form.value.code.trim()) {
    ElMessage.warning('请填写编码')
    return
  }
  if (!form.value.title || !form.value.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  const id = form.value.id
  submitting.value = true
  try {
    if (isEdit.value && id != null) {
      await updateAdminSystemKnowledge(id, buildPayload())
      ElMessage.success('更新成功')
    } else {
      await createAdminSystemKnowledge(buildPayload())
      ElMessage.success('新建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

const handleToggleShow = async (row: SystemKnowledgeItem) => {
  const next = row.isShow === 1 ? 1 : 0
  try {
    await updateAdminSystemKnowledgeShow(row.id, next)
    ElMessage.success(next === 1 ? '已展示' : '已隐藏')
  } catch (e: any) {
    row.isShow = next === 1 ? 0 : 1
    ElMessage.error(e?.message || '操作失败')
  }
}

const handleDelete = async (row: SystemKnowledgeItem) => {
  try {
    await ElMessageBox.confirm(`确认删除知识条目「${row.title}」？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteAdminSystemKnowledge(row.id)
    ElMessage.success('已删除')
    fetchData()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(() => fetchData())
</script>

<template>
  <div class="system-knowledge-page">
    <div class="page-header">
      <h2>系统知识库</h2>
      <el-button type="primary" @click="openCreate">新建知识</el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="categoryFilter" placeholder="全部分类" clearable style="width: 160px" @change="handleSearch">
        <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
      </el-select>
      <el-input v-model="keyword" placeholder="按标题 / 关键词搜索" clearable style="width: 240px" @keyup.enter="handleSearch" />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-table :data="dataList" v-loading="loading" stripe>
      <el-table-column prop="code" label="编码" width="160" show-overflow-tooltip />
      <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
      <el-table-column label="分类" width="100">
        <template #default="{ row }">
          {{ categoryLabel(row.category) }}
        </template>
      </el-table-column>
      <el-table-column prop="keywords" label="关键词" min-width="180" show-overflow-tooltip />
      <el-table-column prop="route" label="关联入口" width="160" show-overflow-tooltip />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="展示" width="90">
        <template #default="{ row }">
          <el-switch v-model="row.isShow" :active-value="1" :inactive-value="0" @change="handleToggleShow(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="fetchData"
        @size-change="fetchData"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑知识' : '新建知识'" width="680px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="编码" required>
          <el-input v-model="form.code" :disabled="isEdit" placeholder="唯一编码，如 play.feihua" />
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="请选择分类" clearable style="width: 100%">
            <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keywords" placeholder="英文逗号分隔，如 飞花令,对诗,行酒令" />
        </el-form-item>
        <el-form-item label="别名">
          <el-input v-model="form.aliases" placeholder="英文逗号分隔，如 花令,飞花令游戏" />
        </el-form-item>
        <el-form-item label="关联入口">
          <el-input v-model="form.route" placeholder="关联路由，如 /game/feihua（选填）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="是否展示">
          <el-switch v-model="form.isShow" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="form.body" type="textarea" :rows="4" placeholder="正文说明（简洁，不超过 300 字）" />
        </el-form-item>
        <el-form-item label="步骤">
          <el-input v-model="form.steps" type="textarea" :rows="4" placeholder="每行一条步骤" />
        </el-form-item>
        <el-form-item label="FAQ">
          <el-input v-model="form.faq" type="textarea" :rows="5" :placeholder="faqPlaceholder" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.system-knowledge-page {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  h2 {
    margin: 0;
    font-size: 20px;
  }
}
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.pagination-bar {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
