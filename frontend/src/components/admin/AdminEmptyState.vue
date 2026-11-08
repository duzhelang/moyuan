<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
    note?: string
    /** 演示态用「示」字印章，与真实空数据区分 */
    demo?: boolean
  }>(),
  { title: '暂无数据', note: '', demo: false }
)
</script>

<template>
  <div class="adm-empty">
    <span class="ae-seal">{{ demo ? '示' : '墨' }}</span>
    <div class="ae-text">
      <b>{{ title }}</b>
      <p v-if="note">{{ note }}</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
.adm-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 120px;
  padding: 16px;
  color: var(--adm-text-faint);
}

.ae-seal {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  border: 1px solid color-mix(in srgb, var(--adm-primary) 32%, transparent);
  border-radius: 8px;
  font-family: var(--adm-font-serif);
  font-size: 16px;
  color: color-mix(in srgb, var(--adm-primary) 72%, var(--adm-text-faint));
  background: color-mix(in srgb, var(--adm-primary) 6%, transparent);
}

.ae-text {
  min-width: 0;

  b {
    display: block;
    font-size: 12.5px;
    font-weight: 500;
    color: var(--adm-text-muted);
  }

  p {
    margin: 3px 0 0;
    font-size: 11px;
    line-height: 1.6;
    color: var(--adm-text-faint);
  }
}
</style>
