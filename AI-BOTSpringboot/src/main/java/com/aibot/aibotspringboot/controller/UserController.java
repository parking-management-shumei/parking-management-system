package com.aibot.aibotspringboot.controller;

import com.aibot.aibotspringboot.common.*;
import com.aibot.aibotspringboot.entity.User;
import com.aibot.aibotspringboot.service.*;
import java.util.*;
import javax.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {
  private final UserService users;
  private final MenuService menus;

  public UserController(UserService u, MenuService m) {
    users = u;
    menus = m;
  }

  @GetMapping("/api/csrf")
  public Result csrf(CsrfToken token) {
    return Result.success(Map.of("token", token.getToken()));
  }

  @GetMapping("/api/health")
  public Result health() {
    return Result.success(Map.of("status", "UP"));
  }

  @PostMapping("/user/login")
  public Result login(@RequestBody Map<String, String> input, HttpServletRequest req) {
    User u = users.authenticate(input);
    HttpSession previous = req.getSession(false);
    if (previous != null) previous.invalidate();
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            u.getId().toString(), null, AuthorityUtils.createAuthorityList("ROLE_USER")));
    SecurityContextHolder.setContext(context);
    HttpSession s = req.getSession(true);
    s.setAttribute("userId", u.getId());
    s.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    return Result.success(Map.of("user", u, "menu", menus.forUser(u)));
  }

  @GetMapping("/user/me")
  public Result me(HttpSession s) {
    User u = users.current(s);
    return Result.success(Map.of("user", u, "menu", menus.forUser(u)));
  }

  @PostMapping("/user/logout")
  public Result logout(HttpServletRequest r) {
    HttpSession s = r.getSession(false);
    if (s != null) s.invalidate();
    SecurityContextHolder.clearContext();
    return Result.success();
  }

  @PostMapping("/user/save")
  public Result save(@RequestBody User u, HttpSession s) {
    return Result.success(
        users.create(u, s.getAttribute("userId") == null ? null : users.current(s)));
  }

  @PostMapping("/user/update")
  public Result update(@RequestBody User u, HttpSession s) {
    return Result.success(users.update(u, users.current(s)));
  }

  @GetMapping("/user/list")
  public Result list(HttpSession s) {
    return Result.success(users.list(users.current(s)));
  }

  @DeleteMapping("/user/del")
  public Result del(@RequestParam Long id, HttpSession s) {
    users.delete(id, users.current(s));
    return Result.success();
  }

  @GetMapping("/user/findByNo")
  public Result exists(@RequestParam String no) {
    return users.findByNo(no) == null ? Result.fail() : Result.success(Map.of("exists", true));
  }

  @PostMapping("/user/listPageC1")
  public Result page(@RequestBody QueryPageParam body, HttpSession s) {
    var p =
        users.page(
            body.getPageNum(),
            body.getPageSize(),
            body.getParam() == null ? Collections.emptyMap() : body.getParam(),
            users.current(s));
    return Result.success(p.getRecords(), p.getTotal());
  }
}
