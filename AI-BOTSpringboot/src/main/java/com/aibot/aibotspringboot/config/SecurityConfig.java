package com.aibot.aibotspringboot.config;

import com.aibot.aibotspringboot.common.Errors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain chain(HttpSecurity http, ObjectMapper json) throws Exception {
    http.cors()
        .and()
        .csrf()
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        .and()
        .httpBasic()
        .disable()
        .formLogin()
        .disable()
        .logout()
        .disable()
        .authorizeRequests()
        .antMatchers(
            "/api/integration/time",
            "/api/integration/billing",
            "/api/csrf",
            "/api/health",
            "/user/login",
            "/user/save",
            "/user/findByNo")
        .permitAll()
        .anyRequest()
        .authenticated()
        .and()
        .exceptionHandling()
        .authenticationEntryPoint(
            (req, res, e) -> {
              res.setStatus(401);
              res.setContentType("application/json;charset=UTF-8");
              json.writeValue(res.getWriter(), Errors.error(401, "请先登录"));
            })
        .accessDeniedHandler(
            (req, res, e) -> {
              res.setStatus(403);
              res.setContentType("application/json;charset=UTF-8");
              json.writeValue(res.getWriter(), Errors.error(403, "权限不足或会话校验失败，请刷新重试"));
            });
    return http.build();
  }

  @Bean
  public CorsConfigurationSource cors(@Value("${app.allowed-origins}") String origins) {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(Arrays.asList(origins.split(",")));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
    c.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
    s.registerCorsConfiguration("/**", c);
    return s;
  }
}
