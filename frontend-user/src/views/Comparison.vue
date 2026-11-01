<template>
  <div class="page">
    <div class="inner">
      <div class="page-header">
        <h2 class="page-title">新老用户对比</h2>
      </div>

      <div v-if="loading" class="loading"><div class="spinner"></div>加载中...</div>
      <template v-else>
        <!-- 核心指标对比卡片 -->
        <div class="compare-header">
          <div class="user-badge new-badge">🆕 新用户</div>
          <div class="vs-divider">VS</div>
          <div class="user-badge old-badge">🔄 老用户</div>
        </div>

        <div v-for="metric in basicMetrics" :key="metric.key" class="card metric-compare">
          <div class="mc-label">{{ metric.label }}</div>
          <div class="mc-bars">
            <div class="mc-side new-side">
              <div class="mc-value" style="color: #00d4ff">{{ metric.newVal }}</div>
              <div class="mc-bar-wrap">
                <div class="mc-bar new-bar" :style="{ width: metric.newPct }"></div>
              </div>
            </div>
            <div class="mc-center">{{ metric.unit }}</div>
            <div class="mc-side old-side">
              <div class="mc-bar-wrap reverse">
                <div class="mc-bar old-bar" :style="{ width: metric.oldPct }"></div>
              </div>
              <div class="mc-value" style="color: #7c3aed">{{ metric.oldVal }}</div>
            </div>
          </div>
        </div>

        <!-- 雷达图 -->
        <div class="card section">
          <div class="section-title" style="margin-bottom:12px">行为偏好雷达</div>
          <div v-if="hasRadar" ref="radarRef" class="chart-radar"></div>
          <div v-else class="no-data">暂无数据</div>
        </div>

        <!-- 页面偏好对比柱图 -->
        <div class="card section">
          <div class="section-title" style="margin-bottom:12px">页面访问偏好对比</div>
          <div v-if="hasPage" ref="pageRef" class="chart-bar"></div>
          <div v-else class="no-data">暂无数据</div>
        </div>

        <!-- 流量来源对比 -->
        <div class="card section">
          <div class="section-title" style="margin-bottom:12px">流量来源对比</div>
          <div v-if="hasSrc" ref="sourceRef" class="chart-bar"></div>
          <div v-else class="no-data">暂无数据</div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getComparisonList } from '@/api/index.js'

const loading = ref(true)
const rawList = ref([])
const radarRef  = ref(null)
const pageRef   = ref(null)
const sourceRef = ref(null)
const hasRadar  = ref(false)
const hasPage   = ref(false)
const hasSrc    = ref(false)

// 按 userType + metricName 整理
const byType = computed(() => {
  const m = { new: {}, old: {} }
  rawList.value.forEach(item => {
    if (m[item.userType]) m[item.userType][item.metricName] = Number(item.metricValue)
  })
  return m
})

const basicMetrics = computed(() => {
  const n = byType.value.new || {}
  const o = byType.value.old || {}
  const max = (a, b) => Math.max(a || 0, b || 0) || 1
  return [
    { key: 'user_count',     label: '用户数量',   unit: '人',  newVal: fmt(n.user_count),     oldVal: fmt(o.user_count),     newPct: (n.user_count     || 0) / max(n.user_count, o.user_count) * 100 + '%',     oldPct: (o.user_count     || 0) / max(n.user_count, o.user_count) * 100 + '%' },
    { key: 'avg_pages',      label: '人均页数',   unit: '页',  newVal: fmtDec(n.avg_pages),   oldVal: fmtDec(o.avg_pages),   newPct: (n.avg_pages     || 0) / max(n.avg_pages, o.avg_pages) * 100 + '%',     oldPct: (o.avg_pages     || 0) / max(n.avg_pages, o.avg_pages) * 100 + '%' },
    { key: 'conversion_rate',label: '转化率',     unit: '%',   newVal: pct(n.conversion_rate),oldVal: pct(o.conversion_rate),newPct: (n.conversion_rate|| 0) / max(n.conversion_rate,o.conversion_rate)*100+'%', oldPct: (o.conversion_rate|| 0) / max(n.conversion_rate,o.conversion_rate)*100+'%' }
  ]
})

async function load() {
  loading.value = true
  try {
    rawList.value = await getComparisonList() || []
  } catch (e) { console.error(e) }
  loading.value = false
  await nextTick()
  renderCharts()
}

function getMetrics(type, category) {
  return rawList.value.filter(r => r.userType === type && r.metricCategory === category)
}

async function renderCharts() {
  const TIP = { backgroundColor: '#0d1626', borderColor: '#1e3a5f', textStyle: { color: '#e2e8f0', fontSize: 12 } }
  const GRID = { left: 0, right: 8, top: 16, bottom: 0, containLabel: true }

  // 雷达图
  const pageNew = getMetrics('new', 'pages_preference')
  const pageOld = getMetrics('old', 'pages_preference')
  hasRadar.value = pageNew.length > 0
  hasPage.value  = pageNew.length > 0
  const srcNew = getMetrics('new', 'source')
  const srcOld = getMetrics('old', 'source')
  hasSrc.value = srcNew.length > 0
  await nextTick()  // 等 v-if 把 chart div 挂载到 DOM
  const pageNames = { home_page_rate:'首页', listing_page_rate:'列表页', product_page_rate:'商品页', payment_page_rate:'支付页', confirmation_page_rate:'确认页' }
  if (pageNew.length && radarRef.value) {
    const rc = echarts.init(radarRef.value)
    const indicators = pageNew.map(r => ({ name: pageNames[r.metricName] || r.metricName, max: 1 }))
    rc.setOption({
      tooltip: { ...TIP },
      legend: { data: ['新用户','老用户'], textStyle: { color: '#94a3b8' }, bottom: 0 },
      radar: {
        indicator: indicators, center: ['50%','46%'], radius: '58%',
        axisName: { color: '#94a3b8', fontSize: 11 },
        splitLine: { lineStyle: { color: '#1e3a5f' } },
        splitArea: { areaStyle: { color: ['#0d1626','#111827'] } },
        axisLine: { lineStyle: { color: '#1e3a5f' } }
      },
      series: [{
        type: 'radar',
        data: [
          { name: '新用户', value: pageNew.map(r => Number(r.metricValue)), lineStyle: { color: '#00d4ff' }, areaStyle: { color: '#00d4ff33' }, itemStyle: { color: '#00d4ff' } },
          { name: '老用户', value: pageOld.map(r => Number(r.metricValue)), lineStyle: { color: '#7c3aed' }, areaStyle: { color: '#7c3aed33' }, itemStyle: { color: '#7c3aed' } }
        ]
      }]
    })
    new ResizeObserver(() => rc.resize()).observe(radarRef.value)
  }

  // 页面偏好柱图
  if (pageNew.length && pageRef.value) {
    const pc = echarts.init(pageRef.value)
    pc.setOption({
      tooltip: { trigger: 'axis', ...TIP },
      legend: { data: ['新用户','老用户'], textStyle: { color: '#94a3b8' }, right: 0, top: 0 },
      grid: GRID,
      xAxis: { type: 'category', data: pageNew.map(r => pageNames[r.metricName] || r.metricName), axisLabel: { color: '#475569', fontSize: 10 }, axisLine: { lineStyle: { color: '#1e3a5f' } } },
      yAxis: { type: 'value', axisLabel: { color: '#475569', fontSize: 10, formatter: v => (v*100).toFixed(0)+'%' }, splitLine: { lineStyle: { color: '#1e3a5f' } } },
      series: [
        { name: '新用户', type: 'bar', barGap: '10%', barMaxWidth: 20, data: pageNew.map(r => r.metricValue), itemStyle: { color: '#00d4ff99' } },
        { name: '老用户', type: 'bar', barGap: '10%', barMaxWidth: 20, data: pageOld.map(r => Number(r.metricValue)), itemStyle: { color: '#7c3aed99' } }
      ]
    })
    new ResizeObserver(() => pc.resize()).observe(pageRef.value)
  }

  // 来源柱图
  if (srcNew.length && sourceRef.value) {
    const srcNames = { direct_rate:'直接访问', seo_rate:'搜索引擎', ads_rate:'广告投放' }
    const sc = echarts.init(sourceRef.value)
    sc.setOption({
      tooltip: { trigger: 'axis', ...TIP },
      legend: { data: ['新用户','老用户'], textStyle: { color: '#94a3b8' }, right: 0, top: 0 },
      grid: GRID,
      xAxis: { type: 'category', data: srcNew.map(r => srcNames[r.metricName] || r.metricName), axisLabel: { color: '#475569', fontSize: 11 }, axisLine: { lineStyle: { color: '#1e3a5f' } } },
      yAxis: { type: 'value', axisLabel: { color: '#475569', fontSize: 10, formatter: v => (v*100).toFixed(0)+'%' }, splitLine: { lineStyle: { color: '#1e3a5f' } } },
      series: [
        { name: '新用户', type: 'bar', barGap: '10%', barMaxWidth: 24, data: srcNew.map(r => r.metricValue), itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#00d4ff'},{offset:1,color:'#00d4ff44'}]) } },
        { name: '老用户', type: 'bar', barGap: '10%', barMaxWidth: 24, data: srcOld.map(r => Number(r.metricValue)), itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#7c3aed'},{offset:1,color:'#7c3aed44'}]) } }
      ]
    })
    new ResizeObserver(() => sc.resize()).observe(sourceRef.value)
  }
}

const fmt    = v => v != null ? Number(v).toLocaleString() : '——'
const fmtDec = v => v != null ? Number(v).toFixed(2) : '——'
const pct    = v => v != null ? (Number(v) * 100).toFixed(2) + '%' : '——'
onMounted(load)
</script>

<style scoped>
.inner { padding: 16px; max-width: 600px; margin: 0 auto; }
.page-header { margin-bottom: 16px; }
.page-title  { font-size: 20px; font-weight: 700; }

/* VS 区 */
.compare-header {
  display: flex; align-items: center; justify-content: center; gap: 16px;
  margin-bottom: 14px;
}
.user-badge {
  padding: 6px 16px; border-radius: 20px; font-size: 13px; font-weight: 600;
}
.new-badge { background: #00d4ff22; color: #00d4ff; border: 1px solid #00d4ff44; }
.old-badge { background: #7c3aed22; color: #a78bfa;  border: 1px solid #7c3aed44; }
.vs-divider { font-size: 16px; font-weight: 800; color: var(--text-sub); }

/* 指标对比 */
.metric-compare { margin-bottom: 10px; }
.mc-label { font-size: 13px; color: var(--text-sub); margin-bottom: 8px; }
.mc-bars { display: flex; align-items: center; gap: 10px; }
.mc-side { flex: 1; display: flex; align-items: center; gap: 8px; }
.old-side { flex-direction: row-reverse; }
.mc-value { font-size: 15px; font-weight: 700; flex-shrink: 0; min-width: 60px; text-align: center; }
.mc-bar-wrap { flex: 1; height: 8px; background: var(--border); border-radius: 4px; overflow: hidden; }
.mc-bar-wrap.reverse { }
.mc-bar { height: 100%; border-radius: 4px; transition: width 1s ease; }
.new-bar { background: linear-gradient(90deg, #00d4ff44, #00d4ff); }
.old-bar { background: linear-gradient(90deg, #7c3aed, #7c3aed44); }
.mc-center { font-size: 11px; color: var(--text-muted); flex-shrink: 0; }

.section { margin-bottom: 14px; }
.section-title { font-size: 14px; font-weight: 600; }
.chart-radar { height: 260px; }
.chart-bar   { height: 180px; }
.no-data { text-align: center; padding: 32px 0; font-size: 13px; color: var(--text-muted); }
</style>
