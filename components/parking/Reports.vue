<template>
  <div class="page">
    <h1>停车统计报表</h1>
    <p class="muted">按服务器日期统计入场、离场；营收以收款日期统计，不包含待收款订单。</p>
    <div class="toolbar">
      <el-date-picker
        v-model="range"
        type="daterange"
        value-format="yyyy-MM-dd"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
      /><el-button type="primary" :loading="busy" @click="load">查询</el-button
      ><el-button :disabled="!days.length" @click="download">导出 CSV</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div class="metrics">
      <div class="metric">
        期间入场<strong>{{ entries }}</strong>
      </div>
      <div class="metric">
        期间离场<strong>{{ exits }}</strong>
      </div>
      <div class="metric">
        期间实收<strong>¥{{ Number(revenue).toFixed(2) }}</strong>
      </div>
    </div>
    <div class="section">
      <h2>每日实收趋势</h2>
      <svg v-if="days.length" viewBox="0 0 800 180" role="img" aria-label="每日实收柱状图">
        <g v-for="(d, i) in days" :key="d.date">
          <title>{{ d.date }}：{{ d.revenue }}元</title>
          <rect
            :x="(i * 800) / days.length + 2"
            :y="150 - (Number(d.revenue) / maxRevenue) * 130"
            :width="Math.max(1, 800 / days.length - 4)"
            :height="Math.max(1, (Number(d.revenue) / maxRevenue) * 130)"
            rx="2"
            fill="#3f77ce"
          />
        </g>
        <line x1="0" y1="150" x2="800" y2="150" stroke="#aabbcc" />
      </svg>
      <p class="muted">
        {{ days.length ? days[0].date + ' 至 ' + days[days.length - 1].date : '暂无数据' }} ·
        具体数值见下表
      </p>
      <el-table :data="days"
        ><el-table-column prop="date" label="日期" /><el-table-column
          prop="entries"
          label="入场" /><el-table-column prop="exits" label="离场" /><el-table-column
          prop="revenue"
          label="实收（元）"
      /></el-table>
    </div>
  </div>
</template>
<script>
function date(d) {
  return [
    d.getFullYear(),
    String(d.getMonth() + 1).padStart(2, '0'),
    String(d.getDate()).padStart(2, '0'),
  ].join('-')
}
export default {
  name: 'ReportsPage',
  data() {
    const end = new Date(),
      start = new Date()
    start.setDate(end.getDate() - 6)
    return {
      range: [date(start), date(end)],
      days: [],
      entries: 0,
      exits: 0,
      revenue: 0,
      busy: false,
      error: '',
    }
  },
  computed: {
    maxRevenue() {
      return Math.max(1, ...this.days.map((d) => Number(d.revenue)))
    },
  },
  mounted() {
    this.load()
  },
  methods: {
    async load() {
      if (this.busy) return
      if (!this.range || this.range.length !== 2) {
        this.error = '请选择日期范围'
        return
      }
      this.busy = true
      this.error = ''
      try {
        const { data } = await this.$axios.get('/api/parking/reports', {
          params: { start: this.range[0], end: this.range[1] },
        })
        Object.assign(this, data.data)
      } catch (e) {
        this.error = e.message
      } finally {
        this.busy = false
      }
    },
    download() {
      const rows = [
        ['日期', '入场', '离场', '实收（元）'],
        ...this.days.map((d) => [d.date, d.entries, d.exits, d.revenue]),
      ]
      const url = URL.createObjectURL(
        new Blob(['\ufeff' + rows.map((r) => r.join(',')).join('\r\n')], {
          type: 'text/csv;charset=utf-8',
        }),
      )
      const a = document.createElement('a')
      a.href = url
      a.download = '停车统计.csv'
      a.click()
      URL.revokeObjectURL(url)
    },
  },
}
</script>
<style scoped>
svg {
  width: 100%;
  max-height: 240px;
}
</style>
