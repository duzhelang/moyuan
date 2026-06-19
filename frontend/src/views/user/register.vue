<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import AuthHeader from '@/components/common/AuthHeader.vue'
import { useParticles } from '@/composables/useParticles'

const showAgreement = ref(false)

const router = useRouter()
const userStore = useUserStore()

const particleCanvasRef = ref<HTMLCanvasElement | null>(null)
useParticles(particleCanvasRef, {
  count: 50,
  colors: ['#d4af87', '#f0e4d7', '#c9a06c'],
  opacityRange: [0.2, 0.4],
  sizeRange: [1.5, 3]
})

const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: '',
  interests: [] as string[],
  agreement: false
})

const avatarFile = ref<File | null>(null)
const avatarPreview = ref('')

const handleReset = () => {
  form.username = ''
  form.password = ''
  form.confirmPassword = ''
  form.nickname = ''
  form.email = ''
  form.interests = []
  form.agreement = false
  avatarFile.value = null
  avatarPreview.value = ''
}

const goToLogin = () => {
  router.push('/user/login')
}

const handleAvatarChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  if (input.files && input.files[0]) {
    const file = input.files[0]
    if (file.size > 2 * 1024 * 1024) {
      ElMessage.warning('头像图片大小不能超过2MB')
      return
    }
    avatarFile.value = file
    avatarPreview.value = URL.createObjectURL(file)
  }
}

const handleSubmit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (form.username.length < 3 || form.username.length > 20) {
    ElMessage.warning('用户名长度应为3-20个字符')
    return
  }
  if (form.password.length < 6 || form.password.length > 20) {
    ElMessage.warning('密码长度应为6-20个字符')
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入密码不一致')
    return
  }
  if (!form.agreement) {
    ElMessage.warning('请同意条款和条件')
    return
  }
  loading.value = true
  try {
    await userStore.register({
      username: form.username,
      password: form.password,
      email: form.email || undefined,
      nickname: form.nickname || form.username,
      interests: form.interests.length > 0 ? form.interests : undefined
    })
    ElMessage.success('注册成功')
    router.push('/')
  } catch (error) {
    ElMessage.error('注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="dl_container">
    <canvas ref="particleCanvasRef" class="particle-bg"></canvas>
    <AuthHeader />

    <div class="body">
      <div class="dl_tubiao">
        <img src="/img/lb_shiwen_ (1).png" class="dl_zhuang_s1_"/>
        <img src="/img/YIJI_tuobiao (6).png" class="zc_zhuang_s2"/>
        <img src="/img/fy_tubiao (5).png" class="dl_zhuang_s3"/>
        <img src="/img/tuobiao (5).png" class="zc_zhuang_s4"/>
        <img src="/img/fy_tubiao (10).png" class="dl_zhuang_s5"/>
      </div>

      <div class="zc_main">
        <div class="biao">
          <form @submit.prevent="handleSubmit">
            <table border="0" cellspacing="2" align="center">
              <caption>
                <h2>用&nbsp;户&nbsp;注&nbsp;册</h2>
                <div class="login-link">
                  <a href="javascript:void(0)" @click="goToLogin">已有账号?去登录</a>
                </div>
              </caption>
              <tr>
                <td align="right" width="200px">用户名：</td>
                <td><input type="text" class="wb" placeholder="请输入用户名" v-model="form.username" required></td>
              </tr>
              <tr>
                <td align="right">密码：</td>
                <td><input type="password" class="wb" placeholder="请输入密码" v-model="form.password" required></td>
              </tr>
              <tr>
                <td align="right">确认密码：</td>
                <td><input type="password" class="wb" placeholder="请再次输入密码" v-model="form.confirmPassword" required></td>
              </tr>
              <tr>
                <td align="right">昵称：</td>
                <td><input type="text" class="wb" placeholder="请输入昵称（可选）" v-model="form.nickname" /></td>
              </tr>
              <tr>
                <td align="right">头像：</td>
                <td>
                  <div class="avatar-upload">
                    <div class="avatar-preview" @click="$refs.avatarInput.click()">
                      <img v-if="avatarPreview" :src="avatarPreview" alt="头像预览" />
                      <div v-else class="avatar-placeholder">
                        <span>+</span>
                        <span>上传头像</span>
                      </div>
                    </div>
                    <input
                      ref="avatarInput"
                      type="file"
                      accept="image/*"
                      style="display: none"
                      @change="handleAvatarChange"
                    />
                    <span class="avatar-tip">支持jpg、png格式，大小不超过2MB</span>
                  </div>
                </td>
              </tr>
              <tr>
                <td align="right">邮箱：</td>
                <td><input type="email" class="wb" placeholder="请输入邮箱（可选）" v-model="form.email" /></td>
              </tr>
              <tr>
                <td align="right">兴趣：</td>
                <td>
                  <div class="interests-container">
                    <label class="interest-item">
                      <input type="checkbox" value="古典" v-model="form.interests" />
                      <span>古典</span>
                    </label>
                    <label class="interest-item">
                      <input type="checkbox" value="现代" v-model="form.interests" />
                      <span>现代</span>
                    </label>
                    <label class="interest-item">
                      <input type="checkbox" value="自由体" v-model="form.interests" />
                      <span>自由体</span>
                    </label>
                    <label class="interest-item">
                      <input type="checkbox" value="外国" v-model="form.interests" />
                      <span>外国</span>
                    </label>
                  </div>
                </td>
              </tr>
              <tr>
                <td align="right">
                  <a href="javascript:void(0)" @click="showAgreement = true" title="查看详情">点击查看条约</a>
                </td>
                <td>
                  <div class="w3-agree">
                    <input type="checkbox" id="c1" name="cc" v-model="form.agreement">
                    <label class="agileits-agree">我同意这项<a href="javascript:void(0)" @click="showAgreement = true">条款和条件</a></label>
                    <div class="clear"></div>
                  </div>
                </td>
              </tr>
              <tr style="height: 55px;">
                <td align="right"><input type="button" value="重置" class="cz" @click="handleReset" /></td>
                <td align="center"><input type="submit" value="注册" class="tj" :disabled="loading" /></td>
              </tr>
            </table>
          </form>
        </div>
      </div>
    </div>

    <div class="dlzc_ditu_div"></div>

    <!-- 协议条款模态框 -->
    <div v-if="showAgreement" class="agreement-modal" @click.self="showAgreement = false">
      <div class="agreement-content">
        <div class="agreement-header">
          <h3>用户注册协议</h3>
          <button class="close-btn" @click="showAgreement = false">&times;</button>
        </div>
        <div class="agreement-body">
          <h4>一、总则</h4>
          <p>欢迎使用"古今诗话"平台（以下简称"本平台"）。在注册成为本平台用户之前，请您仔细阅读以下协议条款。您一旦注册成功，即表示您已充分理解并同意本协议的所有条款。</p>
          
          <h4>二、用户注册</h4>
          <p>1. 您在注册时应提供真实、准确、完整的个人资料，并在资料发生变动时及时更新。</p>
          <p>2. 您应妥善保管您的账号和密码，因您的保管不当而导致的损失由您自行承担。</p>
          <p>3. 您不得将账号转让、赠与或借给他人使用。</p>
          
          <h4>三、用户行为规范</h4>
          <p>1. 您在使用本平台时应遵守中华人民共和国相关法律法规。</p>
          <p>2. 您不得利用本平台发布、传播含有以下内容的信息：</p>
          <ul>
            <li>违反国家法律法规的</li>
            <li>危害国家安全、泄露国家秘密的</li>
            <li>颠覆国家政权、破坏国家统一的</li>
            <li>损害国家荣誉和利益的</li>
            <li>煽动民族仇恨、民族歧视的</li>
            <li>破坏民族团结的</li>
            <li>侮辱、滥用英烈形象的</li>
            <li>宣扬恐怖主义、极端主义的</li>
            <li>散布谣言、扰乱社会秩序的</li>
            <li>散布淫秽、色情、赌博、暴力、恐怖内容的</li>
            <li>侮辱、诽谤他人的</li>
            <li>侵犯他人合法权益的</li>
          </ul>
          
          <h4>四、知识产权</h4>
          <p>1. 本平台上的所有内容（包括但不限于文字、图片、音频、视频、软件、代码等）均受知识产权保护。</p>
          <p>2. 用户在本平台发布的原创内容，其知识产权归用户所有，但用户授予本平台免费的、非独占的使用权。</p>
          
          <h4>五、隐私保护</h4>
          <p>1. 本平台重视用户隐私保护，具体隐私政策请参阅《隐私政策》。</p>
          <p>2. 未经用户同意，本平台不会向第三方披露用户个人信息，但法律法规另有规定的除外。</p>
          
          <h4>六、免责声明</h4>
          <p>1. 本平台不对用户发布的内容的准确性、完整性负责。</p>
          <p>2. 因网络状况、通讯线路等不可抗力因素导致的服务中断，本平台不承担责任。</p>
          
          <h4>七、协议修改</h4>
          <p>本平台有权根据需要修改本协议，修改后的协议将在平台上公布。如您在协议修改后继续使用本平台，即表示您接受修改后的协议。</p>
          
          <h4>八、法律适用</h4>
          <p>本协议的解释、效力及争议的解决，适用中华人民共和国法律。如发生争议，应友好协商解决；协商不成的，任何一方均可向本平台所在地人民法院提起诉讼。</p>
        </div>
        <div class="agreement-footer">
          <button class="agree-btn" @click="showAgreement = false">我已阅读</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.dl_container {
  height: 100vh;
  background: url('/img/dt_0.0.jpg') no-repeat -155px 0 / cover;
  position: relative;
  font-family: cursive;
  overflow-x: hidden;
  overflow-y: auto;
}

.particle-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

.dl_container::-webkit-scrollbar {
  width: 8px;
}

.dl_container::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.1);
}

.dl_container::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 4px;
}

.dl_container::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.5);
}

.body {
  width: 100%;
  max-width: 1512px;
  margin: 0 auto;
  padding-top: 55px;
  padding-bottom: 80px;
  min-height: calc(100vh - 55px);
}

.dl_tubiao {
  height: 0;
  position: relative;
}

.dl_tubiao img {
  position: fixed;
  pointer-events: none;
}

.dl_zhuang_s1_ {
  position: fixed;
  top: 75px;
  z-index: 1;
  left: 150px;
  width: 180px;
  height: 350px;
}

.zc_zhuang_s2 {
  position: fixed;
  top: 440px;
  z-index: 1;
  left: 200px;
  width: 100px;
  height: 100px;
}

.dl_zhuang_s3 {
  position: fixed;
  width: 200px;
  z-index: 1;
  height: 300px;
  top: 70px;
  right: 20px;
}

.zc_zhuang_s4 {
  position: fixed;
  bottom: 250px;
  z-index: 1;
  right: -50px;
  width: 468px;
  height: 300px;
}

.dl_zhuang_s5 {
  position: fixed;
  bottom: 25px;
  z-index: 1;
  left: 20px;
  width: 200px;
  height: 300px;
}

.zc_main {
  display: flex;
  width: 100%;
  position: relative;
  text-align: center;
  justify-content: center;
  z-index: 10;
}

.zc_main .biao {
  margin-top: 40px;
  margin-bottom: 40px;
  font-size: 24px;
  z-index: 50;
  display: flex;
  background: url('/img/dt_0.jpg');
  background-size: cover;
  background-repeat: no-repeat;
  background-position: 0px 0px;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  min-width: 500px;
}

.zc_main .biao table {
  width: 100%;
}

.zc_main .biao tr td {
  color: black;
  background-color: rgba(225, 225, 225, 0.2);
  border-radius: 8px;
  padding: 6px 10px;
  white-space: nowrap;
}

.biao caption h2 {
  height: 50px;
  margin: 0px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.login-link {
  text-align: center;
  margin-top: 8px;
}

.login-link a {
  color: #007BFF;
  font-size: 14px;
  transition: color 0.3s ease;
}

.login-link a:hover {
  color: #0056b3;
  text-decoration: underline;
}



.wb {
  width: 280px;
  height: 36px;
  border: 2px solid rgba(100, 100, 100, 0.3);
  border-radius: 10px;
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
  letter-spacing: 0.5px;
  line-height: 1.5;
  padding: 0 12px;
  font-size: 16px;
  background-color: rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
  outline: none;
}

.wb:focus {
  border-color: #007BFF;
  background-color: rgba(255, 255, 255, 0.95);
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
}

.wb:hover {
  border-color: rgba(100, 100, 100, 0.5);
}



.interests-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}

.interest-item {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 6px;
  background-color: rgba(255, 255, 255, 0.6);
  transition: all 0.3s ease;
}

.interest-item:hover {
  background-color: rgba(255, 255, 255, 0.9);
}

.interest-item input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #007BFF;
}

.interest-item span {
  font-size: 14px;
  color: #333;
}

.avatar-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.avatar-preview {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 2px dashed rgba(100, 100, 100, 0.3);
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;
}

.avatar-preview:hover {
  border-color: #007BFF;
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: rgba(255, 255, 255, 0.6);
  color: #666;
  font-size: 12px;
}

.avatar-placeholder span:first-child {
  font-size: 24px;
  line-height: 1;
}

.avatar-tip {
  font-size: 12px;
  color: #999;
}

.biao .cz {
  width: 100px;
  height: 36px;
  border-radius: 8px;
  border: 2px solid rgba(100, 100, 100, 0.3);
  cursor: pointer;
  font-family: cursive;
  font-size: 16px;
  background-color: rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.biao .cz:hover {
  background-color: rgba(255, 255, 255, 0.95);
  border-color: #007BFF;
  color: #007BFF;
}

.tj {
  width: 200px;
  height: 43px;
  border-radius: 8px;
  border: none;
  font-size: 25px;
  cursor: pointer;
  font-family: cursive;
  background-color: #007BFF;
  color: white;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 123, 255, 0.3);
}

.tj:hover {
  background-color: #0056b3;
  box-shadow: 0 6px 16px rgba(0, 123, 255, 0.4);
  transform: translateY(-1px);
}

.tj:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(0, 123, 255, 0.3);
}

.tj:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  background-color: #6c757d;
  box-shadow: none;
  transform: none;
}

.w3-agree {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
}

.w3-agree input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: #007BFF;
}

.w3-agree a {
  color: #007BFF;
}

.w3-agree a:hover {
  text-decoration: underline;
}

.biao a {
  color: #007BFF;
  font-size: 16px;
  transition: color 0.3s ease;
}

.biao a:hover {
  color: #0056b3;
  text-decoration: underline;
}

input:hover {
  opacity: 1;
}





input::-webkit-input-placeholder {
  color: #000000;
  font-size: 14px;
  font-style: italic;
}

.dlzc_ditu_div {
  position: fixed;
  z-index: 50;
  bottom: 0;
  left: 0;
  right: 0;
  width: 100%;
  height: 60px;
  background: url('/img/dt_1.jpg') no-repeat 0px -360px / cover;
}

.agreement-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.agreement-content {
  background-color: #fff;
  border-radius: 10px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.agreement-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
}

.agreement-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
  line-height: 1;
}

.close-btn:hover {
  color: #333;
}

.agreement-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.agreement-body h4 {
  margin: 15px 0 10px 0;
  color: #333;
  font-size: 16px;
}

.agreement-body h4:first-child {
  margin-top: 0;
}

.agreement-body p {
  margin: 8px 0;
  line-height: 1.6;
  color: #666;
  font-size: 14px;
}

.agreement-body ul {
  margin: 10px 0;
  padding-left: 20px;
}

.agreement-body ul li {
  margin: 5px 0;
  line-height: 1.6;
  color: #666;
  font-size: 14px;
}

.agreement-footer {
  padding: 15px 20px;
  border-top: 1px solid #eee;
  text-align: center;
}

.agree-btn {
  background-color: #007BFF;
  color: white;
  border: none;
  padding: 10px 40px;
  border-radius: 5px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.agree-btn:hover {
  background-color: #0056b3;
}
</style>
