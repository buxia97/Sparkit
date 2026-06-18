<template>
  <div>
    <div class="page-header">
      <div class="page-title">用户管理</div>
      <div class="page-desc">管理C端用户，包括查看、编辑、启用/禁用、等级管理、黑名单、实名认证等操作</div>
    </div>

    <div class="search-bar">
      <div class="search-item">
        <span class="search-label">用户名</span>
        <el-input v-model="searchForm.keyword" placeholder="请输入用户名" clearable style="width: 180px;" />
      </div>
      <div class="search-item">
        <span class="search-label">手机号</span>
        <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable style="width: 160px;" />
      </div>
      <div class="search-item">
        <span class="search-label">状态</span>
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px;">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </div>
      <div class="search-item">
        <span class="search-label">注册时间</span>
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
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column prop="level" label="等级" width="70" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.level" size="small" type="warning">Lv.{{ row.level }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="realNameStatus" label="实名" width="80">
          <template #default="{ row }">
            <span :class="realNameClass(row.realNameStatus)">{{ realNameLabel(row.realNameStatus) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="isBlacklisted" label="黑名单" width="80">
          <template #default="{ row }">
            <span v-if="row.isBlacklisted === 1" class="tag-danger">已拉黑</span>
            <span v-else class="tag-success">正常</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <span :class="row.status === 1 ? 'tag-success' : 'tag-danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="170" />
        <el-table-column prop="createTime" label="注册时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">详情</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              :type="row.status === 1 ? 'warning' : 'success'"
              link
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="650px" destroy-on-close>
      <div v-if="currentUser" class="detail-panel">
        <el-tabs v-model="detailTab" v-if="currentUser.id">
          <el-tab-pane label="基本信息" name="info">
            <div class="detail-body">
              <div class="detail-row">
                <span class="detail-label">用户ID</span>
                <span class="detail-value">{{ currentUser.id }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">用户名</span>
                <span class="detail-value">{{ currentUser.username }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">昵称</span>
                <span class="detail-value">{{ currentUser.nickname }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">手机号</span>
                <span class="detail-value">{{ currentUser.phone || '-' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">邮箱</span>
                <span class="detail-value">{{ currentUser.email || '-' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">性别</span>
                <span class="detail-value">{{ currentUser.gender === 1 ? '男' : currentUser.gender === 2 ? '女' : '未知' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">等级</span>
                <span class="detail-value">Lv.{{ currentUser.level || 0 }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">实名状态</span>
                <span class="detail-value">
                  <span :class="realNameClass(currentUser.realNameStatus)">{{ realNameLabel(currentUser.realNameStatus) }}</span>
                </span>
              </div>
              <div class="detail-row">
                <span class="detail-label">实名姓名</span>
                <span class="detail-value">{{ currentUser.realName || '-' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">黑名单</span>
                <span class="detail-value">
                  <span :class="currentUser.isBlacklisted === 1 ? 'tag-danger' : 'tag-success'">
                    {{ currentUser.isBlacklisted === 1 ? '已拉黑' : '正常' }}
                  </span>
                </span>
              </div>
              <div class="detail-row">
                <span class="detail-label">状态</span>
                <span class="detail-value">
                  <span :class="currentUser.status === 1 ? 'tag-success' : 'tag-danger'">
                    {{ currentUser.status === 1 ? '正常' : '禁用' }}
                  </span>
                </span>
              </div>
              <div class="detail-row">
                <span class="detail-label">注册来源</span>
                <span class="detail-value">{{ currentUser.registerSource || '-' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">最后登录</span>
                <span class="detail-value">{{ currentUser.lastLoginTime || '-' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">注册时间</span>
                <span class="detail-value">{{ currentUser.createTime }}</span>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="社交绑定" name="social">
            <div class="toolbar" style="margin-bottom: 12px;">
              <div class="toolbar-left">
                <el-button type="primary" size="small" @click="refreshSocialList">刷新</el-button>
              </div>
            </div>
            <el-table :data="socialData" v-loading="socialLoading" stripe size="small">
              <el-table-column prop="platform" label="平台" width="120">
                <template #default="{ row }">
                  <span class="tag-info">{{ socialPlatformLabel(row.platform) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="nickname" label="昵称" min-width="120" />
              <el-table-column prop="openid" label="OpenID" min-width="180" show-overflow-tooltip />
              <el-table-column prop="bindTime" label="绑定时间" width="170" />
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button type="danger" link size="small" @click="handleUnbindSocial(row)">解绑</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="socialData.length === 0 && !socialLoading" class="empty-state" style="padding: 20px;">
              <div class="empty-text">暂无社交绑定</div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑用户" width="500px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" label-width="100px">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="等级">
          <el-input-number v-model="editForm.level" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="黑名单">
          <el-radio-group v-model="editForm.isBlacklisted">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">拉黑</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="实名状态">
          <el-select v-model="editForm.realNameStatus" style="width: 100%;">
            <el-option label="未认证" :value="0" />
            <el-option label="已认证" :value="1" />
            <el-option label="审核中" :value="2" />
            <el-option label="认证失败" :value="3" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit" :loading="editSubmitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'

definePageMeta({
  middleware: 'auth',
  meta: { title: '用户管理' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/user', title: '用户管理', icon: 'UserFilled' })
  fetchData()
})

const realNameLabel = (s) => ({ 0: '未认证', 1: '已认证', 2: '审核中', 3: '失败' }[s] || '未知')
const realNameClass = (s) => ({ 0: '', 1: 'tag-success', 2: 'tag-warning', 3: 'tag-danger' }[s] || '')
const socialPlatformLabel = (p) => ({
  wechat: '微信', wechat_work: '企业微信', qq: 'QQ', weibo: '微博',
  github: 'GitHub', dingtalk: '钉钉', apple: 'Apple', google: 'Google'
}[p] || p)

const searchForm = reactive({ keyword: '', phone: '', status: '', dateRange: [] })
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])
const loading = ref(false)

const handleSearch = () => { page.value = 1; fetchData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.phone = ''; searchForm.status = ''; searchForm.dateRange = []; handleSearch() }

const fetchData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.phone) params.phone = searchForm.phone
    if (searchForm.status !== '') params.status = searchForm.status
    const { data } = await useFetch('/api/v1/admin/users/c', {
      params, headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      tableData.value = data.value.data.records || []
      total.value = data.value.data.total || 0
    }
  } catch (e) { /* fallback */ } finally { loading.value = false }
}

const detailVisible = ref(false)
const detailTab = ref('info')
const currentUser = ref(null)

const handleDetail = (row) => {
  currentUser.value = { ...row }
  detailTab.value = 'info'
  detailVisible.value = true
  fetchSocialList(row.id)
}

const socialData = ref([])
const socialLoading = ref(false)

const fetchSocialList = async (userId) => {
  socialLoading.value = true
  try {
    const { data } = await useFetch(`/api/v1/admin/users/c/${userId}/social`, {
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      socialData.value = data.value.data || []
    }
  } catch (e) { socialData.value = [] } finally { socialLoading.value = false }
}

const refreshSocialList = () => {
  if (currentUser.value?.id) fetchSocialList(currentUser.value.id)
}

const handleUnbindSocial = (row) => {
  ElMessageBox.confirm(`确定解除${socialPlatformLabel(row.platform)}绑定？`, '解绑确认', {
    type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
  }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/users/c/${currentUser.value.id}/social/${row.platform}`, {
      method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      ElMessage.success('解绑成功')
      fetchSocialList(currentUser.value.id)
    }
  }).catch(() => {})
}

const editVisible = ref(false)
const editFormRef = ref(null)
const editSubmitting = ref(false)
const editForm = reactive({
  id: '', nickname: '', email: '', level: 0, isBlacklisted: 0, realNameStatus: 0
})

const handleEdit = (row) => {
  Object.assign(editForm, {
    id: row.id, nickname: row.nickname || '', email: row.email || '',
    level: row.level || 0, isBlacklisted: row.isBlacklisted || 0,
    realNameStatus: row.realNameStatus || 0
  })
  editVisible.value = true
}

const handleEditSubmit = async () => {
  editSubmitting.value = true
  try {
    const { data } = await useFetch(`/api/v1/admin/users/c/${editForm.id}`, {
      method: 'PUT', body: editForm,
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      ElMessage.success('保存成功')
      editVisible.value = false
      fetchData()
    }
  } catch (e) { /* ignore */ } finally { editSubmitting.value = false }
}

const handleToggleStatus = (row) => {
  const action = row.status === 1 ? '禁用' : '启用'
  ElMessageBox.confirm(`确定${action}用户「${row.username}」？`, `${action}确认`, {
    type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
  }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/users/c/${row.id}/status`, {
      method: 'PUT', body: { status: row.status === 1 ? 0 : 1 },
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      ElMessage.success(`${action}成功`)
      fetchData()
    }
  }).catch(() => {})
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除用户「${row.username}」？此操作不可恢复。`, '删除确认', {
    type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
  }).then(async () => {
    const { data } = await useFetch(`/api/v1/admin/users/c/${row.id}`, {
      method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    }
  }).catch(() => {})
}
</script>