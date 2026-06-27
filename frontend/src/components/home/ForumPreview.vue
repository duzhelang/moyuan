<template>
  <div class="forum_poem_layout">
    <div class="luntan" id="luntan">
      <div class="luntan_title">
        <h1>论 坛</h1>
        <h2>沟通 | 分享 | 探索 | 成长</h2>
      </div>
      <div class="luntan_ss">
        <div class="sy_ss_0_lt">
          <input type="text" class="sy_ss_lt" placeholder="搜索你感兴趣的内容">
          <div class="sy_ss_tp_lt">
            <img :src="asset('/img/tuobiao_4.png')" alt="">
          </div>
        </div>
      </div>
      <div class="luntan_gundong">
        <div class="marquee-content">
          论坛是我们心灵的栖息地，在诗词的韵律中找到共鸣，在分享与探索中共同成长！
        </div>
      </div>
      <div class="luntan_anniu">
        <ul>
          <li><router-link to="/forum">首页</router-link></li>
          <li><router-link to="/forum">全部</router-link></li>
          <li><router-link to="/forum">热帖</router-link></li>
          <li><router-link to="/forum">精华</router-link></li>
          <li><router-link to="/forum">畅言</router-link></li>
        </ul>
      </div>
      <div class="luntan_pin_layout">
        <div class="luntan_pin_top">
          <div class="jxh_left">
            <div class="jxh_left_title">
              <span>精选板块</span>
            </div>
            <div class="jxh_left_nr">
              <div class="jxh_left_nr0">
                <router-link to="/forum">
                  <img :src="asset('/img/lt_jx_1.jpg')" alt="">
                </router-link>
              </div>
              <div class="jxh_left_nr1">
                <router-link to="/forum">
                  <span class="jxh_left_nr1_title">古诗词鉴赏</span>
                </router-link>
                <span class="jxh_left_nr1_d">古诗词鉴赏是一个深邃而美妙的领域，它带领我们穿越时光的长河，品味那些流传千年的文字之美。</span>
              </div>
            </div>
            <div class="jxh_left_nr">
              <div class="jxh_left_nr0">
                <router-link to="/forum">
                  <img :src="asset('/img/lt_jx_2.jpg')" alt="">
                </router-link>
              </div>
              <div class="jxh_left_nr1">
                <router-link to="/forum">
                  <span class="jxh_left_nr1_title">古诗词分享</span>
                </router-link>
                <span class="jxh_left_nr1_d">诗词分享是一座桥梁，连接着古今的心灵，让我们共同品味诗词之美，传承文化之韵。</span>
              </div>
            </div>
            <div class="jxh_left_nr">
              <div class="jxh_left_nr0">
                <router-link to="/forum">
                  <img :src="asset('/img/lt_jx_3.jpg')" alt="">
                </router-link>
              </div>
              <div class="jxh_left_nr1">
                <router-link to="/forum">
                  <span class="jxh_left_nr1_title">新星闪耀</span>
                </router-link>
                <span class="jxh_left_nr1_d">新一代青年笔下流淌着千年的韵律，以古风新韵续写着不朽的篇章。</span>
              </div>
            </div>
          </div>
          <div class="jxh_right">
            <div class="jxh_right_title">
              <span>畅言寓所</span>
            </div>
            <div
              v-for="post in posts.slice(0, 3)"
              :key="post.id"
              class="jxh_right_nr"
              @click="router.push(`/forum/${post.id}`)"
            >
              <span class="jxh_right_nr_title">{{ post.title }}</span>
              <span class="jxh_right_nr_d">{{ post.username }} · {{ post.viewCount }}阅读</span>
            </div>
            <div v-if="posts.length === 0" class="jxh_right_nr">
              <span class="jxh_right_nr_title">欢迎来到论坛交流</span>
              <span class="jxh_right_nr_d">暂无帖子</span>
            </div>
            <router-link to="/forum" class="jxh_right_anniu">
              <span>查看更多</span>
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { ForumPost } from '@/types/model'

defineProps<{
  posts: ForumPost[]
}>()

const router = useRouter()

const asset = (path: string) => path
</script>

<style scoped>
.forum_poem_layout {
  display: flex;
  flex-wrap: wrap;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  margin: 0 auto;
  position: relative;
}

.luntan {
  display: flex;
  flex-wrap: wrap;
  flex-direction: column;
  position: relative;
  width: 60%;
  margin: 0 auto;
  background-color: rgba(255, 252, 245, 0.92);
  backdrop-filter: blur(12px);
  border-radius: 15px;
  border: 1px solid rgba(139, 69, 19, 0.12);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  overflow: visible;
  z-index: 1;
}

.luntan_title {
  width: 100%;
  text-align: center;
  padding: 10px 0;
}

.luntan_title h1 {
  font-size: 40px;
  color: #333;
  margin: 10px 0 5px;
}

.luntan_title h2 {
  font-size: 18px;
  color: #666;
  margin: 0;
}

.luntan_ss {
  width: 100%;
  display: flex;
  justify-content: center;
  padding: 10px 0;
}

.sy_ss_0_lt {
  display: flex;
  align-items: center;
  width: 400px;
}

.sy_ss_lt {
  flex: 1;
  height: 30px;
  padding: 0 15px;
  border-radius: 20px 0 0 20px;
  font-size: 14px;
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
  letter-spacing: 0.5px;
  line-height: 1.5;
  background-color: rgba(100, 100, 100, 0.5);
  color: #fff;
  border: none;
}

.sy_ss_lt::-webkit-input-placeholder {
  color: rgba(250, 250, 250, 0.8);
  font-size: 14px;
}

.sy_ss_tp_lt {
  width: 35px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #4CAF50;
  border-radius: 0 20px 20px 0;
  cursor: pointer;
}

.sy_ss_tp_lt img {
  width: 20px;
  height: 20px;
}

.luntan_gundong {
  width: 100%;
  font-size: 18px;
  color: yellow;
  background-color: rgba(100, 100, 100, 0.5);
  padding: 5px 0;
  overflow: hidden;
  white-space: nowrap;
}

.marquee-content {
  display: inline-block;
  animation: marquee-scroll 15s linear infinite;
}

@keyframes marquee-scroll {
  0% {
    transform: translateX(100%);
  }
  100% {
    transform: translateX(-100%);
  }
}

.luntan_anniu {
  width: 100%;
  display: flex;
  justify-content: center;
  padding: 10px 0;
}

.luntan_anniu ul {
  display: flex;
  gap: 10px;
}

.luntan_anniu ul li {
  list-style: none;
}

.luntan_anniu ul li a {
  display: block;
  padding: 8px 20px;
  background-color: rgba(100, 100, 100, 0.75);
  color: wheat;
  border-radius: 5px;
  font-size: 16px;
  text-decoration: none;
  transition: all 0.3s;
}

.luntan_anniu ul li a:hover {
  background-color: rgba(220, 220, 220, 0.8);
  color: #333;
}

.luntan_pin_layout {
  display: flex;
  flex-direction: column;
  width: 100%;
  gap: 20px;
}

.luntan_pin_top {
  display: flex;
  width: 100%;
  padding: 15px;
  gap: 20px;
}

.jxh_left {
  flex: 2;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.jxh_left_title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  padding-bottom: 8px;
  border-bottom: 2px solid #ccc;
}

.jxh_left_nr {
  display: flex;
  gap: 15px;
  padding: 10px;
  border: 1px solid gainsboro;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s;
}

.jxh_left_nr:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.jxh_left_nr0 img {
  width: 200px;
  height: 150px;
  border-radius: 7px;
  object-fit: cover;
}

.jxh_left_nr1 {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.jxh_left_nr1_title {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.jxh_left_nr1_d {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  overflow: hidden;
}

.jxh_right {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.jxh_right_title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  padding-bottom: 8px;
  border-bottom: 2px solid #ccc;
}

.jxh_right_nr {
  display: flex;
  flex-direction: column;
  padding: 10px;
  border: 1px solid gainsboro;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.jxh_right_nr:hover {
  background-color: rgba(227, 196, 165, 0.3);
}

.jxh_right_nr_title {
  font-size: 15px;
  color: #333;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.jxh_right_nr_d {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.jxh_right_anniu {
  display: flex;
  justify-content: center;
  margin-top: 10px;
}

.jxh_right_anniu span {
  padding: 8px 20px;
  background-color: #007BFF;
  color: white;
  border-radius: 5px;
  text-decoration: none;
  font-size: 14px;
  transition: background-color 0.3s;
}

.jxh_right_anniu span:hover {
  background-color: #0056b3;
}
</style>
