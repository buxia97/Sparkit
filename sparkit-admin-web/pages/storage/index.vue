<template>
  <div>
    <div class="page-header">
      <div class="page-title">存储管理</div>
      <div class="page-desc">管理文件存储，支持多存储源切换、文件上传与分片上传</div>
    </div>

    <el-card>
      <el-tabs v-model="activeTab">
        <!-- 文件管理 -->
        <el-tab-pane label="文件管理" name="files">
          <div class="search-bar">
            <div class="search-item">
              <span class="search-label">文件名</span>
              <el-input v-model="searchForm.keyword" placeholder="请输入文件名" clearable style="width: 200px;" />
            </div>
            <div class="search-item">
              <span class="search-label">存储源</span>
              <el-select v-model="searchForm.storageSource" placeholder="全部" clearable style="width: 140px;">
                <el-option v-for="s in storageSources" :key="s.sourceCode" :label="s.name" :value="s.sourceCode" />
              </el-select>
            </div>
            <div class="search-item">
              <span class="search-label">上传时间</span>
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
              <el-upload
                :action="uploadUrl"
                :headers="uploadHeaders"
                :show-file-list="false"
                :on-success="handleUploadSuccess"
                :on-error="handleUploadError"
              >
                <el-button type="primary">
                  <el-icon><Upload /></el-icon>上传文件
                </el-button>
              </el-upload>
              <el-button @click="handleChunkUpload">
                <el-icon><Files /></el-icon>分片上传
              </el-button>
            </div>
          </div>

          <el-table :data="tableData" v-loading="loading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="缩略图" width="80" align="center">
              <template #default="{ row }">
                <img v-if="row.thumbnailPath" :src="`/api/v1/public/storage/thumbnail/${row.id}`" class="file-thumbnail" />
                <span v-else class="text-secondary">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="originalName" label="文件名" min-width="200" show-overflow-tooltip />
            <el-table-column prop="fileSize" label="文件大小" width="100">
              <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
            </el-table-column>
            <el-table-column prop="storageSource" label="存储源" width="100">
              <template #default="{ row }">
                <span class="tag-info">{{ storageLabel(row.storageSource) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="fileMd5" label="MD5" width="120" show-overflow-tooltip />
            <el-table-column prop="createTime" label="上传时间" width="170" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handlePreview(row)">预览</el-button>
                <el-button type="primary" link size="small" @click="handleDownload(row)">下载</el-button>
                <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="page" v-model:page-size="pageSize" :total="total"
            :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchData" @current-change="fetchData"
          />
        </el-tab-pane>

        <!-- 存储源配置 -->
        <el-tab-pane label="存储源配置" name="configs">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-button type="primary" @click="handleConfigAdd">
                <el-icon><Plus /></el-icon>新增存储源
              </el-button>
            </div>
          </div>

          <el-table :data="configData" v-loading="configLoading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="存储源名称" min-width="150" />
            <el-table-column prop="sourceCode" label="编码" width="120" />
            <el-table-column prop="storageType" label="类型" width="120">
              <template #default="{ row }">
                <span class="tag-info">{{ storageTypeLabel(row.storageType) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="bucket" label="Bucket/路径" min-width="150" show-overflow-tooltip />
            <el-table-column prop="isDefault" label="默认" width="70" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.isDefault === 1" type="success" size="small">默认</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <span :class="row.status === 1 ? 'tag-success' : 'tag-danger'">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleConfigEdit(row)">编辑</el-button>
                <el-button v-if="row.isDefault !== 1" type="warning" link size="small" @click="handleConfigSetDefault(row)">设为默认</el-button>
                <el-button type="danger" link size="small" @click="handleConfigDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 分片上传弹窗 -->
    <el-dialog v-model="chunkVisible" title="分片上传" width="500px" destroy-on-close>
      <el-upload
        ref="chunkUploadRef"
        :action="chunkUploadUrl"
        :headers="uploadHeaders"
        :data="chunkData"
        :show-file-list="true"
        :on-success="handleChunkSuccess"
        :on-error="handleChunkError"
        drag
      >
        <div class="empty-state">
          <div class="empty-icon"><el-icon :size="48"><UploadFilled /></el-icon></div>
          <div class="empty-text">将文件拖到此处，或点击上传</div>
        </div>
      </el-upload>
      <template #footer>
        <el-button @click="chunkVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 存储源配置弹窗 -->
    <el-dialog v-model="configDialogVisible" :title="configDialogTitle" width="600px" destroy-on-close>
      <el-form ref="configFormRef" :model="configForm" :rules="configFormRules" label-width="110px">
        <el-form-item label="存储源名称" prop="name">
          <el-input v-model="configForm.name" placeholder="如：本地存储、阿里云OSS" />
        </el-form-item>
        <el-form-item label="存储源编码" prop="sourceCode">
          <el-input v-model="configForm.sourceCode" placeholder="如：local、aliyun-oss" :disabled="!!configForm.id" />
        </el-form-item>
        <el-form-item label="存储类型" prop="storageType">
          <el-select v-model="configForm.storageType" placeholder="选择存储类型" style="width: 100%;">
            <el-option label="本地存储" value="local" />
            <el-option label="FTP远程存储" value="ftp" />
            <el-option label="阿里云OSS" value="aliyun-oss" />
            <el-option label="腾讯云COS" value="tencent-cos" />
            <el-option label="七牛云Kodo" value="qiniu-kodo" />
            <el-option label="S3兼容协议（MinIO/华为云OBS）" value="s3" />
          </el-select>
        </el-form-item>
        <el-form-item label="访问域名" prop="domain">
          <el-input v-model="configForm.domain" placeholder="CDN域名或自定义域名，如：https://cdn.example.com" />
        </el-form-item>
        <el-form-item label="AccessKey" prop="accessKey">
          <el-input v-model="configForm.accessKey" placeholder="访问密钥ID" />
        </el-form-item>
        <el-form-item label="SecretKey" prop="secretKey">
          <el-input v-model="configForm.secretKey" type="password" placeholder="访问密钥Secret" show-password />
        </el-form-item>
        <el-form-item label="Bucket/空间" prop="bucket">
          <el-input v-model="configForm.bucket" placeholder="存储空间名称" />
        </el-form-item>
        <el-form-item label="Endpoint/区域" prop="endpoint">
          <el-input v-model="configForm.endpoint" placeholder="如：oss-cn-hangzhou.aliyuncs.com" />
        </el-form-item>
        <el-form-item label="存储路径前缀" prop="basePath">
          <el-input v-model="configForm.basePath" placeholder="文件存储的基础路径" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="configForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="configForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfigSubmit" :loading="configSubmitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { Upload, Files, UploadFilled, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

definePageMeta({
  middleware: 'auth',
  meta: { title: '存储管理' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/storage', title: '存储管理', icon: 'FolderOpened' })
  fetchData()
  fetchConfigs()
})

const activeTab = ref('files')

const storageTypeLabel = (t) => ({
  local: '本地存储', ftp: 'FTP远程', 'aliyun-oss': '阿里云OSS',
  'tencent-cos': '腾讯云COS', 'qiniu-kodo': '七牛云Kodo', s3: 'S3兼容'
}[t] || t)

const storageSources = ref([])
const storageLabel = (t) => {
  const found = storageSources.value.find(s => s.sourceCode === t)
  return found ? found.name : (t || '未知')
}

const formatSize = (bytes) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

const uploadUrl = '/api/v1/public/storage/upload'
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${auth.token}` }))

// ============ 文件管理 ============
const searchForm = reactive({ keyword: '', storageSource: '', dateRange: [] })
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const loading = ref(false)

const handleSearch = () => { page.value = 1; fetchData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.storageSource = ''; searchForm.dateRange = []; handleSearch() }

const fetchData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.storageSource) params.storageType = searchForm.storageSource
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startTime = searchForm.dateRange[0]
      params.endTime = searchForm.dateRange[1]
    }
    const { data } = await useFetch('/api/v1/admin/storage/files', {
      params, headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      tableData.value = data.value.data.records || []
      total.value = data.value.data.total || 0
    }
  } catch (e) { /* fallback */ } finally { loading.value = false }
}

const handleUploadSuccess = (res) => {
  if (res.code === 200) {
    ElMessage.success('上传成功')
    fetchData()
  }
}

const handleUploadError = () => {
  ElMessage.error('上传失败')
}

const handlePreview = (row) => {
  window.open(`/api/v1/public/storage/preview/${row.id}`, '_blank')
}

const handleDownload = (row) => {
  window.open(`/api/v1/public/storage/download/${row.id}`, '_blank')
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除文件「${row.originalName}」？`, '删除确认', {
    type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
  }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/storage/files/${row.id}`, {
      method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    }
  }).catch(() => {})
}

// ============ 分片上传 ============
const chunkVisible = ref(false)
const chunkUploadUrl = '/api/v1/public/storage/chunk/upload'
const chunkData = reactive({ fileMd5: '', chunkIndex: 0, chunkTotal: 0 })

const handleChunkUpload = () => { chunkVisible.value = true }

const handleChunkSuccess = (res) => {
  if (res.code === 200) {
    ElMessage.success('分片上传成功')
    chunkVisible.value = false
    fetchData()
  }
}

const handleChunkError = () => { ElMessage.error('分片上传失败') }

// ============ 存储源配置 ============
const configData = ref([])
const configLoading = ref(false)

const fetchConfigs = async () => {
  configLoading.value = true
  try {
    const { data } = await useFetch('/api/v1/admin/storage/configs/all', {
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      configData.value = data.value.data || []
      storageSources.value = data.value.data || []
    }
  } catch (e) { /* ignore */ } finally { configLoading.value = false }
}

const configDialogVisible = ref(false)
const configDialogTitle = ref('新增存储源')
const configFormRef = ref(null)
const configSubmitting = ref(false)
const configForm = reactive({
  id: '', name: '', sourceCode: '', storageType: 'local',
  domain: '', accessKey: '', secretKey: '', bucket: '',
  endpoint: '', basePath: '', sort: 0, status: 1
})
const configFormRules = {
  name: [{ required: true, message: '请输入存储源名称', trigger: 'blur' }],
  sourceCode: [{ required: true, message: '请输入存储源编码', trigger: 'blur' }],
  storageType: [{ required: true, message: '请选择存储类型', trigger: 'change' }]
}

const resetConfigForm = () => {
  Object.assign(configForm, {
    id: '', name: '', sourceCode: '', storageType: 'local',
    domain: '', accessKey: '', secretKey: '', bucket: '',
    endpoint: '', basePath: '', sort: 0, status: 1
  })
  configFormRef.value?.resetFields()
}

const handleConfigAdd = () => {
  configDialogTitle.value = '新增存储源'
  resetConfigForm()
  configDialogVisible.value = true
}

const handleConfigEdit = (row) => {
  configDialogTitle.value = '编辑存储源'
  Object.assign(configForm, {
    id: row.id, name: row.name, sourceCode: row.sourceCode, storageType: row.storageType,
    domain: row.domain || '', accessKey: row.accessKey || '', secretKey: row.secretKey || '',
    bucket: row.bucket || '', endpoint: row.endpoint || '', basePath: row.basePath || '',
    sort: row.sort || 0, status: row.status
  })
  configFormRef.value?.resetFields()
  configDialogVisible.value = true
}

const handleConfigSubmit = async () => {
  if (!configFormRef.value) return
  await configFormRef.value.validate(async (valid) => {
    if (!valid) return
    configSubmitting.value = true
    try {
      const url = configForm.id
        ? `/api/v1/admin/storage/configs/${configForm.id}`
        : '/api/v1/admin/storage/configs'
      const method = configForm.id ? 'PUT' : 'POST'
      const { data } = await useFetch(url, {
        method, body: configForm,
        headers: { Authorization: `Bearer ${auth.token}` }
      })
      if (data.value?.code === 200) {
        ElMessage.success(configForm.id ? '编辑成功' : '新增成功')
        configDialogVisible.value = false
        fetchConfigs()
      }
    } catch (e) { /* ignore */ } finally { configSubmitting.value = false }
  })
}

const handleConfigSetDefault = async (row) => {
  const { data } = await useFetch(`/api/v1/admin/storage/configs/${row.id}/default`, {
    method: 'PUT',
    headers: { Authorization: `Bearer ${auth.token}` }
  })
  if (data.value?.code === 200) {
    ElMessage.success(`已将「${row.name}」设为默认存储源`)
    fetchConfigs()
  }
}

const handleConfigDelete = (row) => {
  ElMessageBox.confirm(`确定删除存储源「${row.name}」？`, '删除确认', {
    type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
  }).then(async () => {
    if (row.isDefault === 1) {
      ElMessage.warning('不能删除默认存储源，请先设置其他存储源为默认')
      return
    }
    const { data } = await useFetch(`/api/v1/admin/storage/configs/${row.id}`, {
      method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      ElMessage.success('删除成功')
      fetchConfigs()
    }
  }).catch(() => {})
}
</script>

<style scoped>
.file-thumbnail {
  width: 60px;
  height: 45px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}
</style>