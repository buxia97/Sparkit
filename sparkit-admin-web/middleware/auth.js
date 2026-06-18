// 认证中间件
export default defineNuxtRouteMiddleware((to) => {
  if (to.path === '/login') return

  const token = useCookie('accessToken')
  if (!token.value) {
    return navigateTo('/login')
  }
})