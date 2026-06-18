// API 请求封装 - 基于 useFetch
import { ElMessage } from 'element-plus'

const BASE_URL = '/api/v1'

export const useApi = () => {
  const token = useCookie('accessToken')
  const router = useRouter()

  const request = async (url, options = {}) => {
    const config = {
      baseURL: BASE_URL,
      headers: {
        'Content-Type': 'application/json',
        ...(token.value ? { Authorization: `Bearer ${token.value}` } : {}),
        ...options.headers
      },
      ...options
    }

    try {
      const { data, error } = await useFetch(url, config)

      if (error.value) {
        throw error.value
      }

      const result = data.value

      if (result.code === 401) {
        token.value = null
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(new Error('未授权'))
      }

      if (result.code !== 200) {
        ElMessage.error(result.msg || '请求失败')
        return Promise.reject(new Error(result.msg))
      }

      return result
    } catch (err) {
      console.error('API Error:', err)
      throw err
    }
  }

  return {
    get: (url, params) => request(url, { method: 'GET', params }),
    post: (url, body) => request(url, { method: 'POST', body }),
    put: (url, body) => request(url, { method: 'PUT', body }),
    delete: (url, params) => request(url, { method: 'DELETE', params })
  }
}