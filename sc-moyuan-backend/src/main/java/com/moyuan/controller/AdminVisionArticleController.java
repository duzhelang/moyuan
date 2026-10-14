package com.moyuan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.common.R;
import com.moyuan.entity.VisionArticle;
import com.moyuan.service.VisionArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 后台诗话视野管理接口
 */
@Slf4j
@Tag(name = "后台诗话视野管理接口")
@RestController
@RequestMapping("/api/admin/vision-articles")
@RequiredArgsConstructor
public class AdminVisionArticleController {

    private final VisionArticleService visionArticleService;

    @Operation(summary = "分页查询文章列表（含草稿）")
    @GetMapping
    public R<Map<String, Object>> listArticles(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        try {
            IPage<VisionArticle> page = visionArticleService.getAdminArticleList(pageNum, pageSize, status, category, keyword);
            Map<String, Object> result = new HashMap<>();
            result.put("records", page.getRecords());
            result.put("total", page.getTotal());
            result.put("pageNum", page.getCurrent());
            result.put("pageSize", page.getSize());
            return R.success(result);
        } catch (Exception e) {
            log.error("查询诗话视野列表失败", e);
            return R.error("查询诗话视野列表失败");
        }
    }

    @Operation(summary = "获取文章详情（任意状态）")
    @GetMapping("/{id}")
    public R<VisionArticle> getArticle(@PathVariable Long id) {
        VisionArticle article = visionArticleService.getById(id);
        if (article == null) {
            return R.error("文章不存在");
        }
        return R.success(article);
    }

    @Operation(summary = "新建文章")
    @PostMapping
    public R<VisionArticle> createArticle(@RequestBody VisionArticle article) {
        try {
            article.setId(null);
            if (article.getStatus() == null) article.setStatus(0);
            if (article.getIsFeatured() == null) article.setIsFeatured(0);
            if (article.getSortOrder() == null) article.setSortOrder(0);
            article.setViewCount(0);
            article.setLikeCount(0);
            visionArticleService.save(article);
            return R.success(article);
        } catch (Exception e) {
            log.error("新建诗话视野文章失败", e);
            return R.error("新建失败");
        }
    }

    @Operation(summary = "更新文章")
    @PutMapping("/{id}")
    public R<VisionArticle> updateArticle(@PathVariable Long id, @RequestBody VisionArticle article) {
        try {
            VisionArticle existing = visionArticleService.getById(id);
            if (existing == null) {
                return R.error("文章不存在");
            }
            article.setId(id);
            // 浏览量、点赞数不允许被覆盖
            article.setViewCount(null);
            article.setLikeCount(null);
            visionArticleService.updateById(article);
            return R.success(visionArticleService.getById(id));
        } catch (Exception e) {
            log.error("更新诗话视野文章失败：id={}", id, e);
            return R.error("更新失败");
        }
    }

    @Operation(summary = "上下架文章")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        Integer status = params.get("status");
        if (status == null) {
            return R.error("状态不能为空");
        }
        VisionArticle article = visionArticleService.getById(id);
        if (article == null) {
            return R.error("文章不存在");
        }
        article.setStatus(status);
        visionArticleService.updateById(article);
        return R.success();
    }

    @Operation(summary = "设置文章推荐状态")
    @PutMapping("/{id}/featured")
    public R<Void> updateFeatured(@PathVariable Long id, @RequestBody Map<String, Integer> params) {
        Integer isFeatured = params.get("isFeatured");
        if (isFeatured == null) {
            return R.error("推荐状态不能为空");
        }
        VisionArticle article = visionArticleService.getById(id);
        if (article == null) {
            return R.error("文章不存在");
        }
        article.setIsFeatured(isFeatured);
        visionArticleService.updateById(article);
        return R.success();
    }

    @Operation(summary = "删除文章（逻辑删除）")
    @DeleteMapping("/{id}")
    public R<Void> deleteArticle(@PathVariable Long id) {
        try {
            visionArticleService.removeById(id);
            return R.success();
        } catch (Exception e) {
            log.error("删除诗话视野文章失败：id={}", id, e);
            return R.error("删除失败");
        }
    }
}
