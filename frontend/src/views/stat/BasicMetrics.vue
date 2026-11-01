<template>
  <div class="basic-metrics-container">
    <el-page-header @back="$router.back()" title="返回">
      <template #content>
        <span class="page-title">基础指标统计</span>
      </template>
    </el-page-header>

    <!-- 核心指标卡片 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon" style="color:#409EFF">
              <el-icon :size="40"><View /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ metrics.pv ?? '—' }}</div>
              <div class="metric-label">页面浏览量 (PV)</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon" style="color:#67C23A">
              <el-icon :size="40"><User /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ metrics.uv ?? '—' }}</div>
              <div class="metric-label">独立访客数 (UV)</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon" style="color:#E6A23C">
              <el-icon :size="40"><UserFilled /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-value">{{ metrics.newUserCount ?? '—' }}</div>
              <div class="metric-label">新用户数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-content">
            <div class="metric-icon" style="color:#F56C6C">
              <el-icon :size="40"><TrendCharts /></el-icon>
            </div>
            <div class="metric-info">
              <div class="metric-value">
                {{ metrics.conversionRate != null ? (Number(metrics.conversionRate) * 100).toFixed(2) + '%' : '—' }}
              </div>
              <div class="metric-label">整体转化率</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- PV/UV趋势图 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <span>近 30 天 PV/UV 趋势</span>
      </template>
      <div ref="trendChartRef" style="height: 400px"></div>
    </el-card>

    <!-- 历史数据表格 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <span>历史数据（近 30 天）</span>
      </template>
      <el-table :data="historyData" border stripe>
        <el-table-column prop="statDate"     label="数据日期"   width="150" />
        <el-table-column prop="pv"           label="PV"        width="120" />
        <el-table-column prop="uv"           label="UV"        width="120" />
        <el-table-column prop="newUserCount" label="新用户数"   width="120" />
        <el-table-column prop="oldUserCount" label="老用户数"   width="120" />
        <el-table-column prop="avgPages"     label="人均页数"   width="120" />
        <el-table-column prop="newUserRatio" label="新用户占比" width="130">
          <template #default="{ row }">
            {{ row.newUserRatio != null ? (Number(row.newUserRatio) * 100).toFixed(1) + '%' : '—' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { View, User, UserFilled, TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getBasicMetricsLatest, getBasicMetricsHistory } from '@/api/stat'

const metrics     = ref({})
const historyData = ref([])
const trendChartRef = ref(null)
let trendChart = null

const loadLatestMetrics = async () => {
  try {
    metrics.value = await getBasicMetricsLatest() || {}
  } catch {
    ElMessage.error('加载最新指标失败')
  }
}

const loadHistoryData = async () => {
  try {
    historyData.value = await getBasicMetricsHistory(30) || []
    renderTrendChart()
  } catch {
    ElMessage.error('加载历史数据失败')
  }
}

const renderTrendChart = () => {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)

  const list  = [...historyData.value].sort((a, b) => String(a.statDate).localeCompare(String(b.statDate)))
  const dates = list.map(d => d.statDate)
  const pvs   = list.map(d => d.pv)
  const uvs   = list.map(d => d.uv)

  trendChart.setOption({
    tooltip:  { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend:   { data: ['PV', 'UV'] },
    grid:     { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis:    { type: 'category', data: dates, boundaryGap: false },
    yAxis:    { type: 'value' },
    series: [
      {
        name: 'PV', type: 'line', data: pvs, smooth: true,
        itemStyle: { color: '#409EFF' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64,158,255,0.3)' },
          { offset: 1, color: 'rgba(64,158,255,0.05)' }
        ])}
      },
      {
        name: 'UV', type: 'line', data: uvs, smooth: true,
        itemStyle: { color: '#67C23A' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(103,194,58,0.3)' },
          { offset: 1, color: 'rgba(103,194,58,0.05)' }
        ])}
      }
    ]
  })
}

onMounted(async () => {
  await Promise.all([loadLatestMetrics(), loadHistoryData()])
  window.addEventListener('resize', () => trendChart?.resize())
})
</script>

<style scoped>
.basic-metrics-container { padding: 20px; }
.page-title { font-size: 18px; font-weight: bold; }

.metric-card { text-align: center; }
.metric-content {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 10px 0;
}
.metric-info { text-align: left; }
.metric-value { font-size: 28px; font-weight: bold; color: #303133; }
.metric-label { font-size: 14px; color: #909399; margin-top: 5px; }
</style>
