<template>
  <div class="admin-tabs">
    <div class="tabs-wrapper" ref="tabsWrapper">
      <div
        v-for="tab in tabsStore.tabs"
        :key="tab.path"
        class="tab-item"
        :class="{ active: tabsStore.activeTab === tab.path }"
        @click="switchTab(tab.path)"
      >
        <span class="tab-title">{{ tab.title }}</span>
        <span
          v-if="tab.closable"
          class="tab-close"
          @click.stop="tabsStore.removeTab(tab.path)"
        >
          <el-icon :size="10"><Close /></el-icon>
        </span>
      </div>
    </div>
    <div v-if="tabsStore.tabs.length > 0" class="tabs-actions">
      <el-dropdown trigger="click" @command="handleCommand">
        <el-icon :size="14" style="cursor: pointer; padding: 4px;"><ArrowDown /></el-icon>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="closeOther">关闭其他</el-dropdown-item>
            <el-dropdown-item command="closeAll">关闭全部</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { Close, ArrowDown } from '@element-plus/icons-vue'

const tabsStore = useTabsStore()
const router = useRouter()

const switchTab = (path) => {
  tabsStore.setActive(path)
  router.push(path)
}

const handleCommand = (command) => {
  if (command === 'closeOther') {
    tabsStore.closeOther(tabsStore.activeTab)
  } else if (command === 'closeAll') {
    tabsStore.closeAll()
    if (tabsStore.activeTab) {
      router.push(tabsStore.activeTab)
    }
  }
}
</script>