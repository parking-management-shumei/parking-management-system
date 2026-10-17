package com.aibot.aibotspringboot.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.*;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {
  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder b) {
    return b.setConnectTimeout(Duration.ofSeconds(3)).setReadTimeout(Duration.ofSeconds(8)).build();
  }
}
