package com.aibot.aibotspringboot;

import static org.assertj.core.api.Assertions.*;

import com.aibot.aibotspringboot.entity.ParkingLot;
import com.aibot.aibotspringboot.service.BillingService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BillingServiceTest {
  @Test
  void freeThresholdStartedHoursAndDailyCaps() {
    ParkingLot l = new ParkingLot();
    l.setFreeMinutes(15);
    l.setFirstPrice(new BigDecimal("5"));
    l.setHourlyPrice(new BigDecimal("3"));
    l.setDailyCap(new BigDecimal("20"));
    BillingService b = new BillingService();
    long[] seconds = {0, 900, 901, 3600, 3601, 86400, 86401, 172800};
    String[] expected = {"0.00", "0.00", "5.00", "5.00", "8.00", "20.00", "25.00", "40.00"};
    for (int i = 0; i < seconds.length; i++)
      assertThat(b.calculate(seconds[i], l)).isEqualByComparingTo(expected[i]);
    assertThatThrownBy(() -> b.calculate(-1, l)).isInstanceOf(IllegalArgumentException.class);
  }
}
