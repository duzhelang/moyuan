package com.moyuan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.common.R;
import com.moyuan.entity.Poet;
import com.moyuan.service.PoetRecommendationService;
import com.moyuan.service.PoetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "诗人接口")
@RestController
@RequestMapping("/api/poets")
@RequiredArgsConstructor
public class PoetController {

    private final PoetService poetService;
    private final PoetRecommendationService poetRecommendationService;

    @Operation(summary = "获取诗人列表")
    @GetMapping
    public R<Map<String, Object>> getPoetList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long dynastyId,
            @RequestParam(required = false) String keyword) {
        IPage<Poet> page = poetService.getPoetList(pageNum, pageSize, dynastyId, keyword);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return R.success(result);
    }

    @Operation(summary = "获取诗人详情")
    @GetMapping("/{id}")
    public R<Poet> getPoetDetail(@PathVariable Long id) {
        return R.success(poetService.getPoetDetail(id));
    }

    @Operation(summary = "推荐著名诗人（协同过滤）")
    @GetMapping("/recommend")
    public R<List<Poet>> getRecommendedPoets(@RequestParam(defaultValue = "6") int limit) {
        Long userId = resolveCurrentUserId();
        List<Poet> poets = poetRecommendationService.getRecommendedPoets(userId, Math.min(limit, 20));
        return R.success(poets);
    }

    @Operation(summary = "热门著名诗人排行")
    @GetMapping("/popular")
    public R<List<Poet>> getPopularPoets(@RequestParam(defaultValue = "10") int limit) {
        List<Poet> poets = poetRecommendationService.getPopularPoets(Math.min(limit, 50));
        return R.success(poets);
    }

    private Long resolveCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof com.moyuan.security.LoginUser loginUser) {
                return loginUser.getUser().getId();
            }
        } catch (Exception e) {
            log.debug("获取当前用户ID失败，将使用热门推荐: {}", e.getMessage());
        }
        return null;
    }
}
