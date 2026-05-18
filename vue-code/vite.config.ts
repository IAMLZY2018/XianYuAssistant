import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import pkg from './package.json'

export default defineConfig({
  plugins: [vue()],
  define: {
    __APP_VERSION__: JSON.stringify(pkg.version)
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:12400',
        changeOrigin: true
      },
      '/ai': {
        target: 'http://localhost:12400',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: '../src/main/resources/static',
    assetsDir: 'assets',
    sourcemap: false,
    emptyOutDir: true
  }
})
