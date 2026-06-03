<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminCategories, createAdminCategory, updateAdminCategory, deleteAdminCategory } from '@/api/modules/admin'

const allCategories = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)

const form = ref({
  id: 0,
  name: '',
  description: '',
  parentId: 0,
  sortOrder: 0,
  status: 1
})

const parentCategories = computed(() => {
  return allCategories.value.filter(c => c.parentId === 0)
})

const treeData = computed(() => {
  const roots = allCategories.value.filter(c => c.parentId === 0)
  return roots.map(root => ({
    ...root,
    children: allCategories.value
      .filter(c => c.parentId === root.id)
      .sort((a, b) => a.sortOrder - b.sortOrder)
  }))
})

const getSubCategoryCount = (parentId: number) => {
  return allCategories.value.filter(c => c.parentId === parentId).length
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminCategories({ page: 1, size: 100 })
    allCategories.value = res.data.records
  } catch (error) {
    console.error('获取分类列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleAdd = (parentId = 0) => {
  isEdit.value = false
  form.value = { id: 0, name: '', description: '', parentId, sortOrder: 0, status: 1 }
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
    ? `该分类下有 ${subCount} 个子分类，删除后子分类也会被删除，确定要删除吗？`
    : '确定要删除该分类吗？'
  
  try {
    await ElMessageBox.confirm(confirmMsg, '提示', { type: 'warning' })
    await deleteAdminCategory(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') console.error('删除失败', error)
  }
}

const handleSave = async () => {
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

onMounted(fetchData)
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">分类管理</h2>

    <el-card class="table-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>分类列表</span>
          <el-button type="primary" size="small" @click="handleAdd()">新增顶级分类</el-button>
        </div>
      </template>

      <div v-loading="loading" class="category-tree">
        <div v-for="root in treeData" :key="root.id" class="category-group">
          <div class="category-header">
            <div class="category-info">
              <el-tag type="primary" effect="dark" size="large">{{ root.name }}</el-tag>
              <span class="category-desc">{{ root.description }}</span>
              <el-tag size="small" type="info" class="sub-count">
                {{ getSubCategoryCount(root.id) }} 个子分类
              </el-tag>
            </div>
            <div class="category-actions">
              <el-button size="small" type="success" plain @click="handleAddSub(root.id)">
                + 添加子分类
              </el-button>
              <el-button size="small" @click="handleEdit(root)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(root)">删除</el-button>
            </div>
          </div>
          
          <div v-if="root.children && root.children.length > 0" class="sub-categories">
            <div
              v-for="child in root.children"
              :key="child.id"
              class="sub-category-item"
            >
              <div class="sub-info">
                <el-tag type="success" size="default">{{ child.name }}</el-tag>
                <span class="sub-desc">{{ child.description }}</span>
              </div>
              <div class="sub-actions">
                <el-button size="small" @click="handleEdit(child)">编辑</el-button>
                <el-button size="small" type="danger" @click="handleDelete(child)">删除</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="450px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="分类名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="分类描述" />
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
              <el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" />
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
.table-card {
  border-radius: 8px;
}
.category-tree {
  padding: 10px 0;
}
.category-group {
  margin-bottom: 24px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}
.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
  border-bottom: 1px solid #e4e7ed;
}
.category-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.category-desc {
  color: #606266;
  font-size: 14px;
}
.sub-count {
  margin-left: 8px;
}
.category-actions {
  display: flex;
  gap: 8px;
}
.sub-categories {
  padding: 12px 20px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}
.sub-category-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
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
  gap: 10px;
}
.sub-desc {
  color: #909399;
  font-size: 12px;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-actions {
  display: flex;
  gap: 4px;
}
</style>
