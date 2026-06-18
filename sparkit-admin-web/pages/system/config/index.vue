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
                <template v-else-if="item.configType === 'number'">
                  <el-input-number v-model="configs[item.configKey]" :min="0" :max="1" :step="0.1" :precision="1" style="width: 200px;" />
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
  { label: '存储设置', value: 'storage' },
  { label: '安全设置', value: 'security' },
  { label: '邮件设置', value: 'email' },
  { label: '短信设置', value: 'sms' },
  { label: '支付设置', value: 'payment' },
  { label: '社交登录', value: 'social' }
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
        configName: formatConfigName(key),
        configType: detectConfigType(key, val),
        configValue: val
      }))
    }
  } catch (e) { /* fallback */ } finally { loading.value = false }
}

/** 根据 key 转换为可读名称 */
const formatConfigName = (key) => {
  const nameMap = {
    // 存储设置
    'storage.image_compress_enabled': '图片压缩开关',
    'storage.image_compress_quality': '图片压缩质量',
    'storage.video_transcode_enabled': '视频转码开关',

    // 社交登录 - 微信
    'social.wechat.app_id': '微信 AppID',
    'social.wechat.app_secret': '微信 AppSecret',
    'social.wechat.mini_app_id': '微信小程序 AppID',
    'social.wechat.mini_app_secret': '微信小程序 AppSecret',
    'social.wechat.mp_token': '微信公众号 Token',

    // 社交登录 - QQ
    'social.qq.app_id': 'QQ AppID',
    'social.qq.app_key': 'QQ AppKey',

    // 社交登录 - 微博
    'social.weibo.app_key': '微博 AppKey',
    'social.weibo.app_secret': '微博 AppSecret',

    // 社交登录 - GitHub
    'social.github.client_id': 'GitHub ClientID',
    'social.github.client_secret': 'GitHub ClientSecret',

    // 社交登录 - 钉钉
    'social.dingtalk.app_key': '钉钉 AppKey',
    'social.dingtalk.app_secret': '钉钉 AppSecret',

    // 社交登录 - 企业微信
    'social.wechat_work.corp_id': '企业微信 CorpID',
    'social.wechat_work.corp_secret': '企业微信 CorpSecret',
    'social.wechat_work.agent_id': '企业微信 AgentID',

    // 短信设置
    'sms.provider': '短信服务商',
    'sms.aliyun.access_key_id': '阿里云 AccessKeyId',
    'sms.aliyun.access_key_secret': '阿里云 AccessKeySecret',
    'sms.aliyun.sign_name': '阿里云短信签名',
    'sms.aliyun.template_code': '阿里云短信模板ID',
    'sms.feige.access_key_id': '飞鸽云 AccessKeyId',
    'sms.feige.access_key_secret': '飞鸽云 AccessKeySecret',
    'sms.feige.sign_id': '飞鸽云签名ID',
    'sms.feige.template_id': '飞鸽云模板ID',

    // 邮件设置
    'email.smtp_host': 'SMTP 服务器',
    'email.smtp_port': 'SMTP 端口',
    'email.smtp_ssl': '启用 SSL',
    'email.username': '邮箱账号',
    'email.password': '邮箱密码',
    'email.from': '发件人地址',

    // 实名认证
    'realname.provider': '实名认证服务商',
    'realname.aliyun.app_code': '阿里云 AppCode',
    'realname.tencent.secret_id': '腾讯云 SecretId',
    'realname.tencent.secret_key': '腾讯云 SecretKey',

    // 支付 - 微信
    'payment.wechat.mch_id': '微信商户号',
    'payment.wechat.api_v3_key': '微信 APIv3 密钥',
    'payment.wechat.serial_no': '微信证书序列号',
    'payment.wechat.private_key': '微信商户私钥',
    'payment.wechat.app_id': '微信支付 AppID',
    'payment.wechat.mini_app_id': '微信小程序 AppID',
    'payment.wechat.notify_url': '微信支付回调地址',

    // 支付 - 支付宝
    'payment.alipay.app_id': '支付宝 AppID',
    'payment.alipay.private_key': '支付宝商户私钥',
    'payment.alipay.public_key': '支付宝公钥',
    'payment.alipay.notify_url': '支付宝回调地址',

    // 支付 - PayPal
    'payment.paypal.client_id': 'PayPal ClientID',
    'payment.paypal.client_secret': 'PayPal ClientSecret',
    'payment.paypal.mode': 'PayPal 模式',
    'payment.paypal.notify_url': 'PayPal 回调地址',

    // 支付 - Apple Pay
    'payment.apple.merchant_id': 'Apple商户ID',
    'payment.apple.merchant_cert_path': 'Apple商户证书路径',
    'payment.apple.merchant_cert_password': 'Apple商户证书密码',
    'payment.apple.domain': 'Apple验证域名',

    // 支付 - Google Pay
    'payment.google.merchant_id': 'Google商户ID',
    'payment.google.merchant_name': 'Google商户名称',
    'payment.google.service_account_key': 'Google服务账号密钥',

    // 微信支付回调
    'payment.wechat_key': '微信支付回调密钥',
  }
  return nameMap[key] || key
}

/** 根据 key 和 value 推断配置类型 */
const detectConfigType = (key, val) => {
  if (key.includes('_enabled') || key.includes('_switch') || key.includes('_open')) {
    return 'switch'
  }
  if (key.includes('_quality') || key.includes('_rate') || key.includes('_ratio')) {
    return 'number'
  }
  if (typeof val === 'boolean') {
    return 'switch'
  }
  if (typeof val === 'number') {
    return 'number'
  }
  return 'text'
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