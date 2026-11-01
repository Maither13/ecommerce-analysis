<template>
  <div class="user-comparison-container">
    <el-page-header @back="$router.back()" title="返回">
      <template #content>
        <span class="page-title">新老用户对比分析</span>
      </template>
    </el-page-header>

    <!-- 核心指标对比卡片 -->
    <el-row :gutter="20" style="margin-top: 20px" v-loading="loading">
      <el-col :span="12">
        <el-card class="comparison-card">
          <template #header>
            <div class="card-header">
              <el-icon :size="20" color="#67C23A"><UserFilled /></el-icon>
              <span>新用户</span>
            </div>
          </template>
          <div class="metric-grid">
            <div class="metric-item" v-for="m in metricDefs" :key="'new-' + m.key">
              <div class="metric-value" style="color:#67C23A">{{ m.fmt(newStats[m.key]) }}</div>
              <div class="metric-label">{{ m.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="comparison-card">
          <template #header>
            <div class="card-header">
              <el-icon :size="20" color="#409EFF"><User /></el-icon>
              <span>老用户</span>
            </div>
          </template>
          <div class="metric-grid">
            <div class="metric-item" v-for="m in metricDefs" :key="'old-' + m.key">
              <div class="metric-value" style="color:#409EFF">{{ m.fmt(oldStats[m.key]) }}</div>
              <div class="metric-label">{{ m.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 对比柱图 -->
    <el-card style="margin-top: 20px">
      <template #header><span>新老用户核心指标对比</span></template>
      <div ref="comparisonChartRef" style="height: 360px"></div>
    </el-card>

    <!-- 详细对比表格 -->
    <el-card style="margin-top: 20px">
      <template #header><span>详细对比数据</span></template>
      <el-table :data="tableRows" border stripe>
        <el-table-column prop="label"    label="指标"   width="160" />
        <el-table-column prop="newVal"   label="新用户" width="150">
          <template #default="{ row }">
            <span style="color:#67C23A; font-weight:600">{{ row.newVal }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="oldVal"   label="老用户" width="150">
          <template #default="{ row }">
            <span style="color:#409EFF; font-weight:600">{{ row.oldVal }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="diff"     label="差异"   width="150">
          <template #default="{ row }">
            <el-tag v-if="row.diff !== null" :type="row.diff > 0 ? 'success' : row.diff < 0 ? 'danger' : 'info'">
              {{ row.diff > 0 ? '+' : '' }}{{ row.diff }}%
            </el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="insight"  label="洞察"   min-width="200" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User, UserFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getUserComparisonList } from '@/api/stat'

const loading           = ref(false)
const rawList           = ref([])
const comparisonChartRef = ref(null)
let compChart = null

// 从原始列表中按 userType + metricName 提取 Map
const newStats = computed(() => extract('new'))
const oldStats = computed(() => extract('old'))

function extract(type) {
  const m = {}
  rawList.value.forEach(r => {
    if (r.userType === type) m[r.metricName] = Number(r.metricValue)
  })
  return m
}

const fmt    = v => v != null ? Number(v).toLocaleString()               : '—'
const fmtDec = v => v != null ? Number(v).toFixed(2)                    : '—'
const fmtPct = v => v != null ? (Number(v) * 100).toFixed(2) + '%'      : '—'

const metricDefs = [
  { key: 'user_count',       label: '用户数',   fmt: fmt    },
  { key: 'avg_pages',        label: '人均页数', fmt: fmtDec },
  { key: 'conversion_rate',  label: '转化率',   fmt: fmtPct }
]

const tableRows = computed(() => {
  const n = newStats.value, o = oldStats.value
  return metricDefs.map(m => {
    const nv = n[m.key], ov = o[m.key]
    const diff = ov && ov !== 0 ? ((nv - ov) / Math.abs(ov) * 100).toFixed(2) : null
    return { label: m.label, newVal: m.fmt(nv), oldVal: m.fmt(ov), diff: diff != null ? Number(diff) : null,
             insight: insights[m.key] }
  })
})

const insights = {
  user_count:      '新用户增长情况反映获客能力',
  avg_pages:       '老用户通常有更高的浏览深度和粘性',
  conversion_rate: '老用户熟悉平台，通常有更高的转化率'
}

const loadData = async () => {
  loading.value = true
  try {
    rawList.value = await getUserComparisonList() || []
    renderChart()
  } catch {
    ElMessage.error('加载对比数据失败')
  } finally {
    loading.value = false
  }
}

const renderChart = () => {
  if (!comparisonChartRef.value) return
  if (!compChart) compChart = echarts.init(comparisonChartRef.value)

  const n = newStats.value, o = oldStats.value
  const labels = metricDefs.map(m => m.label)

  compChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend:  { data: ['新用户', '老用户'], top: 10 },
    grid:    { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis:   { type: 'category', data: labels },
    yAxis:   { type: 'value' },
    series: [
      { name: '新用户', type: 'bar',
        data: metricDefs.map(m => n[m.key] ?? 0),
        itemStyle: { color: '#67C23A' },
        label: { show: true, position: 'top' } },
      { name: '老用户', type: 'bar',
        data: metricDefs.map(m => o[m.key] ?? 0),
        itemStyle: { color: '#409EFF' },
        label: { show: true, position: 'top' } }
    ]
  })
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', () => compChart?.resize())
})
</script>

<style scoped>
.user-comparison-container { padding: 20px; }
.page-title { font-size: 18px; font-weight: bold; }

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: bold;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  padding: 10px 0;
}

.metric-item { text-align: center; }
.metric-value { font-size: 22px; font-weight: bold; }
.metric-label { font-size: 13px; color: #909399; margin-top: 5px; }
</style>
