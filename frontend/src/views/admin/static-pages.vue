<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminStaticPages, updateAdminStaticPage, type StaticPage } from '@/api/modules/static-page'
import { Plus, Delete, Top, Bottom } from '@element-plus/icons-vue'

interface Section {
  title: string
  content: string
}

const pages = ref<StaticPage[]>([])
const loading = ref(false)
const editingPageId = ref<number | null>(null)
const pageTitle = ref('')
const sections = ref<Section[]>([])
const pageStatus = ref(1)

const pageKeyLabels: Record<string, string> = {
  terms: '使用条款',
  privacy: '隐私政策',
  contact: '联系我们'
}

const fetchPages = async () => {
  loading.value = true
  try {
    const res = await getAdminStaticPages()
    pages.value = res.data || []
  } finally {
    loading.value = false
  }
}

const parseSections = (content: string): Section[] => {
  try {
    const parsed = JSON.parse(content)
    if (Array.isArray(parsed)) {
      return parsed.map(item => ({
        title: item.title || '',
        content: item.content || ''
      }))
    }
  } catch {
    // 如果解析失败，返回单个章节
  }
  return [{ title: '', content }]
}

const handleEdit = (row: StaticPage) => {
  editingPageId.value = row.id
  pageTitle.value = row.title
  pageStatus.value = row.status
  sections.value = parseSections(row.content)
}

const handleCancel = () => {
  editingPageId.value = null
  pageTitle.value = ''
  sections.value = []
}

const handleAddSection = () => {
  sections.value.push({ title: '', content: '' })
}

const handleRemoveSection = (index: number) => {
  sections.value.splice(index, 1)
}

const handleMoveUp = (index: number) => {
  if (index > 0) {
    const temp = sections.value[index]
    sections.value[index] = sections.value[index - 1]
    sections.value[index - 1] = temp
  }
}

const handleMoveDown = (index: number) => {
  if (index < sections.value.length - 1) {
    const temp = sections.value[index]
    sections.value[index] = sections.value[index + 1]
    sections.value[index + 1] = temp
  }
}

const handleSave = async () => {
  if (!editingPageId.value) return

  // 验证章节标题不为空
  const emptyTitle = sections.value.some(s => !s.title.trim())
  if (emptyTitle) {
    ElMessage.warning('章节标题不能为空')
    return
  }

  try {
    const content = JSON.stringify(sections.value)
    await updateAdminStaticPage(editingPageId.value, {
      title: pageTitle.value,
      content,
      status: pageStatus.value
    })
    ElMessage.success('保存成功')
    editingPageId.value = null
    fetchPages()
  } catch {
    ElMessage.error('保存失败')
  }
}

const editingPageName = computed(() => {
  const page = pages.value.find(p => p.id === editingPageId.value)
  return page ? pageKeyLabels[page.pageKey] || page.pageKey : ''
})

onMounted(fetchPages)
</script>

<template>
  <div class="static-pages-container">
    <div class="page-header">
      <h2>静态页面管理</h2>
      <p class="desc">管理使用条款、隐私政策、联系我们等静态页面内容，支持章节增删编辑</p>
    </div>

    <!-- 页面列表 -->
    <el-card v-if="!editingPageId" class="list-card">
      <el-table :data="pages" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column label="页面类型" width="150" align="center">
          <template #default="{ row }">
            <el-tag type="primary">{{ pageKeyLabels[row.pageKey] || row.pageKey }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="页面标题" min-width="150" />
        <el-table-column label="章节数" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ parseSections(row.content).length }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180" align="center" />
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑页面 -->
    <template v-if="editingPageId">
      <el-card class="edit-card">
        <template #header>
          <div class="edit-header">
            <span>编辑 {{ editingPageName }}</span>
            <div>
              <el-button @click="handleCancel">取消</el-button>
              <el-button type="primary" @click="handleSave">保存</el-button>
            </div>
          </div>
        </template>

        <el-form label-width="100px" class="edit-form">
          <el-form-item label="页面标题">
            <el-input v-model="pageTitle" placeholder="请输入页面标题" />
          </el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="pageStatus" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
          </el-form-item>
        </el-form>

        <div class="sections-header">
          <h3>章节内容</h3>
          <el-button type="primary" :icon="Plus" @click="handleAddSection">添加章节</el-button>
        </div>

        <div class="sections-list">
          <el-card v-for="(section, index) in sections" :key="index" class="section-card" shadow="hover">
            <div class="section-header">
              <span class="section-index">章节 {{ index + 1 }}</span>
              <div class="section-actions">
                <el-button :icon="Top" circle size="small" @click="handleMoveUp(index)" :disabled="index === 0" />
                <el-button :icon="Bottom" circle size="small" @click="handleMoveDown(index)" :disabled="index === sections.length - 1" />
                <el-button :icon="Delete" circle size="small" type="danger" @click="handleRemoveSection(index)" />
              </div>
            </div>
            <el-form label-width="80px" class="section-form">
              <el-form-item label="标题">
                <el-input v-model="section.title" placeholder="请输入章节标题" />
              </el-form-item>
              <el-form-item label="内容">
                <el-input v-model="section.content" type="textarea" :rows="6" placeholder="请输入章节内容" />
              </el-form-item>
            </el-form>
          </el-card>

          <el-empty v-if="sections.length === 0" description="暂无章节，点击上方按钮添加" />
        </div>
      </el-card>
    </template>
  </div>
</template>

<style scoped lang="scss">
.static-pages-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;

  h2 {
    font-size: 20px;
    margin: 0 0 8px;
    color: #303133;
  }

  .desc {
    color: #909399;
    font-size: 14px;
    margin: 0;
  }
}

.list-card {
  :deep(.el-card__body) {
    padding: 0;
  }
}

.edit-card {
  .edit-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 16px;
    font-weight: 600;
  }
}

.edit-form {
  max-width: 600px;
  margin-bottom: 20px;
}

.sections-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;

  h3 {
    margin: 0;
    font-size: 16px;
    color: #303133;
  }
}

.sections-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-card {
  :deep(.el-card__body) {
    padding: 16px;
  }
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-index {
  font-weight: 600;
  color: #409eff;
}

.section-actions {
  display: flex;
  gap: 8px;
}

.section-form {
  :deep(.el-form-item) {
    margin-bottom: 12px;
  }
}
</style>
