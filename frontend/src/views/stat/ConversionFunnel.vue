<template>
  <div class="conversion-funnel-container">
    <el-page-header @back="$router.back()" title="返回">
      <template #content>
        <span class="page-title">转化漏斗分析</span>
      </template>
    </el-page-header>

    <!-- 过滤器 -->
    <el-card style="margin-top: 20px">
      <el-form :inline="true">
        <el-form-item label="用户群体">
          <el-select v-model="filterType" @change="loadFunnelData" style="width: 150px">
            <el-option label="全部用户" value="ALL" />
            <el-option label="新用户"   value="new_user" />
            <el-option label="老用户"   value="old_user" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 漏斗图 -->
    <el-card style="margin-top: 20px" v-loading="loading">
      <template #header><span>用户转化漏斗</span></template>
      <div v-if="funnelData.length" ref="funnelChartRef" style="height: 480px"></div>
      <el-empty v-else description="暂无数据（仅「全部用户」有漏斗统计结果）" style="padding: 60px 0" />
    </el-card>

    <!-- 转化详情表格 -->
    <el-card style="margin-top: 20px">
      <template #header><span>转化详情</span></template>
      <el-table :data="funnelData" border stripe>
        <el-table-column prop="stepOrder"             label="步骤序号" width="100" />
        <el-table-column prop="stepName"              label="步骤名称" width="200" />
        <el-table-column prop="userCount"             label="用户数"   width="120" />
        <el-table-column prop="conversionRate"        label="本步转化率" width="130">
          <template #default="{ row }">
            {{ row.conversionRate != null ? (Number(row.conversionRate) * 100).toFixed(2) + '%' : '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="lossRate"              label="本步流失率" width="130">
          <template #default="{ row }">
            {{ row.lossRate != null ? (Number(row.lossRate) * 100).toFixed(2) + '%' : '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="overallConversionRate" label="整体转化率" width="130">
          <template #default="{ row }">
            {{ row.overallConversionRate != null ? (Number(row.overallConversionRate) * 100).toFixed(2) + '%' : '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="statDate"              label="统计日期"  width="130" />
      </el-table>
    </el-card>

    <!-- 优化建议 -->
    <el-card v-if="suggestions.length" style="margin-top: 20px">
      <template #header><span>优化建议</span></template>
      <el-alert
        v-for="(s, i) in suggestions"
        :key="i"
        :title="s.title"
        :type="s.type"
        :description="s.description"
        show-icon
        style="margin-bottom: 10px"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getConversionFunnel } from '@/api/stat'

const filterType     = ref('ALL')
const funnelData     = ref([])
const loading        = ref(false)
const funnelChartRef = ref(null)
let funnelChart = null

const suggestions = computed(() => {
  if (!funnelData.value.length) return []
  const result = []

  // 找流失率最高的步骤
  const worstStep = funnelData.value.reduce(
    (acc, s) => (Number(s.lossRate) > Number(acc.lossRate ?? 0) ? s : acc),
    funnelData.value[0]
  )
  if (Number(worstStep.lossRate) > 0.5) {
    result.push({
      title: '重点关注流失严重的环节',
      type:  'warning',
      description: `「${worstStep.stepName}」的流失率高达 ${(Number(worstStep.lossRate) * 100).toFixed(1)}%，建议优化该环节的用户体验`
    })
  }

  // 整体转化率
  const last = funnelData.value[funnelData.value.length - 1]
  if (last?.overallConversionRate != null) {
    const rate = Number(last.overallConversionRate) * 100
    if (rate < 10) {
      result.push({ title: '整体转化率偏低', type: 'error',
        description: `整体转化率仅为 ${rate.toFixed(2)}%，建议全面优化转化流程` })
    } else if (rate < 30) {
      result.push({ title: '整体转化率有提升空间', type: 'info',
        description: `整体转化率为 ${rate.toFixed(2)}%，仍有较大提升空间` })
    }
  }
  return result
})

const loadFunnelData = async () => {
  loading.value = true
  try {
    funnelData.value = await getConversionFunnel(filterType.value) || []
    await nextTick()   // 等 v-if 把 chart div 挂载到 DOM 再初始化
    renderFunnelChart()
  } catch {
    ElMessage.error('加载漏斗数据失败')
  } finally {
    loading.value = false
  }
}

const renderFunnelChart = () => {
  if (!funnelChartRef.value) return
  if (!funnelChart) funnelChart = echarts.init(funnelChartRef.value)

  const maxCount = funnelData.value[0]?.userCount || 100

  funnelChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        const step = funnelData.value[params.dataIndex] || {}
        const conv = step.conversionRate != null ? (Number(step.conversionRate) * 100).toFixed(2) + '%' : '—'
        const loss = step.lossRate != null ? (Number(step.lossRate) * 100).toFixed(2) + '%' : '—'
        return `${params.name}<br/>用户数: ${params.value}<br/>转化率: ${conv}<br/>流失率: ${loss}`
      }
    },
    series: [{
      name:    '转化漏斗',
      type:    'funnel',
      left:    '10%',
      width:   '80%',
      min:     0,
      max:     maxCount,
      minSize: '0%',
      maxSize: '100%',
      sort:    'descending',
      gap:     4,
      label:   { show: true, position: 'inside', formatter: '{b}\n{c}' },
      itemStyle: { borderColor: '#fff', borderWidth: 1 },
      emphasis:  { label: { fontSize: 14 } },
      data: funnelData.value.map(s => ({ name: s.stepName, value: s.userCount }))
    }]
  })
}

onMounted(() => {
  loadFunnelData()
  window.addEventListener('resize', () => funnelChart?.resize())
})
</script>

<style scoped>
.conversion-funnel-container { padding: 20px; }
.page-title { font-size: 18px; font-weight: bold; }
</style>
