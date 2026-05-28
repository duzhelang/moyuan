<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const currentTime = ref('')

const form = reactive({
  username: '',
  password: ''
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

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login({
      username: form.username,
      password: form.password
    })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect as string
    router.push(redirect || '/')
  } catch (error) {
    ElMessage.error('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

const goToRegister = () => {
  router.push('/user/register')
}

const goToAdmin = () => {
  router.push('/admin')
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
      <hr class="hr"/>
      <hr />
    </div>

    <div class="body">
      <div class="dl_tubiao">
        <img src="/img/qyc.png" class="dl_zhuang_s1"/>
        <img src="/img/010.png" class="dl_zhuang_s2"/>
        <img src="/img/fy_tubiao (5).png" class="dl_zhuang_s3"/>
        <img src="/img/tuobiao (3).png" class="dl_zhuang_s4"/>
        <img src="/img/fy_tubiao (10).png" class="dl_zhuang_s5"/>
      </div>

      <div class="dl_zhuti">
        <div class="zhong">
          <div class="dlzt_tx_div">
            <img src="/img/00.jpg"/>
            <input type="text" placeholder="输入用户名" v-model="form.username">
            <div class="register-link">
              <a class="fuzhu" href="javascript:void(0)" @click="goToRegister">还没有账号?注册</a>
            </div>
          </div>

          <div class="zhong_shuxian"></div>
          <div class="dlzt_wz_div">
            <h1>登&nbsp;&nbsp;&nbsp;&nbsp;录</h1>
            <form @submit.prevent="handleLogin">
              <div class="a1">
                <label for="username" class="">用户名:</label>
                <input type="text" id="username" class="dl_shurukuang" placeholder="请输入注册手机号" v-model="form.username">
              </div>
              <div class="a2">
                <label for="password">密&nbsp;&nbsp;码:</label>
                <input type="password" id="password" class="dl_shurukuang" placeholder="请输入密码" required v-model="form.password">
              </div>
              <div class="a3">
                <button class="dl_anjian" type="submit" :disabled="loading">
                  {{ loading ? '登录中...' : '登录' }}
                </button>
              </div>
            </form>
            <div class="fu">
              <input type="button" value="管理员入口" title="管理员入口" @click="goToAdmin">
            </div>
          </div>
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

.dl_zhuang_s1 {
  position: absolute;
  top: 75px;
  z-index: 10;
  left: 150px;
  width: 1200px;
  height: 400px;
}

.dl_zhuang_s2 {
  position: absolute;
  top: 35px;
  z-index: 10;
  left: -20px;
  width: 300px;
  height: 200px;
}

.dl_zhuang_s3 {
  position: absolute;
  width: 200px;
  z-index: 10;
  height: 300px;
  bottom: 50px;
  left: 1328px;
}

.dl_zhuang_s4 {
  position: absolute;
  top: 400px;
  z-index: 10;
  left: 0px;
  width: 503px;
  height: 300px;
}

.dl_zhuang_s5 {
  position: absolute;
  bottom: 20px;
  z-index: 10;
  width: 200px;
  height: 300px;
}

.dl_zhuti {
  margin: 100px 10px;
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  z-index: 50;
}

.zhong img {
  width: 430px;
  height: 280px;
}

.zhong h1 {
  font-size: 50px;
  font-weight: 600;
  margin: 10px auto;
  display: flex;
  justify-content: center;
}

.zhong {
  backdrop-filter: blur(1.5px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.2);
  width: 600px;
  height: 380px;
  border-radius: 27px;
  background-color: rgba(227,196,165,0.7);
  background-size: cover;
  background-repeat: no-repeat;
  background-position: 0px 0px;
  display: flex;
  align-items: flex-start;
}

.zhong .dlzt_tx_div {
  display: flex;
  flex-direction: column;
  justify-content: space-around;
  align-items: center;
  width: 35%;
  height: 40%;
  position: relative;
  top: -30px;
}

.zhong .dlzt_tx_div > img {
  display: block;
  border-radius: 50%;
  margin: 10px;
  width: 80px;
  height: 80px;
  position: relative;
  z-index: 50;
}

.zhong .dlzt_tx_div > input {
  display: block;
  border: 1px solid #ccc;
  border-radius: 35px 0 35px 0;
  padding: 2px 30px;
  width: 75px;
  height: 30px;
  color: #ffffff;
  background-color: rgba(255, 255, 255, 0.5);
  font-size: 20px;
  font-family: cursive;
  outline: none;
}

.register-link {
  font-size: 20px;
  text-align: center;
  margin-top: 0px;
  position: absolute;
  bottom: -100px;
  right: 60px;
}

.register-link .fuzhu {
  background-color: rgba(204, 204, 204, 0.7);
  line-height: 0px;
  text-shadow: 0 0 10px white;
  color: #55aaff;
  cursor: pointer;
}

.register-link .fuzhu:hover {
  text-decoration: none;
  color: #007BFF;
}

.zhong_shuxian {
  width: 1px;
  height: 300px;
  background-color: #e0e0e0;
}

.zhong .dlzt_wz_div {
  width: 55%;
}

.zhong .dlzt_wz_div h1 {
  text-shadow: 8px 2px 3px rgba(254, 254, 254, 0.4);
  color: #333;
  font-size: 50px;
  font-weight: 600;
  margin: 10px auto 25px;
  display: flex;
  justify-content: center;
}

.dl_shurukuang {
  width: 250px;
  height: 33px;
  border-radius: 10px;
  border: none;
  background-color: rgba(255, 255, 255, 0.5);
  font-family: cursive;
  padding: 0 5px;
  font-size: 16px;
  outline: none;
}

.a1, .a2, .a3 {
  height: 50px;
  font-size: 20px;
  display: flex;
  align-items: center;
}

.a1 label, .a2 label {
  width: 70px;
  text-align: right;
  margin-right: 10px;
  font-size: 20px;
  font-family: cursive;
}

.a3 {
  padding-left: 80px;
}

.dl_anjian {
  display: flex;
  width: 200px;
  height: 43px;
  font-size: 25px;
  padding: 5px;
  border-radius: 5px;
  background-color: #007BFF;
  color: white;
  border: none;
  cursor: pointer;
  justify-content: center;
  align-items: center;
  font-family: cursive;
}

.dl_anjian:hover {
  background-color: #0056b3;
}

.dl_anjian:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.fu input {
  width: 80px;
  font-size: 24px;
  background: #9acd32;
  color: rgb(100,100,100);
  margin-top: 15px;
  float: right;
  border: 2px solid gainsboro;
  border-radius: 20px;
  cursor: pointer;
  font-family: cursive;
  padding: 0 5px;
}

.fu input:hover {
  background: #779d26;
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
