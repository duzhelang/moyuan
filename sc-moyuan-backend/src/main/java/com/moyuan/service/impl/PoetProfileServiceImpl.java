package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.entity.PoetProfile;
import com.moyuan.entity.User;
import com.moyuan.mapper.PoetProfileMapper;
import com.moyuan.service.PoetProfileService;
import com.moyuan.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        PoetProfile profile = getById(profileId);
        profile.setVerifiedStatus(status);
        profile.setVerifiedReason(reason);
        if (status == 1) {
            profile.setVerifiedTime(LocalDateTime.now());
        }
        updateById(profile);

        User user = userService.getById(profile.getUserId());
        user.setPoetVerified(status);
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
