<template>
  <div class="admin-header">
    <div class="header-left">
      <div class="header-collapse" @click="$emit('toggle-collapse')">
        <el-icon :size="20"><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
      </div>
      <div class="header-breadcrumb">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path" :to="item.path">
            {{ item.title }}
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>
    </div>

    <div class="header-right">
      <GlobalSearch />
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="header-user">
          <div class="user-avatar">{{ userInitial }}</div>
          <span class="user-name">{{ auth.userInfo?.nickname || auth.userInfo?.username || '管理员' }}</span>
          <el-icon :size="12"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon> 个人中心
            </el-dropdown-item>
            <el-dropdown-item command="password">
              <el-icon><Lock /></el-icon> 修改密码
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon> 退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { Fold, Expand, ArrowDown, User, Lock, SwitchButton } from '@element-plus/icons-vue'

const props = defineProps({
  collapsed: { type: Boolean, default: false }
})

defineEmits(['toggle-collapse'])

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const userInitial = computed(() => {
  const name = auth.userInfo?.nickname || auth.userInfo?.username || 'A'
  return name.charAt(0).toUpperCase()
})

const breadcrumbs = computed(() => {
  const matched = route.matched || []
  return matched
    .filter(r => r.meta?.title)
    .map(r => ({ title: r.meta.title, path: r.path }))
})

const handleCommand = async (command) => {
  if (command === 'logout') {
    await auth.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'password') {
    router.push('/profile?tab=password')
  }
}
</script>