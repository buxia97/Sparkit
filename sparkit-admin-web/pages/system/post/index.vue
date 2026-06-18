<template>
  <div>
    <div class="page-header">
      <div class="page-title">岗位管理</div>
      <div class="page-desc">管理岗位信息，岗位与部门关联</div>
    </div>

    <div class="search-bar">
      <div class="search-item">
        <span class="search-label">岗位名称</span>
        <el-input v-model="searchForm.keyword" placeholder="请输入岗位名称" clearable style="width: 180px;" />
      </div>
      <div class="search-item">
        <span class="search-label">状态</span>
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px;">
          <el-option label="正常" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </div>
      <div class="search-actions">
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增岗位
        </el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="postName" label="岗位名称" min-width="150" />
        <el-table-column prop="postCode" label="岗位编码" min-width="150" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <span :class="row.status === 1 ? 'tag-success' : 'tag-danger'">
              {{ row.status === 1 ? '正常' : '停用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page" v-model:page-size="pageSize" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchData" @current-change="fetchData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="岗位名称" prop="postName">
          <el-input v-model="formData.postName" placeholder="请输入岗位名称" />
        </el-form-item>
        <el-form-item label="岗位编码" prop="postCode">
          <el-input v-model="formData.postCode" placeholder="请输入岗位编码" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="请输入备注" />
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
  meta: { title: '岗位管理' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/system/post', title: '岗位管理', icon: 'Briefcase' })
  fetchData()
})

const searchForm = reactive({ keyword: '', status: '' })
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const loading = ref(false)

const handleSearch = () => { page.value = 1; fetchData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.status = ''; handleSearch() }

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await useFetch('/api/v1/admin/posts', {
      params: { page: page.value, pageSize: pageSize.value, ...searchForm },
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) { tableData.value = data.value.data.records; total.value = data.value.data.total }
  } catch (e) { /* fallback */ } finally { loading.value = false }
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增岗位')
const formRef = ref(null)
const submitting = ref(false)
const formData = reactive({ id: '', postName: '', postCode: '', sort: 0, status: 1, remark: '' })
const formRules = {
  postName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  postCode: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }]
}

const handleAdd = () => { dialogTitle.value = '新增岗位'; resetForm(); dialogVisible.value = true }
const handleEdit = (row) => {
  dialogTitle.value = '编辑岗位'
  Object.assign(formData, { id: row.id, postName: row.postName, postCode: row.postCode, sort: row.sort, status: row.status, remark: row.remark || '' })
  dialogVisible.value = true
}
const resetForm = () => { Object.assign(formData, { id: '', postName: '', postCode: '', sort: 0, status: 1, remark: '' }); formRef.value?.resetFields() }

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const url = formData.id ? `/api/v1/admin/posts/${formData.id}` : '/api/v1/admin/posts'
      const method = formData.id ? 'PUT' : 'POST'
      const { data } = await useFetch(url, { method, body: formData, headers: { Authorization: `Bearer ${auth.token}` } })
      if (data.value?.code === 200) { ElMessage.success(formData.id ? '编辑成功' : '新增成功'); dialogVisible.value = false; fetchData() }
    } catch (e) { /* ignore */ } finally { submitting.value = false }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除岗位「${row.postName}」？`, '删除确认', { type: 'warning' }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/posts/${row.id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` } })
    if (data.value?.code === 200) { ElMessage.success('删除成功'); fetchData() }
  }).catch(() => {})
}
</script>