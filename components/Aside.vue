<template>
  <div class="aside-container" :class="{ 'collapsed': isCollapse }">
  <el-menu
      class="cecece"
      text-color="cadetblue"
      active-text-color="darkslategray"
      style="height: 100%;"
      :default-active="$route.path"
      :collapse="isCollapse"
      :collapse-transition="false"
      router
  >
    <el-menu-item index="/IndexMain">
      <i class="el-icon-s-home"></i>
      <span slot="title" >首页</span>
    </el-menu-item>

    <el-menu-item :index="'/'+item.menuClick" v-for="(item, i) in menu" :key="i">
      <i :class="item.menuIcon"></i>
      <span slot="title">{{item.menuName}}</span>
    </el-menu-item>
  </el-menu>
  </div>
</template>

<script>
export default {
  name: "AppAside",
  props: {
    isCollapse: Boolean
  },
  computed: {
    menu() {
      return this.$store.state.menu;
    }
  },
  methods: {
  }
};
</script>

<style scoped>
.aside-container {
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  width: 200px;
  overflow-y: auto;
  z-index: 1000;
  transition: width 0.2s;
}

.aside-container::-webkit-scrollbar {
  width: 8px;
}

.aside-container::-webkit-scrollbar-thumb {
  background-color: #444;
  border-radius: 4px;
}

.aside-container::-webkit-scrollbar-track {
  background-color: #333;
}

.aside-container.collapsed {
  width: 60px;
}
.cecece{
  background-image: url('../assets/image/侧边栏.jpg');
  opacity: 0.8;
  background-size: cover;
  background-position: center;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  font-family: 微软雅黑, sans-serif;
  font-weight: bold;
}
</style>
