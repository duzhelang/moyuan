package com.moyuan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.common.R;
import com.moyuan.common.ResultCode;
import com.moyuan.entity.Dynasty;
import com.moyuan.entity.Poem;
import com.moyuan.entity.Poet;
import com.moyuan.exception.BusinessException;
import com.moyuan.service.DynastyService;
import com.moyuan.service.PoemService;
import com.moyuan.service.PoetService;
import com.moyuan.service.UserService;
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
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
    private final PoetService poetService;
    private final DynastyService dynastyService;
    private final CacheManager cacheManager;

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

    @Operation(summary = "导入外部诗词（按标题+诗人查重，已有返回ID，没有则导入）")
    @PostMapping("/import-external")
    public R<Map<String, Object>> importExternalPoem(@RequestBody Map<String, String> params) {
        String title = params.get("title");
        String content = params.get("content");
        String author = params.get("author");
        String dynasty = params.get("dynasty");

        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        Long poetId = null;
        if (author != null && !author.trim().isEmpty()) {
            LambdaQueryWrapper<Poet> poetWrapper = new LambdaQueryWrapper<>();
            poetWrapper.eq(Poet::getName, author.trim()).last("LIMIT 1");
            Poet poet = poetService.getOne(poetWrapper);
            if (poet != null) {
                poetId = poet.getId();
            }
        }

        LambdaQueryWrapper<Poem> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(Poem::getTitle, title.trim());
        if (poetId != null) {
            existWrapper.eq(Poem::getPoetId, poetId);
        } else {
            existWrapper.isNull(Poem::getPoetId);
        }
        existWrapper.last("LIMIT 1");
        Poem existPoem = poemService.getOne(existWrapper);

        if (existPoem != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("id", existPoem.getId());
            result.put("imported", false);
            return R.success(result);
        }

        Poem newPoem = new Poem();
        newPoem.setTitle(title.trim());
        newPoem.setContent(content != null ? content.trim() : "");
        newPoem.setPoetId(poetId);
        newPoem.setSource("external");

        if (dynasty != null && !dynasty.trim().isEmpty()) {
            LambdaQueryWrapper<Dynasty> dynastyWrapper = new LambdaQueryWrapper<>();
            dynastyWrapper.eq(Dynasty::getName, dynasty.trim()).last("LIMIT 1");
            Dynasty dynastyEntity = dynastyService.getOne(dynastyWrapper);
            if (dynastyEntity != null) {
                newPoem.setDynastyId(dynastyEntity.getId());
            }
        }

        newPoem.setViewCount(0);
        newPoem.setLikeCount(0);
        newPoem.setFavoriteCount(0);
        newPoem.setStatus(1);
        newPoem.setIsFeatured(0);
        newPoem.setIsOriginal(0);
        newPoem.setPoemType("ancient");
        newPoem.setAuditStatus(1);
        poemService.save(newPoem);

        if (cacheManager.getCache("poems") != null) {
            cacheManager.getCache("poems").clear();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", newPoem.getId());
        result.put("imported", true);
        return R.success(result);
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
