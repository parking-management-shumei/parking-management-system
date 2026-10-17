package com.aibot.aibotspringboot;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aibot.aibotspringboot.common.ApiException;
import com.aibot.aibotspringboot.entity.*;
import com.aibot.aibotspringboot.mapper.*;
import com.aibot.aibotspringboot.service.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:workflowtest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "app.admin-password=Test-admin-2026!"
    })
class BillingWorkflowIntegrationTest {
  @MockBean CozeService coze;
  @Autowired ParkingService service;
  @Autowired UserMapper users;
  @Autowired JdbcTemplate db;
  @Autowired ObjectMapper json;

  @Test
  void invalidWorkflowFeeRollsBackAndValidFeeCreatesOneOrder() throws Exception {
    User actor = users.selectOne(new QueryWrapper<User>().eq("no", "admin"));
    VehicleRecord entry = new VehicleRecord();
    entry.setLotId(1L);
    entry.setUserId(actor.getId());
    entry.setNumberplate("京AWF001");
    entry.setSpaceCode("B1-WF001");
    VehicleRecord record = service.enter(entry, actor);
    db.update(
        "update vehicle_record set entry_time=? where id=?",
        LocalDateTime.now().minusMinutes(90),
        record.getId());
    when(coze.configured("billing")).thenReturn(true);
    when(coze.workflow(eq("billing"), anyMap())).thenReturn(json.readTree("{\"price\":100000}"));
    assertThatThrownBy(() -> service.exit(record.getId(), actor)).isInstanceOf(ApiException.class);
    assertThat(
            db.queryForObject(
                "select count(*) from parking_order where record_id=?",
                Integer.class,
                record.getId()))
        .isZero();
    assertThat(
            db.queryForObject(
                "select exit_time from vehicle_record where id=?",
                LocalDateTime.class,
                record.getId()))
        .isNull();
    when(coze.workflow(eq("billing"), anyMap())).thenReturn(json.readTree("{\"price\":8}"));
    ParkingOrder order = service.exit(record.getId(), actor);
    assertThat(order.getBillingSource()).isEqualTo("coze");
    assertThat(order.getAmount()).isEqualByComparingTo("8.00");
    assertThat(service.exit(record.getId(), actor).getId()).isEqualTo(order.getId());
    verify(coze, times(2))
        .workflow("billing", Map.of("car_number", "京AWF001", "numberplate", "京AWF001"));
  }
}
