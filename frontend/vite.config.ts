import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  build: {
    target: 'es2015',
    minify: 'esbuild',
    cssMinify: 'lightningcss',
    rollupOptions: {
      output: {
        manualChunks: (id: string) => {
          if (id.includes('node_modules')) {
            if (id.includes('vue') || id.includes('vue-router') || id.includes('pinia')) {
              return 'vendor-vue'
            }
            if (id.includes('ant-design-vue')) {
              return 'vendor-antd'
            }
            if (id.includes('echarts')) {
              return 'vendor-echarts'
            }
            if (id.includes('@antv/x6')) {
              return 'vendor-x6'
            }
          }
        },
      }
    },
    analyze: false,
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
  optimizeDeps: {
    include: ['vue', 'vue-router', 'pinia', 'ant-design-vue', 'axios', 'dayjs'],
  },
})
