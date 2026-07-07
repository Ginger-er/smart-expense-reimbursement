import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import '@/styles/global.scss'
import '@/styles/element-override.scss'

// 前端版本标记：F12 控制台可见，用于确认浏览器加载的是否为最新代码
console.log('智报前端版本: v2 (OCR轮询修复版) 2026-08-23')

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { size: 'default' })

app.mount('#app')
