import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

function normalizeBasePath(value) {
  const basePath = value?.trim() || '/'
  return `/${basePath.replace(/^\/+|\/+$/g, '')}/`.replace('//', '/')
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    base: normalizeBasePath(process.env.VITE_BASE_PATH || env.VITE_BASE_PATH),
    plugins: [vue()],
    server: {
      host: '0.0.0.0',
      port: 5174,
      strictPort: true,
      proxy: {
        '/api': {
          target: process.env.VITE_API_PROXY_TARGET || 'http://127.0.0.1:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
