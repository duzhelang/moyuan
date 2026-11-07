package com.moyuan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.common.R;
import com.moyuan.entity.UserNotification;
import com.moyuan.service.NotificationService;
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "站内通知接口")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "我的通知列表")
    @GetMapping
    public R<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Boolean unreadOnly) {
        IPage<UserNotification> page = notificationService.getMyNotifications(
                SecurityUtil.getCurrentUserId(), pageNum, pageSize, unreadOnly);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return R.success(result);
    }

    @Operation(summary = "未读通知数量")
    @GetMapping("/unread-count")
    public R<Map<String, Long>> unreadCount() {
        Map<String, Long> result = new HashMap<>();
        result.put("count", notificationService.getUnreadCount(SecurityUtil.getCurrentUserId()));
        return R.success(result);
    }

    @Operation(summary = "标记单条通知已读")
    @PutMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(SecurityUtil.getCurrentUserId(), id);
        return R.success();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        notificationService.markAllRead(SecurityUtil.getCurrentUserId());
        return R.success();
    }
}
