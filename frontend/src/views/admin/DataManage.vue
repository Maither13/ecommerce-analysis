<template>
  <div class="data-manage-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>HDFS 数据管理</span>
          <el-button size="small" @click="loadDir">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>

      <!-- 路径导航 + 新建目录 -->
      <el-row :gutter="12" style="margin-bottom: 16px; align-items: center">
        <el-col :span="14">
          <el-input v-model="currentPath" placeholder="HDFS 路径" @keyup.enter="loadDir">
            <template #prepend>路径</template>
            <template #append>
              <el-button @click="loadDir">Go</el-button>
            </template>
          </el-input>
        </el-col>
        <el-col :span="6">
          <el-input v-model="newDirName" placeholder="新目录名" />
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="mkdir" :disabled="!newDirName">
            <el-icon><FolderAdd /></el-icon> 新建目录
          </el-button>
        </el-col>
      </el-row>

      <!-- 文件列表 -->
      <el-table :data="fileList" v-loading="listLoading" border style="width: 100%; margin-bottom: 20px">
        <el-table-column label="类型" width="60">
          <template #default="{ row }">
            <el-icon v-if="row.type === 'directory'" color="#409EFF"><Folder /></el-icon>
            <el-icon v-else color="#67C23A"><Document /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称">
          <template #default="{ row }">
            <el-link v-if="row.type === 'directory'" @click="navigate(row.path)" type="primary">
              {{ row.name }}
            </el-link>
            <span v-else>{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="size" label="大小" width="100">
          <template #default="{ row }">
            {{ row.type === 'directory' ? '—' : formatSize(row.size) }}
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="所有者" width="100" />
        <el-table-column prop="date" label="修改日期" width="130">
          <template #default="{ row }">{{ row.date }} {{ row.time }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-popconfirm title="确定删除？" @confirm="deleteFile(row.path)">
              <template #reference>
                <el-button size="small" type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!listLoading && fileList.length === 0" description="目录为空或路径不存在" />

      <!-- 上传 CSV 到当前目录 -->
      <el-divider content-position="left">上传文件到当前目录：{{ currentPath }}</el-divider>
      <el-upload
        drag
        :action="uploadUrl"
        :data="{ hdfsDir: currentPath }"
        :headers="uploadHeaders"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :before-upload="beforeUpload"
        accept=".csv,.txt"
        :show-file-list="false"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此，或 <em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 CSV / TXT，文件会上传到 HDFS 当前目录</div>
        </template>
      </el-upload>

      <!-- 数据库统计 -->
      <el-divider content-position="left">MySQL 统计结果记录数</el-divider>
      <el-descriptions :column="2" border v-loading="statsLoading">
        <el-descriptions-item label="基础指标">{{ statistics.basicMetrics ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="多维度分析">{{ statistics.multiDimension ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="漏斗步骤">{{ statistics.funnelStep ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="用户对比">{{ statistics.userComparison ?? '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Folder, Document, FolderAdd, Refresh } from '@element-plus/icons-vue'
import axios from 'axios'
import { getStatistics } from '@/api/stat'

const currentPath = ref('/input/user_behavior')
const newDirName  = ref('')
const fileList    = ref([])
const listLoading = ref(false)
const statsLoading = ref(false)
const statistics  = ref({})

const uploadHeaders = computed(() => ({ Authorization: localStorage.getItem('token') || '' }))
const uploadUrl = '/api/hdfs/upload'

// 列出目录
const loadDir = async () => {
  listLoading.value = true
  try {
    const res = await axios.get('/api/hdfs/list', {
      params: { path: currentPath.value },
      headers: uploadHeaders.value
    })
    fileList.value = res.data?.data || []
  } catch {
    fileList.value = []
  } finally {
    listLoading.value = false
  }
}

// 点击目录跳转
const navigate = (path) => {
  currentPath.value = path
  loadDir()
}

// 新建目录
const mkdir = async () => {
  const path = currentPath.value.replace(/\/+$/, '') + '/' + newDirName.value
  try {
    await axios.post('/api/hdfs/mkdir', null, {
      params: { path },
      headers: uploadHeaders.value
    })
    ElMessage.success('目录创建成功')
    newDirName.value = ''
    loadDir()
  } catch (e) {
    ElMessage.error('创建失败：' + (e.response?.data?.message || e.message))
  }
}

// 删除
const deleteFile = async (path) => {
  try {
    await axios.delete('/api/hdfs/delete', {
      params: { path },
      headers: uploadHeaders.value
    })
    ElMessage.success('删除成功')
    loadDir()
  } catch (e) {
    ElMessage.error('删除失败：' + (e.response?.data?.message || e.message))
  }
}

// 上传回调
const beforeUpload = (file) => {
  if (file.size / 1024 / 1024 > 100) { ElMessage.error('文件超过 100MB'); return false }
  return true
}
const handleUploadSuccess = () => {
  ElMessage.success('上传到 HDFS 成功')
  loadDir()
  loadStatistics()
}
const handleUploadError = (err) => {
  ElMessage.error('上传失败：' + err.message)
}

// 格式化文件大小
const formatSize = (bytes) => {
  const n = Number(bytes)
  if (isNaN(n)) return bytes
  if (n < 1024) return n + ' B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
  return (n / 1024 / 1024).toFixed(1) + ' MB'
}

// 数据库统计
const loadStatistics = async () => {
  statsLoading.value = true
  try {
    statistics.value = await getStatistics() || {}
  } catch { /* 静默 */ } finally {
    statsLoading.value = false
  }
}

onMounted(() => {
  loadDir()
  loadStatistics()
})
</script>

<style scoped>
.data-manage-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-weight: bold; font-size: 16px; }
</style>
