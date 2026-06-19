package com.moyuan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.common.R;
import com.moyuan.entity.ForumPost;
import com.moyuan.entity.Poem;
import com.moyuan.entity.Poet;
import com.moyuan.mapper.PoemMapper;
import com.moyuan.mapper.PoetMapper;
import com.moyuan.service.ForumPostService;
import com.moyuan.service.RecommendationService;
import com.moyuan.service.SmartSearchService;
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "搜索接口")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final PoemMapper poemMapper;
    private final PoetMapper poetMapper;
    private final ForumPostService forumPostService;
    private final RecommendationService recommendationService;
    private final SmartSearchService smartSearchService;

    @Operation(summary = "全局搜索")
    @GetMapping
    public R<Map<String, Object>> search(@RequestParam String keyword) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<Poem> poemWrapper = new LambdaQueryWrapper<>();
        poemWrapper.eq(Poem::getStatus, 1)
                .and(w -> w.like(Poem::getTitle, keyword).or().like(Poem::getContent, keyword))
                .last("LIMIT 10");
        result.put("poems", poemMapper.selectList(poemWrapper));

        LambdaQueryWrapper<Poet> poetWrapper = new LambdaQueryWrapper<>();
        poetWrapper.eq(Poet::getStatus, 1)
                .like(Poet::getName, keyword)
                .last("LIMIT 10");
        result.put("poets", poetMapper.selectList(poetWrapper));

        LambdaQueryWrapper<ForumPost> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.eq(ForumPost::getStatus, 1)
                .and(w -> w.like(ForumPost::getTitle, keyword).or().like(ForumPost::getContent, keyword))
                .last("LIMIT 10");
        result.put("posts", forumPostService.list(postWrapper));

        return R.success(result);
    }

    @Operation(summary = "诗词搜索（支持协同过滤推荐）")
    @GetMapping("/poems")
    public R<List<Map<String, Object>>> searchPoems(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = null;
        try {
            userId = SecurityUtil.getCurrentUserId();
        } catch (Exception e) {
            // 未登录用户，userId为null
        }
        List<Map<String, Object>> results = recommendationService.searchPoems(keyword, userId, page, size);
        return R.success(results);
    }

    @Operation(summary = "获取推荐诗词（基于协同过滤）")
    @GetMapping("/poems/recommended")
    public R<List<Map<String, Object>>> getRecommendedPoems(
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = null;
        try {
            userId = SecurityUtil.getCurrentUserId();
        } catch (Exception e) {
            // 未登录用户，返回热门诗词
        }
        List<Map<String, Object>> results = recommendationService.getRecommendedPoems(userId, limit);
        return R.success(results);
    }

    @Operation(summary = "获取热门诗词")
    @GetMapping("/poems/popular")
    public R<List<Map<String, Object>>> getPopularPoems(
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> results = recommendationService.getPopularPoems(limit);
        return R.success(results);
    }

    @Operation(summary = "从外部API获取古诗词")
    @GetMapping("/poems/external")
    public R<List<Map<String, Object>>> getExternalPoems(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "5") int limit) {
        List<Map<String, Object>> results = recommendationService.getExternalPoems(keyword, limit);
        return R.success(results);
    }

    @Operation(summary = "从外部API获取诗词详情")
    @GetMapping("/poems/external/detail")
    public R<Map<String, Object>> getExternalPoemDetail(@RequestParam String keyword) {
        Map<String, Object> detail = recommendationService.getExternalPoemDetail(keyword);
        return R.success(detail);
    }

    @Operation(summary = "智能搜索（支持渐进式匹配）")
    @GetMapping("/smart")
    public R<Map<String, Object>> smartSearch(
            @RequestParam(required = false) Long dynastyId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long poetId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sortBy,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = null;
        try {
            userId = SecurityUtil.getCurrentUserId();
        } catch (Exception ignored) {}
        
        Map<String, Object> result = smartSearchService.smartSearch(dynastyId, categoryId, poetId, keyword, sortBy, pageNum, pageSize);
        
        if (userId != null && StringUtils.hasText(keyword)) {
            smartSearchService.saveSearchHistory(userId, keyword);
        }
        
        return R.success(result);
    }

    @Operation(summary = "获取搜索建议")
    @GetMapping("/suggestions")
    public R<List<String>> getSearchSuggestions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        return R.success(smartSearchService.getSearchSuggestions(keyword, limit));
    }

    @Operation(summary = "获取热门搜索")
    @GetMapping("/hot")
    public R<List<String>> getHotSearches(
            @RequestParam(defaultValue = "8") int limit) {
        return R.success(smartSearchService.getHotSearches(limit));
    }

    @Operation(summary = "获取搜索历史")
    @GetMapping("/history")
    public R<List<String>> getSearchHistory(
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = null;
        try {
            userId = SecurityUtil.getCurrentUserId();
        } catch (Exception ignored) {}
        return R.success(smartSearchService.getSearchHistory(userId, limit));
    }

    @Operation(summary = "清除搜索历史")
    @DeleteMapping("/history")
    public R<Void> clearSearchHistory() {
        Long userId = null;
        try {
            userId = SecurityUtil.getCurrentUserId();
        } catch (Exception ignored) {}
        smartSearchService.clearSearchHistory(userId);
        return R.success();
    }
}
