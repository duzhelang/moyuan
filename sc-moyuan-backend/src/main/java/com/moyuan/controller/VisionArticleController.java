package com.moyuan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.common.R;
import com.moyuan.entity.VisionArticle;
import com.moyuan.service.VisionArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "诗话视野")
@RestController
@RequestMapping("/api/vision")
@RequiredArgsConstructor
public class VisionArticleController {

    private final VisionArticleService visionArticleService;

    @Operation(summary = "获取文章列表")
    @GetMapping("/articles")
    public R<Map<String, Object>> getArticleList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String category) {
        IPage<VisionArticle> page = visionArticleService.getArticleList(pageNum, pageSize, category);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return R.success(result);
    }

    @Operation(summary = "获取推荐文章列表")
    @GetMapping("/articles/featured")
    public R<Map<String, Object>> getFeaturedArticles(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        IPage<VisionArticle> page = visionArticleService.getFeaturedArticles(pageNum, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        return R.success(result);
    }

    @Operation(summary = "获取文章详情")
    @GetMapping("/articles/{id}")
    public R<VisionArticle> getArticleDetail(@PathVariable Long id) {
        return R.success(visionArticleService.getArticleDetail(id));
    }

    @Operation(summary = "点赞文章")
    @PostMapping("/articles/{id}/like")
    public R<Void> likeArticle(@PathVariable Long id) {
        visionArticleService.toggleLike(id);
        return R.success();
    }
}
