import axios from 'axios'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  withCredentials: true,
})

// 响应拦截器：解析业务错误码，非 0 视为异常
api.interceptors.response.use(
  (response) => {
    const data = response.data
    // 只有 JSON 格式且 code 字段存在且不为 0 时才拦截
    if (data && typeof data === 'object' && 'code' in data && data.code !== 0) {
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
