package com.aibot.aibotspringboot.service;

import com.aibot.aibotspringboot.entity.ParkingLot;
import java.math.*;
import org.springframework.stereotype.Service;

@Service
public class BillingService {
  // Free time is a grace threshold, not deducted. Each started hour is billed; each 24h block is
  // capped.
  public BigDecimal calculate(long seconds, ParkingLot lot) {
    if (seconds < 0
        || lot.getFreeMinutes() < 0
        || lot.getFirstPrice().signum() < 0
        || lot.getHourlyPrice().signum() < 0
        || lot.getDailyCap().signum() < 0)
      throw new IllegalArgumentException("Invalid billing input");
    if (seconds <= lot.getFreeMinutes() * 60L) return BigDecimal.ZERO.setScale(2);
    long days = seconds / 86400, remaining = seconds % 86400;
    BigDecimal fullDay =
        lot.getFirstPrice()
            .add(lot.getHourlyPrice().multiply(BigDecimal.valueOf(23)))
            .min(lot.getDailyCap());
    BigDecimal partial =
        remaining == 0
            ? BigDecimal.ZERO
            : lot.getFirstPrice()
                .add(
                    lot.getHourlyPrice()
                        .multiply(BigDecimal.valueOf(Math.max(0, (remaining + 3599) / 3600 - 1))))
                .min(lot.getDailyCap());
    return fullDay
        .multiply(BigDecimal.valueOf(days))
        .add(partial)
        .setScale(2, RoundingMode.HALF_UP);
  }
}
