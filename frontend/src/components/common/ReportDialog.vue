<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { checkReported, submitReport } from '@/api/modules/report'
import { useUserStore } from '@/stores/user'

const props = defineProps<{
  modelValue: boolean
  targetType: 'post' | 'comment' | 'poem' | 'user'
  targetId: number
  targetTitle?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submitted: []
}>()

const router = useRouter()
const userStore = useUserStore()

const submitting = ref(false)
const checking = ref(false)
const reported = ref(false)
const form = reactive({
  reasonType: '',
  reason: '',
  evidence: ''
})

const reasonOptions = [
  { value: 'spam', label: '垃圾广告 / 引流' },
  { value: 'abuse', label: '辱骂攻击 / 人身攻击' },
  { value: 'porn', label: '色情低俗' },
  { value: 'plagiarism', label: '抄袭 / 洗稿' },
  { value: 'illegal', label: '违法违规' },
  { value: 'other', label: '其他违规' }
]

const targetLabel = () => {
  switch (props.targetType) {
    case 'post': return '帖子'
    case 'comment': return '评论'
    case 'poem': return '诗词'
    default: return '用户'
  }
}

const resetForm = () => {
  form.reasonType = ''
  form.reason = ''
  form.evidence = ''
  reported.value = false
}

const check = async () => {
  if (!userStore.isLoggedIn || !props.targetId) return
  checking.value = true
  try {
    const res = await checkReported({ targetType: props.targetType, targetId: props.targetId })
    reported.value = !!res.data?.reported
  } catch {
    reported.value = false
  } finally {
    checking.value = false
  }
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      resetForm()
      check()
    }
  }
)

const close = () => emit('update:modelValue', false)

const goRepair = () => {
  close()
  router.push('/repair/create')
}

const submit = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再举报')
    close()
    router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  if (!form.reasonType) {
    ElMessage.warning('请选择举报原因')
    return
  }
  if (form.reason.trim().length < 5) {
    ElMessage.warning('请补充至少 5 个字的举报说明')
    return
  }
  submitting.value = true
  try {
    await submitReport({
      targetType: props.targetType,
      targetId: props.targetId,
      reasonType: form.reasonType,
      reason: form.reason.trim(),
      evidence: form.evidence.trim() || undefined
    })
    ElMessage.success('举报已提交，我们会尽快核实处理')
    emit('submitted')
    close()
  } catch {
    // 业务错误已由请求层提示（如重复举报）
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="举报违规内容"
    width="520px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="report-tip">
      <span>当前举报对象：{{ targetLabel() }}{{ targetTitle ? `｜${targetTitle}` : '' }}</span>
      <el-button link type="primary" @click="goRepair">系统 Bug 请走报修 →</el-button>
    </div>

    <el-alert
      v-if="reported"
      type="warning"
      :closable="false"
      show-icon
      title="你已举报过该内容"
      description="请勿重复提交，处理结果会通过站内通知告知。"
      class="reported-tip"
    />

    <el-form v-else v-loading="checking" label-width="88px" class="report-form">
      <el-form-item label="举报原因" required>
        <el-select v-model="form.reasonType" placeholder="请选择举报原因" style="width: 100%">
          <el-option v-for="item in reasonOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="举报说明" required>
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请描述具体违规情况，便于我们核实"
        />
      </el-form-item>
      <el-form-item label="证据">
        <el-input
          v-model="form.evidence"
          type="textarea"
          :rows="2"
          maxlength="500"
          show-word-limit
          placeholder="可填写截图链接或补充说明（选填）"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button v-if="!reported" type="primary" :loading="submitting" @click="submit">提交举报</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.report-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  padding: 8px 12px;
  border-radius: 8px;
  background: rgba(212, 175, 135, 0.12);
  color: #8a6d4a;
  font-size: 13px;
}

.reported-tip {
  margin-bottom: 8px;
}

.report-form {
  margin-top: 4px;
}
</style>
