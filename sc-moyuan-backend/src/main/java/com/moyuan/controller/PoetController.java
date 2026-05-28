package com.moyuan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.common.R;
import com.moyuan.entity.Poet;
import com.moyuan.service.PoetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "诗人接口")
@RestController
@RequestMapping("/api/poets")
@RequiredArgsConstructor
public class PoetController {

    private final PoetService poetService;

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
}
