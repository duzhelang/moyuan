package com.moyuan.mapper;

import com.moyuan.dto.StatsTrendDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StatsMapper {

    List<StatsTrendDTO> selectStatsTrend(@Param("days") int days);
}
