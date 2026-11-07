package com.moyuan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.entity.UserNotification;

public interface NotificationService {

    /** 发送一条站内通知（弱管控，内部调用，不抛业务异常） */
    void push(Long userId, String type, String title, String content,
              String bizType, Long bizId, String link);

    IPage<UserNotification> getMyNotifications(Long userId, int pageNum, int pageSize, Boolean unreadOnly);

    long getUnreadCount(Long userId);

    void markRead(Long userId, Long id);

    int markAllRead(Long userId);
}
