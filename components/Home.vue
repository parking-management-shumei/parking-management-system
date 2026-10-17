<template>
  <div class="page">
    <h1>个人中心</h1>
    <div class="section">
      <p>账号：{{ user.no }}</p>
      <p>角色：{{ ['超级管理员', '管理员', '用户'][user.roleId] }}</p>
      <el-form label-width="100px" @submit.native.prevent="save"
        ><el-form-item label="姓名"
          ><el-input v-model.trim="form.name" maxlength="60" /></el-form-item
        ><el-form-item label="手机号"><el-input v-model.trim="form.phone" /></el-form-item
        ><el-form-item label="新密码"
          ><el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="留空不修改；8–72位"
            autocomplete="new-password" /></el-form-item
        ><el-button native-type="submit" type="primary" :loading="busy"
          >保存修改</el-button
        ></el-form
      ><el-alert v-if="error" :title="error" type="error" :closable="false" />
    </div>
  </div>
</template>
<script>
export default {
  name: 'ProfilePage',
  data() {
    const u = JSON.parse(sessionStorage.getItem('CurUser')) || {}
    return { user: u, form: { ...u, password: '' }, busy: false, error: '' }
  },
  methods: {
    async save() {
      if (this.busy) return
      this.busy = true
      this.error = ''
      try {
        const { data } = await this.$axios.post('/user/update', this.form)
        this.user = data.data
        this.form = { ...data.data, password: '' }
        sessionStorage.setItem('CurUser', JSON.stringify(data.data))
        this.$message.success('已保存')
      } catch (e) {
        this.error = e.message
      } finally {
        this.busy = false
      }
    },
  },
}
</script>
