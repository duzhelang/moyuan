<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminVisionArticles,
  createAdminVisionArticle,
  updateAdminVisionArticle,
  updateAdminVisionArticleStatus,
  updateAdminVisionArticleFeatured,
  deleteAdminVisionArticle,
} from '@/api/modules/admin-vision'

const loading = ref(false)
const dataList = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const statusFilter = ref<number | null>(null)
const categoryFilter = ref('')
const keyword = ref('')

const categoryOptions = ['诗词鉴赏', '文化探索', '诗人故事', '诗词历史']

const statusOptions = [
  { label: '全部', value: null },
  { label: '已发布', value: 1 },
  { label: '草稿/下架', value: 0 },
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminVisionArticles({
      pageNum: page.value,
      pageSize: size.value,
      status: statusFilter.value,
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

// 新建/编辑弹窗
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const form = ref<any>({
  id: null,
  title: '',
  summary: '',
  content: '',
  coverImage: '',
  author: '',
  category: '',
  status: 0,
  isFeatured: 0,
  sortOrder: 0,
})

const resetForm = () => {
  form.value = {
    id: null,
    title: '',
    summary: '',
    content: '',
    coverImage: '',
    author: '',
    category: '',
    status: 0,
    isFeatured: 0,
    sortOrder: 0,
  }
}

const openCreate = () => {
  resetForm()
  isEdit.value = false
  dialogVisible.value = true
}

const openEdit = (row: any) => {
  form.value = { ...row }
  isEdit.value = true
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!form.value.title || !form.value.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateAdminVisionArticle(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createAdminVisionArticle(form.value)
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

const handleToggleStatus = async (row: any) => {
  const next = row.status === 1 ? 0 : 1
  try {
    await updateAdminVisionArticleStatus(row.id, next)
    row.status = next
    ElMessage.success(next === 1 ? '已发布' : '已下架')
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const handleToggleFeatured = async (row: any) => {
  const next = row.isFeatured === 1 ? 0 : 1
  try {
    await updateAdminVisionArticleFeatured(row.id, next)
    row.isFeatured = next
    ElMessage.success(next === 1 ? '已推荐' : '已取消推荐')
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认删除文章「${row.title}」？`, '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteAdminVisionArticle(row.id)
    ElMessage.success('已删除')
    fetchData()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(() => fetchData())
</script>

<template>
  <div class="vision-page">
    <div class="page-header">
      <h2>诗话视野管理</h2>
      <el-button type="primary" @click="openCreate">新建文章</el-button>
    </div>

    <div class="filter-bar">
      <el-radio-group v-model="statusFilter" @change="handleSearch">
        <el-radio-button v-for="opt in statusOptions" :key="String(opt.value)" :label="opt.value">
          {{ opt.label }}
        </el-radio-button>
      </el-radio-group>
      <el-select v-model="categoryFilter" placeholder="全部分类" clearable style="width: 160px" @change="handleSearch">
        <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
      </el-select>
      <el-input v-model="keyword" placeholder="按标题 / 作者搜索" clearable style="width: 240px" @keyup.enter="handleSearch" />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <el-table :data="dataList" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="author" label="作者" width="120" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="120" show-overflow-tooltip />
      <el-table-column label="推荐" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isFeatured === 1 ? 'danger' : 'info'" size="small">
            {{ row.isFeatured === 1 ? '推荐' : '普通' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
            {{ row.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览量" width="90" />
      <el-table-column prop="likeCount" label="点赞" width="80" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleToggleStatus(row)">
            {{ row.status === 1 ? '下架' : '发布' }}
          </el-button>
          <el-button size="small" @click="handleToggleFeatured(row)">
            {{ row.isFeatured === 1 ? '取消推荐' : '推荐' }}
          </el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑文章' : '新建文章'" width="680px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="form.author" placeholder="请输入作者" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select
            v-model="form.category"
            placeholder="可选择或输入分类"
            filterable
            allow-create
            default-first-option
            clearable
            style="width: 100%"
          >
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="请输入摘要" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入正文内容" />
        </el-form-item>
        <el-form-item label="封面图">
          <el-input v-model="form.coverImage" placeholder="封面图 URL（选填）" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="0">草稿</el-radio>
            <el-radio :label="1">发布</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否推荐">
          <el-radio-group v-model="form.isFeatured">
            <el-radio :label="0">否</el-radio>
            <el-radio :label="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
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
.vision-page {
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
