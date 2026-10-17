import Vue from 'vue'
import App from './App.vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import './assets/global.css'
import api from './api'
import router from './router'
import store from './store'
Vue.use(ElementUI, { size: 'small' })
Vue.prototype.$axios = api
// Existing components build endpoint paths with this prefix; api.js owns the environment base URL.
Vue.prototype.$httpUrl = ''
Vue.config.productionTip = false
window.addEventListener('session-expired', () => {
  store.commit('setMenu', [])
  if (!['/', '/register'].includes(router.currentRoute.path)) router.replace('/')
})
new Vue({ router, store, render: (h) => h(App) }).$mount('#app')
