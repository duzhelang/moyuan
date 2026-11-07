<script setup lang="ts">
import { computed } from 'vue'
import type { ChainNode } from '@/types/admin-dashboard'

const props = withDefaults(defineProps<{ nodes?: ChainNode[] }>(), {
  nodes: () => []
})

const STATUS_TEXT: Record<string, string> = {
  ok: '正常',
  warn: '降级',
  down: '断连'
}

/**
 * 占位链路：health 接口（/api/admin/system/health）未接入前使用，
 * 节点按「有没有真实数据」标记 live，避免把占位值当成实时指标。
 */
const FALLBACK_NODES: ChainNode[] = [
  {
    key: 'web',
    name: '前端访问',
    status: 'ok',
    latency: '—',
    live: false,
    metrics: [
      { label: '今日 PV', value: '—' },
      { label: '今日 UV', value: '—' },
      { label: '首屏加载', value: '—' },
      { label: '静态命中', value: '—' }
    ]
  },
  {
    key: 'api',
    name: '后端 API',
    status: 'ok',
    latency: '—',
    live: false,
    metrics: [
      { label: 'QPS', value: '—' },
      { label: 'P95 延迟', value: '—' },
      { label: '5xx 率', value: '—' },
      { label: '活跃线程', value: '—' }
    ]
  },
  {
    key: 'mysql',
    name: 'MySQL',
    status: 'ok',
    latency: '—',
    live: false,
    metrics: [
      { label: '连接池', value: '—' },
      { label: '慢查询', value: '—' },
      { label: '主从延迟', value: '—' },
      { label: 'QPS', value: '—' }
    ]
  },
  {
    key: 'redis',
    name: 'Redis',
    status: 'ok',
    latency: '—',
    live: false,
    metrics: [
      { label: '命中率', value: '—' },
      { label: '内存占用', value: '—' },
      { label: 'Key 数量', value: '—' },
      { label: '淘汰次数', value: '—' }
    ]
  },
  {
    key: 'rag',
    name: 'RAG 服务',
    status: 'ok',
    latency: '—',
    live: false,
    metrics: [
      { label: '索引文档', value: '—' },
      { label: '检索耗时', value: '—' },
      { label: 'Ollama 模型', value: '—' },
      { label: '生成耗时', value: '—' }
    ]
  }
]

/** 节点职责说明；health 接口不可用时也要能解释清楚，所以这里再兜一份 */
const NODE_DESC: Record<string, string> = {
  web: '浏览器到站点的入口层，PV 是接口请求数，UV 按来源 IP 去重。',
  api: 'Spring Boot 主服务，所有业务请求的必经节点，指标取自 JVM 运行时。',
  mysql: '主数据库，存放诗词、诗人、用户等核心业务数据，探活执行 SELECT 1。',
  redis: '缓存与会话存储，命中率偏低通常意味着缓存未预热或 Key 已过期。',
  rag: '诗词知识检索与生成服务，依赖本地 Ollama 模型，未启动时 AI 问答不可用。'
}

const list = computed<ChainNode[]>(() =>
  (props.nodes.length ? props.nodes : FALLBACK_NODES).map((n) => ({
    ...n,
    desc: n.desc || NODE_DESC[n.key]
  }))
)

const onlineCount = computed(
  () => list.value.filter((n) => n.status !== 'down').length
)

const allLive = computed(() => list.value.every((n) => n.live))

/** 端到端耗时：把各节点延迟相加，「<1 ms」按 0 计入但仍算作有效样本 */
const totalLatency = computed(() => {
  let sum = 0
  let counted = 0
  for (const n of list.value) {
    if (n.latency === '断连') continue
    const m = /^(\d+)\s*ms$/.exec(n.latency || '')
    if (m) {
      sum += Number(m[1])
      counted++
    } else if (n.latency === '<1 ms') {
      counted++
    }
  }
  if (!counted) return '—'
  return sum === 0 ? '<1 ms' : `${sum} ms`
})
</script>

<template>
  <el-card class="chain-card" shadow="never">
    <div class="chain-head">
      <span class="chain-title">系统链路 · 请求主路径</span>
      <span class="chain-meta">
        <em class="dot" :class="{ warn: onlineCount < list.length }"></em>
        {{ onlineCount }} / {{ list.length }} 在线
        <span class="sep">·</span>
        端到端 {{ totalLatency }}
        <span v-if="!allLive" class="pending">· 部分节点待接入</span>
      </span>
    </div>

    <div class="chain">
      <template v-for="(node, index) in list" :key="node.key">
        <el-popover
          placement="top"
          :width="340"
          trigger="hover"
          :show-after="200"
          popper-class="chain-popper"
        >
          <template #reference>
            <div class="node" :class="node.status">
              <span class="node-ring">
                <i class="node-core"></i>
              </span>
              <span class="node-name">{{ node.name }}</span>
              <span class="node-latency">{{ node.latency }}</span>
            </div>
          </template>
          <div class="detail">
            <div class="detail-head">
              <span class="detail-name">{{ node.name }}</span>
              <span class="detail-status" :class="node.status">
                {{ STATUS_TEXT[node.status] }}
              </span>
            </div>

            <div class="detail-grid">
              <div v-for="m in node.metrics" :key="m.label" class="detail-row">
                <span class="detail-label">{{ m.label }}</span>
                <span class="detail-value">{{ m.value }}</span>
              </div>
            </div>

            <p v-if="node.desc" class="detail-desc">{{ node.desc }}</p>

            <div class="detail-foot">
              <span>{{ node.live ? '数据来自健康检查接口' : '健康检查接口待接入' }}</span>
              <span>30 秒刷新</span>
            </div>
            <p class="detail-rule">
              判定规则：连接失败记为「断连」，探测超过 800ms 记为「降级」，其余为「正常」。
            </p>
          </div>
        </el-popover>

        <div v-if="index < list.length - 1" class="link" aria-hidden="true">
          <i class="link-base"></i>
          <i class="link-flow"></i>
        </div>
      </template>
    </div>
  </el-card>
</template>

<style scoped lang="scss">
.chain-card {
  border-radius: 8px;
  margin-bottom: 20px;

  :deep(.el-card__body) {
    padding: 16px 20px 18px;
  }
}

.chain-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.chain-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--adm-text);
}

.chain-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--adm-ok);

  .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--adm-ok);

    &.warn {
      background: var(--adm-warn);
    }
  }

  .pending {
    color: var(--adm-text-faint);
  }

  .sep {
    color: var(--adm-border);
  }
}

.chain {
  display: flex;
  align-items: flex-start;
  padding: 14px 22px 10px;
  background: var(--adm-surface-muted);
  border: 1px solid var(--adm-border-soft);
  border-radius: 6px;
}

.node {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  min-width: 132px;
  cursor: default;

  .node-ring {
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: color-mix(in srgb, var(--adm-chart-1) 12%, transparent);
    border: 2px solid var(--adm-primary);
    box-sizing: border-box;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: box-shadow 0.15s;
  }

  .node-core {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: var(--adm-primary);
  }

  .node-name {
    font-size: 13px;
    font-weight: 500;
    color: var(--adm-text);
    white-space: nowrap;
  }

  .node-latency {
    font-size: 12px;
    color: var(--adm-text-muted);
    font-variant-numeric: tabular-nums;
  }

  &.warn {
    .node-ring {
      background: color-mix(in srgb, var(--adm-warn) 12%, transparent);
      border-color: var(--adm-warn);
    }

    .node-core {
      background: var(--adm-warn);
    }

    .node-latency {
      color: var(--adm-warn);
    }
  }

  &.down {
    .node-ring {
      background: color-mix(in srgb, var(--adm-down) 12%, transparent);
      border-color: var(--adm-down);
    }

    .node-core {
      background: var(--adm-down);
    }

    .node-latency {
      color: var(--adm-down);
    }
  }

  &:hover {
    .node-name {
      color: var(--adm-primary);
    }

    .node-ring {
      box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.12);
    }
  }
}

.link {
  position: relative;
  flex: 1;
  height: 22px;
  margin: 0 2px;
  align-self: flex-start;
  margin-top: 11px;
}

.link-base,
.link-flow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
}

.link-base {
  background: var(--adm-border);
}

.link-flow {
  background-image: repeating-linear-gradient(
    90deg,
    var(--adm-primary) 0 5px,
    transparent 5px 14px
  );
  background-size: 14px 2px;
  opacity: 0.75;
  animation: chain-flow 2.2s linear infinite;
}

@keyframes chain-flow {
  to {
    background-position-x: 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .link-flow {
    animation: none;
  }
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--adm-border-soft);
  margin-bottom: 8px;
}

.detail-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--adm-text);
}

.detail-status {
  padding: 1px 8px;
  border-radius: 9px;
  font-size: 11px;
  flex-shrink: 0;

  &.ok {
    background: color-mix(in srgb, var(--adm-ok) 12%, transparent);
    color: var(--adm-ok);
  }

  &.warn {
    background: color-mix(in srgb, var(--adm-warn) 12%, transparent);
    color: var(--adm-warn);
  }

  &.down {
    background: color-mix(in srgb, var(--adm-down) 12%, transparent);
    color: var(--adm-down);
  }
}

/* 单列更易读：数据源、地址这类长值不会被两列挤掉 */
.detail-grid {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.detail-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  font-size: 12.5px;
  line-height: 1.6;
  min-width: 0;
}

.detail-label {
  color: var(--adm-text-muted);
  flex-shrink: 0;
}

.detail-value {
  color: var(--adm-text);
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  text-align: right;
  word-break: break-all;
}

.detail-desc {
  margin: 10px 0 0;
  padding: 9px 11px;
  background: var(--adm-surface-muted);
  border-radius: 4px;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--adm-text-2);
}

.detail-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--adm-border-soft);
  font-size: 11px;
  color: var(--adm-text-faint);
}

.detail-rule {
  margin: 6px 0 0;
  font-size: 11px;
  line-height: 1.7;
  color: var(--adm-text-faint);
}
</style>
