import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Host Spring Boot defaults to 8081 to avoid clashing with common 8080 services (e.g. Jenkins).
const API_PROXY_TARGET = process.env.VITE_PROXY_TARGET || 'http://localhost:8081'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: API_PROXY_TARGET,
        changeOrigin: true,
      },
      '/uploads-book-reviewer': {
        target: API_PROXY_TARGET,
        changeOrigin: true,
      },
    },
  },
})
