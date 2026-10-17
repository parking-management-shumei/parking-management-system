package com.aibot.aibotspringboot.controller;

import com.aibot.aibotspringboot.common.Result;
import com.aibot.aibotspringboot.entity.*;
import com.aibot.aibotspringboot.service.*;
import java.time.LocalDate;
import javax.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parking")
public class ParkingController {
  private final ParkingService parking;
  private final UserService users;

  public ParkingController(ParkingService p, UserService u) {
    parking = p;
    users = u;
  }

  @GetMapping("/lots")
  public Result lots() {
    return Result.success(parking.lots());
  }

  @GetMapping("/records")
  public Result records(@RequestParam(defaultValue = "false") boolean active, HttpSession s) {
    return Result.success(parking.records(users.current(s), active));
  }

  @GetMapping("/orders")
  public Result orders(HttpSession s) {
    return Result.success(parking.orders(users.current(s)));
  }

  @PostMapping("/entry")
  public Result entry(@RequestBody VehicleRecord r, HttpSession s) {
    return Result.success(parking.enter(r, users.current(s)));
  }

  @PostMapping("/exit/{id}")
  public Result exit(@PathVariable Long id, HttpSession s) {
    return Result.success(parking.exit(id, users.current(s)));
  }

  @PostMapping("/orders/{id}/paid")
  public Result paid(@PathVariable Long id, HttpSession s) {
    return Result.success(parking.paid(id, users.current(s)));
  }

  @PostMapping("/reverse")
  public Result reverse(@RequestBody java.util.Map<String, String> body, HttpSession s) {
    return Result.success(
        parking.reverse(body.get("numberplate"), body.get("now_location"), users.current(s)));
  }

  @PostMapping("/recommend")
  public Result recommend(@RequestBody java.util.Map<String, String> body) {
    return Result.success(parking.recommend(body.getOrDefault("preference", "available")));
  }

  @GetMapping({"/overview", "/reports"})
  public Result report(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate start,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
      HttpSession s) {
    return Result.success(
        parking.report(
            start == null ? LocalDate.now() : start,
            end == null ? LocalDate.now() : end,
            users.current(s)));
  }
}
