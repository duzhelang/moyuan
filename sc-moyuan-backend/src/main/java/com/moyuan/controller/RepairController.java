package com.moyuan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.common.R;
import com.moyuan.dto.request.RepairCommentCreateRequest;
import com.moyuan.dto.request.RepairOrderCreateRequest;
import com.moyuan.entity.RepairComment;
import com.moyuan.entity.RepairOrder;
import com.moyuan.service.RepairOrderService;
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "报修接口")
@RestController
@RequestMapping("/api/repair")
@RequiredArgsConstructor
public class RepairController {

    private final RepairOrderService repairOrderService;

    @Operation(summary = "提交报修")
    @PostMapping("/orders")
    public R<RepairOrder> createOrder(@Valid @RequestBody RepairOrderCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return R.success(repairOrderService.createOrder(userId, request));
    }

    @Operation(summary = "获取我的报修列表")
    @GetMapping("/orders")
    public R<Map<String, Object>> getMyOrders(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        Long userId = SecurityUtil.getCurrentUserId();
        IPage<RepairOrder> page = repairOrderService.getMyOrders(userId, pageNum, pageSize, status);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return R.success(result);
    }

    @Operation(summary = "获取报修详情")
    @GetMapping("/orders/{id}")
    public R<RepairOrder> getOrderDetail(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        return R.success(repairOrderService.getOrderDetail(id, userId));
    }

    @Operation(summary = "添加反馈评论")
    @PostMapping("/orders/{id}/comments")
    public R<RepairComment> addComment(@PathVariable Long id, @Valid @RequestBody RepairCommentCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return R.success(repairOrderService.addComment(id, userId, request));
    }

    @Operation(summary = "关闭工单")
    @PutMapping("/orders/{id}/close")
    public R<Void> closeOrder(@PathVariable Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        repairOrderService.closeOrder(id, userId);
        return R.success();
    }

    @Operation(summary = "提交满意度评价")
    @PutMapping("/orders/{id}/satisfaction")
    public R<Void> submitSatisfaction(
            @PathVariable Long id,
            @RequestParam Integer satisfaction,
            @RequestParam(required = false) String comment) {
        Long userId = SecurityUtil.getCurrentUserId();
        repairOrderService.submitSatisfaction(id, userId, satisfaction, comment);
        return R.success();
    }

    @Operation(summary = "获取评论列表")
    @GetMapping("/orders/{id}/comments")
    public R<List<RepairComment>> getComments(@PathVariable Long id) {
        return R.success(repairOrderService.getComments(id, false));
    }
}
