<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminPoets, createAdminPoet, updateAdminPoet, deleteAdminPoet } from '@/api/modules/admin'
import { getDynastyList } from '@/api/modules/dynasty'

const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const dynasties = ref<any[]>([])

const form = ref({
  id: 0,
  name: '',
  courtesyName: '',
  pseudonym: '',
  dynastyId: undefined as number | undefined,
  birthYear: null as number | null,
  deathYear: null as number | null,
  birthplace: '',
  biography: '',
  status: 1
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminPoets({ page: page.value, size: pageSize.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('获取诗人列表失败', error)
  } finally {
    loading.value = false
  }
}

const fetchDynasties = async () => {
  try {
    const res = await getDynastyList()
    dynasties.value = res.data
  } catch (error) {
    console.error('获取朝代列表失败', error)
  }
}

const getDynastyName = (id: number) => {
  return dynasties.value.find(d => d.id === id)?.name || ''
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { id: 0, name: '', courtesyName: '', pseudonym: '', dynastyId: undefined, birthYear: null, deathYear: null, birthplace: '', biography: '', status: 1 }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该诗人吗？', '提示', { type: 'warning' })
    await deleteAdminPoet(row.id)
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
      await updateAdminPoet(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createAdminPoet(form.value)
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
  fetchDynasties()
})
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">诗人管理</h2>

    <el-card class="table-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>诗人列表</span>
          <el-button type="primary" size="small" @click="handleAdd">新增诗人</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="courtesyName" label="字" width="100" />
        <el-table-column prop="pseudonym" label="号" width="100" />
        <el-table-column label="朝代" width="100">
          <template #default="{ row }">{{ getDynastyName(row.dynastyId) }}</template>
        </el-table-column>
        <el-table-column prop="birthplace" label="出生地" width="120" show-overflow-tooltip />
        <el-table-column label="生卒年" width="150">
          <template #default="{ row }">{{ row.birthYear || '?' }} ~ {{ row.deathYear || '?' }}</template>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑诗人' : '新增诗人'" width="550px">
      <el-form :model="form" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" required>
              <el-input v-model="form.name" placeholder="诗人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="朝代">
              <el-select v-model="form.dynastyId" placeholder="选择朝代" clearable style="width: 100%">
                <el-option v-for="d in dynasties" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="字">
              <el-input v-model="form.courtesyName" placeholder="字" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="号">
              <el-input v-model="form.pseudonym" placeholder="号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="出生地">
              <el-input v-model="form.birthplace" placeholder="出生地" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="生年">
              <el-input-number v-model="form.birthYear" :min="-3000" :max="2500" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="卒年">
              <el-input-number v-model="form.deathYear" :min="-3000" :max="2500" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="生平">
          <el-input v-model="form.biography" type="textarea" :rows="4" placeholder="生平简介" />
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
