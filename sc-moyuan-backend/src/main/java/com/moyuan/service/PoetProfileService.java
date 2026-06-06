package com.moyuan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.entity.PoetProfile;

public interface PoetProfileService extends IService<PoetProfile> {
    PoetProfile getByUserId(Long userId);
    void applyVerification(Long userId, PoetProfile profile);
    void verifyProfile(Long profileId, Integer status, String reason);
    void incrementWorkCount(Long userId);
    void incrementLikeCount(Long userId, int count);
}
