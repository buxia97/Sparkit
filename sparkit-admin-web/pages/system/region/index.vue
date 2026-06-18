<template>
  <div>
    <div class="page-header">
      <div class="page-title">地区管理</div>
      <div class="page-desc">管理中国省/市/区三级地区数据</div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="handleAdd(null)">
          <el-icon><Plus /></el-icon>新增顶级地区
        </el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="regionTree" v-loading="loading" row-key="id" stripe default-expand-all>
        <el-table-column prop="code" label="地区编码" width="140" />
        <el-table-column prop="name" label="地区名称" min-width="200" />
        <el-table-column prop="level" label="层级" width="80" align="center">
          <template #default="{ row }">
            <span :class="levelTag(row.level)">{{ levelLabel(row.level) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.level < 3" type="primary" link size="small" @click="handleAdd(row)">新增下级</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="450px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item v-if="formData.parentName" label="上级地区">
          <span class="text-secondary">{{ formData.parentName }}</span>
        </el-form-item>
        <el-form-item label="地区名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入地区名称" />
        </el-form-item>
        <el-form-item label="地区编码" prop="code">
          <el-input v-model="formData.code" placeholder="请输入地区编码" :disabled="!!formData.id" />
        </el-form-item>
        <el-form-item label="层级">
          <span class="text-secondary">{{ levelLabel(formData.level) }}</span>
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
  meta: { title: '地区管理' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/system/region', title: '地区管理', icon: 'Location' })
  fetchData()
})

const regionTree = ref([])
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await useFetch('/api/v1/admin/regions/tree', { headers: { Authorization: `Bearer ${auth.token}` } })
    if (data.value?.code === 200) regionTree.value = data.value.data
  } catch (e) { /* fallback */ } finally { loading.value = false }
}

const levelLabel = (level) => ({ 1: '省级', 2: '市级', 3: '区县' }[level] || '')
const levelTag = (level) => ({ 1: 'tag-success', 2: 'tag-info', 3: 'tag-warning' }[level] || '')

const dialogVisible = ref(false)
const dialogTitle = ref('新增地区')
const formRef = ref(null)
const submitting = ref(false)
const formData = reactive({ id: '', name: '', code: '', level: 1, parentCode: '', parentName: '' })
const formRules = {
  name: [{ required: true, message: '请输入地区名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入地区编码', trigger: 'blur' }]
}

const handleAdd = (parent) => {
  dialogTitle.value = '新增地区'
  resetForm()
  if (parent) {
    formData.parentCode = parent.code
    formData.parentName = parent.name
    formData.level = (parent.level || 0) + 1
  }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑地区'
  Object.assign(formData, {
    id: row.id, name: row.name, code: row.code, level: row.level,
    parentCode: row.parentCode || '', parentName: row.parentName || ''
  })
  dialogVisible.value = true
}

const resetForm = () => {
  Object.assign(formData, { id: '', name: '', code: '', level: 1, parentCode: '', parentName: '' })
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const url = formData.id ? `/api/v1/admin/regions/${formData.id}` : '/api/v1/admin/regions'
      const method = formData.id ? 'PUT' : 'POST'
      const { data } = await useFetch(url, { method, body: formData, headers: { Authorization: `Bearer ${auth.token}` } })
      if (data.value?.code === 200) { ElMessage.success(formData.id ? '编辑成功' : '新增成功'); dialogVisible.value = false; fetchData() }
    } catch (e) { /* ignore */ } finally { submitting.value = false }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除「${row.name}」？`, '删除确认', { type: 'warning' }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/regions/${row.id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` } })
    if (data.value?.code === 200) { ElMessage.success('删除成功'); fetchData() }
  }).catch(() => {})
}
</script>