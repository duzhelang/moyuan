<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminCategories, createAdminCategory, updateAdminCategory, deleteAdminCategory } from '@/api/modules/admin'

const allCategories = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const searchText = ref('')
const filterStatus = ref<number | ''>('')
const expandAll = ref(true)

const form = ref({
  id: 0,
  name: '',
  description: '',
  icon: '',
  parentId: 0,
  sortOrder: 0,
  status: 1
})

const parentCategories = computed(() => {
  return allCategories.value.filter(c => c.parentId === 0)
})

const filteredCategories = computed(() => {
  let list = allCategories.value
  if (searchText.value) {
    const keyword = searchText.value.toLowerCase()
    list = list.filter(c =>
      c.name?.toLowerCase().includes(keyword) ||
      c.description?.toLowerCase().includes(keyword)
    )
  }
  if (filterStatus.value !== '') {
    list = list.filter(c => c.status === filterStatus.value)
  }
  return list
})

const treeData = computed(() => {
  const roots = filteredCategories.value.filter(c => c.parentId === 0)
  return roots.map(root => ({
    ...root,
    children: filteredCategories.value
      .filter(c => c.parentId === root.id)
      .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  }))
})

const stats = computed(() => {
  const total = allCategories.value.length
  const topLevel = allCategories.value.filter(c => c.parentId === 0).length
  const sub = total - topLevel
  const enabled = allCategories.value.filter(c => c.status === 1).length
  const disabled = total - enabled
  return { total, topLevel, sub, enabled, disabled }
})

const getSubCategoryCount = (parentId: number) => {
  return allCategories.value.filter(c => c.parentId === parentId).length
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminCategories({ page: 1, size: 500 })
    allCategories.value = res.data.records
  } catch (error) {
    console.error('获取分类列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleAdd = (parentId = 0) => {
  isEdit.value = false
  form.value = { id: 0, name: '', description: '', icon: '', parentId, sortOrder: 0, status: 1 }
  dialogVisible.value = true
}

const handleAddSub = (parentId: number) => {
  handleAdd(parentId)
}

const handleEdit = (row: any) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  const subCount = getSubCategoryCount(row.id)
  const confirmMsg = subCount > 0
    ? `该分类下有 ${subCount} 个子分类，删除后子分类也会被删除，确定要删除「${row.name}」吗？`
    : `确定要删除「${row.name}」吗？`

  try {
    await ElMessageBox.confirm(confirmMsg, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
    await deleteAdminCategory(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') console.error('删除失败', error)
  }
}

const handleToggleStatus = async (row: any) => {
  const newStatus = row.status === 1 ? 0 : 1
  const label = newStatus === 1 ? '启用' : '禁用'
  try {
    await updateAdminCategory(row.id, { ...row, status: newStatus })
    ElMessage.success(`已${label}「${row.name}」`)
    fetchData()
  } catch (error) {
    console.error('状态更新失败', error)
  }
}

const handleSave = async () => {
  if (!form.value.name?.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  loading.value = true
  try {
    if (isEdit.value) {
      await updateAdminCategory(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createAdminCategory(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('保存失败', error)
  } finally {
    loading.value = false
  }
}

const formatTime = (time: string) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}

onMounted(fetchData)
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">分类管理</h2>

    <div class="stats-bar">
      <div class="stat-item">
        <span class="stat-value">{{ stats.total }}</span>
        <span class="stat-label">分类总数</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ stats.topLevel }}</span>
        <span class="stat-label">顶级分类</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ stats.sub }}</span>
        <span class="stat-label">子分类</span>
      </div>
      <div class="stat-item">
        <span class="stat-value enabled">{{ stats.enabled }}</span>
        <span class="stat-label">已启用</span>
      </div>
      <div class="stat-item">
        <span class="stat-value disabled">{{ stats.disabled }}</span>
        <span class="stat-label">已禁用</span>
      </div>
    </div>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>分类列表</span>
            <el-tag size="small" type="info">共 {{ stats.total }} 项</el-tag>
          </div>
          <div class="header-right">
            <el-button size="small" @click="expandAll = !expandAll">
              {{ expandAll ? '全部折叠' : '全部展开' }}
            </el-button>
            <el-button type="primary" size="small" @click="handleAdd()">新增顶级分类</el-button>
          </div>
        </div>
      </template>

      <div class="filter-bar">
        <el-input
          v-model="searchText"
          placeholder="搜索分类名称或描述"
          clearable
          prefix-icon="Search"
          style="width: 280px"
        />
        <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 140px">
          <el-option label="已启用" :value="1" />
          <el-option label="已禁用" :value="0" />
        </el-select>
      </div>

      <div v-loading="loading" class="category-tree">
        <div v-if="treeData.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无分类数据" />
        </div>

        <div v-for="root in treeData" :key="root.id" class="category-group">
          <div class="category-header">
            <div class="category-info">
              <span v-if="root.icon" class="category-icon">{{ root.icon }}</span>
              <el-tag type="primary" effect="dark" size="large">{{ root.name }}</el-tag>
              <el-tag
                size="small"
                :type="root.status === 1 ? 'success' : 'danger'"
                effect="plain"
              >
                {{ root.status === 1 ? '启用' : '禁用' }}
              </el-tag>
              <span class="category-desc">{{ root.description || '暂无描述' }}</span>
              <el-tag size="small" type="info" class="sub-count">
                {{ getSubCategoryCount(root.id) }} 个子分类
              </el-tag>
              <span class="category-time">{{ formatTime(root.createTime) }}</span>
            </div>
            <div class="category-actions">
              <el-button size="small" type="success" plain @click="handleAddSub(root.id)">
                + 添加子分类
              </el-button>
              <el-button size="small" @click="handleEdit(root)">编辑</el-button>
              <el-button
                size="small"
                :type="root.status === 1 ? 'warning' : 'success'"
                plain
                @click="handleToggleStatus(root)"
              >
                {{ root.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button size="small" type="danger" @click="handleDelete(root)">删除</el-button>
            </div>
          </div>

          <div v-if="expandAll && root.children && root.children.length > 0" class="sub-categories">
            <div
              v-for="child in root.children"
              :key="child.id"
              class="sub-category-item"
            >
              <div class="sub-info">
                <span v-if="child.icon" class="sub-icon">{{ child.icon }}</span>
                <el-tag type="success" size="default">{{ child.name }}</el-tag>
                <el-tag
                  size="small"
                  :type="child.status === 1 ? 'success' : 'danger'"
                  effect="plain"
                >
                  {{ child.status === 1 ? '启用' : '禁用' }}
                </el-tag>
                <span class="sub-desc" :title="child.description">{{ child.description || '-' }}</span>
                <span class="sub-sort">排序: {{ child.sortOrder || 0 }}</span>
              </div>
              <div class="sub-actions">
                <el-button size="small" @click="handleEdit(child)">编辑</el-button>
                <el-button
                  size="small"
                  :type="child.status === 1 ? 'warning' : 'success'"
                  plain
                  @click="handleToggleStatus(child)"
                >
                  {{ child.status === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button size="small" type="danger" @click="handleDelete(child)">删除</el-button>
              </div>
            </div>
          </div>

          <div v-else-if="expandAll && root.children && root.children.length === 0" class="no-sub">
            暂无子分类
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="550px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="输入 Emoji 或图标文字（如 📖）" maxlength="10" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入分类描述" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="父分类">
          <el-select v-model="form.parentId" placeholder="选择父分类（留空为顶级分类）" clearable style="width: 100%">
            <el-option label="无（顶级分类）" :value="0" />
            <el-option
              v-for="cat in parentCategories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch
                v-model="form.status"
                :active-value="1"
                :inactive-value="0"
                active-text="启用"
                inactive-text="禁用"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSave">保存</el-button>
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

.stats-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;

  .stat-item {
    flex: 1;
    background: #fff;
    border-radius: 8px;
    padding: 16px 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    border: 1px solid #e4e7ed;
    transition: box-shadow 0.3s;

    &:hover {
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    }

    .stat-value {
      font-size: 28px;
      font-weight: 600;
      color: #303133;
      line-height: 1.2;

      &.enabled { color: #67c23a; }
      &.disabled { color: #f56c6c; }
    }

    .stat-label {
      font-size: 13px;
      color: #909399;
      margin-top: 4px;
    }
  }
}

.table-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .header-left {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .header-right {
    display: flex;
    gap: 8px;
  }
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.empty-state {
  padding: 40px 0;
}

:deep(.el-dialog) {
  .el-form-item__content {
    flex: 1;
    min-width: 0;
  }
  .el-textarea {
    width: 100% !important;
  }
}

.category-tree {
  padding: 10px 0;
}

.category-group {
  margin-bottom: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
  border-bottom: 1px solid #e4e7ed;
}

.category-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.category-icon {
  font-size: 20px;
}

.category-desc {
  color: #606266;
  font-size: 13px;
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-time {
  color: #c0c4cc;
  font-size: 12px;
}

.sub-count {
  margin-left: 4px;
}

.category-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.sub-categories {
  padding: 12px 20px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 10px;
}

.sub-category-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #f9fafc;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  transition: all 0.3s ease;

  &:hover {
    background: #f0f2f5;
    border-color: #c0c4cc;
  }
}

.sub-info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.sub-icon {
  font-size: 16px;
}

.sub-desc {
  color: #909399;
  font-size: 12px;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sub-sort {
  color: #c0c4cc;
  font-size: 11px;
  white-space: nowrap;
}

.sub-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.no-sub {
  padding: 12px 20px;
  color: #c0c4cc;
  font-size: 13px;
  text-align: center;
}
</style>
