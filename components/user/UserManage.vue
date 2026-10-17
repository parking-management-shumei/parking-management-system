<template>
  <div class="page">
    <h1>{{ administrators ? '管理员管理' : '用户管理' }}</h1>
    <div class="toolbar">
      <el-input
        v-model.trim="name"
        placeholder="按姓名筛选"
        @keyup.enter.native="search"
      /><el-button @click="search">查询</el-button
      ><el-button
        @click="
          name = ''
          search()
        "
        >重置</el-button
      ><el-button type="primary" @click="edit()">新增</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" /><el-table
      :data="rows"
      v-loading="busy"
      ><el-table-column prop="no" label="账号" /><el-table-column
        prop="name"
        label="姓名"
      /><el-table-column prop="phone" label="电话" /><el-table-column label="角色"
        ><template slot-scope="s">{{
          ['超级管理员', '管理员', '用户'][s.row.roleId]
        }}</template></el-table-column
      ><el-table-column label="操作" width="180"
        ><template slot-scope="s"
          ><el-button @click="edit(s.row)">编辑</el-button
          ><el-button type="danger" @click="remove(s.row)">删除</el-button></template
        ></el-table-column
      ></el-table
    ><el-pagination
      :current-page="page"
      :page-size="10"
      :total="total"
      layout="prev,pager,next,total"
      @current-change="
        page = $event
        load()
      "
    /><el-dialog
      :title="form.id ? '编辑用户' : '新增用户'"
      :visible.sync="dialog"
      width="min(520px,95vw)"
      ><el-form label-width="90px"
        ><el-form-item label="账号"
          ><el-input v-model.trim="form.no" :disabled="!!form.id" /></el-form-item
        ><el-form-item label="姓名"><el-input v-model.trim="form.name" /></el-form-item
        ><el-form-item label="电话"><el-input v-model.trim="form.phone" /></el-form-item
        ><el-form-item label="密码"
          ><el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="form.id ? '留空不修改' : '8–72位'"
            autocomplete="new-password" /></el-form-item
        ><el-form-item label="角色"
          ><el-select v-model="form.roleId" :disabled="!!form.id && form.id === currentId"
            ><el-option label="用户" :value="2" /><el-option
              label="管理员"
              :value="1" /></el-select></el-form-item></el-form
      ><span slot="footer"
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save">保存</el-button></span
      ></el-dialog
    >
  </div>
</template>
<script>
export default {
  name: 'UserManage',
  props: { administrators: Boolean },
  data: () => ({
    rows: [],
    name: '',
    page: 1,
    total: 0,
    busy: false,
    saving: false,
    error: '',
    dialog: false,
    form: {},
    currentId: (JSON.parse(sessionStorage.getItem('CurUser')) || {}).id,
  }),
  mounted() {
    this.load()
  },
  methods: {
    search() {
      this.page = 1
      this.load()
    },
    async load() {
      this.busy = true
      this.error = ''
      try {
        const { data } = await this.$axios.post('/user/listPageC1', {
          pageNum: this.page,
          pageSize: 10,
          param: { name: this.name, roleId: this.administrators ? '1' : '2' },
        })
        this.rows = data.data
        this.total = data.total
      } catch (e) {
        this.error = e.message
      } finally {
        this.busy = false
      }
    },
    edit(row) {
      this.form = row
        ? { ...row, password: '' }
        : { no: '', name: '', phone: '', password: '', roleId: this.administrators ? 1 : 2 }
      this.dialog = true
    },
    async save() {
      if (this.saving) return
      this.saving = true
      try {
        await this.$axios.post(this.form.id ? '/user/update' : '/user/save', this.form)
        this.dialog = false
        await this.load()
      } catch (e) {
        this.$message.error(e.message)
      } finally {
        this.saving = false
      }
    },
    async remove(row) {
      try {
        await this.$confirm('确定删除此账号？存在停车记录的账号会保留。', '删除确认')
        await this.$axios.delete('/user/del', { params: { id: row.id } })
        await this.load()
      } catch (e) {
        if (e instanceof Error) this.$message.error(e.message)
      }
    },
  },
}
</script>
