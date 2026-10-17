package com.aibot.aibotspringboot.controller;

import com.aibot.aibotspringboot.common.ApiException;
import com.aibot.aibotspringboot.entity.*;
import com.aibot.aibotspringboot.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

// Narrow, read-only callbacks for the two legacy Coze billing code nodes.
@RestController
@RequestMapping("/api/integration")
public class IntegrationController {
  private final VehicleRecordMapper records;
  private final ParkingLotMapper lots;
  private final String token;

  public IntegrationController(
      VehicleRecordMapper r, ParkingLotMapper l, @Value("${coze.service-token:}") String t) {
    records = r;
    lots = l;
    token = t;
  }

  private VehicleRecord active(String plate, String header) {
    if (token.length() < 32) throw new ApiException(503, "工作流数据接口未配置");
    String actual = header != null && header.startsWith("Bearer ") ? header.substring(7) : "";
    if (!MessageDigest.isEqual(
        token.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8)))
      throw new ApiException(401, "工作流凭据无效");
    VehicleRecord r =
        records.selectOne(
            new QueryWrapper<VehicleRecord>().eq("numberplate", plate).isNull("exit_time"));
    if (r == null) throw new ApiException(404, "没有有效停车记录");
    return r;
  }

  @GetMapping("/time")
  public Map<String, Object> time(
      @RequestParam String car_number,
      @RequestHeader(value = "Authorization", required = false) String header) {
    VehicleRecord r = active(car_number, header);
    return Map.of(
        "time", Math.max(0, Duration.between(r.getEntryTime(), LocalDateTime.now()).getSeconds()));
  }

  @GetMapping("/billing")
  public Map<String, Object> billing(
      @RequestParam String numberplate,
      @RequestHeader(value = "Authorization", required = false) String header) {
    VehicleRecord r = active(numberplate, header);
    ParkingLot l = lots.selectById(r.getLotId());
    return Map.of(
        "reg1",
        l.getFirstPrice(),
        "reg2",
        l.getDailyCap(),
        "reg3",
        l.getHourlyPrice(),
        "unit",
        60,
        "free_time",
        l.getFreeMinutes());
  }
}
