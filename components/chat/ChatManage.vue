<template>
  <div class="page">
    <h1>AI 停车对话</h1>
    <p class="muted">通过后端调用 Coze；未配置智能体时，请使用停车助手。</p>
    <div class="section messages" aria-live="polite">
      <p v-if="!messages.length" class="muted">请输入停车相关问题。</p>
      <article v-for="m in messages" :key="m.id" :class="m.role">
        <strong>{{ m.role === 'user' ? '你' : '停车助手' }}</strong>
        <p>{{ m.text }}</p>
      </article>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" /><el-input
      v-model="input"
      type="textarea"
      :rows="3"
      maxlength="4000"
      placeholder="输入问题；Ctrl + Enter 发送"
      :disabled="busy"
      @keydown.ctrl.enter.native.prevent="send"
    />
    <div class="toolbar">
      <el-button type="primary" :loading="busy" @click="send">发送</el-button
      ><el-button :disabled="busy" @click="messages = []">清空本页消息</el-button>
    </div>
  </div>
</template>
<script>
let id = 0
export default {
  name: 'ChatPage',
  data: () => ({ input: '', messages: [], busy: false, error: '' }),
  methods: {
    async send() {
      const text = this.input.trim()
      if (this.busy || !text) return
      this.busy = true
      this.error = ''
      this.messages.push({ id: ++id, role: 'user', text })
      this.input = ''
      try {
        const { data } = await this.$axios.post('/api/sendMessage', { message: text })
        this.messages.push({ id: ++id, role: 'assistant', text: data.data.answer })
        this.$nextTick(() => {
          const panel = this.$el.querySelector('.messages')
          panel.scrollTop = panel.scrollHeight
        })
      } catch (e) {
        this.error = e.message
        this.input = text
      } finally {
        this.busy = false
      }
    },
  },
}
</script>
<style scoped>
.messages {
  height: 50vh;
  overflow: auto;
}
article {
  padding: 16px;
  margin-bottom: 12px;
  border-radius: 8px;
  background: #f1f5fa;
}
article.user {
  background: #eaf1ff;
}
article p {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>
