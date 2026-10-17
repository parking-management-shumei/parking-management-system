package com.aibot.aibotspringboot.controller;

import com.aibot.aibotspringboot.common.Result;
import com.aibot.aibotspringboot.service.AIService;
import java.util.Map;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {
  private final AIService ai;

  public ChatController(AIService ai) {
    this.ai = ai;
  }

  @PostMapping("/sendMessage")
  public Result send(@RequestBody Map<String, String> body, HttpSession session) {
    return Result.success(Map.of("answer", ai.sendMessageToAI(body.get("message"), session)));
  }
}
