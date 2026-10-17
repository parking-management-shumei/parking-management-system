package com.aibot.aibotspringboot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("vehicle_record")
public class VehicleRecord {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long lotId;
  private Long userId;
  private String numberplate;
  private String spaceCode;
  private String activePlate;
  private String activeSpace;
  private java.time.LocalDateTime entryTime;
  private java.time.LocalDateTime exitTime;
}
