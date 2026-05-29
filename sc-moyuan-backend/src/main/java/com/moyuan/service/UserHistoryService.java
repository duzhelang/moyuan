package com.moyuan.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.entity.UserHistory;

public interface UserHistoryService extends IService<UserHistory> {
    void addHistory(Long userId, Long targetId, Integer targetType);
    IPage<UserHistory> getHistory(Long userId, Integer targetType, int pageNum, int pageSize);
    void clearHistory(Long userId, Integer targetType);
}
