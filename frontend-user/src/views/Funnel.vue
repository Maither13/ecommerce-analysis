<template>
  <div class="page">
    <div class="inner">
      <div class="page-header">
        <h2 class="page-title">转化漏斗</h2>
        <div class="filter-tabs">
          <button
            v-for="f in filterTypes" :key="f.value"
            class="filter-btn" :class="{ active: activeFilter === f.value }"
            @click="switchFilter(f.value)"
          >{{ f.label }}</button>
        </div>
      </div>

      <div v-if="loading" class="loading"><div class="spinner"></div>加载中...</div>
      <template v-else>
        <!-- ECharts 漏斗图 -->
        <div class="card section">
          <div ref="funnelRef" class="chart-funnel"></div>
        </div>

        <!-- 步骤明细卡片 -->
        <div
          v-for="(step, idx) in steps" :key="step.stepName"
          class="card step-card"
          :style="{ '--bar-pct': (step.overallConversionRate * 100).toFixed(1) + '%',
                    '--bar-color': stepColors[idx % stepColors.length] }"
        >
          <div class="step-rank">{{ step.stepOrder }}</div>
          <div class="step-body">
            <div class="step-name">{{ nameMap[step.stepName] || step.stepName }}</div>
            <div class="step-count">{{ fmt(step.userCount) }} 人</div>
            <div class="step-bar">
              <div class="step-bar-fill"></div>
            </div>
            <div class="step-rates">
              <span class="rate-item" v-if="idx > 0">
                较上步 <b :style="{ color: '#ef4444' }">-{{ pct(step.lossRate) }}</b>
              </span>
              <span class="rate-item">
                整体转化 <b :style="{ color: stepColors[idx % stepColors.length] }">{{ pct(step.overallConversionRate) }}</b>
              </span>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getFunnel } from '@/api/index.js'

const filterTypes = [
  { value: 'ALL',      label: '全部' },
  { value: 'new_user', label: '新用户' }
]
const activeFilter = ref('ALL')
const loading = ref(true)
const steps = ref([])
const funnelRef = ref(null)

const stepColors = ['#00d4ff', '#7c3aed', '#10b981', '#f59e0b', '#ef4444']
const nameMap = {
  home_page: '首页',
  listing_page: '列表页',
  product_page: '商品页',
  payment_page: '支付页',
  confirmation_page: '确认页'
}

async function load() {
  loading.value = true
  try {
    steps.value = await getFunnel(activeFilter.value) || []
  } catch (e) { console.error(e) }
  loading.value = false
  await nextTick()
  renderFunnel()
}

function switchFilter(f) {
  activeFilter.value = f
  load()
}

function renderFunnel() {
  if (!funnelRef.value || !steps.value.length) return
  const chart = echarts.init(funnelRef.value)
  const max = steps.value[0]?.userCount || 1
  chart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: '#0d1626', borderColor: '#1e3a5f',
      textStyle: { color: '#e2e8f0', fontSize: 12 },
      formatter: p => `${p.name}<br/>用户数: ${Number(p.value).toLocaleString()}<br/>整体转化: ${(p.data.overall*100).toFixed(1)}%`
    },
    series: [{
      type: 'funnel',
      left: '5%', width: '90%',
      min: 0, max,
      minSize: '30%', maxSize: '100%',
      sort: 'descending',
      gap: 6,
      label: { position: 'inside', color: '#fff', fontSize: 13, fontWeight: 600,
               formatter: p => `${nameMap[p.data.rawName] || p.name}\n${Number(p.value).toLocaleString()}` },
      itemStyle: { borderWidth: 0 },
      data: steps.value.map((s, i) => ({
        name: nameMap[s.stepName] || s.stepName,
        rawName: s.stepName,
        value: s.userCount,
        overall: s.overallConversionRate,
        itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: stepColors[i % stepColors.length] + 'cc' },
          { offset: 1, color: stepColors[i % stepColors.length] }
        ]) }
      }))
    }]
  })
  new ResizeObserver(() => chart.resize()).observe(funnelRef.value)
}

const fmt = v => v != null ? Number(v).toLocaleString() : '——'
const pct = v => v != null ? (v * 100).toFixed(1) + '%' : '——'
onMounted(load)
</script>

<style scoped>
.inner { padding: 16px; max-width: 600px; margin: 0 auto; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 10px; }
.page-title  { font-size: 20px; font-weight: 700; }

.filter-tabs { display: flex; gap: 6px; }
.filter-btn {
  padding: 5px 12px; border-radius: 20px; font-size: 12px; cursor: pointer;
  border: 1px solid var(--border); background: transparent; color: var(--text-sub); transition: all .2s;
}
.filter-btn.active { border-color: var(--primary); background: #00d4ff22; color: var(--primary); }

.section { margin-bottom: 14px; }
.chart-funnel { height: 280px; }

.step-card {
  display: flex; align-items: flex-start; gap: 14px;
  margin-bottom: 10px; position: relative; overflow: hidden;
}
.step-card::before {
  content: ''; position: absolute; left: 0; top: 0; bottom: 0;
  width: 3px; background: var(--bar-color, var(--primary));
  border-radius: 3px 0 0 3px;
}
.step-rank {
  width: 28px; height: 28px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%; background: var(--bg-card2);
  border: 1px solid var(--border);
  font-size: 13px; font-weight: 700; color: var(--text-sub);
}
.step-body { flex: 1; }
.step-name  { font-size: 14px; font-weight: 600; margin-bottom: 4px; }
.step-count { font-size: 13px; color: var(--text-sub); margin-bottom: 8px; }
.step-bar   { height: 4px; background: var(--border); border-radius: 2px; overflow: hidden; margin-bottom: 8px; }
.step-bar-fill {
  height: 100%; background: var(--bar-color, var(--primary));
  width: var(--bar-pct, 0%); border-radius: 2px; transition: width 1s ease;
}
.step-rates { display: flex; gap: 16px; flex-wrap: wrap; }
.rate-item  { font-size: 12px; color: var(--text-sub); }
.rate-item b { font-weight: 700; }
</style>
