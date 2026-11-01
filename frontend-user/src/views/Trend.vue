<template>
  <div class="page">
    <div class="inner">
      <div class="page-header">
        <h2 class="page-title">访问趋势</h2>
        <!-- 时间范围选择 -->
        <div class="range-tabs">
          <button
            v-for="r in ranges" :key="r.days"
            class="range-btn" :class="{ active: activeRange === r.days }"
            @click="switchRange(r.days)"
          >{{ r.label }}</button>
        </div>
      </div>

      <div v-if="loading" class="loading"><div class="spinner"></div>加载中...</div>
      <template v-else>
        <!-- 当期汇总 -->
        <div class="sum-row">
          <div class="sum-card card">
            <div class="sum-label">总 PV</div>
            <div class="sum-val" style="color:var(--primary)">{{ fmt(totalPv) }}</div>
          </div>
          <div class="sum-card card">
            <div class="sum-label">总 UV</div>
            <div class="sum-val" style="color:var(--accent)">{{ fmt(totalUv) }}</div>
          </div>
          <div class="sum-card card">
            <div class="sum-label">UV/PV 比</div>
            <div class="sum-val" style="color:var(--success)">{{ uvPvRatio }}</div>
          </div>
        </div>

        <!-- 主趋势图 -->
        <div class="card section">
          <div class="section-header">
            <span class="section-title">PV / UV 走势</span>
            <span class="badge badge-blue">{{ activeRange }} 天</span>
          </div>
          <div ref="trendRef" class="chart-main"></div>
        </div>

        <!-- 新用户比例趋势 -->
        <div class="card section">
          <div class="section-title" style="margin-bottom:12px">新用户占比趋势</div>
          <div ref="ratioRef" class="chart-medium"></div>
        </div>

        <!-- 日均访问页数 -->
        <div class="card section">
          <div class="section-title" style="margin-bottom:12px">日均访问页数</div>
          <div ref="avgRef" class="chart-medium"></div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getHistory } from '@/api/index.js'

const ranges = [
  { days: 7, label: '7天' },
  { days: 14, label: '14天' },
  { days: 30, label: '30天' }
]
const activeRange = ref(7)
const loading = ref(true)
const list = ref([])

const totalPv = computed(() => list.value.reduce((s, m) => s + (m.pv || 0), 0))
const totalUv = computed(() => list.value.reduce((s, m) => s + (m.uv || 0), 0))
const uvPvRatio = computed(() => totalPv.value ? (totalUv.value / totalPv.value * 100).toFixed(1) + '%' : '——')

const trendRef = ref(null)
const ratioRef = ref(null)
const avgRef   = ref(null)

async function load() {
  loading.value = true
  try {
    list.value = await getHistory(activeRange.value) || []
  } catch (e) { console.error(e) }
  loading.value = false
  await nextTick()
  renderAll()
}

function switchRange(days) {
  activeRange.value = days
  load()
}

const GRID = { left: 0, right: 8, top: 24, bottom: 0, containLabel: true }
const AXIS_LABEL = { color: '#475569', fontSize: 11 }
const SPLIT_LINE = { lineStyle: { color: '#1e3a5f' } }
const TIP_STYLE = { backgroundColor: '#0d1626', borderColor: '#1e3a5f', textStyle: { color: '#e2e8f0', fontSize: 12 } }

function xAxis(data) {
  return { type: 'category', data, axisLabel: AXIS_LABEL, axisLine: { lineStyle: { color: '#1e3a5f' } } }
}
function yAxis() {
  return { type: 'value', axisLabel: { ...AXIS_LABEL, formatter: v => v >= 1000 ? (v/1000).toFixed(0)+'k' : v }, splitLine: SPLIT_LINE }
}
function line(name, data, color) {
  return {
    name, type: 'line', smooth: true, data,
    lineStyle: { color, width: 2 }, itemStyle: { color },
    areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:color+'55'},{offset:1,color:color+'00'}]) }
  }
}

function renderAll() {
  const dates = list.value.map(m => m.statDate)
  const pvData = list.value.map(m => m.pv || 0)
  const uvData = list.value.map(m => m.uv || 0)
  const ratioData = list.value.map(m => m.newUserRatio != null ? (m.newUserRatio * 100).toFixed(2) : 0)
  const avgData = list.value.map(m => m.avgPages != null ? Number(m.avgPages).toFixed(2) : 0)

  // PV/UV
  const c1 = echarts.init(trendRef.value)
  c1.setOption({
    tooltip: { trigger: 'axis', ...TIP_STYLE },
    legend: { data: ['PV','UV'], textStyle: { color: '#94a3b8' }, right: 0 },
    grid: GRID,
    xAxis: xAxis(dates),
    yAxis: yAxis(),
    series: [line('PV', pvData, '#00d4ff'), line('UV', uvData, '#7c3aed')]
  })

  // 新用户比例
  const c2 = echarts.init(ratioRef.value)
  c2.setOption({
    tooltip: { trigger: 'axis', ...TIP_STYLE, formatter: p => `${p[0].name}<br/>新用户占比: ${p[0].value}%` },
    grid: GRID,
    xAxis: xAxis(dates),
    yAxis: { ...yAxis(), axisLabel: { ...AXIS_LABEL, formatter: v => v + '%' }, max: 100 },
    series: [{ ...line('新用户占比', ratioData, '#10b981'), areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#10b98155'},{offset:1,color:'#10b98100'}]) } }]
  })

  // 日均页数
  const c3 = echarts.init(avgRef.value)
  c3.setOption({
    tooltip: { trigger: 'axis', ...TIP_STYLE, formatter: p => `${p[0].name}<br/>人均页数: ${p[0].value}` },
    grid: GRID,
    xAxis: xAxis(dates),
    yAxis: { ...yAxis(), axisLabel: AXIS_LABEL },
    series: [{
      type: 'bar', barMaxWidth: 28, data: avgData,
      itemStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#f59e0b'},{offset:1,color:'#f59e0b44'}]) },
      label: { show: true, position: 'top', color: '#f59e0b', fontSize: 10 }
    }]
  })

  const ro = new ResizeObserver(() => [c1,c2,c3].forEach(c => c.resize()))
  ;[trendRef, ratioRef, avgRef].forEach(r => r.value && ro.observe(r.value))
}

const fmt = v => v != null ? Number(v).toLocaleString() : '——'
onMounted(load)
</script>

<style scoped>
.inner { padding: 16px; max-width: 600px; margin: 0 auto; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 10px; }
.page-title  { font-size: 20px; font-weight: 700; }

.range-tabs { display: flex; gap: 6px; }
.range-btn {
  padding: 5px 12px; border-radius: 20px; font-size: 12px; cursor: pointer;
  border: 1px solid var(--border); background: transparent; color: var(--text-sub);
  transition: all .2s;
}
.range-btn.active { border-color: var(--primary); background: #00d4ff22; color: var(--primary); }

.sum-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 14px; }
.sum-card { text-align: center; }
.sum-label { font-size: 11px; color: var(--text-sub); margin-bottom: 6px; }
.sum-val   { font-size: 18px; font-weight: 700; }

.section { margin-bottom: 14px; }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.section-title  { font-size: 14px; font-weight: 600; }
.chart-main   { height: 220px; }
.chart-medium { height: 160px; }
</style>
