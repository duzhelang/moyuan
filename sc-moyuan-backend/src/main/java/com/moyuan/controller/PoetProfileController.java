package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.entity.PoetProfile;
import com.moyuan.service.PoetProfileService;
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/poet-profile")
@RequiredArgsConstructor
@Tag(name = "诗人认证接口")
public class PoetProfileController {

    private final PoetProfileService poetProfileService;

    @GetMapping("/me")
    @Operation(summary = "获取当前用户诗人资料")
    public R<PoetProfile> getMyProfile() {
        Long userId = SecurityUtil.getCurrentUserId();
        PoetProfile profile = poetProfileService.getByUserId(userId);
        if (profile == null) {
            return R.error("诗人资料不存在");
        }
        return R.success(profile);
    }

    @PostMapping("/apply")
    @Operation(summary = "申请诗人认证")
    public R<Void> applyVerification(@RequestBody PoetProfile profile) {
        Long userId = SecurityUtil.getCurrentUserId();
        poetProfileService.applyVerification(userId, profile);
        return R.success();
    }

    @GetMapping("/{userId}")
    @Operation(summary = "获取指定用户诗人资料")
    public R<PoetProfile> getProfile(@PathVariable Long userId) {
        PoetProfile profile = poetProfileService.getByUserId(userId);
        if (profile == null) {
            return R.error("诗人资料不存在");
        }
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (!currentUserId.equals(userId)) {
            profile.setContactInfo(null);
        }
        return R.success(profile);
    }
}
