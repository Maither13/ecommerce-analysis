<template>
  <div class="task-list-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的任务列表</span>
          <el-button type="primary" @click="$router.push('/task/submit')">
            提交新任务
          </el-button>
        </div>
      </template>

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
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 任务列表 -->
      <el-table :data="taskList" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="jobName" label="任务名称" min-width="150" />
        <el-table-column prop="jobType" label="任务类型" width="160">
          <template #default="{ row }">
            <el-tag :type="getTaskTypeTag(row.jobType)">
              {{ getTaskTypeName(row.jobType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="130">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0" :status="getProgressStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row.id)">
              详情
            </el-button>
            <el-button
              link
              type="danger"
              @click="handleDelete(row.id)"
              :disabled="row.status === 'RUNNING'"
            >
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTaskList, deleteTask } from '@/api/task'

const router = useRouter()
const loading = ref(false)
const taskList = ref([])

const searchForm = reactive({
  keyword: '',
  status: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const getTaskTypeName = (type) => {
  const map = {
    basic:            '基础指标统计',
    basic_metrics:    '基础指标统计',
    multi_dimension:  '多维度行为分析',
    funnel:           '转化漏斗分析',
    conversion_funnel:'转化漏斗分析',
    comparison:       '新老用户对比',
    user_comparison:  '新老用户对比'
  }
  return map[type] || type
}

const getTaskTypeTag = (type) => {
  const map = {
    basic:            'primary',
    basic_metrics:    'primary',
    multi_dimension:  'success',
    funnel:           'warning',
    conversion_funnel:'warning',
    comparison:       'info',
    user_comparison:  'info'
  }
  return map[type] || 'info'
}

const getStatusText = (status) => {
  const map = {
    PENDING: '待执行',
    RUNNING: '执行中',
    SUCCESS: '已完成',
    FAILED: '失败',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = {
    PENDING: 'info',
    RUNNING: 'warning',
    SUCCESS: 'success',
    FAILED: 'danger',
    CANCELLED: 'info'
  }
  return map[status] || ''
}

const getProgressStatus = (row) => {
  if (row.status === 'SUCCESS') return 'success'
  if (row.status === 'FAILED') return 'exception'
  return undefined
}

const loadTaskList = async () => {
  loading.value = true
  try {
    const params = {
      keyword: searchForm.keyword || undefined,
      status:  searchForm.status  || undefined,
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

const handleSearch = () => {
  pagination.pageNum = 1
  loadTaskList()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status  = ''
  pagination.pageNum = 1
  loadTaskList()
}

const handleViewDetail = (id) => {
  router.push(`/task/detail/${id}`)
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该任务吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText:  '取消',
      type: 'warning'
    })
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
.task-list-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}
</style>
