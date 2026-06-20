import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 12000,
  headers: {
    'Content-Type': 'application/json',
  },
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message =
      error.response?.data?.message ||
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
