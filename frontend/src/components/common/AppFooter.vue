<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
const asset = (path: string) => path

const currentYear = new Date().getFullYear()

const navigateTo = (path: string) => {
  router.push(path)
}

const openExternalLink = (url: string) => {
  window.open(url, '_blank')
}
</script>

<template>
  <footer class="dibu">
    <div class="dibu-container">
      <div class="dibu-top">
        <div class="dibu-brand">
          <div class="dibu-logo">
            <img :src="asset('/img/tubiao (2).png')" alt="古诗词汇">
            <span class="dibu-title">古诗词汇</span>
          </div>
          <p class="dibu-desc">传承千年文脉，品味诗词之美。让古典诗词在数字时代焕发新生。</p>
          <div class="dibu-social">
            <div class="social-icon" title="微信">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 01.213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 00.167-.054l1.903-1.114a.864.864 0 01.717-.098 10.16 10.16 0 002.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 01-1.162 1.178A1.17 1.17 0 014.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 01-1.162 1.178 1.17 1.17 0 01-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 01.598.082l1.584.926a.272.272 0 00.14.047c.134 0 .24-.11.24-.245 0-.06-.023-.12-.038-.177l-.327-1.233a.582.582 0 01-.023-.156.49.49 0 01.201-.398C23.024 18.48 24 16.82 24 14.98c0-3.21-2.931-5.837-7.062-6.122zm-2.18 2.768c.535 0 .969.44.969.982a.976.976 0 01-.969.983.976.976 0 01-.969-.983c0-.542.434-.982.97-.982zm4.844 0c.535 0 .969.44.969.982a.976.976 0 01-.969.983.976.976 0 01-.969-.983c0-.542.434-.982.97-.982z"/>
              </svg>
            </div>
            <div class="social-icon" title="微博">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                <path d="M10.098 20.323c-3.977.391-7.414-1.406-7.672-4.02-.259-2.609 2.759-5.047 6.74-5.441 3.979-.394 7.413 1.404 7.671 4.018.259 2.6-2.759 5.049-6.739 5.443zM9.05 17.219c-.384.616-1.208.884-1.829.602-.612-.279-.793-.991-.406-1.593.379-.595 1.176-.861 1.793-.583.631.275.825.985.442 1.574zm1.27-1.627c-.141.237-.449.353-.689.253-.236-.09-.307-.362-.158-.59.141-.229.444-.346.685-.246.242.1.319.362.162.583zm.176-2.719c-1.893-.494-4.038.45-4.857 2.118-.836 1.704-.026 3.591 1.886 4.21 1.983.642 4.318-.341 5.132-2.179.8-1.793-.201-3.642-2.161-4.149zm7.563-1.224c-.346-.105-.583-.176-.405-.636.39-1.004.431-1.871.006-2.487-.804-1.172-3.006-1.109-5.578-.033 0 0-.798.351-.594-.285.393-1.227.334-2.249-.277-2.808-1.386-1.27-5.08.048-8.248 2.948C2.345 11.257 1 13.97 1 16.332c0 4.559 5.852 7.337 11.569 7.337 7.498 0 12.499-4.345 12.499-7.825 0-2.093-1.77-3.285-3.006-3.676zM20.875 8.25c.738-.825 1.049-1.93.775-3.048-.466-1.897-2.377-3.06-4.273-2.607-1.896.453-3.067 2.377-2.607 4.273.274 1.118 1.165 1.972 2.225 2.229.279.069.564.101.852.101 1.043 0 2.027-.49 2.663-1.284l.365-.664z"/>
              </svg>
            </div>
            <div class="social-icon" title="GitHub">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                <path d="M12 0C5.374 0 0 5.373 0 12c0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23A11.509 11.509 0 0112 5.803c1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576C20.566 21.797 24 17.3 24 12c0-6.627-5.373-12-12-12z"/>
              </svg>
            </div>
          </div>
        </div>

        <div class="dibu-nav">
          <div class="dibu-nav-section">
            <h4>诗词探索</h4>
            <ul>
              <li @click="navigateTo('/poem')">诗词库</li>
              <li @click="navigateTo('/poet')">诗人风采</li>
              <li @click="navigateTo('/forum')">诗词论坛</li>
              <li @click="navigateTo('/poem')">诗词搜索</li>
            </ul>
          </div>

          <div class="dibu-nav-section">
            <h4>智能体验</h4>
            <ul>
              <li @click="navigateTo('/')">看图写诗</li>
              <li @click="navigateTo('/')">智能解析</li>
              <li @click="navigateTo('/')">诗词问答</li>
            </ul>
          </div>

          <div class="dibu-nav-section">
            <h4>文化资源</h4>
            <ul>
              <li @click="openExternalLink('https://www.gushiwen.org/')">古诗文网</li>
              <li @click="openExternalLink('https://www.zhonghuadiancang.com/')">中华典藏</li>
              <li @click="openExternalLink('https://ctext.org/')">中国哲学书电子化计划</li>
            </ul>
          </div>
        </div>
      </div>

      <div class="dibu-divider"></div>

      <div class="dibu-bottom">
        <div class="dibu-copyright">
          <span>© {{ currentYear }} 古诗词汇 版权所有</span>
          <span class="dibu-separator">|</span>
          <span>传承经典 · 品味诗词</span>
        </div>
        <div class="dibu-links">
          <span class="dibu-link" @click="navigateTo('/')">关于我们</span>
          <span class="dibu-separator">|</span>
          <span class="dibu-link" @click="navigateTo('/')">使用条款</span>
          <span class="dibu-separator">|</span>
          <span class="dibu-link" @click="navigateTo('/')">隐私政策</span>
          <span class="dibu-separator">|</span>
          <span class="dibu-link" @click="navigateTo('/')">联系我们</span>
        </div>
      </div>
    </div>
  </footer>
</template>

<style scoped lang="scss">
.dibu {
  width: 100%;
  background: linear-gradient(135deg, #5a3e2b 0%, #6b4c36 50%, #5a3e2b 100%);
  color: #f0e4d7;
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
  padding: 0;
  margin-top: 30px;
  overflow: hidden;
  position: relative;
  z-index: 10;
  border: none;
  border-radius: 0;
  float: none;
  clear: both;

  :deep(div) {
    width: auto;
    height: auto;
    float: none;
  }
}

.dibu-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 25px 40px 20px;
}

.dibu-top {
  display: flex;
  gap: 60px;
  margin-bottom: 30px;
  justify-content: center;
  align-items: flex-start;
}

.dibu-brand {
  flex: 0 0 auto;
  max-width: 280px;
}

.dibu-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;

  img {
    width: 40px;
    height: 40px;
    border-radius: 8px;
    flex-shrink: 0;
  }

  .dibu-title {
    font-size: 20px;
    font-weight: 600;
    color: #f8f0e6;
    letter-spacing: 2px;
    white-space: nowrap;
  }
}

.dibu-desc {
  font-size: 14px;
  line-height: 1.8;
  color: #d9c7b2;
  margin-bottom: 25px;
}

.dibu-social {
  display: flex;
  gap: 15px;
}

.social-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  flex-shrink: 0;

  &:hover {
    background: rgba(212, 175, 135, 0.4);
    transform: translateY(-2px);
  }

  svg {
    color: #f0e4d7;
    width: 18px;
    height: 18px;
  }
}

.dibu-nav {
  flex: 1;
  display: flex;
  justify-content: space-between;
  gap: 40px;
}

.dibu-nav-section {
  h4 {
    font-size: 16px;
    font-weight: 600;
    color: #f8f0e6;
    margin: 0 0 20px;
    padding: 5px 0 10px;
    position: relative;

    &::after {
      content: '';
      position: absolute;
      left: 0;
      bottom: 0;
      width: 30px;
      height: 2px;
      background: linear-gradient(90deg, #d4af87, transparent);
    }
  }

  ul {
    list-style: none;
    margin: 0;
    padding: 0;
  }

  li {
    font-size: 14px;
    color: #d9c7b2;
    line-height: 2.5;
    cursor: pointer;
    transition: all 0.3s ease;
    padding-left: 10px;

    &:hover {
      color: #f8f0e6;
      transform: translateX(5px);
    }
  }
}

.dibu-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(212, 175, 135, 0.4), transparent);
  margin-bottom: 20px;
}

.dibu-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
}

.dibu-copyright {
  font-size: 13px;
  color: #b8a898;
}

.dibu-separator {
  margin: 0 10px;
  color: #7a6a5a;
}

.dibu-links {
  display: flex;
  align-items: center;
}

.dibu-link {
  font-size: 13px;
  color: #b8a898;
  cursor: pointer;
  transition: color 0.3s ease;

  &:hover {
    color: #d4af87;
  }
}

@media (max-width: 992px) {
  .dibu-top {
    flex-direction: column;
    gap: 30px;
  }

  .dibu-brand {
    flex: none;
    text-align: center;
  }

  .dibu-logo {
    justify-content: center;
  }

  .dibu-social {
    justify-content: center;
  }

  .dibu-nav {
    flex-wrap: wrap;
    justify-content: center;
    gap: 30px;
  }

  .dibu-nav-section {
    text-align: center;

    h4::after {
      left: 50%;
      transform: translateX(-50%);
    }

    li {
      padding-left: 0;
    }
  }
}

@media (max-width: 576px) {
  .dibu-container {
    padding: 30px 20px 20px;
  }

  .dibu-nav {
    flex-direction: column;
    align-items: center;
    gap: 25px;
  }

  .dibu-bottom {
    flex-direction: column;
    text-align: center;
  }

  .dibu-links {
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>
