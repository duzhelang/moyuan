<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminPoetFeatured, createAdminPoetFeatured, updateAdminPoetFeatured, deleteAdminPoetFeatured } from '@/api/modules/admin'
import ImageUpload from '@/components/common/ImageUpload.vue'

const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)

const form = ref({
  id: 0,
  poetId: null as number | null,
  name: '',
  dynasty: '',
  description: '',
  biography: '',
  imageUrl: '',
  sortOrder: 0,
  status: 1
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminPoetFeatured({ page: page.value, size: pageSize.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('获取精选诗人列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { id: 0, poetId: null, name: '', dynasty: '', description: '', biography: '', imageUrl: '', sortOrder: 0, status: 1 }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该精选诗人吗？', '提示', { type: 'warning' })
    await deleteAdminPoetFeatured(row.id)
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
      await updateAdminPoetFeatured(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createAdminPoetFeatured(form.value)
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

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">精选诗人管理</h2>

    <el-card class="table-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>精选诗人列表</span>
          <el-button type="primary" size="small" @click="handleAdd">新增精选诗人</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="意境图" width="100">
          <template #default="{ row }">
            <el-image :src="row.imageUrl" :size="50" fit="cover" style="width: 60px; height: 60px; border-radius: 4px">
              <template #error>
                <div style="width: 60px; height: 60px; display: flex; align-items: center; justify-content: center; background: #f5f5f5; border-radius: 4px">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="诗人姓名" width="120" />
        <el-table-column prop="dynasty" label="朝代" width="100" />
        <el-table-column prop="description" label="简介" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑精选诗人' : '新增精选诗人'" width="650px" append-to-body>
      <el-form :model="form" label-width="100px">
        <el-form-item label="意境图">
          <ImageUpload v-model="form.imageUrl" :maxSize="5" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="诗人姓名" required>
              <el-input v-model="form.name" placeholder="诗人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="朝代">
              <el-input v-model="form.dynasty" placeholder="朝代" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关联诗人ID">
              <el-input-number v-model="form.poetId" :min="0" placeholder="关联诗人表ID" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="诗人简介（显示在卡片正面）" />
        </el-form-item>
        <el-form-item label="详细生平">
          <el-input v-model="form.biography" type="textarea" :rows="5" placeholder="详细生平（显示在卡片背面）" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="禁用" />
        </el-form-item>
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
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>