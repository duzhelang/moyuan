package com.moyuan.service;

import java.util.Map;

/**
 * 用户分析扩展指标：角色结构、活跃与回访留存。
 */
public interface UserAnalyticsService {

    /**
     * 用户结构与活跃留存。
     * 回访口径：注册满 N 天、且最近 N 天有登录记录的用户 / 注册满 N 天的用户。
     */
    Map<String, Object> structure();

    /**
     * 用户活跃热力图：星期 × 小时的请求量分布，数据来自 visit_log。
     *
     * @param days 统计窗口天数
     */
    Map<String, Object> activityHeatmap(int days);
}
