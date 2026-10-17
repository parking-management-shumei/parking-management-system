package com.aibot.aibotspringboot.mapper;

import com.aibot.aibotspringboot.entity.ParkingOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ParkingOrderMapper extends BaseMapper<ParkingOrder> {
  @org.apache.ibatis.annotations.Select("SELECT * FROM parking_order WHERE id = #{id} FOR UPDATE")
  ParkingOrder lock(Long id);
}
