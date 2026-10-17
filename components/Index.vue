<template>
  <el-container class="main-container">
    <el-aside :width="asideWidth" class="aside-container">
      <Aside :isCollapse="isCollapse"></Aside>
    </el-aside>

    <el-container>
      <el-header class="header-container">
        <Header @doCollapse="toggleCollapse" :icon="icon"></Header>
      </el-header>

      <el-main class="main-content">
        <router-view/>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import Aside from "./Aside";
import Header from "./Header";

export default {
  name: "AppLayout",
  components: { Aside, Header },
  data() {
    return {
      isCollapse: false,
    };
  },
  computed: {
    asideWidth() {
      return this.isCollapse ? '64px' : '200px';
    },
    icon() {
      return this.isCollapse ? 'el-icon-s-unfold' : 'el-icon-s-fold';
    }
  },
  methods: {
    toggleCollapse() {
      this.isCollapse = !this.isCollapse;
    }
  }
};
</script>

<style scoped>
.main-container {
  height: 100%;
  border: 1px solid #eee;
  background-size: cover; /* 使背景图像覆盖整个元素 */
}

.aside-container {
  height: 100%;
  background-color: lightblue;
  margin-left: -1px;
}

.header-container {
  text-align: right;
  font-size: 12px;
  height: 60px; /* 明确头部高度 */
  border-bottom: 1px solid rgba(168, 168, 168, 0.3);
  background-color: #f0f2f5;
  color: #333;
  line-height: 60px;
}

.main-content {
  height: calc(100vh - 60px); /* 减去头部高度 */
  padding: 5px;
}
</style>
