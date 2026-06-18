<template>
  <div>
    <div class="page-header">
      <div class="page-title">通知管理</div>
      <div class="page-desc">管理通知模板与发送记录，支持多渠道通知</div>
    </div>

    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="通知模板" name="templates">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-button type="primary" @click="handleTemplateAdd">
                <el-icon><Plus /></el-icon>新增模板
              </el-button>
            </div>
          </div>

          <el-table :data="templateData" v-loading="templateLoading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="templateName" label="模板名称" min-width="150" />
            <el-table-column prop="templateCode" label="模板编码" width="150" />
            <el-table-column prop="channel" label="通知渠道" width="100">
              <template #default="{ row }">
                <span class="tag-info">{{ notifyChannelLabel(row.channel) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <span :class="row.status === 1 ? 'tag-success' : 'tag-danger'">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleTemplateEdit(row)">编辑</el-button>
                <el-button type="danger" link size="small" @click="handleTemplateDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="发送记录" name="records">
          <div class="search-bar">
            <div class="search-item">
              <span class="search-label">通知渠道</span>
              <el-select v-model="recordSearch.channel" placeholder="全部" clearable style="width: 140px;">
                <el-option label="邮件" value="email" />
                <el-option label="短信" value="sms" />
                <el-option label="公众号" value="wechat_mp" />
                <el-option label="UniPush" value="unipush" />
              </el-select>
            </div>
            <div class="search-item">
              <span class="search-label">时间</span>
              <el-date-picker
                v-model="recordSearch.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 260px;"
              />
            </div>
            <div class="search-actions">
              <el-button type="primary" @click="fetchRecords">搜索</el-button>
              <el-button @click="resetRecordSearch">重置</el-button>
            </div>
          </div>

          <el-table :data="recordData" v-loading="recordLoading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="templateName" label="模板名称" min-width="150" />
            <el-table-column prop="channel" label="渠道" width="100">
              <template #default="{ row }">
                <span class="tag-info">{{ notifyChannelLabel(row.channel) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="target" label="发送目标" min-width="180" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <span :class="row.status === 1 ? 'tag-success' : 'tag-danger'">
                  {{ row.status === 1 ? '成功' : '失败' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="发送时间" width="170" />
          </el-table>
          <el-pagination
            v-model:current-page="recordPage" v-model:page-size="recordPageSize" :total="recordTotal"
            :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchRecords" @current-change="fetchRecords"
          />
        </el-tab-pane>

        <el-tab-pane label="通知统计" name="statistics">
          <div class="chart-card" style="margin-bottom: var(--spacing-md);">
            <div class="chart-header">
              <span class="chart-title">按渠道统计</span>
              <el-date-picker
                v-model="notifyStatDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                @change="fetchNotifyStatistics"
              />
            </div>
            <div class="chart-body">
              <div ref="notifyChartRef" style="width: 100%; height: 320px;"></div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 模板弹窗 -->
    <el-dialog v-model="templateDialogVisible" :title="templateDialogTitle" width="600px" destroy-on-close>
      <el-form ref="templateFormRef" :model="templateForm" :rules="templateFormRules" label-width="100px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="templateForm.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板编码" prop="templateCode">
          <el-input v-model="templateForm.templateCode" placeholder="请输入模板编码" :disabled="!!templateForm.id" />
        </el-form-item>
        <el-form-item label="通知渠道" prop="channel">
          <el-select v-model="templateForm.channel" placeholder="请选择通知渠道" style="width: 100%;">
            <el-option label="邮件" value="email" />
            <el-option label="短信" value="sms" />
            <el-option label="微信公众号" value="wechat_mp" />
            <el-option label="UniPush" value="unipush" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板标题" prop="title">
          <el-input v-model="templateForm.title" placeholder="请输入模板标题" />
        </el-form-item>
        <el-form-item label="模板内容" prop="content">
          <el-input v-model="templateForm.content" type="textarea" :rows="5" placeholder="请输入模板内容（支持变量）" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="templateForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTemplateSubmit" :loading="templateSubmitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

definePageMeta({
  middleware: 'auth',
  meta: { title: '通知管理' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/notification', title: '通知管理', icon: 'Bell' })
  fetchTemplates()
  fetchRecords()
})

const notifyChannelLabel = (c) => ({ email: '邮件', sms: '短信', wechat_mp: '公众号', unipush: 'UniPush' }[c] || c)

const activeTab = ref('templates')

// 模板
const templateData = ref([])
const templateLoading = ref(false)

const fetchTemplates = async () => {
  templateLoading.value = true
  try {
    const { data } = await useFetch('/api/v1/admin/notification/templates', {
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) templateData.value = data.value.data || []
  } catch (e) { /* ignore */ } finally { templateLoading.value = false }
}

const templateDialogVisible = ref(false)
const templateDialogTitle = ref('新增模板')
const templateFormRef = ref(null)
const templateSubmitting = ref(false)
const templateForm = reactive({ id: '', templateName: '', templateCode: '', channel: '', title: '', content: '', status: 1 })
const templateFormRules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  channel: [{ required: true, message: '请选择通知渠道', trigger: 'change' }],
  title: [{ required: true, message: '请输入模板标题', trigger: 'blur' }]
}

const handleTemplateAdd = () => {
  templateDialogTitle.value = '新增模板'
  Object.assign(templateForm, { id: '', templateName: '', templateCode: '', channel: '', title: '', content: '', status: 1 })
  templateFormRef.value?.resetFields()
  templateDialogVisible.value = true
}

const handleTemplateEdit = (row) => {
  templateDialogTitle.value = '编辑模板'
  Object.assign(templateForm, {
    id: row.id, templateName: row.templateName, templateCode: row.templateCode,
    channel: row.channel, title: row.title || '', content: row.content || '', status: row.status
  })
  templateFormRef.value?.resetFields()
  templateDialogVisible.value = true
}

const handleTemplateSubmit = async () => {
  if (!templateFormRef.value) return
  await templateFormRef.value.validate(async (valid) => {
    if (!valid) return
    templateSubmitting.value = true
    try {
      const url = templateForm.id
        ? `/api/v1/admin/notification/templates/${templateForm.id}`
        : '/api/v1/admin/notification/templates'
      const method = templateForm.id ? 'PUT' : 'POST'
      const { data } = await useFetch(url, {
        method, body: templateForm,
        headers: { Authorization: `Bearer ${auth.token}` }
      })
      if (data.value?.code === 200) {
        ElMessage.success(templateForm.id ? '编辑成功' : '新增成功')
        templateDialogVisible.value = false
        fetchTemplates()
      }
    } catch (e) { /* ignore */ } finally { templateSubmitting.value = false }
  })
}

const handleTemplateDelete = (row) => {
  ElMessageBox.confirm(`确定删除模板「${row.templateName}」？`, '删除确认', {
    type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
  }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/notification/templates/${row.id}`, {
      method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      ElMessage.success('删除成功')
      fetchTemplates()
    }
  }).catch(() => {})
}

// 记录
const recordSearch = reactive({ channel: '', dateRange: [] })
const recordPage = ref(1)
const recordPageSize = ref(10)
const recordTotal = ref(0)
const recordData = ref([])
const recordLoading = ref(false)

const resetRecordSearch = () => {
  recordSearch.channel = ''; recordSearch.dateRange = []
  recordPage.value = 1; fetchRecords()
}

const fetchRecords = async () => {
  recordLoading.value = true
  try {
    const params = { page: recordPage.value, pageSize: recordPageSize.value }
    if (recordSearch.channel) params.channel = recordSearch.channel
    if (recordSearch.dateRange && recordSearch.dateRange.length === 2) {
      params.startTime = recordSearch.dateRange[0]
      params.endTime = recordSearch.dateRange[1]
    }
    const { data } = await useFetch('/api/v1/admin/notification/records', {
      params, headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      recordData.value = data.value.data.records || []
      recordTotal.value = data.value.data.total || 0
    }
  } catch (e) { /* fallback */ } finally { recordLoading.value = false }
}

// 统计
const notifyStatDateRange = ref([])
const notifyChartRef = ref(null)
let notifyChart = null

const fetchNotifyStatistics = async () => {
  try {
    const params = {}
    if (notifyStatDateRange.value && notifyStatDateRange.value.length === 2) {
      params.startTime = notifyStatDateRange.value[0]
      params.endTime = notifyStatDateRange.value[1]
    }
    const { data } = await useFetch('/api/v1/admin/notification/statistics/type-count', {
      params, headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200 && notifyChartRef.value) {
      const stats = data.value.data || []
      if (!notifyChart) {
        notifyChart = echarts.init(notifyChartRef.value)
      }
      notifyChart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: '0%' },
        series: [{
          name: '通知统计',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: { show: true, formatter: '{b}: {c}' },
          data: stats.map(s => ({ name: notifyChannelLabel(s.channel) || s.channel, value: s.count }))
        }]
      })
    }
  } catch (e) { /* ignore */ }
}

watch(activeTab, (val) => {
  if (val === 'statistics') {
    nextTick(() => { fetchNotifyStatistics() })
  }
})
</script>