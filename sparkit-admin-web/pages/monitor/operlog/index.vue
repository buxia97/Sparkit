<template>
  <div>
    <div class="page-header">
      <div class="page-title">操作日志</div>
      <div class="page-desc">查看管理员操作日志，记录所有增删改操作</div>
    </div>

    <div class="search-bar">
      <div class="search-item">
        <span class="search-label">操作人</span>
        <el-input v-model="searchForm.operator" placeholder="请输入操作人" clearable style="width: 180px;" />
      </div>
      <div class="search-item">
        <span class="search-label">操作类型</span>
        <el-select v-model="searchForm.operType" placeholder="全部" clearable style="width: 140px;">
          <el-option label="新增" value="INSERT" />
          <el-option label="修改" value="UPDATE" />
          <el-option label="删除" value="DELETE" />
          <el-option label="导出" value="EXPORT" />
          <el-option label="其他" value="OTHER" />
        </el-select>
      </div>
      <div class="search-item">
        <span class="search-label">操作模块</span>
        <el-input v-model="searchForm.module" placeholder="请输入模块名" clearable style="width: 160px;" />
      </div>
      <div class="search-item">
        <span class="search-label">状态</span>
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px;">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
      </div>
      <div class="search-item">
        <span class="search-label">操作时间</span>
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

    <el-card>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="module" label="操作模块" min-width="120" />
        <el-table-column prop="operType" label="操作类型" width="100">
          <template #default="{ row }">
            <span :class="operTypeClass(row.operType)">{{ row.operType }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="operDesc" label="操作描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="operMethod" label="请求方法" min-width="200" show-overflow-tooltip />
        <el-table-column prop="operUrl" label="请求URL" min-width="200" show-overflow-tooltip />
        <el-table-column prop="operIp" label="操作IP" width="140" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <span :class="row.status === 1 ? 'tag-success' : 'tag-danger'">
              {{ row.status === 1 ? '成功' : '失败' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="90" align="center" />
        <el-table-column prop="createTime" label="操作时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page" v-model:page-size="pageSize" :total="total"
        :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchData" @current-change="fetchData"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="操作日志详情" width="650px" destroy-on-close>
      <div v-if="currentLog" class="detail-panel">
        <div class="detail-body">
          <div class="detail-row">
            <span class="detail-label">日志ID</span>
            <span class="detail-value">{{ currentLog.id }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">操作模块</span>
            <span class="detail-value">{{ currentLog.module }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">操作类型</span>
            <span class="detail-value">
              <span :class="operTypeClass(currentLog.operType)">{{ currentLog.operType }}</span>
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">操作描述</span>
            <span class="detail-value">{{ currentLog.operDesc }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">请求方法</span>
            <span class="detail-value">{{ currentLog.operMethod }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">请求URL</span>
            <span class="detail-value">{{ currentLog.operUrl }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">请求参数</span>
            <span class="detail-value">
              <pre class="code-preview">{{ currentLog.operParam || '-' }}</pre>
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">返回结果</span>
            <span class="detail-value">
              <pre class="code-preview">{{ currentLog.jsonResult || '-' }}</pre>
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">操作IP</span>
            <span class="detail-value">{{ currentLog.operIp }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">操作地点</span>
            <span class="detail-value">{{ currentLog.operLocation || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">操作人</span>
            <span class="detail-value">{{ currentLog.operator }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">状态</span>
            <span class="detail-value">
              <span :class="currentLog.status === 1 ? 'tag-success' : 'tag-danger'">
                {{ currentLog.status === 1 ? '成功' : '失败' }}
              </span>
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">耗时</span>
            <span class="detail-value">{{ currentLog.costTime }} ms</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">操作时间</span>
            <span class="detail-value">{{ currentLog.createTime }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'

definePageMeta({
  middleware: 'auth',
  meta: { title: '操作日志' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/monitor/operlog', title: '操作日志', icon: 'List' })
  fetchData()
})

const operTypeClass = (t) => {
  const map = { INSERT: 'tag-success', UPDATE: 'tag-warning', DELETE: 'tag-danger', EXPORT: 'tag-info', OTHER: '' }
  return map[t] || ''
}

const searchForm = reactive({ operator: '', operType: '', module: '', status: '', dateRange: [] })
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const loading = ref(false)

const handleSearch = () => { page.value = 1; fetchData() }
const handleReset = () => {
  searchForm.operator = ''; searchForm.operType = ''; searchForm.module = ''
  searchForm.status = ''; searchForm.dateRange = []
  handleSearch()
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (searchForm.operator) params.operator = searchForm.operator
    if (searchForm.operType) params.operType = searchForm.operType
    if (searchForm.module) params.module = searchForm.module
    if (searchForm.status !== '') params.status = searchForm.status
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startTime = searchForm.dateRange[0]
      params.endTime = searchForm.dateRange[1]
    }
    const { data } = await useFetch('/api/v1/admin/monitor/operlog', {
      params, headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      tableData.value = data.value.data.records || []
      total.value = data.value.data.total || 0
    }
  } catch (e) { /* fallback */ } finally { loading.value = false }
}

const detailVisible = ref(false)
const currentLog = ref(null)

const handleDetail = (row) => {
  currentLog.value = row
  detailVisible.value = true
}
</script>