package com.aibot.aibotspringboot.config;

import com.aibot.aibotspringboot.entity.ParkingLot;
import com.aibot.aibotspringboot.mapper.ParkingLotMapper;
import java.math.BigDecimal;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;

@Configuration
public class ParkingBootstrap {
  @Bean
  ApplicationRunner seedParking(ParkingLotMapper m) {
    return args -> {
      if (m.selectCount(null) == 0) {
        ParkingLot p = new ParkingLot();
        p.setName("校园演示停车场");
        p.setAddress("演示数据，请按实际场地配置");
        p.setCapacity(100);
        p.setFirstPrice(new BigDecimal("5.00"));
        p.setHourlyPrice(new BigDecimal("3.00"));
        p.setDailyCap(new BigDecimal("20.00"));
        p.setFreeMinutes(15);
        m.insert(p);
      }
    };
  }
}
