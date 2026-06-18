<template>
  <div>
    <div class="page-header">
      <div class="page-title">AI 管理</div>
      <div class="page-desc">管理AI模型、查看生成记录与统计</div>
    </div>

    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="模型管理" name="models">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-button type="primary" @click="handleModelAdd">
                <el-icon><Plus /></el-icon>新增模型
              </el-button>
            </div>
          </div>

          <el-table :data="modelData" v-loading="modelLoading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="modelName" label="模型名称" min-width="150" />
            <el-table-column prop="modelCode" label="模型编码" width="150" />
            <el-table-column prop="provider" label="提供商" width="110">
              <template #default="{ row }">
                <span class="tag-info">{{ providerMap[row.provider] || row.provider }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="modelType" label="类型" width="100">
              <template #default="{ row }">
                <span class="tag-info">{{ row.modelType || 'text' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <span :class="row.status === 1 ? 'tag-success' : 'tag-danger'">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="sort" label="排序" width="80" align="center" />
            <el-table-column prop="createTime" label="创建时间" width="170" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleModelEdit(row)">编辑</el-button>
                <el-button type="danger" link size="small" @click="handleModelDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="生成记录" name="generations">
          <div class="search-bar">
            <div class="search-item">
              <span class="search-label">模型</span>
              <el-select v-model="genSearch.modelId" placeholder="全部" clearable style="width: 180px;">
                <el-option v-for="m in modelData" :key="m.id" :label="m.modelName" :value="m.id" />
              </el-select>
            </div>
            <div class="search-item">
              <span class="search-label">时间</span>
              <el-date-picker
                v-model="genSearch.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 260px;"
              />
            </div>
            <div class="search-actions">
              <el-button type="primary" @click="fetchGenerations">搜索</el-button>
              <el-button @click="resetGenSearch">重置</el-button>
            </div>
          </div>

          <el-table :data="genData" v-loading="genLoading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="modelType" label="模型" width="120" />
            <el-table-column prop="prompt" label="输入提示" min-width="200" show-overflow-tooltip />
            <el-table-column prop="tokensUsed" label="消耗Token" width="100" align="center" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <span :class="row.status === 1 ? 'tag-success' : 'tag-danger'">
                  {{ row.status === 1 ? '成功' : '失败' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="生成时间" width="170" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleGenDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="genPage" v-model:page-size="genPageSize" :total="genTotal"
            :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchGenerations" @current-change="fetchGenerations"
          />
        </el-tab-pane>

        <el-tab-pane label="模型统计" name="statistics">
          <div class="chart-card" style="margin-bottom: var(--spacing-md);">
            <div class="chart-header">
              <span class="chart-title">按模型统计</span>
              <el-date-picker
                v-model="aiStatDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                @change="fetchAiStatistics"
              />
            </div>
            <div class="chart-body">
              <div ref="aiStatChartRef" style="width: 100%; height: 350px;"></div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 模型弹窗 -->
    <el-dialog v-model="modelDialogVisible" :title="modelDialogTitle" width="650px" destroy-on-close>
      <el-form ref="modelFormRef" :model="modelForm" :rules="modelFormRules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模型名称" prop="modelName">
              <el-input v-model="modelForm.modelName" placeholder="例如: deepseek-chat" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型编码" prop="modelCode">
              <el-input v-model="modelForm.modelCode" placeholder="唯一编码" :disabled="!!modelForm.id" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="提供商" prop="provider">
              <el-select v-model="modelForm.provider" placeholder="请选择提供商" style="width: 100%;">
                <el-option label="DeepSeek" value="deepseek" />
                <el-option label="阿里百炼" value="aliyun-bailian" />
                <el-option label="小米大模型" value="xiaomi" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型类型" prop="modelType">
              <el-select v-model="modelForm.modelType" placeholder="请选择类型" style="width: 100%;">
                <el-option label="文本(text)" value="text" />
                <el-option label="视觉(vision)" value="vision" />
                <el-option label="音频(audio)" value="audio" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="API Base URL" prop="apiBase">
          <el-input v-model="modelForm.apiBase" placeholder="例如: https://api.deepseek.com" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="API Key" prop="apiKey">
              <el-input v-model="modelForm.apiKey" type="password" placeholder="请输入API Key" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="API Secret" prop="apiSecret">
              <el-input v-model="modelForm.apiSecret" type="password" placeholder="可选" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模型版本" prop="modelVersion">
              <el-input v-model="modelForm.modelVersion" placeholder="例如: v1" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大Token" prop="maxTokens">
              <el-input-number v-model="modelForm.maxTokens" :min="1" :max="131072" placeholder="默认" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="Temperature" prop="temperature">
              <el-input-number v-model="modelForm.temperature" :min="0" :max="2" :step="0.1" :precision="1" placeholder="默认" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="modelForm.sort" :min="0" :max="999" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="modelForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleModelSubmit" :loading="modelSubmitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 生成记录详情弹窗 -->
    <el-dialog v-model="genDetailVisible" title="生成记录详情" width="650px" destroy-on-close>
      <div v-if="currentGen" class="detail-panel">
        <div class="detail-body">
          <div class="detail-row">
            <span class="detail-label">记录ID</span>
            <span class="detail-value">{{ currentGen.id }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">模型</span>
            <span class="detail-value">{{ currentGen.modelType }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">消耗Token</span>
            <span class="detail-value">{{ currentGen.tokensUsed }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">状态</span>
            <span class="detail-value">
              <span :class="currentGen.status === 1 ? 'tag-success' : 'tag-danger'">
                {{ currentGen.status === 1 ? '成功' : '失败' }}
              </span>
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">输入提示</span>
            <span class="detail-value">{{ currentGen.prompt }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">生成结果</span>
            <span class="detail-value">{{ currentGen.response || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">生成时间</span>
            <span class="detail-value">{{ currentGen.createTime }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

definePageMeta({
  middleware: 'auth',
  meta: { title: 'AI 管理' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/ai', title: 'AI 管理', icon: 'Cpu' })
  fetchModels()
  fetchGenerations()
})

const activeTab = ref('models')

const providerMap = {
  'deepseek': 'DeepSeek',
  'aliyun-bailian': '阿里百炼',
  'xiaomi': '小米大模型'
}

// 模型
const modelData = ref([])
const modelLoading = ref(false)

const fetchModels = async () => {
  modelLoading.value = true
  try {
    const { data } = await useFetch('/api/v1/admin/ai/models', {
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) modelData.value = data.value.data || []
  } catch (e) { /* ignore */ } finally { modelLoading.value = false }
}

const modelDialogVisible = ref(false)
const modelDialogTitle = ref('新增模型')
const modelFormRef = ref(null)
const modelSubmitting = ref(false)
const modelForm = reactive({
  id: '', modelName: '', modelCode: '', provider: '', modelType: 'text',
  apiBase: '', apiKey: '', apiSecret: '', modelVersion: '',
  maxTokens: null, temperature: null, sort: 0, status: 1
})
const modelFormRules = {
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  modelCode: [{ required: true, message: '请输入模型编码', trigger: 'blur' }],
  provider: [{ required: true, message: '请选择提供商', trigger: 'change' }]
}

const defaultModelForm = {
  id: '', modelName: '', modelCode: '', provider: '', modelType: 'text',
  apiBase: '', apiKey: '', apiSecret: '', modelVersion: '',
  maxTokens: null, temperature: null, sort: 0, status: 1
}

const handleModelAdd = () => {
  modelDialogTitle.value = '新增模型'
  Object.assign(modelForm, { ...defaultModelForm })
  modelFormRef.value?.resetFields()
  modelDialogVisible.value = true
}

const handleModelEdit = (row) => {
  modelDialogTitle.value = '编辑模型'
  Object.assign(modelForm, {
    id: row.id, modelName: row.modelName || '', modelCode: row.modelCode || '',
    provider: row.provider || '', modelType: row.modelType || 'text',
    apiBase: row.apiBase || '', apiKey: row.apiKey || '', apiSecret: row.apiSecret || '',
    modelVersion: row.modelVersion || '', maxTokens: row.maxTokens || null,
    temperature: row.temperature || null, sort: row.sort || 0, status: row.status
  })
  modelFormRef.value?.resetFields()
  modelDialogVisible.value = true
}

const handleModelSubmit = async () => {
  if (!modelFormRef.value) return
  await modelFormRef.value.validate(async (valid) => {
    if (!valid) return
    modelSubmitting.value = true
    try {
      const url = modelForm.id ? `/api/v1/admin/ai/models/${modelForm.id}` : '/api/v1/admin/ai/models'
      const method = modelForm.id ? 'PUT' : 'POST'
      const { data } = await useFetch(url, {
        method, body: modelForm,
        headers: { Authorization: `Bearer ${auth.token}` }
      })
      if (data.value?.code === 200) {
        ElMessage.success(modelForm.id ? '编辑成功' : '新增成功')
        modelDialogVisible.value = false
        fetchModels()
      }
    } catch (e) { /* ignore */ } finally { modelSubmitting.value = false }
  })
}

const handleModelDelete = (row) => {
  ElMessageBox.confirm(`确定删除模型「${row.modelName}」？`, '删除确认', {
    type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
  }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/ai/models/${row.id}`, {
      method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      ElMessage.success('删除成功')
      fetchModels()
    }
  }).catch(() => {})
}

// 生成记录
const genSearch = reactive({ modelId: '', dateRange: [] })
const genPage = ref(1)
const genPageSize = ref(10)
const genTotal = ref(0)
const genData = ref([])
const genLoading = ref(false)

const resetGenSearch = () => { genSearch.modelId = ''; genSearch.dateRange = []; genPage.value = 1; fetchGenerations() }

const fetchGenerations = async () => {
  genLoading.value = true
  try {
    const params = { page: genPage.value, pageSize: genPageSize.value }
    if (genSearch.modelId) params.modelId = genSearch.modelId
    if (genSearch.dateRange && genSearch.dateRange.length === 2) {
      params.startTime = genSearch.dateRange[0]
      params.endTime = genSearch.dateRange[1]
    }
    const { data } = await useFetch('/api/v1/admin/ai/generations', {
      params, headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      genData.value = data.value.data.records || []
      genTotal.value = data.value.data.total || 0
    }
  } catch (e) { /* fallback */ } finally { genLoading.value = false }
}

const genDetailVisible = ref(false)
const currentGen = ref(null)

const handleGenDetail = (row) => {
  currentGen.value = row
  genDetailVisible.value = true
}

// 统计
const aiStatDateRange = ref([])
const aiStatChartRef = ref(null)
let aiStatChart = null

const fetchAiStatistics = async () => {
  try {
    const params = {}
    if (aiStatDateRange.value && aiStatDateRange.value.length === 2) {
      params.startTime = aiStatDateRange.value[0]
      params.endTime = aiStatDateRange.value[1]
    }
    const { data } = await useFetch('/api/v1/admin/ai/statistics/model-count', {
      params, headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200 && aiStatChartRef.value) {
      const stats = data.value.data || []
      if (!aiStatChart) {
        aiStatChart = echarts.init(aiStatChartRef.value)
      }
      aiStatChart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: stats.map(s => s.modelName || s.modelCode), axisLabel: { rotate: 30 } },
        yAxis: { type: 'value', name: '调用次数' },
        series: [{
          name: '调用次数',
          type: 'bar',
          data: stats.map(s => s.count),
          itemStyle: {
            color: {
              type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: '#00C853' },
                { offset: 1, color: '#2196F3' }
              ]
            },
            borderRadius: [6, 6, 0, 0]
          }
        }]
      })
    }
  } catch (e) { /* ignore */ }
}

watch(activeTab, (val) => {
  if (val === 'statistics') {
    nextTick(() => { fetchAiStatistics() })
  }
})
</script>