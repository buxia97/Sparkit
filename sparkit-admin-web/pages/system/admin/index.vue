<template>
  <div>
    <div class="page-header">
      <div class="page-title">管理员管理</div>
      <div class="page-desc">管理系统后台管理员账号，包括增删改查、角色分配、状态管理</div>
    </div>

    <div class="search-bar">
      <div class="search-item">
        <span class="search-label">用户名</span>
        <el-input v-model="searchForm.keyword" placeholder="请输入用户名" clearable style="width: 180px;" />
      </div>
      <div class="search-item">
        <span class="search-label">状态</span>
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px;">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </div>
      <div class="search-item">
        <span class="search-label">部门</span>
        <el-tree-select
          v-model="searchForm.deptId"
          :data="deptTree"
          placeholder="全部"
          clearable
          check-strictly
          :props="{ label: 'deptName', value: 'id' }"
          style="width: 180px;"
        />
      </div>
      <div class="search-actions">
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="handleAdd" v-if="auth.hasPermission('system:admin:add')">
          <el-icon><Plus /></el-icon>新增管理员
        </el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="deptName" label="部门" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="150" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <span :class="row.status === 1 ? 'tag-success' : 'tag-danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link size="small" @click="handleResetPwd(row)">重置密码</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" :disabled="!!formData.id" />
        </el-form-item>
        <el-form-item v-if="!formData.id" label="密码" prop="password">
          <el-input v-model="formData.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="formData.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-tree-select
            v-model="formData.deptId"
            :data="deptTree"
            placeholder="请选择部门"
            check-strictly
            :props="{ label: 'deptName', value: 'id' }"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="formData.roleIds" multiple placeholder="请选择角色" style="width: 100%;">
            <el-option v-for="r in roleList" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">正常</el-radio>
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
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

definePageMeta({
  middleware: 'auth',
  meta: { title: '管理员管理' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/system/admin', title: '管理员管理', icon: 'User' })
  fetchData()
  fetchDeptTree()
  fetchRoleList()
})

// 搜索
const searchForm = reactive({ keyword: '', status: '', deptId: '' })
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const loading = ref(false)

const handleSearch = () => { page.value = 1; fetchData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.status = ''; searchForm.deptId = ''; handleSearch() }

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await useFetch('/api/v1/admin/users', {
      params: { page: page.value, pageSize: pageSize.value, ...searchForm },
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      tableData.value = data.value.data.records
      total.value = data.value.data.total
    }
  } catch (e) { /* fallback */ } finally { loading.value = false }
}

// 部门树
const deptTree = ref([])
const fetchDeptTree = async () => {
  try {
    const { data } = await useFetch('/api/v1/admin/depts/tree', {
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) deptTree.value = data.value.data
  } catch (e) { /* ignore */ }
}

// 角色列表
const roleList = ref([])
const fetchRoleList = async () => {
  try {
    const { data } = await useFetch('/api/v1/admin/roles', {
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) roleList.value = data.value.data || []
  } catch (e) { /* ignore */ }
}

// 新增/编辑
const dialogVisible = ref(false)
const dialogTitle = ref('新增管理员')
const formRef = ref(null)
const submitting = ref(false)
const formData = reactive({
  id: '', username: '', password: '', nickname: '', phone: '', email: '',
  deptId: '', roleIds: [], status: 1
})
const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur', min: 6 }],
  roleIds: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const handleAdd = () => {
  dialogTitle.value = '新增管理员'
  resetForm()
  formData.password = ''
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑管理员'
  resetForm()
  Object.assign(formData, {
    id: row.id, username: row.username, nickname: row.nickname,
    phone: row.phone || '', email: row.email || '',
    deptId: row.deptId || '', roleIds: row.roleIds || [],
    status: row.status
  })
  dialogVisible.value = true
}

const resetForm = () => {
  Object.assign(formData, { id: '', username: '', password: '', nickname: '', phone: '', email: '', deptId: '', roleIds: [], status: 1 })
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const url = formData.id ? `/api/v1/admin/users/${formData.id}` : '/api/v1/admin/users'
      const method = formData.id ? 'PUT' : 'POST'
      const { data } = await useFetch(url, {
        method, body: formData,
        headers: { Authorization: `Bearer ${auth.token}` }
      })
      if (data.value?.code === 200) {
        ElMessage.success(formData.id ? '编辑成功' : '新增成功')
        dialogVisible.value = false
        fetchData()
      }
    } catch (e) { /* ignore */ } finally { submitting.value = false }
  })
}

const handleResetPwd = (row) => {
  if (!row.password) { ElMessage.warning('无法获取当前密码信息'); return }
  ElMessageBox.prompt('请输入新密码', '重置密码', {
    inputType: 'password',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async ({ value }) => {
    if (!value) return
    const { data } = await useFetch(`/api/v1/admin/users/${row.id}`, {
      method: 'PUT', body: { ...row, password: value },
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) ElMessage.success('密码重置成功')
  }).catch(() => {})
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除管理员「${row.username}」？`, '删除确认', {
    type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
  }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/users/${row.id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    }
  }).catch(() => {})
}
</script>