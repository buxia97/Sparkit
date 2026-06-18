<template>
  <div>
    <div class="page-header">
      <div class="page-title">登录日志</div>
      <div class="page-desc">查看管理员登录日志，监控登录行为</div>
    </div>

    <div class="search-bar">
      <div class="search-item">
        <span class="search-label">用户名</span>
        <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable style="width: 180px;" />
      </div>
      <div class="search-item">
        <span class="search-label">登录状态</span>
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px;">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
      </div>
      <div class="search-item">
        <span class="search-label">登录IP</span>
        <el-input v-model="searchForm.ip" placeholder="请输入IP地址" clearable style="width: 160px;" />
      </div>
      <div class="search-item">
        <span class="search-label">登录时间</span>
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
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="loginType" label="登录方式" width="100">
          <template #default="{ row }">
            <span class="tag-info">{{ loginTypeLabel(row.loginType) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="登录IP" width="140" />
        <el-table-column prop="location" label="登录地点" min-width="150" show-overflow-tooltip />
        <el-table-column prop="browser" label="浏览器" min-width="150" show-overflow-tooltip />
        <el-table-column prop="os" label="操作系统" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <span :class="row.status === 1 ? 'tag-success' : 'tag-danger'">
              {{ row.status === 1 ? '成功' : '失败' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="msg" label="失败原因" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="登录时间" width="170" />
      </el-table>
      <el-pagination
        v-model:current-page="page" v-model:page-size="pageSize" :total="total"
        :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchData" @current-change="fetchData"
      />
    </el-card>
  </div>
</template>

<script setup>
definePageMeta({
  middleware: 'auth',
  meta: { title: '登录日志' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/monitor/loginlog', title: '登录日志', icon: 'UserFilled' })
  fetchData()
})

const loginTypeLabel = (t) => ({ password: '密码登录', sms: '短信登录', oauth: '第三方登录' }[t] || t)

const searchForm = reactive({ username: '', status: '', ip: '', dateRange: [] })
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const loading = ref(false)

const handleSearch = () => { page.value = 1; fetchData() }
const handleReset = () => {
  searchForm.username = ''; searchForm.status = ''; searchForm.ip = ''; searchForm.dateRange = []
  handleSearch()
}

const fetchData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (searchForm.username) params.username = searchForm.username
    if (searchForm.status !== '') params.status = searchForm.status
    if (searchForm.ip) params.ip = searchForm.ip
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startTime = searchForm.dateRange[0]
      params.endTime = searchForm.dateRange[1]
    }
    const { data } = await useFetch('/api/v1/admin/monitor/loginlog', {
      params, headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      tableData.value = data.value.data.records || []
      total.value = data.value.data.total || 0
    }
  } catch (e) { /* fallback */ } finally { loading.value = false }
}
</script>