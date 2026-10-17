package com.aibot.aibotspringboot;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:parkingtest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "app.admin-password=Test-admin-2026!",
      "coze.token=",
      "coze.service-token=integration-token-with-at-least-32-characters"
    })
@AutoConfigureMockMvc
class ParkingIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate db;

  private String name() {
    return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }

  private JsonNode node(MvcResult r) throws Exception {
    return json.readTree(r.getResponse().getContentAsString());
  }

  private MvcResult postJson(String url, Object body, MockHttpSession s) throws Exception {
    var request =
        post(url)
            .with(csrf())
            .contentType("application/json")
            .content(json.writeValueAsString(body));
    if (s != null) request.session(s);
    return mvc.perform(request).andReturn();
  }

  private MockHttpSession login(String no, String password) throws Exception {
    MvcResult r = postJson("/user/login", Map.of("no", no, "password", password), null);
    assertThat(r.getResponse().getStatus()).isEqualTo(200);
    assertThat(node(r).path("data").path("user").has("password")).isFalse();
    return (MockHttpSession) r.getRequest().getSession(false);
  }

  private JsonNode register(String no) throws Exception {
    MvcResult r =
        postJson(
            "/user/save",
            Map.of("no", no, "name", "测试车主", "password", "User-password-2026!", "roleId", 0),
            null);
    assertThat(r.getResponse().getStatus()).isEqualTo(200);
    JsonNode u = node(r).path("data");
    assertThat(u.path("roleId").asInt()).isEqualTo(2);
    assertThat(u.has("password")).isFalse();
    return u;
  }

  @Test
  void sessionsCsrfAndRoleBoundaries() throws Exception {
    mvc.perform(get("/user/list")).andExpect(status().isUnauthorized());
    mvc.perform(post("/user/login").contentType("application/json").content("{}"))
        .andExpect(status().isForbidden());
    String no = name();
    register(no);
    MockHttpSession user = login(no, "User-password-2026!");
    mvc.perform(get("/user/me").session(user))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.user.no").value(no));
    mvc.perform(get("/user/list").session(user)).andExpect(status().isForbidden());
    mvc.perform(get("/menu/list").param("roleId", "0").session(user))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[?(@.menuClick == 'admin')]").isEmpty());
    MvcResult escalation =
        postJson(
            "/user/update",
            Map.of(
                "id",
                db.queryForObject("select id from app_user where no=?", Long.class, no),
                "name",
                "升级",
                "roleId",
                0),
            user);
    assertThat(escalation.getResponse().getStatus()).isEqualTo(403);
    mvc.perform(
            post("/api/parking/entry")
                .session(user)
                .with(csrf())
                .contentType("application/json")
                .content("{\"lotId\":1}"))
        .andExpect(status().isForbidden());
    mvc.perform(post("/user/logout").session(user).with(csrf())).andExpect(status().isOk());
    mvc.perform(get("/user/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void completeUserCrudAndPagination() throws Exception {
    MockHttpSession admin = login("admin", "Test-admin-2026!");
    String no = name();
    MvcResult created =
        postJson(
            "/user/save",
            Map.of("no", no, "name", "新用户", "password", "User-password-2026!", "roleId", 2),
            admin);
    assertThat(created.getResponse().getStatus()).isEqualTo(200);
    long id = node(created).path("data").path("id").asLong();
    mvc.perform(get("/user/findByNo").param("no", no))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.exists").value(true))
        .andExpect(jsonPath("$.data.password").doesNotExist());
    MvcResult updated =
        postJson("/user/update", Map.of("id", id, "name", "修改后", "password", ""), admin);
    assertThat(updated.getResponse().getStatus()).isEqualTo(200);
    MvcResult page =
        postJson(
            "/user/listPageC1",
            Map.of("pageNum", 1, "pageSize", 10, "param", Map.of("name", "修改后")),
            admin);
    assertThat(node(page).path("total").asInt()).isGreaterThanOrEqualTo(1);
    mvc.perform(get("/user/list").session(admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].password").doesNotExist());
    mvc.perform(delete("/user/del").param("id", Long.toString(id)).session(admin).with(csrf()))
        .andExpect(status().isOk());
    assertThat(db.queryForObject("select count(*) from app_user where id=?", Integer.class, id))
        .isZero();
  }

  @Test
  void durableParkingOrdersReportsAndOwnership() throws Exception {
    MockHttpSession admin = login("admin", "Test-admin-2026!");
    String no = name();
    long userId = register(no).path("id").asLong();
    MockHttpSession user = login(no, "User-password-2026!");
    String plate = "京A" + name().substring(1, 7).toUpperCase();
    String space = "B1-" + name();
    Map<String, Object> entry =
        Map.of("lotId", 1, "userId", userId, "numberplate", plate, "spaceCode", space);
    MvcResult entered = postJson("/api/parking/entry", entry, admin);
    assertThat(entered.getResponse().getStatus()).isEqualTo(200);
    long record = node(entered).path("data").path("id").asLong();
    assertThat(
            db.queryForObject(
                "select count(*) from vehicle_record where id=?", Integer.class, record))
        .isEqualTo(1);
    assertThat(postJson("/api/parking/entry", entry, admin).getResponse().getStatus())
        .isEqualTo(409);
    MvcResult reverse =
        postJson("/api/parking/reverse", Map.of("numberplate", plate, "now_location", "入口"), user);
    assertThat(node(reverse).path("data").path("record").path("spaceCode").asText())
        .isEqualTo(space);
    assertThat(node(reverse).path("data").path("source").asText()).isEqualTo("local");
    String otherNo = name();
    register(otherNo);
    MockHttpSession other = login(otherNo, "User-password-2026!");
    assertThat(
            postJson("/api/parking/reverse", Map.of("numberplate", plate), other)
                .getResponse()
                .getStatus())
        .isEqualTo(404);
    db.update(
        "update vehicle_record set entry_time=? where id=?",
        LocalDateTime.now().minusHours(2),
        record);
    MvcResult exited = postJson("/api/parking/exit/" + record, Map.of(), admin);
    assertThat(exited.getResponse().getStatus()).isEqualTo(200);
    long order = node(exited).path("data").path("id").asLong();
    assertThat(node(exited).path("data").path("status").asText()).isEqualTo("UNPAID");
    assertThat(
            node(postJson("/api/parking/exit/" + record, Map.of(), admin))
                .path("data")
                .path("id")
                .asLong())
        .isEqualTo(order);
    assertThat(
            db.queryForObject(
                "select active_plate from vehicle_record where id=?", String.class, record))
        .isNull();
    mvc.perform(get("/api/parking/orders").session(other)).andExpect(jsonPath("$.data").isEmpty());
    mvc.perform(get("/api/parking/records").param("active", "true").session(user))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    double before =
        node(mvc.perform(get("/api/parking/overview").session(admin)).andReturn())
            .path("data")
            .path("revenue")
            .asDouble();
    MvcResult paid = postJson("/api/parking/orders/" + order + "/paid", Map.of(), admin);
    assertThat(node(paid).path("data").path("status").asText()).isEqualTo("PAID");
    postJson("/api/parking/orders/" + order + "/paid", Map.of(), admin);
    double after =
        node(mvc.perform(get("/api/parking/overview").session(admin)).andReturn())
            .path("data")
            .path("revenue")
            .asDouble();
    assertThat(after - before).isEqualTo(node(paid).path("data").path("amount").asDouble());
    mvc.perform(
            get("/api/parking/reports")
                .param("start", "2026-10-20")
                .param("end", "2026-10-17")
                .session(admin))
        .andExpect(status().isBadRequest());
    assertThat(postJson("/api/parking/entry", entry, admin).getResponse().getStatus())
        .isEqualTo(200);
  }

  @Test
  void integrationCallbacksAndMissingConfiguration() throws Exception {
    MockHttpSession admin = login("admin", "Test-admin-2026!");
    mvc.perform(get("/api/parking/integrations").session(admin))
        .andExpect(jsonPath("$.data.route").value(false));
    assertThat(
            postJson(
                    "/api/parking/route",
                    Map.of("start_lat", 28, "start_lng", 112, "end_lat", 29, "end_lng", 113),
                    admin)
                .getResponse()
                .getStatus())
        .isEqualTo(503);
    assertThat(
            postJson(
                    "/api/parking/route",
                    Map.of("start_lat", 928, "start_lng", 112, "end_lat", 29, "end_lng", 113),
                    admin)
                .getResponse()
                .getStatus())
        .isEqualTo(400);
    assertThat(
            postJson("/api/sendMessage", Map.of("message", "你好"), admin).getResponse().getStatus())
        .isEqualTo(503);
    mvc.perform(get("/api/integration/time").param("car_number", "京A00000"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/integration/time")
                .param("car_number", "京A00000")
                .header("Authorization", "Bearer integration-token-with-at-least-32-characters"))
        .andExpect(status().isNotFound());
  }
}
