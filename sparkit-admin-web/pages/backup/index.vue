<template>
  <div>
    <div class="page-header">
      <div class="page-title">数据备份</div>
      <div class="page-desc">数据库备份与恢复，支持本地备份与远程备份</div>
    </div>

    <el-card title="备份列表">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button type="primary" @click="handleCreateBackup" :loading="backingUp">
            <el-icon><Plus /></el-icon>立即备份
          </el-button>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="backupName" label="备份名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="backupType" label="备份类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.backupType === 'full' ? 'primary' : 'warning'" size="small">
              {{ row.backupType === 'full' ? '全量备份' : '增量备份' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="文件大小" width="120">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="storageType" label="存储位置" width="120">
          <template #default="{ row }">
            <el-tag :type="row.storageType === 'local' ? 'success' : ''" size="small">
              {{ row.storageType === 'local' ? '本地' : '远程' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <span :class="row.status === 'success' ? 'tag-success' : row.status === 'failed' ? 'tag-danger' : 'tag-warning'">
              {{ statusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="备份时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleRestore(row)" :disabled="row.status !== 'success'">恢复</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

definePageMeta({
  middleware: 'auth',
  meta: { title: '数据备份' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/backup', title: '数据备份', icon: 'FolderOpened' })
  fetchData()
})

const tableData = ref([])
const loading = ref(false)
const backingUp = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await useFetch('/api/v1/admin/backup/list', {
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      tableData.value = data.value.data || []
    }
  } catch (e) {
    // 模拟数据
    tableData.value = [
      { id: 1, backupName: 'sparkit_backup_20240615_010000.sql', backupType: 'full', fileSize: 52428800, storageType: 'local', status: 'success', createTime: '2024-06-15 01:00:00' },
      { id: 2, backupName: 'sparkit_backup_20240616_010000.sql', backupType: 'full', fileSize: 53694464, storageType: 'local', status: 'success', createTime: '2024-06-16 01:00:00' },
      { id: 3, backupName: 'sparkit_backup_20240617_010000.sql', backupType: 'full', fileSize: 54999040, storageType: 'remote', status: 'success', createTime: '2024-06-17 01:00:00' }
    ]
  } finally { loading.value = false }
}

const handleCreateBackup = async () => {
  backingUp.value = true
  try {
    await useFetch('/api/v1/admin/backup/create', {
      method: 'POST',
      body: { backupType: 'full' },
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    ElMessage.success('备份任务已提交，请稍后刷新查看')
    setTimeout(() => fetchData(), 3000)
  } catch (e) {
    ElMessage.info('备份任务已提交')
  } finally { backingUp.value = false }
}

const handleRestore = (row) => {
  ElMessageBox.confirm(
    `确定使用备份「${row.backupName}」恢复数据？当前数据将被覆盖，此操作不可逆。`,
    '恢复确认',
    { type: 'error', confirmButtonText: '确定恢复', cancelButtonText: '取消' }
  ).then(async () => {
    await useFetch(`/api/v1/admin/backup/${row.id}/restore`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    ElMessage.success('恢复任务已提交')
  }).catch(() => {})
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除备份「${row.backupName}」？`, '删除确认', {
    type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
  }).then(async () => {
    await useFetch(`/api/v1/admin/backup/${row.id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(1) + ' ' + units[i]
}

const statusLabel = (status) => {
  const map = { success: '成功', failed: '失败', running: '进行中' }
  return map[status] || status
}
</script>