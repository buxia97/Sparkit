<template>
  <div>
    <div class="page-header">
      <div class="page-title">新闻文章</div>
      <div class="page-desc">管理新闻文章，包括发布、编辑、审核、删除，支持AI生成</div>
    </div>

    <div class="search-bar">
      <div class="search-item">
        <span class="search-label">分类</span>
        <el-tree-select
          v-model="searchForm.categoryId"
          :data="categoryTree"
          placeholder="全部分类"
          clearable
          check-strictly
          :props="{ label: 'categoryName', value: 'id' }"
          style="width: 180px;"
        />
      </div>
      <div class="search-item">
        <span class="search-label">状态</span>
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px;">
          <el-option label="草稿" :value="0" />
          <el-option label="已发布" :value="1" />
          <el-option label="已下架" :value="2" />
        </el-select>
      </div>
      <div class="search-item">
        <span class="search-label">关键词</span>
        <el-input v-model="searchForm.keyword" placeholder="标题/作者" clearable style="width: 200px;" />
      </div>
      <div class="search-item">
        <span class="search-label">发布时间</span>
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px;"
        />
      </div>
      <div class="search-actions">
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增文章
        </el-button>
        <el-button @click="handleAiGenerate">
          <el-icon><MagicStick /></el-icon>AI生成
        </el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column prop="author" label="作者" width="100" />
        <el-table-column prop="viewCount" label="浏览量" width="90" align="center" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <span :class="articleStatusClass(row.status)">
              {{ articleStatusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="isTop" label="置顶" width="70">
          <template #default="{ row }">
            <span :class="row.isTop === 1 ? 'tag-success' : ''">{{ row.isTop === 1 ? '是' : '否' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" type="success" link size="small" @click="handlePublish(row)">发布</el-button>
            <el-button v-if="row.status === 1" type="warning" link size="small" @click="handleOffline(row)">下架</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page" v-model:page-size="pageSize" :total="total"
        :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchData" @current-change="fetchData"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="750px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="分类" prop="categoryId">
          <el-tree-select
            v-model="formData.categoryId"
            :data="categoryTree"
            placeholder="请选择分类"
            check-strictly
            :props="{ label: 'categoryName', value: 'id' }"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入文章标题" />
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="formData.author" placeholder="请输入作者" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="封面图" prop="coverImage">
          <el-input v-model="formData.coverImage" placeholder="请输入封面图URL" />
        </el-form-item>
        <el-form-item label="摘要" prop="summary">
          <el-input v-model="formData.summary" type="textarea" :rows="3" placeholder="请输入文章摘要" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="formData.content" type="textarea" :rows="8" placeholder="请输入文章内容" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="置顶" prop="isTop">
          <el-switch v-model="formData.isTop" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button @click="handleSaveDraft" :loading="submitting">保存草稿</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- AI生成弹窗 -->
    <el-dialog v-model="aiDialogVisible" title="AI生成文章" width="600px" destroy-on-close>
      <el-form ref="aiFormRef" :model="aiForm" :rules="aiFormRules" label-width="80px">
        <el-form-item label="AI模型" prop="modelId">
          <el-select v-model="aiForm.modelId" placeholder="请选择AI模型" style="width: 100%;">
            <el-option v-for="m in aiModels" :key="m.id" :label="m.modelName" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-tree-select
            v-model="aiForm.categoryId"
            :data="categoryTree"
            placeholder="请选择分类"
            check-strictly
            :props="{ label: 'categoryName', value: 'id' }"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="主题/关键词" prop="topic">
          <el-input v-model="aiForm.topic" placeholder="请输入文章主题或关键词" />
        </el-form-item>
        <el-form-item label="生成要求" prop="requirement">
          <el-input v-model="aiForm.requirement" type="textarea" :rows="3" placeholder="额外的生成要求（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAiSubmit" :loading="aiGenerating">开始生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { Plus, MagicStick } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

definePageMeta({
  middleware: 'auth',
  meta: { title: '新闻文章' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/news/article', title: '新闻文章', icon: 'Notebook' })
  fetchData()
  fetchCategoryTree()
  fetchAiModels()
})

const articleStatusLabel = (s) => ({ 0: '草稿', 1: '已发布', 2: '已下架' }[s] || '未知')
const articleStatusClass = (s) => ({ 0: '', 1: 'tag-success', 2: 'tag-warning' }[s] || '')

const searchForm = reactive({ categoryId: '', status: '', keyword: '', dateRange: [] })
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const loading = ref(false)

const handleSearch = () => { page.value = 1; fetchData() }
const handleReset = () => { searchForm.categoryId = ''; searchForm.status = ''; searchForm.keyword = ''; searchForm.dateRange = []; handleSearch() }

const fetchData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (searchForm.categoryId) params.categoryId = searchForm.categoryId
    if (searchForm.status !== '') params.status = searchForm.status
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startTime = searchForm.dateRange[0]
      params.endTime = searchForm.dateRange[1]
    }
    const { data } = await useFetch('/api/v1/admin/news/articles', {
      params, headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      tableData.value = data.value.data.records || []
      total.value = data.value.data.total || 0
    }
  } catch (e) { /* fallback */ } finally { loading.value = false }
}

const categoryTree = ref([])
const fetchCategoryTree = async () => {
  try {
    const { data } = await useFetch('/api/v1/admin/news/categories/tree', {
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) categoryTree.value = data.value.data || []
  } catch (e) { /* ignore */ }
}

const aiModels = ref([])
const fetchAiModels = async () => {
  try {
    const { data } = await useFetch('/api/v1/admin/ai/models', {
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) aiModels.value = data.value.data || []
  } catch (e) { /* ignore */ }
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增文章')
const formRef = ref(null)
const submitting = ref(false)
const formData = reactive({
  id: '', categoryId: '', title: '', author: '', coverImage: '',
  summary: '', content: '', sort: 0, isTop: 0
})
const formRules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }]
}

const handleAdd = () => {
  dialogTitle.value = '新增文章'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑文章'
  resetForm()
  Object.assign(formData, {
    id: row.id, categoryId: row.categoryId, title: row.title,
    author: row.author || '', coverImage: row.coverImage || '',
    summary: row.summary || '', content: row.content || '',
    sort: row.sort || 0, isTop: row.isTop || 0
  })
  dialogVisible.value = true
}

const resetForm = () => {
  Object.assign(formData, { id: '', categoryId: '', title: '', author: '', coverImage: '', summary: '', content: '', sort: 0, isTop: 0 })
  formRef.value?.resetFields()
}

const handleSaveDraft = async () => {
  formData.status = 0
  await doSubmit()
}

const handleSubmit = async () => {
  formData.status = 1
  await doSubmit()
}

const doSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const url = formData.id
        ? `/api/v1/admin/news/articles/${formData.id}`
        : '/api/v1/admin/news/articles'
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

const handlePublish = async (row) => {
  const { data } = await useFetch(`/api/v1/admin/news/articles/${row.id}/publish`, {
    method: 'PUT',
    headers: { Authorization: `Bearer ${auth.token}` }
  })
  if (data.value?.code === 200) {
    ElMessage.success('发布成功')
    fetchData()
  }
}

const handleOffline = async (row) => {
  const { data } = await useFetch(`/api/v1/admin/news/articles/${row.id}/unpublish`, {
    method: 'PUT',
    headers: { Authorization: `Bearer ${auth.token}` }
  })
  if (data.value?.code === 200) {
    ElMessage.success('已下架')
    fetchData()
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除文章「${row.title}」？`, '删除确认', {
    type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
  }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/news/articles/${row.id}`, {
      method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    }
  }).catch(() => {})
}

const aiDialogVisible = ref(false)
const aiFormRef = ref(null)
const aiGenerating = ref(false)
const aiForm = reactive({ modelId: '', categoryId: '', topic: '', requirement: '' })
const aiFormRules = {
  modelId: [{ required: true, message: '请选择AI模型', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  topic: [{ required: true, message: '请输入主题/关键词', trigger: 'blur' }]
}

const handleAiGenerate = () => {
  ElMessage.info('AI生成功能需要后端集成AI服务，请先配置AI模型')
}

const handleAiSubmit = async () => {
  if (!aiFormRef.value) return
  await aiFormRef.value.validate(async (valid) => {
    if (!valid) return
    aiGenerating.value = true
    try {
      const { data } = await useFetch('/api/v1/admin/ai/generate', {
        method: 'POST', body: aiForm,
        headers: { Authorization: `Bearer ${auth.token}` }
      })
      if (data.value?.code === 200) {
        ElMessage.success('AI生成成功')
        aiDialogVisible.value = false
        fetchData()
      } else {
        ElMessage.warning(data.value?.msg || 'AI生成失败，请检查AI服务配置')
      }
    } catch (e) { /* ignore */ } finally { aiGenerating.value = false }
  })
}
</script>