<template>
  <div class="cache-manage-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>缓存管理</span>
          <el-button type="danger" @click="handleClearAllCache" :loading="clearing">
            清空所有缓存
          </el-button>
        </div>
      </template>

      <!-- 缓存统计 -->
      <el-row :gutter="20" style="margin-bottom: 20px">
        <el-col :span="8">
          <el-card class="cache-stat-card">
            <div class="cache-stat-content">
              <div class="cache-stat-value">{{ cacheStats.totalKeys ?? '—' }}</div>
              <div class="cache-stat-label">缓存总键数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="cache-stat-card">
            <div class="cache-stat-content">
              <div class="cache-stat-value">{{ cacheStats.hitRate ?? '—' }}</div>
              <div class="cache-stat-label">缓存命中率</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="cache-stat-card">
            <div class="cache-stat-content">
              <div class="cache-stat-value">{{ cacheStats.usedMemory ?? '—' }}</div>
              <div class="cache-stat-label">已用内存</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 缓存列表 -->
      <el-card>
        <template #header>
          <div class="sub-header">
            <span>缓存列表</span>
            <el-button size="small" @click="loadCacheList">刷新</el-button>
          </div>
        </template>

        <el-table :data="cacheList" v-loading="loading" border stripe>
          <el-table-column prop="key"            label="缓存键"   min-width="220" />
          <el-table-column prop="type"           label="类型"     width="120" />
          <el-table-column prop="size"           label="大小"     width="100" />
          <el-table-column prop="hitCount"       label="命中次数" width="110" />
          <el-table-column prop="lastAccessTime" label="最近访问" width="180" />
          <el-table-column prop="ttl"            label="TTL"      width="100" />
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleRefreshCache(row.key)">刷新</el-button>
              <el-button link type="danger"  @click="handleDeleteCache(row.key)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 缓存配置 -->
      <el-card style="margin-top: 20px">
        <template #header><span>缓存配置</span></template>
        <el-descriptions :column="2" border v-if="Object.keys(cacheConfig).length">
          <el-descriptions-item label="缓存类型">{{ cacheConfig.type }}</el-descriptions-item>
          <el-descriptions-item label="主机">{{ cacheConfig.host }}:{{ cacheConfig.port }}</el-descriptions-item>
          <el-descriptions-item label="默认 TTL">{{ cacheConfig.defaultTtl }} 秒</el-descriptions-item>
          <el-descriptions-item label="最大内存">{{ cacheConfig.maxMemory }}</el-descriptions-item>
          <el-descriptions-item label="启用状态">
            <el-tag :type="cacheConfig.enabled ? 'success' : 'info'">
              {{ cacheConfig.enabled ? '已启用' : '已禁用' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="暂未加载配置" />
      </el-card>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCacheList, getCacheStats, getCacheConfig, refreshCache, deleteCache, clearAllCache } from '@/api/cache'

const loading  = ref(false)
const clearing = ref(false)
const cacheList   = ref([])
const cacheStats  = ref({})
const cacheConfig = ref({})

const loadCacheList = async () => {
  loading.value = true
  try {
    cacheList.value = await getCacheList() || []
  } catch {
    ElMessage.error('加载缓存列表失败')
  } finally {
    loading.value = false
  }
}

const loadCacheStats = async () => {
  try {
    cacheStats.value = await getCacheStats() || {}
  } catch { /* 静默 */ }
}

const loadCacheConfig = async () => {
  try {
    cacheConfig.value = await getCacheConfig() || {}
  } catch { /* 静默 */ }
}

const handleRefreshCache = async (key) => {
  try {
    await refreshCache(key)
    ElMessage.success('缓存刷新成功')
    loadCacheList()
    loadCacheStats()
  } catch {
    ElMessage.error('缓存刷新失败')
  }
}

const handleDeleteCache = async (key) => {
  try {
    await ElMessageBox.confirm('确定要删除该缓存吗？', '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await deleteCache(key)
    ElMessage.success('删除成功')
    loadCacheList()
    loadCacheStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleClearAllCache = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有缓存吗？此操作不可恢复！', '警告',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'error' })
    clearing.value = true
    await clearAllCache()
    ElMessage.success('所有缓存已清空')
    loadCacheList()
    loadCacheStats()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('清空缓存失败')
  } finally {
    clearing.value = false
  }
}

onMounted(() => {
  loadCacheList()
  loadCacheStats()
  loadCacheConfig()
})
</script>

<style scoped>
.cache-manage-container { padding: 20px; }

.card-header, .sub-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cache-stat-card { text-align: center; }
.cache-stat-content { padding: 20px 0; }
.cache-stat-value { font-size: 30px; font-weight: bold; color: #409EFF; }
.cache-stat-label { font-size: 14px; color: #909399; margin-top: 10px; }
</style>
