import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getAuthToken } from '../state/session'

const http = axios.create({
  baseURL: '/api',
  timeout: 12000,
  headers: {
    'Content-Type': 'application/json',
  },
})

http.interceptors.request.use((config) => {
  const token = getAuthToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error.response?.status
    const message =
      status >= 500
        ? '后端接口返回 500，请检查后端服务日志'
        : error.response?.data?.message ||
          error.response?.data?.error ||
          error.message ||
          '请求失败'

    if (error.config?.method && error.config.method.toLowerCase() !== 'get') {
      ElMessage.error(message)
    }

    return Promise.reject(error)
  },
)

export default http
