package com.moyuan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.common.R;
import com.moyuan.entity.UserHistory;
import com.moyuan.service.UserHistoryService;
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "浏览历史接口")
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final UserHistoryService userHistoryService;

    @Operation(summary = "添加浏览历史")
    @PostMapping
    public R<Void> addHistory(@RequestParam Long targetId, @RequestParam Integer targetType) {
        Long userId = SecurityUtil.getCurrentUserId();
        userHistoryService.addHistory(userId, targetId, targetType);
        return R.success();
    }

    @Operation(summary = "获取浏览历史")
    @GetMapping
    public R<Map<String, Object>> getHistory(
            @RequestParam(required = false) Integer targetType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = SecurityUtil.getCurrentUserId();
        IPage<UserHistory> page = userHistoryService.getHistory(userId, targetType, pageNum, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return R.success(result);
    }

    @Operation(summary = "清空浏览历史")
    @DeleteMapping
    public R<Void> clearHistory(@RequestParam(required = false) Integer targetType) {
        Long userId = SecurityUtil.getCurrentUserId();
        userHistoryService.clearHistory(userId, targetType);
        return R.success();
    }
}
