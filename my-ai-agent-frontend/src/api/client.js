import axios from 'axios'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  withCredentials: true,
})

// 响应拦截器：解析业务错误码，非 0 视为异常；401 自动跳转登录
api.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data && typeof data === 'object' && 'code' in data && data.code !== 0) {
      if (data.code === 40100) {
        // 跳过 /user/current 的 401（该接口预期在未登录时返回 401）
        const isCurrentUser = response.config.url?.includes('/user/current')
        if (!isCurrentUser) {
          window.location.href = '/login'
        }
        return Promise.reject(new Error('未登录'))
      }
      const err = new Error(data.message || '请求失败')
      err.response = response
      return Promise.reject(err)
    }
    return response
  },
  (error) => {
    return Promise.reject(error)
  },
)

export function createSseUrl(path, params = {}) {
  return api.getUri({ url: path, params })
}

export default api
