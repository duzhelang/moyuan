<script setup lang="ts">
import RecommendationScreen from '@/components/admin/RecommendationScreen.vue'
</script>

<template>
  <div class="recommend-page">
    <div class="page-head">
      <h2 class="page-title">推荐与实验</h2>
      <p class="page-desc">
        推荐策略实时状态与离线实验评测。左边是实时埋点与缓存覆盖，右边是 6
        组离线结果，两者口径不同，请勿混读。
      </p>
    </div>
    <RecommendationScreen />
  </div>
</template>

<style scoped lang="scss">
.page-head {
  margin-bottom: 16px;
}

.page-title {
  font-size: 24px;
  font-weight: 500;
  color: var(--adm-text);
  margin: 0 0 6px;
}

.page-desc {
  font-size: 13px;
  color: var(--adm-text-muted);
  margin: 0;
}
</style>
