<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminHomeNavigation, createHomeNavigation, updateHomeNavigation, deleteHomeNavigation } from '@/api/modules/admin'

const NAV_ANIMATION_KEY = 'home_nav_animation_config'

const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const filterType = ref('')

const animationOptions = [
  { label: '波浪', value: 'wave' },
  { label: '淡入', value: 'fade' },
  { label: '滑入', value: 'slide' }
]

const animationConfig = ref<Record<string, string>>({
  works: 'wave',
  genres: 'fade',
  dynasties: 'slide'
})

const loadAnimationConfig = () => {
  try {
    const saved = localStorage.getItem(NAV_ANIMATION_KEY)
    if (saved) animationConfig.value = { ...animationConfig.value, ...JSON.parse(saved) }
  } catch {}
}

const saveAnimationConfig = () => {
  localStorage.setItem(NAV_ANIMATION_KEY, JSON.stringify(animationConfig.value))
  ElMessage.success('动画配置已保存')
}

const form = ref({
  id: 0,
  type: 'works',
  title: '',
  subtitle: '',
  imageUrl: '',
  linkId: null as number | null,
  linkType: 'poem',
  sortOrder: 0,
  status: 1
})

const typeOptions = [
  { label: '作品', value: 'works' },
  { label: '流派', value: 'genres' },
  { label: '朝代', value: 'dynasties' }
]

const linkTypeOptions = [
  { label: '诗词', value: 'poem' },
  { label: '分类', value: 'category' },
  { label: '朝代', value: 'dynasty' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getAdminHomeNavigation({ 
      page: page.value, 
      size: pageSize.value,
      type: filterType.value || undefined
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('获取首页导航列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { id: 0, type: 'works', title: '', subtitle: '', imageUrl: '', linkId: null, linkType: 'poem', sortOrder: 0, status: 1 }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该导航项吗？', '提示', { type: 'warning' })
    await deleteHomeNavigation(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') console.error('删除失败', error)
  }
}

const handleSave = async () => {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!form.value.imageUrl.trim()) {
    ElMessage.warning('请输入图片URL')
    return
  }
  
  loading.value = true
  try {
    if (isEdit.value) {
      await updateHomeNavigation(form.value.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createHomeNavigation(form.value)
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

const handleFilter = () => {
  page.value = 1
  fetchData()
}

const getTypeLabel = (type: string) => {
  const option = typeOptions.find(o => o.value === type)
  return option ? option.label : type
}

const getLinkTypeLabel = (linkType: string) => {
  const option = linkTypeOptions.find(o => o.value === linkType)
  return option ? option.label : linkType
}

onMounted(() => {
  loadAnimationConfig()
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">首页导航管理</h2>

    <el-card class="animation-card">
      <template #header>
        <div class="card-header">
          <span>动画效果配置</span>
          <el-button type="primary" size="small" @click="saveAnimationConfig">保存配置</el-button>
        </div>
      </template>
      <el-row :gutter="24">
        <el-col :span="8">
          <div class="animation-item">
            <span class="animation-label">作品模块</span>
            <el-select v-model="animationConfig.works" style="width: 100%">
              <el-option v-for="opt in animationOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="animation-item">
            <span class="animation-label">流派模块</span>
            <el-select v-model="animationConfig.genres" style="width: 100%">
              <el-option v-for="opt in animationOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="animation-item">
            <span class="animation-label">朝代模块</span>
            <el-select v-model="animationConfig.dynasties" style="width: 100%">
              <el-option v-for="opt in animationOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="filter-card">
      <el-row :gutter="16" align="middle">
        <el-col :span="8">
          <el-select v-model="filterType" placeholder="筛选类型" clearable @change="handleFilter" style="width: 100%">
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-col>
        <el-col :span="16" style="text-align: right">
          <el-button type="primary" @click="handleAdd">新增导航项</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 'works' ? 'primary' : row.type === 'genres' ? 'success' : 'warning'" size="small">
              {{ getTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" width="150" show-overflow-tooltip />
        <el-table-column prop="subtitle" label="副标题" width="180" show-overflow-tooltip />
        <el-table-column prop="imageUrl" label="图片" min-width="120" show-overflow-tooltip />
        <el-table-column label="链接类型" width="80">
          <template #default="{ row }">
            {{ getLinkTypeLabel(row.linkType) }}
          </template>
        </el-table-column>
        <el-table-column prop="linkId" label="关联ID" width="80" />
        <el-table-column prop="sortOrder" label="排序" width="60" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
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
        <el-pagination 
          v-model:current-page="page" 
          v-model:page-size="pageSize" 
          :total="total" 
          layout="total, prev, pager, next" 
          @current-change="fetchData" 
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑导航项' : '新增导航项'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="类型" required>
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" placeholder="请输入副标题（可选）" />
        </el-form-item>
        <el-form-item label="图片URL" required>
          <el-input v-model="form.imageUrl" placeholder="请输入图片文件名" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="链接类型">
              <el-select v-model="form.linkType" style="width: 100%">
                <el-option v-for="opt in linkTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联ID">
              <el-input-number v-model="form.linkId" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
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

.animation-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.animation-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.animation-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.filter-card {
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