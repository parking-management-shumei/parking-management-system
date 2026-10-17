package com.aibot.aibotspringboot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("parking_order")
public class ParkingOrder {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long recordId;
  private Long userId;
  private Long lotId;
  private java.math.BigDecimal amount;
  private String status;
  private String billingSource;
  private String workflowTrace;
  private java.time.LocalDateTime createdAt;
  private java.time.LocalDateTime paidAt;
}
