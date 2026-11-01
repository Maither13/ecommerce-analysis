<template>
  <div class="multi-dimension-container">
    <el-page-header @back="$router.back()" title="返回">
      <template #content>
        <span class="page-title">多维度行为分析</span>
      </template>
    </el-page-header>

    <!-- 维度选择 -->
    <el-card style="margin-top: 20px">
      <el-form :inline="true">
        <el-form-item label="分析维度">
          <el-select v-model="selectedDimension" @change="loadDimensionData" style="width: 200px">
            <el-option label="年龄分布" value="age" />
            <el-option label="性别分布" value="sex" />
            <el-option label="市场渠道" value="market" />
            <el-option label="设备类型" value="device" />
            <el-option label="操作系统" value="os" />
            <el-option label="流量来源" value="source" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 维度分布图表 -->
    <el-card style="margin-top: 20px" v-loading="loading">
      <template #header>
        <span>{{ dimensionTitle }}</span>
      </template>
      <div ref="dimensionChartRef" style="height: 400px"></div>
    </el-card>

    <!-- 详细数据表格 -->
    <el-card style="margin-top: 20px">
      <template #header><span>详细数据</span></template>
      <el-table :data="dimensionData" border stripe>
        <el-table-column prop="dimensionValue"  :label="getDimensionLabel()" width="150" />
        <el-table-column prop="userCount"       label="用户数"    width="120" />
        <el-table-column prop="pv"              label="PV"        width="120" />
        <el-table-column prop="avgPages"        label="人均页数"  width="120" />
        <el-table-column prop="convertedCount"  label="转化用户数" width="120" />
        <el-table-column prop="conversionRate"  label="转化率"    width="120">
          <template #default="{ row }">
            {{ row.conversionRate != null ? (Number(row.conversionRate) * 100).toFixed(2) + '%' : '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="statDate"        label="统计日期"  width="130" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getMultiDimension } from '@/api/stat'

const selectedDimension = ref('age')
const dimensionData     = ref([])
const loading           = ref(false)
const dimensionChartRef = ref(null)
let dimensionChart = null

const dimensionTitle = computed(() => ({
  age:    '年龄分布分析',
  sex:    '性别分布分析',
  market: '市场渠道分析',
  device: '设备类型分析',
  os:     '操作系统分析',
  source: '流量来源分析'
})[selectedDimension.value] || '维度分析')

const getDimensionLabel = () => ({
  age:    '年龄段',
  sex:    '性别',
  market: '市场渠道',
  device: '设备类型',
  os:     '操作系统',
  source: '流量来源'
})[selectedDimension.value] || '维度值'

const loadDimensionData = async () => {
  loading.value = true
  try {
    dimensionData.value = await getMultiDimension(selectedDimension.value) || []
    renderDimensionChart()
  } catch {
    ElMessage.error('加载维度数据失败')
  } finally {
    loading.value = false
  }
}

const renderDimensionChart = () => {
  if (!dimensionChartRef.value) return
  if (!dimensionChart) dimensionChart = echarts.init(dimensionChartRef.value)
  dimensionChart.clear()  // 切换维度时清空旧配置，避免 pie/bar 混叠

  // 饼图：age / sex / device / os / source；市场渠道用柱图
  if (selectedDimension.value !== 'market') {
    dimensionChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend:  { orient: 'vertical', left: 'left', top: 'middle' },
      series: [{
        name: '用户数',
        type: 'pie',
        radius: '60%',
        data: dimensionData.value.map(d => ({ name: d.dimensionValue, value: d.userCount })),
        emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.5)' } },
        label: { formatter: '{b}\n{d}%' }
      }]
    })
  } else {
    dimensionChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend:  { data: ['用户数', 'PV'] },
      grid:    { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis:   { type: 'category', data: dimensionData.value.map(d => d.dimensionValue) },
      yAxis:   { type: 'value' },
      series: [
        { name: '用户数', type: 'bar', data: dimensionData.value.map(d => d.userCount), itemStyle: { color: '#409EFF' } },
        { name: 'PV',    type: 'bar', data: dimensionData.value.map(d => d.pv),        itemStyle: { color: '#67C23A' } }
      ]
    })
  }
}

onMounted(() => {
  loadDimensionData()
  window.addEventListener('resize', () => dimensionChart?.resize())
})
</script>

<style scoped>
.multi-dimension-container { padding: 20px; }
.page-title { font-size: 18px; font-weight: bold; }
</style>
