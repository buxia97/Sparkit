<template>
  <div>
    <div class="page-header">
      <div class="page-title">仪表盘</div>
      <div class="page-desc">欢迎使用 Sparkit 企业级开发框架管理中心</div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon">
          <el-icon :size="24"><UserFilled /></el-icon>
        </div>
        <div class="stat-value">{{ stats.totalUsers?.toLocaleString() || 0 }}</div>
        <div class="stat-label">总用户数</div>
        <div class="stat-trend up">
          <el-icon><Top /></el-icon> 较昨日 +{{ stats.todayNew || 0 }}
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <el-icon :size="24"><Notebook /></el-icon>
        </div>
        <div class="stat-value">{{ stats.totalArticles?.toLocaleString() || 0 }}</div>
        <div class="stat-label">新闻文章</div>
        <div class="stat-trend up">
          <el-icon><Top /></el-icon> 较上月 +{{ stats.monthArticles || 0 }}
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <el-icon :size="24"><FolderOpened /></el-icon>
        </div>
        <div class="stat-value">{{ stats.totalFiles?.toLocaleString() || 0 }}</div>
        <div class="stat-label">存储文件</div>
        <div class="stat-trend">正常运行中</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">
          <el-icon :size="24"><Clock /></el-icon>
        </div>
        <div class="stat-value">{{ stats.jobSuccessRate || '0%' }}</div>
        <div class="stat-label">定时任务成功率</div>
        <div class="stat-trend up">
          <el-icon><Top /></el-icon> 共 {{ stats.totalJobs || 0 }} 个任务
        </div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="dashboard-grid">
      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title">用户增长趋势</span>
          <el-radio-group v-model="chartPeriod" size="small">
            <el-radio-button label="week">近7天</el-radio-button>
            <el-radio-button label="month">近30天</el-radio-button>
          </el-radio-group>
        </div>
        <div class="chart-body">
          <div ref="userChartRef" style="width: 100%; height: 320px;"></div>
        </div>
      </div>
      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title">系统概览</span>
        </div>
        <div class="chart-body">
          <div ref="overviewChartRef" style="width: 100%; height: 320px;"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { UserFilled, Notebook, FolderOpened, Clock, Top } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

definePageMeta({
  middleware: 'auth',
  meta: { title: '仪表盘' }
})

const tabsStore = useTabsStore()
const chartPeriod = ref('week')
const userChartRef = ref(null)
const overviewChartRef = ref(null)
let userChart = null
let overviewChart = null

const stats = reactive({
  totalUsers: 0,
  todayNew: 0,
  totalArticles: 0,
  monthArticles: 0,
  totalFiles: 0,
  jobSuccessRate: '0%',
  totalJobs: 0
})

onMounted(async () => {
  tabsStore.addTab({ path: '/', title: '仪表盘', icon: 'Odometer', closable: false })
  await fetchStats()
  initCharts()
})

onUnmounted(() => {
  userChart?.dispose()
  overviewChart?.dispose()
})

const fetchStats = async () => {
  try {
    const { data } = await useFetch('/api/v1/admin/dashboard')
    if (data.value?.code === 200) {
      Object.assign(stats, data.value.data)
    }
  } catch (e) {
    // 使用模拟数据
    stats.totalUsers = 12800
    stats.todayNew = 156
    stats.totalArticles = 3456
    stats.monthArticles = 128
    stats.totalFiles = 8920
    stats.jobSuccessRate = '98.5%'
    stats.totalJobs = 15
  }
}

const initCharts = () => {
  // 用户增长趋势图
  if (userChartRef.value) {
    userChart = echarts.init(userChartRef.value)
    userChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { color: '#94a3b8' }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#f1f5f9' } },
        axisLabel: { color: '#94a3b8' }
      },
      series: [{
        name: '新增用户',
        type: 'line',
        smooth: true,
        data: [120, 200, 150, 80, 70, 110, 130],
        lineStyle: {
          width: 3,
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#00C853' },
            { offset: 1, color: '#2196F3' }
          ])
        },
        itemStyle: { color: '#2196F3' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0,200,83,0.2)' },
            { offset: 1, color: 'rgba(33,150,243,0.02)' }
          ])
        }
      }]
    })
  }

  // 系统概览图
  if (overviewChartRef.value) {
    overviewChart = echarts.init(overviewChartRef.value)
    overviewChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, textStyle: { color: '#475569', fontSize: 12 } },
      series: [{
        name: '系统概览',
        type: 'pie',
        radius: ['45%', '75%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 3
        },
        label: {
          show: true,
          position: 'outside',
          formatter: '{b}\n{d}%',
          color: '#475569',
          fontSize: 11
        },
        data: [
          { value: 12800, name: '用户数', itemStyle: { color: '#00C853' } },
          { value: 3456, name: '文章数', itemStyle: { color: '#2196F3' } },
          { value: 8920, name: '文件数', itemStyle: { color: '#69F0AE' } },
          { value: 15, name: '定时任务', itemStyle: { color: '#64B5F6' } },
          { value: 120, name: '管理员', itemStyle: { color: '#0D47A1' } }
        ]
      }]
    })
  }
}

watch(chartPeriod, () => {
  initCharts()
})
</script>