import Vue from 'vue'
import Vuex from 'vuex'
Vue.use(Vuex)
export default new Vuex.Store({
  state: { menu: [] },
  mutations: {
    setMenu(state, menu) {
      state.menu = Array.isArray(menu) ? menu : []
    },
  },
  getters: { getMenu: (state) => state.menu },
})
