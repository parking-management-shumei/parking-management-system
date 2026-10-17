<template>
  <header class="top">
    <el-button :icon="icon" circle aria-label="折叠菜单" @click="$emit('doCollapse')" /><strong
      >停车与寻车工作台</strong
    >
    <div>
      <span>{{ user.name }}</span
      ><el-button type="text" @click="$router.push('/Home')">个人中心</el-button
      ><el-button type="text" @click="logout">退出登录</el-button>
    </div>
  </header>
</template>
<script>
export default {
  name: 'AppHeader',
  props: { icon: String },
  computed: {
    user() {
      try {
        return JSON.parse(sessionStorage.getItem('CurUser')) || {}
      } catch (e) {
        return {}
      }
    },
  },
  methods: {
    async logout() {
      try {
        await this.$axios.post('/user/logout')
        sessionStorage.removeItem('CurUser')
        this.$store.commit('setMenu', [])
        this.$router.replace('/')
      } catch (e) {
        this.$message.error(e.message)
      }
    },
  },
}
</script>
<style scoped>
.top {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  line-height: normal;
}
@media (max-width: 640px) {
  strong {
    display: none;
  }
}
</style>
