package com.moyuan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.common.R;
import com.moyuan.dto.request.ReportCreateRequest;
import com.moyuan.entity.UserReport;
import com.moyuan.service.ReportService;
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "举报接口")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "提交举报（用户对用户/内容违规）")
    @PostMapping
    public R<UserReport> submit(@Valid @RequestBody ReportCreateRequest request) {
        return R.success(reportService.submit(SecurityUtil.getCurrentUserId(), request));
    }

    @Operation(summary = "我的举报列表")
    @GetMapping("/mine")
    public R<Map<String, Object>> getMyReports(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        IPage<UserReport> page = reportService.getMyReports(
                SecurityUtil.getCurrentUserId(), pageNum, pageSize, status);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return R.success(result);
    }

    @Operation(summary = "检查是否已举报过该内容")
    @GetMapping("/check")
    public R<Map<String, Boolean>> check(
            @RequestParam String targetType,
            @RequestParam Long targetId) {
        boolean reported = reportService.hasReported(
                SecurityUtil.getCurrentUserId(), targetType, targetId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("reported", reported);
        return R.success(result);
    }
}
