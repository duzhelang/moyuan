package com.moyuan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.common.R;
import com.moyuan.dto.request.ReportHandleRequest;
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

@Tag(name = "举报管理接口")
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @Operation(summary = "举报分页列表")
    @GetMapping
    public R<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Integer aiFlag,
            @RequestParam(required = false) String keyword) {
        IPage<UserReport> result = reportService.adminPage(page, size, status, targetType, aiFlag, keyword);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return R.success(data);
    }

    @Operation(summary = "举报统计")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        return R.success(reportService.stats());
    }

    @Operation(summary = "处理举报")
    @PutMapping("/{id}/handle")
    public R<Void> handle(@PathVariable Long id, @Valid @RequestBody ReportHandleRequest request) {
        reportService.handle(SecurityUtil.getCurrentUserId(), id, request);
        return R.success();
    }
}
