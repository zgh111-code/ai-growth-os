import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import { MotionPlugin } from '@vueuse/motion'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(ElementPlus)
app.use(MotionPlugin)
app.use(router)

app.mount('#app')
