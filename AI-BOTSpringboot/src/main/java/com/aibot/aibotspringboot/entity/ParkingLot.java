package com.aibot.aibotspringboot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("parking_lot")
public class ParkingLot {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String name;
  private String address;
  private Integer capacity;
  private java.math.BigDecimal firstPrice;
  private java.math.BigDecimal hourlyPrice;
  private java.math.BigDecimal dailyCap;
  private Integer freeMinutes;
}
