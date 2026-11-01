<template>
  <div class="user-manage-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleAddUser">
            <el-icon><Plus /></el-icon>
            新增用户
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="用户名 / 昵称 / 邮箱" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 用户列表 -->
      <el-table :data="userList" v-loading="loading" border stripe>
        <el-table-column prop="id"         label="ID"    width="80" />
        <el-table-column prop="username"   label="用户名" width="150" />
        <el-table-column prop="nickname"   label="昵称"   width="120" />
        <el-table-column prop="email"      label="邮箱"   min-width="180" />
        <el-table-column prop="phone"      label="手机号" width="130" />
        <el-table-column prop="status"     label="状态"   width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="180" />
        <el-table-column prop="createTime"    label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
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
        @size-change="loadUserList"
        @current-change="loadUserList"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      @close="handleDialogClose"
    >
      <el-form :model="userForm" :rules="userRules" ref="formRef" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="!!userForm.id" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!userForm.id">
          <el-input v-model="userForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="userForm.nickname" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="userForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getUserList, createUser, updateUser, deleteUser, toggleUserStatus } from '@/api/user'

const loading       = ref(false)
const userList      = ref([])
const dialogVisible = ref(false)
const submitting    = ref(false)
const formRef       = ref(null)

const searchForm = reactive({ keyword: '', status: null })

const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const userForm = reactive({
  id: null, username: '', password: '',
  nickname: '', email: '', phone: '', status: 1
})

const userRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

const dialogTitle = computed(() => userForm.id ? '编辑用户' : '新增用户')

const loadUserList = async () => {
  loading.value = true
  try {
    const params = {
      keyword:  searchForm.keyword  || undefined,
      status:   searchForm.status   != null ? searchForm.status : undefined,
      pageNum:  pagination.pageNum,
      pageSize: pagination.pageSize
    }
    const result = await getUserList(params)
    userList.value     = result.records || []
    pagination.total   = result.total   || 0
  } catch {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.pageNum = 1; loadUserList() }
const handleReset  = () => {
  searchForm.keyword = ''; searchForm.status = null
  pagination.pageNum = 1; loadUserList()
}

const handleAddUser = () => {
  Object.assign(userForm, { id: null, username: '', password: '', nickname: '', email: '', phone: '', status: 1 })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(userForm, {
    id: row.id, username: row.username, password: '',
    nickname: row.nickname, email: row.email, phone: row.phone, status: row.status
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (userForm.id) {
        await updateUser(userForm.id, userForm)
      } else {
        await createUser(userForm)
      }
      ElMessage.success(userForm.id ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadUserList()
    } catch {
      // 拦截器已弹出错误
    } finally {
      submitting.value = false
    }
  })
}

const handleToggleStatus = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要${row.status === 1 ? '禁用' : '启用'}该用户吗？`, '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await toggleUserStatus(row.id)
    ElMessage.success('状态更新成功')
    loadUserList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('状态更新失败')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？', '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await deleteUser(id)
    ElMessage.success('删除成功')
    loadUserList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleDialogClose = () => formRef.value?.resetFields()

onMounted(loadUserList)
</script>

<style scoped>
.user-manage-container { padding: 20px; }
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-form { margin-bottom: 20px; }
</style>
