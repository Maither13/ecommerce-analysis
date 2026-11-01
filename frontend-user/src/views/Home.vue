<template>
  <div class="page home-page">
    <div class="inner">
      <!-- 页头 -->
      <div class="page-header">
        <div>
          <h2 class="page-title">数据总览</h2>
          <p class="page-sub">{{ latestDate || '——' }}</p>
        </div>
        <button class="refresh-btn" @click="load" :class="{ spinning: loading }">🔄</button>
      </div>

      <!-- 4 大指标卡片 -->
      <div v-if="loading" class="loading"><div class="spinner"></div>加载中...</div>
      <div v-else class="metric-grid">
        <div class="metric-card card" v-for="m in metrics" :key="m.key">
          <div class="metric-icon">{{ m.icon }}</div>
          <div class="metric-val num-large" :style="{ color: m.color }">{{ m.value }}</div>
          <div class="label">{{ m.label }}</div>
          <div class="metric-bar">
            <div class="metric-bar-fill" :style="{ width: m.pct + '%', background: m.color }"></div>
          </div>
        </div>
      </div>

      <!-- 最近趋势迷你图 -->
      <div class="card section">
        <div class="section-header">
          <span class="section-title">近 7 天 PV/UV 趋势</span>
          <span class="badge badge-blue">实时</span>
        </div>
        <div ref="miniChartRef" class="mini-chart"></div>
      </div>

      <!-- 流量来源 + 设备占比 -->
      <div class="card-row">
        <div class="card half-card">
          <div class="section-title" style="margin-bottom:10px">流量来源</div>
          <div ref="sourceChartRef" class="half-chart"></div>
        </div>
        <div class="card half-card">
          <div class="section-title" style="margin-bottom:10px">设备分布</div>
          <div ref="deviceChartRef" class="half-chart"></div>
        </div>
      </div>

      <!-- 数据概况小结 -->
      <div class="card section" v-if="info">
        <div class="section-title" style="margin-bottom:12px">数据库概况</div>
        <div class="info-row" v-for="(v,k) in infoDisplay" :key="k">
          <span class="info-key">{{ k }}</span>
          <span class="info-val">{{ v }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getLatestMetrics, getTrend, getDimensionAll, getStatInfo } from '@/api/index.js'

const loading = ref(true)
const data = ref({})
const trendData = ref({})
const sourceData = ref([])
const deviceData = ref([])
const info = ref(null)

const latestDate = computed(() => data.value.statDate || '')

const metrics = computed(() => {
  const d = data.value
  return [
    { key: 'pv',    icon: '👁️',  label: '页面浏览量(PV)', value: fmt(d.pv),    color: '#00d4ff', pct: 100 },
    { key: 'uv',    icon: '👤',  label: '独立访客(UV)',   value: fmt(d.uv),    color: '#7c3aed', pct: d.uv && d.pv ? Math.round(d.uv/d.pv*100) : 0 },
    { key: 'conv',  icon: '💰',  label: '整体转化率',      value: pct(d.conversionRate), color: '#10b981', pct: d.conversionRate ? d.conversionRate*100 : 0 },
    { key: 'ratio', icon: '🆕', label: '新用户占比',      value: pct(d.newUserRatio),   color: '#f59e0b', pct: d.newUserRatio ? d.newUserRatio*100 : 0 }
  ]
})

const infoDisplay = computed(() => info.value ? {
  '基础指标记录': info.value.basicMetricsCount + ' 条',
  '多维度记录':   info.value.multiDimensionCount + ' 条',
  '漏斗步骤记录': info.value.funnelStepCount + ' 条',
  '用户对比记录': info.value.userComparisonCount + ' 条',
  '最新统计日期': info.value.latestStatDate || '——'
} : {})

const miniChartRef  = ref(null)
const sourceChartRef = ref(null)
const deviceChartRef = ref(null)
let charts = []

async function load() {
  loading.value = true
  try {
    const [m, t, s, d2, i] = await Promise.all([
      getLatestMetrics(),
      getTrend({ startDate: daysAgo(7), endDate: today() }),
      getDimensionAll('source'),
      getDimensionAll('device'),
      getStatInfo()
    ])
    data.value = m || {}
    trendData.value = t || {}
    sourceData.value = s || []
    deviceData.value = d2 || []
    info.value = i
  } catch (e) { console.error(e) }
  loading.value = false
  await nextTick()
  renderCharts()
}

function renderCharts() {
  // 趋势迷你图
  const mc = echarts.init(miniChartRef.value, null, { renderer: 'canvas' })
  mc.setOption({
    tooltip: { trigger: 'axis', backgroundColor: '#0d1626', borderColor: '#1e3a5f', textStyle: { color: '#e2e8f0', fontSize: 12 } },
    legend: { data: ['PV', 'UV'], textStyle: { color: '#94a3b8' }, right: 0, top: 0 },
    grid: { left: 0, right: 0, top: 28, bottom: 0, containLabel: true },
    xAxis: { type: 'category', data: trendData.value.dates || [], axisLabel: { color: '#475569', fontSize: 11 }, axisLine: { lineStyle: { color: '#1e3a5f' } } },
    yAxis: { type: 'value', axisLabel: { color: '#475569', fontSize: 10, formatter: v => v >= 1000 ? (v/1000).toFixed(0)+'k' : v }, splitLine: { lineStyle: { color: '#1e3a5f' } } },
    series: [
      { name: 'PV', type: 'line', smooth: true, data: trendData.value.pvData || [],
        lineStyle: { color: '#00d4ff', width: 2 }, itemStyle: { color: '#00d4ff' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1, [{offset:0, color:'#00d4ff44'},{offset:1, color:'#00d4ff00'}]) } },
      { name: 'UV', type: 'line', smooth: true, data: trendData.value.uvData || [],
        lineStyle: { color: '#7c3aed', width: 2 }, itemStyle: { color: '#7c3aed' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1, [{offset:0, color:'#7c3aed44'},{offset:1, color:'#7c3aed00'}]) } }
    ]
  })

  // 来源饼图
  const sc = echarts.init(sourceChartRef.value)
  const srcColors = ['#00d4ff','#7c3aed','#10b981','#f59e0b','#ef4444']
  sc.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)', backgroundColor: '#0d1626', borderColor: '#1e3a5f', textStyle: { color: '#e2e8f0' } },
    series: [{
      type: 'pie', radius: ['40%','70%'], center: ['50%','50%'],
      data: sourceData.value.map((i,idx) => ({ name: i.dimensionValue, value: i.userCount, itemStyle: { color: srcColors[idx % srcColors.length] } })),
      label: { color: '#94a3b8', fontSize: 11 }
    }]
  })

  // 设备柱图
  const dc = echarts.init(deviceChartRef.value)
  dc.setOption({
    tooltip: { trigger: 'axis', backgroundColor: '#0d1626', borderColor: '#1e3a5f', textStyle: { color: '#e2e8f0' } },
    grid: { left: 0, right: 8, top: 8, bottom: 0, containLabel: true },
    xAxis: { type: 'category', data: deviceData.value.map(i => i.dimensionValue), axisLabel: { color: '#94a3b8', fontSize: 11 }, axisLine: { lineStyle: { color: '#1e3a5f' } } },
    yAxis: { type: 'value', axisLabel: { color: '#475569', fontSize: 10 }, splitLine: { lineStyle: { color: '#1e3a5f' } } },
    series: [{
      type: 'bar', barMaxWidth: 32,
      data: deviceData.value.map((i,idx) => ({ value: i.userCount, itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1, [{offset:0,color:['#00d4ff','#7c3aed','#10b981'][idx%3]},{offset:1,color:['#00d4ff44','#7c3aed44','#10b98144'][idx%3]}]) } })),
      label: { show: true, position: 'top', color: '#94a3b8', fontSize: 11, formatter: v => v >= 1000 ? (v/1000).toFixed(0)+'k' : v }
    }]
  })

  charts = [mc, sc, dc]
  const ro = new ResizeObserver(() => charts.forEach(c => c.resize()))
  ;[miniChartRef, sourceChartRef, deviceChartRef].forEach(r => r.value && ro.observe(r.value))
}

// utils
const fmt = v => v != null ? Number(v).toLocaleString() : '——'
const pct = v => v != null ? (v * 100).toFixed(2) + '%' : '——'
const today = () => new Date().toISOString().slice(0, 10)
const daysAgo = n => { const d = new Date(); d.setDate(d.getDate() - n); return d.toISOString().slice(0, 10) }

onMounted(load)
</script>

<style scoped>
.inner { padding: 16px; max-width: 600px; margin: 0 auto; }

.page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; }
.page-title  { font-size: 20px; font-weight: 700; margin-bottom: 4px; }
.page-sub    { font-size: 12px; color: var(--text-sub); }

.refresh-btn {
  background: none; border: none; font-size: 20px; cursor: pointer;
  padding: 4px; transition: transform .3s;
}
.refresh-btn.spinning { animation: spin .8s linear infinite; }

/* 指标卡片 */
.metric-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px; }
.metric-card { display: flex; flex-direction: column; gap: 6px; }
.metric-icon { font-size: 24px; }
.metric-val  { margin-bottom: 2px; }
.metric-bar  { height: 3px; background: var(--border); border-radius: 2px; overflow: hidden; margin-top: 6px; }
.metric-bar-fill { height: 100%; border-radius: 2px; transition: width .8s ease; }

/* Section */
.section { margin-bottom: 16px; }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.section-title  { font-size: 14px; font-weight: 600; color: var(--text-main); }
.mini-chart { height: 180px; }

/* 二列卡片 */
.card-row   { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px; }
.half-card  { }
.half-chart { height: 160px; }

/* 信息行 */
.info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid var(--border); }
.info-row:last-child { border-bottom: none; }
.info-key { font-size: 13px; color: var(--text-sub); }
.info-val { font-size: 13px; font-weight: 600; color: var(--primary); }
</style>
