package com.moyuan.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 推荐模块实时统计（管理端指标屏的"活"数据）。
 * 缓存行数/覆盖用户来自 recommendation_cache，事件数据来自 recommendation_event，
 * 模型版本与指标来自 recommendation_model_version，均为查询时刻的当前值。
 */
@Data
public class RecommendStatsDTO {
    /** 缓存推荐行总数 */
    private Long cacheRows;
    /** 缓存覆盖用户数 */
    private Long cacheUsers;
    /** 缓存覆盖作品数 */
    private Long cachePoems;
    /** 缓存覆盖策略数 */
    private Long cacheStrategies;
    /** 线上古典推荐池规模（classical + 上架） */
    private Long poolSize;
    /** 策略切换事件总数 */
    private Long eventTotal;
    /** 今日（0 点起）策略切换事件数 */
    private Long eventToday;
    /** 最近一次事件时间，无事件时为 null */
    private LocalDateTime lastEventTime;
    /** 四个模型策略的版本与缓存状态 */
    private List<ModelStat> models;
    /** 近 7 天切换事件数（缺失日期补 0） */
    private List<DayCount> eventTrend;
    /** 累计切换去向分布（to_strategy） */
    private List<NameCount> eventByStrategy;
    /** 累计切换来源页面分布（page） */
    private List<NameCount> eventByPage;
    /** 最近 10 条切换事件 */
    private List<EventItem> recentEvents;

    @Data
    public static class ModelStat {
        private String strategy;
        private String version;
        private Integer status;
        private String metricsPool;
        private Map<String, Double> metrics;
        private Long cacheRows;
        private Long cacheUsers;
        private LocalDateTime createTime;
    }

    @Data
    public static class DayCount {
        private String date;
        private Long count;
    }

    @Data
    public static class NameCount {
        private String name;
        private Long count;
    }

    @Data
    public static class EventItem {
        private LocalDateTime createTime;
        private Long userId;
        private String page;
        private String fromStrategy;
        private String toStrategy;
    }
}
