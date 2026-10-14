<script setup lang="ts">
withDefaults(defineProps<{
  /** full：游戏页；mini：首页小窗口 */
  size?: 'full' | 'mini'
  phase?: string
  gameType?: 'feihua' | 'chain'
}>(), {
  size: 'full',
  phase: 'idle',
  gameType: 'feihua'
})
</script>

<template>
  <div class="game-stage" :class="[`is-${size}`, `phase-${phase}`, `type-${gameType}`]">
    <header v-if="$slots.head" class="stage-head">
      <slot name="head" />
    </header>
    <div class="stage-body">
      <section class="stage-main">
        <div v-if="$slots.timer" class="stage-timer">
          <slot name="timer" />
        </div>
        <slot name="keyword" />
        <div v-if="$slots.loading" class="stage-loading">
          <slot name="loading" />
        </div>
        <slot name="board" />
      </section>
      <aside v-if="$slots.aside" class="stage-aside">
        <slot name="aside" />
      </aside>
    </div>
    <div v-if="$slots.feedback" class="stage-feedback">
      <slot name="feedback" />
    </div>
    <div v-if="$slots.input" class="stage-input">
      <slot name="input" />
    </div>
    <slot />
  </div>
</template>

<style scoped lang="scss">
.game-stage {
  position: relative;
  border-radius: $border-radius-xl;
  border: 1px solid $border-color-bronze;
  background:
    linear-gradient(135deg, rgba($primary-50, 0.85), rgba($surface-primary, 0.95)),
    repeating-linear-gradient(90deg, rgba($border-color-bronze, 0.08) 0 1px, transparent 1px 22px);
  box-shadow: $box-shadow-lg;
  padding: $spacing-lg;
  box-sizing: border-box;

  &::before {
    content: '';
    position: absolute;
    inset: 6px;
    border: 1px solid rgba($border-color-bronze, 0.45);
    border-radius: calc(#{$border-radius-xl} - 4px);
    pointer-events: none;
  }
}

.stage-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.stage-body {
  display: flex;
  gap: $spacing-xl;
  align-items: flex-start;
}

.stage-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: $spacing-md;
}

.stage-aside {
  flex: 0 0 auto;
}

.stage-feedback {
  margin-top: $spacing-md;
}

.stage-input {
  margin-top: $spacing-lg;
  padding-top: $spacing-md;
  border-top: 1px dashed rgba($border-color-bronze, 0.7);
}

/* mini 形态：单列紧凑 */
.game-stage.is-mini {
  padding: $spacing-md;
  border-radius: $border-radius-lg;
  box-shadow: $box-shadow-md;

  &::before {
    inset: 4px;
    border-radius: calc(#{$border-radius-lg} - 4px);
  }
}

.game-stage.is-mini .stage-body {
  flex-direction: column;
  gap: $spacing-sm;
}

.game-stage.is-mini .stage-main {
  align-items: stretch;
  gap: $spacing-sm;
}

.game-stage.is-mini .stage-input {
  margin-top: $spacing-md;
  padding-top: $spacing-sm;
}
</style>
