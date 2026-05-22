import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)

// 使用 Element Plus UI 组件库
app.use(ElementPlus)

// 使用 Vue Router 路由
app.use(router)

// 挂载应用
app.mount('#app')
