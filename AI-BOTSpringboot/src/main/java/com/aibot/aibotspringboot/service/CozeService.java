package com.aibot.aibotspringboot.service;

import com.aibot.aibotspringboot.common.ApiException;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

@Service
public class CozeService {
  private final Environment env;
  private final RestTemplate http;
  private final ObjectMapper json;

  public CozeService(Environment e, RestTemplate h, ObjectMapper j) {
    env = e;
    http = h;
    json = j;
  }

  public boolean configured(String kind) {
    return !env.getProperty("coze.token", "").isBlank()
        && !env.getProperty("coze." + kind + "-workflow-id", "").isBlank();
  }

  public JsonNode workflow(String kind, Map<String, Object> parameters) {
    if (!List.of("billing", "reverse", "route").contains(kind) || !configured(kind))
      throw new ApiException(503, "Coze 工作流尚未配置");
    JsonNode result =
        call(
            "/v1/workflow/run",
            Map.of(
                "workflow_id",
                env.getProperty("coze." + kind + "-workflow-id"),
                "parameters",
                parameters,
                "is_async",
                false),
            HttpMethod.POST);
    JsonNode data = result.path("data");
    try {
      return data.isTextual() ? json.readTree(data.asText()) : data;
    } catch (Exception e) {
      throw new ApiException(502, "Coze 工作流输出必须为 JSON 对象");
    }
  }

  public JsonNode call(String path, Object body, HttpMethod method) {
    String token = env.getProperty("coze.token", "");
    if (token.isBlank()) throw new ApiException(503, "Coze 尚未配置，请使用停车助手的业务功能");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    try {
      JsonNode r =
          http.exchange(
                  env.getProperty("coze.base-url", "https://api.coze.cn") + path,
                  method,
                  new HttpEntity<>(body, headers),
                  JsonNode.class)
              .getBody();
      if (r == null || r.path("code").asInt(-1) != 0)
        throw new ApiException(502, "Coze 调用失败，请检查发布状态、权限与工作流参数");
      return r;
    } catch (RestClientException e) {
      throw new ApiException(502, "Coze 服务暂不可用，请稍后重试");
    }
  }
}
