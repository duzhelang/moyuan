<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

let now = 1
const max = 6
let nowShi = 1
const maxShi = 7
const scrollStep = 295

const currentTime = ref('')

const imgLoopShow = (di: number) => {
  now = di
  updateImage()
}

const updateImage = () => {
  const adScroll = document.getElementById('ad_scroll') as HTMLImageElement
  if (adScroll) {
    adScroll.src = `/img/lb_ (${now}).jpg`
  }
  for (let i = 1; i <= max; i++) {
    const li = document.getElementById('li' + i)
    if (li) {
      li.style.color = now === i ? 'rgb(249,255,0)' : 'black'
    }
  }
}

const prevImage = () => {
  now--
  if (now < 1) now = max
  updateImage()
}

const nextImage = () => {
  now++
  if (now > max) now = 1
  updateImage()
}

const shiwenLoopShow = (diji: number) => {
  nowShi = diji
  const adScrollShiwen = document.getElementById('ad_scroll_shiwen') as HTMLImageElement
  if (adScrollShiwen) {
    adScrollShiwen.src = `/img/lb_shiwen (${nowShi}).png`
  }
  for (let i = 1; i <= maxShi; i++) {
    const el = document.getElementById('l' + i)
    if (el) {
      el.style.backgroundColor = nowShi === i ? 'rgb(249,255,0)' : '#bfbcbc'
    }
  }
  if (nowShi >= maxShi) {
    nowShi = 0
  }
}

const showTime = () => {
  const date = new Date()
  currentTime.value = date.toLocaleString()
}

const toggleMusic = () => {
  const audio = document.getElementById('myAudio_zhu') as HTMLAudioElement
  if (audio) {
    if (audio.paused || audio.ended) {
      audio.play()
    } else {
      audio.pause()
    }
  }
}

const handleSousuoSubmit = (e: Event) => {
  e.preventDefault()
  window.location.href = 'https://wenku.baidu.com/ndlaunch/browse/chat?noAutoSend=1&fr=launch_ad&utm_source=bingss-WD&utm_medium=cpa&utm_account=SS-bingtg10'
  return false
}

const handleLuntanSousuoSubmit = (e: Event) => {
  e.preventDefault()
  window.location.href = 'https://wenku.baidu.com/ndlaunch/browse/chat?noAutoSend=1&fr=launch_ad&utm_source=bingss-WD&utm_medium=cpa&utm_account=SS-bingtg10'
  return false
}

const handleShiSousuoSubmit = (e: Event) => {
  e.preventDefault()
  window.location.href = 'https://www.gushici.net/zuozhe/tang/'
  return false
}

const handleImageUpload = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) {
    const reader = new FileReader()
    reader.onload = (e) => {
      const previewImage = document.getElementById('previewImage') as HTMLImageElement
      const recognizeButton = document.getElementById('recognizeButton') as HTMLButtonElement
      if (previewImage && e.target) {
        previewImage.src = e.target.result as string
        previewImage.style.display = 'block'
        recognizeButton.disabled = false
      }
    }
    reader.readAsDataURL(file)
  }
}

let carouselTimer: ReturnType<typeof setInterval> | null = null
let shiwenTimer: ReturnType<typeof setInterval> | null = null
let timeTimer: ReturnType<typeof setInterval> | null = null
let scrollHandler: (() => void) | null = null

onMounted(() => {
  updateImage()
  carouselTimer = setInterval(nextImage, 4000)
  shiwenTimer = setInterval(() => {
    nowShi++
    shiwenLoopShow(nowShi)
  }, 6000)

  timeTimer = setInterval(showTime, 1000)
  showTime()

  scrollHandler = () => {
    const navbar = document.querySelector('.daohang')
    const sousuo = document.querySelector('.sousuo')
    if (window.pageYOffset > 640) {
      navbar?.classList.add('fixed')
      sousuo?.classList.add('fixed')
    } else {
      navbar?.classList.remove('fixed')
      sousuo?.classList.remove('fixed')
    }
  }
  window.addEventListener('scroll', scrollHandler)

  const leftButtons = document.querySelectorAll('.leftButton')
  const rightButtons = document.querySelectorAll('.rightButton')

  leftButtons.forEach((leftButton) => {
    leftButton.addEventListener('click', function (this: HTMLElement) {
      const targetClass = '.' + this.getAttribute('data-target')
      const scrollableDiv = document.querySelector(targetClass) as HTMLElement
      if (scrollableDiv) {
        scrollableDiv.scrollLeft -= scrollStep
      }
    })
  })

  rightButtons.forEach((rightButton) => {
    rightButton.addEventListener('click', function (this: HTMLElement) {
      const targetClass = '.' + this.getAttribute('data-target')
      const scrollableDiv = document.querySelector(targetClass) as HTMLElement
      if (scrollableDiv) {
        scrollableDiv.scrollLeft += scrollStep
      }
    })
  })
})

onUnmounted(() => {
  if (carouselTimer) clearInterval(carouselTimer)
  if (shiwenTimer) clearInterval(shiwenTimer)
  if (timeTimer) clearInterval(timeTimer)
  if (scrollHandler) window.removeEventListener('scroll', scrollHandler)
})
</script>

<template>
<div class="top" id="sy_top">
  <div class="logo">
    <a href="index.html" target="_blank"><img class="logo_img" src="/img/tubiao (1).jpg" />
      <div class="txt">墨渊</div>
    </a>
  </div>
  <div class="top_txt">
    <div class="shijian" title="现在时间">{{ currentTime }}</div>

    <div class="txt1">了解我们
      <div class="erw"><img src="/img/微信二维.jpg" alt="扫一扫查看" />
      </div>
    </div>
    <div class="txt2">联系我们
      <div class="erw"><img src="/img/微信二维.jpg" alt="扫一扫联系" />
      </div>
    </div>
  </div>

</div>
<hr class="hr" />
<hr />

<div class="lbye">
  <div class="sy_top_daohang_biao">
    <div class="toptub1"><a href="#luntan" title="交流论坛"><img src="/img/top_tubiao4.png"/></a></div>
    <div class="toptub2" title="菜单"><a href="#sy_fljx"><img src="/img/top_tubiao2.png"/></a></div>
    <div class="toptub3" title="搜索"><a href=""><img src="/img/top_tubiao1.png"/></a></div>
    <div class="toptub4" title="去登录"><a href="html/denglu.html"><img src="/img/top_tubiao3.png"/></a></div>
  </div>
  <div id="lb_jt_z" class="lb_jiantou_left" @click="prevImage"><img src="/img/jianzu (3).png"/></div>
  <div id="lb_jt_y" class="lb_jiantou_right" @click="nextImage"><img src="/img/jianzu (4).png"/></div>
  <img class="lb_zhutu" src="/img/lb_ (1).jpg" id="ad_scroll" />
  <ul>
    <li @mouseover="imgLoopShow(1)" id="li1" style="color: red;">1</li>
    <li @mouseover="imgLoopShow(2)" id="li2">2</li>
    <li @mouseover="imgLoopShow(3)" id="li3">3</li>
    <li @mouseover="imgLoopShow(4)" id="li4">4</li>
    <li @mouseover="imgLoopShow(5)" id="li5">5</li>
    <li @mouseover="imgLoopShow(6)" id="li6">6</li>
  </ul>

  <img class="zhuang_s4_1" src="/img/gn_ (2).png" />
</div>

<div class="body">
  <div class="center">
    <div class="zhu_yao">
      <hr class="hr" />
      <hr />
      <div class="daohang">
        <ul>
          <li><a href="html/fenyejiagou.html" target="_blank">进入论坛</a></li>

          <li>古体诗词
            <ul>
              <li>怀古诗</li>
              <li><a href="html/fenyejiagou.html" target="_blank">咏物感怀</a></li>
              <li>边塞风云</li>
              <li>山水田园</li>
              <li>赠友送别</li>
              <li><a href="html/fenyejiagou.html" target="_blank">闲适隐逸</a></li>
              <li>···</li>
            </ul>
          </li>
          <li>现代诗歌
            <ul>
              <li>‌<a href="html/fenyejiagou.html" target="_blank">人生派</a></li>
              <li>七月派</li>
              <li>‌朦胧派</li>
              <li>现实主义诗派</li>
              <li>新时代派</li>
              <li>‌流萤诗派</li>
              <li>···</li>
            </ul>
          </li>
          <li>外国诗歌
            <ul>
              <li>俄罗斯</li>
              <li>法国</li>
              <li>英国</li>
              <li>日本</li>
            </ul>
          </li>
          <li>当代青年
            <ul>
              <li>九刀</li>
              <li>初芒</li>
              <li>精工庞华</li>
              <li>清幽雅丽</li>
            </ul>
          </li>
          <li>关于我们
            <ul>
              <li>查看详情</li>
              <li>联系我们</li>
              <li>错误指正</li>
            </ul>
          </li>
          <li id="xdl_kaiguan">个人中心<img class="dl_tb2" src="/img/dl_tb2.png"/>
            <img class="dl_tb1" src="/img/dl_tb1.png"/>
            <div id="sy_xdlchuang" class="sy_xdlchuang">
              <div class="login-container">
                <h2 class="login-title">登&nbsp;&nbsp;&nbsp;&nbsp;录</h2>
                <form action="" method="post">
                  <div class="lieb">
                    <label for="username" class="dl_ts">用户名:</label>
                    <input type="text" id="username" class="input-field" placeholder="请输入用户名">
                  </div>
                  <div class="lieb">
                    <label for="password" class="dl_ts">密&nbsp;&nbsp;码:</label>
                    <input type="password" id="password" class="input-field" placeholder="请输入密码" required>
                  </div>
                  <button type="submit" class="login-button">登录</button>
                </form>
                <div class="forgot-password">
                  <a class="fuzhu" href="html/denglu.html">忘记密码?</a>
                </div>
                <div class="register-link">
                  <a class="fuzhu" href="html/zhuce.html">还没有账号?注册</a>
                </div>
              </div>
            </div>
          </li>
        </ul>
      </div>
      <div class="sousuo">
        <form action="https://www.baidu.com" method="post" id="sousuo0" @submit="handleSousuoSubmit">
          <input type="text" placeholder="输入搜索内容" name="search">
          <input type="submit" value="搜索">
        </form>
      </div>

      <div class="youce_fangbiao">
        <div class="youce_fangbiao_s">
          <div class="you"><img src="/img/ce_tubiao2.png"/></div>
          <div class="you" id="yinyuebofang_zhu" title="音乐" @click="toggleMusic">
            <img src="/img/ce_tubiao3.png"/>
            <audio id="myAudio_zhu" controls>
              <source src="/js/music.mp3" type="audio/mpeg">
              您的浏览器不支持音频播放功能
            </audio>
          </div>
        </div>
        <div class="youce_fangbiao_x">
          <div class="you" title="返回顶部"><a href="#sy_top"><img src="/img/jianzu (5).png"/></a></div>
        </div>
      </div>

      <div class="lbye_shiwen">
        <img class="lb_zhushi" src="/img/lb_shiwen (1).png" id="ad_scroll_shiwen"/>
        <ul>
          <li class="xubiao" @mouseover="shiwenLoopShow(1)" id="l1" style="background-color: rgb(249,255,0);"></li>
          <li class="xubiao" @mouseover="shiwenLoopShow(2)" id="l2"></li>
          <li class="xubiao" @mouseover="shiwenLoopShow(3)" id="l3"></li>
          <li class="xubiao" @mouseover="shiwenLoopShow(4)" id="l4"></li>
          <li class="xubiao" @mouseover="shiwenLoopShow(5)" id="l5"></li>
          <li class="xubiao" @mouseover="shiwenLoopShow(6)" id="l6"></li>
          <li class="xubiao" @mouseover="shiwenLoopShow(7)" id="l7"></li>
        </ul>

        <img class="zhuang_s1" src="/img/gn_3.png" />
        <img class="zhuang_s2" src="/img/fy_tubiao (6).png" />
        <img class="zhuang_s5" src="/img/zichu(1).png" />
        <img class="zhuang_s7" src="/img/tuobiao (4).png" />
        <img class="zhuang_s11" src="/img/lt_bg6.jpg" />
        <img class="zhuang_s3" src="/img/canci.png" />
        <img class="zhuang_s4" src="/img/gn_ (2).png" />
      </div>

      <hr />
      <h1 class="sy_fljx" id="sy_fljx">分类精赏</h1>
      <div class="z_top">
        <div class="h4">
          <h1>著名诗人</h1>
          <div class="flip-box">
            <div class="front">
              <div class="l2"><img src="/img/cd_suolue (10).jpg" />
                <li>李清照</li>
                <p>（1084年3月13日—1155年），号易安居士，齐州章丘（今山东省济南市章丘区）人。宋代婉约派代表词人。</p>
              </div>
            </div>
            <div class="back">
              <div class="l3">
                <p class="cd_b_jianjie">李清照善于以简洁自然的语言表达真挚的情感，使词作通俗易懂却又意味深长，
                如 "此情无计可消除，才下眉头，却上心头"。她的词作
                情感表达细腻入微，无论是少女的娇羞、对爱情的渴望，还是后期的国仇家恨、孤独愁苦，都能让读者产生强烈的共鸣。</p>
                <a class="gdxq" href="html/fenyejiagou.html">&lt;更多详情&gt;</a>
              </div>
            </div>
          </div>

          <div class="flip-box">
            <div class="front">
              <div class="l2"><img src="/img/cd_suolue (11).jpg" />
                <li>杜牧</li>
                <p>（803年—852年），字牧之，京兆万年（今陕西省西安市）人。杜佑之孙，唐朝文学家、诗人。</p>
              </div>
            </div>
            <div class="back">
              <div class="l3">
                <p class="cd_b_jianjie">杜牧为人性情刚直，不拘小节，不屑逢迎。自负经略之才，诗文均有盛名。文以《阿房宫赋》为最著，诗作明丽隽永，绝句诗尤受人称赞，世称小杜。与李商隐齐名，合称"小李杜"。年轻时，好读兵书，注解曹操所定《孙子兵法》十三篇。</p>
                <a class="gdxq" href="html/fenyejiagou.html">&lt;更多详情&gt;</a>
              </div>
            </div>
          </div>

          <div class="flip-box">
            <div class="front">
              <div class="l2"><img src="/img/cd_suolue (12).jpg" />
                <li>苏轼</li>
                <p>（1037年—1101年），字子瞻，又字和仲，号铁冠道人、东坡居士，世称苏东坡、苏仙、坡仙。</p>
              </div>
            </div>
            <div class="back">
              <div class="l3">
                <p class="cd_b_jianjie">苏轼的思想融合了儒、释、道三家。儒家思想使他积极入世，关心民生疾苦，有强烈的社会责任感；道家思想让他在面对困境时能保持内心的超脱和豁达；佛家思想则使他在历经磨难后，获得心灵的慰藉和宁静，这种复杂而多元的思想体系在他的作品中多有体现。</p>
                <a class="gdxq" href="html/fenyejiagou.html">&lt;更多详情&gt;</a>
              </div>
            </div>
          </div>

          <div class="flip-box">
            <div class="front">
              <div class="l2"><img src="/img/cd_suolue (9).jpg" />
                <li>上官婉儿</li>
                <p>（664 年 —710 年）是唐代一位极具影响力的女官和诗人。与蔡文姬、李清照、卓文君并称中国古代四大才女。</p>
              </div>
            </div>
            <div class="back">
              <div class="l3">
                <p class="cd_b_jianjie">她代朝廷品评天下诗文，大力提倡辞藻华丽、用律严谨的文风，使得当时宫廷内外都流行这种诗歌风格，对唐朝文坛的繁荣和诗歌艺术水平的提高作出了重要贡献，与蔡文姬、李清照、卓文君并称中国古代四大才女。近代文艺理论家谢无量称 "婉儿承其祖，与诸学士争务华藻，沈、宋应制之作多经婉儿评定，当时以此相慕，遂成风俗，故律诗之成，上官祖孙功尤多也"，高度赞扬了她在文学和诗歌发展方面的贡献。</p>
                <a class="gdxq" href="html/fenyejiagou.html">&lt;更多详情&gt;</a>
              </div>
            </div>
          </div>

        </div>

        <div class="cd_fenge"></div>
        <div class="h6">
          <h1 class="sy_cebiaoti">作品</h1>

          <div class="h6_li_1 scrollableDiv h6_li">
            <div class="leftButton" data-target="h6_li_1"><img src="/img/jianzu (3).png"/></div>
            <div class="rightButton" data-target="h6_li_1"><img src="/img/jianzu (4).png"/></div>
            <div class="l1"><img src="/img/h6.jpg" />
              <li>黄鹤楼</li>
              <p>唐代:崔颢</p>
            </div>
            <div class="l1"><img src="/img/h6.2.jpg" />
              <li>春宫怨</li>
              <p>唐代:杜荀鹤</p>
            </div>
            <div class="l1">
              <a href="html/diushi.html"><img src="/img/h6.3.jpg" />
                <li>秋日赴阙题潼关驿楼</li>
                <p>唐代:杜荀鹤</p>
              </a>
            </div>
            <div class="l1"><img src="/img/h6.4.jpg" />
              <li>次北固山下</li>
              <p>唐代:许浑</p>
            </div>
            <div class="l1"><img src="/img/h6.jpg" />
              <li>黄鹤楼</li>
              <p>唐代:崔颢</p>
            </div>
            <div class="l1"><img src="/img/h6.2.jpg" />
              <li>春宫怨</li>
              <p>唐代:杜荀鹤</p>
            </div>
            <div class="l1">
              <a href="html/diushi.html"><img src="/img/h6.3.jpg" />
                <li>秋日赴阙题潼关驿楼</li>
                <p>唐代:杜荀鹤</p>
              </a>
            </div>
            <div class="l1"><img src="/img/h6.4.jpg" />
              <li>次北固山下</li>
              <p>唐代:许浑</p>
            </div>
          </div>
        </div>
        <div class="cd_fenge"></div>
        <div class="h6">
          <h1 class="sy_cebiaoti">流派</h1>
          <div class="h6_li_2 scrollableDiv h6_li">
            <div class="leftButton" data-target="h6_li_2"><img src="/img/jianzu (3).png"/></div>
            <div class="rightButton" data-target="h6_li_2"><img src="/img/jianzu (4).png"/></div>
            <div class="l1"><img src="/img/h6_liupai_4.png" /><li>边塞·豪放</li></div>
            <div class="l1">
              <a href="html/diushi.html"><img src="/img/h6_liupai_1.jpg" /><li>唐宋八大家</li>
              </a></div>
            <div class="l1"><img src="/img/h6_liupai_3.png" /><li>竹林七贤</li>
              <li>logo1</li>
            </div>
            <div class="l1"><img src="/img/h6_liupai_2.jpg" /><li>元曲四大家</li></div>
            <div class="l1"><img src="/img/h6_chaodai_1.jpg" /><li>两汉</li></div>
            <div class="l1"><img src="/img/h6_chaodai_2.jpg" /><li>唐朝</li></div>
            <div class="l1"><img src="/img/h6_chaodai_3.jpg" /><li>宋朝</li></div>
            <div class="l1"><img src="/img/cd_suolue (4).jpg" /><li>元朝</li></div>
          </div>
        </div>
        <div class="h6">
          <h1 class="sy_cebiaoti">朝代</h1>
          <div class="h6_li_3 scrollableDiv h6_li">
            <div class="leftButton" data-target="h6_li_3"><img src="/img/jianzu (3).png"/></div>
            <div class="rightButton" data-target="h6_li_3"><img src="/img/jianzu (4).png"/></div>
            <div class="l1"><img src="/img/h6_chaodai_1.jpg" /><li>先秦</li></div>
            <div class="l1"><img src="/img/h6_chaodai_1.jpg" /><li>两汉</li></div>
            <div class="l1"><img src="/img/h6_chaodai_2.jpg" /><li>唐朝</li></div>
            <div class="l1"><img src="/img/h6_chaodai_3.jpg" /><li>宋朝</li></div>
            <div class="l1"><img src="/img/cd_suolue (4).jpg" /><li>元朝</li></div>
            <div class="l1"><img src="/img/h6_chaodai_2.jpg" /><li>南北朝</li></div>
            <div class="l1"><img src="/img/h6_chaodai_3.jpg" /><li>金朝</li></div>
            <div class="l1"><img src="/img/cd_suolue (4).jpg" /><li>明清</li></div>
          </div>
        </div>
        <div class="cd_fenge"></div>
        <h2>展风拓潮</h2>
      </div>
      <hr />
      <p class="ai_title">趣味AI</p>
      <div class="z_center">

        <div class="z_left">

          <div class="cheng" style="height: 5px;width: 100%;"></div>
          <div class="container">
            <h1>看图写诗</h1>
            <p>上传图片生成古诗</p>
            <label for="imageUpload" class="upload-button">上传图片</label>
            <input type="file" id="imageUpload" accept="image/*" style="display: none;" @change="handleImageUpload">
            <img id="previewImage" src="" alt="Preview Image" style="display: none;">
            <button id="recognizeButton" disabled>图片识别</button>
            <div id="resultContainer">
              <h2>Recognition Result:</h2>
              <p id="recognitionResult"></p>
            </div>
          </div>
          <div class="container">
            <h2>智能分析</h2>
            <p>输入您要解析的诗句，然后点击"智能AI"按钮进行解析。</p>
            <input type="text" id="question" placeholder="例如：床前明月光,疑是地上霜.">
            <button id="bt">智能AI解析</button>
            <div id="result">解析：</div>
          </div>
        </div>
        <div class="z_right">
          <h4>诗话视野</h4>
          <hr />
          <p>---1. "穿越千年的诗意对话：唐诗宋词中的文化传承与创新"</p>
          <a class="shsy_gdxq" href="html/fenyejiagou.html">&lt;更多详情&gt;</a>
          <p>---2. "意象之美：如何通过自然景观来抒发人文情怀"</p>
          <a class="shsy_gdxq" href="html/fenyejiagou.html">&lt;更多详情&gt;</a>
          <p>---3. "从'床前明月光'到'明月几时有'：月亮在古诗词中的意象变迁"</p>
          <a class="shsy_gdxq" href="html/fenyejiagou.html">&lt;更多详情&gt;</a>
          <p>---4. "诗词中的哲学思考：从杜甫到苏轼的忧国忧民情怀"</p>
          <p>---5. "诗词创作工作坊：提升你的押韵与对仗技巧"</p>
          <p>---6. "诗词与书法：传统艺术的双重魅力"</p>
          <a class="shsy_gdxq" href="html/fenyejiagou.html">&lt;更多详情&gt;</a>

          <p>---7. "诗词教育在当代：如何在学校和社会中推广古诗词"</p>
          <a class="shsy_gdxq" href="html/fenyejiagou.html">&lt;更多详情&gt;</a>
          <p>---8. "国际视野下的中国诗词：翻译与传播的挑战"</p>
          <p>---9. "诗词与现代生活：寻找古典与现代的平衡点"</p>
          <p>---10. "诗词论坛互动专区：分享你的原创诗词，赢取点评与建议"</p>
          <a class="shsy_gdxq" href="html/fenyejiagou.html">&lt;更多详情&gt;</a>
          <a class="shsy_gdxq" href="html/fenyejiagou.html">&lt;······&gt;</a>
          <hr />
          <p>---1. "穿越千年的诗意对话：唐诗宋词中的文化传承与创新"</p>
          <a class="shsy_gdxq" href="html/fenyejiagou.html">&lt;更多详情&gt;</a>
          <p>---2. "意象之美：如何通过自然景观来抒发人文情怀"</p>
          <a class="shsy_gdxq" href="html/fenyejiagou.html">&lt;更多详情&gt;</a>
          <p>---3. "从'床前明月光'到'明月几时有'：月亮在古诗词中的意象变迁"</p>
          <a class="shsy_gdxq" href="html/fenyejiagou.html">&lt;更多详情&gt;</a>
          <p>---4. "诗词中的哲学思考：从杜甫到苏轼的忧国忧民情怀"</p>
          <p>---5. "诗词创作工作坊：提升你的押韵与对仗技巧"</p>
          <p>---6. "诗词与书法：传统艺术的双重魅力"</p>
          <a class="shsy_gdxq" href="html/fenyejiagou.html">&lt;更多详情&gt;</a>

          <p>---7. "诗词教育在当代：如何在学校和社会中推广古诗词"</p>
          <a class="shsy_gdxq" href="html/fenyejiagou.html">&lt;更多详情&gt;</a>
          <p>---8. "国际视野下的中国诗词：翻译与传播的挑战"</p>
          <p>---9. "诗词与现代生活：寻找古典与现代的平衡点"</p>
          <p>---10. "诗词论坛互动专区：分享你的原创诗词，赢取点评与建议"</p>
          <a class="shsy_gdxq" href="html/fenyejiagou.html">&lt;更多详情&gt;</a>
          <a id="luntan" class="shsy_gdxq" href="html/fenyejiagou.html">&lt;······&gt;</a>

        </div>
      </div>
    </div>

  </div>
  <img class="zhuang_s" src="/img/gn_ (2).png" />
  <img class="zhuang_s6" src="/img/zichu (4).png" />
  <img class="zhuang_s8" src="/img/YIJI_tuobiao (5).png" />
  <img class="zhuang_s9" src="/img/wz_tubiao (2).png" />
  <img class="zhuang_s10" src="/img/tuobiao (2).png" />
  <h1>诗汇论坛</h1>
  <div class="luntan">
    <div class="luntan_sousuo">

      <form action="/action_page.php" id="luntan_sousuo" @submit="handleLuntanSousuoSubmit">
        <input type="text" placeholder="请输入查询内容" name="search">
        <input type="submit" value="查询">
      </form>
    </div>
    <div class="gundong">
      <marquee scrollamount="10" direction="right" behavior="alternate" @mouseout="($event.target as HTMLElement)?.closest('marquee')?.setAttribute('scrollamount', '10')"
        @mouseover="($event.target as HTMLElement)?.closest('marquee')?.setAttribute('scrollamount', '0')">古诗词评鉴·论论坛坛</marquee>
    </div>

    <div class="luntan_daohang">
      <ul>
        <li>秦汉</li>
        <li>魏晋</li>
        <li>唐朝</li>
        <li>宋朝</li>
        <li>明朝</li>
        <li>清朝</li>
        <li>诗人选择
          <ul>
            <li>李白</li>
            <li>白居易</li>
            <li>杜甫</li>
            <li>刘禹锡</li>
            <li class="shi_sousuo_li">
              <div class="shi_sousuo">
                <form action="../html/index.html" id="shi_suosou" @submit="handleShiSousuoSubmit">
                  <input type="text" placeholder="请输入诗人姓名" name="search">
                  <input type="submit" value="搜索">
                </form>
              </div>
            </li>
          </ul>
        </li>
      </ul>
    </div>
    <h2>古诗词推选</h2>

    <div class="luntan_jx">
      <div class="luntan_jx_title">历史的印痕</div>
      <div class="x1"><img src="/img/lt_jx (4).jpg" />
        <div class="jingxuan_pl">
          <p><span>2019年6月24日 04:45</span><br />
            愿如花已落千行，未闻花语浅别殇。
            幽僻心境漫过少，唯有诗语解锁缰。
            金甲未脱抬眼望，怒剑难收向疆场。
            笑祝功成人与事，再鼓一旬又何妨。<br /><span>常平逼王</span></p>
          <img src="/img/微信二维.jpg" /></div>
      </div>
      <div class="x1"><img src="/img/lt_jx (1).jpg" />
        <div class="jingxuan_pl">
          <p><span>2019年6月24日 04:45</span><br />
            愿如花已落千行，未闻花语浅别殇。
            幽僻心境漫过少，唯有诗语解锁缰。
            金甲未脱抬眼望，怒剑难收向疆场。
            笑祝功成人与事，再鼓一旬又何妨。<br /><span>常平逼王</span></p>
          <img src="/img/微信二维.jpg" /></div>
      </div>
      <div class="x2"><img src="/img/lt_jx (2).jpg" />
        <div class="jingxuan_pl">
          <p><span>2018年9月1日 11:01</span><br />
            <span>西江月.未语</span>
            华灯初照白路，夜来树影凄楚，俳徊只与月相伴，茫然一片迷雾。
            离人兀自心扬，未语难避哀伤，只若通明过后，真心但化霓裳。<br />
            <span>常平逼王</span></p>

          <img src="/img/微信二维.jpg" /></div>
      </div>

      <div class="x3"><img src="/img/lt_jx (3).jpg" />
        <div class="jingxuan_pl">
          <p><span>2018年9月24日 08:35</span><br />
            <span>忆江南.哀</span>
            秋风寒，
            落红自凋残。
            未识伊人愁几许，
            伊人兀自笑颜开。
            吾心不胜哀！<br /><span>常平逼王</span>
          </p>
          <img src="/img/微信二维.jpg" />
        </div>
      </div>

      <div class="x3"><img src="/img/lt_jx (4).jpg" />
        <div class="jingxuan_pl">

          <p><span>2022年4月5日 14:02</span><br />
            东风吹起白蝴蝶，散入云幕尽青烟。
            但觉离魂归来少，春日暖暖艳阳天。
            松柏长青久可立，香印再燃意更坚。
            思情尽付潇湘处，绵远流长年复年。<br /><span>常平逼王</span>
          </p>
          <img src="/img/微信二维.jpg" />
        </div>
      </div>
      <div class="x3">
        <img src="/img/lt_jx (4).jpg" />
        <div class="jingxuan_pl">
          <p><span>2018年4月22日 12:03</span><br />
            <span>清平乐.梦怀</span>
            雨落窗外，淅声渐消潺，一帘凝愁入梦来，若雾浸湿枕畔。
            经年离愁之初，再会此情更苦，最是万千惆怅，与白共登天目!<br /><span>常平逼王</span>
          </p>
          <img src="/img/微信二维.jpg" />
        </div>
      </div>
      <div class="x3">
        <img src="/img/lt_jx (4).jpg" />
        <div class="jingxuan_pl">
          <p><span>2018年5月20日 12:43</span><br />
            <span>乌夜啼.叹城府</span>
            淡雾兀遮明眸，更添愁，独倚玉阑不觉泪空流。
            人情恶，谁易错，自长酌，一心明月向沟成污浊。<br /><span>常平逼王</span>
          </p>
          <img src="/img/微信二维.jpg" />
        </div>
      </div>
      <div class="x3">
        <img src="/img/lt_jx (4).jpg" />
        <div class="jingxuan_pl">
          <p><span>2019年2月4日 23:27</span><br />
            新桃映红把福迎，银烛高照万家兴。
            火尾连排上清夜，笑语欢声动耳惊。
            奈何遣词功夫浅，此夜此景休论明。
            唯愿持酒歌一曲，与君同乐一身轻<br /><span>常平逼王</span>
          </p>
          <img src="/img/微信二维.jpg" />
        </div>
      </div>
      <div class="x3">
        <img src="/img/lt_jx (4).jpg" />
        <div class="jingxuan_pl">
          <p><span>2020年1月25日 01:56</span><br />
            回首一九已成空，展望二零佳意浓。
            世事岂敢轻年少，天意哪可断前程。
            唐猊又着战意显，三尺重出徙侣从。
            烂柯沉木枉悲夫，夫且若怡也可容。<br /><span>常平逼王</span>
          </p>
          <img src="/img/微信二维.jpg" />
        </div>
      </div>
      <div class="x3">
        <img src="/img/lt_jx (4).jpg" />
        <div class="jingxuan_pl">
          <p><span>2021年4月3日 15:15</span><br />
            沉影迷叠千层障，乱云归处是它乡。
            酒酣仍识昔日客，心迷难辨眼前芳。
            纵把凡锦比仙缎，不需经年多思量。
            一笑即随羊角去，九风还作万华芳。<br /><span>常平逼王</span>
          </p>
          <img src="/img/微信二维.jpg" />
        </div>
      </div>
      <div class="x3">
        <img src="/img/lt_jx (4).jpg" />
        <div class="jingxuan_pl">
          <p><span>2022年4月5日 14:02</span><br />
            东风吹起白蝴蝶，散入云幕尽青烟。
            但觉离魂归来少，春日暖暖艳阳天。
            松柏长青久可立，香印再燃意更坚。
            思情尽付潇湘处，绵远流长年复年。<br /><span>常平逼王</span>
          </p>
          <img src="/img/微信二维.jpg" />
        </div>
      </div>
      <div class="x3">
        <img src="/img/lt_jx (4).jpg" />
        <div class="jingxuan_pl">
          <p><span>2023年1月4日 10:31</span><br />
            <span>一剪梅.无题</span>
            一别三秋未招摇，
            山也迢迢，水也昭昭。
            何人再添新衣袍，
            意笑盈绕，喜上眉梢。
            忆昔花开岁月好，
            蜂字飘摇，蝶字舞蹈。
            而今方知云未晓，
            风又飘飘，雨又萧萧。<br /><span>常平逼王</span>
          </p>
          <img src="/img/微信二维.jpg" />
        </div>
      </div>
    </div>

    <h2>当代·精选</h2>
    <div class="gundong">
      <marquee scrollamount="10" direction="right" behavior="alternate" @mouseout="($event.target as HTMLElement)?.closest('marquee')?.setAttribute('scrollamount', '10')"
        @mouseover="($event.target as HTMLElement)?.closest('marquee')?.setAttribute('scrollamount', '0')">繁华齐绘,添墨共赏</marquee>
    </div>

    <div class="luntan_ddjx">
      <div class="luntan_jx_title">时代画像</div>
      <div class="x1"><img src="/img/lt_jx (4).jpg" />
        <div class="jingxuan_pl">
          <p><span>2019年6月24日 04:45</span><br />
            愿如花已落千行，未闻花语浅别殇。
            幽僻心境漫过少，唯有诗语解锁缰。
            金甲未脱抬眼望，怒剑难收向疆场。
            笑祝功成人与事，再鼓一旬又何妨。<br /><span>常平逼王</span></p>
        </div>
      </div>
      <div class="x1"><img src="/img/lt_jx (1).jpg" />
        <div class="jingxuan_pl">
          <p><span>2019年6月24日 04:45</span><br />
            愿如花已落千行，未闻花语浅别殇。
            幽僻心境漫过少，唯有诗语解锁缰。
            金甲未脱抬眼望，怒剑难收向疆场。
            笑祝功成人与事，再鼓一旬又何妨。<br /><span>常平逼王</span></p>
        </div>
      </div>
      <div class="x2"><img src="/img/lt_jx (2).jpg" />
        <div class="jingxuan_pl">
          <p><span>2018年9月1日 11:01</span><br />
            <span>西江月.未语</span>
            华灯初照白路，夜来树影凄楚，俳徊只与月相伴，茫然一片迷雾。
            离人兀自心扬，未语难避哀伤，只若通明过后，真心但化霓裳。<br />
            <span>常平逼王</span></p>
        </div>
      </div>

      <div class="x3"><img src="/img/lt_jx (3).jpg" />
        <div class="jingxuan_pl">
          <p><span>2018年9月24日 08:35</span><br />
            <span>忆江南.哀</span>
            秋风寒，
            落红自凋残。
            未识伊人愁几许，
            伊人兀自笑颜开。
            吾心不胜哀！<br /><span>常平逼王</span>
          </p>
          <img src="/img/微信二维.jpg" />
        </div>
      </div>

      <div class="x3"><img src="/img/lt_jx (4).jpg" />
        <div class="jingxuan_pl">

          <p><span>2022年4月5日 14:02</span><br />
            东风吹起白蝴蝶，散入云幕尽青烟。
            但觉离魂归来少，春日暖暖艳阳天。
            松柏长青久可立，香印再燃意更坚。
            思情尽付潇湘处，绵远流长年复年。<br /><span>常平逼王</span>
          </p>
          <img src="/img/微信二维.jpg" />
        </div>
      </div>
      <div class="x3">
        <img src="/img/lt_jx (4).jpg" />
        <div class="jingxuan_pl">
          <p><span>2018年4月22日 12:03</span><br />
            <span>清平乐.梦怀</span>
            雨落窗外，淅声渐消潺，一帘凝愁入梦来，若雾浸湿枕畔。
            经年离愁之初，再会此情更苦，最是万千惆怅，与白共登天目!<br /><span>常平逼王</span>
          </p>
          <img src="/img/微信二维.jpg" />
        </div>
      </div>
      <div class="x3">
        <img src="/img/lt_jx (4).jpg" />
        <div class="jingxuan_pl">
          <p><span>2018年5月20日 12:43</span><br />
            <span>乌夜啼.叹城府</span>
            淡雾兀遮明眸，更添愁，独倚玉阑不觉泪空流。
            人情恶，谁易错，自长酌，一心明月向沟成污浊。<br /><span>常平逼王</span>
          </p>
          <img src="/img/微信二维.jpg" />
        </div>
      </div>
      <div class="x3">
        <img src="/img/lt_jx (4).jpg" />
        <div class="jingxuan_pl">
          <p><span>2019年2月4日 23:27</span><br />
            新桃映红把福迎，银烛高照万家兴。
            火尾连排上清夜，笑语欢声动耳惊。
            奈何遣词功夫浅，此夜此景休论明。
            唯愿持酒歌一曲，与君同乐一身轻<br /><span>常平逼王</span>
          </p>
          <img src="/img/微信二维.jpg" />
        </div>
      </div>
      <div class="x3">
        <img src="/img/lt_jx (4).jpg" />
        <div class="jingxuan_pl">
          <p><span>2020年1月25日 01:56</span><br />
            回首一九已成空，展望二零佳意浓。
            世事岂敢轻年少，天意哪可断前程。
            唐猊又着战意显，三尺重出徙侣从。
            烂柯沉木枉悲夫，夫且若怡也可容。<br /><span>常平逼王</span>
          </p>
          <img src="/img/微信二维.jpg" />
        </div>
      </div>
      <div class="x3">
        <img src="/img/lt_jx (4).jpg" />
        <div class="jingxuan_pl">
          <p><span>2021年4月3日 15:15</span><br />
            沉影迷叠千层障，乱云归处是它乡。
            酒酣仍识昔日客，心迷难辨眼前芳。
            纵把凡锦比仙缎，不需经年多思量。
            一笑即随羊角去，九风还作万华芳。<br /><span>常平逼王</span>
          </p>
          <img src="/img/微信二维.jpg" />
        </div>
      </div>
      <div class="x3">
        <img src="/img/lt_jx (4).jpg" />
        <div class="jingxuan_pl">
          <p><span>2022年4月5日 14:02</span><br />
            东风吹起白蝴蝶，散入云幕尽青烟。
            但觉离魂归来少，春日暖暖艳阳天。
            松柏长青久可立，香印再燃意更坚。
            思情尽付潇湘处，绵远流长年复年。<br /><span>常平逼王</span>
          </p>
          <img src="/img/微信二维.jpg" />
        </div>
      </div>
      <div class="x3">
        <img src="/img/lt_jx (4).jpg" />
        <div class="jingxuan_pl">
          <p><span>2023年1月4日 10:31</span><br />
            <span>一剪梅.无题</span>
            一别三秋未招摇，
            山也迢迢，水也昭昭。
            何人再添新衣袍，
            意笑盈绕，喜上眉梢。
            忆昔花开岁月好，
            蜂字飘摇，蝶字舞蹈。
            而今方知云未晓，
            风又飘飘，雨又萧萧。<br /><span>常平逼王</span>
          </p>
          <img src="/img/微信二维.jpg" />
        </div>
      </div>
    </div>
    <div class="luntan_jxsz">
      <ul>
        <li><input type="reset" value="投稿" class="dianji" title="我的诗文" /></li>
        <li><input type="reset" value="上传" class="dianji" title="我的观点" /></li>
        <li> </li>
      </ul>
    </div>

    <div class="modern-poems-section">
      <h3>现代诗词管理</h3>
      <div class="poem-controls">
        <button id="extractPoemsBtn">提取页面诗词</button>
        <button id="showAddFormBtn">添加新诗词</button>
        <button id="loadPoemsBtn">加载已存储诗词</button>
      </div>

      <div id="statusMessage" class="status-message" style="display: none;"></div>

      <div id="addPoemForm" class="poem-form" style="display: none;">
        <h4>添加现代诗词</h4>
        <div class="form-group">
          <label for="poemTitle">诗词标题</label>
          <input type="text" id="poemTitle" placeholder="请输入诗词标题">
        </div>
        <div class="form-group">
          <label for="poemContent">诗词内容</label>
          <textarea id="poemContent" placeholder="请输入诗词内容"></textarea>
        </div>
        <div class="form-group">
          <label for="poemAuthor">作者</label>
          <input type="text" id="poemAuthor" placeholder="请输入作者姓名">
        </div>
        <div class="form-group">
          <label for="poemDate">创作时间</label>
          <input type="date" id="poemDate">
        </div>
        <div class="form-group">
          <label for="poemCategory">诗词流派</label>
          <select id="poemCategory">
            <option value="人生派">人生派</option>
            <option value="七月派">七月派</option>
            <option value="朦胧派">朦胧派</option>
            <option value="现实主义诗派">现实主义诗派</option>
            <option value="新时代派">新时代派</option>
            <option value="流萤诗派">流萤诗派</option>
            <option value="当代青年">当代青年</option>
          </select>
        </div>
        <div class="form-actions">
          <button type="button" class="btn-secondary" id="cancelAddBtn">取消</button>
          <button type="button" class="btn-primary" id="submitPoemBtn">提交</button>
        </div>
      </div>

      <div id="poemList" class="poem-list">
        <div class="poem-list-header">
          <h4>已存储的现代诗词</h4>
          <span id="poemCount">0 首</span>
        </div>
        <div id="poemListContent">
        </div>
      </div>
    </div>

    <div class="luntan_jl">
      <form>畅言寓所</form>
      <div class="jingxuan_zt">
        <p>
          <span class="zt_timo">清平乐.情(道姑)</span>
          幽情迷朦，三年怀一梦，一遭尘世皆为恒，无怨无悔无憎。
          来日打马南屏，忆昔紫夜流萤，纵剑吴钩霜雪，锦夜孤灯长明。<br />
          <span class="zt_zuozhe">～～初芒·2018年2月13日 16:58</span>
        </p>
        <div class="jx_zt_pinglun">
          蹄子抱膊:寥寥数语勾勒出超凡脱俗的隐逸画卷。<br />
          卡皮巴拉鸭:意境开阔，给人以强烈的视觉冲击与心灵震撼。<br />
          虚伪文青:情与景交融，展现出诗人对生命和时光的敏锐洞察与深沉思索。<br />
          <img src="/img/00.jpg" />
        </div>
        <div class="zt_shuru">
          <input type="text" placeholder="请输入" class="zt_pl_shuru" required />
          <input type="reset" value="我说两句" class="zt_pl_tijiao" />
        </div>

      </div>
      <div class="jingxuan_zt">
        <p>
          <span class="zt_timo">西江月.未语</span>
          华灯初照白路，夜来树影凄楚，俳徊只与月相伴，茫然一片迷雾。
          离人兀自心扬，未语难避哀伤，只若通明过后，真心但化霓裳。
          <span class="zt_zuozhe">2018年9月1日 11:01</span>

        </p>
        <div class="jx_zt_pinglun">
          抱走Kiku酱 抱走Kiku酱<img src="/img/00.jpg" />: 行万里路，装万年逼。常平逼王，何人能比。
          2018年9月1日 12:09<br />

          蹄子抱膊:寥寥数语勾勒出超凡脱俗的隐逸画卷。<br />
          卡皮巴拉鸭:意境开阔，给人以强烈的视觉冲击与心灵震撼。<br />
          虚伪文青:情与景交融，展现出诗人对生命和时光的敏锐洞察与深沉思索。<br />
          <img src="/img/00.jpg" />
        </div>
        <div class="zt_shuru">
          <input type="text" placeholder="请输入" class="zt_pl_shuru" required />
          <input type="reset" value="我说两句" class="zt_pl_tijiao" />
        </div>
        <img src="/img/00.jpg" />
      </div>

      <div class="jingxuan_zt">
        <p>
          <span class="zt_timo">清平乐.情(道姑)</span>
          幽情迷朦，三年怀一梦，一遭尘世皆为恒，无怨无悔无憎。
          来日打马南屏，忆昔紫夜流萤，纵剑吴钩霜雪，锦夜孤灯长明。<br />
          <span class="zt_zuozhe">～～初芒·2018年2月13日 16:58</span>

          <img src="/img/00.jpg" />
        </p>
        <div class="zt_shuru">
          <input type="text" placeholder="请输入" class="zt_pl_shuru" required />
          <input type="reset" value="我说两句" class="zt_pl_tijiao" />
        </div>

      </div>
      <div class="jingxuan_zt">
        <p>
          <span class="zt_timo">西江月.未语</span>
          华灯初照白路，夜来树影凄楚，俳徊只与月相伴，茫然一片迷雾。
          离人兀自心扬，未语难避哀伤，只若通明过后，真心但化霓裳。
          <span class="zt_zuozhe">2018年9月1日 11:01</span>
          抱走Kiku酱 抱走Kiku酱<img src="/img/00.jpg" />: 行万里路，装万年逼。常平逼王，何人能比。
          2018年9月1日 12:09<br />
        </p>
        <div class="zt_shuru">
          <input type="text" placeholder="请输入" class="zt_pl_shuru" required />
          <input type="reset" value="我说两句" class="zt_pl_tijiao" />
        </div>
        <img src="/img/00.jpg" />
      </div>

      <div class="jingxuan_zt">
        <img id="duihua_kuang_yang" src="/img/1733142558144.jpg" />
      </div>
    </div>

  </div>

  <hr />
</div>
<div class="zhant"><a href="html/diushi.html" target="_blank" title="点击了解详情">
    功能拓展<marquee scrollamount="10" direction="left" bgcolor="red" color="yellow" behavior="scroll"
      @mouseout="($event.target as HTMLElement)?.closest('marquee')?.setAttribute('scrollamount', '10')"
      @mouseover="($event.target as HTMLElement)?.closest('marquee')?.setAttribute('scrollamount', '0')">后续建设·紧张进行中……</marquee>
  </a>
</div>
<div class="dibu">
  <div class="d_left">
    <img src="/img/tubiao (2).png" />
  </div>
  <div class="d_center">

    <ul>
      <li>联系我们：</li>
      <li>事务联络</li>
      <li>媒体采访</li>
      <li>人才应聘</li>
      <li>就业招聘</li>
    </ul>
    <ul>
      <li>链 接：</li>
      <li>教育部</li>
      <li>江苏省教育厅</li>
      <li>华夏视听教育集团</li>
      <li>中国传媒大学</li>
    </ul>
  </div>
  <div class="d_right">
    <ul>
      <li>电话：</li>
      <li>(025)86179990</li>
    </ul>
    <ul>
      <li>地址：</li>
      <li>江苏省南京市江宁区弘景大道3666号</li>
    </ul>
    <ul>
      <li>邮编：</li>
      <li>211172</li>
    </ul>
  </div>
</div>
</template>