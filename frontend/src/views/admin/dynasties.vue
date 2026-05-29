<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminDynasties, createAdminDynasty, updateAdminDynasty, deleteAdminDynasty } from '@/api/modules/admin'

const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)

const form = ref({
  id: 0,
  name: '',
  description: '',
  startYear: null as number | null,
  endYear: null as number | null,
  sortOrder: 0
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminDynasties({ page: page.value, size: pageSize.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('获取朝代列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { id: 0, name: '', description: '', startYear: null, endYear: null, sortOrder: 0 }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该朝代吗？', '提示', { type: 'warning' })
    await deleteAdminDynasty(row.id)
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
      await updateAdminDynasty(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createAdminDynasty(form.value)
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
    <h2 class="page-title">朝代管理</h2>

    <el-card class="table-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>朝代列表</span>
          <el-button type="primary" size="small" @click="handleAdd">新增朝代</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="朝代名称" width="120" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ row.startYear ? row.startYear + '年' : '?' }} ~ {{ row.endYear ? row.endYear + '年' : '至今' }}
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="60" />
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑朝代' : '新增朝代'" width="450px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="朝代名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="朝代简介" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="起始年">
              <el-input-number v-model="form.startYear" :min="-3000" :max="2500" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束年">
              <el-input-number v-model="form.endYear" :min="-3000" :max="2500" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" />
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
.page-title { font-size: 24px; margin-bottom: 20px; color: #333; }
.table-card { border-radius: 8px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
