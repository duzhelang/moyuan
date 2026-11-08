<script setup lang="ts">
import RecommendationScreen from '@/components/admin/RecommendationScreen.vue'
import AdminDemoToggle from '@/components/admin/AdminDemoToggle.vue'
</script>

<template>
  <div class="recommend-page">
    <div class="page-head">
      <div class="page-title-box">
        <h2 class="page-title">推荐与实验</h2>
        <p class="page-desc">
          推荐策略实时状态与离线实验评测。左边是实时埋点与缓存覆盖，右边是 6
          组离线结果，两者口径不同，请勿混读。
        </p>
      </div>
      <div class="page-actions">
        <AdminDemoToggle />
      </div>
    </div>
    <RecommendationScreen />
  </div>
</template>

<style scoped lang="scss">
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.page-title-box {
  min-width: 0;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 2px;
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
