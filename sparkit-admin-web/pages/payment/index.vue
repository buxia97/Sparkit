<template>
  <div>
    <div class="page-header">
      <div class="page-title">支付管理</div>
      <div class="page-desc">管理支付订单、渠道配置与支付统计</div>
    </div>

    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="支付订单" name="orders">
          <div class="search-bar">
            <div class="search-item">
              <span class="search-label">订单号</span>
              <el-input v-model="orderSearch.keyword" placeholder="请输入订单号" clearable style="width: 200px;" />
            </div>
            <div class="search-item">
              <span class="search-label">状态</span>
              <el-select v-model="orderSearch.status" placeholder="全部" clearable style="width: 140px;">
                <el-option label="待支付" :value="0" />
                <el-option label="已支付" :value="1" />
                <el-option label="已退款" :value="2" />
                <el-option label="已关闭" :value="3" />
              </el-select>
            </div>
            <div class="search-item">
              <span class="search-label">支付渠道</span>
              <el-select v-model="orderSearch.channelCode" placeholder="全部" clearable style="width: 140px;">
                <el-option label="微信支付" value="wechat" />
                <el-option label="支付宝" value="alipay" />
                <el-option label="PayPal" value="paypal" />
              </el-select>
            </div>
            <div class="search-item">
              <span class="search-label">时间</span>
              <el-date-picker
                v-model="orderSearch.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 260px;"
              />
            </div>
            <div class="search-actions">
              <el-button type="primary" @click="fetchOrders">搜索</el-button>
              <el-button @click="resetOrderSearch">重置</el-button>
            </div>
          </div>

          <el-table :data="orderData" v-loading="orderLoading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="orderNo" label="订单号" min-width="200" show-overflow-tooltip />
            <el-table-column prop="channelCode" label="支付渠道" width="100">
              <template #default="{ row }">
                <span class="tag-info">{{ channelLabel(row.channelCode) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="金额(元)" width="100" align="right">
              <template #default="{ row }">{{ (row.amount / 100).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <span :class="orderStatusClass(row.status)">{{ orderStatusLabel(row.status) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleOrderDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="orderPage" v-model:page-size="orderPageSize" :total="orderTotal"
            :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchOrders" @current-change="fetchOrders"
          />
        </el-tab-pane>

        <el-tab-pane label="支付渠道" name="channels">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-button type="primary" @click="handleChannelAdd">
                <el-icon><Plus /></el-icon>新增渠道
              </el-button>
            </div>
          </div>

          <el-table :data="channelData" v-loading="channelLoading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="channelCode" label="渠道编码" width="120" />
            <el-table-column prop="channelName" label="渠道名称" min-width="150" />
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
                <el-button type="primary" link size="small" @click="handleChannelEdit(row)">编辑</el-button>
                <el-button type="danger" link size="small" @click="handleChannelDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="退款管理" name="refunds">
          <div class="search-bar">
            <div class="search-item">
              <span class="search-label">退款单号</span>
              <el-input v-model="refundSearch.keyword" placeholder="请输入退款单号" clearable style="width: 200px;" />
            </div>
            <div class="search-item">
              <span class="search-label">状态</span>
              <el-select v-model="refundSearch.status" placeholder="全部" clearable style="width: 140px;">
                <el-option label="处理中" :value="0" />
                <el-option label="退款成功" :value="1" />
                <el-option label="退款失败" :value="2" />
              </el-select>
            </div>
            <div class="search-item">
              <span class="search-label">渠道</span>
              <el-select v-model="refundSearch.channelCode" placeholder="全部" clearable style="width: 140px;">
                <el-option label="微信支付" value="wechat" />
                <el-option label="支付宝" value="alipay" />
                <el-option label="PayPal" value="paypal" />
              </el-select>
            </div>
            <div class="search-actions">
              <el-button type="primary" @click="fetchRefunds">搜索</el-button>
              <el-button @click="resetRefundSearch">重置</el-button>
            </div>
          </div>

          <el-table :data="refundData" v-loading="refundLoading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="refundNo" label="退款单号" min-width="200" show-overflow-tooltip />
            <el-table-column prop="orderId" label="关联订单ID" width="100" />
            <el-table-column prop="refundAmount" label="退款金额(元)" width="120" align="right">
              <template #default="{ row }">{{ (row.refundAmount / 100).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="channelCode" label="渠道" width="100">
              <template #default="{ row }">
                <span class="tag-info">{{ channelLabel(row.channelCode) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <span :class="refundStatusClass(row.status)">{{ refundStatusLabel(row.status) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="refundReason" label="退款原因" min-width="150" show-overflow-tooltip />
            <el-table-column prop="refundTime" label="退款时间" width="170" />
          </el-table>
          <el-pagination
            v-model:current-page="refundPage" v-model:page-size="refundPageSize" :total="refundTotal"
            :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchRefunds" @current-change="fetchRefunds"
          />
        </el-tab-pane>

        <el-tab-pane label="支付统计" name="statistics">
          <div class="chart-card" style="margin-bottom: var(--spacing-md);">
            <div class="chart-header">
              <span class="chart-title">支付统计</span>
              <el-date-picker
                v-model="statDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                @change="fetchStatistics"
              />
            </div>
            <div class="chart-body">
              <div class="stat-cards">
                <div class="stat-card">
                  <div class="stat-value">{{ stats.totalAmount?.toFixed(2) || '0.00' }}</div>
                  <div class="stat-label">总交易额(元)</div>
                </div>
                <div class="stat-card">
                  <div class="stat-value">{{ stats.totalOrders || 0 }}</div>
                  <div class="stat-label">总订单数</div>
                </div>
                <div class="stat-card">
                  <div class="stat-value">{{ stats.successOrders || 0 }}</div>
                  <div class="stat-label">成功订单</div>
                </div>
                <div class="stat-card">
                  <div class="stat-value">{{ stats.successRate || '0%' }}</div>
                  <div class="stat-label">成功率</div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 渠道弹窗 -->
    <el-dialog v-model="channelDialogVisible" :title="channelDialogTitle" width="550px" destroy-on-close>
      <el-form ref="channelFormRef" :model="channelForm" :rules="channelFormRules" label-width="100px">
        <el-form-item label="渠道编码" prop="channelCode">
          <el-input v-model="channelForm.channelCode" placeholder="如：wechat" :disabled="!!channelForm.id" />
        </el-form-item>
        <el-form-item label="渠道名称" prop="channelName">
          <el-input v-model="channelForm.channelName" placeholder="如：微信支付" />
        </el-form-item>
        <el-form-item label="配置参数" prop="configJson">
          <el-input v-model="channelForm.configJson" type="textarea" :rows="5" placeholder="JSON格式配置" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="channelForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="channelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChannelSubmit" :loading="channelSubmitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="orderDetailVisible" title="订单详情" width="550px" destroy-on-close>
      <div v-if="currentOrder" class="detail-panel">
        <div class="detail-body">
          <div class="detail-row">
            <span class="detail-label">订单ID</span>
            <span class="detail-value">{{ currentOrder.id }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">订单号</span>
            <span class="detail-value">{{ currentOrder.orderNo }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">支付渠道</span>
            <span class="detail-value">{{ channelLabel(currentOrder.channelCode) }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">金额</span>
            <span class="detail-value">{{ (currentOrder.amount / 100).toFixed(2) }} 元</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">状态</span>
            <span class="detail-value">
              <span :class="orderStatusClass(currentOrder.status)">{{ orderStatusLabel(currentOrder.status) }}</span>
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">创建时间</span>
            <span class="detail-value">{{ currentOrder.createTime }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

definePageMeta({
  middleware: 'auth',
  meta: { title: '支付管理' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/payment', title: '支付管理', icon: 'Money' })
  fetchOrders()
  fetchChannels()
  fetchStatistics()
  fetchRefunds()
})

const channelLabel = (c) => ({ wechat: '微信支付', alipay: '支付宝', paypal: 'PayPal' }[c] || c)
const orderStatusLabel = (s) => ({ 0: '待支付', 1: '已支付', 2: '已退款', 3: '已关闭' }[s] || '未知')
const orderStatusClass = (s) => ({ 0: 'tag-warning', 1: 'tag-success', 2: 'tag-info', 3: 'tag-danger' }[s] || '')
const refundStatusLabel = (s) => ({ 0: '处理中', 1: '退款成功', 2: '退款失败' }[s] || '未知')
const refundStatusClass = (s) => ({ 0: 'tag-warning', 1: 'tag-success', 2: 'tag-danger' }[s] || '')

const activeTab = ref('orders')

// 订单
const orderSearch = reactive({ keyword: '', status: '', channelCode: '', dateRange: [] })
const orderPage = ref(1)
const orderPageSize = ref(10)
const orderTotal = ref(0)
const orderData = ref([])
const orderLoading = ref(false)

const resetOrderSearch = () => {
  orderSearch.keyword = ''; orderSearch.status = ''; orderSearch.channelCode = ''; orderSearch.dateRange = []
  orderPage.value = 1; fetchOrders()
}

const fetchOrders = async () => {
  orderLoading.value = true
  try {
    const params = { page: orderPage.value, pageSize: orderPageSize.value }
    if (orderSearch.keyword) params.keyword = orderSearch.keyword
    if (orderSearch.status !== '') params.status = orderSearch.status
    if (orderSearch.channelCode) params.channelCode = orderSearch.channelCode
    if (orderSearch.dateRange && orderSearch.dateRange.length === 2) {
      params.startTime = orderSearch.dateRange[0]
      params.endTime = orderSearch.dateRange[1]
    }
    const { data } = await useFetch('/api/v1/admin/payment/orders', {
      params, headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      orderData.value = data.value.data.records || []
      orderTotal.value = data.value.data.total || 0
    }
  } catch (e) { /* fallback */ } finally { orderLoading.value = false }
}

const orderDetailVisible = ref(false)
const currentOrder = ref(null)

const handleOrderDetail = (row) => {
  currentOrder.value = row
  orderDetailVisible.value = true
}

// 渠道
const channelData = ref([])
const channelLoading = ref(false)

const fetchChannels = async () => {
  channelLoading.value = true
  try {
    const { data } = await useFetch('/api/v1/admin/payment/channels', {
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) channelData.value = data.value.data || []
  } catch (e) { /* ignore */ } finally { channelLoading.value = false }
}

const channelDialogVisible = ref(false)
const channelDialogTitle = ref('新增渠道')
const channelFormRef = ref(null)
const channelSubmitting = ref(false)
const channelForm = reactive({ id: '', channelCode: '', channelName: '', configJson: '', status: 1 })
const channelFormRules = {
  channelCode: [{ required: true, message: '请输入渠道编码', trigger: 'blur' }],
  channelName: [{ required: true, message: '请输入渠道名称', trigger: 'blur' }]
}

const handleChannelAdd = () => {
  channelDialogTitle.value = '新增渠道'
  Object.assign(channelForm, { id: '', channelCode: '', channelName: '', configJson: '', status: 1 })
  channelFormRef.value?.resetFields()
  channelDialogVisible.value = true
}

const handleChannelEdit = (row) => {
  channelDialogTitle.value = '编辑渠道'
  Object.assign(channelForm, {
    id: row.id, channelCode: row.channelCode, channelName: row.channelName,
    configJson: row.configJson || '', status: row.status
  })
  channelFormRef.value?.resetFields()
  channelDialogVisible.value = true
}

const handleChannelSubmit = async () => {
  if (!channelFormRef.value) return
  await channelFormRef.value.validate(async (valid) => {
    if (!valid) return
    channelSubmitting.value = true
    try {
      const url = channelForm.id
        ? `/api/v1/admin/payment/channels/${channelForm.id}`
        : '/api/v1/admin/payment/channels'
      const method = channelForm.id ? 'PUT' : 'POST'
      const { data } = await useFetch(url, {
        method, body: channelForm,
        headers: { Authorization: `Bearer ${auth.token}` }
      })
      if (data.value?.code === 200) {
        ElMessage.success(channelForm.id ? '编辑成功' : '新增成功')
        channelDialogVisible.value = false
        fetchChannels()
      }
    } catch (e) { /* ignore */ } finally { channelSubmitting.value = false }
  })
}

const handleChannelDelete = (row) => {
  ElMessageBox.confirm(`确定删除渠道「${row.channelName}」？`, '删除确认', {
    type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
  }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/payment/channels/${row.id}`, {
      method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      ElMessage.success('删除成功')
      fetchChannels()
    }
  }).catch(() => {})
}

// 退款管理
const refundSearch = reactive({ keyword: '', status: '', channelCode: '' })
const refundPage = ref(1)
const refundPageSize = ref(10)
const refundTotal = ref(0)
const refundData = ref([])
const refundLoading = ref(false)

const resetRefundSearch = () => {
  refundSearch.keyword = ''; refundSearch.status = ''; refundSearch.channelCode = ''
  refundPage.value = 1; fetchRefunds()
}

const fetchRefunds = async () => {
  refundLoading.value = true
  try {
    const params = { page: refundPage.value, pageSize: refundPageSize.value }
    if (refundSearch.keyword) params.keyword = refundSearch.keyword
    if (refundSearch.status !== '') params.status = refundSearch.status
    if (refundSearch.channelCode) params.channelCode = refundSearch.channelCode
    const { data } = await useFetch('/api/v1/admin/payment/refunds', {
      params, headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      refundData.value = data.value.data.records || []
      refundTotal.value = data.value.data.total || 0
    }
  } catch (e) { /* ignore */ } finally { refundLoading.value = false }
}

// 统计
const statDateRange = ref([])
const stats = reactive({ totalAmount: 0, totalOrders: 0, successOrders: 0, successRate: '0%' })

const fetchStatistics = async () => {
  try {
    const params = {}
    if (statDateRange.value && statDateRange.value.length === 2) {
      params.startTime = statDateRange.value[0]
      params.endTime = statDateRange.value[1]
    }
    const { data } = await useFetch('/api/v1/admin/payment/statistics', {
      params, headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      Object.assign(stats, data.value.data || {})
    }
  } catch (e) { /* ignore */ }
}
</script>