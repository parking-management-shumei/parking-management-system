<template>
  <div class="page">
    <h1>停车与寻车助手</h1>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div class="section">
      <h2>停车场推荐</h2>
      <p class="muted">基于数据库登记占用排序；与高德驾车路线工作流分开。</p>
      <div class="toolbar">
        <el-select v-model="preference"
          ><el-option label="空位更多" value="available" /><el-option
            label="首小时更便宜"
            value="price" /></el-select
        ><el-button :loading="busy" @click="recommend">刷新推荐</el-button>
      </div>
      <div class="metrics">
        <div v-for="r in candidates" :key="r.lot.id" class="metric">
          <h3>{{ r.lot.name }}</h3>
          <p>{{ r.lot.address }}</p>
          <strong>{{ r.available }} 个空位</strong>
          <p>占用 {{ r.occupied }} / {{ r.lot.capacity }}</p>
          <p>
            首小时 ¥{{ r.lot.firstPrice }}，后续每小时 ¥{{ r.lot.hourlyPrice }}，每24小时封顶 ¥{{
              r.lot.dailyCap
            }}
          </p>
        </div>
      </div>
      <p v-if="!candidates.length" class="muted">暂无可推荐停车场，请刷新或稍后再试。</p>
    </div>
    <div class="section">
      <h2>反向寻车</h2>
      <p class="muted">普通用户只能查询管理员登记到本人账号的在场车辆。</p>
      <div class="toolbar">
        <el-input v-model.trim="numberplate" placeholder="车牌号" /><el-input
          v-model.trim="nowLocation"
          placeholder="当前位置（选填）"
        /><el-button :loading="busy" @click="reverse">查找车辆</el-button>
      </div>
      <div v-if="location">
        <p>
          停车场：{{ location.lot.name }} · 车位：<strong>{{ location.record.spaceCode }}</strong>
        </p>
        <p>入场时间：{{ location.record.entryTime }}</p>
        <el-alert
          :title="
            location.source === 'coze'
              ? '已调用 Coze 反向寻车工作流'
              : 'Coze 未配置；当前展示数据库登记位置'
          "
          type="info"
          :closable="false"
        />
        <pre v-if="location.workflow">{{ format(location.workflow.message) }}</pre>
      </div>
    </div>
    <div class="section">
      <h2>高德驾车导航路线</h2>
      <p class="muted">对应 track_recommend 代码节点，展示路线策略及文字指引；不是空车位推荐。</p>
      <el-alert
        :title="
          routeConfigured
            ? 'Coze 路线工作流已配置'
            : 'Coze 路线工作流未配置；注入服务端环境变量后即可启用'
        "
        type="info"
        :closable="false"
      />
      <div class="toolbar">
        <el-input v-model.number="route.start_lng" placeholder="起点经度" /><el-input
          v-model.number="route.start_lat"
          placeholder="起点纬度"
        /><el-input v-model.number="route.end_lng" placeholder="终点经度" /><el-input
          v-model.number="route.end_lat"
          placeholder="终点纬度"
        /><el-button :disabled="!routeConfigured" :loading="busy" @click="navigate"
          >获取路线</el-button
        >
      </div>
      <div v-for="(r, i) in routes" :key="i">
        <h3>{{ r.strategy }}</h3>
        <p>{{ r.track }}</p>
      </div>
    </div>
    <div class="section">
      <h2>我的停车记录与订单</h2>
      <el-table :data="records"
        ><el-table-column prop="numberplate" label="车牌" /><el-table-column
          prop="spaceCode"
          label="车位" /><el-table-column prop="entryTime" label="入场时间" /><el-table-column
          prop="exitTime"
          label="离场时间" /></el-table
      ><el-table :data="orders"
        ><el-table-column prop="recordId" label="停车记录" /><el-table-column
          prop="amount"
          label="费用（元）"
        /><el-table-column label="状态"
          ><template slot-scope="s">{{
            s.row.status === 'PAID' ? '已收款' : '待收款'
          }}</template></el-table-column
        ></el-table
      >
    </div>
  </div>
</template>
<script>
export default {
  name: 'ParkingAssistant',
  data: () => ({
    preference: 'available',
    candidates: [],
    numberplate: '',
    nowLocation: '',
    location: null,
    busy: false,
    error: '',
    records: [],
    orders: [],
    routeConfigured: false,
    route: { start_lat: null, start_lng: null, end_lat: null, end_lng: null },
    routes: [],
  }),
  async mounted() {
    await this.recommend()
    try {
      const [records, orders, status] = await Promise.all([
        this.$axios.get('/api/parking/records'),
        this.$axios.get('/api/parking/orders'),
        this.$axios.get('/api/parking/integrations'),
      ])
      this.records = records.data.data
      this.orders = orders.data.data
      this.routeConfigured = status.data.data.route
    } catch (e) {
      this.error = e.message
    }
  },
  methods: {
    format(v) {
      return typeof v === 'string' ? v : JSON.stringify(v, null, 2)
    },
    async task(fn) {
      if (this.busy) return
      this.busy = true
      this.error = ''
      try {
        await fn()
      } catch (e) {
        this.error = e.message
      } finally {
        this.busy = false
      }
    },
    async recommend() {
      await this.task(async () => {
        this.candidates = (
          await this.$axios.post('/api/parking/recommend', { preference: this.preference })
        ).data.data.candidates
      })
    },
    async reverse() {
      this.location = null
      await this.task(async () => {
        this.location = (
          await this.$axios.post('/api/parking/reverse', {
            numberplate: this.numberplate,
            now_location: this.nowLocation,
          })
        ).data.data
      })
    },
    async navigate() {
      this.routes = []
      await this.task(async () => {
        const { data } = await this.$axios.post('/api/parking/route', this.route)
        if (!Array.isArray(data.data.message)) throw new Error('工作流未返回路线列表')
        this.routes = data.data.message
      })
    },
  },
}
</script>
