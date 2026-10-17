package com.aibot.aibotspringboot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("app_menu")
public class Menu {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String menuName;
  private String menuClick;
  private String menuComponent;
  private String menuIcon;
  private Integer minRole;
}
