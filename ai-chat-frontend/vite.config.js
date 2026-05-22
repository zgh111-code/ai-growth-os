import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // 加载当前模式下的环境变量
  // mode 的值：dev 时是 "development"，build 时是 "production"
  // 第三个参数 '' 表示加载所有 .env 文件，而不仅是 VITE_ 开头的
  const env = loadEnv(mode, process.cwd(), '')

  console.log(`[Vite] 当前模式: ${mode}`)
  console.log(`[Vite] API 地址: ${env.VITE_API_BASE_URL}`)

  // 是否为生产环境构建
  const isProd = mode === 'production'

  return {
    plugins: [vue()],

    // 构建配置
    build: {
      // 打包输出目录（默认就是 dist，这里显式写出方便理解）
      outDir: 'dist',
      // 使用 esbuild 进行代码压缩（速度快）
      minify: 'esbuild',
      // 生产环境下移除 console.log 和 debugger
      ...(isProd ? {
        esbuild: {
          drop: ['console', 'debugger']
        }
      } : {}),
      // 第三方库体积较大，提高 chunk 大小警告阈值（2MB 以下不警告）
      chunkSizeWarningLimit: 2000,
      // 代码分割：将第三方依赖拆分为独立 chunk，利用浏览器缓存
      rollupOptions: {
        output: {
          manualChunks(id) {
            // element-plus 单独打包（约 1MB）
            if (id.includes('node_modules/element-plus')) {
              return 'element-plus'
            }
            // highlight.js + marked 单独打包（约 1MB）
            if (id.includes('node_modules/highlight.js') || id.includes('node_modules/marked')) {
              return 'markdown'
            }
            // 其他第三方依赖
            if (id.includes('node_modules')) {
              return 'vendor'
            }
          }
        }
      }
    },

    // 开发服务器配置
    server: {
      // 开发服务器端口
      port: 5173,
      // 启动后自动打开浏览器
      open: true
    }
  }
})
