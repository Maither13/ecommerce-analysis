<template>
  <div class="task-detail-container">
    <el-page-header @back="$router.back()" title="返回">
      <template #content>
        <span class="page-title">任务详情</span>
      </template>
    </el-page-header>

    <el-row :gutter="20" style="margin-top: 20px" v-loading="loading">
      <!-- 左侧：任务基本信息 -->
      <el-col :span="16">
        <el-card>
          <template #header><span>基本信息</span></template>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="任务ID">{{ taskDetail.id }}</el-descriptions-item>
            <el-descriptions-item label="任务名称">{{ taskDetail.jobName }}</el-descriptions-item>
            <el-descriptions-item label="任务类型">
              <el-tag :type="getTaskTypeTag(taskDetail.jobType)">
                {{ getTaskTypeName(taskDetail.jobType) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusType(taskDetail.status)">
                {{ getStatusText(taskDetail.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ taskDetail.submitTime }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ taskDetail.createTime }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ taskDetail.startTime || '—' }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ taskDetail.endTime || '—' }}</el-descriptions-item>
            <el-descriptions-item label="耗时">
              {{ taskDetail.duration != null ? taskDetail.duration + ' 秒' : '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="任务 Job ID">{{ taskDetail.jobId }}</el-descriptions-item>
            <el-descriptions-item label="输入路径" :span="2">{{ taskDetail.inputPath }}</el-descriptions-item>
            <el-descriptions-item label="输出路径" :span="2">{{ taskDetail.outputPath }}</el-descriptions-item>
            <el-descriptions-item v-if="taskDetail.errorMsg" label="错误信息" :span="2">
              <span style="color: #F56C6C">{{ taskDetail.errorMsg }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 右侧：执行进度和操作 -->
      <el-col :span="8">
        <el-card>
          <template #header><span>执行进度</span></template>
          <div class="progress-section">
            <el-progress
              type="dashboard"
              :percentage="taskDetail.progress || 0"
              :status="getProgressStatus(taskDetail)"
            />
            <div class="progress-text">
              <p>状态：{{ getStatusText(taskDetail.status) }}</p>
              <p v-if="taskDetail.status === 'RUNNING'">正在执行中，请稍候...</p>
              <p v-else-if="taskDetail.status === 'SUCCESS'">执行成功 ✓</p>
              <p v-else-if="taskDetail.status === 'FAILED'">执行失败 ✗</p>
              <p v-else>等待执行</p>
            </div>
            <!-- 执行中时每 3 秒刷新一次 -->
            <el-button
              v-if="taskDetail.status === 'RUNNING' || taskDetail.status === 'PENDING'"
              size="small"
              @click="loadTaskDetail"
              style="margin-top: 12px"
            >
              刷新进度
            </el-button>
          </div>
        </el-card>

        <!-- 完成后跳转分析 -->
        <el-card v-if="taskDetail.status === 'SUCCESS'" style="margin-top: 20px">
          <template #header><span>统计结果</span></template>
          <el-alert
            title="任务已完成"
            type="success"
            description="统计结果已存入数据库，可在统计分析页面查看"
            show-icon
          />
          <el-button
            type="primary"
            style="margin-top: 15px; width: 100%"
            @click="goToAnalysis"
          >
            查看分析结果
          </el-button>
        </el-card>

        <!-- 操作 -->
        <el-card style="margin-top: 20px">
          <template #header><span>操作</span></template>
          <div style="display: flex; flex-direction: column; gap: 10px">
            <el-button
              type="warning"
              style="width: 100%; margin-left: 0"
              @click="handleRetry"
              :disabled="taskDetail.status !== 'FAILED' && taskDetail.status !== 'SUCCESS'"
            >
              重新执行
            </el-button>
            <el-button
              type="danger"
              style="width: 100%; margin-left: 0"
              @click="handleDelete"
              :disabled="taskDetail.status === 'RUNNING'"
            >
              删除任务
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTaskDetail, retryTask, deleteTask } from '@/api/task'

const route  = useRoute()
const router = useRouter()
const loading    = ref(false)
const taskDetail = ref({})

const getTaskTypeName = (type) => ({
  basic:            '基础指标统计',
  basic_metrics:    '基础指标统计',
  multi_dimension:  '多维度行为分析',
  funnel:           '转化漏斗分析',
  conversion_funnel:'转化漏斗分析',
  comparison:       '新老用户对比',
  user_comparison:  '新老用户对比'
})[type] || type

const getTaskTypeTag = (type) => ({
  basic:            'primary',
  basic_metrics:    'primary',
  multi_dimension:  'success',
  funnel:           'warning',
  conversion_funnel:'warning',
  comparison:       'info',
  user_comparison:  'info'
})[type] || ''

const getStatusText = (status) => ({
  PENDING:   '待执行',
  RUNNING:   '执行中',
  SUCCESS:   '已完成',
  FAILED:    '失败',
  CANCELLED: '已取消'
})[status] || status

const getStatusType = (status) => ({
  PENDING:   'info',
  RUNNING:   'warning',
  SUCCESS:   'success',
  FAILED:    'danger',
  CANCELLED: 'info'
})[status] || ''

const getProgressStatus = (task) => {
  if (task.status === 'SUCCESS') return 'success'
  if (task.status === 'FAILED')  return 'exception'
  return undefined
}

const loadTaskDetail = async () => {
  loading.value = true
  try {
    taskDetail.value = await getTaskDetail(route.params.id)
  } catch {
    ElMessage.error('加载任务详情失败')
  } finally {
    loading.value = false
  }
}

const handleRetry = async () => {
  try {
    await ElMessageBox.confirm('确定要重新执行该任务吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText:  '取消',
      type: 'warning'
    })
    await retryTask(route.params.id)
    ElMessage.success('任务已重新提交，正在后台执行')
    loadTaskDetail()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('重新执行失败')
  }
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除该任务吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText:  '取消',
      type: 'warning'
    })
    await deleteTask(route.params.id)
    ElMessage.success('删除成功')
    router.back()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const goToAnalysis = () => {
  const map = {
    basic:            '/stat/basic-metrics',
    basic_metrics:    '/stat/basic-metrics',
    multi_dimension:  '/stat/multi-dimension',
    funnel:           '/stat/conversion-funnel',
    conversion_funnel:'/stat/conversion-funnel',
    comparison:       '/stat/user-comparison',
    user_comparison:  '/stat/user-comparison'
  }
  const target = map[taskDetail.value.jobType]
  if (target) router.push(target)
  else router.push('/stat/basic-metrics')
}

onMounted(loadTaskDetail)
</script>

<style scoped>
.task-detail-container {
  padding: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: bold;
}

.progress-section {
  text-align: center;
  padding: 20px 0;
}

.progress-text {
  margin-top: 20px;
  color: #606266;
}

.progress-text p {
  margin: 5px 0;
}
</style>
