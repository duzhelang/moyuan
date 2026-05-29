<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminPoems, createAdminPoem, updateAdminPoem, deleteAdminPoem } from '@/api/modules/admin'
import { getCategoryList } from '@/api/modules/category'
import { getDynastyList } from '@/api/modules/dynasty'

const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)

const form = ref({
  id: 0,
  title: '',
  content: '',
  source: '',
  poetId: undefined as number | undefined,
  dynastyId: undefined as number | undefined,
  categoryId: undefined as number | undefined,
  status: 1,
  isFeatured: 0
})

const categories = ref<any[]>([])
const dynasties = ref<any[]>([])

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminPoems({ page: page.value, size: pageSize.value, keyword: keyword.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('获取诗词列表失败', error)
  } finally {
    loading.value = false
  }
}

const fetchOptions = async () => {
  try {
    const [catRes, dynRes] = await Promise.all([getCategoryList(), getDynastyList()])
    categories.value = catRes.data
    dynasties.value = dynRes.data
  } catch (error) {
    console.error('获取选项失败', error)
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增诗词'
  isEdit.value = false
  form.value = { id: 0, title: '', content: '', source: '', poetId: undefined, dynastyId: undefined, categoryId: undefined, status: 1, isFeatured: 0 }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑诗词'
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该诗词吗？', '提示', { type: 'warning' })
    await deleteAdminPoem(row.id)
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
      await updateAdminPoem(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createAdminPoem(form.value)
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

const handleSearch = () => {
  page.value = 1
  fetchData()
}

onMounted(() => {
  fetchData()
  fetchOptions()
})
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">诗词管理</h2>

    <el-card class="search-card">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-input v-model="keyword" placeholder="搜索诗词标题/内容" clearable @keyup.enter="handleSearch" />
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </el-col>
        <el-col :span="12" style="text-align: right">
          <el-button type="primary" @click="handleAdd">新增诗词</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="source" label="来源" width="120" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="精选" width="60">
          <template #default="{ row }">
            <el-tag v-if="row.isFeatured" type="danger">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="60" />
        <el-table-column prop="likeCount" label="点赞" width="60" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入诗词标题" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入诗词内容" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="form.source" placeholder="来源" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="朝代">
              <el-select v-model="form.dynastyId" placeholder="选择朝代" clearable style="width: 100%">
                <el-option v-for="d in dynasties" :key="d.id" :label="d.name" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分类">
              <el-select v-model="form.categoryId" placeholder="选择分类" clearable style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option :label="'已发布'" :value="1" />
                <el-option :label="'草稿'" :value="0" />
              </el-select>
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

.search-card {
  margin-bottom: 16px;
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
