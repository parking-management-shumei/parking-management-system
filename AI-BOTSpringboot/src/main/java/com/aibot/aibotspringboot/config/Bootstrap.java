package com.aibot.aibotspringboot.config;

import com.aibot.aibotspringboot.entity.*;
import com.aibot.aibotspringboot.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class Bootstrap {
  @Bean
  ApplicationRunner seed(
      UserMapper users,
      MenuMapper menus,
      PasswordEncoder encoder,
      @Value("${app.admin-password:}") String pass) {
    return args -> {
      if (!pass.isBlank() && users.selectCount(new QueryWrapper<User>().eq("role_id", 0)) == 0) {
        if (pass.length() < 12)
          throw new IllegalArgumentException("ADMIN_PASSWORD must contain at least 12 characters");
        User u = new User();
        u.setNo("admin");
        u.setName("停车场管理员");
        u.setPassword(encoder.encode(pass));
        u.setRoleId(0);
        users.insert(u);
      }
      if (menus.selectCount(null) == 0) {
        String[][] items = {
          {"停车运营", "operations", "parking/Operations.vue", "el-icon-data-line", "1"},
          {"统计报表", "reports", "parking/Reports.vue", "el-icon-pie-chart", "1"},
          {"停车助手", "parking", "parking/ParkingAssistant.vue", "el-icon-location", "2"},
          {"AI 对话", "chat", "chat/ChatManage.vue", "el-icon-chat-dot-round", "2"},
          {"用户管理", "user", "user/UserManage.vue", "el-icon-user", "1"},
          {"管理员管理", "admin", "admin/AdminManage.vue", "el-icon-setting", "0"},
          {"个人中心", "Home", "Home.vue", "el-icon-s-custom", "2"}
        };
        for (String[] a : items) {
          Menu m = new Menu();
          m.setMenuName(a[0]);
          m.setMenuClick(a[1]);
          m.setMenuComponent(a[2]);
          m.setMenuIcon(a[3]);
          m.setMinRole(Integer.valueOf(a[4]));
          menus.insert(m);
        }
      }
    };
  }
}
