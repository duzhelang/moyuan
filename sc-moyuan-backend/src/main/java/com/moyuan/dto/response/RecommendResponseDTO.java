package com.moyuan.dto.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 模型策略推荐响应（GET /api/recommend）。
 * 缓存未命中/版本下线/过滤后为空时仍返回 HTTP 200：
 * fallback=true、strategy=rule、items 为规则推荐结果，由前端静默切回规则。
 */
@Data
public class RecommendResponseDTO {
    /** 前端请求的策略（可能非法或不可用） */
    private String requestedStrategy;
    /** 实际生效的策略（回退时为 rule） */
    private String strategy;
    private boolean fallback;
    /** cache-miss / model-offline / empty */
    private String fallbackReason;
    private String modelVersion;
    /** 离线指标（recall10/ndcg10/precision10/coverage） */
    private Map<String, Double> metrics;
    private List<String> availableStrategies;
    private List<RecommendItemDTO> items;
}
