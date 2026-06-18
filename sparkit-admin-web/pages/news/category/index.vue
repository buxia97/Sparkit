<template>
  <div>
    <div class="page-header">
      <div class="page-title">新闻分类</div>
      <div class="page-desc">管理新闻分类，支持多级分类树结构</div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="handleAdd({})">
          <el-icon><Plus /></el-icon>新增根分类
        </el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="categoryTree" v-loading="loading" row-key="id" stripe default-expand-all>
        <el-table-column prop="categoryName" label="分类名称" min-width="200" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <span :class="row.status === 1 ? 'tag-success' : 'tag-danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
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
        <el-form-item label="上级分类">
          <el-tree-select
            v-model="formData.parentId"
            :data="categoryTree"
            placeholder="无（根分类）"
            clearable
            check-strictly
            :props="{ label: 'categoryName', value: 'id' }"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="formData.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" :max="999" />
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
  meta: { title: '新闻分类' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/news/category', title: '新闻分类', icon: 'CollectionTag' })
  fetchData()
})

const categoryTree = ref([])
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await useFetch('/api/v1/admin/news/categories/tree', {
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) categoryTree.value = data.value.data || []
  } catch (e) { /* ignore */ } finally { loading.value = false }
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const formRef = ref(null)
const submitting = ref(false)
const formData = reactive({ id: '', parentId: '', categoryName: '', sort: 0, status: 1 })
const formRules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const handleAdd = (parent) => {
  dialogTitle.value = '新增分类'
  resetForm()
  if (parent.id) { formData.parentId = parent.id }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑分类'
  resetForm()
  Object.assign(formData, {
    id: row.id, parentId: row.parentId || '', categoryName: row.categoryName,
    sort: row.sort || 0, status: row.status
  })
  dialogVisible.value = true
}

const resetForm = () => {
  Object.assign(formData, { id: '', parentId: '', categoryName: '', sort: 0, status: 1 })
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const url = formData.id
        ? `/api/v1/admin/news/categories/${formData.id}`
        : '/api/v1/admin/news/categories'
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

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除分类「${row.categoryName}」？`, '删除确认', {
    type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
  }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/news/categories/${row.id}`, {
      method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    }
  }).catch(() => {})
}
</script>