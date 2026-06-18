<template>
  <div>
    <div class="page-header">
      <div class="page-title">系统配置</div>
      <div class="page-desc">管理全局系统参数，支持按分组批量保存</div>
    </div>

    <el-card>
      <el-tabs v-model="activeGroup" @tab-change="fetchConfigs">
        <el-tab-pane v-for="g in groups" :key="g.value" :label="g.label" :name="g.value" />
      </el-tabs>

      <div v-loading="loading" style="min-height: 200px;">
        <el-form v-if="configs" ref="formRef" :model="configs" label-width="140px">
          <div class="config-form-section">
            <div class="section-title">{{ groupLabel }}设置</div>
            <div class="form-inline">
              <el-form-item
                v-for="item in currentConfigs"
                :key="item.configKey"
                :label="item.configName"
                :prop="item.configKey"
              >
                <template v-if="item.configType === 'switch'">
                  <el-switch v-model="configs[item.configKey]" :active-value="'true'" :inactive-value="'false'" />
                </template>
                <template v-else-if="item.configType === 'textarea'">
                  <el-input v-model="configs[item.configKey]" type="textarea" :rows="3" />
                </template>
                <template v-else-if="item.configType === 'image'">
                  <el-input v-model="configs[item.configKey]" placeholder="请输入图片URL" />
                </template>
                <template v-else>
                  <el-input v-model="configs[item.configKey]" :placeholder="`请输入${item.configName}`" />
                </template>
                <span class="text-sm text-secondary" style="margin-left: 8px;">{{ item.configKey }}</span>
              </el-form-item>
            </div>
          </div>
          <el-form-item>
            <el-button type="primary" @click="handleSave" :loading="saving">
              <el-icon><Check /></el-icon>保存配置
            </el-button>
            <el-button @click="fetchConfigs">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { Check } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

definePageMeta({
  middleware: 'auth',
  meta: { title: '系统配置' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/system/config', title: '系统配置', icon: 'Tools' })
  fetchConfigs()
})

const groups = [
  { label: '站点设置', value: 'site' },
  { label: '上传设置', value: 'upload' },
  { label: '安全设置', value: 'security' },
  { label: '邮件设置', value: 'email' },
  { label: '短信设置', value: 'sms' },
  { label: '支付设置', value: 'payment' }
]

const activeGroup = ref('site')
const loading = ref(false)
const saving = ref(false)
const configs = ref(null)
const currentConfigs = ref([])

const groupLabel = computed(() => {
  const g = groups.find(g => g.value === activeGroup.value)
  return g ? g.label : ''
})

const fetchConfigs = async () => {
  loading.value = true
  try {
    const { data } = await useFetch(`/api/v1/admin/configs/group/${activeGroup.value}`, {
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      configs.value = data.value.data
      // 从返回数据中提取配置项元数据
      currentConfigs.value = Object.entries(data.value.data || {}).map(([key, val]) => ({
        configKey: key,
        configName: key,
        configType: typeof val === 'boolean' ? 'switch' : 'text',
        configValue: val
      }))
    }
  } catch (e) { /* fallback */ } finally { loading.value = false }
}

const handleSave = async () => {
  saving.value = true
  try {
    const { data } = await useFetch('/api/v1/admin/configs/batch', {
      method: 'PUT',
      body: { group: activeGroup.value, configs: configs.value },
      headers: { Authorization: `Bearer ${auth.token}` }
    })
    if (data.value?.code === 200) {
      ElMessage.success('配置保存成功')
    }
  } catch (e) { /* ignore */ } finally { saving.value = false }
}
</script>