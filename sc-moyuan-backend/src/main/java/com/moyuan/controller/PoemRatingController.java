package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.entity.PoemRating;
import com.moyuan.service.PoemRatingService;
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/poems/{poemId}/ratings")
@RequiredArgsConstructor
@Tag(name = "诗词评分接口")
public class PoemRatingController {

    private final PoemRatingService poemRatingService;
    private final SecurityUtil securityUtil;

    @GetMapping
    @Operation(summary = "获取诗词评分详情")
    public R<Map<String, Object>> getRatings(@PathVariable Long poemId) {
        return R.success(poemRatingService.getPoemRatings(poemId));
    }

    @PostMapping
    @Operation(summary = "用户评分")
    public R<Void> ratePoem(@PathVariable Long poemId,
                            @RequestParam BigDecimal score,
                            @RequestParam(required = false) String comment) {
        if (score.compareTo(BigDecimal.ONE) < 0 || score.compareTo(new BigDecimal("5")) > 0) {
            return R.error("评分范围应在 1.0-5.0 之间");
        }
        Long userId = securityUtil.getCurrentUserId();
        poemRatingService.ratePoem(poemId, userId, score, comment);
        return R.success();
    }

    @PostMapping("/ai")
    @Operation(summary = "请求AI评分")
    public R<Void> requestAiRating(@PathVariable Long poemId) {
        poemRatingService.requestAiRating(poemId);
        return R.success();
    }

    @GetMapping("/ai")
    @Operation(summary = "获取AI评分")
    public R<PoemRating> getAiRating(@PathVariable Long poemId) {
        return R.success(poemRatingService.getAiRating(poemId));
    }
}
