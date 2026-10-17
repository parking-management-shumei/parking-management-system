import Vue from 'vue'
import VueRouter from 'vue-router'
import api from '../api'
import store from '../store'
Vue.use(VueRouter)
const router = new VueRouter({
  mode: 'history',
  routes: [
    { path: '/', component: () => import('../components/Login.vue'), meta: { public: true } },
    {
      path: '/register',
      component: () => import('../components/register.vue'),
      meta: { public: true },
    },
    {
      path: '/Index',
      redirect: '/IndexMain',
      component: () => import('../components/Index.vue'),
      children: [
        { path: '/IndexMain', component: () => import('../components/Index/IndexManage.vue') },
        { path: '/Home', component: () => import('../components/Home.vue') },
        {
          path: '/operations',
          component: () => import('../components/parking/Operations.vue'),
          meta: { maxRole: 1 },
        },
        {
          path: '/reports',
          component: () => import('../components/parking/Reports.vue'),
          meta: { maxRole: 1 },
        },
        { path: '/parking', component: () => import('../components/parking/ParkingAssistant.vue') },
        { path: '/chat', component: () => import('../components/chat/ChatManage.vue') },
        {
          path: '/user',
          component: () => import('../components/user/UserManage.vue'),
          meta: { maxRole: 1 },
        },
        {
          path: '/admin',
          component: () => import('../components/admin/AdminManage.vue'),
          meta: { maxRole: 0 },
        },
      ],
    },
    { path: '*', redirect: '/' },
  ],
})
router.beforeEach(async (to, from, next) => {
  if (to.matched.some((r) => r.meta.public)) return next()
  try {
    const { data: result } = await api.get('/user/me')
    const { user, menu } = result.data
    sessionStorage.setItem('CurUser', JSON.stringify(user))
    store.commit('setMenu', menu)
    if (to.matched.some((r) => r.meta.maxRole !== undefined && user.roleId > r.meta.maxRole))
      return next('/parking')
    next()
  } catch (error) {
    next('/')
  }
})
export default router
