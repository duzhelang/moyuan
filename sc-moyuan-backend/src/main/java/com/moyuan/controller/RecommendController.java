package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.common.ResultCode;
import com.moyuan.dto.request.RecommendEventRequest;
import com.moyuan.dto.response.RecommendModelsResponseDTO;
import com.moyuan.dto.response.RecommendResponseDTO;
import com.moyuan.dto.response.RecommendStatsDTO;
import com.moyuan.exception.BusinessException;
import com.moyuan.service.MlRecommendationService;
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 模型策略推荐接口。
 * GET /api/recommend 需要 JWT（身份以 JWT 为准，不接受前端传 userId）；
 * GET /api/recommend/models 公开可选 JWT（匿名 availableStrategies=['rule']）；
 * POST /api/recommend/events 需要 JWT。规则策略不在本控制器：
 * strategy=rule 一律走现有公开接口 /api/search/poems/recommended。
 */
@Tag(name = "推荐模型接口")
@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private static final Set<String> MODEL_STRATEGIES = Set.of("era", "genre", "fusion", "global");

    private final MlRecommendationService mlRecommendationService;

    @Operation(summary = "模型策略推荐（缓存查表，失败回退规则）")
    @GetMapping
    public R<RecommendResponseDTO> recommend(
            @RequestParam String strategy,
            @RequestParam(defaultValue = "6") int topK) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (!MODEL_STRATEGIES.contains(strategy)) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        return R.success(mlRecommendationService.recommend(userId, strategy, topK));
    }

    @Operation(summary = "上线模型与当前用户可用策略（匿名只返回规则）")
    @GetMapping("/models")
    public R<RecommendModelsResponseDTO> models() {
        Long userId = null;
        try {
            userId = SecurityUtil.getCurrentUserId();
        } catch (Exception ignored) {
            // 匿名访问，availableStrategies 只含 rule
        }
        return R.success(mlRecommendationService.getModels(userId));
    }

    @Operation(summary = "推荐策略切换埋点（失败静默）")
    @PostMapping("/events")
    public R<Void> recordEvent(@Valid @RequestBody RecommendEventRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        mlRecommendationService.recordEvent(userId, request.getPage(), request.getFrom(), request.getTo());
        return R.success();
    }

    @Operation(summary = "推荐模块实时统计（管理端指标屏，缓存与事件表的当前状态）")
    @GetMapping("/stats")
    public R<RecommendStatsDTO> stats() {
        return R.success(mlRecommendationService.getLiveStats());
    }
}
