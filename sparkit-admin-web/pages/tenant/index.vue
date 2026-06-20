<template>
  <div>
    <div class="page-header">
      <div class="page-title">多租户管理</div>
      <div class="page-desc">管理多租户配置，支持租户数据隔离与权限管控</div>
    </div>

    <el-card>
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增租户
          </el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="tenantName" label="租户名称" min-width="150" />
        <el-table-column prop="tenantCode" label="租户编码" width="120" />
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="tenantType" label="租户模式" width="120">
          <template #default="{ row }">
            <el-tag :type="row.tenantType === 'shared' ? 'primary' : 'warning'" size="small">
              {{ row.tenantType === 'shared' ? '共享表' : '独立表' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <span :class="row.status === 1 ? 'tag-success' : 'tag-danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="到期时间" width="170" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="handleToggleStatus(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑租户弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑租户' : '新增租户'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="租户名称" prop="tenantName">
          <el-input v-model="formData.tenantName" placeholder="请输入租户名称" />
        </el-form-item>
        <el-form-item label="租户编码" prop="tenantCode">
          <el-input v-model="formData.tenantCode" placeholder="全局唯一标识" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="formData.contactName" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="租户模式" prop="tenantType">
          <el-radio-group v-model="formData.tenantType">
            <el-radio value="shared">共享表（同一表，tenant_id隔离）</el-radio>
            <el-radio value="isolated">独立表（每个租户独立表）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="到期时间">
          <el-date-picker v-model="formData.expireTime" type="datetime" placeholder="选择到期时间" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
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
  meta: { title: '多租户管理' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/tenant', title: '多租户管理', icon: 'Connection' })
  fetchData()
})

const tableData = ref([])
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await useFetch('/api/v1/admin/tenant/list', {
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      tableData.value = data.value.data || []
    }
  } catch (e) {
    ElMessage.error('获取租户列表失败')
  } finally { loading.value = false }
}

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const submitting = ref(false)
const formData = reactive({
  id: null, tenantName: '', tenantCode: '', contactName: '', contactPhone: '',
  tenantType: 'shared', expireTime: null, status: 1
})
const formRules = {
  tenantName: [{ required: true, message: '请输入租户名称', trigger: 'blur' }],
  tenantCode: [{ required: true, message: '请输入租户编码', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  tenantType: [{ required: true, message: '请选择租户模式', trigger: 'change' }]
}

const resetForm = () => {
  Object.assign(formData, {
    id: null, tenantName: '', tenantCode: '', contactName: '', contactPhone: '',
    tenantType: 'shared', expireTime: null, status: 1
  })
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  formRef.value?.resetFields()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(formData, { ...row })
  formRef.value?.resetFields()
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const method = isEdit.value ? 'PUT' : 'POST'
      await useFetch(`/api/v1/admin/tenant${isEdit.value ? '/' + formData.id : ''}`, {
        method, body: formData,
        headers: { Authorization: `Bearer ${auth.token}` }
      })
      ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
      dialogVisible.value = false
      fetchData()
    } catch (e) { /* ignore */ } finally { submitting.value = false }
  })
}

const handleToggleStatus = async (row) => {
  try {
    await useFetch(`/api/v1/admin/tenant/${row.id}/status`, {
      method: 'PUT',
      body: { status: row.status === 1 ? 0 : 1 },
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    row.status = row.status === 1 ? 0 : 1
    ElMessage.success('状态更新成功')
  } catch (e) { /* ignore */ }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除租户「${row.tenantName}」？所有关联数据将被删除。`, '删除确认', {
    type: 'error', confirmButtonText: '确定删除', cancelButtonText: '取消'
  }).then(async () => {
    await useFetch(`/api/v1/admin/tenant/${row.id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}
</script>