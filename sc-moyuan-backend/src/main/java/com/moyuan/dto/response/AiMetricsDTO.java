package com.moyuan.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 调用指标：失败率 / 平均耗时 / P95 / 首字延迟 / 分入口明细。
 * 数据源为 ai_call_log，时间窗由 hours 控制（默认 24 小时）。
 */
@Data
public class AiMetricsDTO {

    private int windowHours;

    private boolean available = true;

    private String message;

    private long total;

    private long errorCount;

    /** 失败率，0~1 */
    private double errorRate;

    private long avgMs;

    private Long p95Ms;

    /** 平均首字延迟（仅流式接口），无样本时为 null */
    private Long ttftAvgMs;

    /** 流式接口样本数，用于判断 ttftAvgMs 是否可信 */
    private long streamCount;

    private List<EndpointRow> byEndpoint = new ArrayList<>();

    private List<ModelRow> byModel = new ArrayList<>();

    @Data
    public static class EndpointRow {
        private String endpoint;
        private long count;
        private long errorCount;
        private long avgMs;
    }

    @Data
    public static class ModelRow {
        private String model;
        private long count;
        private long errorCount;
    }
}
