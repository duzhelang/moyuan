<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import type { User, Poem, PoetProfile } from '@/types/model'
import { getUserProfile, getUserWorks } from '@/api/modules/user'

const route = useRoute()
const userId = ref<number>(Number(route.params.id))

const user = ref<User | null>(null)
const poetProfile = ref<PoetProfile | null>(null)
const works = ref<Poem[]>([])
const worksTotal = ref(0)
const currentPage = ref(1)
const pageSize = 10

const activeTab = ref('works')

const fetchUserProfile = async () => {
  try {
    const res = await getUserProfile(userId.value)
    user.value = res.data.user
    poetProfile.value = res.data.poetProfile
  } catch (error) {
    ElMessage.error('获取用户信息失败')
  }
}

const fetchWorks = async () => {
  try {
    const res = await getUserWorks(userId.value, currentPage.value, pageSize)
    works.value = res.data.records
    worksTotal.value = res.data.total
  } catch (error) {
    ElMessage.error('获取作品列表失败')
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchWorks()
}

onMounted(() => {
  fetchUserProfile()
  fetchWorks()
})
</script>

<template>
  <div class="user-homepage">
    <div class="container">
      <div class="page-nav">
        <el-button text @click="$router.push('/')">
          <el-icon><HomeFilled /></el-icon>
          首页
        </el-button>
        <el-divider direction="vertical" />
        <span class="current-page">用户主页</span>
      </div>

      <div class="user-card">
        <div class="user-avatar">
          <el-avatar :size="100" :src="user?.avatar" />
          <div v-if="poetProfile?.verifiedStatus === 1" class="verified-badge">
            <el-icon><CircleCheck /></el-icon>
            <span>认证诗人</span>
          </div>
        </div>
        <div class="user-info">
          <h1 class="username">
            {{ user?.nickname || user?.username }}
            <el-tag v-if="poetProfile?.verifiedStatus === 1" type="success" size="small">
              V
            </el-tag>
          </h1>
          <p class="bio">{{ user?.bio || '这个人很懒，什么都没留下' }}</p>
          <div class="stats">
            <div class="stat-item">
              <span class="stat-value">{{ poetProfile?.workCount || 0 }}</span>
              <span class="stat-label">作品</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ poetProfile?.likeCount || 0 }}</span>
              <span class="stat-label">获赞</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ poetProfile?.followerCount || 0 }}</span>
              <span class="stat-label">粉丝</span>
            </div>
          </div>
        </div>
      </div>

      <div class="content-section">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="作品集" name="works">
            <div class="works-grid">
              <el-card v-for="poem in works" :key="poem.id" class="work-card" shadow="hover" @click="$router.push(`/poem/${poem.id}`)">
                <h3 class="work-title">
                  {{ poem.title }}
                </h3>
                <p class="work-content">{{ poem.content?.substring(0, 100) || '' }}...</p>
                <div class="work-meta">
                  <span>{{ poem.viewCount }} 浏览</span>
                  <span>{{ poem.likeCount }} 点赞</span>
                </div>
              </el-card>
            </div>
            <div v-if="worksTotal > pageSize" class="pagination-section">
              <el-pagination
                :current-page="currentPage"
                :page-size="pageSize"
                :total="worksTotal"
                layout="prev, pager, next"
                @current-change="handlePageChange"
              />
            </div>
          </el-tab-pane>
          
          <el-tab-pane v-if="poetProfile" label="诗人简介" name="intro">
            <div class="poet-intro">
              <div v-if="poetProfile.specialty" class="intro-item">
                <h4>擅长体裁</h4>
                <p>{{ poetProfile.specialty }}</p>
              </div>
              <div v-if="poetProfile.introduction" class="intro-item">
                <h4>个人简介</h4>
                <p>{{ poetProfile.introduction }}</p>
              </div>
              <div v-if="poetProfile.literaryConcept" class="intro-item">
                <h4>创作理念</h4>
                <p>{{ poetProfile.literaryConcept }}</p>
              </div>
              <div v-if="poetProfile.achievements" class="intro-item">
                <h4>主要成就</h4>
                <p>{{ poetProfile.achievements }}</p>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.user-homepage {
  padding: $spacing-xl 0;
  min-height: 80vh;
}

.container {
  @include container;
}

.page-nav {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-lg;

  .el-button {
    color: $text-color-secondary;

    &:hover {
      color: $primary-color;
    }
  }

  .current-page {
    font-size: $font-size-sm;
    color: $text-color-light;
  }
}

.user-card {
  display: flex;
  gap: $spacing-xl;
  @include card;
  margin-bottom: $spacing-xl;
}

.user-avatar {
  position: relative;
  flex-shrink: 0;
  
  .verified-badge {
    position: absolute;
    bottom: -5px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    align-items: center;
    gap: 4px;
    background: $success-color;
    color: #fff;
    padding: 2px 8px;
    border-radius: 10px;
    font-size: $font-size-xs;
    white-space: nowrap;
  }
}

.user-info {
  flex: 1;
  
  .username {
    font-size: $font-size-title;
    margin-bottom: $spacing-sm;
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-family: $font-family-title;
  }
  
  .bio {
    color: $text-color-secondary;
    margin-bottom: $spacing-lg;
  }
}

.stats {
  display: flex;
  gap: $spacing-xl;
  
  .stat-item {
    text-align: center;
    
    .stat-value {
      display: block;
      font-size: $font-size-xxl;
      font-weight: bold;
      color: $primary-color;
    }
    
    .stat-label {
      font-size: $font-size-xs;
      color: $text-color-secondary;
    }
  }
}

.content-section {
  @include card;
}

.works-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: $spacing-lg;
}

.work-card {
  cursor: pointer;
  transition: transform $transition-fast;
  
  &:hover {
    transform: translateY(-2px);
  }
  
  .work-title {
    font-size: $font-size-lg;
    margin-bottom: $spacing-sm;
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }
  
  .work-content {
    color: $text-color-secondary;
    font-size: $font-size-base;
    line-height: $line-height-loose;
    margin-bottom: $spacing-sm;
    @include text-clamp(3);
  }
  
  .work-meta {
    display: flex;
    gap: $spacing-lg;
    font-size: $font-size-xs;
    color: $text-color-secondary;
  }
}

.pagination-section {
  margin-top: $spacing-xl;
  display: flex;
  justify-content: center;
}

.poet-intro {
  .intro-item {
    margin-bottom: $spacing-xl;
    
    h4 {
      font-size: $font-size-lg;
      color: $primary-color;
      margin-bottom: $spacing-sm;
    }
    
    p {
      color: $text-color;
      line-height: $line-height-loose;
    }
  }
}

@include responsive(md) {
  .user-card {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  .user-info .username {
    justify-content: center;
  }
  
  .stats {
    justify-content: center;
  }
}
</style>