package com.moyuan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.common.R;
import com.moyuan.common.ResultCode;
import com.moyuan.entity.Poem;
import com.moyuan.exception.BusinessException;
import com.moyuan.service.PoemService;
import com.moyuan.service.UserService;
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "诗词接口")
@RestController
@RequestMapping("/api/poems")
@RequiredArgsConstructor
public class PoemController {

    private final PoemService poemService;
    private final UserService userService;

    @Operation(summary = "获取诗词列表")
    @GetMapping
    public R<Map<String, Object>> getPoemList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long dynastyId,
            @RequestParam(required = false) Long poetId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        IPage<Poem> page = poemService.getPoemList(pageNum, pageSize, dynastyId, poetId, categoryId, keyword);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return R.success(result);
    }

    @Operation(summary = "获取随机诗词")
    @GetMapping("/random")
    public R<Poem> getRandomPoem() {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getStatus, 1)
                .last("ORDER BY RAND() LIMIT 1");
        Poem poem = poemService.getOne(wrapper);
        if (poem == null) {
            throw new BusinessException(ResultCode.POEM_NOT_FOUND);
        }
        return R.success(poem);
    }

    @Operation(summary = "获取每日推荐诗词")
    @GetMapping("/daily")
    public R<List<Poem>> getDailyPoems() {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getStatus, 1)
                .orderByDesc(Poem::getViewCount)
                .last("LIMIT 3");
        return R.success(poemService.list(wrapper));
    }

    @Operation(summary = "分页获取现代诗词")
    @GetMapping("/modern/page")
    public R<Map<String, Object>> getModernPoems(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Boolean isOriginal,
            @RequestParam(required = false) Boolean hasCertifiedPoet,
            @RequestParam(required = false, defaultValue = "latest") String sortBy) {
        IPage<Poem> page = poemService.getModernPoems(pageNum, pageSize, isOriginal, hasCertifiedPoet, sortBy);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return R.success(result);
    }

    @Operation(summary = "创建诗词（用户发布）")
    @PostMapping
    public R<Poem> createPoem(@RequestBody Poem poem) {
        SecurityUtil.getCurrentUserId();
        poem.setViewCount(0);
        poem.setLikeCount(0);
        poem.setFavoriteCount(0);
        poem.setStatus(1);
        poem.setIsFeatured(0);
        poemService.save(poem);
        return R.success(poem);
    }

    @Operation(summary = "获取诗词详情")
    @GetMapping("/{id}")
    public R<Poem> getPoemDetail(@PathVariable Long id) {
        return R.success(poemService.getPoemDetail(id));
    }

    @Operation(summary = "点赞/取消点赞")
    @PostMapping("/{id}/like")
    public R<Void> toggleLike(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        poemService.toggleLike(userId, id);
        return R.success();
    }

    @Operation(summary = "检查是否点赞")
    @GetMapping("/{id}/like")
    public R<Map<String, Boolean>> isLike(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        Map<String, Boolean> result = new HashMap<>();
        result.put("liked", poemService.isLike(userId, id));
        return R.success(result);
    }

    @Operation(summary = "收藏/取消收藏")
    @PostMapping("/{id}/favorite")
    public R<Void> toggleFavorite(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        userService.toggleFavorite(userId, id);
        return R.success();
    }

    @Operation(summary = "检查是否收藏")
    @GetMapping("/{id}/favorite")
    public R<Map<String, Boolean>> isFavorite(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        Map<String, Boolean> result = new HashMap<>();
        result.put("favorited", userService.isFavorite(userId, id));
        return R.success(result);
    }

    @Operation(summary = "获取我的收藏列表")
    @GetMapping("/favorites")
    public R<Map<String, Object>> getFavorites(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = SecurityUtil.getCurrentUserId();
        IPage<Poem> page = poemService.getFavorites(userId, pageNum, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return R.success(result);
    }
}
