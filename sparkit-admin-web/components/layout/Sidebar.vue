<template>
  <div class="admin-sidebar" :class="{ collapsed }">
    <div class="admin-sidebar-logo">
      <span class="logo-icon">&#9889;</span>
      <span v-show="!collapsed" class="logo-text">Sparkit</span>
    </div>
    <div class="admin-sidebar-menu">
      <template v-for="menu in visibleMenus" :key="menu.path || menu.title">
        <!-- 有子菜单 -->
        <div v-if="menu.children && menu.children.length > 0">
          <div
            class="menu-item"
            :class="{ active: isMenuActive(menu) }"
            @click="toggleSubmenu(menu)"
          >
            <el-icon class="menu-icon"><component :is="menu.icon" /></el-icon>
            <span v-show="!collapsed" class="menu-title">{{ menu.title }}</span>
            <el-icon v-show="!collapsed" class="menu-arrow" :class="{ open: expandedMenus.includes(menu.title) }">
              <ArrowRight />
            </el-icon>
          </div>
          <div
            v-show="!collapsed && expandedMenus.includes(menu.title)"
            class="submenu-list"
          >
            <div
              v-for="child in menu.children"
              :key="child.path"
              class="menu-item submenu-item"
              :class="{ active: route.path === child.path }"
              @click="navigateTo(child.path)"
            >
              <el-icon class="menu-icon"><component :is="child.icon" /></el-icon>
              <span class="menu-title">{{ child.title }}</span>
            </div>
          </div>
        </div>
        <!-- 无子菜单 -->
        <div
          v-else
          class="menu-item"
          :class="{ active: route.path === menu.path }"
          @click="navigateTo(menu.path)"
        >
          <el-icon class="menu-icon"><component :is="menu.icon" /></el-icon>
          <span v-show="!collapsed" class="menu-title">{{ menu.title }}</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ArrowRight } from '@element-plus/icons-vue'
import { menuConfig } from '~/composables/menuConfig'

const props = defineProps({
  collapsed: { type: Boolean, default: false }
})

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const expandedMenus = ref([])

const visibleMenus = computed(() => {
  return menuConfig.filter(menu => {
    if (menu.children) {
      const visibleChildren = menu.children.filter(c => auth.hasPermission(c.perm))
      if (visibleChildren.length > 0) {
        menu.children = visibleChildren
        return true
      }
      return false
    }
    return auth.hasPermission(menu.perm)
  })
})

const isMenuActive = (menu) => {
  if (menu.children) {
    return menu.children.some(c => route.path === c.path)
  }
  return route.path === menu.path
}

const toggleSubmenu = (menu) => {
  const idx = expandedMenus.value.indexOf(menu.title)
  if (idx > -1) {
    expandedMenus.value.splice(idx, 1)
  } else {
    expandedMenus.value.push(menu.title)
  }
}

const navigateTo = (path) => {
  router.push(path)
}

// 自动展开当前路由所在菜单
watch(() => route.path, () => {
  menuConfig.forEach(menu => {
    if (menu.children && menu.children.some(c => c.path === route.path)) {
      if (!expandedMenus.value.includes(menu.title)) {
        expandedMenus.value.push(menu.title)
      }
    }
  })
}, { immediate: true })
</script>