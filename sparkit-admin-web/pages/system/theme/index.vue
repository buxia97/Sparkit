<template>
  <div>
    <div class="page-header">
      <div class="page-title">主题皮肤</div>
      <div class="page-desc">自定义后台主题配色与暗黑模式设置</div>
    </div>

    <el-card title="预设主题">
      <div class="theme-grid">
        <div
          v-for="theme in themes"
          :key="theme.name"
          class="theme-card"
          :class="{ active: currentTheme === theme.name }"
          @click="switchTheme(theme.name)"
        >
          <div class="theme-preview" :style="themePreviewStyle(theme)" />
          <div class="theme-name">{{ theme.label }}</div>
          <el-icon v-if="currentTheme === theme.name" class="theme-check"><CircleCheck /></el-icon>
        </div>
      </div>
    </el-card>

    <el-card title="自定义配色" style="margin-top: 16px;">
      <el-form label-width="120px" style="max-width: 500px;">
        <el-form-item label="主色调">
          <el-color-picker v-model="customColor.primary" @change="applyCustomColor" />
          <span style="margin-left: 8px; color: var(--text-secondary);">{{ customColor.primary }}</span>
        </el-form-item>
        <el-form-item label="成功色">
          <el-color-picker v-model="customColor.success" @change="applyCustomColor" />
          <span style="margin-left: 8px; color: var(--text-secondary);">{{ customColor.success }}</span>
        </el-form-item>
        <el-form-item label="警告色">
          <el-color-picker v-model="customColor.warning" @change="applyCustomColor" />
          <span style="margin-left: 8px; color: var(--text-secondary);">{{ customColor.warning }}</span>
        </el-form-item>
        <el-form-item label="危险色">
          <el-color-picker v-model="customColor.danger" @change="applyCustomColor" />
          <span style="margin-left: 8px; color: var(--text-secondary);">{{ customColor.danger }}</span>
        </el-form-item>
        <el-form-item label="暗黑模式">
          <el-switch v-model="darkMode" @change="toggleDarkMode" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

definePageMeta({
  middleware: 'auth',
  meta: { title: '主题皮肤' }
})

const auth = useAuthStore()
const tabsStore = useTabsStore()

onMounted(() => {
  tabsStore.addTab({ path: '/system/theme', title: '主题皮肤', icon: 'Brush' })
  const saved = localStorage.getItem('sparkit-theme')
  if (saved) {
    const config = JSON.parse(saved)
    currentTheme.value = config.theme || 'default'
    darkMode.value = config.darkMode || false
    if (config.customColor) {
      Object.assign(customColor, config.customColor)
      applyCustomColor()
    }
  }
  if (darkMode.value) {
    document.documentElement.classList.add('dark')
  }
})

const themes = [
  { name: 'default', label: '默认蓝', primary: '#409EFF', success: '#67C23A', warning: '#E6A23C', danger: '#F56C6C' },
  { name: 'green', label: '清新绿', primary: '#00C853', success: '#64DD17', warning: '#FFD600', danger: '#FF1744' },
  { name: 'purple', label: '优雅紫', primary: '#7C4DFF', success: '#69F0AE', warning: '#FFD740', danger: '#FF5252' },
  { name: 'orange', label: '活力橙', primary: '#FF6D00', success: '#00E676', warning: '#FFD600', danger: '#D50000' },
  { name: 'teal', label: '稳重青', primary: '#009688', success: '#4CAF50', warning: '#FF9800', danger: '#f44336' },
  { name: 'pink', label: '浪漫粉', primary: '#E91E63', success: '#8BC34A', warning: '#FFC107', danger: '#FF5722' }
]

const currentTheme = ref('default')
const darkMode = ref(false)
const customColor = reactive({ primary: '#409EFF', success: '#67C23A', warning: '#E6A23C', danger: '#F56C6C' })

const themePreviewStyle = (theme) => ({
  background: `linear-gradient(135deg, ${theme.primary}, ${theme.primary}88)`
})

const switchTheme = (name) => {
  currentTheme.value = name
  const theme = themes.find(t => t.name === name)
  if (theme) {
    Object.assign(customColor, { primary: theme.primary, success: theme.success, warning: theme.warning, danger: theme.danger })
    applyCustomColor()
  }
  saveTheme()
  ElMessage.success(`已切换至「${theme.label}」主题`)
}

const applyCustomColor = () => {
  const root = document.documentElement
  root.style.setProperty('--el-color-primary', customColor.primary)
  root.style.setProperty('--el-color-success', customColor.success)
  root.style.setProperty('--el-color-warning', customColor.warning)
  root.style.setProperty('--el-color-danger', customColor.danger)
  saveTheme()
}

const toggleDarkMode = (val) => {
  if (val) {
    document.documentElement.classList.add('dark')
  } else {
    document.documentElement.classList.remove('dark')
  }
  saveTheme()
  ElMessage.success(val ? '已开启暗黑模式' : '已关闭暗黑模式')
}

const saveTheme = () => {
  localStorage.setItem('sparkit-theme', JSON.stringify({
    theme: currentTheme.value,
    darkMode: darkMode.value,
    customColor: { ...customColor }
  }))
}
</script>

<style scoped>
.theme-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
}

.theme-card {
  position: relative;
  cursor: pointer;
  border: 2px solid var(--border-color);
  border-radius: 8px;
  padding: 12px;
  text-align: center;
  transition: all 0.2s;
}

.theme-card:hover {
  border-color: var(--el-color-primary);
  transform: translateY(-2px);
}

.theme-card.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.theme-preview {
  height: 60px;
  border-radius: 6px;
  margin-bottom: 8px;
}

.theme-name {
  font-size: 13px;
  font-weight: 500;
}

.theme-check {
  position: absolute;
  top: 8px;
  right: 8px;
  color: var(--el-color-primary);
  font-size: 20px;
}
</style>