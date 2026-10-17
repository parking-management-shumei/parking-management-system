<template>
  <div id="app"><router-view /></div>
</template>
<script>
export default { name: 'App' }
</script>
<style>
html,
body,
#app {
  min-height: 100%;
  margin: 0;
}
body {
  background: #f3f6fa url('./assets/image/背景2.jpg') center / cover fixed;
  font-family: 'Microsoft YaHei', sans-serif;
  color: #20334b;
}
* {
  box-sizing: border-box;
}
.page {
  padding: 24px;
  background: #f5f7fb;
  min-height: 100%;
}
.page h1 {
  margin: 0 0 8px;
  font-size: 26px;
}
.muted {
  color: #62748b;
  line-height: 1.7;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin: 20px 0;
}
.toolbar .el-input {
  width: 200px;
}
.metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 16px;
  margin: 20px 0;
}
.metric {
  background: white;
  padding: 22px;
  border: 1px solid #e4eaf3;
  border-radius: 12px;
}
.metric strong {
  display: block;
  font-size: 30px;
  margin-top: 12px;
  color: #245cbe;
}
.section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin: 20px 0;
  border: 1px solid #e4eaf3;
}
pre {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
@media (max-width: 768px) {
  .page {
    padding: 12px;
  }
  .toolbar .el-input {
    width: 100%;
  }
}
</style>
