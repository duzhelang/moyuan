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
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
    private final SecurityUtil securityUtil;

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
            userId = securityUtil.getCurrentUserId();
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
            userId = securityUtil.getCurrentUserId();
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
}
