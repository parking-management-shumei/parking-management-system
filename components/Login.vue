<template>
  <main class="auth">
    <el-card
      ><h1>停车管理系统</h1>
      <p class="muted">登录后进入停车、寻车与运营工作台</p>
      <el-form @submit.native.prevent="login"
        ><el-form-item label="账号或手机号"
          ><el-input v-model.trim="identifier" autocomplete="username"
        /></el-form-item>
        <el-form-item label="密码"
          ><el-input
            v-model="password"
            type="password"
            autocomplete="current-password"
            show-password
        /></el-form-item>
        <el-alert v-if="error" :title="error" type="error" :closable="false" />
        <div class="toolbar">
          <el-button type="primary" native-type="submit" :loading="busy">登录</el-button
          ><el-button @click="$router.push('/register')">注册账号</el-button>
        </div></el-form
      >
    </el-card>
  </main>
</template>
<script>
export default {
  name: 'LoginPage',
  data: () => ({ identifier: '', password: '', busy: false, error: '' }),
  methods: {
    async login() {
      if (this.busy) return
      this.error = ''
      if (!this.identifier || !this.password) {
        this.error = '请输入账号和密码'
        return
      }
      this.busy = true
      try {
        const key = /^1[3-9]\d{9}$/.test(this.identifier) ? 'phone' : 'no'
        const { data } = await this.$axios.post('/user/login', {
          [key]: this.identifier,
          password: this.password,
        })
        sessionStorage.setItem('CurUser', JSON.stringify(data.data.user))
        this.$store.commit('setMenu', data.data.menu)
        await this.$router.replace('/IndexMain')
      } catch (e) {
        this.error = e.message
      } finally {
        this.busy = false
      }
    },
  },
}
</script>
<style scoped>
.auth {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 20px;
}
.el-card {
  width: 420px;
  max-width: 100%;
  padding: 12px;
}
</style>
