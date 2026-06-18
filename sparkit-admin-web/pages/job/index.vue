<template>
  <div>
    <div class="page-header">
      <div class="page-title">定时任务</div>
      <div class="page-desc">管理定时任务，包括任务CRUD、暂停/恢复、执行日志与统计</div>
    </div>

    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="任务列表" name="jobs">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-button type="primary" @click="handleAdd">
                <el-icon><Plus /></el-icon>新增任务
              </el-button>
            </div>
          </div>

          <el-table :data="tableData" v-loading="loading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="jobName" label="任务名称" min-width="150" />
            <el-table-column prop="jobGroup" label="任务组" width="120" />
            <el-table-column prop="cronExpression" label="Cron表达式" width="150">
              <template #default="{ row }">
                <code class="cron-text">{{ row.cronExpression }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="beanName" label="执行类" width="180" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <span :class="row.status === 1 ? 'tag-success' : 'tag-warning'">
                  {{ row.status === 1 ? '运行中' : '已暂停' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="lastRunTime" label="上次执行" width="170" />
            <el-table-column label="操作" width="250" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
                <el-button
                  v-if="row.status === 1"
                  type="warning" link size="small"
                  @click="handlePause(row)"
                >暂停</el-button>
                <el-button
                  v-else
                  type="success" link size="small"
                  @click="handleResume(row)"
                >恢复</el-button>
                <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="page" v-model:page-size="pageSize" :total="total"
            :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchData" @current-change="fetchData"
          />
        </el-tab-pane>

        <el-tab-pane label="执行日志" name="logs">
          <div class="search-bar">
            <div class="search-item">
              <span class="search-label">任务名称</span>
              <el-input v-model="logSearch.jobName" placeholder="请输入任务名称" clearable style="width: 180px;" />
            </div>
            <div class="search-item">
              <span class="search-label">执行状态</span>
              <el-select v-model="logSearch.status" placeholder="全部" clearable style="width: 120px;">
                <el-option label="成功" :value="1" />
                <el-option label="失败" :value="0" />
              </el-select>
            </div>
            <div class="search-item">
              <span class="search-label">时间</span>
              <el-date-picker
                v-model="logSearch.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 260px;"
              />
            </div>
            <div class="search-actions">
              <el-button type="primary" @click="fetchLogs">搜索</el-button>
              <el-button @click="resetLogSearch">重置</el-button>
            </div>
          </div>

          <el-table :data="logData" v-loading="logLoading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="jobName" label="任务名称" min-width="150" />
            <el-table-column prop="status" label="执行状态" width="90">
              <template #default="{ row }">
                <span :class="row.status === 1 ? 'tag-success' : 'tag-danger'">
                  {{ row.status === 1 ? '成功' : '失败' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="duration" label="耗时(ms)" width="100" align="center" />
            <el-table-column prop="errorMsg" label="错误信息" min-width="200" show-overflow-tooltip />
            <el-table-column prop="createTime" label="执行时间" width="170" />
          </el-table>
          <el-pagination
            v-model:current-page="logPage" v-model:page-size="logPageSize" :total="logTotal"
            :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchLogs" @current-change="fetchLogs"
          />
        </el-tab-pane>

        <el-tab-pane label="成功率统计" name="statistics">
          <div class="chart-card" style="margin-bottom: var(--spacing-md);">
            <div class="chart-header">
              <span class="chart-title">任务成功率统计</span>
              <el-date-picker
                v-model="jobStatDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                @change="fetchJobStatistics"
              />
            </div>
            <div class="chart-body">
              <div ref="jobStatChartRef" style="width: 100%; height: 350px;"></div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="任务名称" prop="jobName">
          <el-input v-model="formData.jobName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="任务组" prop="jobGroup">
          <el-input v-model="formData.jobGroup" placeholder="请输入任务组" />
        </el-form-item>
        <el-form-item label="Cron表达式" prop="cronExpression">
          <el-input v-model="formData.cronExpression" placeholder="如：0 0/5 * * * ?">
            <template #append>
              <el-button @click="cronVisible = true">预览</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="执行类" prop="beanName">
          <el-input v-model="formData.beanName" placeholder="请输入Spring Bean名称" />
        </el-form-item>
        <el-form-item label="方法名" prop="methodName">
          <el-input v-model="formData.methodName" placeholder="请输入执行方法名" />
        </el-form-item>
        <el-form-item label="参数" prop="methodParams">
          <el-input v-model="formData.methodParams" placeholder="方法参数（JSON格式，可选）" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="任务描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- Cron预览弹窗 -->
    <el-dialog v-model="cronVisible" title="Cron表达式预览" width="500px">
      <div class="cron-preview">{{ cronPreviewText }}</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

definePageMeta({
  middleware: 'auth',
  meta: { title: '定时任务' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/job', title: '定时任务', icon: 'Clock' })
  fetchData()
  fetchLogs()
})

const activeTab = ref('jobs')

// 任务
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const loading = ref(false)

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await useFetch('/api/v1/admin/job', {
      params: { page: page.value, pageSize: pageSize.value },
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      tableData.value = data.value.data.records || []
      total.value = data.value.data.total || 0
    }
  } catch (e) { /* fallback */ } finally { loading.value = false }
}

const dialogVisible = ref(false)
const dialogTitle = ref('新增任务')
const formRef = ref(null)
const submitting = ref(false)
const formData = reactive({
  id: '', jobName: '', jobGroup: 'DEFAULT', cronExpression: '', beanName: '',
  methodName: '', methodParams: '', description: ''
})
const formRules = {
  jobName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  jobGroup: [{ required: true, message: '请输入任务组', trigger: 'blur' }],
  cronExpression: [{ required: true, message: '请输入Cron表达式', trigger: 'blur' }],
  beanName: [{ required: true, message: '请输入执行类', trigger: 'blur' }],
  methodName: [{ required: true, message: '请输入方法名', trigger: 'blur' }]
}

const handleAdd = () => {
  dialogTitle.value = '新增任务'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑任务'
  resetForm()
  Object.assign(formData, {
    id: row.id, jobName: row.jobName, jobGroup: row.jobGroup,
    cronExpression: row.cronExpression, beanName: row.beanName,
    methodName: row.methodName || '', methodParams: row.methodParams || '',
    description: row.description || ''
  })
  dialogVisible.value = true
}

const resetForm = () => {
  Object.assign(formData, { id: '', jobName: '', jobGroup: 'DEFAULT', cronExpression: '', beanName: '', methodName: '', methodParams: '', description: '' })
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const url = formData.id ? `/api/v1/admin/job/${formData.id}` : '/api/v1/admin/job'
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

const handlePause = async (row) => {
  const { data } = await useFetch(`/api/v1/admin/job/${row.id}/pause`, {
    method: 'PUT', headers: { Authorization: `Bearer ${auth.token}` }
  })
  if (data.value?.code === 200) {
    ElMessage.success('已暂停')
    fetchData()
  }
}

const handleResume = async (row) => {
  const { data } = await useFetch(`/api/v1/admin/job/${row.id}/resume`, {
    method: 'PUT', headers: { Authorization: `Bearer ${auth.token}` }
  })
  if (data.value?.code === 200) {
    ElMessage.success('已恢复')
    fetchData()
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除任务「${row.jobName}」？`, '删除确认', {
    type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
  }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/job/${row.id}`, {
      method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    }
  }).catch(() => {})
}

// 日志
const logSearch = reactive({ jobName: '', status: '', dateRange: [] })
const logPage = ref(1)
const logPageSize = ref(10)
const logTotal = ref(0)
const logData = ref([])
const logLoading = ref(false)

const resetLogSearch = () => {
  logSearch.jobName = ''; logSearch.status = ''; logSearch.dateRange = []
  logPage.value = 1; fetchLogs()
}

const fetchLogs = async () => {
  logLoading.value = true
  try {
    const params = { page: logPage.value, pageSize: logPageSize.value }
    if (logSearch.jobName) params.jobName = logSearch.jobName
    if (logSearch.status !== '') params.status = logSearch.status
    if (logSearch.dateRange && logSearch.dateRange.length === 2) {
      params.startTime = logSearch.dateRange[0]
      params.endTime = logSearch.dateRange[1]
    }
    const { data } = await useFetch('/api/v1/admin/job/logs', {
      params, headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      logData.value = data.value.data.records || []
      logTotal.value = data.value.data.total || 0
    }
  } catch (e) { /* fallback */ } finally { logLoading.value = false }
}

// 统计
const jobStatDateRange = ref([])
const jobStatChartRef = ref(null)
let jobStatChart = null
const cronVisible = ref(false)
const cronPreviewText = computed(() => {
  try {
    return `表达式: ${formData.cronExpression || '未输入'}\n\n请使用在线Cron工具验证表达式是否正确。`
  } catch (e) { return '表达式格式错误' }
})

const fetchJobStatistics = async () => {
  try {
    const params = {}
    if (jobStatDateRange.value && jobStatDateRange.value.length === 2) {
      params.startTime = jobStatDateRange.value[0]
      params.endTime = jobStatDateRange.value[1]
    }
    const { data } = await useFetch('/api/v1/admin/job/statistics/count', {
      params, headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200 && jobStatChartRef.value) {
      const stats = data.value.data || []
      if (!jobStatChart) {
        jobStatChart = echarts.init(jobStatChartRef.value)
      }
      jobStatChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['成功', '失败'] },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: stats.map(s => s.jobName) },
        yAxis: { type: 'value', name: '执行次数' },
        series: [
          {
            name: '成功', type: 'bar', stack: 'total',
            data: stats.map(s => s.successCount || 0),
            itemStyle: { color: '#00C853', borderRadius: [0, 0, 0, 0] }
          },
          {
            name: '失败', type: 'bar', stack: 'total',
            data: stats.map(s => s.failCount || 0),
            itemStyle: { color: '#ef4444', borderRadius: [6, 6, 0, 0] }
          }
        ]
      })
    }
  } catch (e) { /* ignore */ }
}

watch(activeTab, (val) => {
  if (val === 'statistics') {
    nextTick(() => { fetchJobStatistics() })
  }
})
</script>