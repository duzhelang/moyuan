<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminHomeNavigation, createHomeNavigation, updateHomeNavigation, deleteHomeNavigation } from '@/api/modules/admin'
import ImageUpload from '@/components/common/ImageUpload.vue'

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
  { label: '波浪', value: 'wave', icon: '🌊' },
  { label: '淡入', value: 'fade', icon: '✨' },
  { label: '滑入', value: 'slide', icon: '➡️' },
  { label: '缩放', value: 'zoom', icon: '🔍' },
  { label: '弹跳', value: 'bounce', icon: '⬆️' },
  { label: '旋转', value: 'rotate', icon: '🔄' }
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
              <el-option v-for="opt in animationOptions" :key="opt.value" :label="`${opt.icon} ${opt.label}`" :value="opt.value" />
            </el-select>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="animation-item">
            <span class="animation-label">流派模块</span>
            <el-select v-model="animationConfig.genres" style="width: 100%">
              <el-option v-for="opt in animationOptions" :key="opt.value" :label="`${opt.icon} ${opt.label}`" :value="opt.value" />
            </el-select>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="animation-item">
            <span class="animation-label">朝代模块</span>
            <el-select v-model="animationConfig.dynasties" style="width: 100%">
              <el-option v-for="opt in animationOptions" :key="opt.value" :label="`${opt.icon} ${opt.label}`" :value="opt.value" />
            </el-select>
          </div>
        </el-col>
      </el-row>
      <div class="animation-preview">
        <div class="preview-title">动画预览</div>
        <div class="preview-cards">
          <div 
            v-for="(anim, key) in animationConfig" 
            :key="key"
            class="preview-card"
            :class="`anim-${anim}`"
          >
            <div class="preview-card-icon">{{ animationOptions.find(o => o.value === anim)?.icon }}</div>
            <div class="preview-card-text">{{ key === 'works' ? '作品' : key === 'genres' ? '流派' : '朝代' }}</div>
          </div>
        </div>
      </div>
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
        <el-table-column label="图片" width="100">
          <template #default="{ row }">
            <el-image :src="row.imageUrl" fit="cover" style="width: 60px; height: 60px; border-radius: 4px">
              <template #error>
                <div style="width: 60px; height: 60px; display: flex; align-items: center; justify-content: center; background: #f5f5f5; border-radius: 4px">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑导航项' : '新增导航项'" width="550px">
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
        <el-form-item label="图片" required>
          <ImageUpload v-model="form.imageUrl" :maxSize="5" fileType="config" />
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

.animation-preview {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.preview-title {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  margin-bottom: 16px;
}

.preview-cards {
  display: flex;
  gap: 16px;
}

.preview-card {
  flex: 1;
  height: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.preview-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
}

.preview-card-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.preview-card-text {
  font-size: 12px;
  font-weight: 500;
}

.anim-wave {
  animation: wave 2s ease-in-out infinite;
}

.anim-fade {
  animation: fade 2s ease-in-out infinite;
}

.anim-slide {
  animation: slide 2s ease-in-out infinite;
}

.anim-zoom {
  animation: zoom 2s ease-in-out infinite;
}

.anim-bounce {
  animation: bounce 2s ease-in-out infinite;
}

.anim-rotate {
  animation: rotate 2s ease-in-out infinite;
}

@keyframes wave {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

@keyframes fade {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@keyframes slide {
  0%, 100% { transform: translateX(0); }
  50% { transform: translateX(10px); }
}

@keyframes zoom {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  25% { transform: translateY(-15px); }
  50% { transform: translateY(0); }
  75% { transform: translateY(-8px); }
}

@keyframes rotate {
  0%, 100% { transform: rotate(0deg); }
  50% { transform: rotate(5deg); }
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

:deep(.el-dialog) {
  .el-form-item__content {
    flex: 1;
    min-width: 0;
  }
  .el-input,
  .el-textarea,
  .el-select,
  .el-input-number {
    width: 100% !important;
  }
}
</style>