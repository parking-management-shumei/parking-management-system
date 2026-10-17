package com.aibot.aibotspringboot.common;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.*;

@Configuration
public class MybatisPlusConfig {
  @Bean
  public MybatisPlusInterceptor interceptor() {
    var i = new MybatisPlusInterceptor();
    i.addInnerInterceptor(new PaginationInnerInterceptor());
    return i;
  }
}
