package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.entity.PoemContentCache;
import com.moyuan.service.PoemContentCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "诗词内容缓存接口")
@RestController
@RequestMapping("/api/poem-content")
@RequiredArgsConstructor
public class PoemContentController {

    private final PoemContentCacheService poemContentCacheService;

    @Operation(summary = "获取缓存的诗词内容")
    @GetMapping("/cache")
    public R<Map<String, String>> getCachedContent(
            @RequestParam String poemTitle,
            @RequestParam String poetName,
            @RequestParam String contentType) {
        PoemContentCache cache = poemContentCacheService.getCachedContent(poemTitle, poetName, contentType);
        Map<String, String> result = new HashMap<>();
        if (cache != null) {
            result.put("content", cache.getContent());
            result.put("source", cache.getSource());
        }
        return R.success(result);
    }

    @Operation(summary = "保存诗词内容缓存")
    @PostMapping("/cache")
    public R<Void> saveCachedContent(@RequestBody Map<String, String> request) {
        String poemTitle = request.get("poemTitle");
        String poetName = request.get("poetName");
        String contentType = request.get("contentType");
        String content = request.get("content");
        String source = request.get("source");
        
        poemContentCacheService.saveCachedContent(poemTitle, poetName, contentType, content, source);
        return R.success();
    }
}
