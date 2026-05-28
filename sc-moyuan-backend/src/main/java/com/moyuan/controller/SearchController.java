package com.moyuan.controller;

import com.moyuan.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@Tag(name = "搜索接口")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    @Operation(summary = "全局搜索")
    @GetMapping
    public R<Map<String, Object>> search(@RequestParam String keyword) {
        Map<String, Object> result = Collections.singletonMap("results", Collections.emptyList());
        return R.success(result);
    }
}
