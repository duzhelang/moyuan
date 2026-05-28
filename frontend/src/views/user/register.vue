<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const currentTime = ref('')

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  gender: '',
  birthYear: '',
  college: '',
  major: '',
  interests: [] as string[],
  email: '',
  phone: '',
  agreement: false
})

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(() => {
  updateTime()
  setInterval(updateTime, 1000)
})

const handleReset = () => {
  form.username = ''
  form.password = ''
  form.confirmPassword = ''
  form.gender = ''
  form.birthYear = ''
  form.college = ''
  form.major = ''
  form.interests = []
  form.email = ''
  form.phone = ''
  form.agreement = false
}

const handleSubmit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次输入密码不一致')
    return
  }
  loading.value = true
  try {
    await userStore.login({
      username: form.username,
      password: form.password
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
    <div class="header">
      <div class="top_genglu">
        <div class="logo" title="首页">
          <a href="/" target="_blank">
            <img src="/img/tubiao (1).jpg" />
            <div class="txt">墨渊</div>
          </a>
        </div>
        <div class="top_txt">
          <div class="shijian" title="现在时间">
            {{ currentTime }}
          </div>
          <div class="txt1">了解我们
            <div class="erw">
              <img src="/img/微信二维.jpg" />
            </div>
          </div>
          <div class="txt2">联系我们
            <div class="erw">
              <img src="/img/微信二维.jpg" />
            </div>
          </div>
        </div>
      </div>
      <hr class="hr" />
      <hr />
    </div>

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
          <table border="0" cellspacing="2" align="center">
          <form @submit.prevent="handleSubmit">
              <caption>
                <h2>信&nbsp;&nbsp;&nbsp;&nbsp;息</h2>
              </caption>
              <tr>
                <td align="right" width="200px">用户名：</td>
                <td><input type="text" class="wb" placeholder="请输入" v-model="form.username"></td>
              </tr>
              <tr>
                <td align="right">密码：</td>
                <td><input type="password" class="lock" name="password" placeholder="密码" id="password1" required="" v-model="form.password"></td>
              </tr>
              <tr>
                <td align="right">确认密码：</td>
                <td><input type="password" class="lock" name="confirm-password" placeholder="确认密码" id="password2" required="" v-model="form.confirmPassword"></td>
              </tr>
              <tr>
                <td align="right">性别：</td>
                <td>
                  <input type="radio" name="性别" class="xb" value="男" v-model="form.gender" />男
                  <input type="radio" name="性别" class="xb" value="女" v-model="form.gender" />女
                </td>
              </tr>
              <tr>
                <td align="right">出生年份：</td>
                <td>
                  <select v-model="form.birthYear">
                    <option value="">-请选择-</option>
                    <option>2020</option>
                    <option>2021</option>
                    <option>2022</option>
                    <option>2023</option>
                  </select>
                </td>
              </tr>
              <tr>
                <td align="right">学院：</td>
                <td>
                  <select v-model="form.college">
                    <option value="">-请选择-</option>
                    <option>传媒技术学院</option>
                    <option>广播电视学院</option>
                    <option>播音主持艺术学院</option>
                    <option>国际传播学院</option>
                    <option>文化管理学院</option>
                    <option>新闻传播学院</option>
                    <option>戏剧影视学院</option>
                    <option>美术与设计学院</option>
                    <option>动画与数字艺术学院</option>
                    <option>舞蹈学院</option>
                    <option>音乐学院</option>
                    <option>电竞学院</option>
                    <option>继续教育学院</option>
                    <option>国际学院</option>
                    <option>马克思主义学院</option>
                    <option>摄影学院</option>
                  </select>
                </td>
              </tr>
              <tr>
                <td align="right">专业：</td>
                <td>
                  <select v-model="form.major">
                    <option value="">-请选择-</option>
                    <option>计算机科学</option>
                    <option>电子信息类</option>
                  </select>
                </td>
              </tr>
              <tr>
                <td align="right">兴趣：</td>
                <td>
                  <input type="checkbox" class="xq" value="古典" v-model="form.interests" />古典
                  <input type="checkbox" class="xq" value="现代" v-model="form.interests" />现代
                  <input type="checkbox" class="xq" value="自由体" v-model="form.interests" />自由体
                  <input type="checkbox" class="xq" value="外国" v-model="form.interests" />外国
                </td>
              </tr>
              <tr>
                <td align="right">邮箱：</td>
                <td><input type="email" name="email" value="" placeholder="请输入" class="wb" v-model="form.email" /></td>
              </tr>
              <tr>
                <td align="right">电话：</td>
                <td><input type="text" placeholder="请输入" class="wb" required v-model="form.phone" /></td>
              </tr>
              <tr>
                <td align="right">
                  <a target="_blank" href="javascript:void(0)" title="查看详情">点击查看条约</a>
                </td>
                <td>
                  <div class="w3-agree">
                    <input type="checkbox" id="c1" name="cc" v-model="form.agreement">
                    <label class="agileits-agree">我同意这项<a href="javascript:void(0)">条款和条件</a></label>
                    <div class="clear"></div>
                  </div>
                </td>
              </tr>
              <tr style="height: 55px;">
                <td align="right"><input type="button" value="重置" class="cz" @click="handleReset" /></td>
                <td align="center"><input type="submit" value="提交" class="tj" :disabled="loading" /></td>
              </tr>
          </form>
          </table>
        </div>
      </div>
    </div>

    <div class="dlzc_ditu_div"></div>
  </div>
</template>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.dl_container {
  min-height: 100vh;
  background: url('/img/dt_0.0.jpg') no-repeat -155px 0 / cover;
  position: relative;
  font-family: cursive;
}

.header {
  width: 100vw;
  height: 60px;
  text-decoration: none;
  border-top-left-radius: 0px;
  border-top-right-radius: 0px;
  border-bottom-right-radius: 50px;
  border-bottom-left-radius: 5px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  background: url('/img/dt_3.jpg');
  background-size: cover;
  background-repeat: no-repeat;
  background-position: 0px -320px;
  box-shadow: 0 2px 5px rgba(0,0,0,.2);
  z-index: 51;
}

.header hr {
  border: none;
  height: 1px;
  background-color: #dedede;
  margin-top: -2px;
  margin-bottom: -3px;
}

.header .hr {
  border: none;
  height: 1px;
  background-color: #303030;
  margin-top: -11px;
  margin-bottom: -3px;
}

.header .top_genglu {
  width: 100%;
  height: 50px;
  display: flex;
  justify-content: space-between;
}

.header .top_genglu .logo {
  font-size: 34px;
  color: #000;
  vertical-align: middle;
}

.header .top_genglu .logo a {
  font-size: 34px;
  color: #000;
  vertical-align: middle;
  display: flex;
  text-align: center;
  text-decoration: none;
}

.header .top_genglu .logo .txt {
  display: flex;
  align-items: center;
}

.header .top_genglu .logo img {
  width: 50px;
  height: 50px;
  float: left;
}

.top_genglu .top_txt {
  display: flex;
  align-items: center;
  margin-right: 20px;
  font-size: 25px;
}

.header .top_genglu .shijian {
  color: white;
  background-color: rgba(200, 200, 200, 0.3);
  margin-right: 50px;
  font-size: 14px;
  padding: 2px 10px;
}

.header .top_genglu .txt1 {
  width: 90px;
  float: right;
  color: whitesmoke;
  font-size: 20px;
  position: relative;
  z-index: 50;
  cursor: pointer;
}

.header .top_genglu .txt2 {
  width: 90px;
  float: right;
  color: whitesmoke;
  font-size: 20px;
  position: relative;
  z-index: 50;
  cursor: pointer;
}

.top_genglu .erw img {
  width: 100px;
  height: 100px;
  float: left;
}

.header .top_genglu .txt1 .erw {
  display: none;
  width: 100px;
  height: 100px;
  margin-left: -12px;
  overflow: hidden;
  position: absolute;
  z-index: 50;
  top: 100%;
  left: 0;
}

.header .top_genglu .txt2 .erw {
  display: none;
  width: 100px;
  height: 100px;
  margin-left: -12px;
  overflow: hidden;
  position: absolute;
  z-index: 50;
  top: 100%;
  left: 0;
}

.header .top_genglu .txt1:hover .erw {
  display: block;
  background: blue;
}

.header .top_genglu .txt1:hover {
  font-size: 21px;
}

.header .top_genglu .txt2:hover .erw {
  display: block;
  background: blue;
}

.header .top_genglu .txt2:hover {
  font-size: 22px;
}

.body {
  width: 1512.8px;
  overflow-x: hidden;
  margin: 0 auto;
  padding-top: 60px;
}

.dl_tubiao {
  height: 1px;
  position: relative;
}

.dl_tubiao img {
  position: absolute;
}

.dl_zhuang_s1_ {
  position: absolute;
  top: 90px;
  z-index: 10;
  left: 220px;
  width: 180px;
  height: 350px;
}

.zc_zhuang_s2 {
  position: absolute;
  top: 410px;
  z-index: 10;
  left: 280px;
  width: 100px;
  height: 100px;
}

.dl_zhuang_s3 {
  position: absolute;
  width: 200px;
  z-index: 10;
  height: 300px;
  bottom: 50px;
  left: 1328px;
}

.zc_zhuang_s4 {
  position: absolute;
  top: 400px;
  z-index: 10;
  left: 1060px;
  width: 468px;
  height: 300px;
}

.dl_zhuang_s5 {
  position: absolute;
  bottom: 20px;
  z-index: 10;
  width: 200px;
  height: 300px;
}

.zc_main {
  display: flex;
  width: 100%;
  position: relative;
  text-align: center;
  justify-content: center;
}

.zc_main .biao {
  margin-top: 75px;
  font-size: 26px;
  z-index: 50;
  display: flex;
  background: url('/img/dt_0.jpg');
  background-size: cover;
  background-repeat: no-repeat;
  background-position: 0px 0px;
  border-radius: 10px;
}

.zc_main .biao table {
  width: 100%;
}

.zc_main .biao tr td {
  color: black;
  background-color: rgba(225, 225, 225, 0.2);
  border-radius: 8px;
  padding: 4px 8px;
}

.biao caption h2 {
  height: 50px;
  margin: 0px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.biao select {
  width: 260px;
  height: 23px;
  border-radius: 5px;
  border: none;
  background-color: rgba(100, 100, 100, 0.6);
  font-family: cursive;
}

.wb {
  width: 300px;
  height: 30px;
  border: none;
  border-radius: 10px;
  font-family: cursive;
  padding: 0 5px;
  font-size: 16px;
}

.lock {
  width: 300px;
  height: 30px;
  border: none;
  border-radius: 10px;
  font-family: cursive;
  padding: 0 5px;
  font-size: 16px;
  background-color: transparent;
}

.biao .xb {
  width: 50px;
  height: 23px;
  border-radius: 5px;
  border: none;
  background-color: rgba(100, 100, 100, 0.6);
}

.biao .xq {
  width: 40px;
  height: 23px;
  border-radius: 5px;
  border: none;
  background-color: rgba(100, 100, 100, 0.6);
}

.biao .cz {
  width: 100px;
  height: 33px;
  border-radius: 5px;
  border: none;
  cursor: pointer;
  font-family: cursive;
  font-size: 16px;
}

.tj {
  width: 200px;
  height: 43px;
  border-radius: 5px;
  border: none;
  font-size: 25px;
  cursor: pointer;
  font-family: cursive;
  background-color: #007BFF;
  color: white;
}

.tj:hover, .cz:hover {
  background-color: aliceblue;
}

.tj:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.w3-agree {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 16px;
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
}

.biao a:hover {
  color: #007BFF;
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
  position: absolute;
  z-index: 50;
  bottom: 0;
  width: 100%;
  height: 60px;
  background: url('/img/dt_1.jpg') no-repeat 0px -360px / cover;
}
</style>
