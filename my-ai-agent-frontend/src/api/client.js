import axios from 'axios'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
})

export function createSseUrl(path, params = {}) {
  return api.getUri({
    url: path,
    params,
  })
}
