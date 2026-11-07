package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.entity.UserNotification;
import com.moyuan.mapper.UserNotificationMapper;
import com.moyuan.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<UserNotificationMapper, UserNotification>
        implements NotificationService {

    private final UserNotificationMapper notificationMapper;

    @Override
    public void push(Long userId, String type, String title, String content,
                     String bizType, Long bizId, String link) {
        if (userId == null || userId <= 0) {
            return;
        }
        try {
            UserNotification n = new UserNotification();
            n.setUserId(userId);
            n.setType(type);
            n.setTitle(title);
            n.setContent(content);
            n.setBizType(bizType);
            n.setBizId(bizId);
            n.setLink(link);
            n.setIsRead(0);
            notificationMapper.insert(n);
        } catch (Exception e) {
            // 通知属于增强能力，失败不能影响主业务
            log.warn("站内通知发送失败 userId={} type={} title={}", userId, type, title, e);
        }
    }

    @Override
    public IPage<UserNotification> getMyNotifications(Long userId, int pageNum, int pageSize, Boolean unreadOnly) {
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getUserId, userId)
                .orderByDesc(UserNotification::getCreateTime);
        if (Boolean.TRUE.equals(unreadOnly)) {
            wrapper.eq(UserNotification::getIsRead, 0);
        }
        return notificationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getIsRead, 0));
    }

    @Override
    public void markRead(Long userId, Long id) {
        notificationMapper.update(null, new LambdaUpdateWrapper<UserNotification>()
                .eq(UserNotification::getId, id)
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getIsRead, 0)
                .set(UserNotification::getIsRead, 1)
                .set(UserNotification::getReadAt, LocalDateTime.now()));
    }

    @Override
    public int markAllRead(Long userId) {
        return notificationMapper.update(null, new LambdaUpdateWrapper<UserNotification>()
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getIsRead, 0)
                .set(UserNotification::getIsRead, 1)
                .set(UserNotification::getReadAt, LocalDateTime.now()));
    }
}
