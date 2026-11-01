<template>
  <div class="task-manage-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>任务管理（管理员）</span>
        </div>
      </template>

      <!-- 统计卡片 -->
      <el-row :gutter="20" style="margin-bottom: 20px">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-value">{{ pagination.total }}</div>
              <div class="stat-label">总任务数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card pending">
            <div class="stat-content">
              <div class="stat-value">{{ statusCount.PENDING }}</div>
              <div class="stat-label">待执行</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card running">
            <div class="stat-content">
              <div class="stat-value">{{ statusCount.RUNNING }}</div>
              <div class="stat-label">执行中</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card failed">
            <div class="stat-content">
              <div class="stat-value">{{ statusCount.FAILED }}</div>
              <div class="stat-label">失败</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="任务名称" clearable />
        </el-form-item>
        <el-form-item label="任务状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option label="待执行" value="PENDING" />
            <el-option label="执行中" value="RUNNING" />
            <el-option label="已完成" value="SUCCESS" />
            <el-option label="失败"   value="FAILED"  />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 任务列表 -->
      <el-table :data="taskList" v-loading="loading" border stripe>
        <el-table-column prop="id"         label="ID"      width="80" />
        <el-table-column prop="jobName"    label="任务名称" min-width="150" />
        <el-table-column prop="userId"     label="提交用户ID" width="110" />
        <el-table-column prop="jobType"    label="任务类型" width="160">
          <template #default="{ row }">
            <el-tag :type="getTaskTypeTag(row.jobType)">
              {{ getTaskTypeName(row.jobType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status"     label="状态"    width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress"   label="进度"    width="130">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0" :status="getProgressStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/task/detail/${row.id}`)">详情</el-button>
            <el-button
              link type="warning"
              @click="handleRetry(row.id)"
              :disabled="row.status !== 'FAILED' && row.status !== 'SUCCESS'"
            >
              重试
            </el-button>
            <el-button link type="danger" @click="handleDelete(row.id)"
              :disabled="row.status === 'RUNNING'">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadTaskList"
        @current-change="loadTaskList"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTaskList, retryTask, deleteTask } from '@/api/task'

const loading  = ref(false)
const taskList = ref([])

const searchForm = reactive({ keyword: '', status: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

// 当前页状态分布（仅供参考，全量统计需单独接口）
const statusCount = computed(() => {
  const c = { PENDING: 0, RUNNING: 0, SUCCESS: 0, FAILED: 0 }
  taskList.value.forEach(t => { if (c[t.status] != null) c[t.status]++ })
  return c
})

const getTaskTypeName = (type) => ({
  basic_metrics:    '基础指标统计',
  basic:            '基础指标统计',
  multi_dimension:  '多维度行为分析',
  conversion_funnel:'转化漏斗分析',
  funnel:           '转化漏斗分析',
  user_comparison:  '新老用户对比',
  comparison:       '新老用户对比'
})[type] || type

const getTaskTypeTag = (type) => ({
  basic_metrics:    'primary',
  basic:            'primary',
  multi_dimension:  'success',
  conversion_funnel:'warning',
  funnel:           'warning',
  user_comparison:  'info',
  comparison:       'info'
})[type] || 'info'

const getStatusText = (status) => ({
  PENDING:   '待执行',
  RUNNING:   '执行中',
  SUCCESS:   '已完成',
  FAILED:    '失败',
  CANCELLED: '已取消'
})[status] || status

const getStatusType = (status) => ({
  PENDING: 'info', RUNNING: 'warning', SUCCESS: 'success', FAILED: 'danger'
})[status] || ''

const getProgressStatus = (row) => {
  if (row.status === 'SUCCESS') return 'success'
  if (row.status === 'FAILED')  return 'exception'
  return undefined
}

const loadTaskList = async () => {
  loading.value = true
  try {
    const params = {
      keyword:  searchForm.keyword || undefined,
      status:   searchForm.status  || undefined,
      pageNum:  pagination.pageNum,
      pageSize: pagination.pageSize
    }
    const result = await getTaskList(params)
    taskList.value   = result.records || []
    pagination.total = result.total   || 0
  } catch {
    ElMessage.error('加载任务列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.pageNum = 1; loadTaskList() }
const handleReset  = () => {
  searchForm.keyword = ''; searchForm.status = ''
  pagination.pageNum = 1; loadTaskList()
}

const handleRetry = async (id) => {
  try {
    await ElMessageBox.confirm('确定要重新执行该任务吗？', '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await retryTask(id)
    ElMessage.success('任务已重新提交')
    loadTaskList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('重新执行失败')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该任务吗？', '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await deleteTask(id)
    ElMessage.success('删除成功')
    loadTaskList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(loadTaskList)
</script>

<style scoped>
.task-manage-container { padding: 20px; }
.card-header { font-weight: bold; font-size: 16px; }
.search-form { margin-bottom: 20px; }

.stat-card { text-align: center; }
.stat-content { padding: 10px 0; }
.stat-value { font-size: 32px; font-weight: bold; color: #303133; }
.stat-label { font-size: 14px; color: #909399; margin-top: 5px; }
.pending .stat-value { color: #E6A23C; }
.running .stat-value { color: #409EFF; }
.failed  .stat-value { color: #F56C6C; }
</style>
