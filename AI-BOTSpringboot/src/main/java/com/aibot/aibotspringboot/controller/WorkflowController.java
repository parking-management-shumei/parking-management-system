package com.aibot.aibotspringboot.controller;

import com.aibot.aibotspringboot.common.*;
import com.aibot.aibotspringboot.service.CozeService;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parking")
public class WorkflowController {
  private final CozeService coze;

  public WorkflowController(CozeService c) {
    coze = c;
  }

  @GetMapping("/integrations")
  public Result status() {
    return Result.success(
        Map.of(
            "billing",
            coze.configured("billing"),
            "reverse",
            coze.configured("reverse"),
            "route",
            coze.configured("route")));
  }

  @PostMapping("/route")
  public Result route(@RequestBody Map<String, Double> input) {
    for (String key : List.of("start_lat", "start_lng", "end_lat", "end_lng")) {
      Double v = input.get(key);
      double bound = key.endsWith("lat") ? 90 : 180;
      if (v == null || !Double.isFinite(v) || Math.abs(v) > bound)
        throw new ApiException(400, "经纬度无效");
    }
    Map<String, Object> parameters = new LinkedHashMap<>();
    for (String key : List.of("start_lat", "start_lng", "end_lat", "end_lng"))
      parameters.put(key, input.get(key));
    return Result.success(coze.workflow("route", parameters));
  }
}
