package com.aibot.aibotspringboot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("app_user")
public class User {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String no;
  private String name;

  @com.fasterxml.jackson.annotation.JsonProperty(
      access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
  private String password;

  private Integer age;
  private Integer sex;
  private String phone;
  private Integer roleId;
}
