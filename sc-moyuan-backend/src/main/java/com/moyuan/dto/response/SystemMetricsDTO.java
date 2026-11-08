package com.moyuan.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 接口调用指标汇总：错误率 / P95 / 慢接口 TOP。
 * 数据源为 api_request_metric 表，时间窗由 hours 参数控制（默认 24 小时）。
 */
@Data
public class SystemMetricsDTO {

    /** 统计时间窗（小时） */
    private int windowHours;

    /** 表不可用（未执行迁移）时为 false，前端据此提示「待接入」而不是显示 0 */
    private boolean available = true;

    private String message;

    private long total;

    private long errorCount;

    /** 错误率，0~1 */
    private double errorRate;

    private long avgMs;

    private Long p95Ms;

    private List<SlowApi> slowTop = new ArrayList<>();

    /** 按时间桶聚合的请求量 / 错误数 / 平均耗时，供 24 小时趋势图 */
    private List<HourlyPoint> hourly = new ArrayList<>();

    /** 按状态码区间聚合的请求分布（2xx/3xx/4xx/5xx） */
    private List<StatusBucket> statusBuckets = new ArrayList<>();

    @Data
    public static class SlowApi {
        private String path;
        private long count;
        private long avgMs;
        private long maxMs;
    }

    @Data
    public static class HourlyPoint {
        /** 时间桶标签，如 11-08 14:00 */
        private String label;
        private long count;
        private long errorCount;
        private long avgMs;
    }

    @Data
    public static class StatusBucket {
        private String name;
        private long count;
    }
}
