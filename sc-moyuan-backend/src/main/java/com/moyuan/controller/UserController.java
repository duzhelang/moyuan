package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.dto.request.PasswordUpdateRequest;
import com.moyuan.dto.request.UserUpdateRequest;
import com.moyuan.entity.User;
import com.moyuan.service.UserService;
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户接口")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SecurityUtil securityUtil;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public R<User> getCurrentUser() {
        Long userId = securityUtil.getCurrentUserId();
        return R.success(userService.getUserInfo(userId));
    }

    @Operation(summary = "更新当前用户信息")
    @PutMapping("/me")
    public R<User> updateCurrentUser(@Valid @RequestBody UserUpdateRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        return R.success(userService.updateUserInfo(userId, request));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/me/password")
    public R<Void> updatePassword(@Valid @RequestBody PasswordUpdateRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        userService.updatePassword(userId, request);
        return R.success();
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/{id}")
    public R<User> getUser(@PathVariable Long id) {
        return R.success(userService.getUserInfo(id));
    }
}
