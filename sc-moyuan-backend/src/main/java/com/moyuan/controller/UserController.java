package com.moyuan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.moyuan.common.R;
import com.moyuan.dto.request.PasswordUpdateRequest;
import com.moyuan.dto.request.UserUpdateRequest;
import com.moyuan.entity.ForumPost;
import com.moyuan.entity.PoetProfile;
import com.moyuan.entity.User;
import com.moyuan.service.ForumPostService;
import com.moyuan.service.PoetProfileService;
import com.moyuan.service.UserService;
import com.moyuan.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "用户接口")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ForumPostService forumPostService;
    private final PoetProfileService poetProfileService;
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

    @Operation(summary = "获取当前用户的帖子列表")
    @GetMapping("/me/posts")
    public R<Map<String, Object>> getMyPosts(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = securityUtil.getCurrentUserId();
        IPage<ForumPost> page = forumPostService.getPostsByUserId(userId, pageNum, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return R.success(result);
    }

    @Operation(summary = "获取当前用户的统计信息")
    @GetMapping("/me/stats")
    public R<Map<String, Object>> getUserStats() {
        Long userId = securityUtil.getCurrentUserId();
        return R.success(userService.getUserStats(userId));
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/{id}")
    public R<User> getUser(@PathVariable Long id) {
        return R.success(userService.getUserInfo(id));
    }

    @Operation(summary = "获取用户主页信息")
    @GetMapping("/{id}/profile")
    public R<Map<String, Object>> getUserProfile(@PathVariable Long id) {
        User user = userService.getUserInfo(id);
        PoetProfile poetProfile = poetProfileService.getByUserId(id);
        Map<String, Object> stats = userService.getUserStats(id);

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("poetProfile", poetProfile);
        result.put("stats", stats);
        return R.success(result);
    }

    @Operation(summary = "获取用户作品列表")
    @GetMapping("/{id}/works")
    public R<Map<String, Object>> getUserWorks(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        IPage<ForumPost> page = forumPostService.getPostsByUserId(id, pageNum, pageSize);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return R.success(result);
    }
}
