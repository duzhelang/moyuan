package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.common.ResultCode;
import com.moyuan.entity.PoetProfile;
import com.moyuan.entity.User;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.PoetProfileMapper;
import com.moyuan.service.PoetProfileService;
import com.moyuan.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PoetProfileServiceImpl extends ServiceImpl<PoetProfileMapper, PoetProfile> implements PoetProfileService {

    private final UserService userService;

    @Override
    public PoetProfile getByUserId(Long userId) {
        return getOne(new LambdaQueryWrapper<PoetProfile>()
                .eq(PoetProfile::getUserId, userId));
    }

    @Override
    @Transactional
    public void applyVerification(Long userId, PoetProfile profile) {
        profile.setUserId(userId);
        profile.setVerifiedStatus(2);
        save(profile);

        User user = userService.getById(userId);
        user.setPoetVerified(2);
        user.setPoetProfileId(profile.getId());
        userService.updateById(user);
    }

    @Override
    @Transactional
    public void verifyProfile(Long profileId, Integer status, String reason) {
        // 审核结果只接受「通过(1)」与「拒绝(3)」，其余值直接拒绝，避免把 0/2 误写成终态
        if (status == null || (status != 1 && status != 3)) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        if (status == 3 && !StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        PoetProfile profile = getById(profileId);
        if (profile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        profile.setVerifiedStatus(status);
        if (StringUtils.hasText(reason)) {
            profile.setVerifiedReason(reason);
        }
        if (status == 1) {
            profile.setVerifiedTime(LocalDateTime.now());
        }
        // 置空交给数据库 ON UPDATE 维护，避免把读到的旧时间写回
        profile.setUpdateTime(null);
        updateById(profile);

        User user = userService.getById(profile.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setPoetVerified(status);
        user.setUpdateTime(null);
        userService.updateById(user);
    }

    @Override
    public void incrementWorkCount(Long userId) {
        baseMapper.incrementWorkCount(userId);
    }

    @Override
    public void incrementLikeCount(Long userId, int count) {
        baseMapper.incrementLikeCount(userId, count);
    }
}
