package com.aibot.aibotspringboot.service;

import com.aibot.aibotspringboot.common.*;
import com.aibot.aibotspringboot.entity.User;
import com.aibot.aibotspringboot.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.*;
import javax.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private final UserMapper mapper;
  private final PasswordEncoder encoder;

  public UserService(UserMapper m, PasswordEncoder e) {
    mapper = m;
    encoder = e;
  }

  public User current(HttpSession s) {
    Object id = s.getAttribute("userId");
    User u = id == null ? null : mapper.selectById((Long) id);
    if (u == null) throw new ApiException(401, "请先登录");
    return u;
  }

  public void admin(User u) {
    if (u.getRoleId() > 1) throw new ApiException(403, "需要管理员权限");
  }

  public User authenticate(Map<String, String> input) {
    String field = input.get("no") != null ? "no" : "phone";
    String value = input.get(field);
    User u = value == null ? null : mapper.selectOne(new QueryWrapper<User>().eq(field, value));
    if (u == null || !encoder.matches(input.getOrDefault("password", ""), u.getPassword()))
      throw new ApiException(401, "账号或密码错误");
    return u;
  }

  public User findByNo(String no) {
    return mapper.selectOne(new QueryWrapper<User>().eq("no", no));
  }

  @Transactional
  public User create(User input, User actor) {
    input.setId(null);
    validate(input, true);
    if (actor == null) input.setRoleId(2);
    else {
      admin(actor);
      int role = input.getRoleId() == null ? 2 : input.getRoleId();
      if (role < actor.getRoleId() || role > 2 || role < 0) throw new ApiException(403, "无权创建此角色");
      input.setRoleId(role);
    }
    input.setPassword(encoder.encode(input.getPassword()));
    mapper.insert(input);
    return input;
  }

  @Transactional
  public User update(User input, User actor) {
    User old = mapper.selectById(input.getId());
    if (old == null) throw new ApiException(404, "用户不存在");
    boolean self = old.getId().equals(actor.getId());
    if (!self) {
      admin(actor);
      if (old.getRoleId() < actor.getRoleId()) throw new ApiException(403, "无权修改此用户");
    }
    if (input.getRoleId() != null && !input.getRoleId().equals(old.getRoleId())) {
      if (self || actor.getRoleId() != 0 || input.getRoleId() < 0 || input.getRoleId() > 2)
        throw new ApiException(403, "无权修改角色");
      old.setRoleId(input.getRoleId());
    }
    old.setName(input.getName());
    old.setAge(input.getAge());
    old.setSex(input.getSex());
    old.setPhone(input.getPhone());
    if (input.getPassword() != null && !input.getPassword().isBlank()) {
      password(input.getPassword());
      old.setPassword(encoder.encode(input.getPassword()));
    }
    validate(old, false);
    mapper.updateById(old);
    return old;
  }

  public void delete(Long id, User actor) {
    admin(actor);
    User target = mapper.selectById(id);
    if (target == null) throw new ApiException(404, "用户不存在");
    if (id.equals(actor.getId()) || target.getRoleId() < actor.getRoleId())
      throw new ApiException(403, "不能删除自己或更高权限用户");
    mapper.deleteById(id);
  }

  public List<User> list(User actor) {
    admin(actor);
    return mapper.selectList(new QueryWrapper<User>().ge("role_id", actor.getRoleId()));
  }

  public Page<User> page(int num, int size, Map<String, Object> param, User actor) {
    admin(actor);
    if (num < 1 || size < 1 || size > 100) throw new ApiException(400, "分页范围无效");
    var q = new QueryWrapper<User>().ge("role_id", actor.getRoleId());
    for (String key : List.of("name", "sex", "roleId")) {
      Object v = param.get(key);
      if (v != null && !v.toString().isBlank()) {
        if (key.equals("name")) q.like("name", v);
        else q.eq(key.equals("roleId") ? "role_id" : key, v);
      }
    }
    q.orderByDesc("id");
    return mapper.selectPage(new Page<>(num, size), q);
  }

  private void validate(User u, boolean creating) {
    if (u.getNo() == null
        || !u.getNo().matches("[A-Za-z0-9_]{3,32}")
        || u.getName() == null
        || u.getName().isBlank()
        || u.getName().length() > 60) throw new ApiException(400, "账号需为3–32位字母数字下划线，姓名不能为空");
    if (u.getAge() != null && (u.getAge() < 0 || u.getAge() > 150))
      throw new ApiException(400, "年龄无效");
    if (u.getSex() != null && (u.getSex() < 0 || u.getSex() > 1))
      throw new ApiException(400, "性别无效");
    if (u.getPhone() != null && !u.getPhone().isBlank() && !u.getPhone().matches("1[3-9][0-9]{9}"))
      throw new ApiException(400, "手机号格式错误");
    if (u.getPhone() != null && u.getPhone().isBlank()) u.setPhone(null);
    if (creating) password(u.getPassword());
  }

  private void password(String p) {
    if (p == null || p.length() < 8 || p.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) throw new ApiException(400, "密码长度须为8–72位");
  }
}
