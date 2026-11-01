<template>
  <div class="page">
    <div class="inner">
      <div class="page-header">
        <h2 class="page-title">多维分析</h2>
      </div>

      <!-- 维度切换 Tab -->
      <div class="dim-tabs card">
        <button
          v-for="d in dims" :key="d.value"
          class="dim-btn" :class="{ active: activeDim === d.value }"
          @click="switchDim(d.value)"
        >
          {{ d.icon }} {{ d.label }}
        </button>
      </div>

      <div v-if="loading" class="loading"><div class="spinner"></div>加载中...</div>
      <template v-else-if="rows.length">
        <!-- 饼图/环图 -->
        <div class="card section">
          <div class="section-header">
            <span class="section-title">{{ currentDim?.label }} 用户分布</span>
            <span class="badge badge-purple">{{ rows.length }} 个维度值</span>
          </div>
          <div ref="pieRef" class="chart-pie"></div>
        </div>

        <!-- 柱状图：用户数 + 转化率 -->
        <div class="card section">
          <div class="section-title" style="margin-bottom:12px">转化率对比</div>
          <div ref="barRef" class="chart-bar"></div>
        </div>

        <!-- 明细列表 -->
        <div class="card section">
          <div class="section-title" style="margin-bottom:12px">明细数据</div>
          <div class="dim-table">
            <div class="dt-head dt-row">
              <span>{{ currentDim?.label }}</span>
              <span>用户数</span>
              <span>PV</span>
              <span>转化率</span>
            </div>
            <div class="dt-row" v-for="(row, idx) in rows" :key="idx">
              <span class="dt-label">
                <span class="dot" :style="{ background: colors[idx % colors.length] }"></span>
                {{ row.dimensionValue }}
              </span>
              <span>{{ fmt(row.userCount) }}</span>
              <span>{{ fmt(row.pv) }}</span>
              <span :style="{ color: '#10b981', fontWeight: 600 }">{{ pct(row.conversionRate) }}</span>
            </div>
          </div>
        </div>
      </template>
      <div v-else class="loading">暂无数据</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDimensionAll } from '@/api/index.js'

const dims = [
  { value: 'age',    icon: '🎂', label: '年龄段' },
  { value: 'sex',    icon: '⚧',  label: '性别' },
  { value: 'device', icon: '📱', label: '设备' },
  { value: 'source', icon: '🌐', label: '流量来源' },
  { value: 'os',     icon: '💻', label: '操作系统' }
]
const colors = ['#00d4ff','#7c3aed','#10b981','#f59e0b','#ef4444','#06b6d4','#8b5cf6']

const activeDim = ref('age')
const loading = ref(true)
const rows = ref([])
const pieRef = ref(null)
const barRef = ref(null)

const currentDim = computed(() => dims.find(d => d.value === activeDim.value))

async function load() {
  loading.value = true
  try {
    rows.value = await getDimensionAll(activeDim.value) || []
  } catch (e) { console.error(e) }
  loading.value = false
  await nextTick()
  renderCharts()
}

function switchDim(v) {
  activeDim.value = v
  load()
}

function renderCharts() {
  if (!rows.value.length) return

  const TIP = { backgroundColor: '#0d1626', borderColor: '#1e3a5f', textStyle: { color: '#e2e8f0', fontSize: 12 } }

  // 销毁旧实例再重建，避免重复初始化报错
  ;[pieRef, barRef].forEach(r => {
    if (r.value) {
      const old = echarts.getInstanceByDom(r.value)
      if (old) old.dispose()
    }
  })

  // 环形饼图
  const pc = echarts.init(pieRef.value)
  pc.setOption({
    tooltip: { trigger: 'item', ...TIP, formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', right: 8, top: 'middle', textStyle: { color: '#94a3b8', fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['35%','65%'], center: ['38%','50%'],
      label: { show: false },
      data: rows.value.map((r, i) => ({
        name: r.dimensionValue, value: r.userCount,
        itemStyle: { color: colors[i % colors.length] }
      }))
    }]
  })

  // 转化率柱图
  const bc = echarts.init(barRef.value)
  bc.setOption({
    tooltip: { trigger: 'axis', ...TIP, formatter: p => `${p[0].name}<br/>转化率: ${(Number(p[0].value)*100).toFixed(2)}%` },
    grid: { left: 0, right: 8, top: 8, bottom: 0, containLabel: true },
    xAxis: { type: 'category', data: rows.value.map(r => r.dimensionValue),
              axisLabel: { color: '#475569', fontSize: 11 }, axisLine: { lineStyle: { color: '#1e3a5f' } } },
    yAxis: { type: 'value', axisLabel: { color: '#475569', fontSize: 10, formatter: v => (v*100).toFixed(0)+'%' }, splitLine: { lineStyle: { color: '#1e3a5f' } } },
    series: [{
      type: 'bar', barMaxWidth: 28,
      data: rows.value.map((r, i) => ({
        value: r.conversionRate,
        itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:colors[i%colors.length]},{offset:1,color:colors[i%colors.length]+'44'}]) }
      })),
      label: { show: true, position: 'top', color: '#94a3b8', fontSize: 10,
               formatter: p => (Number(p.value)*100).toFixed(1)+'%' }
    }]
  })

  const ro = new ResizeObserver(() => [pc, bc].forEach(c => c.resize()))
  ;[pieRef, barRef].forEach(r => r.value && ro.observe(r.value))
}

const fmt = v => v != null ? Number(v).toLocaleString() : '——'
const pct = v => v != null ? (v * 100).toFixed(2) + '%' : '——'
onMounted(load)
</script>

<style scoped>
.inner { padding: 16px; max-width: 600px; margin: 0 auto; }
.page-header { margin-bottom: 14px; }
.page-title  { font-size: 20px; font-weight: 700; }

.dim-tabs {
  display: flex; gap: 6px; padding: 10px; margin-bottom: 14px;
  overflow-x: auto; flex-wrap: nowrap;
  scrollbar-width: none;
}
.dim-tabs::-webkit-scrollbar { display: none; }
.dim-btn {
  flex: 0 0 auto; padding: 7px 12px; border-radius: 10px;
  border: 1px solid var(--border); background: transparent; color: var(--text-sub);
  font-size: 12px; cursor: pointer; text-align: center; transition: all .2s; white-space: nowrap;
}
.dim-btn.active { border-color: var(--accent); background: #7c3aed22; color: #a78bfa; }

.section { margin-bottom: 14px; }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.section-title  { font-size: 14px; font-weight: 600; }
.chart-pie { height: 220px; }
.chart-bar { height: 160px; }

/* 明细表 */
.dim-table .dt-row {
  display: grid; grid-template-columns: 1.5fr 1fr 1fr 1fr;
  padding: 9px 4px; border-bottom: 1px solid var(--border);
  font-size: 13px; align-items: center;
}
.dim-table .dt-row:last-child { border-bottom: none; }
.dt-head { font-size: 11px; color: var(--text-sub); font-weight: 600; }
.dt-label { display: flex; align-items: center; gap: 6px; }
.dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
</style>
