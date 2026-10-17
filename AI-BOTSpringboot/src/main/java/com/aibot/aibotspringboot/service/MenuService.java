package com.aibot.aibotspringboot.service;

import com.aibot.aibotspringboot.entity.*;
import com.aibot.aibotspringboot.mapper.MenuMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MenuService {
  private final MenuMapper mapper;

  public MenuService(MenuMapper mapper) {
    this.mapper = mapper;
  }

  public List<Menu> forUser(User u) {
    return mapper.selectList(
        new QueryWrapper<Menu>().ge("min_role", u.getRoleId()).orderByAsc("id"));
  }
}
