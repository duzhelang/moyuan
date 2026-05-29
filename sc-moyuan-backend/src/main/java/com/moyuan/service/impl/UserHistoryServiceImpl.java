package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.entity.UserHistory;
import com.moyuan.mapper.UserHistoryMapper;
import com.moyuan.service.UserHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserHistoryServiceImpl extends ServiceImpl<UserHistoryMapper, UserHistory> implements UserHistoryService {

    private final UserHistoryMapper userHistoryMapper;

    @Override
    @Transactional
    public void addHistory(Long userId, Long targetId, Integer targetType) {
        LambdaQueryWrapper<UserHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserHistory::getUserId, userId)
                .eq(UserHistory::getTargetId, targetId)
                .eq(UserHistory::getTargetType, targetType);
        userHistoryMapper.delete(wrapper);

        UserHistory history = new UserHistory();
        history.setUserId(userId);
        history.setTargetId(targetId);
        history.setTargetType(targetType);
        userHistoryMapper.insert(history);

        LambdaQueryWrapper<UserHistory> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(UserHistory::getUserId, userId)
                .eq(UserHistory::getTargetType, targetType)
                .orderByDesc(UserHistory::getCreateTime);
        Page<UserHistory> page = new Page<>(1, 200);
        IPage<UserHistory> existing = userHistoryMapper.selectPage(page, countWrapper);
        if (existing.getTotal() > 200) {
            List<UserHistory> toDelete = existing.getRecords().subList(200, existing.getRecords().size());
            toDelete.forEach(h -> userHistoryMapper.deleteById(h.getId()));
        }
    }

    @Override
    public IPage<UserHistory> getHistory(Long userId, Integer targetType, int pageNum, int pageSize) {
        LambdaQueryWrapper<UserHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserHistory::getUserId, userId);
        if (targetType != null) wrapper.eq(UserHistory::getTargetType, targetType);
        wrapper.orderByDesc(UserHistory::getCreateTime);
        return userHistoryMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public void clearHistory(Long userId, Integer targetType) {
        LambdaQueryWrapper<UserHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserHistory::getUserId, userId);
        if (targetType != null) wrapper.eq(UserHistory::getTargetType, targetType);
        userHistoryMapper.delete(wrapper);
    }
}
