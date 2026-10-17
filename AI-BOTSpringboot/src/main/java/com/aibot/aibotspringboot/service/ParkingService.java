package com.aibot.aibotspringboot.service;

import com.aibot.aibotspringboot.common.*;
import com.aibot.aibotspringboot.entity.*;
import com.aibot.aibotspringboot.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingService {
  private final ParkingLotMapper lots;
  private final VehicleRecordMapper records;
  private final ParkingOrderMapper orders;
  private final UserMapper users;
  private final UserService auth;
  private final BillingService billing;
  private final CozeService coze;

  public ParkingService(
      ParkingLotMapper l,
      VehicleRecordMapper r,
      ParkingOrderMapper o,
      UserMapper u,
      UserService a,
      BillingService b,
      CozeService c) {
    lots = l;
    records = r;
    orders = o;
    users = u;
    auth = a;
    billing = b;
    coze = c;
  }

  public List<Map<String, Object>> lots() {
    List<Map<String, Object>> result = new ArrayList<>();
    for (ParkingLot l : lots.selectList(new QueryWrapper<ParkingLot>().orderByAsc("id"))) {
      long occupied =
          records.selectCount(
              new QueryWrapper<VehicleRecord>().eq("lot_id", l.getId()).isNull("exit_time"));
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("lot", l);
      row.put("occupied", occupied);
      row.put("available", Math.max(0, l.getCapacity() - occupied));
      result.add(row);
    }
    return result;
  }

  public List<VehicleRecord> records(User actor, boolean active) {
    var q = new QueryWrapper<VehicleRecord>().orderByDesc("entry_time");
    if (active) q.isNull("exit_time");
    if (actor.getRoleId() == 2) q.eq("user_id", actor.getId());
    q.last("LIMIT 500");
    return records.selectList(q);
  }

  public List<ParkingOrder> orders(User actor) {
    var q = new QueryWrapper<ParkingOrder>().orderByDesc("id");
    if (actor.getRoleId() == 2) q.eq("user_id", actor.getId());
    q.last("LIMIT 500");
    return orders.selectList(q);
  }

  private String plate(String input) {
    String p = input == null ? "" : input.trim().toUpperCase(Locale.ROOT);
    if (!p.matches("[\u4e00-\u9fffA-Z0-9·-]{5,12}")) throw new ApiException(400, "车牌格式无效");
    return p;
  }

  @Transactional
  public VehicleRecord enter(VehicleRecord r, User actor) {
    auth.admin(actor);
    ParkingLot lot = lots.lock(r.getLotId());
    if (lot == null) throw new ApiException(404, "停车场不存在");
    if (records.selectCount(
            new QueryWrapper<VehicleRecord>().eq("lot_id", lot.getId()).isNull("exit_time"))
        >= lot.getCapacity()) throw new ApiException(409, "停车场已满");
    String p = plate(r.getNumberplate());
    User owner = r.getUserId() == null ? null : users.selectById(r.getUserId());
    if (owner == null) throw new ApiException(400, "请选择已注册的车主");
    String space = r.getSpaceCode() == null ? "" : r.getSpaceCode().trim();
    if (space.isBlank() || space.length() > 40) throw new ApiException(400, "请输入车位编号");
    VehicleRecord n = new VehicleRecord();
    n.setLotId(lot.getId());
    n.setUserId(owner.getId());
    n.setNumberplate(p);
    n.setSpaceCode(space);
    n.setEntryTime(LocalDateTime.now());
    n.setActivePlate(p);
    n.setActiveSpace(lot.getId() + ":" + space);
    records.insert(n);
    return n;
  }

  @Transactional
  public ParkingOrder exit(Long id, User actor) {
    auth.admin(actor);
    VehicleRecord before = records.selectById(id);
    if (before == null) throw new ApiException(404, "停车记录不存在");
    ParkingLot lot = lots.lock(before.getLotId());
    VehicleRecord r = records.selectById(id);
    ParkingOrder existing = orders.selectOne(new QueryWrapper<ParkingOrder>().eq("record_id", id));
    if (existing != null) return existing;
    LocalDateTime now = LocalDateTime.now();
    long seconds = Math.max(0, Duration.between(r.getEntryTime(), now).getSeconds());
    BigDecimal fee = billing.calculate(seconds, lot);
    String source = "local";
    String trace = null;
    if (coze.configured("billing")) {
      var output =
          coze.workflow(
              "billing",
              Map.of("car_number", r.getNumberplate(), "numberplate", r.getNumberplate()));
      var amount = output.get("price");
      if (amount == null || !amount.isNumber() || amount.decimalValue().compareTo(fee) != 0)
        throw new ApiException(502, "Coze 计费结果与已发布费率不一致，未生成订单，请核对工作流");
      source = "coze";
      trace = output.toString();
      if (trace.length() > 8000) trace = trace.substring(0, 8000);
    }
    ParkingOrder o = new ParkingOrder();
    o.setRecordId(id);
    o.setLotId(r.getLotId());
    o.setUserId(r.getUserId());
    o.setAmount(fee);
    o.setStatus("UNPAID");
    o.setBillingSource(source);
    o.setWorkflowTrace(trace);
    o.setCreatedAt(now);
    orders.insert(o);
    r.setExitTime(now);
    r.setActivePlate(null);
    r.setActiveSpace(null);
    // MyBatis updateById skips null fields; explicit SQL clears both unique active keys.
    records.update(
        null,
        new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<VehicleRecord>()
            .eq("id", id)
            .set("exit_time", now)
            .set("active_plate", null)
            .set("active_space", null));
    return o;
  }

  @Transactional
  public ParkingOrder paid(Long id, User actor) {
    auth.admin(actor);
    ParkingOrder o = orders.lock(id);
    if (o == null) throw new ApiException(404, "订单不存在");
    if (!"PAID".equals(o.getStatus())) {
      o.setStatus("PAID");
      o.setPaidAt(LocalDateTime.now());
      orders.updateById(o);
    }
    return o;
  }

  public Map<String, Object> reverse(String number, String nowLocation, User actor) {
    var q = new QueryWrapper<VehicleRecord>().eq("numberplate", plate(number)).isNull("exit_time");
    if (actor.getRoleId() == 2) q.eq("user_id", actor.getId());
    VehicleRecord r = records.selectOne(q);
    if (r == null) throw new ApiException(404, "未找到有权访问的在场车辆");
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("record", r);
    result.put("lot", lots.selectById(r.getLotId()));
    result.put("source", "local");
    if (coze.configured("reverse")) {
      result.put(
          "workflow",
          coze.workflow(
              "reverse",
              Map.of(
                  "car_number",
                  r.getNumberplate(),
                  "now_location",
                  nowLocation == null ? "" : nowLocation)));
      result.put("source", "coze");
    }
    return result;
  }

  public Map<String, Object> recommend(String preference) {
    if (!List.of("available", "price").contains(preference)) throw new ApiException(400, "推荐偏好无效");
    List<Map<String, Object>> candidates = lots();
    candidates.removeIf(r -> ((Number) r.get("available")).longValue() <= 0);
    candidates.sort(
        (a, b) ->
            preference.equals("price")
                ? ((ParkingLot) a.get("lot"))
                    .getFirstPrice()
                    .compareTo(((ParkingLot) b.get("lot")).getFirstPrice())
                : Long.compare(
                    ((Number) b.get("available")).longValue(),
                    ((Number) a.get("available")).longValue()));
    Map<String, Object> r = new LinkedHashMap<>();
    r.put("candidates", candidates);
    r.put("source", "local");
    return r;
  }

  public Map<String, Object> report(LocalDate start, LocalDate end, User actor) {
    auth.admin(actor);
    if (end.isBefore(start) || ChronoUnitDays(start, end) > 366)
      throw new ApiException(400, "报表区间须为1–367天");
    List<VehicleRecord> arrivals =
        records.selectList(
            new QueryWrapper<VehicleRecord>()
                .ge("entry_time", start.atStartOfDay())
                .lt("entry_time", end.plusDays(1).atStartOfDay()));
    List<VehicleRecord> departures =
        records.selectList(
            new QueryWrapper<VehicleRecord>()
                .ge("exit_time", start.atStartOfDay())
                .lt("exit_time", end.plusDays(1).atStartOfDay()));
    List<ParkingOrder> paid =
        orders.selectList(
            new QueryWrapper<ParkingOrder>()
                .eq("status", "PAID")
                .ge("paid_at", start.atStartOfDay())
                .lt("paid_at", end.plusDays(1).atStartOfDay()));
    List<Map<String, Object>> days = new ArrayList<>();
    BigDecimal total = BigDecimal.ZERO;
    for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
      final LocalDate day = d;
      BigDecimal revenue =
          paid.stream()
              .filter(o -> o.getPaidAt().toLocalDate().equals(day))
              .map(ParkingOrder::getAmount)
              .reduce(BigDecimal.ZERO, BigDecimal::add);
      total = total.add(revenue);
      days.add(
          Map.of(
              "date",
              d.toString(),
              "entries",
              arrivals.stream().filter(r -> r.getEntryTime().toLocalDate().equals(day)).count(),
              "exits",
              departures.stream().filter(r -> r.getExitTime().toLocalDate().equals(day)).count(),
              "revenue",
              revenue));
    }
    return Map.of(
        "days",
        days,
        "revenue",
        total,
        "entries",
        arrivals.size(),
        "exits",
        departures.size(),
        "lots",
        lots(),
        "updatedAt",
        LocalDateTime.now());
  }

  private long ChronoUnitDays(LocalDate a, LocalDate b) {
    return java.time.temporal.ChronoUnit.DAYS.between(a, b);
  }
}
