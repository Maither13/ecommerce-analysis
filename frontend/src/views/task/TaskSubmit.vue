<template>
  <div class="task-submit-container">
    <el-card class="submit-card">
      <template #header>
        <div class="card-header">
          <span>提交MapReduce任务</span>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="任务名称" prop="jobName">
          <el-input v-model="form.jobName" placeholder="请输入任务名称" />
        </el-form-item>

        <el-form-item label="任务类型" prop="jobType">
          <el-select v-model="form.jobType" placeholder="请选择任务类型" style="width: 100%">
            <el-option label="基础指标统计" value="basic" />
            <el-option label="多维度行为分析" value="multi_dimension" />
            <el-option label="转化漏斗分析" value="funnel" />
            <el-option label="新老用户对比" value="comparison" />
          </el-select>
        </el-form-item>

        <el-form-item label="输入路径" prop="inputPath">
          <el-input v-model="form.inputPath" placeholder="HDFS输入路径，如：/data/input/user_behavior.csv" />
        </el-form-item>

        <el-form-item label="输出路径" prop="outputPath">
          <el-input v-model="form.outputPath" placeholder="HDFS输出路径，如：/data/output/basic_metrics_20240610" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            提交任务
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="tips-card" style="margin-top: 20px">
      <template #header>
        <span>提交说明</span>
      </template>
      <ul>
        <li>任务提交后将通过 SSH 自动在 Hadoop 虚拟机上执行真实的 MapReduce 作业</li>
        <li>可在"任务列表"页面实时查看进度和状态</li>
        <li>任务完成后，统计结果将自动解析并存入 MySQL 数据库</li>
        <li>输入路径默认使用 <code>/input/user_behavior/data.csv</code>（已上传到 HDFS）</li>
        <li>输出路径留空则系统自动生成，也可手动指定</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitTask } from '@/api/task'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  jobName: '',
  jobType: '',
  inputPath: '',
  outputPath: ''
})

const rules = {
  jobName: [
    { required: true, message: '请输入任务名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  jobType: [
    { required: true, message: '请选择任务类型', trigger: 'change' }
  ],
  inputPath: [
    { required: true, message: '请输入输入路径', trigger: 'blur' }
  ],
  outputPath: [
    { required: true, message: '请输入输出路径', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await submitTask(form)
        ElMessage.success('任务提交成功，正在后台执行')
        router.push('/task/list')
      } catch {
        // 拦截器已弹出错误提示
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleReset = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
}
</script>

<style scoped>
.task-submit-container {
  padding: 20px;
}

.submit-card {
  max-width: 800px;
}

.card-header {
  font-weight: bold;
  font-size: 16px;
}

.tips-card ul {
  padding-left: 20px;
  line-height: 1.8;
}

.tips-card li {
  color: #606266;
}
</style>
