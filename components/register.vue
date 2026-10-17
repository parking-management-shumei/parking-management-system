<template>
  <main class="auth">
    <el-card
      ><h1>创建停车账号</h1>
      <el-form @submit.native.prevent="save">
        <el-form-item label="账号（3–32位字母、数字或下划线）"
          ><el-input v-model.trim="form.no" maxlength="32" autocomplete="username"
        /></el-form-item>
        <el-form-item label="姓名"
          ><el-input v-model.trim="form.name" maxlength="60"
        /></el-form-item>
        <el-form-item label="密码（8–72位）"
          ><el-input
            v-model="form.password"
            type="password"
            show-password
            autocomplete="new-password"
        /></el-form-item>
        <el-form-item label="手机号（选填）"
          ><el-input v-model.trim="form.phone" maxlength="11"
        /></el-form-item>
        <el-alert v-if="error" type="error" :title="error" :closable="false" />
        <div class="toolbar">
          <el-button native-type="submit" type="primary" :loading="busy">注册</el-button
          ><el-button @click="$router.push('/')">返回登录</el-button>
        </div>
      </el-form></el-card
    >
  </main>
</template>
<script>
export default {
  name: 'RegisterPage',
  data: () => ({ form: { no: '', name: '', password: '', phone: '' }, busy: false, error: '' }),
  methods: {
    async save() {
      if (this.busy) return
      this.busy = true
      this.error = ''
      try {
        await this.$axios.post('/user/save', this.form)
        this.$message.success('注册成功，请登录')
        this.$router.push('/')
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
  width: 460px;
  max-width: 100%;
  padding: 12px;
}
</style>
