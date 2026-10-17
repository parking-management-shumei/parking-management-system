package com.aibot.aibotspringboot.mapper;

import com.aibot.aibotspringboot.entity.ParkingLot;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ParkingLotMapper extends BaseMapper<ParkingLot> {
  @org.apache.ibatis.annotations.Select("SELECT * FROM parking_lot WHERE id = #{id} FOR UPDATE")
  ParkingLot lock(Long id);
}
