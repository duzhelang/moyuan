<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  name: '',
  email: '',
  type: 'suggestion',
  title: '',
  content: ''
})

const typeOptions = [
  { value: 'suggestion', label: '功能建议' },
  { value: 'feedback', label: '意见反馈' },
  { value: 'cooperation', label: '商务合作' },
  { value: 'other', label: '其他' }
]

const rules: FormRules = {
  name: [
    { required: true, message: '请输入您的姓名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入留言内容', trigger: 'blur' },
    { min: 10, max: 2000, message: '内容长度在 10 到 2000 个字符', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        ElMessage.success('留言提交成功，我们会尽快回复您！')
        form.name = ''
        form.email = ''
        form.type = 'suggestion'
        form.title = ''
        form.content = ''
      } catch (error) {
        console.error('提交失败', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

const goToRepair = () => {
  router.push('/repair')
}

const goToHome = () => {
  router.push('/')
}

const goToProfile = () => {
  router.push('/user/profile')
}

const goBack = () => {
  router.go(-1)
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    userStore.logout()
    router.push('/')
    ElMessage.success('已退出登录')
  } catch {}
}
</script>

<template>
  <div class="contact-page">
    <div class="contact-hero">
      <div class="hero-nav">
        <el-button class="nav-btn" @click="goBack">
          <el-icon><Back /></el-icon>
          <span>上一页</span>
        </el-button>
        <el-button class="nav-btn" @click="goToHome">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-button>
        <el-button class="nav-btn" @click="goToProfile">
          <el-icon><User /></el-icon>
          <span>个人中心</span>
        </el-button>
        <el-button class="nav-btn" @click="goToRepair">
          <el-icon><Tickets /></el-icon>
          <span>报修中心</span>
        </el-button>
        <el-button class="nav-btn logout-btn" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          <span>退出</span>
        </el-button>
      </div>
      <h1 class="hero-title">联系我们</h1>
      <p class="hero-subtitle">您的意见是我们前进的动力</p>
    </div>

    <div class="contact-container">
      <div class="contact-content">
        <div class="contact-main">
          <el-card class="message-card">
            <template #header>
              <div class="card-header">
                <span>在线留言</span>
                <span class="card-desc">向创始人团队反馈您的建议和意见</span>
              </div>
            </template>

            <el-form
              ref="formRef"
              :model="form"
              :rules="rules"
              label-width="80px"
              label-position="top"
            >
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="您的姓名" prop="name">
                    <el-input v-model="form.name" placeholder="请输入姓名" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="邮箱地址" prop="email">
                    <el-input v-model="form.email" placeholder="请输入邮箱" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="留言类型" prop="type">
                <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
                  <el-option
                    v-for="item in typeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="标题" prop="title">
                <el-input v-model="form.title" placeholder="请输入标题" maxlength="100" show-word-limit />
              </el-form-item>

              <el-form-item label="留言内容" prop="content">
                <el-input
                  v-model="form.content"
                  type="textarea"
                  placeholder="请详细描述您的建议或意见..."
                  :rows="6"
                  maxlength="2000"
                  show-word-limit
                />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" :loading="submitting" @click="handleSubmit">
                  提交留言
                </el-button>
                <el-button @click="formRef?.resetFields()">重置</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <el-card class="repair-card">
            <template #header>
              <div class="card-header">
                <span>报修服务</span>
                <span class="card-desc">遇到问题？提交报修工单</span>
              </div>
            </template>
            <div class="repair-content">
              <div class="repair-info">
                <p>如果您在使用过程中遇到任何技术问题或系统故障，可以通过报修系统提交工单，我们的技术团队会尽快处理。</p>
                <ul class="repair-features">
                  <li>快速响应：24小时内处理紧急问题</li>
                  <li>进度追踪：实时查看工单处理状态</li>
                  <li>满意度评价：处理完成后可评价服务质量</li>
                </ul>
              </div>
              <el-button type="primary" size="large" @click="goToRepair" class="repair-btn">
                <el-icon><Tickets /></el-icon>
                <span>进入报修中心</span>
              </el-button>
            </div>
          </el-card>
        </div>

        <div class="contact-sidebar">
          <el-card class="team-card">
            <template #header>
              <div class="card-header">
                <span>创始人团队</span>
              </div>
            </template>
            <div class="team-members">
              <div class="team-member">
                <el-avatar :size="64" class="member-avatar">
                  <span class="avatar-text">杜</span>
                </el-avatar>
                <div class="member-info">
                  <h4 class="member-name">D125</h4>
                  <span class="member-role">创始人 / 全栈开发</span>
                  <p class="member-desc">热爱古典文学与现代技术的融合，致力于让诗词文化在数字时代焕发新生。</p>
                </div>
              </div>
              <el-divider />
              <div class="team-member">
                <el-avatar :size="64" class="member-avatar">
                  <span class="avatar-text">渊</span>
                </el-avatar>
                <div class="member-info">
                  <h4 class="member-name">墨渊团队</h4>
                  <span class="member-role">技术团队</span>
                  <p class="member-desc">一群热爱中国传统文化的年轻人，用代码传承千年文脉，用技术点亮诗词之美。</p>
                </div>
              </div>
            </div>
            <div class="team-message">
              <p>亲爱的用户：</p>
              <p>感谢您对古诗词汇的支持与厚爱！我们是一群热爱古典文学的年轻人，希望通过现代科技的力量，让更多人能够便捷地接触、学习和欣赏中国古典诗词。</p>
              <p>您的每一个建议都是我们前进的动力，您的每一次反馈都帮助我们变得更好。如果您有任何想法、建议或合作意向，欢迎通过左侧的留言表单与我们联系。</p>
              <p>让我们一起传承经典，品味诗词之美！</p>
              <p class="signature">—— 墨渊创始人团队</p>
            </div>
          </el-card>

          <el-card class="contact-info-card">
            <template #header>
              <div class="card-header">
                <span>联系方式</span>
              </div>
            </template>
            <div class="info-list">
              <div class="info-item">
                <el-icon><Message /></el-icon>
                <div>
                  <span class="info-label">客服邮箱</span>
                  <span class="info-value">support@gushihui.com</span>
                </div>
              </div>
              <div class="info-item">
                <el-icon><Promotion /></el-icon>
                <div>
                  <span class="info-label">商务合作</span>
                  <span class="info-value">business@gushihui.com</span>
                </div>
              </div>
              <div class="info-item">
                <el-icon><ChatDotRound /></el-icon>
                <div>
                  <span class="info-label">意见反馈</span>
                  <span class="info-value">feedback@gushihui.com</span>
                </div>
              </div>
              <div class="info-item">
                <el-icon><Clock /></el-icon>
                <div>
                  <span class="info-label">工作时间</span>
                  <span class="info-value">周一至周五 9:00-18:00</span>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.contact-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5ede3 0%, #ede3d7 50%, #f5ede3 100%);
}

.contact-hero {
  padding: 100px 20px 60px;
  text-align: center;
  background: linear-gradient(135deg, #5a3e2b 0%, #6b4c36 50%, #5a3e2b 100%);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: url('/img/dt_3.jpg') no-repeat center / cover;
    opacity: 0.15;
  }
}

.hero-nav {
  position: absolute;
  top: 70px;
  left: 20px;
  display: flex;
  gap: 10px;
  z-index: 2;
}

.nav-btn {
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #f8f0e6;
  backdrop-filter: blur(4px);

  &:hover {
    background: rgba(255, 255, 255, 0.25);
    color: #fff;
  }
}

.logout-btn {
  background: rgba(245, 108, 108, 0.3);
  border-color: rgba(245, 108, 108, 0.5);

  &:hover {
    background: rgba(245, 108, 108, 0.5);
  }
}

.hero-title {
  font-size: 42px;
  color: #f8f0e6;
  font-weight: 700;
  margin: 0 0 15px;
  letter-spacing: 4px;
  position: relative;
  z-index: 1;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
}

.hero-subtitle {
  font-size: 18px;
  color: #d9c7b2;
  margin: 0;
  letter-spacing: 2px;
  position: relative;
  z-index: 1;
}

.contact-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px 60px;
}

.contact-content {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 30px;
}

.contact-main {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.contact-sidebar {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;

  span {
    font-size: 18px;
    font-weight: 600;
    color: #2c3e50;
  }

  .card-desc {
    font-size: 14px;
    color: #999;
    font-weight: normal;
  }
}

.message-card,
.repair-card,
.team-card,
.contact-info-card {
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.repair-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.repair-info {
  p {
    font-size: 15px;
    color: #666;
    line-height: 1.8;
    margin: 0 0 15px;
  }
}

.repair-features {
  list-style: none;
  padding: 0;
  margin: 0;

  li {
    font-size: 14px;
    color: #666;
    padding: 8px 0;
    padding-left: 24px;
    position: relative;

    &::before {
      content: '✓';
      position: absolute;
      left: 0;
      color: #67c23a;
      font-weight: bold;
    }
  }
}

.repair-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
}

.team-members {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.team-member {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.member-avatar {
  background: linear-gradient(135deg, #5a3e2b, #6b4c36);
  flex-shrink: 0;
}

.avatar-text {
  font-size: 24px;
  font-weight: 600;
  color: #f8f0e6;
}

.member-info {
  flex: 1;
}

.member-name {
  font-size: 18px;
  color: #2c3e50;
  margin: 0 0 4px;
  font-weight: 600;
}

.member-role {
  font-size: 13px;
  color: #d4af87;
  display: block;
  margin-bottom: 8px;
}

.member-desc {
  font-size: 14px;
  color: #666;
  margin: 0;
  line-height: 1.6;
}

.team-message {
  margin-top: 20px;
  padding: 20px;
  background: #faf7f4;
  border-radius: 12px;
  border-left: 4px solid #d4af87;

  p {
    font-size: 14px;
    color: #666;
    line-height: 1.8;
    margin: 0 0 12px;

    &:last-of-type {
      margin-bottom: 0;
    }
  }

  .signature {
    text-align: right;
    color: #5a3e2b;
    font-weight: 500;
    margin-top: 16px;
  }
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;

  .el-icon {
    font-size: 20px;
    color: #d4af87;
    margin-top: 2px;
  }

  div {
    display: flex;
    flex-direction: column;
  }
}

.info-label {
  font-size: 13px;
  color: #999;
  margin-bottom: 4px;
}

.info-value {
  font-size: 15px;
  color: #2c3e50;
  font-weight: 500;
}

@media (max-width: 1024px) {
  .contact-content {
    grid-template-columns: 1fr;
  }
}

.message-card {
  .el-form :deep(.el-textarea__inner) {
    font-family: $font-family-input;
    color: $comment-input-color;
    letter-spacing: 0.5px;
    line-height: 1.6;
    resize: vertical;
    padding: 12px;
    background-color: $comment-input-bg;
    border: 1px solid $comment-input-border;
    border-radius: $border-radius-sm;
    transition: all 0.2s;

    &::placeholder {
      color: $comment-input-placeholder;
    }

    &:focus {
      border-color: $comment-input-border-focus;
      box-shadow: 0 0 0 2px rgba(134, 142, 150, 0.1);
      outline: none;
    }
  }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 28px;
  }

  .hero-subtitle {
    font-size: 14px;
  }

  .contact-container {
    padding: 20px;
  }

  .team-member {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
}
</style>
