package com.aibot.aibotspringboot;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.aibot.aibotspringboot.common.ApiException;
import com.aibot.aibotspringboot.service.CozeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class CozeServiceTest {
  @Test
  void workflowUsesServerCredentialsAndUnwrapsStringifiedJson() {
    RestTemplate http = new RestTemplate();
    var server = MockRestServiceServer.bindTo(http).build();
    var env =
        new MockEnvironment()
            .withProperty("coze.token", "test-token")
            .withProperty("coze.billing-workflow-id", "123");
    var coze = new CozeService(env, http, new ObjectMapper());
    server
        .expect(requestTo("https://api.coze.cn/v1/workflow/run"))
        .andExpect(header("Authorization", "Bearer test-token"))
        .andExpect(
            content()
                .json(
                    "{\"workflow_id\":\"123\",\"parameters\":{\"car_number\":\"TEST\"},\"is_async\":false}"))
        .andRespond(
            withSuccess("{\"code\":0,\"data\":\"{\\\"price\\\":5}\"}", MediaType.APPLICATION_JSON));
    assertThat(coze.workflow("billing", Map.of("car_number", "TEST")).path("price").asInt())
        .isEqualTo(5);
    server.verify();
  }

  @Test
  void unconfiguredWorkflowIsExplicit() {
    var c = new CozeService(new MockEnvironment(), new RestTemplate(), new ObjectMapper());
    assertThat(c.configured("reverse")).isFalse();
    assertThatThrownBy(() -> c.workflow("reverse", Map.of())).isInstanceOf(ApiException.class);
  }
}
