<template>
  <div class="dashboard-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据看板</span>
          <el-button type="primary" size="small" @click="refreshData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>
      
      <!-- 核心指标卡片 -->
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-content">
              <div class="metric-icon" style="background-color: #409EFF;">
                <el-icon :size="30"><View /></el-icon>
              </div>
              <div class="metric-info">
                <div class="metric-value">{{ metrics.pv || 0 }}</div>
                <div class="metric-label">PV (页面浏览量)</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-content">
              <div class="metric-icon" style="background-color: #67C23A;">
                <el-icon :size="30"><User /></el-icon>
              </div>
              <div class="metric-info">
                <div class="metric-value">{{ metrics.uv || 0 }}</div>
                <div class="metric-label">UV (独立用户数)</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-content">
              <div class="metric-icon" style="background-color: #E6A23C;">
                <el-icon :size="30"><TrendCharts /></el-icon>
              </div>
              <div class="metric-info">
                <div class="metric-value">{{ (metrics.conversionRate * 100).toFixed(2) }}%</div>
                <div class="metric-label">转化率</div>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :span="6">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-content">
              <div class="metric-icon" style="background-color: #F56C6C;">
                <el-icon :size="30"><UserFilled /></el-icon>
              </div>
              <div class="metric-info">
                <div class="metric-value">{{ (metrics.newUserRatio * 100).toFixed(2) }}%</div>
                <div class="metric-label">新用户占比</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <!-- PV/UV 趋势图 -->
      <el-card style="margin-top: 20px;">
        <template #header>
          <span>PV/UV 趋势分析</span>
        </template>
        <div ref="trendChartRef" style="height: 400px;"></div>
      </el-card>
      
      <!-- 多维度分析图表 -->
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>流量来源分布</span>
            </template>
            <div ref="sourceChartRef" style="height: 300px;"></div>
          </el-card>
        </el-col>
        
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>设备类型分布</span>
            </template>
            <div ref="deviceChartRef" style="height: 300px;"></div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getBasicMetricsLatest, getBasicMetricsHistory, getMultiDimension } from '@/api/stat'

const metrics = ref({
  pv: 0,
  uv: 0,
  conversionRate: 0,
  newUserRatio: 0
})

const trendChartRef = ref(null)
const sourceChartRef = ref(null)
const deviceChartRef = ref(null)

let trendChart = null
let sourceChart = null
let deviceChart = null

// 加载基础指标
const loadMetrics = async () => {
  try {
    const data = await getBasicMetricsLatest()
    metrics.value = data
  } catch (error) {
    console.error(error)
  }
}

// 加载趋势数据（用 history 接口，取最近有数据的 8 天，不受今天日期限制）
const loadTrendData = async () => {
  try {
    const list = await getBasicMetricsHistory(8) || []
    const data = {
      dates:  list.map(item => item.statDate),
      pvData: list.map(item => item.pv),
      uvData: list.map(item => item.uv)
    }
    renderTrendChart(data)
  } catch (error) {
    console.error(error)
  }
}

// 加载多维度数据
const loadMultiDimensionData = async () => {
  try {
    const [sourceData, deviceData] = await Promise.all([
      getMultiDimension('source'),
      getMultiDimension('device')
    ])
    await nextTick()
    if (Array.isArray(sourceData) && sourceData.length) renderSourceChart(sourceData)
    if (Array.isArray(deviceData) && deviceData.length) renderDeviceChart(deviceData)
  } catch (error) {
    console.error(error)
  }
}

// 渲染趋势图
const renderTrendChart = (data) => {
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['PV', 'UV']
    },
    xAxis: {
      type: 'category',
      data: data.dates || []
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: 'PV',
        type: 'line',
        data: data.pvData || [],
        smooth: true,
        areaStyle: {}
      },
      {
        name: 'UV',
        type: 'line',
        data: data.uvData || [],
        smooth: true,
        areaStyle: {}
      }
    ]
  }
  
  trendChart.setOption(option)
}

// 渲染流量来源图
const renderSourceChart = (data) => {
  if (!sourceChart) {
    sourceChart = echarts.init(sourceChartRef.value)
  }
  
  const option = {
    tooltip: {
      trigger: 'item'
    },
    series: [
      {
        type: 'pie',
        radius: '50%',
        data: data.map(item => ({
          value: item.userCount,
          name: item.dimensionValue
        }))
      }
    ]
  }
  
  sourceChart.setOption(option)
}

// 渲染设备类型图
const renderDeviceChart = (data) => {
  if (!deviceChart) {
    deviceChart = echarts.init(deviceChartRef.value)
  }
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    xAxis: {
      type: 'category',
      data: data.map(item => item.dimensionValue)
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        type: 'bar',
        data: data.map(item => item.userCount),
        itemStyle: {
          color: '#409EFF'
        }
      }
    ]
  }
  
  deviceChart.setOption(option)
}

// 刷新数据
const refreshData = () => {
  loadMetrics()
  loadTrendData()
  loadMultiDimensionData()
}

onMounted(() => {
  refreshData()
  
  // 窗口大小改变时重新渲染图表
  window.addEventListener('resize', () => {
    trendChart?.resize()
    sourceChart?.resize()
    deviceChart?.resize()
  })
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.metric-card {
  height: 120px;
}

.metric-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.metric-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-right: 15px;
}

.metric-info {
  flex: 1;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.metric-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}
</style>
