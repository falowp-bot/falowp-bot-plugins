import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwind from '@tailwindcss/vite'

export default defineConfig({
  base: '/qq-native/',
  plugins: [react(), tailwind()],
  server: { proxy: { '/qq-native/status': 'http://127.0.0.1:8041' } },
})
