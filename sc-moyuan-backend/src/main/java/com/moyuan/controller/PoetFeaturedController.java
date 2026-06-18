package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.entity.PoetFeatured;
import com.moyuan.service.PoetFeaturedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "精选诗人接口")
@RestController
@RequestMapping("/api/poet-featured")
@RequiredArgsConstructor
public class PoetFeaturedController {

    private final PoetFeaturedService poetFeaturedService;

    @Operation(summary = "获取随机精选诗人")
    @GetMapping("/random")
    public R<List<PoetFeatured>> getRandomPoetFeatured(@RequestParam(defaultValue = "4") int count) {
        List<PoetFeatured> list = poetFeaturedService.getRandomPoetFeatured(count);
        return R.success(list);
    }

    @Operation(summary = "获取精选诗人列表")
    @GetMapping("/list")
    public R<List<PoetFeatured>> getPoetFeaturedList() {
        List<PoetFeatured> list = poetFeaturedService.list();
        return R.success(list);
    }

    @Operation(summary = "获取精选诗人详情")
    @GetMapping("/{id}")
    public R<PoetFeatured> getPoetFeaturedDetail(@PathVariable Long id) {
        PoetFeatured poetFeatured = poetFeaturedService.getPoetFeaturedDetail(id);
        if (poetFeatured == null) {
            return R.error("精选诗人不存在");
        }
        return R.success(poetFeatured);
    }
}