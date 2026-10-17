package com.aibot.aibotspringboot.service;

import com.aibot.aibotspringboot.common.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import javax.servlet.http.HttpSession;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

@Service
public class AIService {
  private final CozeService coze;
  private final Environment env;

  public AIService(CozeService c, Environment e) {
    coze = c;
    env = e;
  }

  public String sendMessageToAI(String message, HttpSession session) {
    if (message == null || message.isBlank() || message.length() > 4000)
      throw new ApiException(400, "消息长度须为1–4000字");
    String bot = env.getProperty("coze.bot-id", "");
    if (bot.isBlank()) throw new ApiException(503, "Coze 智能体尚未配置，停车业务功能仍可使用");
    synchronized (session) {
      String user = (String) session.getAttribute("cozeUser");
      if (user == null) {
        user = UUID.randomUUID().toString();
        session.setAttribute("cozeUser", user);
      }
      String conversation = (String) session.getAttribute("cozeConversation");
      JsonNode r =
          coze.call(
                  "/v3/chat" + (conversation == null ? "" : "?conversation_id=" + conversation),
                  Map.of(
                      "bot_id",
                      bot,
                      "user_id",
                      user,
                      "stream",
                      false,
                      "auto_save_history",
                      true,
                      "additional_messages",
                      List.of(Map.of("role", "user", "content", message, "content_type", "text"))),
                  HttpMethod.POST)
              .path("data");
      String id = r.path("id").asText(), cid = r.path("conversation_id").asText();
      if (!id.matches("[0-9]+") || !cid.matches("[0-9]+"))
        throw new ApiException(502, "Coze 对话响应无效");
      session.setAttribute("cozeConversation", cid);
      long deadline = System.nanoTime() + 20_000_000_000L;
      String status = r.path("status").asText();
      while (!status.equals("completed")) {
        if (List.of("failed", "canceled", "requires_action").contains(status))
          throw new ApiException(502, "Coze 对话未完成");
        if (System.nanoTime() > deadline) throw new ApiException(504, "AI 回复超时，请稍后重试");
        try {
          Thread.sleep(250);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new ApiException(503, "请求被中断");
        }
        status =
            coze.call(
                    "/v3/chat/retrieve?conversation_id=" + cid + "&chat_id=" + id,
                    null,
                    HttpMethod.GET)
                .path("data")
                .path("status")
                .asText();
      }
      StringBuilder answer = new StringBuilder();
      for (JsonNode m :
          coze.call(
                  "/v3/chat/message/list?conversation_id=" + cid + "&chat_id=" + id,
                  null,
                  HttpMethod.GET)
              .path("data")) {
        if ("answer".equals(m.path("type").asText()))
          answer.append(m.path("content").asText()).append("\n");
      }
      if (answer.length() == 0) throw new ApiException(502, "AI 未返回文本回复");
      return answer.toString().trim();
    }
  }
}
