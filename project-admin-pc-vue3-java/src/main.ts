// 项目入口文件
import { createApp } from 'vue'

import TDesign from 'tdesign-vue-next'
import 'tdesign-vue-next/es/style/index.css'
import 'default-passive-events'

import { store } from './store'
import router from './router'
import '@/style/index.less'
import './permission' // permission是路由权限控制
import App from './App.vue'

const app = createApp(App)

app.use(TDesign)
app.use(store)
app.use(router)

app.mount('#app')
