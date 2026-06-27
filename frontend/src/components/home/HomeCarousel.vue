<script setup lang="ts">
import { Swiper, SwiperSlide } from 'swiper/vue'
import { Autoplay, Pagination, EffectFade, Navigation } from 'swiper/modules'
import 'swiper/css'
import 'swiper/css/pagination'
import 'swiper/css/effect-fade'
import 'swiper/css/navigation'

const props = defineProps<{
  type: 'main' | 'shiwen'
}>()

const mainSlides = Array.from({ length: 6 }, (_, i) => `/img/lb_${i + 1}.jpg`)
const shiwenSlides = Array.from({ length: 7 }, (_, i) => `/img/lb_shiwen_${i + 1}.png`)
const slides = props.type === 'main' ? mainSlides : shiwenSlides
const delay = props.type === 'main' ? 4000 : 6000

const modules = [Autoplay, Pagination, EffectFade, Navigation]

const mainPagination = {
  clickable: true,
  renderBullet: (index: number, className: string) => {
    return `<span class="${className}">${index + 1}</span>`
  }
}

const shiwenPagination = {
  clickable: true,
  bulletClass: 'xubiao swiper-pagination-bullet',
  bulletActiveClass: 'xubiao swiper-pagination-bullet-active'
}
</script>

<template>
  <div :class="type === 'main' ? 'lbye' : 'lbye_shiwen'">
    <Swiper
      :modules="modules"
      :slides-per-view="1"
      :loop="true"
      :autoplay="{ delay, disableOnInteraction: false }"
      :effect="'fade'"
      :fade-effect="{ crossFade: true }"
      :pagination="type === 'main' ? mainPagination : shiwenPagination"
      :navigation="true"
      class="carousel-swiper"
    >
      <SwiperSlide v-for="(src, idx) in slides" :key="idx">
        <img
          :src="src"
          :class="type === 'main' ? 'lb_zhutu' : 'lb_zhushi'"
          :alt="`${type === 'main' ? '轮播' : '诗文'}图片 ${idx + 1}`"
        />
      </SwiperSlide>
    </Swiper>
  </div>
</template>

<style scoped lang="scss">
.lbye {
  margin: 0 auto;
  width: 80%;
  height: 620px;
  position: relative;
  top: 5px;
  margin-bottom: 10px;
  border-radius: 20px;
  overflow: hidden;
  padding: 0;
}

.lbye .lb_zhutu {
  width: 100%;
  height: 600px;
  display: block;
  border: 1px solid gainsboro;
  border-radius: 20px;
  object-fit: cover;
}

.lbye_shiwen {
  margin: 0 auto;
  width: calc(100% - 120px);
  height: 400px;
  position: relative;
  top: 5px;
  left: 30px;
  overflow: hidden;
}

.lbye_shiwen .lb_zhushi {
  width: 88%;
  height: 380px;
  border: 0px solid gainsboro;
  border-radius: 20px;
  margin-left: 20px;
}

.carousel-swiper {
  width: 100%;
  height: 100%;
}

.carousel-swiper :deep(.swiper-slide) {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 主轮播指示器样式 */
.lbye :deep(.swiper-pagination) {
  bottom: 30px !important;
  left: 250px;
  width: auto;
  display: flex;
  gap: 10px;
}

.lbye :deep(.swiper-pagination-bullet) {
  width: 21px;
  height: 21px;
  background-color: #bfbcbc;
  color: #333;
  font-size: 12px;
  line-height: 21px;
  text-align: center;
  opacity: 1;
  border-radius: 50%;
  cursor: pointer;
}

.lbye :deep(.swiper-pagination-bullet-active) {
  color: rgb(249, 255, 0);
}

/* 诗文轮播指示器样式 */
.lbye_shiwen :deep(.swiper-pagination) {
  bottom: 30px !important;
  left: 250px;
  width: auto;
  display: flex;
  gap: 10px;
}

:deep(.xubiao) {
  cursor: pointer;
  list-style: none;
  float: left;
  background-color: #bfbcbc;
  text-align: center;
  width: 41px;
  height: 10px;
  margin-right: 10px;
  font-size: 12px;
  border-radius: 50px;
  opacity: 1;
}

:deep(.xubiao.swiper-pagination-bullet-active) {
  background-color: rgb(249, 255, 0);
}

/* 导航箭头通用样式 */
.carousel-swiper :deep(.swiper-button-prev),
.carousel-swiper :deep(.swiper-button-next) {
  width: 30px;
  height: 100%;
  top: 0;
  margin-top: 0;
  background-color: rgba(80, 80, 80, 0.15);
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.3s ease;
  opacity: 0;
  z-index: 31;
}

.carousel-swiper:hover :deep(.swiper-button-prev),
.carousel-swiper:hover :deep(.swiper-button-next) {
  opacity: 1;
}

.carousel-swiper :deep(.swiper-button-prev) {
  left: 0;
  border-radius: 20px 0 0 20px;
}

.carousel-swiper :deep(.swiper-button-next) {
  right: 0;
  border-radius: 0 20px 20px 0;
}

.carousel-swiper :deep(.swiper-button-prev:hover),
.carousel-swiper :deep(.swiper-button-next:hover) {
  background-color: rgba(255, 255, 255, 0.3);
  width: 40px;
}

.carousel-swiper :deep(.swiper-button-prev::after),
.carousel-swiper :deep(.swiper-button-next::after) {
  font-size: 12px;
  font-weight: 600;
}

/* 诗文轮播箭头样式 */
.lbye_shiwen .carousel-swiper :deep(.swiper-button-prev),
.lbye_shiwen .carousel-swiper :deep(.swiper-button-next) {
  width: 25px;
  background-color: rgba(100, 100, 100, 0.2);
}

.lbye_shiwen .carousel-swiper :deep(.swiper-button-prev) {
  border-radius: 0 20px 20px 0;
  left: 0;
}

.lbye_shiwen .carousel-swiper :deep(.swiper-button-next) {
  border-radius: 20px 0 0 20px;
  right: 0;
}
</style>
