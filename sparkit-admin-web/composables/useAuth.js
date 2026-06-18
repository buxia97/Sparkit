// 认证状态管理
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', () => {
  const token = useCookie('accessToken')
  const refreshToken = useCookie('refreshToken')
  const userInfo = ref(null)
  const permissions = ref([])
  const roles = ref([])

  const isLoggedIn = computed(() => !!token.value)

  const login = async (credentials) => {
    const { data } = await useFetch('/api/v1/admin/users/login', {
      method: 'POST',
      body: { username: credentials.username, password: credentials.password }
    })

    const result = data.value
    if (!result || result.code !== 200) {
      throw new Error(result?.msg || '登录失败')
    }

    token.value = result.data.accessToken
    refreshToken.value = result.data.refreshToken
    userInfo.value = result.data.userInfo || { username: credentials.username }
    permissions.value = result.data.userInfo?.permissions || []
    roles.value = result.data.userInfo?.roleIds || []

    return result
  }

  const logout = async () => {
    token.value = null
    refreshToken.value = null
    userInfo.value = null
    permissions.value = []
    roles.value = []
  }

  const hasPermission = (perm) => {
    if (!permissions.value || permissions.value.length === 0) return false
    if (permissions.value.includes('*:*:*')) return true
    return permissions.value.includes(perm)
  }

  return {
    token, refreshToken, userInfo, permissions, roles, isLoggedIn,
    login, logout, hasPermission
  }
})