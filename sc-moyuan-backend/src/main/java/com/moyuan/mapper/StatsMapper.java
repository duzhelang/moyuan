package com.moyuan.mapper;

import com.moyuan.dto.StatsTrendDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface StatsMapper {

    List<StatsTrendDTO> selectStatsTrend(@Param("days") int days);

    /**
     * 热门诗词：近 N 天收藏数 × 3 + 浏览数，按热度倒序。
     * 只统计有真实收藏/浏览记录的诗词，不把 0 热度内容混进榜单。
     */
    List<Map<String, Object>> selectHotPoems(@Param("days") int days, @Param("limit") int limit);
}
