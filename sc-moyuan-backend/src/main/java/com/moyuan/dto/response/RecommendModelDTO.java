package com.moyuan.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 上线模型信息（GET /api/recommend/models）。
 * metricsPool 固定 classical-670：线上展示的离线指标统一取古典 670 池结果。
 */
@Data
public class RecommendModelDTO {
    private String strategy;
    private String version;
    private String dataFingerprint;
    private String metricsPool;
    /** 离线指标（recall10/ndcg10/precision10/coverage） */
    private Map<String, Double> metrics;
    /** 0=下线，1=上线 */
    private Integer status;
    private LocalDateTime createTime;
}
