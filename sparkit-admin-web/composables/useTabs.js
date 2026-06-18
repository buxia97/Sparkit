// 页面标签管理
import { defineStore } from 'pinia'

export const useTabsStore = defineStore('tabs', () => {
  const tabs = ref([])
  const activeTab = ref('')

  const addTab = (tab) => {
    const exists = tabs.value.find(t => t.path === tab.path)
    if (!exists) {
      tabs.value.push({
        path: tab.path,
        title: tab.title,
        icon: tab.icon || '',
        closable: tab.closable !== false
      })
    }
    activeTab.value = tab.path
  }

  const removeTab = (path) => {
    const idx = tabs.value.findIndex(t => t.path === path)
    if (idx === -1) return

    tabs.value.splice(idx, 1)

    if (activeTab.value === path) {
      if (tabs.value.length > 0) {
        const newIdx = Math.min(idx, tabs.value.length - 1)
        activeTab.value = tabs.value[newIdx].path
      } else {
        activeTab.value = ''
      }
    }
  }

  const closeOther = (path) => {
    tabs.value = tabs.value.filter(t => t.path === path || !t.closable)
    activeTab.value = path
  }

  const closeAll = () => {
    tabs.value = tabs.value.filter(t => !t.closable)
    if (tabs.value.length > 0) {
      activeTab.value = tabs.value[0].path
    } else {
      activeTab.value = ''
    }
  }

  const setActive = (path) => {
    activeTab.value = path
  }

  return { tabs, activeTab, addTab, removeTab, closeOther, closeAll, setActive }
})