package com.aibot.aibotspringboot.controller;

import com.aibot.aibotspringboot.common.Result;
import com.aibot.aibotspringboot.service.*;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/menu")
public class MenuController {
  private final MenuService menus;
  private final UserService users;

  public MenuController(MenuService m, UserService u) {
    menus = m;
    users = u;
  }

  @GetMapping("/list")
  public Result list(HttpSession s) {
    return Result.success(menus.forUser(users.current(s)));
  }
}
