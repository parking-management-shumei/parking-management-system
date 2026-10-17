<template>
  <div class="page">
    <h1>停车场实时运营</h1>
    <p class="muted">每15秒刷新登记数据 · 最近成功更新：{{ updated || '尚未加载' }}</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div class="metrics">
      <div class="metric">
        在场车辆<strong>{{ occupied }}</strong>
      </div>
      <div class="metric">
        剩余车位<strong>{{ capacity - occupied }}</strong>
      </div>
      <div class="metric">
        占用率<strong>{{ capacity ? ((occupied / capacity) * 100).toFixed(1) : 0 }}%</strong>
      </div>
      <div class="metric">
        今日已收款<strong>¥{{ Number(revenue).toFixed(2) }}</strong>
      </div>
    </div>
    <div class="section">
      <h2>车辆入场登记</h2>
      <div class="toolbar">
        <el-select v-model="form.lotId" placeholder="停车场"
          ><el-option
            v-for="r in lots"
            :key="r.lot.id"
            :label="r.lot.name"
            :value="r.lot.id" /></el-select
        ><el-select v-model="form.userId" filterable placeholder="车主账号"
          ><el-option
            v-for="u in users"
            :key="u.id"
            :label="u.name + ' (' + u.no + ')'"
            :value="u.id" /></el-select
        ><el-input v-model.trim="form.numberplate" placeholder="车牌号" /><el-input
          v-model.trim="form.spaceCode"
          placeholder="车位编号，如 B1-A023"
        /><el-button type="primary" :loading="acting" @click="enter">确认入场</el-button
        ><el-button @click="load">刷新</el-button>
      </div>
      <p class="muted">入场时间由服务端记录；重复车牌、重复车位和满位登记会被拒绝。</p>
    </div>
    <div class="section">
      <h2>在场车辆</h2>
      <el-table :data="active"
        ><el-table-column prop="numberplate" label="车牌" /><el-table-column
          prop="spaceCode"
          label="车位"
        /><el-table-column prop="entryTime" label="入场时间" min-width="180" /><el-table-column
          label="操作"
          ><template slot-scope="s"
            ><el-button :disabled="acting" @click="exit(s.row)">离场并生成订单</el-button></template
          ></el-table-column
        ></el-table
      >
    </div>
    <div class="section">
      <h2>最近订单</h2>
      <p class="muted">人工确认收款用于登记实际已收款项，系统未接入线上支付。</p>
      <el-table :data="orders"
        ><el-table-column prop="id" label="订单号" /><el-table-column
          prop="recordId"
          label="停车记录"
        /><el-table-column prop="amount" label="费用（元）" /><el-table-column
          prop="billingSource"
          label="计费来源"
        /><el-table-column label="状态"
          ><template slot-scope="s">{{
            s.row.status === 'PAID' ? '已收款' : '待收款'
          }}</template></el-table-column
        ><el-table-column label="操作" width="180"
          ><template slot-scope="s"
            ><el-button v-if="s.row.status !== 'PAID'" :disabled="acting" @click="paid(s.row)"
              >人工确认收款</el-button
            ></template
          ></el-table-column
        ></el-table
      >
    </div>
  </div>
</template>
<script>
export default {
  name: 'OperationsPage',
  data: () => ({
    lots: [],
    records: [],
    orders: [],
    users: [],
    revenue: 0,
    error: '',
    updated: '',
    acting: false,
    loading: false,
    timer: null,
    form: { lotId: null, userId: null, numberplate: '', spaceCode: '' },
  }),
  computed: {
    active() {
      return this.records.filter((r) => !r.exitTime)
    },
    occupied() {
      return this.lots.reduce((n, r) => n + r.occupied, 0)
    },
    capacity() {
      return this.lots.reduce((n, r) => n + r.lot.capacity, 0)
    },
  },
  async mounted() {
    try {
      this.users = (await this.$axios.get('/user/list')).data.data
    } catch (e) {
      this.error = e.message
    }
    await this.load()
    this.timer = setInterval(() => this.load(), 15000)
  },
  beforeDestroy() {
    clearInterval(this.timer)
  },
  methods: {
    async load() {
      if (this.loading) return
      this.loading = true
      try {
        const [overview, records, orders] = await Promise.all([
          this.$axios.get('/api/parking/overview'),
          this.$axios.get('/api/parking/records', { params: { active: true } }),
          this.$axios.get('/api/parking/orders'),
        ])
        this.lots = overview.data.data.lots
        this.revenue = overview.data.data.revenue
        this.records = records.data.data
        this.orders = orders.data.data
        this.updated = new Date().toLocaleTimeString()
        this.error = ''
        if (!this.form.lotId && this.lots.length) this.form.lotId = this.lots[0].lot.id
      } catch (e) {
        this.error = e.message
      } finally {
        this.loading = false
      }
    },
    async enter() {
      await this.act(async () => {
        await this.$axios.post('/api/parking/entry', this.form)
        this.form.numberplate = ''
        this.form.spaceCode = ''
      })
    },
    async exit(row) {
      try {
        await this.$confirm('确认车辆离场并按登记费率生成订单？', '车辆离场')
        await this.act(() => this.$axios.post('/api/parking/exit/' + row.id))
      } catch (e) {
        if (e instanceof Error) this.$message.error(e.message)
      }
    },
    async paid(row) {
      try {
        await this.$confirm(
          '确认已实际收到 ¥' + row.amount + '？此操作不会发起线上支付。',
          '人工收款登记',
        )
        await this.act(() => this.$axios.post('/api/parking/orders/' + row.id + '/paid'))
      } catch (e) {
        if (e instanceof Error) this.$message.error(e.message)
      }
    },
    async act(fn) {
      if (this.acting) return
      this.acting = true
      try {
        await fn()
        await this.load()
        this.$message.success('操作成功')
      } catch (e) {
        this.error = e.message
      } finally {
        this.acting = false
      }
    },
  },
}
</script>
