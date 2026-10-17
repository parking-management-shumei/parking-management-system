<template>
  <div class="page">
    <h1>让停车与寻车更清楚</h1>
    <p class="muted">业务记录存入数据库，停车信息与 AI 工作流各司其职。</p>
    <div class="metrics">
      <div class="metric">
        <span>查找停车场</span>
        <p>查看实时登记占用和收费标准</p>
        <el-button type="primary" @click="$router.push('/parking')">打开停车助手</el-button>
      </div>
      <div class="metric">
        <span>AI 对话</span>
        <p>配置 Coze 智能体后可使用自然语言问答</p>
        <el-button @click="$router.push('/chat')">打开对话</el-button>
      </div>
      <div v-if="admin" class="metric">
        <span>停车运营</span>
        <p>登记入场、离场、订单及人工收款</p>
        <el-button @click="$router.push('/operations')">进入运营页</el-button>
      </div>
    </div>
    <el-alert
      title="首次启动的校园停车场是演示配置，占用来自系统登记记录，并非传感器实时测量。请按实际场地调整。"
      type="info"
      :closable="false"
    />
  </div>
</template>
<script>
export default {
  name: 'ParkingHome',
  computed: {
    admin() {
      return (JSON.parse(sessionStorage.getItem('CurUser')) || {}).roleId < 2
    },
  },
}
</script>
