<template>
  <div>
    <div class="page-header">
      <div class="page-title">代码生成器</div>
      <div class="page-desc">根据数据库表结构自动生成代码，支持生成Controller、Service、Mapper、Entity等</div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="handleImport">
          <el-icon><Download /></el-icon>导入表
        </el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="tableName" label="表名" min-width="180" />
        <el-table-column prop="tableComment" label="表注释" min-width="200" show-overflow-tooltip />
        <el-table-column prop="className" label="类名" width="150" />
        <el-table-column prop="packageName" label="包名" width="200" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" link size="small" @click="handlePreview(row)">预览</el-button>
            <el-button type="success" link size="small" @click="handleGenerate(row)">生成</el-button>
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

    <!-- 导入表弹窗 -->
    <el-dialog v-model="importVisible" title="导入数据库表" width="700px" destroy-on-close>
      <div class="search-bar">
        <div class="search-item">
          <span class="search-label">表名</span>
          <el-input v-model="importKeyword" placeholder="请输入表名" clearable style="width: 200px;" @input="fetchDbTables" />
        </div>
        <div class="search-actions">
          <el-button type="primary" @click="fetchDbTables">搜索</el-button>
        </div>
      </div>
      <el-table :data="dbTables" v-loading="dbLoading" stripe @selection-change="handleSelectChange" max-height="400">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="tableName" label="表名" min-width="200" />
        <el-table-column prop="tableComment" label="表注释" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
      </el-table>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImportSubmit" :loading="importSubmitting" :disabled="selectedTables.length === 0">
          导入选中表 ({{ selectedTables.length }})
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="表名" prop="tableName">
          <el-input v-model="formData.tableName" :disabled="!!formData.id" />
        </el-form-item>
        <el-form-item label="表注释" prop="tableComment">
          <el-input v-model="formData.tableComment" />
        </el-form-item>
        <el-form-item label="类名" prop="className">
          <el-input v-model="formData.className" />
        </el-form-item>
        <el-form-item label="包名" prop="packageName">
          <el-input v-model="formData.packageName" placeholder="如：com.sparkit.news" />
        </el-form-item>
        <el-form-item label="模块名" prop="moduleName">
          <el-input v-model="formData.moduleName" placeholder="如：sparkit-news" />
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="formData.author" />
        </el-form-item>
        <el-form-item label="生成选项">
          <el-checkbox-group v-model="genOptions">
            <el-checkbox label="entity">Entity</el-checkbox>
            <el-checkbox label="mapper">Mapper</el-checkbox>
            <el-checkbox label="service">Service</el-checkbox>
            <el-checkbox label="controller">Controller</el-checkbox>
            <el-checkbox label="xml">Mapper XML</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <!-- 预览弹窗 -->
    <el-dialog v-model="previewVisible" title="代码预览" width="800px" destroy-on-close>
      <el-tabs v-model="previewTab">
        <el-tab-pane v-for="file in previewFiles" :key="file.name" :label="file.name" :name="file.name">
          <pre class="code-preview"><code>{{ file.content }}</code></pre>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { Download } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

definePageMeta({
  middleware: 'auth',
  meta: { title: '代码生成器' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/generator', title: '代码生成器', icon: 'EditPen' })
  fetchData()
})

const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await useFetch('/api/v1/admin/generator/tables', {
      params: { page: page.value, pageSize: pageSize.value },
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      tableData.value = data.value.data.records || []
      total.value = data.value.data.total || 0
    }
  } catch (e) { /* fallback */ } finally { loading.value = false }
}

// 导入
const importVisible = ref(false)
const importKeyword = ref('')
const dbTables = ref([])
const dbLoading = ref(false)
const selectedTables = ref([])
const importSubmitting = ref(false)

const handleImport = () => {
  importKeyword.value = ''
  selectedTables.value = []
  fetchDbTables()
  importVisible.value = true
}

const fetchDbTables = async () => {
  dbLoading.value = true
  try {
    const params = {}
    if (importKeyword.value) params.keyword = importKeyword.value
    const { data } = await useFetch('/api/v1/admin/generator/db-tables', {
      params, headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) dbTables.value = data.value.data || []
  } catch (e) { /* ignore */ } finally { dbLoading.value = false }
}

const handleSelectChange = (rows) => {
  selectedTables.value = rows
}

const handleImportSubmit = async () => {
  importSubmitting.value = true
  try {
    const { data } = await useFetch('/api/v1/admin/generator/tables/import', {
      method: 'POST',
      body: { tables: selectedTables.value.map(t => t.tableName) },
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      ElMessage.success('导入成功')
      importVisible.value = false
      fetchData()
    }
  } catch (e) { /* ignore */ } finally { importSubmitting.value = false }
}

// 编辑
const dialogVisible = ref(false)
const dialogTitle = ref('编辑表配置')
const formRef = ref(null)
const submitting = ref(false)
const formData = reactive({
  id: '', tableName: '', tableComment: '', className: '', packageName: '', moduleName: '', author: ''
})
const formRules = {
  tableName: [{ required: true, message: '请输入表名', trigger: 'blur' }],
  className: [{ required: true, message: '请输入类名', trigger: 'blur' }],
  packageName: [{ required: true, message: '请输入包名', trigger: 'blur' }]
}
const genOptions = ref(['entity', 'mapper', 'service', 'controller', 'xml'])

const handleEdit = (row) => {
  dialogTitle.value = '编辑表配置'
  Object.assign(formData, {
    id: row.id, tableName: row.tableName, tableComment: row.tableComment || '',
    className: row.className, packageName: row.packageName || '',
    moduleName: row.moduleName || '', author: row.author || ''
  })
  formRef.value?.resetFields()
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const url = formData.id
        ? `/api/v1/admin/generator/tables/${formData.id}`
        : '/api/v1/admin/generator/tables'
      const method = formData.id ? 'PUT' : 'POST'
      const { data } = await useFetch(url, {
        method, body: { ...formData, genOptions: genOptions.value },
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
  ElMessageBox.confirm(`确定删除表配置「${row.tableName}」？`, '删除确认', {
    type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
  }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/generator/tables/${row.id}`, {
      method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    }
  }).catch(() => {})
}

// 预览
const previewVisible = ref(false)
const previewTab = ref('')
const previewFiles = ref([])

const handlePreview = async (row) => {
  try {
    const { data } = await useFetch(`/api/v1/admin/generator/tables/${row.id}/preview`, {
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      previewFiles.value = data.value.data || []
      previewTab.value = previewFiles.value[0]?.name || ''
      previewVisible.value = true
    }
  } catch (e) { /* ignore */ }
}

// 生成
const handleGenerate = async (row) => {
  ElMessageBox.confirm(`确定生成「${row.tableName}」的代码？`, '生成确认', {
    type: 'info', confirmButtonText: '确定', cancelButtonText: '取消'
  }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/generator/tables/${row.id}/generate`, {
      method: 'POST', headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      ElMessage.success('代码生成成功')
    }
  }).catch(() => {})
}
</script>