<template>
  <div>
    <div class="page-header">
      <div class="page-title">数据导入导出</div>
      <div class="page-desc">支持用户/角色/部门/配置等数据的批量导入与导出</div>
    </div>

    <el-card>
      <el-tabs v-model="activeTab">
        <!-- 导出 -->
        <el-tab-pane label="数据导出" name="export">
          <el-form label-width="120px" style="max-width: 600px;">
            <el-form-item label="导出模块">
              <el-select v-model="exportForm.module" style="width: 100%;">
                <el-option label="C端用户" value="user" />
                <el-option label="管理用户" value="admin" />
                <el-option label="角色" value="role" />
                <el-option label="部门" value="dept" />
                <el-option label="岗位" value="post" />
                <el-option label="系统配置" value="config" />
                <el-option label="字典数据" value="dict" />
                <el-option label="地区数据" value="region" />
              </el-select>
            </el-form-item>
            <el-form-item label="导出格式">
              <el-radio-group v-model="exportForm.format">
                <el-radio value="csv">CSV（推荐）</el-radio>
                <el-radio value="excel">Excel</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="导出文件名">
              <el-input v-model="exportForm.fileName" placeholder="可选，不填默认使用模块名" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleExport">导出数据</el-button>
              <el-button @click="handleDownloadTemplate">下载导入模板</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 导入 -->
        <el-tab-pane label="数据导入" name="import">
          <el-form label-width="120px" style="max-width: 600px;">
            <el-form-item label="导入模块">
              <el-select v-model="importForm.module" style="width: 100%;">
                <el-option label="C端用户" value="user" />
                <el-option label="管理用户" value="admin" />
                <el-option label="角色" value="role" />
                <el-option label="部门" value="dept" />
                <el-option label="岗位" value="post" />
                <el-option label="字典数据" value="dict" />
              </el-select>
            </el-form-item>
            <el-form-item label="上传文件">
              <el-upload
                :show-file-list="true"
                :before-upload="handleBeforeImport"
                :auto-upload="false"
                accept=".csv"
                drag
              >
                <div class="empty-state">
                  <div class="empty-icon"><el-icon :size="48"><UploadFilled /></el-icon></div>
                  <div class="empty-text">将 CSV 文件拖到此处，或点击上传</div>
                  <div class="empty-desc">仅支持 CSV 格式，请先下载导入模板</div>
                </div>
              </el-upload>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleImport" :loading="importing" :disabled="!importFile">
                开始导入
              </el-button>
              <el-button @click="handleDownloadTemplate">下载导入模板</el-button>
            </el-form-item>
          </el-form>

          <div v-if="importResult.length > 0" style="margin-top: 20px;">
            <el-alert :title="`导入完成，共处理 ${importResult.length} 行数据`" type="success" :closable="false" show-icon />
            <el-table :data="importResult" stripe style="margin-top: 12px;" max-height="400">
              <el-table-column type="index" label="#" width="50" />
              <el-table-column v-for="h in importHeaders" :key="h" :prop="h" :label="h" min-width="120" />
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

definePageMeta({
  middleware: 'auth',
  meta: { title: '导入导出' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/system/import-export', title: '导入导出', icon: 'Upload' })
})

const activeTab = ref('export')

// 导出
const exportForm = reactive({ module: 'user', format: 'csv', fileName: '' })

const handleExport = async () => {
  try {
    const { data } = await useFetch(`/api/v1/admin/import-export/export/${exportForm.module}`, {
      method: 'POST',
      params: { format: exportForm.format },
      body: {
        headers: moduleHeaders[exportForm.module],
        data: moduleData[exportForm.module] || [],
        fileName: exportForm.fileName || exportForm.module
      },
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    ElMessage.success('导出成功')
  } catch (e) {
    // 如果是文件下载，可能不会返回 JSON
    ElMessage.info('导出请求已发送')
  }
}

const handleDownloadTemplate = async () => {
  const module = activeTab.value === 'export' ? exportForm.module : importForm.module
  const headers = moduleHeaders[module] || []
  try {
    const { data } = await useFetch(`/api/v1/admin/import-export/template/${module}`, {
      params: { headers: headers.join(','), fileName: module + '导入模板' },
      headers: { Authorization: `Bearer ${auth.token}` }
    })
  } catch (e) {
    ElMessage.info('模板下载请求已发送')
  }
}

// 导入
const importForm = reactive({ module: 'user' })
const importFile = ref(null)
const importing = ref(false)
const importResult = ref([])
const importHeaders = ref([])

const handleBeforeImport = (file) => {
  importFile.value = file
  return false
}

const handleImport = async () => {
  if (!importFile.value) return
  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', importFile.value)
    const { data } = await useFetch(`/api/v1/admin/import-export/import/${importForm.module}`, {
      method: 'POST',
      body: formData,
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      const rows = data.value.data || []
      if (rows.length > 0) {
        importHeaders.value = rows[0]
        importResult.value = rows.slice(1).map((row, idx) => {
          const obj = { _idx: idx + 1 }
          importHeaders.value.forEach((h, i) => { obj[h] = row[i] || '' })
          return obj
        })
      }
      ElMessage.success(`导入成功，共 ${rows.length - 1} 条数据`)
    }
  } catch (e) {
    ElMessage.error('导入失败')
  } finally { importing.value = false }
}

const moduleHeaders = {
  user: ['用户名', '昵称', '邮箱', '手机号', '状态'],
  admin: ['用户名', '昵称', '邮箱', '手机号', '部门', '状态'],
  role: ['角色名称', '角色编码', '排序', '状态'],
  dept: ['部门名称', '上级部门', '排序', '状态'],
  post: ['岗位名称', '岗位编码', '排序', '状态'],
  config: ['配置键', '配置值', '配置分组'],
  dict: ['字典类型', '字典标签', '字典值', '排序'],
  region: ['地区名称', '地区编码', '上级编码', '层级']
}
</script>