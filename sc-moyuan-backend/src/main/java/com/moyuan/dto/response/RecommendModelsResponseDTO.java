package com.moyuan.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 模型与可用策略响应（GET /api/recommend/models）。
 * 匿名用户 availableStrategies 固定 ['rule']；
 * 登录用户为 ['rule'] + 有上线版本且该用户有缓存记录的模型策略。
 */
@Data
public class RecommendModelsResponseDTO {
    private List<RecommendModelDTO> models;
    private List<String> availableStrategies;
}
