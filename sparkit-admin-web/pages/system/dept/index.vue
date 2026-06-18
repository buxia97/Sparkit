<template>
  <div>
    <div class="page-header">
      <div class="page-title">部门管理</div>
      <div class="page-desc">管理组织架构，维护部门层级关系</div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="handleAdd({})">
          <el-icon><Plus /></el-icon>新增根部门
        </el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="deptTree" v-loading="loading" row-key="id" stripe default-expand-all>
        <el-table-column prop="deptName" label="部门名称" min-width="200" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="leader" label="负责人" width="120" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <span :class="row.status === 1 ? 'tag-success' : 'tag-danger'">
              {{ row.status === 1 ? '正常' : '停用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleAdd(row)">新增子级</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="formData.parentId"
            :data="deptTree"
            placeholder="无（根部门）"
            clearable
            check-strictly
            :props="{ label: 'deptName', value: 'id' }"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="formData.deptName" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="formData.leader" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="formData.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
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
  meta: { title: '部门管理' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/system/dept', title: '部门管理', icon: 'OfficeBuilding' })
  fetchData()
})

const deptTree = ref([])
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await useFetch('/api/v1/admin/depts/tree', { headers: { Authorization: `Bearer ${auth.token}` } })
    if (data.value?.code === 200) deptTree.value = data.value.data
  } catch (e) { /* fallback */ } finally { loading.value = false }
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增部门')
const formRef = ref(null)
const submitting = ref(false)
const formData = reactive({ id: '', parentId: '', deptName: '', sort: 0, leader: '', phone: '', email: '', status: 1 })
const formRules = { deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }] }

const handleAdd = (parent) => { dialogTitle.value = '新增部门'; resetForm(); formData.parentId = parent.id || ''; dialogVisible.value = true }
const handleEdit = (row) => {
  dialogTitle.value = '编辑部门'
  Object.assign(formData, { id: row.id, parentId: row.parentId || '', deptName: row.deptName, sort: row.sort, leader: row.leader || '', phone: row.phone || '', email: row.email || '', status: row.status })
  dialogVisible.value = true
}
const resetForm = () => { Object.assign(formData, { id: '', parentId: '', deptName: '', sort: 0, leader: '', phone: '', email: '', status: 1 }); formRef.value?.resetFields() }

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const url = formData.id ? `/api/v1/admin/depts/${formData.id}` : '/api/v1/admin/depts'
      const method = formData.id ? 'PUT' : 'POST'
      const { data } = await useFetch(url, { method, body: formData, headers: { Authorization: `Bearer ${auth.token}` } })
      if (data.value?.code === 200) { ElMessage.success(formData.id ? '编辑成功' : '新增成功'); dialogVisible.value = false; fetchData() }
    } catch (e) { /* ignore */ } finally { submitting.value = false }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除部门「${row.deptName}」？`, '删除确认', { type: 'warning' }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/depts/${row.id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` } })
    if (data.value?.code === 200) { ElMessage.success('删除成功'); fetchData() }
  }).catch(() => {})
}
</script>